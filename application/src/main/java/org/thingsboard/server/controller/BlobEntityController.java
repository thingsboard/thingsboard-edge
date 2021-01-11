/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class BlobEntityController extends BaseController {

    public static final String BLOB_ENTITY_ID = "blobEntityId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntity/info/{blobEntityId}", method = RequestMethod.GET)
    @ResponseBody
    public BlobEntityWithCustomerInfo getBlobEntityInfoById(@PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        try {
            BlobEntityId blobEntityId = new BlobEntityId(toUUID(strBlobEntityId));
            return checkBlobEntityInfoId(blobEntityId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntity/{blobEntityId}/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> downloadBlobEntity(@PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        try {
            BlobEntityId blobEntityId = new BlobEntityId(toUUID(strBlobEntityId));
            BlobEntity blobEntity = checkBlobEntityId(blobEntityId, Operation.READ);
            ByteArrayResource resource = new ByteArrayResource(blobEntity.getData().array());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + blobEntity.getName())
                    .header("x-filename", blobEntity.getName())
                    .contentLength(resource.contentLength())
                    .contentType(parseMediaType(blobEntity.getContentType()))
                    .body(resource);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private static MediaType parseMediaType(String contentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return mediaType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntity/{blobEntityId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteBlobEntity(@PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        try {
            BlobEntityId blobEntityId = new BlobEntityId(toUUID(strBlobEntityId));
            BlobEntityInfo blobEntityInfo = checkBlobEntityInfoId(blobEntityId, Operation.DELETE);
            blobEntityService.deleteBlobEntity(getTenantId(), blobEntityId);

            logEntityAction(blobEntityId, blobEntityInfo,
                    blobEntityInfo.getCustomerId(),
                    ActionType.DELETED, null, strBlobEntityId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.BLOB_ENTITY),
                    null,
                    null,
                    ActionType.DELETED, e, strBlobEntityId);

            throw handleException(e);
        }
    }


    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntities", method = RequestMethod.GET)
    @ResponseBody
    public PageData<BlobEntityWithCustomerInfo> getBlobEntities(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime
            ) throws ThingsboardException {
        try {
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            TenantId tenantId = getCurrentUser().getTenantId();
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                if (type != null && type.trim().length()>0) {
                    return checkNotNull(blobEntityService.findBlobEntitiesByTenantIdAndType(tenantId, type, pageLink));
                } else {
                    return checkNotNull(blobEntityService.findBlobEntitiesByTenantId(tenantId, pageLink));
                }
            } else { //CUSTOMER_USER
                CustomerId customerId = getCurrentUser().getCustomerId();
                if (type != null && type.trim().length()>0) {
                    return checkNotNull(blobEntityService.findBlobEntitiesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
                } else {
                    return checkNotNull(blobEntityService.findBlobEntitiesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntities", params = {"blobEntityIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<BlobEntityInfo> getBlobEntitiesByIds(
            @RequestParam("blobEntityIds") String[] strBlobEntityIds) throws ThingsboardException {
        checkArrayParameter("blobEntityIds", strBlobEntityIds);
        try {
            if (!accessControlService.hasPermission(getCurrentUser(), org.thingsboard.server.common.data.permission.Resource.BLOB_ENTITY, Operation.READ)) {
                return Collections.emptyList();
            }
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<BlobEntityId> blobEntityIds = new ArrayList<>();
            for (String strBlobEntityId : strBlobEntityIds) {
                blobEntityIds.add(new BlobEntityId(toUUID(strBlobEntityId)));
            }
            List<BlobEntityInfo> blobEntities = checkNotNull(blobEntityService.findBlobEntityInfoByIdsAsync(tenantId, blobEntityIds).get());
            return filterBlobEntitiesByReadPermission(blobEntities);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<BlobEntityInfo> filterBlobEntitiesByReadPermission(List<BlobEntityInfo> blobEntities) {
        return blobEntities.stream().filter(blobEntity -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), org.thingsboard.server.common.data.permission.Resource.BLOB_ENTITY,
                        Operation.READ, blobEntity.getId(), blobEntity);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

}
