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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TenantCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private TenantProfileCloudProcessor tenantProfileCloudProcessor;

    public void createTenantIfNotExists(TenantId tenantId, Long queueStartTs) throws Exception {
        try {
            edgeSynchronizationManager.getSync().set(true);
            Tenant tenant = tenantService.findTenantById(tenantId);
            if (tenant != null) {
                return;
            }
            tenant = new Tenant();
            tenant.setTitle("Tenant");
            // TODO: voba - add validation once tenant profile are propagated to edge from cloud
            // tenantValidator.validate(tenant, Tenant::getId);
            tenant.setId(tenantId);
            tenant.setCreatedTime(Uuids.unixTimestamp(tenantId.getId()));
            Tenant savedTenant = tenantService.saveTenant(tenant, false);
            apiUsageStateService.createDefaultApiUsageState(savedTenant.getId(), null);

            requestForAdditionalData(tenantId, tenantId, queueStartTs).get();
        } finally {
            edgeSynchronizationManager.getSync().remove();
        }
    }

    public ListenableFuture<Void> processTenantMsgFromCloud(TenantUpdateMsg tenantUpdateMsg) {
        TenantId entityId = new TenantId(new UUID(tenantUpdateMsg.getIdMSB(), tenantUpdateMsg.getIdLSB()));
        TenantProfileId tenantProfileId = new TenantProfileId(new UUID(tenantUpdateMsg.getProfileIdMSB(), tenantUpdateMsg.getProfileIdLSB()));
        switch (tenantUpdateMsg.getMsgType()) {
            case ENTITY_UPDATED_RPC_MESSAGE:
                processTenantUpdatedRpcMessage(entityId, tenantUpdateMsg, tenantProfileId);
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(tenantUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }

    private void processTenantUpdatedRpcMessage(TenantId entityId, TenantUpdateMsg tenantUpdateMsg, TenantProfileId tenantProfileId) {
        Tenant tenant = tenantService.findTenantById(entityId);
        if (tenant == null) {
            tenant = createTenant(entityId);
        }

        updateTenantProperties(tenant, tenantUpdateMsg, tenantProfileId);
        Tenant savedTenant = tenantService.saveTenant(tenant, false);
        notifyCluster(entityId, savedTenant);
    }

    public void cleanUp() {
        try {
            edgeSynchronizationManager.getSync().set(true);
            log.debug("Starting clean up procedure");
            PageData<Tenant> tenants = tenantService.findTenants(new PageLink(Integer.MAX_VALUE));
            for (Tenant tenant : tenants.getData()) {
                tenantService.deleteTenant(tenant.getId());
            }

            cleanUpSystemTenant();
            log.debug("Clean up procedure successfully finished!");
        } finally {
            edgeSynchronizationManager.getSync().remove();
        }
    }

    private void cleanUpSystemTenant() {
        try {
            adminSettingsService.deleteAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mailTemplates");
            adminSettingsService.deleteAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
            queueService.deleteQueuesByTenantId(TenantId.SYS_TENANT_ID);
            widgetsBundleService.deleteWidgetsBundlesByTenantId(TenantId.SYS_TENANT_ID);
            List<AttributeKvEntry> attributeKvEntries =
                    attributesService.findAll(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, DataConstants.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            attributesService.removeAll(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, DataConstants.SERVER_SCOPE, attrKeys);
        } catch (Exception e) {
            log.error("Unable to clean up sysadmin tenant", e);
        }
    }

    private void notifyCluster(TenantId tenantId, Tenant savedTenant) {
        tbClusterService.onTenantChange(savedTenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedTenant.getId(), ComponentLifecycleEvent.UPDATED);
    }

    private Tenant createTenant(TenantId tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setCreatedTime(Uuids.unixTimestamp(tenantId.getId()));
        return tenant;
    }

    private void updateTenantProperties(Tenant tenant, TenantUpdateMsg tenantUpdateMsg, TenantProfileId tenantProfileId) {
        tenant.setTitle(tenantUpdateMsg.getTitle());
        tenant.setTenantProfileId(tenantProfileId);
        tenant.setRegion(tenantUpdateMsg.getRegion());

        if (tenantUpdateMsg.hasCountry()) {
            tenant.setCountry(tenantUpdateMsg.getCountry());
        }

        if (tenantUpdateMsg.hasState()) {
            tenant.setState(tenantUpdateMsg.getState());
        }

        if (tenantUpdateMsg.hasCity()) {
            tenant.setCity(tenantUpdateMsg.getCity());
        }

        if (tenantUpdateMsg.hasZip()) {
            tenant.setZip(tenantUpdateMsg.getZip());
        }

        if (tenantUpdateMsg.hasAddress()) {
            tenant.setAddress(tenantUpdateMsg.getAddress());
        }

        if (tenantUpdateMsg.hasAddress2()) {
            tenant.setAddress2(tenantUpdateMsg.getAddress2());
        }

        if (tenantUpdateMsg.hasPhone()) {
            tenant.setPhone(tenantUpdateMsg.getPhone());
        }

        if (tenantUpdateMsg.hasEmail()) {
            tenant.setEmail(tenantUpdateMsg.getEmail());
        }

        if (tenantUpdateMsg.hasAdditionalInfo()) {
            tenant.setAdditionalInfo(JacksonUtil.toJsonNode(tenantUpdateMsg.getAdditionalInfo()));
        }
    }

}
