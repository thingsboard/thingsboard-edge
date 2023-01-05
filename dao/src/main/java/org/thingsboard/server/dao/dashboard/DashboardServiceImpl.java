/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.dashboard;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.extractConstraintViolationException;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("DashboardDaoService")
@Slf4j
public class DashboardServiceImpl extends AbstractEntityService implements DashboardService {

    public static final String INCORRECT_DASHBOARD_ID = "Incorrect dashboardId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private EdgeDao edgeDao;

    @Autowired
    private DataValidator<Dashboard> dashboardValidator;

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
    public Dashboard saveDashboard(Dashboard dashboard, boolean doValidate) {
        return doSaveDashboard(dashboard, doValidate);
    }

    @Override
    public Dashboard saveDashboard(Dashboard dashboard) {
        return doSaveDashboard(dashboard, true);
    }

    private Dashboard doSaveDashboard(Dashboard dashboard, boolean doValidate) {
        log.trace("Executing saveDashboard [{}]", dashboard);
        if (doValidate) {
            dashboardValidator.validate(dashboard, DashboardInfo::getTenantId);
        }
        try {
            Dashboard savedDashboard = dashboardDao.save(dashboard.getTenantId(), dashboard);
            if (dashboard.getId() == null) {
                entityGroupService.addEntityToEntityGroupAll(savedDashboard.getTenantId(), savedDashboard.getOwnerId(), savedDashboard.getId());
            }
            return savedDashboard;
        } catch (Exception e) {
            checkConstraintViolation(e, "dashboard_external_id_unq_key", "Dashboard with such external id already exists!");
            throw e;
        }
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
            } catch (Exception e) {
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
            } catch (Exception e) {
                log.warn("[{}] Failed to delete dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    @Override
    @Transactional
    public void deleteDashboard(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing deleteDashboard [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        deleteEntityRelations(tenantId, dashboardId);
        try {
            dashboardDao.removeById(tenantId, dashboardId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_dashboard_device_profile")) {
                throw new DataValidationException("The dashboard referenced by the device profiles cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public void deleteDashboardsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDashboardsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDashboardsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        customerDashboardsRemover.removeEntities(tenantId, customerId);
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

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByEntityGroupIds, groupIds [{}], pageLink [{}]", groupIds, pageLink);
        validateIds(groupIds, "Incorrect groupIds " + groupIds);
        validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByEntityGroupIds(toUUIDs(groupIds), pageLink);
    }

    @Override
    public DashboardInfo findFirstDashboardInfoByTenantIdAndName(TenantId tenantId, String name) {
        return dashboardInfoDao.findFirstByTenantIdAndName(tenantId.getId(), name);
    }

    @Override
    public List<Dashboard> findTenantDashboardsByTitle(TenantId tenantId, String title) {
        return dashboardDao.findByTenantIdAndTitle(tenantId.getId(), title);
    }

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

    private PaginatedRemover<CustomerId, DashboardInfo> customerDashboardsRemover =
            new PaginatedRemover<CustomerId, DashboardInfo>() {

                @Override
                protected PageData<DashboardInfo> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
                    return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, DashboardInfo entity) {
                    deleteDashboard(tenantId, new DashboardId(entity.getUuidId()));
                }
            };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findDashboardById(tenantId, new DashboardId(entityId.getId())));
    }

    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

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
        Map<DashboardId, DashboardId> idMapping = new HashMap<>();
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
        Map<DashboardId, DashboardId> idMapping = new HashMap<>();
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
