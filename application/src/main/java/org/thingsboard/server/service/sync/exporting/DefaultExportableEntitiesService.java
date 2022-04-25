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
package org.thingsboard.server.service.sync.exporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.sync.exporting.data.request.CustomEntityFilterExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.CustomEntityQueryExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityListExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityTypeExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.ExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.SingleEntityExportRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultExportableEntitiesService implements ExportableEntitiesService {

    private final Map<EntityType, Dao<?>> daos = new HashMap<>();

    private final EntityService entityService;
    private final OwnersCacheService ownersCacheService;
    private final AccessControlService accessControlService;


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(TenantId tenantId, I externalId) {
        EntityType entityType = externalId.getEntityType();
        Dao<E> dao = getDao(entityType);

        E entity = null;

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<E> exportableEntityDao = (ExportableEntityDao<E>) dao;
            entity = exportableEntityDao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId());
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(TenantId tenantId, I id) {
        EntityType entityType = id.getEntityType();
        Dao<E> dao = getDao(entityType);

        E entity = dao.findById(tenantId, id.getId());

        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(TenantId tenantId, EntityType entityType, String name) {
        Dao<E> dao = getDao(entityType);

        E entity = null;

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<E> exportableEntityDao = (ExportableEntityDao<E>) dao;
            try {
                entity = exportableEntityDao.findByTenantIdAndName(tenantId.getId(), name);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    private boolean belongsToTenant(HasId<? extends EntityId> entity, TenantId tenantId) {
        Set<EntityId> owners;
        if (entity instanceof HasOwnerId) {
            owners = ownersCacheService.getOwners(tenantId, entity.getId(), (HasOwnerId) entity);
        } else {
            owners = Set.of(((HasTenantId) entity).getTenantId());
        }
        return owners.contains(tenantId);
    }


    @Override
    public List<EntityId> findEntitiesForRequest(SecurityUser user, ExportRequest request) {
        switch (request.getType()) {
            case SINGLE_ENTITY: {
                return List.of(((SingleEntityExportRequest) request).getEntityId());
            }
            case ENTITY_LIST: {
                return ((EntityListExportRequest) request).getEntitiesIds();
            }
            case ENTITY_TYPE: {
                EntityTypeExportRequest exportRequest = (EntityTypeExportRequest) request;
                if (exportRequest.getEntityType() == EntityType.ENTITY_GROUP) {
                    throw new IllegalArgumentException("Entity type filter is not applicable for exporting Entity Groups. " +
                            "Use Custom entity filter instead");
                }

                EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
                entityTypeFilter.setEntityType(exportRequest.getEntityType());

                CustomerId customerId = new CustomerId(ObjectUtils.defaultIfNull(exportRequest.getCustomerId(), EntityId.NULL_UUID));
                return findEntitiesByFilter(user, customerId, entityTypeFilter, exportRequest.getPage(), exportRequest.getPageSize());
            }
            case CUSTOM_ENTITY_FILTER: {
                CustomEntityFilterExportRequest exportRequest = (CustomEntityFilterExportRequest) request;
                EntityFilter filter = exportRequest.getFilter();

                CustomerId customerId = new CustomerId(ObjectUtils.defaultIfNull(exportRequest.getCustomerId(), EntityId.NULL_UUID));
                return findEntitiesByFilter(user, customerId, filter, exportRequest.getPage(), exportRequest.getPageSize());
            }
            case CUSTOM_ENTITY_QUERY: {
                CustomEntityQueryExportRequest exportRequest = (CustomEntityQueryExportRequest) request;
                EntityDataQuery query = exportRequest.getQuery();

                CustomerId customerId = new CustomerId(ObjectUtils.defaultIfNull(exportRequest.getCustomerId(), EntityId.NULL_UUID));
                return findEntitiesByQuery(user, customerId, query);
            }
            default: {
                throw new IllegalArgumentException("Export request is not supported");
            }
        }
    }

    private List<EntityId> findEntitiesByFilter(SecurityUser user, CustomerId customerId, EntityFilter filter, int page, int pageSize) {
        EntityDataPageLink pageLink = new EntityDataPageLink();
        pageLink.setPage(page);
        pageLink.setPageSize(pageSize);
        EntityKey sortProperty = new EntityKey(EntityKeyType.ENTITY_FIELD, CREATED_TIME);
        pageLink.setSortOrder(new EntityDataSortOrder(sortProperty, EntityDataSortOrder.Direction.DESC));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, List.of(sortProperty), Collections.emptyList(), Collections.emptyList());
        return findEntitiesByQuery(user, customerId, query);
    }

    private List<EntityId> findEntitiesByQuery(SecurityUser user, CustomerId customerId, EntityDataQuery query) {
        try {
            return entityService.findEntityDataByQuery(user.getTenantId(), customerId, user.getUserPermissions(), query).getData().stream()
                    .map(EntityData::getEntityId)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Failed to find entity data by query: {}", e.getMessage());
            throw new IllegalArgumentException("Entity filter cannot be processed");
        }
    }


    @Override
    public void checkPermission(SecurityUser user, HasId<? extends EntityId> entity, EntityType entityType, Operation operation) throws ThingsboardException {
        Resource resource = Resource.resourceFromEntityType(entityType);
        if (entity instanceof TenantEntity) {
            accessControlService.checkPermission(user, resource, operation, entity.getId(), (TenantEntity) entity);
        } else if (entity instanceof EntityGroup) {
            accessControlService.checkEntityGroupPermission(user, operation, (EntityGroup) entity);
        } else {
            accessControlService.checkPermission(user, resource, operation);
        }
    }

    @Override
    public <E extends TenantEntity & HasId<? extends EntityId>> void checkPermission(SecurityUser user, E entity, EntityGroupId entityGroupId, Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.resourceFromEntityType(entity.getEntityType()), operation, entity.getId(), entity, entityGroupId);
    }

    @Override
    public void checkPermission(SecurityUser user, EntityId entityId, Operation operation) throws ThingsboardException {
        HasId<EntityId> entity = findEntityByTenantIdAndId(user.getTenantId(), entityId);
        checkPermission(user, entity, entityId.getEntityType(), operation);
    }


    @SuppressWarnings("unchecked")
    private <E> Dao<E> getDao(EntityType entityType) {
        return (Dao<E>) daos.get(entityType);
    }

    @Autowired
    private void setDaos(Collection<Dao<?>> daos) {
        daos.forEach(dao -> {
            if (dao.getEntityType() != null) {
                this.daos.put(dao.getEntityType(), dao);
            }
        });
    }

}
