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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
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
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.request.EntityFilterExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityListExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityQueryExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityTypeExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.ExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.SingleEntityExportRequest;
import org.thingsboard.server.service.sync.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.importing.data.request.ImportRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;

@RestController
@RequestMapping("/api/entities")
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;
    private final EntityService entityService;

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntities(@RequestBody ExportRequest exportRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            return exportEntitiesByRequest(user, exportRequest);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping(value = "/export", params = {"multiple"})
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntities(@RequestBody List<ExportRequest> exportRequests) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            List<EntityExportData<ExportableEntity<EntityId>>> exportDataList = new ArrayList<>();
            for (ExportRequest exportRequest : exportRequests) {
                exportDataList.addAll(exportEntitiesByRequest(user, exportRequest));
            }
            return exportDataList;
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    private List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByRequest(SecurityUser user, ExportRequest request) throws ThingsboardException {
        List<EntityId> entitiesIds = findEntitiesForRequest(user, request);

        List<EntityExportData<ExportableEntity<EntityId>>> exportDataList = new ArrayList<>();
        for (EntityId entityId : entitiesIds) {
            exportDataList.add(exportImportService.exportEntity(user, entityId, request.getExportSettings()));
        }
        return exportDataList;
    }

    private List<EntityId> findEntitiesForRequest(SecurityUser user, ExportRequest request) {
        switch (request.getType()) {
            case SINGLE_ENTITY: {
                return List.of(((SingleEntityExportRequest) request).getEntityId());
            }
            case ENTITY_LIST: {
                return ((EntityListExportRequest) request).getEntitiesIds();
            }
            case ENTITY_TYPE: {
                EntityTypeExportRequest exportRequest = (EntityTypeExportRequest) request;
                EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
                entityTypeFilter.setEntityType(exportRequest.getEntityType());

                CustomerId customerId = Optional.ofNullable(exportRequest.getCustomerId()).orElse(emptyId(EntityType.CUSTOMER));
                return findEntitiesByFilter(user, customerId, entityTypeFilter, exportRequest.getPage(), exportRequest.getPageSize());
            }
            case ENTITY_FILTER: {
                EntityFilterExportRequest exportRequest = (EntityFilterExportRequest) request;
                EntityFilter filter = exportRequest.getFilter();

                CustomerId customerId = Optional.ofNullable(exportRequest.getCustomerId()).orElse(emptyId(EntityType.CUSTOMER));
                return findEntitiesByFilter(user, customerId, filter, exportRequest.getPage(), exportRequest.getPageSize());
            }
            case ENTITY_QUERY:{
                EntityQueryExportRequest exportRequest = (EntityQueryExportRequest) request;
                EntityDataQuery query = exportRequest.getQuery();

                CustomerId customerId = Optional.ofNullable(exportRequest.getCustomerId()).orElse(emptyId(EntityType.CUSTOMER));
                return findEntitiesByQuery(user, customerId, query);
            }
            default:
                throw new IllegalArgumentException("Export request is not supported");
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
        return entityService.findEntityDataByQuery(user.getTenantId(), customerId, user.getUserPermissions(), query).getData().stream()
                .map(EntityData::getEntityId)
                .collect(Collectors.toList());
    }


    @PostMapping("/import")
    public List<EntityImportResult<ExportableEntity<EntityId>>> importEntities(@RequestBody ImportRequest importRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            List<EntityImportResult<ExportableEntity<EntityId>>> importResults = exportImportService.importEntities(user, importRequest.getExportDataList(), importRequest.getImportSettings());

            importResults.stream()
                    .map(EntityImportResult::getPushEventsCallback)
                    .filter(Objects::nonNull)
                    .forEach(pushEventsCallback -> {
                        try {
                            pushEventsCallback.run();
                        } catch (Exception e) {
                            log.error("Failed to send event for entity", e);
                        }
                    });

            return importResults;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
