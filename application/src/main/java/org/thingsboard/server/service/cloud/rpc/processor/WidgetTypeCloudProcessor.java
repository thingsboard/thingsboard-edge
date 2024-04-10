/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (widgetTypeUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    widgetCreationLock.lock();
                    try {
                        WidgetTypeDetails widgetTypeDetails = JacksonUtil.fromString(widgetTypeUpdateMsg.getEntity(), WidgetTypeDetails.class, true);
                        if (widgetTypeDetails == null) {
                            throw new RuntimeException("[{" + tenantId + "}] widgetTypeUpdateMsg {" + widgetTypeUpdateMsg + "} cannot be converted to widget type");
                        }
                        WidgetType widgetTypeByTenantIdAndFqn = widgetTypeService.findWidgetTypeByTenantIdAndFqn(widgetTypeDetails.getTenantId(), widgetTypeDetails.getFqn());
                        if (widgetTypeByTenantIdAndFqn != null && !widgetTypeByTenantIdAndFqn.getId().equals(widgetTypeDetails.getId())) {
                            widgetTypeService.deleteWidgetType(widgetTypeByTenantIdAndFqn.getTenantId(), widgetTypeByTenantIdAndFqn.getId());
                        }
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
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

}
