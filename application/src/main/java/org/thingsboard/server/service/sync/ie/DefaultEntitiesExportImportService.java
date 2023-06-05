/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.sync.ie;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.dao.util.limits.LimitedApi;
import org.thingsboard.server.dao.util.limits.RateLimitService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.sync.ie.exporting.EntityExportService;
import org.thingsboard.server.service.sync.ie.exporting.impl.BaseEntityExportService;
import org.thingsboard.server.service.sync.ie.exporting.impl.DefaultEntityExportService;
import org.thingsboard.server.service.sync.ie.importing.EntityImportService;
import org.thingsboard.server.service.sync.ie.importing.MissingEntityException;
import org.thingsboard.server.service.sync.vc.LoadEntityException;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesExportImportService implements EntitiesExportImportService {

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();

    private final RelationService relationService;
    private final RateLimitService rateLimitService;
    private final TbNotificationEntityService entityNotificationService;

    protected static final List<EntityType> SUPPORTED_ENTITY_TYPES = List.of(
            EntityType.CUSTOMER, EntityType.ROLE, EntityType.ENTITY_GROUP, EntityType.ASSET_PROFILE, EntityType.ASSET, EntityType.RULE_CHAIN,
            EntityType.DASHBOARD, EntityType.DEVICE_PROFILE, EntityType.DEVICE, EntityType.ENTITY_VIEW, EntityType.CONVERTER,
            EntityType.INTEGRATION, EntityType.WIDGETS_BUNDLE,
            EntityType.NOTIFICATION_TEMPLATE, EntityType.NOTIFICATION_TARGET, EntityType.NOTIFICATION_RULE
    );

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(EntitiesExportCtx<?> ctx, I entityId) throws ThingsboardException {
        if (!rateLimitService.checkRateLimit(LimitedApi.ENTITY_EXPORT, ctx.getTenantId())) {
            throw new ThingsboardException("Rate limit for entities export is exceeded", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }

        EntityType entityType = entityId.getEntityType();
        EntityExportService<I, E, EntityExportData<E>> exportService = getExportService(entityType);

        return exportService.getExportData(ctx, entityId);
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(EntitiesImportCtx ctx, EntityExportData<E> exportData) throws ThingsboardException {
        if (!rateLimitService.checkRateLimit(LimitedApi.ENTITY_IMPORT, ctx.getTenantId())) {
            throw new ThingsboardException("Rate limit for entities import is exceeded", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }
        if (exportData.getEntity() == null || exportData.getEntity().getId() == null) {
            throw new DataValidationException("Invalid entity data");
        }

        EntityType entityType = exportData.getEntityType();
        EntityImportService<I, E, EntityExportData<E>> importService = getImportService(entityType);

        EntityImportResult<E> importResult = importService.importEntity(ctx, exportData);
        ctx.putInternalId(exportData.getExternalId(), importResult.getSavedEntity().getId());

        ctx.addReferenceCallback(exportData.getExternalId(), importResult.getSaveReferencesCallback());
        ctx.addEventCallback(importResult.getSendEventsCallback());
        return importResult;
    }

    @Override
    public void saveReferencesAndRelations(EntitiesImportCtx ctx) throws ThingsboardException {
        for (Map.Entry<EntityId, ThrowingRunnable> callbackEntry : ctx.getReferenceCallbacks().entrySet()) {
            EntityId externalId = callbackEntry.getKey();
            ThrowingRunnable saveReferencesCallback = callbackEntry.getValue();
            try {
                saveReferencesCallback.run();
            } catch (MissingEntityException e) {
                throw new LoadEntityException(externalId, e);
            }
        }

        relationService.saveRelations(ctx.getTenantId(), new ArrayList<>(ctx.getRelations()));

        for (EntityRelation relation : ctx.getRelations()) {
            entityNotificationService.notifyRelation(ctx.getTenantId(), null,
                    relation, ctx.getUser(), ActionType.RELATION_ADD_OR_UPDATE, relation);
        }
    }

    @Override
    public Comparator<EntityType> getEntityTypeComparatorForImport() {
        return Comparator.comparing(SUPPORTED_ENTITY_TYPES::indexOf);
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
