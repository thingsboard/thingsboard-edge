/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.edge.rpc.processor.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class EdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        EdgeId edgeId = new EdgeId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        // TODO: @voba - check this
        if (EdgeEventActionType.CHANGE_OWNER.equals(edgeEvent.getAction())) {
            Edge edge = edgeService.findEdgeById(edgeEvent.getTenantId(), edgeId);
            if (edge != null) {
                EdgeConfiguration edgeConfigMsg = edgeMsgConstructor.constructEdgeConfiguration(edge);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .setEdgeConfiguration(edgeConfigMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processEdgeNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (actionType) {
            case CHANGE_OWNER:
                ListenableFuture<Edge> edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                return Futures.transformAsync(edgeFuture, edge -> {
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    try {
                        EntityId previousOwnerId = JacksonUtil.fromString(edgeNotificationMsg.getBody(), EntityId.class);
                        List<Customer> previousOwnerCustomerHierarchy = new ArrayList<>();
                        if (previousOwnerId != null && EntityType.CUSTOMER.equals(previousOwnerId.getEntityType())) {
                            previousOwnerCustomerHierarchy =
                                    customersHierarchyEdgeService.getCustomersHierarchy(tenantId, new CustomerId(previousOwnerId.getId()));
                        }
                        List<Customer> ownerCustomerHierarchy = new ArrayList<>();
                        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                            ownerCustomerHierarchy =
                                    customersHierarchyEdgeService.getCustomersHierarchy(tenantId, new CustomerId(edge.getOwnerId().getId()));
                        }
                        List<Customer> removedCustomers = new ArrayList<>(previousOwnerCustomerHierarchy);
                        removedCustomers.removeAll(ownerCustomerHierarchy);
                        for (Customer removedCustomer : removedCustomers) {
                            if (!removedCustomer.getId().isNullUid()) {
                                CustomerId removedCustomerId = removedCustomer.getId();
                                futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(),
                                        EdgeEventType.CUSTOMER, EdgeEventActionType.DELETED, removedCustomerId, null));
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.DEVICE, removedCustomerId);
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.ASSET, removedCustomerId);
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.ENTITY_VIEW, removedCustomerId);
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.USER, removedCustomerId);
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.DASHBOARD, removedCustomerId);
                                unassignSchedulerEventsOfRemovedCustomer(tenantId, edgeId, removedCustomerId);
                            }
                        }
                        List<Customer> addedCustomers = new ArrayList<>(ownerCustomerHierarchy);
                        addedCustomers.removeAll(previousOwnerCustomerHierarchy);
                        for (Customer addedCustomer : addedCustomers) {
                            if (!addedCustomer.getId().isNullUid()) {
                                futures.add(syncCustomer(tenantId, edge.getId(), addedCustomer));
                            }
                        }
                        customersHierarchyEdgeService.processEdgeChangeOwner(tenantId, edgeId, previousOwnerId);
                        futures.add(saveEdgeEvent(edge.getTenantId(), edgeId, EdgeEventType.EDGE, EdgeEventActionType.CHANGE_OWNER, edgeId, null));
                    } catch (Exception e) {
                        log.error("[{}] Failed to switch owner for edge [{}]", tenantId, edge, e);
                        return Futures.immediateFailedFuture(e);
                    }
                    return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                }, dbCallbackExecutorService);
            case ATTRIBUTES_UPDATED:
                return processAttributesUpdated(tenantId, edgeId, edgeNotificationMsg);
            default:
                return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> processAttributesUpdated(TenantId tenantId, EdgeId edgeId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        List<String> attributeKeys = new ArrayList<>();
        try {
            ArrayNode attributes = (ArrayNode) JacksonUtil.OBJECT_MAPPER.readTree(edgeNotificationMsg.getBody());
            for (JsonNode attribute : attributes) {
                attributeKeys.add(attribute.get("key").asText());
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Can't process attributes updated event {}", tenantId, edgeId, edgeNotificationMsg, e);
            return Futures.immediateFailedFuture(e);
        }
        Set<IntegrationId> integrationIds = new HashSet<>();
        PageDataIterable<Integration> integrationPageDataIterable = new PageDataIterable<>(
                link -> integrationService.findIntegrationsByTenantIdAndEdgeId(tenantId, edgeId, link), 1024);
        for (Integration integration : integrationPageDataIterable) {
            for (String attributeKey : attributeKeys) {
                if (integration.getConfiguration().toString().contains(EdgeUtils.formatAttributeKeyToPlaceholderFormat(attributeKey))) {
                    integrationIds.add(integration.getId());
                }
            }
        }
        if (integrationIds.isEmpty()) {
            return Futures.immediateFuture(null);
        } else {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            for (IntegrationId integrationId : integrationIds) {
                futures.add(saveEdgeEvent(tenantId, edgeId, EdgeEventType.INTEGRATION, EdgeEventActionType.UPDATED, integrationId, null));
            }
            return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
        }
    }

    private void unassignEntityGroupsOfRemovedCustomer(TenantId tenantId, EdgeId edgeId, EntityType groupType, EntityId customerId) {
        PageDataIterable<EntityGroup> entityGroups = new PageDataIterable<>(
                link -> entityGroupService.findEdgeEntityGroupsByType(tenantId, edgeId, groupType, link), 1024);
        for (EntityGroup entityGroup : entityGroups) {
            if (entityGroup.getOwnerId().equals(customerId)) {
                entityGroupService.unassignEntityGroupFromEdge(tenantId, entityGroup.getId(), edgeId, groupType);
            }
        }
    }

    private void unassignSchedulerEventsOfRemovedCustomer(TenantId tenantId, EdgeId edgeId, EntityId previousOwnerId) {
        PageDataIterable<SchedulerEventInfo> schedulerEventInfos = new PageDataIterable<>(
                link -> schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edgeId, link), 1024);
        for (SchedulerEventInfo schedulerEventInfo : schedulerEventInfos) {
            if (schedulerEventInfo.getOwnerId().equals(previousOwnerId)) {
                schedulerEventService.unassignSchedulerEventFromEdge(tenantId, schedulerEventInfo.getId(), edgeId);
            }
        }
    }

    private ListenableFuture<Void> syncCustomer(TenantId tenantId, EdgeId edgeId, Customer customer) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(saveEdgeEvent(tenantId, edgeId,
                EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, customer.getId(), null, null));
        PageDataIterable<Role> roles = new PageDataIterable<>(
                link -> roleService.findRolesByTenantIdAndCustomerId(tenantId, customer.getId(), link), 1024);
        for (Role role : roles) {
            futures.add(saveEdgeEvent(tenantId, edgeId,
                    EdgeEventType.ROLE, EdgeEventActionType.ADDED, role.getId(), null, null));
        }
        assignCustomerAdministratorsAndUsersGroupToEdge(tenantId, edgeId, customer.getId(), customer.getParentCustomerId());
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private void assignCustomerAdministratorsAndUsersGroupToEdge(TenantId tenantId,
                                                                 EdgeId edgeId,
                                                                 CustomerId customerId,
                                                                 CustomerId parentCustomerId) {
        EntityGroup customerAdmins = entityGroupService.findOrCreateCustomerAdminsGroup(tenantId, customerId, parentCustomerId);
        entityGroupService.assignEntityGroupToEdge(tenantId, customerAdmins.getId(), edgeId, customerAdmins.getType());
        EntityGroup customerUsers = entityGroupService.findOrCreateCustomerUsersGroup(tenantId, customerId, parentCustomerId);
        entityGroupService.assignEntityGroupToEdge(tenantId, customerUsers.getId(), edgeId, customerUsers.getType());
    }

}
