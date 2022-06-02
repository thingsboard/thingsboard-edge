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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.exporting.EntityExportService;
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
@Slf4j
public class DefaultEntitiesExportImportService implements EntitiesExportImportService {

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();

    protected static final List<EntityType> SUPPORTED_ENTITY_TYPES = List.of(
            EntityType.CUSTOMER, EntityType.ENTITY_GROUP, EntityType.ASSET, EntityType.RULE_CHAIN,
            EntityType.DASHBOARD, EntityType.DEVICE_PROFILE, EntityType.DEVICE, EntityType.CONVERTER,
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
    public List<EntityImportResult<?>> importEntities(SecurityUser user, List<EntityExportData<?>> exportDataList, EntityImportSettings importSettings) throws ThingsboardException {
        fixOrder(exportDataList);
        List<EntityImportResult<?>> importResults = new ArrayList<>();

        for (EntityExportData exportData : exportDataList) {
            EntityImportResult<?> importResult = importEntity(user, exportData, importSettings);
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

    private void fixOrder(List<EntityExportData<?>> exportDataList) {
        Map<EntityId, EntityId> entitiesOwners = new HashMap<>();
        exportDataList.stream().map(EntityExportData::getEntity).forEach(entity -> {
            EntityId entityId = entity.getId();
            EntityId ownerId = entity instanceof HasOwnerId ? ((HasOwnerId) entity).getOwnerId() : ((HasTenantId) entity).getTenantId();
            entitiesOwners.put(entityId, ownerId);
        });

        exportDataList.sort(Comparator.<EntityExportData<?>, Integer>comparing(exportData -> SUPPORTED_ENTITY_TYPES.indexOf(exportData.getEntityType()))
                .thenComparing((exportData) -> {
                    if (exportData.getEntityType() == EntityType.CUSTOMER) {
                        return getNestingLevel((CustomerId) exportData.getEntity().getId(), entitiesOwners);
                    } else {
                        return 0;
                    }
                }));
    }

    private int getNestingLevel(CustomerId customerId, Map<EntityId, EntityId> entitiesOwners) {
        EntityId customerOwner = entitiesOwners.get(customerId);
        if (customerOwner == null || customerOwner.getEntityType() == EntityType.TENANT) {
            return 0;
        } else {
            return 1 + getNestingLevel((CustomerId) customerOwner, entitiesOwners);
        }
    }

    private <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(SecurityUser user, EntityExportData<E> exportData, EntityImportSettings importSettings) throws ThingsboardException {
        if (exportData.getEntity() == null || exportData.getEntity().getId() == null) {
            throw new DataValidationException("Invalid entity data");
        }

        EntityType entityType = exportData.getEntityType();
        EntityImportService<I, E, EntityExportData<E>> importService = getImportService(entityType);

        return importService.importEntity(user, exportData, importSettings);
    }


    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityExportService<I, E, D> getExportService(EntityType entityType) {
        EntityExportService<?, ?, ?> exportService = exportServices.get(entityType);
        if (exportService == null) {
            throw new IllegalArgumentException("Export for entity type " + entityType + " is not supported");
        }
        return (EntityExportService<I, E, D>) exportService;
    }

    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityImportService<I, E, D> getImportService(EntityType entityType) {
        EntityImportService<?, ?, ?> importService = importServices.get(entityType);
        if (importService == null) {
            throw new IllegalArgumentException("Import for entity type " + entityType + " is not supported");
        }
        return (EntityImportService<I, E, D>) importService;
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

}
