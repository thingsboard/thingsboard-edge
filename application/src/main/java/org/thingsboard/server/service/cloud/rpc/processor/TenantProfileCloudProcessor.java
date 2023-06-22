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
        switch (tenantProfileUpdateMsg.getMsgType()) {
            case ENTITY_UPDATED_RPC_MESSAGE:
                PageDataIterable<TenantProfile> tenantProfiles = new PageDataIterable<>(
                        link -> tenantProfileService.findTenantProfiles(tenantId, link), DEFAULT_PAGE_SIZE);
                String name = tenantProfileUpdateMsg.getName();
                TenantProfile existingTenant = getExistingTenantWithName(tenantProfiles, name);
                TenantProfile newTenantProfile = tenantProfileService.findTenantProfileById(tenantId, tenantProfileId);
                if (newTenantProfile == null) {
                    newTenantProfile = createTenantProfile(tenantProfileId, existingTenant, name);
                    newTenantProfile = tenantProfileService.saveTenantProfile(tenantId, newTenantProfile, false);
                }
                newTenantProfile = replaceTenantProfileIfNotTheSame(tenantId, newTenantProfile, existingTenant, name);
                updateTenantProfileProperties(newTenantProfile, tenantProfileUpdateMsg);

                TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(tenantId, newTenantProfile, false);
                notifyCluster(tenantId, savedTenantProfile);

                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(tenantProfileUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }

    private void updateTenantProfileProperties(TenantProfile tenantProfile, TenantProfileUpdateMsg tenantProfileUpdateMsg) {
        tenantProfile.setName(tenantProfileUpdateMsg.getName());
        tenantProfile.setDefault(tenantProfileUpdateMsg.getDefault());
        tenantProfile.setDescription(tenantProfileUpdateMsg.getDescription());
        tenantProfile.setIsolatedTbRuleEngine(tenantProfileUpdateMsg.getIsolatedRuleChain());
        tenantProfile.setProfileDataBytes(tenantProfile.getProfileDataBytes());
        Optional<TenantProfileData> profileDataOpt =
                dataDecodingEncodingService.decode(tenantProfileUpdateMsg.getProfileDataBytes().toByteArray());
        tenantProfile.setProfileData(profileDataOpt.orElse(null));
    }

    private TenantProfile replaceTenantProfileIfNotTheSame(TenantId tenantId,
                                                           TenantProfile newTenantProfile,
                                                           TenantProfile existingTenantWithName,
                                                           String name) {
        if (existingTenantWithName != null) {
            if (existingTenantWithName.getId().equals(newTenantProfile.getId()) && existingTenantWithName.getName().equals(name)) {
                return existingTenantWithName;
            }

            if (existingTenantWithName.getName().equals(name)) {
                updateTenantsWithNewTenantProfile(existingTenantWithName.getId(), newTenantProfile.getId());
                deleteOldTenantProfile(tenantId, existingTenantWithName, newTenantProfile);
            }
        }

        return newTenantProfile;
    }

    private void deleteOldTenantProfile(TenantId tenantId, TenantProfile oldTenantProfile, TenantProfile newTenantProfile) {
        if (oldTenantProfile.isDefault()) {
            tenantProfileService.setDefaultTenantProfile(tenantId, newTenantProfile.getId());
        }
        tenantProfileService.deleteTenantProfile(tenantId, oldTenantProfile.getId());
    }

    private void updateTenantsWithNewTenantProfile(TenantProfileId oldTenantProfileId, TenantProfileId newTenantProfileId) {
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

    private TenantProfile createTenantProfile(TenantProfileId tenantProfileId, TenantProfile tenantExists, String name) {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setId(tenantProfileId);
        tenantProfile.setCreatedTime(Uuids.unixTimestamp(tenantProfileId.getId()));

        String profileName = tenantExists != null ? tenantProfile.getId().toString() : name;
        tenantProfile.setName(profileName);

        return tenantProfile;
    }

    private TenantProfile getExistingTenantWithName(PageDataIterable<TenantProfile> tenantProfiles, String desiredName) {
        for (TenantProfile tenantProfile : tenantProfiles) {
            if (tenantProfile.getName().equals(desiredName)) {
                return tenantProfile;
            }
        }
        return null;
    }
}
