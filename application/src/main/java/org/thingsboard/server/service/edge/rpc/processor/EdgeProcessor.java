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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class EdgeProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processEdgeNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (actionType) {
            case CHANGE_OWNER:
                ListenableFuture<Edge> edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                return Futures.transformAsync(edgeFuture, edge -> {
                    if (edge == null) {
                        return Futures.immediateFuture(null);
                    }
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    try {
                        EntityId previousOwnerId = mapper.readValue(edgeNotificationMsg.getBody(), EntityId.class);
                        if (previousOwnerId != null && EntityType.CUSTOMER.equals(previousOwnerId.getEntityType()) && !previousOwnerId.isNullUid()) {
                            futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(),
                                    EdgeEventType.CUSTOMER, EdgeEventActionType.DELETED, previousOwnerId, null));
                            futures.add(unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.DEVICE, previousOwnerId));
                            futures.add(unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.ASSET, previousOwnerId));
                            futures.add(unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.ENTITY_VIEW, previousOwnerId));
                            futures.add(unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.USER, previousOwnerId));
                            futures.add(unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.DASHBOARD, previousOwnerId));
                            futures.add(unassignSchedulerEventsOfPreviousOwnerFromEdge(tenantId, edgeId, previousOwnerId));
                        }
                        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                            futures.add(syncEdgeOwner(tenantId, edge));
                        }
                    } catch (Exception e) {
                        String errMsg = String.format("[%s] Failed to switch owner for edge [%s]", tenantId, edge);
                        log.error(errMsg, e);
                        return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
                    }
                    return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                }, dbCallbackExecutorService);
            default:
                return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> unassignEntityGroupsOfPreviousOwnerFromEdge(TenantId tenantId, EdgeId edgeId, EntityType groupType, EntityId previousOwnerId) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<EntityGroup> pageData;
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        do {
            pageData = entityGroupService.findEdgeEntityGroupsByType(tenantId, edgeId, groupType, pageLink);
            if (!pageData.getData().isEmpty()) {
                for (EntityGroup entityGroup : pageData.getData()) {
                    if (entityGroup.getOwnerId().equals(previousOwnerId)) {
                        entityGroupService.unassignEntityGroupFromEdge(tenantId, entityGroup.getId(), edgeId, groupType);
                        futures.add(saveEdgeEvent(tenantId, edgeId, EdgeEventType.ENTITY_GROUP, EdgeEventActionType.UNASSIGNED_FROM_EDGE, entityGroup.getId(), null));
                    }
                }
            }
        } while (pageData.hasNext());
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private ListenableFuture<Void> unassignSchedulerEventsOfPreviousOwnerFromEdge(TenantId tenantId, EdgeId edgeId, EntityId previousOwnerId) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<SchedulerEventInfo> pageData;
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        do {
            pageData = schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (!pageData.getData().isEmpty()) {
                for (SchedulerEventInfo schedulerEventInfo : pageData.getData()) {
                    if (schedulerEventInfo.getOwnerId().equals(previousOwnerId)) {
                        schedulerEventService.unassignSchedulerEventFromEdge(tenantId, schedulerEventInfo.getId(), edgeId);
                        futures.add(saveEdgeEvent(tenantId, edgeId, EdgeEventType.SCHEDULER_EVENT, EdgeEventActionType.UNASSIGNED_FROM_EDGE, schedulerEventInfo.getId(), null));
                    }
                }
            }
        } while (pageData.hasNext());
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private ListenableFuture<Void> syncEdgeOwner(TenantId tenantId, Edge edge) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(),
                EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, edge.getOwnerId(), null, null));
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<Role> rolesData;
        do {
            rolesData = roleService.findRolesByTenantIdAndCustomerId(tenantId,
                    new CustomerId(edge.getOwnerId().getId()), pageLink);
            if (rolesData != null && rolesData.getData() != null && !rolesData.getData().isEmpty()) {
                for (Role role : rolesData.getData()) {
                    futures.add(saveEdgeEvent(tenantId, edge.getId(),
                            EdgeEventType.ROLE, EdgeEventActionType.ADDED, role.getId(), null, null));
                }
                if (rolesData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (rolesData != null && rolesData.hasNext());
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

}
