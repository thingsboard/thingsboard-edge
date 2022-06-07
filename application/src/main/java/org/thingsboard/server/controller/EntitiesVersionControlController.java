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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.sync.vc.*;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.*;

@RestController
@TbCoreComponent
@RequestMapping("/api/entities/vc")
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
@RequiredArgsConstructor
public class EntitiesVersionControlController extends BaseController {

    private final EntitiesVersionControlService versionControlService;


    @ApiOperation(value = "", notes = "" +
            "SINGLE_ENTITY:" + NEW_LINE +
            "```\n{\n" +
            "  \"type\": \"SINGLE_ENTITY\",\n" +
            "\n" +
            "  \"versionName\": \"Version 1.0\",\n" +
            "  \"branch\": \"dev\",\n" +
            "\n" +
            "  \"entityId\": {\n" +
            "    \"entityType\": \"DEVICE\",\n" +
            "    \"id\": \"b79448e0-d4f4-11ec-847b-0f432358ab48\"\n" +
            "  },\n" +
            "  \"config\": {\n" +
            "    \"saveRelations\": true\n" +
            "  }\n" +
            "}\n```" + NEW_LINE +
            "COMPLEX:" + NEW_LINE +
            "```\n{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "\n" +
            "  \"versionName\": \"Devices and profiles: release 2\",\n" +
            "  \"branch\": \"master\",\n" +
            "\n" +
            "  \"syncStrategy\": \"OVERWRITE\",\n" +
            "  \"entityTypes\": {\n" +
            "    \"DEVICE\": {\n" +
            "      \"syncStrategy\": null,\n" +
            "      \"allEntities\": true,\n" +
            "      \"saveRelations\": true\n" +
            "    },\n" +
            "    \"DEVICE_PROFILE\": {\n" +
            "      \"syncStrategy\": \"MERGE\",\n" +
            "      \"allEntities\": false,\n" +
            "      \"entityIds\": [\n" +
            "        \"b79448e0-d4f4-11ec-847b-0f432358ab48\"\n" +
            "      ],\n" +
            "      \"saveRelations\": true\n" +
            "    }\n" +
            "  }\n" +
            "}\n```")
    @PostMapping("/version")
    public DeferredResult<VersionCreationResult> saveEntitiesVersion(@RequestBody VersionCreateRequest request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
            return wrapFuture(versionControlService.saveEntitiesVersion(user, request));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"id\": \"c30c8bcaed3f0813649f0dee51a89d04d0a12b28\",\n" +
            "    \"name\": \"Device profile 1 version 1.0\"\n" +
            "  }\n" +
            "]\n```")
    @GetMapping(value = "/version/{branch}/{entityType}/{externalEntityUuid}", params = {"pageSize", "page"})
    public DeferredResult<PageData<EntityVersion>> listEntityVersions(@PathVariable String branch,
                                                                      @PathVariable EntityType entityType,
                                                                      @PathVariable UUID externalEntityUuid,
                                                                      @ApiParam(value = PAGE_SIZE_DESCRIPTION)
                                                                      @RequestParam(required = false) UUID internalEntityUuid,
                                                                      @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                                      @RequestParam int pageSize,
                                                                      @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                      @RequestParam int page,
                                                                      @ApiParam(value = ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION)
                                                                      @RequestParam(required = false) String textSearch,
                                                                      @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = "timestamp")
                                                                      @RequestParam(required = false) String sortProperty,
                                                                      @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                                      @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            EntityId externalEntityId = EntityIdFactory.getByTypeAndUuid(entityType, externalEntityUuid);
            EntityId internalEntityId = internalEntityUuid != null ? EntityIdFactory.getByTypeAndUuid(entityType, internalEntityUuid) : null;
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return wrapFuture(versionControlService.listEntityVersions(getTenantId(), branch, externalEntityId, internalEntityId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"id\": \"c30c8bcaed3f0813649f0dee51a89d04d0a12b28\",\n" +
            "    \"name\": \"Device profiles from dev\"\n" +
            "  }\n" +
            "]\n```")
    @GetMapping(value = "/version/{branch}/{entityType}", params = {"pageSize", "page"})
    public DeferredResult<PageData<EntityVersion>> listEntityTypeVersions(@PathVariable String branch,
                                                                          @PathVariable EntityType entityType,
                                                                          @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                                          @RequestParam int pageSize,
                                                                          @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                          @RequestParam int page,
                                                                          @ApiParam(value = ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION)
                                                                          @RequestParam(required = false) String textSearch,
                                                                          @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = "timestamp")
                                                                          @RequestParam(required = false) String sortProperty,
                                                                          @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                                          @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return wrapFuture(versionControlService.listEntityTypeVersions(getTenantId(), branch, entityType, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"id\": \"ba9baaca1742b730e7331f31a6a51da5fc7da7f7\",\n" +
            "    \"name\": \"Device 1 removed\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"id\": \"b3c28d722d328324c7c15b0b30047b0c40011cf7\",\n" +
            "    \"name\": \"Device profiles added\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"id\": \"c30c8bcaed3f0813649f0dee51a89d04d0a12b28\",\n" +
            "    \"name\": \"Devices added\"\n" +
            "  }\n" +
            "]\n```")
    @GetMapping(value = "/version/{branch}", params = {"pageSize", "page"})
    public DeferredResult<PageData<EntityVersion>> listVersions(@PathVariable String branch,
                                                                @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                                @RequestParam int pageSize,
                                                                @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                @RequestParam int page,
                                                                @ApiParam(value = ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION)
                                                                @RequestParam(required = false) String textSearch,
                                                                @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = "timestamp")
                                                                @RequestParam(required = false) String sortProperty,
                                                                @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                                @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return wrapFuture(versionControlService.listVersions(getTenantId(), branch, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @GetMapping("/entity/{branch}/{entityType}/{versionId}")
    public DeferredResult<List<VersionedEntityInfo>> listEntitiesAtVersion(@PathVariable String branch,
                                                                           @PathVariable EntityType entityType,
                                                                           @PathVariable String versionId) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            return wrapFuture(versionControlService.listEntitiesAtVersion(getTenantId(), branch, versionId, entityType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @GetMapping("/entity/{branch}/{versionId}")
    public DeferredResult<List<VersionedEntityInfo>> listAllEntitiesAtVersion(@PathVariable String branch,
                                                                              @PathVariable String versionId) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            return wrapFuture(versionControlService.listAllEntitiesAtVersion(getTenantId(), branch, versionId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @GetMapping("/info/{versionId}/{entityType}/{externalEntityUuid}")
    public DeferredResult<EntityDataInfo> getEntityDataInfo(@PathVariable String versionId,
                                                            @PathVariable EntityType entityType,
                                                            @PathVariable UUID externalEntityUuid) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, externalEntityUuid);
            return wrapFuture(versionControlService.getEntityDataInfo(getCurrentUser(), entityId, versionId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @GetMapping("/diff/{branch}/{entityType}/{internalEntityUuid}")
    public DeferredResult<EntityDataDiff> compareEntityDataToVersion(@PathVariable String branch,
                                                                     @PathVariable EntityType entityType,
                                                                     @PathVariable UUID internalEntityUuid,
                                                                     @RequestParam String versionId) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, internalEntityUuid);
            return wrapFuture(versionControlService.compareEntityDataToVersion(getCurrentUser(), branch, entityId, versionId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "", notes = "" +
            "SINGLE_ENTITY:" + NEW_LINE +
            "```\n{\n" +
            "  \"type\": \"SINGLE_ENTITY\",\n" +
            "  \n" +
            "  \"branch\": \"dev\",\n" +
            "  \"versionId\": \"b3c28d722d328324c7c15b0b30047b0c40011cf7\",\n" +
            "  \n" +
            "  \"externalEntityId\": {\n" +
            "    \"entityType\": \"DEVICE\",\n" +
            "    \"id\": \"b7944123-d4f4-11ec-847b-0f432358ab48\"\n" +
            "  },\n" +
            "  \"config\": {\n" +
            "    \"loadRelations\": false,\n" +
            "    \"findExistingEntityByName\": false\n" +
            "  }\n" +
            "}\n```" + NEW_LINE +
            "ENTITY_TYPE:" + NEW_LINE +
            "```\n{\n" +
            "  \"type\": \"ENTITY_TYPE\",\n" +
            "\n" +
            "  \"branch\": \"dev\",\n" +
            "  \"versionId\": \"b3c28d722d328324c7c15b0b30047b0c40011cf7\",\n" +
            "\n" +
            "  \"entityTypes\": {\n" +
            "    \"DEVICE\": {\n" +
            "      \"loadRelations\": false,\n" +
            "      \"findExistingEntityByName\": false,\n" +
            "      \"removeOtherEntities\": true\n" +
            "    }\n" +
            "  }\n" +
            "}\n```")
    @PostMapping("/entity")
    public DeferredResult<VersionLoadResult> loadEntitiesVersion(@RequestBody VersionLoadRequest request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            return wrapFuture(versionControlService.loadEntitiesVersion(user, request));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"name\": \"master\",\n" +
            "    \"default\": true\n" +
            "  },\n" +
            "  {\n" +
            "    \"name\": \"dev\",\n" +
            "    \"default\": false\n" +
            "  },\n" +
            "  {\n" +
            "    \"name\": \"dev-2\",\n" +
            "    \"default\": false\n" +
            "  }\n" +
            "]\n\n```")
    @GetMapping("/branches")
    public DeferredResult<List<BranchInfo>> listBranches() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            final TenantId tenantId = getTenantId();
            ListenableFuture<List<String>> branches = versionControlService.listBranches(tenantId);
            return wrapFuture(Futures.transform(branches, remoteBranches -> {
                List<BranchInfo> infos = new ArrayList<>();

                String defaultBranch = versionControlService.getVersionControlSettings(tenantId).getDefaultBranch();
                if (StringUtils.isNotEmpty(defaultBranch)) {
                    infos.add(new BranchInfo(defaultBranch, true));
                }

                remoteBranches.forEach(branch -> {
                    if (!branch.equals(defaultBranch)) {
                        infos.add(new BranchInfo(branch, false));
                    }
                });
                return infos;
            }, MoreExecutors.directExecutor()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Data
    public static class BranchInfo {
        private final String name;
        private final boolean isDefault;
    }

}
