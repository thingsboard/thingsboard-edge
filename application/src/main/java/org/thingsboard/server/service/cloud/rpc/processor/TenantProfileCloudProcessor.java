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
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
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

                    TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(tenantId, tenantProfileMsg, false);
                    notifyCluster(tenantId, savedTenantProfile);

                    if (removePreviousProfile) {
                        updateTenants(tenantProfileMsg.getId(), tenantProfileByName.getId());
                        tenantProfileService.deleteTenantProfile(tenantId, tenantProfileByName.getId());
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, tenantProfileByName.getId(), ComponentLifecycleEvent.DELETED);
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

    private void notifyCluster(TenantId tenantId, TenantProfile savedTenantProfile) {
        tbClusterService.onTenantProfileChange(savedTenantProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedTenantProfile.getId(), ComponentLifecycleEvent.UPDATED);
    }
}
