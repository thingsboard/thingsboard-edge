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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class TenantProfileCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    public ListenableFuture<Void> processTenantProfileMsgFromCloud(TenantId tenantId, TenantProfileUpdateMsg tenantProfileUpdateMsg) {
        TenantProfileId tenantProfileId = new TenantProfileId(new UUID(tenantProfileUpdateMsg.getIdMSB(), tenantProfileUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getSync().set(true);

            switch (tenantProfileUpdateMsg.getMsgType()) {
                case ENTITY_UPDATED_RPC_MESSAGE:
                    String tenantProfileMsgName = tenantProfileUpdateMsg.getName();
                    TenantProfile tenantProfileByName = findTenantProfileByName(tenantId, tenantProfileMsgName);
                    boolean removePreviousProfile = false;
                    if (tenantProfileByName != null && !tenantProfileByName.getId().equals(tenantProfileId)) {
                        renamePreviousTenantProfile(tenantProfileByName);
                        removePreviousProfile = true;
                    }
                    TenantProfile tenantProfile = tenantProfileService.findTenantProfileById(tenantId, tenantProfileId);
                    if (tenantProfile == null) {
                        tenantProfile = createTenantProfile(tenantProfileId, tenantProfileMsgName, tenantProfileByName != null && tenantProfileByName.isDefault());
                    }
                    if (!tenantProfile.isDefault() && tenantProfileUpdateMsg.getDefault()) {
                        tenantProfileService.setDefaultTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile.getId());
                    }
                    tenantProfile.setName(tenantProfileMsgName);
                    tenantProfile.setDefault(tenantProfileUpdateMsg.getDefault());
                    tenantProfile.setDescription(tenantProfileUpdateMsg.getDescription());
                    tenantProfile.setIsolatedTbRuleEngine(tenantProfileUpdateMsg.getIsolatedRuleChain());
                    tenantProfile.setProfileDataBytes(tenantProfile.getProfileDataBytes());
                    Optional<TenantProfileData> profileDataOpt =
                            dataDecodingEncodingService.decode(tenantProfileUpdateMsg.getProfileDataBytes().toByteArray());
                    tenantProfile.setProfileData(profileDataOpt.orElse(null));

                    TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(tenantId, tenantProfile, false);
                    notifyCluster(tenantId, savedTenantProfile);

                    if (removePreviousProfile) {
                        updateTenants(tenantProfileId, tenantProfileByName.getId());
                        tenantProfileService.deleteTenantProfile(tenantId, tenantProfileByName.getId());
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, tenantProfileByName.getId(), ComponentLifecycleEvent.DELETED);
                    }

                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(tenantProfileUpdateMsg.getMsgType());
            }
        } finally {
            edgeSynchronizationManager.getSync().remove();
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

    private TenantProfile createTenantProfile(TenantProfileId tenantProfileId, String name, boolean isDefaultPreviousProfile) {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setId(tenantProfileId);
        tenantProfile.setCreatedTime(Uuids.unixTimestamp(tenantProfileId.getId()));
        tenantProfile.setName(name);
        return tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile, false);
    }

    private void notifyCluster(TenantId tenantId, TenantProfile savedTenantProfile) {
        tbClusterService.onTenantProfileChange(savedTenantProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedTenantProfile.getId(), ComponentLifecycleEvent.UPDATED);
    }
}
