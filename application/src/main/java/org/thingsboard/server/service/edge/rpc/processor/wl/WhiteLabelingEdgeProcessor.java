/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor.wl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class WhiteLabelingEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    protected WhiteLabelingService whiteLabelingService;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        DownlinkMsg result = null;
        try {
            EntityId entityId = JacksonUtil.convertValue(edgeEvent.getBody(), EntityId.class);
            if (entityId == null) {
                return null;
            }
            TenantId tenantId = EntityType.TENANT.equals(entityId.getEntityType()) ? (TenantId) entityId : edgeEvent.getTenantId();
            CustomerId customerId = EntityType.CUSTOMER.equals(entityId.getEntityType()) ? new CustomerId(entityId.getId()) : null;
            WhiteLabeling whiteLabeling = whiteLabelingService.findByEntityId(tenantId, customerId, getWhiteLabelingType(edgeEvent.getType()));
            if (whiteLabeling == null) {
                return null;
            }
            WhiteLabelingProto whiteLabelingProto = EdgeMsgConstructorUtils.constructWhiteLabeling(whiteLabeling);
            result = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .setWhiteLabelingProto(whiteLabelingProto)
                    .build();
        } catch (Exception e) {
            log.error("Can't process white labeling msg [{}]", edgeEvent, e);
        }
        return result;
    }

    private WhiteLabelingType getWhiteLabelingType(EdgeEventType type) {
        return switch (type) {
            case WHITE_LABELING -> WhiteLabelingType.GENERAL;
            case MAIL_TEMPLATES -> WhiteLabelingType.MAIL_TEMPLATES;
            case LOGIN_WHITE_LABELING -> WhiteLabelingType.LOGIN;
            default -> null;
        };
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(EdgeEventType.valueOf(edgeNotificationMsg.getEntityType()),
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId sourceEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        switch (entityId.getEntityType()) {
            case TENANT:
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                    PageDataIterable<TenantId> tenantIds = new PageDataIterable<>(link -> edgeCtx.getTenantService().findTenantsIds(link), 1024);
                    for (TenantId tenantId1 : tenantIds) {
                        futures.addAll(processActionForAllEdgesByTenantId(tenantId1, type, actionType, null, JacksonUtil.valueToTree(entityId), sourceEdgeId, null));
                    }
                } else {
                    futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, null, JacksonUtil.valueToTree(entityId), sourceEdgeId, null);
                }
                return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            case CUSTOMER:
                if (EdgeEventActionType.UPDATED.equals(actionType)) {
                    List<EdgeId> edgesByCustomerId =
                            edgeCtx.getCustomersHierarchyEdgeService().findAllEdgesInHierarchyByCustomerId(tenantId, new CustomerId(entityId.getId()));
                    if (edgesByCustomerId != null) {
                        for (EdgeId edgeId : edgesByCustomerId) {
                            saveEdgeEvent(tenantId, edgeId, type, actionType, null, JacksonUtil.valueToTree(entityId));
                        }
                    }
                }
                break;
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.WHITE_LABELING;
    }

}
