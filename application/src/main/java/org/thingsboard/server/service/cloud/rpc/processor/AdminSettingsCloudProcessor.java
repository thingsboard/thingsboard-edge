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
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AdminSettingsCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processAdminSettingsMsgFromCloud(TenantId tenantId, AdminSettingsUpdateMsg adminSettingsUpdateMsg) {
        AdminSettings adminSettingsMsg = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
        if (adminSettingsMsg == null) {
            throw new RuntimeException("[{" + tenantId + "}] adminSettingsUpdateMsg {" + adminSettingsUpdateMsg + " } cannot be converted to admin settings");
        }
        if (TenantId.SYS_TENANT_ID.equals(adminSettingsMsg.getTenantId())) {
            AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, adminSettingsMsg.getKey());
            if (adminSettings != null) {
                adminSettings.setJsonValue(adminSettingsMsg.getJsonValue());
                adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);
            } else {
                adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettingsMsg);
            }
        } else {
            List<AttributeKvEntry> attributes = new ArrayList<>();
            attributes.add(new BaseAttributeKvEntry(new StringDataEntry(adminSettingsMsg.getKey(), JacksonUtil.toString(adminSettingsMsg.getJsonValue())), System.currentTimeMillis()));
            attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes);
        }
        return Futures.immediateFuture(null);
    }

}
