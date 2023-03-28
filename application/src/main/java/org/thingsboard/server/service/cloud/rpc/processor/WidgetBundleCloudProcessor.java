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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class WidgetBundleCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processWidgetsBundleMsgFromCloud(TenantId tenantId,
                                                                   WidgetsBundleUpdateMsg widgetsBundleUpdateMsg,
                                                                   Long queueStartTs) throws ExecutionException, InterruptedException {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(new UUID(widgetsBundleUpdateMsg.getIdMSB(), widgetsBundleUpdateMsg.getIdLSB()));
        switch (widgetsBundleUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                widgetCreationLock.lock();
                try {
                    deleteSystemWidgetBundleIfAlreadyExists(tenantId, widgetsBundleUpdateMsg.getAlias(), widgetsBundleId, queueStartTs);
                    WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, widgetsBundleId);
                    boolean created = false;
                    if (widgetsBundle == null) {
                        created = true;
                        widgetsBundle = new WidgetsBundle();
                        if (widgetsBundleUpdateMsg.getIsSystem()) {
                            widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
                        } else {
                            widgetsBundle.setTenantId(tenantId);
                        }
                        widgetsBundle.setId(widgetsBundleId);
                        widgetsBundle.setCreatedTime(Uuids.unixTimestamp(widgetsBundleId.getId()));
                    }
                    widgetsBundle.setTitle(widgetsBundleUpdateMsg.getTitle());
                    widgetsBundle.setAlias(widgetsBundleUpdateMsg.getAlias());
                    widgetsBundle.setImage(widgetsBundleUpdateMsg.hasImage()
                            ? new String(widgetsBundleUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);
                    widgetsBundle.setDescription(widgetsBundleUpdateMsg.hasDescription() ? widgetsBundleUpdateMsg.getDescription() : null);
                    widgetsBundleService.saveWidgetsBundle(widgetsBundle, false);

                    if (created) {
                        return requestWidgetsBundleTypes(tenantId, widgetsBundleId, queueStartTs);
                    } else {
                        return Futures.immediateFuture(null);
                    }
                } finally {
                    widgetCreationLock.unlock();
                }
            case ENTITY_DELETED_RPC_MESSAGE:
                WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, widgetsBundleId);
                if (widgetsBundle != null) {
                    widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(widgetsBundleUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> requestWidgetsBundleTypes(TenantId tenantId, WidgetsBundleId widgetsBundleId, Long queueStartTs) {
        return cloudEventService.saveCloudEventAsync(tenantId,
                CloudEventType.WIDGETS_BUNDLE,
                EdgeEventActionType.WIDGET_BUNDLE_TYPES_REQUEST,
                widgetsBundleId,
                null,
                null,
                queueStartTs);
    }

    private void deleteSystemWidgetBundleIfAlreadyExists(TenantId tenantId, String bundleAlias, WidgetsBundleId widgetsBundleId, Long queueStartTs) throws ExecutionException, InterruptedException {
        try {
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, bundleAlias);
            if (widgetsBundle != null && !widgetsBundleId.equals(widgetsBundle.getId())) {
                widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, widgetsBundle.getId());
                requestWidgetsBundleTypes(tenantId, widgetsBundleId, queueStartTs).get();
            }
        } catch (IncorrectResultSizeDataAccessException e) {
            // fix for duplicate entries of system widgets
            List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(TenantId.SYS_TENANT_ID);
            for (WidgetsBundle systemWidgetsBundle : systemWidgetsBundles) {
                if (systemWidgetsBundle.getAlias().equals(bundleAlias) &&
                        !systemWidgetsBundle.getId().equals(widgetsBundleId)) {
                    widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, systemWidgetsBundle.getId());
                }
            }
            log.warn("Duplicate widgets bundle found, alias {}. Removed all duplicates!", bundleAlias);
            requestWidgetsBundleTypes(tenantId, widgetsBundleId, queueStartTs).get();
        }
    }

    public UplinkMsg convertWidgetBundleTypesRequestEventToUplink(CloudEvent cloudEvent) {
        EntityId widgetBundleId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg = WidgetBundleTypesRequestMsg.newBuilder()
                .setWidgetBundleIdMSB(widgetBundleId.getId().getMostSignificantBits())
                .setWidgetBundleIdLSB(widgetBundleId.getId().getLeastSignificantBits())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addWidgetBundleTypesRequestMsg(widgetBundleTypesRequestMsg);
        return builder.build();
    }
}
