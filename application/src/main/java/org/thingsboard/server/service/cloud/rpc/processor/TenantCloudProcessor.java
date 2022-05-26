/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TenantCloudProcessor extends BaseCloudProcessor {

    public Tenant getOrCreateTenant(TenantId tenantId) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (tenant != null) {
            return tenant;
        }
        tenant = new Tenant();
        tenant.setTitle("Tenant");
        tenantValidator.validate(tenant, Tenant::getId);
        tenant.setId(tenantId);
        Tenant savedTenant = tenantService.saveTenant(tenant, false);
        apiUsageStateService.createDefaultApiUsageState(savedTenant.getId(), null);
        return savedTenant;
    }

    public void cleanUp() {
        log.debug("Starting clean up procedure");
        PageData<Tenant> tenants = tenantService.findTenants(new PageLink(Integer.MAX_VALUE));
        for (Tenant tenant : tenants.getData()) {
            cleanUpTenant(tenant);
        }

        Tenant systemTenant = new Tenant();
        systemTenant.setId(TenantId.SYS_TENANT_ID);
        systemTenant.setTitle("System");
        cleanUpTenant(systemTenant);

        log.debug("Clean up procedure successfully finished!");
    }

    private void cleanUpTenant(Tenant tenant) {
        log.debug("Removing entities for the tenant [{}][{}]", tenant.getTitle(), tenant.getId());
        userService.deleteTenantAdmins(tenant.getId());
        PageData<Customer> customers = customerService.findCustomersByTenantId(tenant.getId(), new PageLink(Integer.MAX_VALUE));
        if (customers != null && customers.getData() != null && !customers.getData().isEmpty()) {
            for (Customer customer : customers.getData()) {
                userService.deleteCustomerUsers(tenant.getId(), customer.getId());
            }
        }
        ruleChainService.deleteRuleChainsByTenantId(tenant.getId());
        entityViewService.deleteEntityViewsByTenantId(tenant.getId());
        deviceService.deleteDevicesByTenantId(tenant.getId());
        deviceProfileService.deleteDeviceProfilesByTenantId(tenant.getId());
        assetService.deleteAssetsByTenantId(tenant.getId());
        dashboardService.deleteDashboardsByTenantId(tenant.getId());
        adminSettingsService.deleteAdminSettingsByKey(tenant.getId(), "mail");
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenant.getId());
        cloudEventService.deleteCloudEventsByTenantId(tenant.getId());
        try {
            List<AttributeKvEntry> attributeKvEntries = attributesService.findAll(tenant.getId(), tenant.getId(), DataConstants.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            attributesService.removeAll(tenant.getId(), tenant.getId(), DataConstants.SERVER_SCOPE, attrKeys);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Unable to delete entity groups", e);
        }
    }

}
