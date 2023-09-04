/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor.entityview;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class EntityViewEdgeProcessor extends BaseEntityViewProcessor {

    public ListenableFuture<Void> processEntityViewMsgFromEdge(TenantId tenantId, Edge edge, EntityViewUpdateMsg entityViewUpdateMsg) {
        log.trace("[{}] executing processEntityViewMsgFromEdge [{}] from edge [{}]", tenantId, entityViewUpdateMsg, edge.getName());
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getSync().set(true);

            switch (entityViewUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    if (entityViewUpdateMsg.hasEntityGroupIdMSB() && entityViewUpdateMsg.hasEntityGroupIdLSB()) {
                        EntityGroupId entityGroupId = new EntityGroupId(
                                new UUID(entityViewUpdateMsg.getEntityGroupIdMSB(), entityViewUpdateMsg.getEntityGroupIdLSB()));
                        entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, entityViewId);
                    } else {
                        removeEntityViewFromEdgeAllEntityViewGroup(tenantId, edge, entityViewId);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(entityViewUpdateMsg.getMsgType());
            }
        } catch (DataValidationException | ThingsboardException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed entity views violated {}", tenantId, entityViewUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateEntityView(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg, Edge edge) throws ThingsboardException {
        CustomerId customerId = safeGetCustomerId(entityViewUpdateMsg.getCustomerIdMSB(), entityViewUpdateMsg.getCustomerIdLSB());
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg, customerId);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), entityViewId);
            pushEntityViewCreatedEventToRuleEngine(tenantId, edge, entityViewId);
        }
        addEntityViewToEdgeAllEntityViewGroup(tenantId, edge, entityViewId);
        Boolean assetNameUpdated = resultPair.getSecond();
        if (assetNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW, EdgeEventActionType.UPDATED, entityViewId, null);
        }
    }

    private void pushEntityViewCreatedEventToRuleEngine(TenantId tenantId, Edge edge, EntityViewId entityViewId) {
        try {
            EntityView entityView = entityViewService.findEntityViewById(tenantId, entityViewId);
            String entityViewAsString = JacksonUtil.toString(entityView);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, entityView.getCustomerId());
            pushEntityEventToRuleEngine(tenantId, entityViewId, entityView.getCustomerId(), TbMsgType.ENTITY_CREATED, entityViewAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push entity view action to rule engine: {}", tenantId, entityViewId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    private void removeEntityViewFromEdgeAllEntityViewGroup(TenantId tenantId, Edge edge, EntityViewId entityViewId) {
        EntityView entityViewToDelete = entityViewService.findEntityViewById(tenantId, entityViewId);
        if (entityViewToDelete != null) {
            try {
                EntityGroup edgeEntityViewGroup = entityGroupService.findOrCreateEdgeAllGroupAsync(tenantId, edge, edge.getName(), EntityType.ENTITY_VIEW).get();
                if (edgeEntityViewGroup != null) {
                    entityGroupService.removeEntityFromEntityGroup(tenantId, edgeEntityViewGroup.getId(), entityViewToDelete.getId());
                }
            } catch (Exception e) {
                log.warn("[{}] Can't delete entity view from edge entity view 'All' group, entity view id [{}]", tenantId, entityViewId, e);
                throw new RuntimeException(e);
            }
        }
    }

    private void addEntityViewToEdgeAllEntityViewGroup(TenantId tenantId, Edge edge, EntityViewId entityViewId) {
        try {
            EntityGroup edgeEntityViewGroup = entityGroupService.findOrCreateEdgeAllGroupAsync(tenantId, edge, edge.getName(), EntityType.ENTITY_VIEW).get();
            if (edgeEntityViewGroup != null) {
                entityGroupService.addEntityToEntityGroup(tenantId, edgeEntityViewGroup.getId(), entityViewId);
            }
        } catch (Exception e) {
            log.warn("Can't add entity view to edge entity view group, entity view id [{}]", entityViewId, e);
            throw new RuntimeException(e);
        }
    }

    public DownlinkMsg convertEntityViewEventToDownlink(EdgeEvent edgeEvent) {
        EntityViewId entityViewId = new EntityViewId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                EntityView entityView = entityViewService.findEntityViewById(edgeEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    EntityViewUpdateMsg entityViewUpdateMsg =
                            entityViewMsgConstructor.constructEntityViewUpdatedMsg(msgType, entityView, entityGroupId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addEntityViewUpdateMsg(entityViewUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
            case CHANGE_OWNER:
                EntityViewUpdateMsg entityViewUpdateMsg =
                        entityViewMsgConstructor.constructEntityViewDeleteMsg(entityViewId, entityGroupId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addEntityViewUpdateMsg(entityViewUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }
}
