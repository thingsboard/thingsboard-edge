/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class AdminSettingsCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processAdminSettingsMsgFromCloud(TenantId tenantId, AdminSettingsUpdateMsg adminSettingsUpdateMsg) {
        AdminSettings adminSettingsMsg = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
        if (adminSettingsMsg == null) {
            throw new RuntimeException("[{" + tenantId + "}] adminSettingsUpdateMsg {" + adminSettingsUpdateMsg + " } cannot be converted to admin settings");
        }
        AdminSettings adminSettingsFromDb = edgeCtx.getAdminSettingsService().findAdminSettingsByKey(adminSettingsMsg.getTenantId(), adminSettingsMsg.getKey());
        if (adminSettingsFromDb != null && !adminSettingsFromDb.getId().equals(adminSettingsMsg.getId())) {
            edgeCtx.getAdminSettingsService().deleteAdminSettingsByTenantIdAndKey(adminSettingsMsg.getTenantId(), adminSettingsMsg.getKey());
        }
        edgeCtx.getAdminSettingsService().saveAdminSettings(adminSettingsMsg.getTenantId(), adminSettingsMsg);
        return Futures.immediateFuture(null);
    }

}
