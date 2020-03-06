/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class BlobEntityController extends BaseController {

    public static final String BLOB_ENTITY_ID = "blobEntityId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntity/info/{blobEntityId}", method = RequestMethod.GET)
    @ResponseBody
    public BlobEntityInfo getBlobEntityInfoById(@PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
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
    public PageData<BlobEntityInfo> getBlobEntities(
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
            if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
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
