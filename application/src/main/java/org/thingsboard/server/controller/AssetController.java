/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.EntityGroupController.ENTITY_GROUP_ID;

@RestController
@RequestMapping("/api")
public class AssetController extends BaseController {

    public static final String ASSET_ID = "assetId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    public Asset getAssetById(@PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            AssetId assetId = new AssetId(toUUID(strAssetId));
            return checkAssetId(assetId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset", method = RequestMethod.POST)
    @ResponseBody
    public Asset saveAsset(@RequestBody Asset asset,
                           @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId) throws ThingsboardException {
        try {
            asset.setTenantId(getCurrentUser().getTenantId());

            Operation operation = asset.getId() == null ? Operation.CREATE : Operation.WRITE;

            if (operation == Operation.CREATE
                    && getCurrentUser().getAuthority() == Authority.CUSTOMER_USER &&
                    (asset.getCustomerId() == null || asset.getCustomerId().isNullUid())) {
                asset.setCustomerId(getCurrentUser().getCustomerId());
            }

            EntityGroupId entityGroupId = null;
            if (!StringUtils.isEmpty(strEntityGroupId)) {
                entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            }

            accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, operation,
                    asset.getId(), asset, entityGroupId);

            Asset savedAsset  = checkNotNull(assetService.saveAsset(asset));

            if (entityGroupId != null && operation == Operation.CREATE) {
                entityGroupService.addEntityToEntityGroup(getTenantId(), entityGroupId, savedAsset.getId());
            }

            logEntityAction(savedAsset.getId(), savedAsset,
                    savedAsset.getCustomerId(),
                    asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return  savedAsset;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ASSET), asset,
                    null, asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/{assetId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAsset(@PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            AssetId assetId = new AssetId(toUUID(strAssetId));
            Asset asset = checkAssetId(assetId, Operation.DELETE);
            assetService.deleteAsset(getTenantId(), assetId);

            logEntityAction(assetId, asset,
                    asset.getCustomerId(),
                    ActionType.DELETED, null, strAssetId);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ASSET),
                    null,
                    null,
                    ActionType.DELETED, e, strAssetId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/assets", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Asset> getTenantAssets(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length()>0) {
                return checkNotNull(assetService.findAssetsByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(assetService.findAssetsByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/assets", params = {"assetName"}, method = RequestMethod.GET)
    @ResponseBody
    public Asset getTenantAsset(
            @RequestParam String assetName) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(assetService.findAssetByTenantIdAndName(tenantId, assetName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/assets", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Asset> getCustomerAssets(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length()>0) {
                return checkNotNull(assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/assets", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Asset> getUserAssets(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            List<Predicate<Asset>> filters = new ArrayList<>();
            if (type != null && type.trim().length() > 0) {
                filters.add((asset -> asset.getType().equals(type)));
            }
            return getGroupEntitiesByPageLink(getCurrentUser(), EntityType.ASSET, Operation.READ, entityId -> new AssetId(entityId.getId()),
                    (entityIds) -> {
                        try {
                            return assetService.findAssetsByTenantIdAndIdsAsync(getTenantId(), entityIds).get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    filters,
                    pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assets", params = {"assetIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Asset> getAssetsByIds(
            @RequestParam("assetIds") String[] strAssetIds) throws ThingsboardException {
        checkArrayParameter("assetIds", strAssetIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<AssetId> assetIds = new ArrayList<>();
            for (String strAssetId : strAssetIds) {
                assetIds.add(new AssetId(toUUID(strAssetId)));
            }
            List<Asset> assets = checkNotNull(assetService.findAssetsByTenantIdAndIdsAsync(tenantId, assetIds).get());
            return filterAssetsByReadPermission(assets);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assets", method = RequestMethod.POST)
    @ResponseBody
    public List<Asset> findByQuery(@RequestBody AssetSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getAssetTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<Asset> assets = checkNotNull(assetService.findAssetsByQuery(getTenantId(), query).get());
            return filterAssetsByReadPermission(assets);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/assets", method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<Asset> getAssetsByEntityGroupId(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = "Page link limit", required = true, allowableValues = "range[1, infinity]") @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        EntityType entityType = entityGroup.getType();
        checkEntityGroupType(entityType);
        try {
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            ListenableFuture<TimePageData<Asset>> asyncResult = assetService.findAssetEntitiesByEntityGroupId(getTenantId(), entityGroupId, pageLink);
            checkNotNull(asyncResult);
            if (asyncResult != null) {
                return checkNotNull(asyncResult.get());
            } else {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
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

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getAssetTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> assetTypes = assetService.findAssetTypesByTenantId(tenantId);
            return checkNotNull(assetTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
