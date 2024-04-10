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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;

@Component
@Slf4j
public class TenantProfileCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processTenantProfileMsgFromCloud(TenantId tenantId, TenantProfileUpdateMsg tenantProfileUpdateMsg) {
        TenantProfile tenantProfileMsg  = JacksonUtil.fromString(tenantProfileUpdateMsg.getEntity(), TenantProfile.class, true);
        if (tenantProfileMsg == null) {
            throw new RuntimeException("[{" + tenantId + "}] tenantProfileUpdateMsg {" + tenantProfileUpdateMsg + "} cannot be converted to tenant profile");
        }
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (tenantProfileUpdateMsg.getMsgType()) {
                case ENTITY_UPDATED_RPC_MESSAGE:
                    String tenantProfileMsgName = tenantProfileMsg.getName();
                    TenantProfile tenantProfileByName = findTenantProfileByName(tenantId, tenantProfileMsgName);
                    boolean removePreviousProfile = false;
                    if (tenantProfileByName != null && !tenantProfileByName.getId().equals(tenantProfileMsg.getId())) {
                        renamePreviousTenantProfile(tenantProfileByName);
                        removePreviousProfile = true;
                    }
                    TenantProfile tenantProfile = tenantProfileService.findTenantProfileById(tenantId, tenantProfileMsg.getId());
                    boolean isDefault = tenantProfileMsg.isDefault();
                    if (tenantProfile == null) {
                        tenantProfileMsg.setDefault(false);
                        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfileMsg, false);
                    }
                    if (isDefault) {
                        tenantProfileService.setDefaultTenantProfile(TenantId.SYS_TENANT_ID, tenantProfileMsg.getId());
                        tenantProfileMsg.setDefault(true);
                    }

                    clearRateLimitsProfileConfiguration(tenantProfileMsg);
                    tenantProfileService.saveTenantProfile(tenantId, tenantProfileMsg, false);

                    if (removePreviousProfile) {
                        updateTenants(tenantProfileMsg.getId(), tenantProfileByName.getId());
                        tenantProfileService.deleteTenantProfile(tenantId, tenantProfileByName.getId());
                    }

                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(tenantProfileUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private void clearRateLimitsProfileConfiguration(TenantProfile tenantProfile) {
        DefaultTenantProfileConfiguration configuration =
                (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();

        // Clear all rate limit related configurations by setting them to null
        configuration.setTransportTenantMsgRateLimit(null);
        configuration.setTransportDeviceMsgRateLimit(null);
        configuration.setTransportTenantTelemetryMsgRateLimit(null);
        configuration.setTransportDeviceTelemetryMsgRateLimit(null);

        configuration.setTransportTenantTelemetryDataPointsRateLimit(null);
        configuration.setTransportDeviceTelemetryDataPointsRateLimit(null);
        configuration.setTenantServerRestLimitsConfiguration(null);
        configuration.setCustomerServerRestLimitsConfiguration(null);
        configuration.setTenantEntityExportRateLimit(null);
        configuration.setTenantEntityImportRateLimit(null);
        configuration.setWsUpdatesPerSessionRateLimit(null);
        configuration.setCassandraQueryTenantRateLimitsConfiguration(null);
        configuration.setTenantNotificationRequestsRateLimit(null);
        configuration.setTenantNotificationRequestsPerRuleRateLimit(null);
        configuration.setEdgeEventRateLimits(null);
        configuration.setEdgeEventRateLimitsPerEdge(null);

        configuration.setRpcTtlDays(0);
        configuration.setMaxJSExecutions(0);
        configuration.setMaxREExecutions(0);
        configuration.setMaxDPStorageDays(0);
        configuration.setMaxTbelExecutions(0);
        configuration.setQueueStatsTtlDays(0);
        configuration.setMaxTransportMessages(0);
        configuration.setDefaultStorageTtlDays(0);
        configuration.setMaxTransportDataPoints(0);
        configuration.setRuleEngineExceptionsTtlDays(0);
        configuration.setMaxRuleNodeExecutionsPerMessage(0);

        tenantProfile.getProfileData().setConfiguration(configuration);
    }

    private TenantProfile findTenantProfileByName(TenantId tenantId, String name) {
        PageDataIterable<TenantProfile> tenantProfiles = new PageDataIterable<>(
                link -> tenantProfileService.findTenantProfiles(tenantId, link), DEFAULT_PAGE_SIZE);

        for (TenantProfile tenantProfile : tenantProfiles) {
            if (tenantProfile.getName().equals(name)) {
                return tenantProfile;
            }
        }
        return null;
    }

    private void renamePreviousTenantProfile(TenantProfile tenantProfileByName) {
        tenantProfileByName.setName(tenantProfileByName.getName() + StringUtils.randomAlphanumeric(15));
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfileByName);
    }

    private void updateTenants(TenantProfileId newTenantProfileId, TenantProfileId oldTenantProfileId) {
        List<TenantId> tenantIdList = tenantService.findTenantIdsByTenantProfileId(oldTenantProfileId);
        PageDataIterable<Tenant> tenants = new PageDataIterable<>(link -> tenantService.findTenants(link), DEFAULT_PAGE_SIZE);
        for (Tenant tenant : tenants) {
            if (tenantIdList.contains(tenant.getId())) {
                tenant.setTenantProfileId(newTenantProfileId);
                tenantService.saveTenant(tenant);
            }
        }
    }

}
