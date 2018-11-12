/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;

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
            return checkBlobEntityInfoId(blobEntityId);
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
            BlobEntity blobEntity = checkBlobEntityId(blobEntityId);
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
            BlobEntityInfo blobEntityInfo = checkBlobEntityInfoId(blobEntityId);
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
    public TimePageData<BlobEntityInfo> getBlobEntities(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset) throws ThingsboardException {
        try {
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
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

}
