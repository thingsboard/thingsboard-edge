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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.EntitiesExportImportService;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.request.ExportRequest;
import org.thingsboard.server.service.sync.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.importing.data.request.ImportRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/entities")
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;


    @PostMapping("/export")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntities(@RequestBody ExportRequest exportRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            return exportEntitiesByRequest(user, exportRequest);
        } catch (Exception e) {
            log.warn("Failed to export entities for request {}", exportRequest, e);
            throw handleException(e);
        }
    }

    @PostMapping(value = "/export", params = {"multiple"})
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntities(@RequestBody List<ExportRequest> exportRequests) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            List<EntityExportData<?>> result = new ArrayList<>();
            for (ExportRequest exportRequest : exportRequests) {
                List<EntityExportData<?>> exportDataList = exportEntitiesByRequest(user, exportRequest);
                result.addAll(exportDataList);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to export entities for requests {}", exportRequests, e);
            throw handleException(e);
        }
    }

    private List<EntityExportData<?>> exportEntitiesByRequest(SecurityUser user, ExportRequest exportRequest) throws ThingsboardException {
        List<EntityId> entities = exportableEntitiesService.findEntitiesForRequest(user, exportRequest);

        List<EntityExportData<?>> exportDataList = new ArrayList<>();
        for (EntityId entityId : entities) {
            EntityExportData<?> exportData = exportImportService.exportEntity(user, entityId, exportRequest.getExportSettings());
            exportDataList.add(exportData);
        }
        return exportDataList;
    }


    @PostMapping("/import")
    public List<EntityImportResult<?>> importEntities(@RequestBody ImportRequest importRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            List<EntityImportResult<?>> importResults = exportImportService.importEntities(user, importRequest.getExportDataList(), importRequest.getImportSettings());

            importResults.stream()
                    .map(EntityImportResult::getSendEventsCallback)
                    .filter(Objects::nonNull)
                    .forEach(sendEventsCallback -> {
                        try {
                            sendEventsCallback.run();
                        } catch (Exception e) {
                            log.error("Failed to send event for entity", e);
                        }
                    });

            return importResults;
        } catch (Exception e) {
            log.warn("Failed to import entities for request {}", importRequest, e);
            throw handleException(e);
        }
    }

}
