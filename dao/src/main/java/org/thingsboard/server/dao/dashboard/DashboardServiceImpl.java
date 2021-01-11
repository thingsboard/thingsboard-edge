/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.dashboard;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class DashboardServiceImpl extends AbstractEntityService implements DashboardService {

    public static final String INCORRECT_DASHBOARD_ID = "Incorrect dashboardId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    public Dashboard findDashboardById(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardById [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardDao.findById(tenantId, dashboardId.getId());
    }

    @Override
    public ListenableFuture<Dashboard> findDashboardByIdAsync(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardByIdAsync [{}]", dashboardId);
        validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardDao.findByIdAsync(tenantId, dashboardId.getId());
    }

    @Override
    public DashboardInfo findDashboardInfoById(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoById [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardInfoDao.findById(tenantId, dashboardId.getId());
    }

    @Override
    public ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoByIdAsync [{}]", dashboardId);
        validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardInfoDao.findByIdAsync(tenantId, dashboardId.getId());
    }

    @Override
    public ListenableFuture<List<DashboardInfo>> findDashboardInfoByIdsAsync(TenantId tenantId, List<DashboardId> dashboardIds) {
        log.trace("Executing findDashboardInfoByIdsAsync, dashboardIds [{}]", dashboardIds);
        validateIds(dashboardIds, "Incorrect dashboardIds " + dashboardIds);
        return dashboardInfoDao.findDashboardsByIdsAsync(tenantId.getId(), toUUIDs(dashboardIds));
    }

    @Override
    public Dashboard saveDashboard(Dashboard dashboard) {
        log.trace("Executing saveDashboard [{}]", dashboard);
        dashboardValidator.validate(dashboard, DashboardInfo::getTenantId);
        Dashboard savedDashboard = dashboardDao.save(dashboard.getTenantId(), dashboard);
        if (dashboard.getId() == null) {
            entityGroupService.addEntityToEntityGroupAll(savedDashboard.getTenantId(), savedDashboard.getOwnerId(), savedDashboard.getId());
        }
        return savedDashboard;
    }

    @Override
    public Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't assign dashboard to non-existent customer!");
        }
        if (!customer.getTenantId().getId().equals(dashboard.getTenantId().getId())) {
            throw new DataValidationException("Can't assign dashboard to customer from different tenant!");
        }
        if (dashboard.addAssignedCustomer(customer)) {
            try {
                createRelation(tenantId, new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to create dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    @Override
    public Dashboard unassignDashboardFromCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't unassign dashboard from non-existent customer!");
        }
        if (dashboard.removeAssignedCustomer(customer)) {
            try {
                deleteRelation(tenantId, new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to delete dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    private void deleteRelation(TenantId tenantId, EntityRelation dashboardRelation) throws ExecutionException, InterruptedException {
        log.debug("Deleting Dashboard relation: {}", dashboardRelation);
        relationService.deleteRelationAsync(tenantId, dashboardRelation).get();
    }

    private void createRelation(TenantId tenantId, EntityRelation dashboardRelation) throws ExecutionException, InterruptedException {
        log.debug("Creating Dashboard relation: {}", dashboardRelation);
        relationService.saveRelationAsync(tenantId, dashboardRelation).get();
    }

    @Override
    public void deleteDashboard(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing deleteDashboard [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        deleteEntityRelations(tenantId, dashboardId);
        dashboardDao.removeById(tenantId, dashboardId.getId());
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteDashboardsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDashboardsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDashboardsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        try {
            List<DashboardId> dashboardIds = fetchCustomerDashboards(tenantId, customerId);
            for (DashboardId dashboardId : dashboardIds) {
                deleteDashboard(tenantId, dashboardId);
            }
        } catch (Exception e) {
            log.error("Failed to delete dashboards", e);
            throw new RuntimeException(e);
        }
    }

    private List<DashboardId> fetchCustomerDashboards(TenantId tenantId, CustomerId customerId) throws Exception {
        List<DashboardId> dashboardIds = new ArrayList<>();
        Optional<EntityGroup> entityGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, customerId, EntityType.DASHBOARD, EntityGroup.GROUP_ALL_NAME).get();
        if (entityGroup.isPresent()) {
            List<EntityId> childDashboardIds = entityGroupService.findAllEntityIds(tenantId, entityGroup.get().getId(), new PageLink(Integer.MAX_VALUE)).get();
            childDashboardIds.forEach(entityId -> dashboardIds.add(new DashboardId(entityId.getId())));
        }
        return dashboardIds;
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByEntityGroupId(EntityGroupId groupId, PageLink pageLink) {
        log.trace("Executing findDashboardsByEntityGroupId, groupId [{}], pageLink [{}]", groupId, pageLink);
        validateId(groupId, "Incorrect entityGroupId " + groupId);
        validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByEntityGroupId(groupId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink) {
        log.trace("Executing findDashboardsByEntityGroupIds, groupIds [{}], pageLink [{}]", groupIds, pageLink);
        validateIds(groupIds, "Incorrect groupIds " + groupIds);
        validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByEntityGroupIds(toUUIDs(groupIds), pageLink);
    }

    private DataValidator<Dashboard> dashboardValidator =
            new DataValidator<Dashboard>() {
                @Override
                protected void validateCreate(TenantId tenantId, Dashboard data) {
                    DefaultTenantProfileConfiguration profileConfiguration =
                            (DefaultTenantProfileConfiguration)tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
                    long maxDashboards = profileConfiguration.getMaxDashboards();
                    validateNumberOfEntitiesPerTenant(tenantId, dashboardDao, maxDashboards, EntityType.DASHBOARD);
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Dashboard dashboard) {
                    if (StringUtils.isEmpty(dashboard.getTitle())) {
                        throw new DataValidationException("Dashboard title should be specified!");
                    }
                    if (dashboard.getTenantId() == null) {
                        throw new DataValidationException("Dashboard should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, dashboard.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Dashboard is referencing to non-existent tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, DashboardInfo> tenantDashboardsRemover =
            new PaginatedRemover<TenantId, DashboardInfo>() {

                @Override
                protected PageData<DashboardInfo> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return dashboardInfoDao.findDashboardsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, DashboardInfo entity) {
                    deleteDashboard(tenantId, new DashboardId(entity.getUuidId()));
                }
    };

    @Override
    public List<Dashboard> exportDashboards(TenantId tenantId, EntityGroupId entityGroupId, TimePageLink pageLink) throws ThingsboardException {
        PageData<DashboardInfo> pageData = findDashboardsByEntityGroupId(entityGroupId, pageLink);
        if (pageData != null && !CollectionUtils.isEmpty(pageData.getData())) {
            List<DashboardInfo> dashboardViews = pageData.getData();
            Map<DashboardId, DashboardId> idMapping = new HashMap<>();
            List<Dashboard> dashboards = new ArrayList<>();
            for (DashboardInfo dashboardInfo : dashboardViews) {
                Dashboard dashboard = findDashboardById(tenantId, dashboardInfo.getId());
                DashboardId oldDashboardId = dashboard.getId();
                DashboardId newDashboardId = new DashboardId(Uuids.timeBased());
                idMapping.put(oldDashboardId, newDashboardId);
                dashboard.setId(newDashboardId);
                dashboard.setTenantId(null);
                dashboard.setCustomerId(null);
                dashboards.add(dashboard);
            }
            for (Dashboard dashboard : dashboards) {
                JsonNode configuration = dashboard.getConfiguration();
                searchDashboardIdRecursive(idMapping, configuration);
            }
            return dashboards;
        }
        return Collections.emptyList();
    }

    private void searchDashboardIdRecursive(Map<DashboardId, DashboardId> idMapping, JsonNode node) throws ThingsboardException {
        Iterator<String> iter = node.fieldNames();
        boolean isDashboardId = false;
        try {
            while (iter.hasNext()) {
                String field = iter.next();
                if ("targetDashboardId".equals(field)) {
                    isDashboardId = true;
                    break;
                }
            }
            if (isDashboardId) {
                ObjectNode objNode = (ObjectNode) node;
                String oldDashboardIdStr = node.get("targetDashboardId").asText();
                DashboardId dashboardId = new DashboardId(UUID.fromString(oldDashboardIdStr));
                DashboardId replacement = idMapping.get(dashboardId);
                if (replacement != null) {
                    objNode.put("targetDashboardId", replacement.getId().toString());
                }
            } else {
                Iterator<JsonNode> childIter = node.iterator();
                while (childIter.hasNext()) {
                    searchDashboardIdRecursive(idMapping, childIter.next());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ThingsboardException(e.getMessage(), e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public void importDashboards(TenantId tenantId, EntityGroupId entityGroupId, List<Dashboard> dashboards, boolean overwrite) throws ThingsboardException {
        EntityGroup dashboardGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
        resetDashboardOwnerCustomer(tenantId, dashboardGroup.getOwnerId(), dashboards);
        if (overwrite) {
            PageData<DashboardInfo> dashboardData = findDashboardsByEntityGroupId(entityGroupId, new PageLink(Integer.MAX_VALUE));
            try {
                List<DashboardInfo> dashboardInfos = dashboardData.getData();
                if (!CollectionUtils.isEmpty(dashboardInfos)) {
                    replaceOverwriteDashboardIds(dashboards, dashboardInfos);
                } else {
                    replaceDashboardIds(dashboards);
                }
                dashboards.stream().forEach(d -> saveDashboardToEntityGroup(tenantId, entityGroupId, d));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new ThingsboardException(e.getMessage(), e, ThingsboardErrorCode.GENERAL);
            }
        } else {
            try {
                replaceDashboardIds(dashboards);
                dashboards.stream().forEach(d -> saveDashboardToEntityGroup(tenantId, entityGroupId, d));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new ThingsboardException(e.getMessage(), e, ThingsboardErrorCode.GENERAL);
            }
        }
    }

    private void saveDashboardToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, Dashboard dashboard) {
        Dashboard savedDashboard = saveDashboard(dashboard);
        entityGroupService.addEntityToEntityGroupAll(savedDashboard.getTenantId(), savedDashboard.getOwnerId(), savedDashboard.getId());
        entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, savedDashboard.getId());
    }

    private void replaceOverwriteDashboardIds(List<Dashboard> dashboards, List<DashboardInfo> persistentDashboards) throws Exception {
        Map<DashboardId, DashboardId> idMapping = new HashMap();
        for (Dashboard dashboard : dashboards) {
            Optional<DashboardInfo> overwriteDashboardInfoOpt = persistentDashboards.stream().filter(d -> d.getTitle().equals(dashboard.getTitle())).findAny();
            DashboardId importDashboardId = dashboard.getId();
            DashboardId overwriteDashboardId;
            if (overwriteDashboardInfoOpt.isPresent()) {
                overwriteDashboardId = overwriteDashboardInfoOpt.get().getId();
            } else {
                overwriteDashboardId = new DashboardId(Uuids.timeBased());
            }
            idMapping.put(importDashboardId, overwriteDashboardId);
            dashboard.setId(overwriteDashboardId);
        }
        for (Dashboard dashboard : dashboards) {
            JsonNode configuration = dashboard.getConfiguration();
            searchDashboardIdRecursive(idMapping, configuration);
        }
    }

    private void resetDashboardOwnerCustomer(TenantId tenantId, EntityId ownerId, List<Dashboard> dashboards) {
        for (Dashboard dashboard : dashboards) {
            dashboard.setTenantId(tenantId);
            dashboard.setOwnerId(ownerId);
        }
    }

    List<Dashboard> replaceDashboardIds(List<Dashboard> dashboards) throws Exception {
        Map<DashboardId, DashboardId> idMapping = new HashMap();
        for (Dashboard dashboard : dashboards) {
            if (dashboard.getId() != null) {
                DashboardId oldId = dashboard.getId();
                DashboardId newId = new DashboardId(Uuids.timeBased());
                idMapping.put(oldId, newId);
                dashboard.setId(newId);
            } else {
                DashboardId newId = new DashboardId(Uuids.timeBased());
                dashboard.setId(newId);
            }
        }
        for (Dashboard dashboard : dashboards) {
            searchDashboardIdRecursive(idMapping, dashboard.getConfiguration());
        }
        return dashboards;
    }
}
