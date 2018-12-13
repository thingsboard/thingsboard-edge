/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@Profile("install")
@Slf4j
public class DefaultDataUpdateService implements DataUpdateService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String WHITE_LABEL_PARAMS = "whiteLabelParams";
    private static final String LOGO_IMAGE = "logoImage";
    private static final String LOGO_IMAGE_CHECKSUM = "logoImageChecksum";

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Override
    public void updateData(String fromVersion) throws Exception {

        switch (fromVersion) {
            case "1.4.0":
                log.info("Updating data from version 1.4.0 to 2.0.0 ...");
                tenantsDefaultRuleChainUpdater.updateEntities(null);
                break;
            case "2.2.0":
                log.info("Updating data from version 2.2.0 to 2.3.0PE ...");
                tenantsCustomersGroupAllUpdater.updateEntities(null);
                tenantEntitiesGroupAllUpdater.updateEntities(null);

                AdminSettings mailTemplateSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mailTemplates");
                if (mailTemplateSettings == null) {
                    systemDataLoaderService.loadMailTemplates();
                }

                //White Labeling updates
                updateSystemWhiteLabelingParameters();
                tenantsWhiteLabelingUpdater.updateEntities(null);

                break;
            default:
                throw new RuntimeException("Unable to update data, unsupported fromVersion: " + fromVersion);
        }
    }

    private PaginatedUpdater<String, Tenant> tenantsDefaultRuleChainUpdater =
            new PaginatedUpdater<String, Tenant>() {

                @Override
                protected TextPageData<Tenant> findEntities(String region, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        RuleChain ruleChain = ruleChainService.getRootTenantRuleChain(tenant.getId());
                        if (ruleChain == null) {
                            installScripts.createDefaultRuleChains(tenant.getId());
                        }
                    } catch (Exception e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };

    private PaginatedUpdater<String, Tenant> tenantsCustomersGroupAllUpdater =
            new PaginatedUpdater<String, Tenant>() {

                @Override
                protected TextPageData<Tenant> findEntities(String region, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        EntityGroup entityGroup;
                        Optional<EntityGroup> customerGroupOptional =
                                entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, tenant.getId(), EntityType.CUSTOMER, EntityGroup.GROUP_ALL_NAME).get();
                        if (!customerGroupOptional.isPresent()) {
                            entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, tenant.getId(), EntityType.CUSTOMER);
                        } else {
                            entityGroup = customerGroupOptional.get();
                            entityGroup.setOwnerId(tenant.getId());
                            entityGroup = entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, tenant.getId(), entityGroup);
                        }
                        new CustomersGroupAllUpdater(entityGroup).updateEntities(tenant.getId());
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };

    private PaginatedUpdater<String, Tenant> tenantEntitiesGroupAllUpdater =
            new PaginatedUpdater<String, Tenant>() {

                @Override
                protected TextPageData<Tenant> findEntities(String region, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        EntityType[] entityGroupTypes = new EntityType[]{EntityType.USER, EntityType.ASSET, EntityType.DEVICE, EntityType.DASHBOARD, EntityType.ENTITY_VIEW};
                        for (EntityType groupType : entityGroupTypes) {
                            EntityGroup entityGroup;
                            Optional<EntityGroup> entityGroupOptional =
                                    entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, tenant.getId(), groupType, EntityGroup.GROUP_ALL_NAME).get();
                            if (!entityGroupOptional.isPresent()) {
                                entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, tenant.getId(), groupType);
                            } else {
                                entityGroup = entityGroupOptional.get();
                                entityGroup.setOwnerId(tenant.getId());
                                entityGroup = entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, tenant.getId(), entityGroup);
                            }
                            switch (groupType) {
                                case USER:
                                    new TenantAdminsGroupAllUpdater(entityGroup).updateEntities(tenant.getId());
                                    break;
                                case ASSET:
                                    new AssetsGroupAllUpdater(entityGroup).updateEntities(tenant.getId());
                                    break;
                                case DEVICE:
                                    new DevicesGroupAllUpdater(entityGroup).updateEntities(tenant.getId());
                                    break;
                                case DASHBOARD:
                                    new DashboardsGroupAllUpdater(entityGroup).updateEntities(tenant.getId());
                                    break;
                                case ENTITY_VIEW:
                                    new EntityViewGroupAllUpdater(entityGroup).updateEntities(tenant.getId());
                                    break;
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
    };

    private abstract class GroupAllPaginatedUpdater<I, D extends SearchTextBased<? extends UUIDBased>> extends PaginatedUpdater<I,D> {

        protected final EntityGroup groupAll;

        public GroupAllPaginatedUpdater(EntityGroup groupAll) {
            this.groupAll = groupAll;
        }

        @Override
        protected void updateEntity(D entity) {
            updateGroupEntity(entity, groupAll);
        }

        protected abstract void updateGroupEntity(D entity, EntityGroup groupAll);

    }

    private class TenantAdminsGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, User> {

        public TenantAdminsGroupAllUpdater(EntityGroup groupAll) {
            super(groupAll);
        }

        @Override
        protected TextPageData<User> findEntities(TenantId id, TextPageLink pageLink) {
            return userService.findTenantAdmins(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(User entity, EntityGroup groupAll) {
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
        }
    }

    private class CustomerUsersGroupAllUpdater extends GroupAllPaginatedUpdater<CustomerId, User> {

        private final TenantId tenantId;
        private final EntityGroup customerAdmins;

        public CustomerUsersGroupAllUpdater(TenantId tenantId, EntityGroup groupAll, EntityGroup customerAdmins) {
            super(groupAll);
            this.tenantId = tenantId;
            this.customerAdmins = customerAdmins;
        }

        @Override
        protected TextPageData<User> findEntities(CustomerId id, TextPageLink pageLink) {
            return userService.findCustomerUsers(this.tenantId, id, pageLink);
        }

        @Override
        protected void updateGroupEntity(User entity, EntityGroup groupAll) {
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, customerAdmins.getId(), entity.getId());
        }
    }

    private class CustomersGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, Customer> {

        public CustomersGroupAllUpdater(EntityGroup groupAll) {
            super(groupAll);
        }

        @Override
        protected TextPageData<Customer> findEntities(TenantId id, TextPageLink pageLink) {
            return customerService.findCustomersByTenantId(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(Customer customer, EntityGroup groupAll) {
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), customer.getId());
            try {
                EntityType[] entityGroupTypes = new EntityType[]{EntityType.USER, EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE, EntityType.DASHBOARD, EntityType.ENTITY_VIEW};
                for (EntityType groupType : entityGroupTypes) {
                    Optional<EntityGroup> entityGroupOptional =
                            entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, customer.getId(), groupType, EntityGroup.GROUP_ALL_NAME).get();
                    if (!entityGroupOptional.isPresent()) {
                        EntityGroup entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, customer.getId(), groupType);
                        if (groupType == EntityType.USER) {
                            EntityGroup customerAdmins = new EntityGroup();
                            //TODO (Security): validate
                            customerAdmins.setName("Customer Admins");
                            customerAdmins.setType(EntityType.USER);
                            customerAdmins = entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, customer.getId(), customerAdmins);
                            Role customerAdminsRole = new Role();
                            customerAdminsRole.setTenantId(customer.getTenantId());
                            customerAdminsRole.setCustomerId(customer.getId());
                            customerAdminsRole.setType(RoleType.GENERIC);
                            customerAdminsRole.setName("Customer Admin");
                            //TODO (Security):
                            customerAdminsRole.setPermissions(new ObjectMapper().createObjectNode());
                            customerAdminsRole = roleService.saveRole(TenantId.SYS_TENANT_ID, customerAdminsRole);
                            GroupPermission groupPermission = new GroupPermission();
                            groupPermission.setTenantId(customer.getTenantId());
                            groupPermission.setUserGroupId(customerAdmins.getId());
                            groupPermission.setRoleId(customerAdminsRole.getId());
                            groupPermissionService.saveGroupPermission(TenantId.SYS_TENANT_ID, groupPermission);
                            new CustomerUsersGroupAllUpdater(customer.getTenantId(), entityGroup, customerAdmins).updateEntities(customer.getId());
                        }
                    } else {
                        EntityGroup entityGroup = entityGroupOptional.get();
                        entityGroup.setOwnerId(customer.getId());
                        entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, customer.getId(), entityGroup);
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Unable to update Customer", e);
            }
        }
    }

    private class AssetsGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, Asset> {

        public AssetsGroupAllUpdater(EntityGroup groupAll) {
            super(groupAll);
        }

        @Override
        protected TextPageData<Asset> findEntities(TenantId id, TextPageLink pageLink) {
            return assetService.findAssetsByTenantId(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(Asset entity, EntityGroup groupAll) {
            if (entity.getCustomerId() != null && !entity.getCustomerId().isNullUid()) {
                entityGroupService.removeEntityFromEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
                entityGroupService.addEntityToEntityGroupAll(TenantId.SYS_TENANT_ID, entity.getCustomerId(), entity.getId());
            } else {
                entityGroupService.addEntityToEntityGroupAll(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            }
        }
    }

    private class DevicesGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, Device> {

        public DevicesGroupAllUpdater(EntityGroup groupAll) {
            super(groupAll);
        }

        @Override
        protected TextPageData<Device> findEntities(TenantId id, TextPageLink pageLink) {
            return deviceService.findDevicesByTenantId(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(Device entity, EntityGroup groupAll) {
            if (entity.getCustomerId() != null && !entity.getCustomerId().isNullUid()) {
                entityGroupService.removeEntityFromEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
                entityGroupService.addEntityToEntityGroupAll(TenantId.SYS_TENANT_ID, entity.getCustomerId(), entity.getId());
            } else {
                entityGroupService.addEntityToEntityGroupAll(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            }
        }
    }

    private class DashboardsGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, DashboardInfo> {

        private Map<CustomerId, EntityGroupId> customerDashboardGroupsMap = new HashMap<>();

        public DashboardsGroupAllUpdater(EntityGroup groupAll) {
            super(groupAll);
        }

        @Override
        protected TextPageData<DashboardInfo> findEntities(TenantId id, TextPageLink pageLink) {
            return dashboardService.findDashboardsByTenantId(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(DashboardInfo entity, EntityGroup groupAll) {
            entityGroupService.addEntityToEntityGroupAll(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            for (ShortCustomerInfo customer : entity.getAssignedCustomers()) {
                EntityGroupId groupId = getCustomerDashboardGroup(entity.getTenantId(), customer);
                if (groupId != null) {
                    entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupId, entity.getId());
                }
                dashboardService.unassignDashboardFromCustomer(TenantId.SYS_TENANT_ID, entity.getId(), customer.getCustomerId());
            }
        }

        private EntityGroupId getCustomerDashboardGroup(TenantId tenantId, ShortCustomerInfo customer) {
            return customerDashboardGroupsMap.computeIfAbsent(customer.getCustomerId(), customerId -> {
                String groupName = "Dashboards for " + customer.getTitle();
                try {
                    Optional<EntityGroup> entityGroupOptional = entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, tenantId, EntityType.DASHBOARD, groupName).get();
                    if (entityGroupOptional.isPresent()) {
                        return entityGroupOptional.get().getId();
                    } else {
                        EntityGroup entityGroup = new EntityGroup();
                        entityGroup.setName(groupName);
                        entityGroup.setType(EntityType.DASHBOARD);
                        EntityGroup savedEntityGroup = entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, tenantId, entityGroup);
                        //TODO (Security): Add Role and GroupPermission (Customer Admins) -> savedEntityGroup
                        return savedEntityGroup.getId();
                    }
                } catch (Exception e) {
                    log.error("Failed to find/create dashboard group for customer [{}]" + customer.getTitle(), e);
                    return null;
                }
            });
        }
    }

    private class EntityViewGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, EntityView> {

        public EntityViewGroupAllUpdater(EntityGroup groupAll) {
            super(groupAll);
        }

        @Override
        protected TextPageData<EntityView> findEntities(TenantId id, TextPageLink pageLink) {
            return entityViewService.findEntityViewByTenantId(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(EntityView entity, EntityGroup groupAll) {
            if (entity.getCustomerId() != null && !entity.getCustomerId().isNullUid()) {
                entityGroupService.removeEntityFromEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
                entityGroupService.addEntityToEntityGroupAll(TenantId.SYS_TENANT_ID, entity.getCustomerId(), entity.getId());
            } else {
                entityGroupService.addEntityToEntityGroupAll(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            }
        }
    }

    private PaginatedUpdater<String, Tenant> tenantsWhiteLabelingUpdater = new PaginatedUpdater<String, Tenant>() {
                @Override
                protected TextPageData<Tenant> findEntities(String id, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    updateEntityWhiteLabelingParameters(tenant.getId());
                    customersWhiteLabelingUpdater.updateEntities(tenant.getId());
                }
    };

    private PaginatedUpdater<TenantId, Customer> customersWhiteLabelingUpdater = new PaginatedUpdater<TenantId, Customer>() {
        @Override
        protected TextPageData<Customer> findEntities(TenantId id, TextPageLink pageLink) {
            return customerService.findCustomersByTenantId(id, pageLink);
        }
        @Override
        protected void updateEntity(Customer customer) {
            updateEntityWhiteLabelingParameters(customer.getId());
        }
    };

    public abstract class PaginatedUpdater<I, D extends SearchTextBased<? extends UUIDBased>> {

        private static final int DEFAULT_LIMIT = 100;

        public void updateEntities(I id) {
            TextPageLink pageLink = new TextPageLink(DEFAULT_LIMIT);
            boolean hasNext = true;
            while (hasNext) {
                TextPageData<D> entities = findEntities(id, pageLink);
                for (D entity : entities.getData()) {
                    updateEntity(entity);
                }
                hasNext = entities.hasNext();
                if (hasNext) {
                    pageLink = entities.getNextPageLink();
                }
            }
        }

        protected abstract TextPageData<D> findEntities(I id, TextPageLink pageLink);

        protected abstract void updateEntity(D entity);

    }

    private void updateSystemWhiteLabelingParameters() {
        AdminSettings whiteLabelParamsSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, WHITE_LABEL_PARAMS);
        JsonNode storedWl = null;
        String logoImageUrl = null;
        if (whiteLabelParamsSettings != null) {
            String json = whiteLabelParamsSettings.getJsonValue().get("value").asText();
            if (!StringUtils.isEmpty(json)) {
                try {
                    storedWl = objectMapper.readTree(json);
                } catch (IOException e) {
                    log.error("Unable to read System White Labeling Params!", e);
                }
            }
        }
        AdminSettings logoImageSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, LOGO_IMAGE);
        if (logoImageSettings != null) {
            logoImageUrl = logoImageSettings.getJsonValue().get("value").asText();
        }
        WhiteLabelingParams preparedWhiteLabelingParams = createWhiteLabelingParams(storedWl, logoImageUrl);
        whiteLabelingService.saveSystemWhiteLabelingParams(preparedWhiteLabelingParams);
        adminSettingsService.deleteAdminSettingsByKey(TenantId.SYS_TENANT_ID, LOGO_IMAGE);
        adminSettingsService.deleteAdminSettingsByKey(TenantId.SYS_TENANT_ID, LOGO_IMAGE_CHECKSUM);
    }

    private void updateEntityWhiteLabelingParameters(EntityId entityId) {
        JsonNode storedWl = getEntityWhiteLabelParams(entityId);
        String logoImageUrl = getEntityAttributeValue(entityId, LOGO_IMAGE);
        WhiteLabelingParams preparedWhiteLabelingParams = createWhiteLabelingParams(storedWl, logoImageUrl);
        if (entityId.getEntityType() == EntityType.TENANT) {
            whiteLabelingService.saveTenantWhiteLabelingParams(new TenantId(entityId.getId()), preparedWhiteLabelingParams);
        }
        if (entityId.getEntityType() == EntityType.CUSTOMER) {
            whiteLabelingService.saveCustomerWhiteLabelingParams(TenantId.SYS_TENANT_ID, new CustomerId(entityId.getId()), preparedWhiteLabelingParams);
        }
        deleteEntityAttribute(entityId, LOGO_IMAGE);
        deleteEntityAttribute(entityId, LOGO_IMAGE_CHECKSUM);
    }

    private WhiteLabelingParams createWhiteLabelingParams(JsonNode storedWl, String logoImageUrl) {
        WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
        whiteLabelingParams.setLogoImageUrl(logoImageUrl);
        if (storedWl != null) {
            if (storedWl.has("logoImageUrl")) {
                logoImageUrl = storedWl.get("logoImageUrl").asText();
                if (!StringUtils.isEmpty(logoImageUrl) && !"null".equals(logoImageUrl)) {
                    whiteLabelingParams.setLogoImageUrl(logoImageUrl);
                }
            }
            if (storedWl.has("logoImageHeight")) {
                int logoImageHeight = storedWl.get("logoImageHeight").asInt();
                if (logoImageHeight > 0) {
                    whiteLabelingParams.setLogoImageHeight(logoImageHeight);
                }
            }
            if (storedWl.has("appTitle")) {
                String appTitle = storedWl.get("appTitle").asText();
                if (!StringUtils.isEmpty(appTitle) && !"null".equals(appTitle)) {
                    whiteLabelingParams.setAppTitle(appTitle);
                }
            }
            if (storedWl.has("faviconUrl")) {
                String faviconUrl = storedWl.get("faviconUrl").asText();
                if (!StringUtils.isEmpty(faviconUrl) && !"null".equals(faviconUrl)) {
                    String faviconType = "";
                    if (storedWl.has("faviconType")) {
                        faviconType = storedWl.get("faviconType").asText();
                    }
                    Favicon favicon;
                    if (StringUtils.isEmpty(faviconType)) {
                        favicon = new Favicon(faviconUrl);
                    } else {
                        favicon = new Favicon(faviconUrl, faviconType);
                    }
                    whiteLabelingParams.setFavicon(favicon);
                }
            }
            if (storedWl.has("favicon")) {
                JsonNode faviconJson = storedWl.get("favicon");
                Favicon favicon = null;
                try {
                    favicon = objectMapper.treeToValue(faviconJson, Favicon.class);
                } catch (JsonProcessingException e) {
                    log.error("Unable to read Favicon from previous White Labeling Params!", e);
                }
                whiteLabelingParams.setFavicon(favicon);
            }
            if (storedWl.has("paletteSettings")) {
                JsonNode paletteSettingsJson = storedWl.get("paletteSettings");
                PaletteSettings paletteSettings = null;
                try {
                    paletteSettings = objectMapper.treeToValue(paletteSettingsJson, PaletteSettings.class);
                } catch (JsonProcessingException e) {
                    log.error("Unable to read Palette Settings from previous White Labeling Params!", e);
                }
                whiteLabelingParams.setPaletteSettings(paletteSettings);
            }
        }
        return whiteLabelingParams;
    }

    private JsonNode getEntityWhiteLabelParams(EntityId entityId) {
        String value = getEntityAttributeValue(entityId, WHITE_LABEL_PARAMS);
        if (!StringUtils.isEmpty(value)) {
            try {
                return objectMapper.readTree(value);
            } catch (IOException e) {
                log.error("Unable to read White Labeling Params from JSON!", e);
                return null;
            }
        } else {
            return null;
        }
    }

    private String getEntityAttributeValue(EntityId entityId, String key) {
        List<AttributeKvEntry> attributeKvEntries = null;
        try {
            attributeKvEntries = attributesService.find(TenantId.SYS_TENANT_ID, entityId, DataConstants.SERVER_SCOPE, Arrays.asList(key)).get();
        } catch (Exception e) {
            log.error("Unable to find attribute for " + key + "!", e);
        }
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private void deleteEntityAttribute(EntityId entityId, String key) {
        try {
            attributesService.removeAll(TenantId.SYS_TENANT_ID, entityId, DataConstants.SERVER_SCOPE,  Arrays.asList(key)).get();
        } catch (Exception e) {
            log.error("Unable to delete attribute for " + key + "!", e);
        }
    }

}
