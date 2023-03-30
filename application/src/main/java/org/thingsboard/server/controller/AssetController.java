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

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.asset.AssetBulkImportService;
import org.thingsboard.server.service.entitiy.asset.TbAssetService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.ASSET_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ASSET_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ASSET_NAME_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ASSET_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ASSET_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ASSET_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AssetController extends BaseController {
    private final AssetBulkImportService assetBulkImportService;
    private final TbAssetService tbAssetService;

    public static final String ASSET_ID = "assetId";

    @ApiOperation(value = "Get Asset (getAssetById)",
            notes = "Fetch the Asset object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer."
                    + "\n\n" + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    public Asset getAssetById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION, required = true)
                              @PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
        checkParameter(ASSET_ID, strAssetId);
        AssetId assetId = new AssetId(toUUID(strAssetId));
        return checkAssetId(assetId, Operation.READ);
    }

    @ApiOperation(value = "Get Asset Info (getAssetInfoById)",
            notes = "Fetch the Asset Info object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer."
                    + ASSET_INFO_DESCRIPTION + "\n\n" + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/info/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    public AssetInfo getAssetInfoById(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION, required = true)
                                      @PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            AssetId assetId = new AssetId(toUUID(strAssetId));
            return checkAssetInfoId(assetId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Asset (saveAsset)",
            notes = "Creates or Updates the Asset. When creating asset, platform generates Asset Id as " + UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the asset. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Asset entity. "
                    + "\n\n" + ControllerConstants.RBAC_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset", method = RequestMethod.POST)
    @ResponseBody
    public Asset saveAsset(
            @ApiParam(value = "A JSON value representing the asset.", required = true)
            @RequestBody Asset asset,
            @ApiParam(value = ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION)
            @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
            @ApiParam(value = ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION)
            @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return saveGroupEntity(asset, strEntityGroupId, strEntityGroupIds, (asset1, entityGroups) -> tbAssetService.save(asset, entityGroups, user));
    }

    @ApiOperation(value = "Delete asset (deleteAsset)",
            notes = "Deletes the asset and all the relations (from and to the asset). " +
                    "Referencing non-existing asset Id will cause an error." + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/{assetId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAsset(@ApiParam(value = ASSET_ID_PARAM_DESCRIPTION) @PathVariable(ASSET_ID) String strAssetId) throws Exception {
        checkParameter(ASSET_ID, strAssetId);
        AssetId assetId = new AssetId(toUUID(strAssetId));
        Asset asset = checkAssetId(assetId, Operation.DELETE);
        tbAssetService.delete(asset, getCurrentUser()).get();
    }

    @ApiOperation(value = "Get Tenant Assets (getTenantAssets)",
            notes = "Returns a page of assets owned by tenant. " +
                    PAGE_DATA_PARAMETERS + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/assets", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Asset> getTenantAssets(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = ASSET_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (type != null && type.trim().length() > 0) {
            return checkNotNull(assetService.findAssetsByTenantIdAndType(tenantId, type, pageLink));
        } else {
            return checkNotNull(assetService.findAssetsByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Tenant Asset (getTenantAsset)",
            notes = "Requested asset must be owned by tenant that the user belongs to. " +
                    "Asset name is an unique property of asset. So it can be used to identify the asset."
                    + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/assets", params = {"assetName"}, method = RequestMethod.GET)
    @ResponseBody
    public Asset getTenantAsset(
            @ApiParam(value = ASSET_NAME_DESCRIPTION)
            @RequestParam String assetName) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(assetService.findAssetByTenantIdAndName(tenantId, assetName));
    }

    @ApiOperation(value = "Get Customer Assets (getCustomerAssets)",
            notes = "Returns a page of assets objects owned by customer. " +
                    PAGE_DATA_PARAMETERS + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/assets", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Asset> getCustomerAssets(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @ApiParam(value = ASSET_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (type != null && type.trim().length() > 0) {
            return checkNotNull(assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
        } else {
            return checkNotNull(assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @ApiOperation(value = "Get Assets (getUserAssets)",
            notes = "Returns a page of assets objects available for the current user. " +
                    PAGE_DATA_PARAMETERS + ASSET_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/assets", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Asset> getUserAssets(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = ASSET_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = ASSET_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String assetProfileId,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        SecurityUser currentUser = getCurrentUser();
        MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
        return entityService.findUserEntities(currentUser.getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, EntityType.ASSET,
                Operation.READ, type, pageLink);
    }

    @ApiOperation(value = "Get All Asset Infos for current user (getAllAssetInfos)",
            notes = "Returns a page of asset info objects owned by the tenant or the customer of a current user. "
                    + ASSET_INFO_DESCRIPTION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assetInfos/all", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AssetInfo> getAllAssetInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @ApiParam(value = ASSET_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String assetProfileId,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                if (includeCustomers != null && includeCustomers) {
                    if (assetProfileId != null && assetProfileId.length() > 0) {
                        AssetProfileId profileId = new AssetProfileId(toUUID(assetProfileId));
                        return checkNotNull(assetService.findAssetInfosByTenantIdAndAssetProfileId(tenantId, profileId, pageLink));
                    } else {
                        return checkNotNull(assetService.findAssetInfosByTenantId(tenantId, pageLink));
                    }
                } else {
                    if (assetProfileId != null && assetProfileId.length() > 0) {
                        AssetProfileId profileId = new AssetProfileId(toUUID(assetProfileId));
                        return checkNotNull(assetService.findTenantAssetInfosByTenantIdAndAssetProfileId(tenantId, profileId, pageLink));
                    } else {
                        return checkNotNull(assetService.findTenantAssetInfosByTenantId(tenantId, pageLink));
                    }
                }
            } else {
                CustomerId customerId = getCurrentUser().getCustomerId();
                if (includeCustomers != null && includeCustomers) {
                    if (assetProfileId != null && assetProfileId.length() > 0) {
                        AssetProfileId profileId = new AssetProfileId(toUUID(assetProfileId));
                        return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers(tenantId, customerId, profileId, pageLink));
                    } else {
                        return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
                    }
                } else {
                    if (assetProfileId != null && assetProfileId.length() > 0) {
                        AssetProfileId profileId = new AssetProfileId(toUUID(assetProfileId));
                        return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(tenantId, customerId, profileId, pageLink));
                    } else {
                        return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
                    }
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Asset Infos (getCustomerAssetInfos)",
            notes = "Returns a page of asset info objects owned by the specified customer. "
                    + ASSET_INFO_DESCRIPTION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/assetInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AssetInfo> getCustomerAssetInfos(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @ApiParam(value = ASSET_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String assetProfileId,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (includeCustomers != null && includeCustomers) {
                if (assetProfileId != null && assetProfileId.length() > 0) {
                    AssetProfileId profileId = new AssetProfileId(toUUID(assetProfileId));
                    return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers(tenantId, customerId, profileId, pageLink));
                } else {
                    return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
                }
            } else {
                if (assetProfileId != null && assetProfileId.length() > 0) {
                    AssetProfileId profileId = new AssetProfileId(toUUID(assetProfileId));
                    return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(tenantId, customerId, profileId, pageLink));
                } else {
                    return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Assets By Ids (getAssetsByIds)",
            notes = "Requested assets must be owned by tenant or assigned to customer which user is performing the request. " + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assets", params = {"assetIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Asset> getAssetsByIds(
            @ApiParam(value = "A list of asset ids, separated by comma ','", required = true)
            @RequestParam("assetIds") String[] strAssetIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("assetIds", strAssetIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<AssetId> assetIds = new ArrayList<>();
        for (String strAssetId : strAssetIds) {
            assetIds.add(new AssetId(toUUID(strAssetId)));
        }
        List<Asset> assets = checkNotNull(assetService.findAssetsByTenantIdAndIdsAsync(tenantId, assetIds).get());
        return filterAssetsByReadPermission(assets);
    }

    @ApiOperation(value = "Find related assets (findByQuery)",
            notes = "Returns all assets that are related to the specific entity. " +
                    "The entity id, relation type, asset types, depth of the search, and other query parameters defined using complex 'AssetSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info. \n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assets", method = RequestMethod.POST)
    @ResponseBody
    public List<Asset> findByQuery(@RequestBody AssetSearchQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getAssetTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        List<Asset> assets = checkNotNull(assetService.findAssetsByQuery(getTenantId(), query).get());
        return filterAssetsByReadPermission(assets);
    }

    @ApiOperation(value = "Get assets by Entity Group Id (getAssetsByEntityGroupId)",
            notes = "Returns a page of asset objects that belongs to specified Entity Group Id. " +
                    PAGE_DATA_PARAMETERS + "\n\n" + RBAC_GROUP_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/entityGroup/{entityGroupId}/assets", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Asset> getAssetsByEntityGroupId(
            @ApiParam(value = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        checkEntityGroupType(EntityType.ASSET, entityGroup.getType());
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(assetService.findAssetsByEntityGroupId(entityGroupId, pageLink));
    }

    private List<Asset> filterAssetsByReadPermission(List<Asset> assets) {
        return assets.stream().filter(asset -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.ASSET, Operation.READ, asset.getId(), asset);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    @ApiOperation(value = "Get Asset Types (getAssetTypes)",
            notes = "Returns a set of unique asset types based on assets that are either owned by the tenant or assigned to the customer which user is performing the request.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getAssetTypes() throws ThingsboardException, ExecutionException, InterruptedException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        ListenableFuture<List<EntitySubtype>> assetTypes = assetService.findAssetTypesByTenantId(tenantId);
        return checkNotNull(assetTypes.get());
    }

    @ApiOperation(value = "Import the bulk of assets (processAssetsBulkImport)",
            notes = "There's an ability to import the bulk of assets using the only .csv file." + "\n\n" + RBAC_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/asset/bulk_import")
    public BulkImportResult<Asset> processAssetBulkImport(@RequestBody BulkImportRequest request) throws Exception {
        return assetBulkImportService.processBulkImport(request, getCurrentUser(), (asset, savingFunction) -> {
            try {
                saveGroupEntity(asset, request.getEntityGroupId(), savingFunction);
            } catch (ThingsboardException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
