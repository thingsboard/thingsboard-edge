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
package org.thingsboard.server.service.sync.importing.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;
import org.thingsboard.server.service.sync.importing.EntityImportService;
import org.thingsboard.server.service.sync.importing.EntityImportSettings;
import org.thingsboard.server.utils.ThrowingRunnable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseEntityImportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityImportService<I, E, D> {

    @Autowired @Lazy
    protected ExportableEntitiesService exportableEntitiesService;
    @Autowired
    private RelationService relationService;
    @Autowired
    protected EntityActionService entityActionService;
    @Autowired
    protected TbClusterService clusterService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public EntityImportResult<E> importEntity(SecurityUser user, D exportData, EntityImportSettings importSettings) throws ThingsboardException {
        E entity = exportData.getEntity();
        E existingEntity = findExistingEntity(user.getTenantId(), entity, importSettings);

        entity.setExternalId(entity.getId());

        NewIdProvider idProvider = new NewIdProvider(user, entity, existingEntity, importSettings);
        setOwner(user.getTenantId(), entity, idProvider);
        if (existingEntity == null) {
            entity.setId(null);
            exportableEntitiesService.checkPermission(user, entity, getEntityType(), Operation.CREATE);
        } else {
            entity.setId(existingEntity.getId());
            exportableEntitiesService.checkPermission(user, existingEntity, getEntityType(), Operation.WRITE);
        }

        E savedEntity = prepareAndSave(user.getTenantId(), entity, exportData, idProvider);
        ThrowingRunnable callback = processAfterSavedAndGetCallback(user, savedEntity, existingEntity, exportData, importSettings, idProvider);

        EntityImportResult<E> importResult = new EntityImportResult<>();
        importResult.setSavedEntity(savedEntity);
        importResult.setOldEntity(existingEntity);
        importResult.setCallback(callback);
        return importResult;
    }

    protected abstract void setOwner(TenantId tenantId, E entity, NewIdProvider idProvider);

    protected abstract E prepareAndSave(TenantId tenantId, E entity, D exportData, NewIdProvider idProvider);

    protected ThrowingRunnable processAfterSavedAndGetCallback(SecurityUser user, E savedEntity, E oldEntity, D exportData,
                                                               EntityImportSettings importSettings, NewIdProvider idProvider) throws ThingsboardException {
        List<EntityRelation> newRelations = new LinkedList<>();

        if (importSettings.isImportInboundRelations() && CollectionUtils.isNotEmpty(exportData.getInboundRelations())) {
            newRelations.addAll(exportData.getInboundRelations().stream()
                    .peek(relation -> relation.setTo(savedEntity.getId()))
                    .collect(Collectors.toList()));

            if (importSettings.isRemoveExistingRelations() && oldEntity != null) {
                for (EntityRelation existingRelation : relationService.findByTo(user.getTenantId(), savedEntity.getId(), RelationTypeGroup.COMMON)) {
                    exportableEntitiesService.checkPermission(user, existingRelation.getFrom(), Operation.WRITE);
                    relationService.deleteRelation(user.getTenantId(), existingRelation);
                }
            }
        }
        if (importSettings.isImportOutboundRelations() && CollectionUtils.isNotEmpty(exportData.getOutboundRelations())) {
            newRelations.addAll(exportData.getOutboundRelations().stream()
                    .peek(relation -> relation.setFrom(savedEntity.getId()))
                    .collect(Collectors.toList()));

            if (importSettings.isRemoveExistingRelations() && oldEntity != null) {
                for (EntityRelation existingRelation : relationService.findByFrom(user.getTenantId(), savedEntity.getId(), RelationTypeGroup.COMMON)) {
                    exportableEntitiesService.checkPermission(user, existingRelation.getTo(), Operation.WRITE);
                    relationService.deleteRelation(user.getTenantId(), existingRelation);
                }
            }
        }

        for (EntityRelation relation : newRelations) {
            HasId<EntityId> otherEntity = null;
            if (!relation.getTo().equals(savedEntity.getId())) {
                otherEntity = findInternalEntity(user.getTenantId(), relation.getTo());
                relation.setTo(otherEntity.getId());
            }
            if (!relation.getFrom().equals(savedEntity.getId())) {
                otherEntity = findInternalEntity(user.getTenantId(), relation.getFrom());
                relation.setFrom(otherEntity.getId());
            }
            if (otherEntity != null) {
                exportableEntitiesService.checkPermission(user, otherEntity, otherEntity.getId().getEntityType(), Operation.WRITE);
            }

            relationService.saveRelation(user.getTenantId(), relation);
        }

        return getCallback(user, savedEntity, oldEntity);
    }

    protected ThrowingRunnable getCallback(SecurityUser user, E savedEntity, E oldEntity) {
        return () -> {
            entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity,
                    savedEntity instanceof HasCustomerId ? ((HasCustomerId) savedEntity).getCustomerId() : user.getCustomerId(),
                    oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, null);
        };
    }


    private E findExistingEntity(TenantId tenantId, E entity, EntityImportSettings importSettings) {
        return (E) Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndExternalId(tenantId, entity.getId()))
                .or(() -> Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndId(tenantId, entity.getId())))
                .or(() -> {
                    if (importSettings.isFindExistingByName()) {
                        return Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndName(tenantId, getEntityType(), entity.getName()));
                    } else {
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }

    private <ID extends EntityId> HasId<ID> findInternalEntity(TenantId tenantId, ID externalId) {
        if (externalId == null || externalId.isNullUid()) return null;

        return (HasId<ID>) Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndExternalId(tenantId, externalId))
                .or(() -> Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndId(tenantId, externalId)))
                .orElseThrow(() -> new IllegalArgumentException("Cannot find " + externalId.getEntityType() + " by external id " + externalId));
    }


    @RequiredArgsConstructor
    protected class NewIdProvider {
        private final SecurityUser user;
        private final E entity;
        private final E existingEntity;
        private final EntityImportSettings importSettings;

        private final Set<EntityType> ALWAYS_UPDATE_REFERENCED_IDS = Set.of(
                EntityType.RULE_CHAIN
        );

        private final Set<EntityType> NEVER_UPDATE_REFERENCED_IDS = Set.of(
                EntityType.ENTITY_GROUP
        );

        public <ID extends EntityId> ID get(Function<E, ID> idExtractor) {
            if (existingEntity != null) {
                if ((!importSettings.isUpdateReferencesToOtherEntities()
                        && !ALWAYS_UPDATE_REFERENCED_IDS.contains(getEntityType()))
                        || NEVER_UPDATE_REFERENCED_IDS.contains(getEntityType())) {
                    return idExtractor.apply(existingEntity);
                }
            }
            return getInternalId(idExtractor.apply(this.entity));
        }

        public <ID extends EntityId, T> Set<T> get(Function<E, Set<T>> listExtractor, Function<T, ID> idGetter, BiConsumer<T, ID> idSetter) {
            if (existingEntity == null || importSettings.isUpdateReferencesToOtherEntities()) {
                return Optional.ofNullable(listExtractor.apply(entity)).orElse(Collections.emptySet()).stream()
                        .peek(t -> {
                            idSetter.accept(t, getInternalId(idGetter.apply(t)));
                        })
                        .collect(Collectors.toSet());
            } else {
                return listExtractor.apply(existingEntity);
            }
        }

        private <ID extends EntityId> ID getInternalId(ID externalId) {
            HasId<ID> entity = findInternalEntity(user.getTenantId(), externalId);
            if (entity != null) {
                try {
                    exportableEntitiesService.checkPermission(user, entity, entity.getId().getEntityType(), Operation.READ);
                } catch (ThingsboardException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
                return entity.getId();
            } else {
                return null;
            }
        }

    }

}
