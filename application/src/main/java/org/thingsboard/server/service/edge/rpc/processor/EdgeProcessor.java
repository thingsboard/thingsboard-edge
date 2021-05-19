/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
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

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class EdgeProcessor extends BaseEdgeProcessor {

    public void processEdgeNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        try {
            EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
            EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
            ListenableFuture<Edge> edgeFuture;
            switch (actionType) {
                case CHANGE_OWNER:
                    CustomerId customerId = mapper.readValue(edgeNotificationMsg.getBody(), CustomerId.class);
                    edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                    Futures.addCallback(edgeFuture, new FutureCallback<Edge>() {
                        @Override
                        public void onSuccess(@Nullable Edge edge) {
                            if (edge != null && !customerId.isNullUid()) {
                                try {
                                    EntityId previousOwnerId = mapper.readValue(edgeNotificationMsg.getBody(), EntityId.class);
                                    if (previousOwnerId != null && EntityType.CUSTOMER.equals(previousOwnerId.getEntityType())) {
                                        saveEdgeEvent(edge.getTenantId(), edge.getId(),
                                                EdgeEventType.CUSTOMER, EdgeEventActionType.DELETED, previousOwnerId, null);
                                        unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.DEVICE, previousOwnerId);
                                        unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.ASSET, previousOwnerId);
                                        unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.ENTITY_VIEW, previousOwnerId);
                                        unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.USER, previousOwnerId);
                                        unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.DASHBOARD, previousOwnerId);
                                        unassignSchedulerEventsOfPreviousOwnerFromEdge(tenantId, edgeId, previousOwnerId);
                                    }
                                    if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                                        syncEdgeOwner(tenantId, edge);
                                    }
                                } catch (Exception e) {
                                    log.error("[{}] Failed to switch owner for edge [{}]", tenantId, edge, e);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("[{}] Can't find edge by id [{}]", tenantId, edgeNotificationMsg, t);
                        }
                    }, dbCallbackExecutorService);
                    break;

            }
        } catch (Exception e) {
            log.error("[{}] Exception during processing edge event [{}]", tenantId, edgeNotificationMsg, e);
        }
    }

    private void unassignEntityGroupsOfPreviousOwnerFromEdge(TenantId tenantId, EdgeId edgeId, EntityType groupType, EntityId previousOwnerId) {
        ListenableFuture<List<EntityGroup>> future = entityGroupService.findEdgeEntityGroupsByType(tenantId, edgeId, groupType);
        Futures.addCallback(future, new FutureCallback<List<EntityGroup>>() {
            @Override
            public void onSuccess(@Nullable List<EntityGroup> entityGroups) {
                if (entityGroups != null && !entityGroups.isEmpty()) {
                    for (EntityGroup entityGroup : entityGroups) {
                        if (entityGroup.getOwnerId().equals(previousOwnerId)) {
                            entityGroupService.unassignEntityGroupFromEdge(tenantId, entityGroup.getId(), edgeId, groupType);
                            // TODO: voba - remove of the customer should remove entity groups as well - double check this
                            // saveEdgeEvent(tenantId, edgeId, EdgeEventType.ENTITY_GROUP, ActionType.UNASSIGNED_FROM_EDGE, entityGroup.getId(), null);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("[{}] Failed to find edge entity groups by type [{}][{}]", tenantId, edgeId, groupType, throwable);
            }
        }, dbCallbackExecutorService);
    }

    private void unassignSchedulerEventsOfPreviousOwnerFromEdge(TenantId tenantId, EdgeId edgeId, EntityId previousOwnerId) {
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        PageData<SchedulerEventInfo> pageData = schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
        if (pageData.getData() != null && !pageData.getData().isEmpty()) {
            for (SchedulerEventInfo schedulerEventInfo : pageData.getData()) {
                if (schedulerEventInfo.getOwnerId().equals(previousOwnerId)) {
                    schedulerEventService.unassignSchedulerEventFromEdge(tenantId, schedulerEventInfo.getId(), edgeId);
                    saveEdgeEvent(tenantId, edgeId, EdgeEventType.SCHEDULER_EVENT, EdgeEventActionType.UNASSIGNED_FROM_EDGE, schedulerEventInfo.getId(), null);
                }
            }
        }
    }

    private void syncEdgeOwner(TenantId tenantId, Edge edge) {
        saveEdgeEvent(edge.getTenantId(), edge.getId(),
                EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, edge.getOwnerId(), null, null);
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        PageData<Role> rolesData;
        do {
            rolesData = roleService.findRolesByTenantIdAndCustomerId(tenantId,
                    new CustomerId(edge.getOwnerId().getId()), pageLink);
            if (rolesData != null && rolesData.getData() != null && !rolesData.getData().isEmpty()) {
                for (Role role : rolesData.getData()) {
                    saveEdgeEvent(tenantId, edge.getId(),
                            EdgeEventType.ROLE, EdgeEventActionType.ADDED, role.getId(), null, null);
                }
                if (rolesData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (rolesData != null && rolesData.hasNext());
    }

}
