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
package org.thingsboard.server.service.edge.rpc.processor.relation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@TbCoreComponent
public class RelationEdgeProcessor extends BaseRelationProcessor {

    public ListenableFuture<Void> processRelationMsgFromEdge(TenantId tenantId, Edge edge, RelationUpdateMsg relationUpdateMsg, EdgeVersion edgeVersion) {
        log.trace("[{}] executing processRelationMsgFromEdge [{}] from edge [{}]", tenantId, relationUpdateMsg, edge.getId());
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());
            return processRelationMsg(tenantId, relationUpdateMsg, edgeVersion);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    public DownlinkMsg convertRelationEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        EntityRelation entityRelation = JacksonUtil.convertValue(edgeEvent.getBody(), EntityRelation.class);
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        RelationUpdateMsg relationUpdateMsg = relationMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion).constructRelationUpdatedMsg(msgType, entityRelation);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addRelationUpdateMsg(relationUpdateMsg)
                .build();
    }

    public ListenableFuture<Void> processRelationNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EntityRelation relation = JacksonUtil.fromString(edgeNotificationMsg.getBody(), EntityRelation.class);
        if (relation == null || (relation.getFrom().getEntityType().equals(EntityType.EDGE) || relation.getTo().getEntityType().equals(EntityType.EDGE))) {
            return Futures.immediateFuture(null);
        }
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());

        Set<EdgeId> uniqueEdgeIds = new HashSet<>();
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getTo()));
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getFrom()));
        uniqueEdgeIds.remove(originatorEdgeId);
        if (uniqueEdgeIds.isEmpty()) {
            return Futures.immediateFuture(null);
        }
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (EdgeId edgeId : uniqueEdgeIds) {
            futures.add(saveEdgeEvent(tenantId,
                    edgeId,
                    EdgeEventType.RELATION,
                    EdgeEventActionType.valueOf(edgeNotificationMsg.getAction()),
                    null,
                    JacksonUtil.valueToTree(relation)));
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }
}
