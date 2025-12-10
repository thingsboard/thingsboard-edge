/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@TbCoreComponent
public class TenantCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    public void createTenantIfNotExists(TenantId tenantId) throws Exception {
        try {
            cloudSynchronizationManager.getSync().set(true);
            Tenant tenant = edgeCtx.getTenantService().findTenantById(tenantId);
            if (tenant != null) {
                return;
            }
            tenant = new Tenant();
            tenant.setTitle("Tenant");
            tenant.setId(tenantId);
            tenant.setCreatedTime(Uuids.unixTimestamp(tenantId.getId()));
            Tenant savedTenant = edgeCtx.getTenantService().saveTenant(tenant, null, false);
            edgeCtx.getTenantProfileCache().evict(savedTenant.getId());

            try {
                apiUsageStateService.createDefaultApiUsageState(savedTenant.getId(), null);
            } catch (Exception ignored) {}

            requestForAdditionalData(tenantId, tenantId).get();
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    public ListenableFuture<Void> processTenantMsgFromCloud(TenantUpdateMsg tenantUpdateMsg) {
        Tenant tenant = JacksonUtil.fromString(tenantUpdateMsg.getEntity(), Tenant.class, true);
        if (tenant == null) {
            throw new RuntimeException("[{" + TenantId.SYS_TENANT_ID + "}] tenantUpdateMsg {" + tenantUpdateMsg + "} cannot be converted to tenant");
        }
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (tenantUpdateMsg.getMsgType()) {
                case ENTITY_UPDATED_RPC_MESSAGE:
                    edgeCtx.getTenantService().saveTenant(tenant, null, false);
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(tenantUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    public void cleanUp() {
        try {
            cloudSynchronizationManager.getSync().set(true);
            log.debug("Starting clean up procedure");
            PageData<Tenant> tenants = edgeCtx.getTenantService().findTenants(new PageLink(Integer.MAX_VALUE));
            for (Tenant tenant : tenants.getData()) {
                removeTenantAttributes(tenant.getId());
                edgeCtx.getTenantService().deleteTenant(tenant.getId());
            }

            cleanUpSystemTenant();
            log.debug("Clean up procedure successfully finished!");
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void cleanUpSystemTenant() {
        edgeCtx.getAdminSettingsService().deleteByTenantId(TenantId.SYS_TENANT_ID);
        edgeCtx.getQueueService().deleteQueuesByTenantId(TenantId.SYS_TENANT_ID);
        edgeCtx.getWidgetTypeService().deleteWidgetTypesByTenantId(TenantId.SYS_TENANT_ID);
        edgeCtx.getWidgetsBundleService().deleteWidgetsBundlesByTenantId(TenantId.SYS_TENANT_ID);
        removeTenantAttributes(TenantId.SYS_TENANT_ID);
    }

    private void removeTenantAttributes(TenantId tenantId) {
        try {
            List<AttributeKvEntry> attributeKvEntries = edgeCtx.getAttributesService().findAll(tenantId, tenantId, AttributeScope.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            edgeCtx.getAttributesService().removeAll(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attrKeys);
        } catch (Exception e) {
            log.error("Unable to remove tenant attributes", e);
        }
    }

}
