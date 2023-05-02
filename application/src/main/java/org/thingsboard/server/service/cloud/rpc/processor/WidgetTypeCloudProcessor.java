/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Component
@Slf4j
public class WidgetTypeCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processWidgetTypeMsgFromCloud(TenantId tenantId, WidgetTypeUpdateMsg widgetTypeUpdateMsg) {
        WidgetTypeId widgetTypeId = new WidgetTypeId(new UUID(widgetTypeUpdateMsg.getIdMSB(), widgetTypeUpdateMsg.getIdLSB()));
        switch (widgetTypeUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                widgetCreationLock.lock();
                try {
                    WidgetTypeDetails widgetTypeDetails = widgetTypeService.findWidgetTypeDetailsById(tenantId, widgetTypeId);
                    if (widgetTypeDetails == null) {
                        widgetTypeDetails = new WidgetTypeDetails();
                        if (widgetTypeUpdateMsg.getIsSystem()) {
                            widgetTypeDetails.setTenantId(TenantId.SYS_TENANT_ID);
                        } else {
                            widgetTypeDetails.setTenantId(tenantId);
                        }
                        widgetTypeDetails.setId(widgetTypeId);
                        widgetTypeDetails.setCreatedTime(Uuids.unixTimestamp(widgetTypeId.getId()));
                    }
                    widgetTypeDetails.setBundleAlias(widgetTypeUpdateMsg.hasBundleAlias() ? widgetTypeUpdateMsg.getBundleAlias() : null);
                    widgetTypeDetails.setAlias(widgetTypeUpdateMsg.hasAlias() ? widgetTypeUpdateMsg.getAlias() : null);
                    widgetTypeDetails.setName(widgetTypeUpdateMsg.hasName() ? widgetTypeUpdateMsg.getName() : null);
                    widgetTypeDetails.setDescriptor(widgetTypeUpdateMsg.hasDescriptorJson() ? JacksonUtil.toJsonNode(widgetTypeUpdateMsg.getDescriptorJson()) : null);
                    widgetTypeDetails.setImage(widgetTypeUpdateMsg.hasImage() ? widgetTypeUpdateMsg.getImage() : null);
                    widgetTypeDetails.setDescription(widgetTypeUpdateMsg.hasDescription() ? widgetTypeUpdateMsg.getDescription() : null);
                    widgetTypeService.saveWidgetType(widgetTypeDetails, false);
                } finally {
                    widgetCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                WidgetType widgetType = widgetTypeService.findWidgetTypeById(tenantId, widgetTypeId);
                if (widgetType != null) {
                    widgetTypeService.deleteWidgetType(tenantId, widgetType.getId());
                }
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(widgetTypeUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }
}
