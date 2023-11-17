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
package org.thingsboard.server.service.edge.rpc.processor.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class AdminSettingsEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertAdminSettingsEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) throws Exception {
        AdminSettings adminSettings = null;
        JsonNode body = edgeEvent.getBody();
        boolean isSysadmin = body.has("sysadmin");
        EntityId entityId = body.has("customerId") ? new CustomerId(UUID.fromString(body.get("customerId").asText())) : edgeEvent.getTenantId();
        String key = JacksonUtil.convertValue(body.get("key"), String.class);
        if (isSysadmin) {
            adminSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key);
        } else {
            Optional<AttributeKvEntry> tenantSettingsAttr = attributesService.find(edgeEvent.getTenantId(), entityId, DataConstants.SERVER_SCOPE, key).get();
            if (tenantSettingsAttr.isPresent()) {
                adminSettings = new AdminSettings();
                adminSettings.setTenantId(edgeEvent.getTenantId());
                adminSettings.setKey(key);
                String value = tenantSettingsAttr.get().getValueAsString();
                adminSettings.setJsonValue(JacksonUtil.toJsonNode(value));
            }
        }
        if (adminSettings == null) {
            return null;
        }
        AdminSettingsUpdateMsg adminSettingsUpdateMsg = adminSettingsMsgConstructor.constructAdminSettingsUpdateMsg(adminSettings, entityId, edgeVersion);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addAdminSettingsUpdateMsg(adminSettingsUpdateMsg)
                .build();
    }

    public ListenableFuture<Void> processNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(EdgeEventType.valueOf(edgeNotificationMsg.getEntityType()),
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId sourceEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        switch (entityId.getEntityType()) {
            case TENANT:
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                    PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                    PageData<TenantId> tenantsIds;
                    do {
                        tenantsIds = tenantService.findTenantsIds(pageLink);
                        for (TenantId tenantId1 : tenantsIds.getData()) {
                            ObjectNode body = JacksonUtil.newObjectNode().put("sysadmin", true).put("key", edgeNotificationMsg.getBody());
                            futures.addAll(processActionForAllEdgesByTenantId(tenantId1, type, actionType, null, body, sourceEdgeId, null));
                        }
                        pageLink = pageLink.nextPageLink();
                    } while (tenantsIds.hasNext());
                } else {
                    futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, null, JacksonUtil.valueToTree(edgeNotificationMsg.getBody()), sourceEdgeId, null);
                }
                return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            case CUSTOMER:
                if (EdgeEventActionType.UPDATED.equals(actionType)) {
                    CustomerId customerId = new CustomerId(entityId.getId());
                    List<EdgeId> edgesByCustomerId =
                            customersHierarchyEdgeService.findAllEdgesInHierarchyByCustomerId(tenantId, customerId);
                    if (edgesByCustomerId != null) {
                        for (EdgeId edgeId : edgesByCustomerId) {
                            ObjectNode body = JacksonUtil.newObjectNode().put("customerId", customerId.toString()).put("key", edgeNotificationMsg.getBody());
                            saveEdgeEvent(tenantId, edgeId, type, actionType, null, body);
                        }
                    }
                }
                break;
        }
        return Futures.immediateFuture(null);
    }
}
