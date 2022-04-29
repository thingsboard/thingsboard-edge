/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class WidgetBundleCloudProcessor extends BaseCloudProcessor {

    public ListenableFuture<Void> processWidgetsBundleMsgFromCloud(TenantId tenantId, WidgetsBundleUpdateMsg widgetsBundleUpdateMsg) {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(new UUID(widgetsBundleUpdateMsg.getIdMSB(), widgetsBundleUpdateMsg.getIdLSB()));
        switch (widgetsBundleUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                widgetCreationLock.lock();
                try {
                    deleteSystemWidgetBundleIfAlreadyExists(tenantId, widgetsBundleUpdateMsg.getAlias(), widgetsBundleId);
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
                    if (widgetsBundleUpdateMsg.hasImage()) {
                        widgetsBundle.setImage(new String(widgetsBundleUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8));
                    }
                    if (widgetsBundleUpdateMsg.hasDescription()) {
                        widgetsBundle.setDescription(widgetsBundleUpdateMsg.getDescription());
                    }
                    widgetsBundleService.saveWidgetsBundle(widgetsBundle, false);

                    if (created) {
                        requestWidgetsBundleTypes(tenantId, widgetsBundleId);
                    }
                } finally {
                    widgetCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, widgetsBundleId);
                if (widgetsBundle != null) {
                    widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type " + widgetsBundleUpdateMsg.getMsgType()));
        }
        return Futures.immediateFuture(null);
    }

    private void requestWidgetsBundleTypes(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        saveCloudEvent(tenantId,
                CloudEventType.WIDGETS_BUNDLE,
                ActionType.WIDGET_BUNDLE_TYPES_REQUEST,
                widgetsBundleId,
                null);
    }

    private void deleteSystemWidgetBundleIfAlreadyExists(TenantId tenantId, String bundleAlias, WidgetsBundleId widgetsBundleId) {
        try {
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, bundleAlias);
            if (widgetsBundle != null && !widgetsBundleId.equals(widgetsBundle.getId())) {
                widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, widgetsBundle.getId());
                requestWidgetsBundleTypes(tenantId, widgetsBundleId);
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
            requestWidgetsBundleTypes(tenantId, widgetsBundleId);
        }
    }

    public UplinkMsg processWidgetBundleTypesRequestMsgToCloud(CloudEvent cloudEvent) {
        EntityId widgetBundleId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
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
