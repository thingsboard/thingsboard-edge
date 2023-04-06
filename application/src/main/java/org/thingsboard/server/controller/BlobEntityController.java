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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
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
import org.thingsboard.server.service.entitiy.blob.TbBlobService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.BLOB_ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.BLOB_ENTITY_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.BLOB_ENTITY_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.BLOB_ENTITY_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class BlobEntityController extends BaseController {

    public static final String BLOB_ENTITY_ID = "blobEntityId";
    public static final String INVALID_BLOB_ENTITY_ID = "Referencing non-existing Blob entity Id will cause an error.";
    public static final String BLOB_ENTITY_DESCRIPTION = "The platform uses Blob(binary large object) entities in the reporting feature, in order to store Dashboard states snapshots of different content types in base64 format. ";
    public static final String BLOB_ENTITY_INFO_DESCRIPTION = BLOB_ENTITY_DESCRIPTION +
            "BlobEntityInfo represents an object that contains base info about the blob entity(name, type, contentType, etc.). " +
            "See the 'Model' tab of the Response Class for more details.";
    public static final String BLOB_ENTITY_INFO_WITH_CUSTOMER_INFO_DESCRIPTION = BLOB_ENTITY_DESCRIPTION +
            "BlobEntityWithCustomerInfo represents an object that contains base info about the blob entity(name, type, contentType, etc.) " +
            "and info about the customer(customerTitle, customerIsPublic) of the user that scheduled generation of the dashboard report. ";
    public static final String BLOB_ENTITY_QUERY_START_TIME_DESCRIPTION = "The start timestamp in milliseconds of the search time range over the BlobEntityWithCustomerInfo class field: 'createdTime'.";
    public static final String BLOB_ENTITY_QUERY_END_TIME_DESCRIPTION = "The end timestamp in milliseconds of the search time range over the BlobEntityWithCustomerInfo class field: 'createdTime'.";

    private final TbBlobService tbBlobService;

    @ApiOperation(value = "Get Blob Entity With Customer Info (getBlobEntityInfoById)",
            notes = "Fetch the BlobEntityWithCustomerInfo object based on the provided Blob entity Id. " +
                    BLOB_ENTITY_INFO_WITH_CUSTOMER_INFO_DESCRIPTION + INVALID_BLOB_ENTITY_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntity/info/{blobEntityId}", method = RequestMethod.GET)
    @ResponseBody
    public BlobEntityWithCustomerInfo getBlobEntityInfoById(
            @ApiParam(value = BLOB_ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        try {
            BlobEntityId blobEntityId = new BlobEntityId(toUUID(strBlobEntityId));
            return checkBlobEntityInfoId(blobEntityId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Download Blob Entity By Id (downloadBlobEntity)",
            notes = "Download report file based on the provided Blob entity Id. " +
                    INVALID_BLOB_ENTITY_ID + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntity/{blobEntityId}/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> downloadBlobEntity(
            @ApiParam(value = BLOB_ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
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

    @ApiOperation(value = "Delete Blob Entity (deleteBlobEntity)",
            notes = "Delete Blob entity based on the provided Blob entity Id. " +
                    INVALID_BLOB_ENTITY_ID + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntity/{blobEntityId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteBlobEntity(
            @ApiParam(value = BLOB_ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        BlobEntityId blobEntityId = new BlobEntityId(toUUID(strBlobEntityId));
        BlobEntityInfo blobEntityInfo = checkBlobEntityInfoId(blobEntityId, Operation.DELETE);
        tbBlobService.delete(blobEntityInfo, getCurrentUser());
    }

    @ApiOperation(value = "Get Blob Entities (getBlobEntities)",
            notes = "Returns a page of BlobEntityWithCustomerInfo object that are available for the current user. "
                    + BLOB_ENTITY_INFO_WITH_CUSTOMER_INFO_DESCRIPTION + PAGE_DATA_PARAMETERS
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntities", method = RequestMethod.GET)
    @ResponseBody
    public PageData<BlobEntityWithCustomerInfo> getBlobEntities(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = BLOB_ENTITY_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = BLOB_ENTITY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = BLOB_ENTITY_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = BLOB_ENTITY_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = BLOB_ENTITY_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime
    ) throws ThingsboardException {
        try {
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            TenantId tenantId = getCurrentUser().getTenantId();
            if (!accessControlService.hasPermission(getCurrentUser(), org.thingsboard.server.common.data.permission.Resource.BLOB_ENTITY, Operation.READ)) {
                return new PageData<>();
            }
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                if (type != null && type.trim().length() > 0) {
                    return checkNotNull(blobEntityService.findBlobEntitiesByTenantIdAndType(tenantId, type, pageLink));
                } else {
                    return checkNotNull(blobEntityService.findBlobEntitiesByTenantId(tenantId, pageLink));
                }
            } else { //CUSTOMER_USER
                CustomerId customerId = getCurrentUser().getCustomerId();
                if (type != null && type.trim().length() > 0) {
                    return checkNotNull(blobEntityService.findBlobEntitiesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
                } else {
                    return checkNotNull(blobEntityService.findBlobEntitiesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Blob Entities By Ids (getBlobEntitiesByIds)",
            notes = "Requested blob entities must be owned by tenant or assigned to customer which user is performing the request. "
                    + BLOB_ENTITY_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/blobEntities", params = {"blobEntityIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<BlobEntityInfo> getBlobEntitiesByIds(
            @ApiParam(value = "A list of blob entity ids, separated by comma ','", required = true) @RequestParam("blobEntityIds") String[] strBlobEntityIds) throws ThingsboardException {
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
