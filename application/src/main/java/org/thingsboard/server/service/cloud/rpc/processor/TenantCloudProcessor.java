package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

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

        entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.CUSTOMER);
        entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.ASSET);
        entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.DEVICE);
        entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.ENTITY_VIEW);
        entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.EDGE);
        entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.DASHBOARD);
        entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.USER);

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
        adminSettingsService.deleteAdminSettingsByKey(tenant.getId(), "mailTemplates");
        adminSettingsService.deleteAdminSettingsByKey(tenant.getId(), "mail");
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenant.getId());
        whiteLabelingService.saveSystemLoginWhiteLabelingParams(new LoginWhiteLabelingParams());
        whiteLabelingService.saveTenantWhiteLabelingParams(tenant.getId(), new WhiteLabelingParams());
        customTranslationService.saveTenantCustomTranslation(tenant.getId(), new CustomTranslation());
        roleService.deleteRolesByTenantId(tenant.getId());
        groupPermissionService.deleteGroupPermissionsByTenantId(tenant.getId());
        cloudEventService.deleteCloudEventsByTenantId(tenant.getId());
        try {
            List<AttributeKvEntry> attributeKvEntries = attributesService.findAll(tenant.getId(), tenant.getId(), DataConstants.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            attributesService.removeAll(tenant.getId(), tenant.getId(), DataConstants.SERVER_SCOPE, attrKeys);
            ListenableFuture<List<EntityGroup>> entityGroupsFuture = entityGroupService.findAllEntityGroups(tenant.getId(), tenant.getId());
            List<EntityGroup> entityGroups = entityGroupsFuture.get();
            entityGroups.stream()
                    .filter(e -> !e.getName().equals(EntityGroup.GROUP_ALL_NAME))
                    .forEach(entityGroup -> entityGroupService.deleteEntityGroup(tenant.getId(), entityGroup.getId()));
        } catch (InterruptedException | ExecutionException e) {
            log.error("Unable to delete entity groups", e);
        }
    }

}
