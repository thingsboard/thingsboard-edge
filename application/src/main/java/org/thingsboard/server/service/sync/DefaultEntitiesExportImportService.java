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
package org.thingsboard.server.service.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.sync.exporting.EntityExportService;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.request.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.impl.BaseEntityExportService;
import org.thingsboard.server.service.sync.exporting.impl.DefaultEntityExportService;
import org.thingsboard.server.service.sync.importing.EntityImportService;
import org.thingsboard.server.service.sync.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.importing.data.EntityImportSettings;
import org.thingsboard.server.utils.ThrowingRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultEntitiesExportImportService implements EntitiesExportImportService, ExportableEntitiesService {

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();
    private final Map<EntityType, Dao<?>> daos = new HashMap<>();

    private final AccessControlService accessControlService;
    private final OwnersCacheService ownersCacheService;

    protected static final List<EntityType> SUPPORTED_ENTITY_TYPES = List.of(
            EntityType.CUSTOMER, EntityType.ENTITY_GROUP, EntityType.ASSET, EntityType.RULE_CHAIN,
            EntityType.DEVICE_PROFILE, EntityType.DEVICE, EntityType.DASHBOARD, EntityType.CONVERTER,
            EntityType.INTEGRATION, EntityType.ROLE
    );


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(SecurityUser user, I entityId, EntityExportSettings exportSettings) throws ThingsboardException {
        EntityType entityType = entityId.getEntityType();
        EntityExportService<I, E, EntityExportData<E>> exportService = getExportService(entityType);

        return exportService.getExportData(user, entityId, exportSettings);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<EntityImportResult<ExportableEntity<EntityId>>> importEntities(SecurityUser user, List<EntityExportData<ExportableEntity<EntityId>>> exportDataList, EntityImportSettings importSettings) throws ThingsboardException {
        exportDataList.sort(Comparator.comparing(exportData -> SUPPORTED_ENTITY_TYPES.indexOf(exportData.getEntityType())));
        List<EntityImportResult<ExportableEntity<EntityId>>> importResults = new ArrayList<>();

        for (EntityExportData<ExportableEntity<EntityId>> exportData : exportDataList) {
            EntityImportResult<ExportableEntity<EntityId>> importResult = importEntity(user, exportData, importSettings);
            importResults.add(importResult);
        }

        for (ThrowingRunnable saveReferencesCallback : importResults.stream()
                .map(EntityImportResult::getSaveReferencesCallback)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())) {
            saveReferencesCallback.run();
        }

        return importResults;
    }

    private <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(SecurityUser user, EntityExportData<E> exportData, EntityImportSettings importSettings) throws ThingsboardException {
        if (exportData.getEntity() == null || exportData.getEntity().getId() == null) {
            throw new DataValidationException("Invalid entity data");
        }

        EntityType entityType = exportData.getEntityType();
        EntityImportService<I, E, EntityExportData<E>> importService = getImportService(entityType);

        return importService.importEntity(user, exportData, importSettings);
    }


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(TenantId tenantId, I externalId) {
        EntityType entityType = externalId.getEntityType();
        if (SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            ExportableEntityDao<E> dao = (ExportableEntityDao<E>) getDao(entityType);
            return dao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId());
        } else {
            return null;
        }
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(TenantId tenantId, I id) {
        Dao<E> dao = (Dao<E>) getDao(id.getEntityType());
        E entity = dao.findById(tenantId, id.getId());

        if (entity instanceof HasOwnerId) {
            if (ownersCacheService.getOwners(tenantId, entity.getId(), (HasOwnerId) entity).contains(tenantId)) {
                return entity;
            }
        } else if (entity instanceof HasTenantId) {
            if (((HasTenantId) entity).getTenantId().equals(tenantId)) {
                return entity;
            }
        } else if (Objects.equals(ownersCacheService.getOwner(tenantId, entity.getId()), tenantId)) {
            return entity;
        }

        return null;
}

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(TenantId tenantId, EntityType entityType, String name) {
        ExportableEntityDao<E> dao = (ExportableEntityDao<E>) getDao(entityType);
        return dao.findFirstByTenantIdAndName(tenantId.getId(), name);
    }


    @Override
    public void checkPermission(SecurityUser user, HasId<? extends EntityId> entity, EntityType entityType, Operation operation) throws ThingsboardException {
        if (entity instanceof TenantEntity) {
            accessControlService.checkPermission(user, Resource.resourceFromEntityType(entityType), operation, entity.getId(), (TenantEntity) entity);
        } else if (entity instanceof EntityGroup) {
            accessControlService.checkEntityGroupPermission(user, operation, (EntityGroup) entity);
        } else if (entity != null) {
            accessControlService.checkPermission(user, Resource.resourceFromEntityType(entityType), operation);
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
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityExportService<I, E, D> getExportService(EntityType entityType) {
        if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("Export for entity type " + entityType + " is not supported");
        }
        return (EntityExportService<I, E, D>) exportServices.get(entityType);
    }

    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityImportService<I, E, D> getImportService(EntityType entityType) {
        if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("Import for entity type " + entityType + " is not supported");
        }
        return (EntityImportService<I, E, D>) importServices.get(entityType);
    }

    private Dao<?> getDao(EntityType entityType) {
        return daos.get(entityType);
    }


    @Autowired
    private void setExportServices(DefaultEntityExportService<?, ?, ?> defaultExportService,
                                   Collection<BaseEntityExportService<?, ?, ?>> exportServices) {
        exportServices.stream()
                .sorted(Comparator.comparing(exportService -> exportService.getSupportedEntityTypes().size(), Comparator.reverseOrder()))
                .forEach(exportService -> {
                    exportService.getSupportedEntityTypes().forEach(entityType -> {
                        this.exportServices.put(entityType, exportService);
                    });
                });
        SUPPORTED_ENTITY_TYPES.forEach(entityType -> {
            this.exportServices.putIfAbsent(entityType, defaultExportService);
        });
    }

    @Autowired
    private void setImportServices(Collection<EntityImportService<?, ?, ?>> importServices) {
        importServices.forEach(entityImportService -> {
            this.importServices.put(entityImportService.getEntityType(), entityImportService);
        });
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
