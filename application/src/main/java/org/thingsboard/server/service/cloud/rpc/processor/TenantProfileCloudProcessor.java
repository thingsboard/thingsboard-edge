/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;

@Slf4j
@Component
@TbCoreComponent
public class TenantProfileCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processTenantProfileMsgFromCloud(TenantId tenantId, TenantProfileUpdateMsg tenantProfileUpdateMsg) {
        TenantProfile tenantProfileMsg = JacksonUtil.fromString(tenantProfileUpdateMsg.getEntity(), TenantProfile.class, true);
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
                    TenantProfile tenantProfile = edgeCtx.getTenantProfileService().findTenantProfileById(tenantId, tenantProfileMsg.getId());
                    boolean isDefault = tenantProfileMsg.isDefault();
                    if (tenantProfile == null) {
                        tenantProfileMsg.setDefault(false);
                        edgeCtx.getTenantProfileService().saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfileMsg, false);
                    }
                    if (isDefault) {
                        edgeCtx.getTenantProfileService().setDefaultTenantProfile(TenantId.SYS_TENANT_ID, tenantProfileMsg.getId());
                        tenantProfileMsg.setDefault(true);
                    }

                    clearRateLimitsProfileConfiguration(tenantProfileMsg);
                    edgeCtx.getTenantProfileService().saveTenantProfile(tenantId, tenantProfileMsg, false);

                    if (removePreviousProfile) {
                        updateTenants(tenantProfileMsg.getId(), tenantProfileByName.getId());
                        edgeCtx.getTenantProfileService().deleteTenantProfile(tenantId, tenantProfileByName.getId());
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
        configuration.setCassandraReadQueryTenantCoreRateLimits(null);
        configuration.setCassandraWriteQueryTenantCoreRateLimits(null);
        configuration.setCassandraReadQueryTenantRuleEngineRateLimits(null);
        configuration.setCassandraWriteQueryTenantRuleEngineRateLimits(null);
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
                link -> edgeCtx.getTenantProfileService().findTenantProfiles(tenantId, link), 1000);

        for (TenantProfile tenantProfile : tenantProfiles) {
            if (tenantProfile.getName().equals(name)) {
                return tenantProfile;
            }
        }
        return null;
    }

    private void renamePreviousTenantProfile(TenantProfile tenantProfileByName) {
        tenantProfileByName.setName(tenantProfileByName.getName() + StringUtils.randomAlphanumeric(15));
        edgeCtx.getTenantProfileService().saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfileByName);
    }

    private void updateTenants(TenantProfileId newTenantProfileId, TenantProfileId oldTenantProfileId) {
        List<TenantId> tenantIdList = edgeCtx.getTenantService().findTenantIdsByTenantProfileId(oldTenantProfileId);
        PageDataIterable<Tenant> tenants = new PageDataIterable<>(link -> edgeCtx.getTenantService().findTenants(link), 1000);
        for (Tenant tenant : tenants) {
            if (tenantIdList.contains(tenant.getId())) {
                tenant.setTenantProfileId(newTenantProfileId);
                edgeCtx.getTenantService().saveTenant(tenant);
            }
        }
    }

}
