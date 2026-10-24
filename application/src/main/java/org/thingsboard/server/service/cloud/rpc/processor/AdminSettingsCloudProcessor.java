// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
