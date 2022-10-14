/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor;

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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class EdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        EdgeId edgeId = new EdgeId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            // TODO: @voba - check this
            case CHANGE_OWNER:
                Edge edge = edgeService.findEdgeById(edgeEvent.getTenantId(), edgeId);
                if (edge != null) {
                    EdgeConfiguration edgeConfigMsg =
                            edgeMsgConstructor.constructEdgeConfiguration(edge);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setEdgeConfiguration(edgeConfigMsg)
                            .build();
                }
                break;
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
                        EntityId previousOwnerId = JacksonUtil.OBJECT_MAPPER.readValue(edgeNotificationMsg.getBody(), EntityId.class);
                        List<Customer> previousOwnerCustomerHierarchy = new ArrayList<>();
                        if (EntityType.CUSTOMER.equals(previousOwnerId.getEntityType())) {
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
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<Integration> pageData;
        Set<IntegrationId> integrationIds = new HashSet<>();
        do {
            pageData = integrationService.findIntegrationsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (Integration integration : pageData.getData()) {
                    for (String attributeKey : attributeKeys) {
                        if (integration.getConfiguration().toString().contains(EdgeUtils.formatAttributeKeyToPlaceholderFormat(attributeKey))) {
                            integrationIds.add(integration.getId());
                        }
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
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
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<EntityGroup> pageData;
        do {
            pageData = entityGroupService.findEdgeEntityGroupsByType(tenantId, edgeId, groupType, pageLink);
            if (!pageData.getData().isEmpty()) {
                for (EntityGroup entityGroup : pageData.getData()) {
                    if (entityGroup.getOwnerId().equals(customerId)) {
                        entityGroupService.unassignEntityGroupFromEdge(tenantId, entityGroup.getId(), edgeId, groupType);
                    }
                }
            }
        } while (pageData.hasNext());
    }

    private void unassignSchedulerEventsOfRemovedCustomer(TenantId tenantId, EdgeId edgeId, EntityId previousOwnerId) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<SchedulerEventInfo> pageData;
        do {
            pageData = schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (!pageData.getData().isEmpty()) {
                for (SchedulerEventInfo schedulerEventInfo : pageData.getData()) {
                    if (schedulerEventInfo.getOwnerId().equals(previousOwnerId)) {
                        schedulerEventService.unassignSchedulerEventFromEdge(tenantId, schedulerEventInfo.getId(), edgeId);
                    }
                }
            }
        } while (pageData.hasNext());
    }

    private ListenableFuture<Void> syncCustomer(TenantId tenantId, EdgeId edgeId, Customer customer) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(saveEdgeEvent(tenantId, edgeId,
                EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, customer.getId(), null, null));
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<Role> rolesData;
        do {
            rolesData = roleService.findRolesByTenantIdAndCustomerId(tenantId,
                    customer.getId(), pageLink);
            if (rolesData != null && rolesData.getData() != null && !rolesData.getData().isEmpty()) {
                for (Role role : rolesData.getData()) {
                    futures.add(saveEdgeEvent(tenantId, edgeId,
                            EdgeEventType.ROLE, EdgeEventActionType.ADDED, role.getId(), null, null));
                }
                if (rolesData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (rolesData != null && rolesData.hasNext());
        futures.addAll(assignCustomerAdministratorsAndUsersGroupToEdge(tenantId, edgeId, customer.getId(), customer.getParentCustomerId()));
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private List<ListenableFuture<Void>> assignCustomerAdministratorsAndUsersGroupToEdge(TenantId tenantId,
                                                                                         EdgeId edgeId,
                                                                                         CustomerId customerId,
                                                                                         CustomerId parentCustomerId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        EntityGroup customerAdmins = entityGroupService.findOrCreateCustomerAdminsGroup(tenantId, customerId, parentCustomerId);
        entityGroupService.assignEntityGroupToEdge(tenantId, customerAdmins.getId(), edgeId, customerAdmins.getType());
        futures.add(saveEdgeEvent(tenantId, edgeId, EdgeEventType.ENTITY_GROUP, EdgeEventActionType.ASSIGNED_TO_EDGE,
                customerAdmins.getId(), null, null));

        EntityGroup customerUsers = entityGroupService.findOrCreateCustomerUsersGroup(tenantId, customerId, parentCustomerId);
        entityGroupService.assignEntityGroupToEdge(tenantId, customerUsers.getId(), edgeId, customerUsers.getType());
        futures.add(saveEdgeEvent(tenantId, edgeId, EdgeEventType.ENTITY_GROUP, EdgeEventActionType.ASSIGNED_TO_EDGE,
                customerUsers.getId(), null, null));
        return futures;
    }
}
