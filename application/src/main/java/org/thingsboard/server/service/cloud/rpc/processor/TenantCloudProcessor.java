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

    public void createTenantIfNotExists(TenantId tenantId, Long queueStartTs) throws Exception {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (tenant != null) {
            return;
        }
        tenant = new Tenant();
        tenant.setTitle("Tenant");
        tenant.setId(tenantId);
        tenant.setCreatedTime(Uuids.unixTimestamp(tenantId.getId()));
        Tenant savedTenant = tenantService.saveTenant(tenant, false);
        apiUsageStateService.createDefaultApiUsageState(savedTenant.getId(), null);

        requestForAdditionalData(tenantId, tenantId, queueStartTs).get();
    }

    public ListenableFuture<Void> processTenantMsgFromCloud(TenantUpdateMsg tenantUpdateMsg) {
        TenantId entityId = new TenantId(new UUID(tenantUpdateMsg.getIdMSB(), tenantUpdateMsg.getIdLSB()));
        TenantProfileId tenantProfileId = new TenantProfileId(new UUID(tenantUpdateMsg.getProfileIdMSB(), tenantUpdateMsg.getProfileIdLSB()));
        switch (tenantUpdateMsg.getMsgType()) {
            case ENTITY_UPDATED_RPC_MESSAGE:
                processTenantUpdatedMessage(entityId, tenantUpdateMsg, tenantProfileId);
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(tenantUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }

    private void processTenantUpdatedMessage(TenantId entityId, TenantUpdateMsg tenantUpdateMsg, TenantProfileId tenantProfileId) {
        Tenant tenant = tenantService.findTenantById(entityId);
        if (tenant == null) {
            tenant = new Tenant();
            tenant.setId(entityId);
            tenant.setCreatedTime(Uuids.unixTimestamp(entityId.getId()));
        }
        updateTenantProperties(tenant, tenantUpdateMsg, tenantProfileId);
        Tenant savedTenant = tenantService.saveTenant(tenant, false);
        notifyCluster(entityId, savedTenant);
    }

    public void cleanUp() {
        log.debug("Starting clean up procedure");
        PageData<Tenant> tenants = tenantService.findTenants(new PageLink(Integer.MAX_VALUE));
        for (Tenant tenant : tenants.getData()) {
            removeTenantAttributes(tenant.getId());
            tenantService.deleteTenant(tenant.getId());
        }

        cleanUpSystemTenant();
        log.debug("Clean up procedure successfully finished!");
    }

    private void cleanUpSystemTenant() {
        adminSettingsService.deleteAdminSettingsByTenantId(TenantId.SYS_TENANT_ID);
        queueService.deleteQueuesByTenantId(TenantId.SYS_TENANT_ID);
        widgetTypeService.deleteWidgetTypesByTenantId(TenantId.SYS_TENANT_ID);
        widgetsBundleService.deleteWidgetsBundlesByTenantId(TenantId.SYS_TENANT_ID);
        removeTenantAttributes(TenantId.SYS_TENANT_ID);
    }

    private void removeTenantAttributes(TenantId tenantId) {
        try {
            List<AttributeKvEntry> attributeKvEntries =
                    attributesService.findAll(tenantId, tenantId, DataConstants.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            attributesService.removeAll(tenantId, tenantId, DataConstants.SERVER_SCOPE, attrKeys);
        } catch (Exception e) {
            log.error("Unable to remove tenant attributes", e);
        }
    }

    private void notifyCluster(TenantId tenantId, Tenant savedTenant) {
        tbClusterService.onTenantChange(savedTenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedTenant.getId(), ComponentLifecycleEvent.UPDATED);
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
