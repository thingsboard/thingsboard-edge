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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EntityEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.GroupPermissionsEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RoleEdgeProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeNotificationService implements EdgeNotificationService {

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    private EdgeProcessor edgeProcessor;

    @Autowired
    private EntityEdgeProcessor entityProcessor;

    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    @Autowired
    private RelationEdgeProcessor relationProcessor;

    private ExecutorService dbCallBackExecutor;

    // PE context

    @Autowired
    private RoleEdgeProcessor roleProcessor;

    @Autowired
    private GroupPermissionsEdgeProcessor entityGroupProcessor;

    @PostConstruct
    public void initExecutor() {
        dbCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-notifications"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (dbCallBackExecutor != null) {
            dbCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) throws IOException {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN, EdgeEventActionType.UPDATED, ruleChainId, null);
        return savedEdge;
    }

    private void saveEdgeEvent(TenantId tenantId,
                               EdgeId edgeId,
                               EdgeEventType type,
                               EdgeEventActionType action,
                               EntityId entityId,
                               JsonNode body) {
        saveEdgeEvent(tenantId, edgeId, type, action, entityId, body, null);
    }

    private void saveEdgeEvent(TenantId tenantId,
                               EdgeId edgeId,
                               EdgeEventType type,
                               EdgeEventActionType action,
                               EntityId entityId,
                               JsonNode body,
                               EntityGroupId entityGroupId) {
        log.debug("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], type [{}], action[{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body, entityGroupId);

        Futures.addCallback(edgeEventService.saveAsync(edgeEvent), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void unused) {
                clusterService.onEdgeEventUpdate(tenantId, edgeId);
            }

            @Override
            public void onFailure(Throwable t) {
                String errMsg = String.format("Failed to save edge event. edge event [%s]", edgeEvent);
                log.warn(errMsg, t);
            }
        }, dbCallBackExecutor);
    }

    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        log.debug("Pushing notification to edge {}", edgeNotificationMsg);
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
            EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
            switch (type) {
                case EDGE:
                    edgeProcessor.processEdgeNotification(tenantId, edgeNotificationMsg);
                    break;
                case USER:
                case ASSET:
                case DEVICE:
                case DEVICE_PROFILE:
                case ENTITY_VIEW:
                case DASHBOARD:
                case CUSTOMER:
                case RULE_CHAIN:
                case SCHEDULER_EVENT:
                case ENTITY_GROUP:
                    entityProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGETS_BUNDLE:
                case WIDGET_TYPE:
                    entityProcessor.processEntityNotificationForAllEdges(tenantId, edgeNotificationMsg);
                    break;
                case ALARM:
                    alarmProcessor.processAlarmNotification(tenantId, edgeNotificationMsg);
                    break;
                case ROLE:
                    roleProcessor.processRoleNotification(tenantId, edgeNotificationMsg);
                    break;
                case GROUP_PERMISSION:
                    entityGroupProcessor.processGroupPermissionNotification(tenantId, edgeNotificationMsg);
                    break;
                case RELATION:
                    relationProcessor.processRelationNotification(tenantId, edgeNotificationMsg);
                    break;
                default:
                    log.warn("Edge event type [{}] is not designed to be pushed to edge", type);
            }
        } catch (Exception e) {
            callback.onFailure(e);
            String errMsg = String.format("Can't push to edge updates, edgeNotificationMsg [%s]", edgeNotificationMsg);
            log.error(errMsg, e);
        } finally {
            callback.onSuccess();
        }
    }

}

