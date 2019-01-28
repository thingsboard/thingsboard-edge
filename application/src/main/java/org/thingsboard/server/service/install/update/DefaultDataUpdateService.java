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
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
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
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
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
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.SystemDataLoaderService;

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
    private RelationService relationService;

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
                        List<EntityGroup> entityGroups = entityGroupService.findAllEntityGroups(TenantId.SYS_TENANT_ID, tenant.getId()).get();
                        for (EntityGroup entityGroup : entityGroups) {
                            entityGroup.setOwnerId(tenant.getId());
                            entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, tenant.getId(), entityGroup);
                        }
                        EntityGroup entityGroup;
                        Optional<EntityGroup> customerGroupOptional =
                                entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, tenant.getId(), EntityType.CUSTOMER, EntityGroup.GROUP_ALL_NAME).get();
                        if (!customerGroupOptional.isPresent()) {
                            entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, tenant.getId(), EntityType.CUSTOMER);
                        } else {
                            entityGroup = customerGroupOptional.get();
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
                            boolean fetchAllTenantEntities;
                            if (!entityGroupOptional.isPresent()) {
                                entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, tenant.getId(), groupType);
                                fetchAllTenantEntities = true;
                            } else {
                                entityGroup = entityGroupOptional.get();
                                fetchAllTenantEntities = false;
                            }
                            switch (groupType) {
                                case USER:
                                    entityGroupService.findOrCreateTenantUsersGroup(tenant.getId());
                                    Optional<EntityGroup> tenantAdminsOptional =
                                            entityGroupService.findEntityGroupByTypeAndName(tenant.getId(), tenant.getId(), EntityType.USER, EntityGroup.GROUP_TENANT_ADMINS_NAME).get();
                                    if (!tenantAdminsOptional.isPresent()) {
                                        EntityGroup tenantAdmins = entityGroupService.findOrCreateTenantAdminsGroup(tenant.getId());
                                        new TenantAdminsGroupAllUpdater(entityGroup, tenantAdmins).updateEntities(tenant.getId());
                                    }
                                    break;
                                case ASSET:
                                    new AssetsGroupAllUpdater(entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case DEVICE:
                                    new DevicesGroupAllUpdater(entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case ENTITY_VIEW:
                                    new EntityViewGroupAllUpdater(entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case DASHBOARD:
                                    new DashboardsGroupAllUpdater(entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
    };

    private class TenantAdminsGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, User> {

        private final EntityGroup tenantAdmins;

        public TenantAdminsGroupAllUpdater(EntityGroup groupAll, EntityGroup tenantAdmins) {
            super(groupAll);
            this.tenantAdmins = tenantAdmins;
        }

        @Override
        protected TextPageData<User> findEntities(TenantId id, TextPageLink pageLink) {
            return userService.findTenantAdmins(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(User entity, EntityGroup groupAll) {
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, tenantAdmins.getId(), entity.getId());
        }
    }

    private class CustomerUsersGroupAllUpdater extends GroupAllPaginatedUpdater<CustomerId, User> {

        private final TenantId tenantId;
        private final EntityGroup customerUsers;

        public CustomerUsersGroupAllUpdater(TenantId tenantId, EntityGroup groupAll, EntityGroup customerUsers) {
            super(groupAll);
            this.tenantId = tenantId;
            this.customerUsers = customerUsers;
        }

        @Override
        protected TextPageData<User> findEntities(CustomerId id, TextPageLink pageLink) {
            return userService.findCustomerUsers(this.tenantId, id, pageLink);
        }

        @Override
        protected void updateGroupEntity(User entity, EntityGroup groupAll) {
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, customerUsers.getId(), entity.getId());
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
            if (customer.isSubCustomer()) {
                return;
            }
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), customer.getId());
            try {
                List<EntityGroup> entityGroups = entityGroupService.findAllEntityGroups(TenantId.SYS_TENANT_ID, customer.getId()).get();
                for (EntityGroup entityGroup : entityGroups) {
                    entityGroup.setOwnerId(customer.getId());
                    entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, customer.getId(), entityGroup);
                }
                EntityType[] entityGroupTypes = new EntityType[]{EntityType.USER, EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE, EntityType.DASHBOARD, EntityType.ENTITY_VIEW};
                for (EntityType groupType : entityGroupTypes) {
                    Optional<EntityGroup> entityGroupOptional =
                            entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, customer.getId(), groupType, EntityGroup.GROUP_ALL_NAME).get();
                    if (!entityGroupOptional.isPresent()) {
                        EntityGroup entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, customer.getId(), groupType);
                        if (groupType == EntityType.USER) {
                            if (!customer.isPublic()) {
                                entityGroupService.findOrCreateCustomerAdminsGroup(customer.getTenantId(), customer.getId(), null);
                                Optional<EntityGroup> customerUsersOptional =
                                        entityGroupService.findEntityGroupByTypeAndName(customer.getTenantId(), customer.getId(), EntityType.USER, EntityGroup.GROUP_CUSTOMER_USERS_NAME).get();
                                if (!customerUsersOptional.isPresent()) {
                                    EntityGroup customerUsers = entityGroupService.findOrCreateCustomerUsersGroup(customer.getTenantId(), customer.getId(), null);
                                    new CustomerUsersGroupAllUpdater(customer.getTenantId(), entityGroup, customerUsers).updateEntities(customer.getId());
                                }
                            } else {
                                entityGroupService.findOrCreatePublicUsersGroup(customer.getTenantId(), customer.getId());
                            }
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Unable to update Customer", e);
            }
        }
    }

    private class AssetsGroupAllUpdater extends EntityGroupAllPaginatedUpdater<AssetId, Asset> {

        public AssetsGroupAllUpdater(EntityGroup groupAll, boolean fetchAllTenantEntities) {
            super(entityGroupService,
                    groupAll,
                    fetchAllTenantEntities,
                    (tenantId, pageLink) -> assetService.findAssetsByTenantId(tenantId, pageLink),
                    (tenantId, assetIds) -> assetService.findAssetsByTenantIdAndIdsAsync(tenantId, assetIds),
                    entityId -> new AssetId(entityId.getId()),
                    asset -> asset.getId());
        }

        @Override
        protected void unassignFromCustomer(Asset entity) {
            entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
            assetService.saveAsset(entity);
        }

    }

    private class DevicesGroupAllUpdater extends EntityGroupAllPaginatedUpdater<DeviceId, Device> {

        public DevicesGroupAllUpdater(EntityGroup groupAll, boolean fetchAllTenantEntities) {
            super(entityGroupService,
                    groupAll,
                    fetchAllTenantEntities,
                    (tenantId, pageLink) -> deviceService.findDevicesByTenantId(tenantId, pageLink),
                    (tenantId, deviceIds) -> deviceService.findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds),
                    entityId -> new DeviceId(entityId.getId()),
                    device -> device.getId());
        }

        @Override
        protected void unassignFromCustomer(Device entity) {
            entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
            deviceService.saveDevice(entity);
        }
    }


    private class EntityViewGroupAllUpdater extends EntityGroupAllPaginatedUpdater<EntityViewId, EntityView> {

        public EntityViewGroupAllUpdater(EntityGroup groupAll, boolean fetchAllTenantEntities) {
            super(entityGroupService,
                    groupAll,
                    fetchAllTenantEntities,
                    (tenantId, pageLink) -> entityViewService.findEntityViewByTenantId(tenantId, pageLink),
                    (tenantId, entityViewIds) -> entityViewService.findEntityViewsByTenantIdAndIdsAsync(tenantId, entityViewIds),
                    entityId -> new EntityViewId(entityId.getId()),
                    entityView -> entityView.getId());
        }


        @Override
        protected void unassignFromCustomer(EntityView entity) {
            entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
            entityViewService.saveEntityView(entity);
        }
    }

    private class DashboardsGroupAllUpdater extends PaginatedUpdater<TenantId, DashboardInfo> {

        private final EntityGroup groupAll;
        private final boolean fetchAllTenantEntities;

        private Map<CustomerId, EntityGroupId> customersGroupMap = new HashMap<>();
        private Map<CustomerId, Customer> customersMap = new HashMap<>();

        public DashboardsGroupAllUpdater(EntityGroup groupAll,
                                              boolean fetchAllTenantEntities) {
            this.groupAll = groupAll;
            this.fetchAllTenantEntities = fetchAllTenantEntities;
        }

        @Override
        protected TextPageData<DashboardInfo> findEntities(TenantId id, TextPageLink pageLink) {
            if (fetchAllTenantEntities) {
                return dashboardService.findDashboardsByTenantId(id, pageLink);
            } else {
                try {
                    List<EntityId> entityIds = entityGroupService.findAllEntityIds(TenantId.SYS_TENANT_ID, groupAll.getId(), new TimePageLink(Integer.MAX_VALUE)).get();
                    List<DashboardId> dashboardIds = entityIds.stream().map(entityId -> new DashboardId(entityId.getId())).collect(Collectors.toList());
                    List<DashboardInfo> dashboards;
                    if (!dashboardIds.isEmpty()) {
                        dashboards = dashboardService.findDashboardInfoByIdsAsync(TenantId.SYS_TENANT_ID, dashboardIds).get();
                    } else {
                        dashboards = Collections.emptyList();
                    }
                    return new TextPageData<>(dashboards, new TextPageLink(Integer.MAX_VALUE));
                } catch (Exception e) {
                    log.error("Failed to get dashboards from group all!", e);
                    throw new RuntimeException("Failed to get dashboards from group all!", e);
                }
            }
        }

        @Override
        protected void updateEntity(DashboardInfo entity) {
            entityGroupService.addEntityToEntityGroupAll(TenantId.SYS_TENANT_ID, entity.getTenantId(), entity.getId());
            if (entity.getAssignedCustomers() != null) {
                for (ShortCustomerInfo customer : entity.getAssignedCustomers()) {
                    Customer customer1 = customersMap.computeIfAbsent(customer.getCustomerId(), customerId ->
                            customerService.findCustomerById(entity.getTenantId(), customer.getCustomerId()));
                    if (customer1 != null) {
                        EntityGroupId customerEntityGroupId = customersGroupMap.computeIfAbsent(
                                customer.getCustomerId(), customerId ->
                                        entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(entity.getTenantId(),
                                                customerId, entity.getEntityType()).getId()
                        );
                        entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, customerEntityGroupId, entity.getId());
                        dashboardService.unassignDashboardFromCustomer(TenantId.SYS_TENANT_ID, entity.getId(), customer.getCustomerId());
                    } else {
                        Dashboard dashboard = dashboardService.findDashboardById(TenantId.SYS_TENANT_ID, entity.getId());
                        if (dashboard.removeAssignedCustomerInfo(customer)) {
                            EntityRelation relationToDelete =
                                    new EntityRelation(customer.getCustomerId(), entity.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD);
                            relationService.deleteRelation(TenantId.SYS_TENANT_ID, relationToDelete);
                            dashboardService.saveDashboard(dashboard);
                        }
                    }
                }
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
        WhiteLabelingParams preparedWhiteLabelingParams = createWhiteLabelingParams(storedWl, logoImageUrl, true);
        whiteLabelingService.saveSystemWhiteLabelingParams(preparedWhiteLabelingParams);
        adminSettingsService.deleteAdminSettingsByKey(TenantId.SYS_TENANT_ID, LOGO_IMAGE);
        adminSettingsService.deleteAdminSettingsByKey(TenantId.SYS_TENANT_ID, LOGO_IMAGE_CHECKSUM);
    }

    private void updateEntityWhiteLabelingParameters(EntityId entityId) {
        JsonNode storedWl = getEntityWhiteLabelParams(entityId);
        String logoImageUrl = getEntityAttributeValue(entityId, LOGO_IMAGE);
        WhiteLabelingParams preparedWhiteLabelingParams = createWhiteLabelingParams(storedWl, logoImageUrl, false);
        if (entityId.getEntityType() == EntityType.TENANT) {
            whiteLabelingService.saveTenantWhiteLabelingParams(new TenantId(entityId.getId()), preparedWhiteLabelingParams);
        }
        if (entityId.getEntityType() == EntityType.CUSTOMER) {
            whiteLabelingService.saveCustomerWhiteLabelingParams(TenantId.SYS_TENANT_ID, new CustomerId(entityId.getId()), preparedWhiteLabelingParams);
        }
        deleteEntityAttribute(entityId, LOGO_IMAGE);
        deleteEntityAttribute(entityId, LOGO_IMAGE_CHECKSUM);
    }

    private WhiteLabelingParams createWhiteLabelingParams(JsonNode storedWl, String logoImageUrl, boolean isSystem) {
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
            if (isSystem) {
                if (!storedWl.has("helpLinkBaseUrl")) {
                    whiteLabelingParams.setHelpLinkBaseUrl("https://thingsboard.io");
                }
                if (!storedWl.has("enableHelpLinks")) {
                    whiteLabelingParams.setEnableHelpLinks(true);
                }
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
