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
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.EntitiesExportImportService;
import org.thingsboard.server.service.sync.exporting.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;
import org.thingsboard.server.service.sync.importing.EntityImportSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;

@RestController
@RequestMapping("/api/entities")
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
@TbCoreComponent
@RequiredArgsConstructor
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;
    private final EntityService entityService;


    @PostMapping("/export/{entityType}/{id}")
    public EntityExportData<ExportableEntity<EntityId>> exportSingleEntity(@PathVariable EntityType entityType,
                                                                           @PathVariable UUID id,
                                                                           @RequestParam Map<String, String> exportSettingsParams) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, id);
        try {
            return exportEntity(user, entityId, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping(value = "/export/{entityType}", params = "ids")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByIds(@PathVariable EntityType entityType,
                                                                                  @RequestParam UUID[] ids,
                                                                                  @RequestParam Map<String, String> exportSettingsParams) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        List<EntityId> entitiesIds = Arrays.stream(ids)
                .map(id -> EntityIdFactory.getByTypeAndUuid(entityType, id))
                .collect(Collectors.toList());
        try {
            return exportEntitiesByIds(user, entitiesIds, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping(value = "/export/{entityType}")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByEntityType(@PathVariable EntityType entityType,
                                                                                         @RequestParam Map<String, String> exportSettingsParams,
                                                                                         @RequestParam(defaultValue = "0") int page,
                                                                                         @RequestParam(defaultValue = "2147483647") int pageSize,
                                                                                         @RequestParam(name = "customerId", required = false) UUID customerUuid) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        CustomerId customerId = toCustomerId(customerUuid);

        EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
        entityTypeFilter.setEntityType(entityType);
        try {
            return exportEntitiesByFilter(user, customerId, entityTypeFilter, page, pageSize, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping("/exportByFilter")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByFilter(@RequestBody EntityFilter filter,
                                                                                     @RequestParam Map<String, String> exportSettingsParams,
                                                                                     @RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "2147483647") int pageSize,
                                                                                     @RequestParam(name = "customerId", required = false) UUID customerUuid) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        CustomerId customerId = toCustomerId(customerUuid);
        try {
            return exportEntitiesByFilter(user, customerId, filter, page, pageSize, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    // FIXME: too aggressive
    @PostMapping("/exportByFilters")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportAllEntitiesByFilters(@RequestBody List<EntityFilter> filters,
                                                                                         @RequestParam Map<String, String> exportSettingsParams,
                                                                                         @RequestParam(name = "customerId", required = false) UUID customerUuid) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        CustomerId customerId = toCustomerId(customerUuid);
        EntityExportSettings exportSettings = toExportSettings(exportSettingsParams);
        try {
            List<EntityExportData<ExportableEntity<EntityId>>> exportDataList = new ArrayList<>();
            for (EntityFilter filter : filters) {
                exportDataList.addAll(exportEntitiesByFilter(user, customerId, filter, 0, Integer.MAX_VALUE, exportSettings));
            }
            return exportDataList;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping("/exportByQuery")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByQuery(@RequestBody EntityDataQuery entitiesQuery,
                                                                                    @RequestParam Map<String, String> exportSettingsParams,
                                                                                    @RequestParam(name = "customerId", required = false) UUID customerUuid) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        CustomerId customerId = toCustomerId(customerUuid);
        try {
            return exportEntitiesByQuery(user, customerId, entitiesQuery, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    private List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByFilter(SecurityUser user, CustomerId customerId, EntityFilter filter, int page, int pageSize, EntityExportSettings exportSettings) throws ThingsboardException {
        EntityDataPageLink pageLink = new EntityDataPageLink();
        pageLink.setPage(page);
        pageLink.setPageSize(pageSize);
        EntityKey sortProperty = new EntityKey(EntityKeyType.ENTITY_FIELD, CREATED_TIME);
        pageLink.setSortOrder(new EntityDataSortOrder(sortProperty, EntityDataSortOrder.Direction.DESC));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, List.of(sortProperty), Collections.emptyList(), Collections.emptyList());
        return exportEntitiesByQuery(user, customerId, query, exportSettings);
    }

    private List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByQuery(SecurityUser user, CustomerId customerId, EntityDataQuery query, EntityExportSettings exportSettings) throws ThingsboardException {
        List<EntityId> entitiesIds = entityService.findEntityDataByQuery(user.getTenantId(), customerId, user.getUserPermissions(), query).getData().stream()
                .map(EntityData::getEntityId)
                .collect(Collectors.toList());
        return exportEntitiesByIds(user, entitiesIds, exportSettings);
    }

    private List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByIds(SecurityUser user, List<EntityId> entitiesIds, EntityExportSettings exportSettings) throws ThingsboardException {
        List<EntityExportData<ExportableEntity<EntityId>>> exportDataList = new ArrayList<>();
        for (EntityId entityId : entitiesIds) {
            exportDataList.add(exportEntity(user, entityId, exportSettings));
        }
        return exportDataList;
    }

    private <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(SecurityUser user, I entityId, EntityExportSettings exportSettings) throws ThingsboardException {
        return exportImportService.exportEntity(user, entityId, exportSettings);
    }


    @PostMapping("/import")
    public List<EntityImportResult<ExportableEntity<EntityId>>> importEntities(@RequestBody List<EntityExportData<ExportableEntity<EntityId>>> exportDataList,
                                                                               @RequestParam Map<String, String> importSettingsParams) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        EntityImportSettings importSettings = toImportSettings(importSettingsParams);

        try {
            List<EntityImportResult<ExportableEntity<EntityId>>> importResults = importEntities(user, exportDataList, importSettings);
            for (EntityImportResult<ExportableEntity<EntityId>> entityImportResult : importResults) {
                if (entityImportResult.getCallback() != null) {
                    entityImportResult.getCallback().run();
                }
            }
            return importResults;
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    public List<EntityImportResult<ExportableEntity<EntityId>>> importEntities(SecurityUser user, List<EntityExportData<ExportableEntity<EntityId>>> exportDataList, EntityImportSettings importSettings) throws ThingsboardException {
        return exportImportService.importEntities(user, exportDataList, importSettings);
    }


    private EntityExportSettings toExportSettings(Map<String, String> exportSettingsParams) {
        return EntityExportSettings.builder()
                .exportInboundRelations(getBooleanParam(exportSettingsParams, "exportInboundRelations", false))
                .exportOutboundRelations(getBooleanParam(exportSettingsParams, "exportOutboundRelations", false))
                .build();
    }

    private EntityImportSettings toImportSettings(Map<String, String> importSettingsParams) {
        return EntityImportSettings.builder()
                .findExistingByName(getBooleanParam(importSettingsParams, "findExistingByName", false))
                .importInboundRelations(getBooleanParam(importSettingsParams, "importInboundRelations", false))
                .importOutboundRelations(getBooleanParam(importSettingsParams, "importOutboundRelations", false))
                .removeExistingRelations(getBooleanParam(importSettingsParams, "removeExistingRelations", true))
                .updateReferencesToOtherEntities(getBooleanParam(importSettingsParams, "updateReferencesToOtherEntities", true))
                .build();
    }


    protected static boolean getBooleanParam(Map<String, String> requestParams, String key, boolean defaultValue) {
        return getParam(requestParams, key, defaultValue, Boolean::parseBoolean);
    }

    protected static <T> T getParam(Map<String, String> requestParams, String key, T defaultValue, Function<String, T> parsingFunction) {
        return parsingFunction.apply(requestParams.getOrDefault(key, defaultValue.toString()));
    }

    private static CustomerId toCustomerId(UUID customerUuid) {
        return new CustomerId(Optional.ofNullable(customerUuid).orElse(EntityId.NULL_UUID));
    }

}
