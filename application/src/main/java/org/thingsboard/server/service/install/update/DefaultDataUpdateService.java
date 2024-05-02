/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.integration.AbstractIntegration;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceConnectivityConfiguration;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.component.RuleNodeClassInfo;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.utils.TbNodeUpgradeUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Profile("install")
@Slf4j
public class DefaultDataUpdateService implements DataUpdateService {

    private static final int MAX_PENDING_SAVE_RULE_NODE_FUTURES = 256;
    private static final int DEFAULT_PAGE_SIZE = 1024;
    private static final int DEFAULT_LIMIT = 100;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

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
    private EdgeService edgeService;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    private EdgeEventDao edgeEventDao;

    @Autowired
    private IntegrationRateLimitsUpdater integrationRateLimitsUpdater;

    @Autowired
    JpaExecutorService jpaExecutorService;

    @Autowired
    DeviceConnectivityConfiguration connectivityConfiguration;

    @Autowired
    private CustomerDao customerDao;

    @Override
    public void updateData(String fromVersion) throws Exception {

        switch (fromVersion) {
            case "3.5.1":
                log.info("Updating data from version 3.5.1 to 3.6.0 ...");
                integrationRateLimitsUpdater.updateEntities();
                migrateEdgeEvents("Starting edge events migration - adding seq_id column. ");
                break;
            case "3.6.0":
                log.info("Updating data from version 3.6.0 to 3.6.1 ...");
                migrateDeviceConnectivity();
                break;
            case "3.6.4":
                log.info("Updating data from version 3.6.4 to 3.7.0 ...");
                updateCustomersWithTheSameTitle();
                break;
            case "ce":
                log.info("Updating data ...");
                tenantsCustomersGroupAllUpdater.updateEntities();
                tenantEntitiesGroupAllUpdater.updateEntities();
                tenantIntegrationUpdater.updateEntities();
                //for 2.4.0
                JsonNode mailTemplatesSettings = whiteLabelingService.findMailTemplatesByTenantId(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
                if (mailTemplatesSettings.isEmpty()) {
                    systemDataLoaderService.loadMailTemplates();
                } else {
                    systemDataLoaderService.updateMailTemplates(mailTemplatesSettings);
                }
                break;
            default:
                throw new RuntimeException("Unable to update data, unsupported fromVersion: " + fromVersion);
        }
    }

    private void migrateEdgeEvents(String logPrefix) {
        boolean skipEdgeEventsMigration = getEnv("TB_SKIP_EDGE_EVENTS_MIGRATION", false);
        if (!skipEdgeEventsMigration) {
            log.info(logPrefix + "Can be skipped with TB_SKIP_EDGE_EVENTS_MIGRATION env variable set to true");
            edgeEventDao.migrateEdgeEvents();
        } else {
            log.info("Skipping edge events migration");
        }
    }

    private void updateCustomersWithTheSameTitle() {
        var customers = new ArrayList<Customer>();
        new PageDataIterable<>(pageLink ->
                customerDao.findCustomersWithTheSameTitle(pageLink), DEFAULT_PAGE_SIZE
        ).forEach(customers::add);
        if (customers.isEmpty()) {
            return;
        }
        var firstCustomer = customers.get(0);
        var titleToDeduplicate = firstCustomer.getTitle();
        var tenantIdToDeduplicate = firstCustomer.getTenantId();
        int duplicateCounter = 1;

        for (int i = 1; i < customers.size(); i++) {
            var currentCustomer = customers.get(i);
            if (currentCustomer.getTitle().equals(titleToDeduplicate) && currentCustomer.getTenantId().equals(tenantIdToDeduplicate)) {
                duplicateCounter++;
                String currentTitle = currentCustomer.getTitle();
                String newTitle = currentTitle + " " + duplicateCounter;
                try {
                    Optional<Customer> customerOpt = customerService.findCustomerByTenantIdAndTitle(tenantIdToDeduplicate, newTitle);
                    if (customerOpt.isPresent()) {
                        // fallback logic: customer with title 'currentTitle + " " + duplicateCounter;' might be another duplicate.
                        newTitle = currentTitle + "_" + currentCustomer.getId();
                    }
                } catch (Exception e) {
                    log.trace("Failed to find customer with title due to: ", e);
                    // fallback logic: customer with title 'currentTitle + " " + duplicateCounter;' might be another duplicate.
                    newTitle = currentTitle + "_" + currentCustomer.getId();
                }
                currentCustomer.setTitle(newTitle);
                try {
                    Customer savedCustomer = customerDao.save(tenantIdToDeduplicate, currentCustomer);
                    List<EdgeId> edgeIds = edgeService.findAllRelatedEdgeIds(savedCustomer.getTenantId(), savedCustomer.getId());
                    if (edgeIds != null) {
                        for (EdgeId edgeId : edgeIds) {
                            Edge edge = edgeService.findEdgeById(savedCustomer.getTenantId(), edgeId);
                            edgeService.renameEdgeAllGroups(savedCustomer.getTenantId(), edge, edge.getName(), currentTitle, savedCustomer.getTitle());
                        }
                    }
                } catch (Exception e) {
                    log.error("[{}] Failed to update public customer with id and title: {}, oldTitle: {}, due to: ",
                            currentCustomer.getTenantId(), newTitle, currentTitle, e);
                }
                continue;
            }
            titleToDeduplicate = currentCustomer.getTitle();
            tenantIdToDeduplicate = currentCustomer.getTenantId();
            duplicateCounter = 1;
        }
    }

    private void migrateDeviceConnectivity() {
        if (adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "connectivity") == null) {
            AdminSettings connectivitySettings = new AdminSettings();
            connectivitySettings.setTenantId(TenantId.SYS_TENANT_ID);
            connectivitySettings.setKey("connectivity");
            connectivitySettings.setJsonValue(JacksonUtil.valueToTree(connectivityConfiguration.getConnectivity()));
            adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, connectivitySettings);
        }
    }

    @Override
    public void upgradeRuleNodes() {
        try {
            int totalRuleNodesUpgraded = 0;
            log.info("Starting rule nodes upgrade ...");
            var nodeClassToVersionMap = componentDiscoveryService.getVersionedNodes();
            log.debug("Found {} versioned nodes to check for upgrade!", nodeClassToVersionMap.size());
            for (var ruleNodeClassInfo : nodeClassToVersionMap) {
                var ruleNodeTypeForLogs = ruleNodeClassInfo.getSimpleName();
                var toVersion = ruleNodeClassInfo.getCurrentVersion();
                log.debug("Going to check for nodes with type: {} to upgrade to version: {}.", ruleNodeTypeForLogs, toVersion);
                var ruleNodesIdsToUpgrade = getRuleNodesIdsWithTypeAndVersionLessThan(ruleNodeClassInfo.getClassName(), toVersion);
                if (ruleNodesIdsToUpgrade.isEmpty()) {
                    log.debug("There are no active nodes with type {}, or all nodes with this type already set to latest version!", ruleNodeTypeForLogs);
                    continue;
                }
                var ruleNodeIdsPartitions = Lists.partition(ruleNodesIdsToUpgrade, MAX_PENDING_SAVE_RULE_NODE_FUTURES);
                for (var ruleNodePack : ruleNodeIdsPartitions) {
                    totalRuleNodesUpgraded += processRuleNodePack(ruleNodePack, ruleNodeClassInfo);
                    log.info("{} upgraded rule nodes so far ...", totalRuleNodesUpgraded);
                }
            }
            log.info("Finished rule nodes upgrade. Upgraded rule nodes count: {}", totalRuleNodesUpgraded);
        } catch (Exception e) {
            log.error("Unexpected error during rule nodes upgrade: ", e);
        }
    }

    private int processRuleNodePack(List<RuleNodeId> ruleNodeIdsBatch, RuleNodeClassInfo ruleNodeClassInfo) {
        var saveFutures = new ArrayList<ListenableFuture<?>>(MAX_PENDING_SAVE_RULE_NODE_FUTURES);
        String ruleNodeType = ruleNodeClassInfo.getSimpleName();
        int toVersion = ruleNodeClassInfo.getCurrentVersion();
        var ruleNodesPack = ruleChainService.findAllRuleNodesByIds(ruleNodeIdsBatch);
        for (var ruleNode : ruleNodesPack) {
            if (ruleNode == null) {
                continue;
            }
            var ruleNodeId = ruleNode.getId();
            int fromVersion = ruleNode.getConfigurationVersion();
            log.debug("Going to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}",
                    ruleNodeId, ruleNodeType, fromVersion, toVersion);
            try {
                TbNodeUpgradeUtils.upgradeConfigurationAndVersion(ruleNode, ruleNodeClassInfo);
                saveFutures.add(jpaExecutorService.submit(() -> {
                    ruleChainService.saveRuleNode(TenantId.SYS_TENANT_ID, ruleNode);
                    log.debug("Successfully upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}",
                            ruleNodeId, ruleNodeType, fromVersion, toVersion);
                }));
            } catch (Exception e) {
                log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {} due to: ",
                        ruleNodeId, ruleNodeType, fromVersion, toVersion, e);
            }
        }
        try {
            return Futures.allAsList(saveFutures).get().size();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to process save rule nodes requests due to: ", e);
        }
    }

    private List<RuleNodeId> getRuleNodesIdsWithTypeAndVersionLessThan(String type, int toVersion) {
        var ruleNodeIds = new ArrayList<RuleNodeId>();
        new PageDataIterable<>(pageLink ->
                ruleChainService.findAllRuleNodeIdsByTypeAndVersionLessThan(type, toVersion, pageLink), DEFAULT_PAGE_SIZE
        ).forEach(ruleNodeIds::add);
        return ruleNodeIds;
    }

    boolean convertDeviceProfileForVersion330(JsonNode profileData) {
        boolean isUpdated = false;
        if (profileData.has("alarms") && !profileData.get("alarms").isNull()) {
            JsonNode alarms = profileData.get("alarms");
            for (JsonNode alarm : alarms) {
                if (alarm.has("createRules")) {
                    JsonNode createRules = alarm.get("createRules");
                    for (AlarmSeverity severity : AlarmSeverity.values()) {
                        if (createRules.has(severity.name())) {
                            JsonNode spec = createRules.get(severity.name()).get("condition").get("spec");
                            if (convertDeviceProfileAlarmRulesForVersion330(spec)) {
                                isUpdated = true;
                            }
                        }
                    }
                }
                if (alarm.has("clearRule") && !alarm.get("clearRule").isNull()) {
                    JsonNode spec = alarm.get("clearRule").get("condition").get("spec");
                    if (convertDeviceProfileAlarmRulesForVersion330(spec)) {
                        isUpdated = true;
                    }
                }
            }
        }
        return isUpdated;
    }

    private PaginatedUpdater<String, Tenant> tenantsCustomersGroupAllUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenants customers group all updater";
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    new EntityGroupsOwnerUpdater(tenant.getId()).updateEntities(tenant.getId());
                    EntityGroup entityGroup;
                    Optional<EntityGroup> customerGroupOptional =
                            entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, tenant.getId(), EntityType.CUSTOMER, EntityGroup.GROUP_ALL_NAME);
                    if (!customerGroupOptional.isPresent()) {
                        entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, tenant.getId(), EntityType.CUSTOMER);
                    } else {
                        entityGroup = customerGroupOptional.get();
                    }
                    new CustomersGroupAllUpdater(entityGroup).updateEntities(tenant.getId());
                }
            };

    private PaginatedUpdater<String, Tenant> tenantEntitiesGroupAllUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenant entities group all updater";
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        EntityType[] entityGroupTypes = new EntityType[]{EntityType.USER, EntityType.ASSET, EntityType.DEVICE, EntityType.DASHBOARD, EntityType.ENTITY_VIEW, EntityType.EDGE};
                        for (EntityType groupType : entityGroupTypes) {
                            EntityGroup entityGroup;
                            Optional<EntityGroup> entityGroupOptional =
                                    entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, tenant.getId(), groupType, EntityGroup.GROUP_ALL_NAME);
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
                                    new CustomerUsersTenantGroupAllRemover(entityGroup).updateEntities(tenant.getId());
                                    entityGroupService.findOrCreateTenantUsersGroup(tenant.getId());
                                    Optional<EntityGroup> tenantAdminsOptional =
                                            entityGroupService.findEntityGroupByTypeAndName(tenant.getId(), tenant.getId(), EntityType.USER, EntityGroup.GROUP_TENANT_ADMINS_NAME);
                                    if (!tenantAdminsOptional.isPresent()) {
                                        EntityGroup tenantAdmins = entityGroupService.findOrCreateTenantAdminsGroup(tenant.getId());
                                        new TenantAdminsGroupAllUpdater(entityGroup, tenantAdmins).updateEntities(tenant.getId());
                                    }
                                    break;
                                case ASSET:
                                    new AssetsGroupAllUpdater(assetService, customerService, entityGroupService, entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case DEVICE:
                                    new DevicesGroupAllUpdater(deviceService, customerService, entityGroupService, entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case ENTITY_VIEW:
                                    new EntityViewGroupAllUpdater(entityViewService, customerService, entityGroupService, entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case EDGE:
                                    new EdgesGroupAllUpdater(edgeService, customerService, entityGroupService, entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case DASHBOARD:
                                    new DashboardsGroupAllUpdater(entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };

    private class EntityGroupsOwnerUpdater extends PaginatedUpdater<EntityId, EntityGroup> {

        private final EntityId ownerId;

        public EntityGroupsOwnerUpdater(EntityId ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        protected String getName() {
            return "Entity groups owner updater";
        }

        @Override
        protected PageData<EntityGroup> findEntities(EntityId parentEntityId, PageLink pageLink) {
            return entityGroupService.findAllEntityGroupsByParentRelation(TenantId.SYS_TENANT_ID, parentEntityId, pageLink);
        }

        @Override
        protected void updateEntity(EntityGroup entityGroup) {
            if (entityGroup.getOwnerId() == null || entityGroup.getOwnerId().isNullUid()) {
                entityGroup.setOwnerId(this.ownerId);
                entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, this.ownerId, entityGroup);
            }
        }

    }

    private class TenantAdminsGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, User> {

        private final EntityGroup tenantAdmins;

        public TenantAdminsGroupAllUpdater(EntityGroup groupAll, EntityGroup tenantAdmins) {
            super(groupAll);
            this.tenantAdmins = tenantAdmins;
        }

        @Override
        protected String getName() {
            return "Tenant admins group all updater";
        }

        @Override
        protected PageData<User> findEntities(TenantId id, PageLink pageLink) {
            return userService.findTenantAdmins(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(User entity, EntityGroup groupAll) {
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, tenantAdmins.getId(), entity.getId());
        }
    }

    private class CustomerUsersTenantGroupAllRemover extends PaginatedUpdater<TenantId, User> {

        private final EntityGroup groupAll;

        public CustomerUsersTenantGroupAllRemover(EntityGroup groupAll) {
            this.groupAll = groupAll;
        }

        @Override
        protected String getName() {
            return "Customer users tenant group all remover";
        }

        @Override
        protected PageData<User> findEntities(TenantId id, PageLink pageLink) {
            try {
                List<EntityId> entityIds = entityGroupService.findAllEntityIdsAsync(TenantId.SYS_TENANT_ID, groupAll.getId(), new PageLink(Integer.MAX_VALUE)).get();
                List<UserId> userIds = entityIds.stream().map(entityId -> new UserId(entityId.getId())).collect(Collectors.toList());
                List<User> users;
                if (!userIds.isEmpty()) {
                    users = userService.findUsersByTenantIdAndIdsAsync(id, userIds).get();
                } else {
                    users = Collections.emptyList();
                }
                return new PageData<>(users, 1, users.size(), false);
            } catch (Exception e) {
                log.error("Failed to get users from group all!", e);
                throw new RuntimeException("Failed to get users from group all!", e);
            }
        }

        @Override
        protected void updateEntity(User entity) {
            if (Authority.CUSTOMER_USER.equals(entity.getAuthority())) {
                entityGroupService.removeEntityFromEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            }
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
        protected String getName() {
            return "Customer users group all updater";
        }

        @Override
        protected PageData<User> findEntities(CustomerId id, PageLink pageLink) {
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
        protected String getName() {
            return "Customers group all updater";
        }

        @Override
        protected PageData<Customer> findEntities(TenantId id, PageLink pageLink) {
            return customerService.findCustomersByTenantId(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(Customer customer, EntityGroup groupAll) {
            if (customer.getId() == null || customer.getId().isNullUid()) {
                log.warn("Customer has invalid id [{}]", customer.getId());
                log.warn("[{}]", customer);
                return;
            }
            if (customer.isSubCustomer()) {
                return;
            }
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), customer.getId());
            new EntityGroupsOwnerUpdater(customer.getId()).updateEntities(customer.getId());
            EntityType[] entityGroupTypes = new EntityType[]{EntityType.USER, EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE, EntityType.DASHBOARD, EntityType.ENTITY_VIEW, EntityType.EDGE};
            for (EntityType groupType : entityGroupTypes) {
                Optional<EntityGroup> entityGroupOptional =
                        entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, customer.getId(), groupType, EntityGroup.GROUP_ALL_NAME);
                if (!entityGroupOptional.isPresent()) {
                    EntityGroup entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, customer.getId(), groupType);
                    if (groupType == EntityType.USER) {
                        if (!customer.isPublic()) {
                            entityGroupService.findOrCreateCustomerAdminsGroup(customer.getTenantId(), customer.getId(), null);
                            Optional<EntityGroup> customerUsersOptional =
                                    entityGroupService.findEntityGroupByTypeAndName(customer.getTenantId(), customer.getId(), EntityType.USER, EntityGroup.GROUP_CUSTOMER_USERS_NAME);
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
        protected String getName() {
            return "Dashboards group all updater";
        }

        @Override
        protected PageData<DashboardInfo> findEntities(TenantId id, PageLink pageLink) {
            if (fetchAllTenantEntities) {
                return dashboardService.findDashboardsByTenantId(id, pageLink);
            } else {
                try {
                    List<EntityId> entityIds = entityGroupService.findAllEntityIdsAsync(TenantId.SYS_TENANT_ID, groupAll.getId(), new PageLink(Integer.MAX_VALUE)).get();
                    List<DashboardId> dashboardIds = entityIds.stream().map(entityId -> new DashboardId(entityId.getId())).collect(Collectors.toList());
                    List<DashboardInfo> dashboards;
                    if (!dashboardIds.isEmpty()) {
                        dashboards = dashboardService.findDashboardInfoByIdsAsync(TenantId.SYS_TENANT_ID, dashboardIds).get();
                    } else {
                        dashboards = Collections.emptyList();
                    }
                    return new PageData<>(dashboards, 1, dashboards.size(), false);
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

    private PaginatedUpdater<String, Tenant> tenantIntegrationUpdater = new PaginatedUpdater<String, Tenant>() {
        @Override
        protected PageData<Tenant> findEntities(String id, PageLink pageLink) {
            return tenantService.findTenants(pageLink);
        }

        @Override
        protected String getName() {
            return "Tenant integration updater";
        }

        @Override
        protected void updateEntity(Tenant tenant) {
            updateTenantIntegrations(tenant.getId());
        }
    };

    private void updateTenantIntegrations(TenantId tenantId) {
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        PageData<Integration> pageData = integrationService.findTenantIntegrations(tenantId, pageLink);
        boolean hasNext = true;
        while (hasNext) {
            for (Integration integration : pageData.getData()) {
                try {
                    Field enabledField = AbstractIntegration.class.getDeclaredField("enabled");
                    enabledField.setAccessible(true);
                    Boolean booleanVal = (Boolean) enabledField.get(integration);
                    if (booleanVal == null) {
                        integration.setEnabled(true);
                        integrationService.saveIntegration(integration);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
                pageData = integrationService.findTenantIntegrations(tenantId, pageLink);
            } else {
                hasNext = false;
            }
        }
    }

    boolean convertDeviceProfileAlarmRulesForVersion330(JsonNode spec) {
        if (spec != null) {
            if (spec.has("type") && spec.get("type").asText().equals("DURATION")) {
                if (spec.has("value")) {
                    long value = spec.get("value").asLong();
                    var predicate = new FilterPredicateValue<>(
                            value, null, new DynamicValue<>(null, null, false)
                    );
                    ((ObjectNode) spec).remove("value");
                    ((ObjectNode) spec).putPOJO("predicate", predicate);
                    return true;
                }
            } else if (spec.has("type") && spec.get("type").asText().equals("REPEATING")) {
                if (spec.has("count")) {
                    int count = spec.get("count").asInt();
                    var predicate = new FilterPredicateValue<>(
                            count, null, new DynamicValue<>(null, null, false)
                    );
                    ((ObjectNode) spec).remove("count");
                    ((ObjectNode) spec).putPOJO("predicate", predicate);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean getEnv(String name, boolean defaultValue) {
        String env = System.getenv(name);
        if (env == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(env);
        }
    }

}
