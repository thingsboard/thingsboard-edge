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
package org.thingsboard.server.service.edge.rpc.processor.translation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.gen.edge.v1.CustomTranslationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
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
public class CustomTranslationEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        DownlinkMsg result = null;
        try {
            CustomTranslation customTranslation = JacksonUtil.treeToValue(edgeEvent.getBody(), CustomTranslation.class);
            if (customTranslation == null) {
                return null;
            }
            UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            CustomTranslationUpdateMsg customTranslationMsg = EdgeMsgConstructorUtils.constructCustomTranslationMsg(msgType, customTranslation);
            result = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .setCustomTranslationUpdateMsg(customTranslationMsg)
                    .build();
        } catch (Exception e) {
            log.error("Can't process custom translation msg [{}]", edgeEvent, e);
        }
        return result;
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(EdgeEventType.valueOf(edgeNotificationMsg.getEntityType()),
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId sourceEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        switch (entityId.getEntityType()) {
            case TENANT -> {
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                    PageLink pageLink = new PageLink(1000);
                    PageData<TenantId> tenantsIds;
                    do {
                        tenantsIds = edgeCtx.getTenantService().findTenantsIds(pageLink);
                        for (TenantId tenantId1 : tenantsIds.getData()) {
                            futures.addAll(processActionForAllEdgesByTenantId(tenantId1, type, actionType, entityId,
                                    JacksonUtil.toJsonNode(edgeNotificationMsg.getBody()), sourceEdgeId, null));
                        }
                        pageLink = pageLink.nextPageLink();
                    } while (tenantsIds.hasNext());
                } else {
                    futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, entityId,
                            JacksonUtil.toJsonNode(edgeNotificationMsg.getBody()), sourceEdgeId, null);
                }
                return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            }
            case CUSTOMER -> {
                List<EdgeId> edgesByCustomerId = edgeCtx.getCustomersHierarchyEdgeService().findAllEdgesInHierarchyByCustomerId(tenantId, new CustomerId(entityId.getId()));
                if (edgesByCustomerId != null) {
                    for (EdgeId edgeId : edgesByCustomerId) {
                        saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, JacksonUtil.toJsonNode(edgeNotificationMsg.getBody()));
                    }
                }
            }
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.CUSTOM_TRANSLATION;
    }

}
