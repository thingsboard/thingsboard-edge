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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api")
public class OwnerController extends BaseController {

    public static final String OWNER_TYPE = "ownerType";
    public static final String OWNER_ID = "ownerId";
    public static final String ENTITY_TYPE = "entityType";
    public static final String ENTITY_ID = "entityId";

    @Autowired
    private OwnersCacheService ownersCacheService;

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/owner/TENANT/{ownerId}/{entityType}/{entityId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void changeOwnerToTenant(
            @PathVariable(OWNER_ID) String ownerIdStr,
            @PathVariable(ENTITY_TYPE) String entityType,
            @PathVariable(ENTITY_ID) String entityIdStr) throws ThingsboardException {
        checkParameter(OWNER_ID, ownerIdStr);
        checkParameter(ENTITY_TYPE, entityType);
        checkParameter(ENTITY_ID, entityIdStr);
        TenantId targetOwnerId = new TenantId(UUID.fromString(ownerIdStr));
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        if (!getCurrentUser().getTenantId().equals(targetOwnerId)) {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        try {
            checkEntityId(entityId, Operation.CHANGE_OWNER);
            changeOwner(getCurrentUser().getTenantId(), targetOwnerId, entityId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/owner/CUSTOMER/{ownerId}/{entityType}/{entityId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void changeOwnerToCustomer(
            @PathVariable(OWNER_ID) String ownerIdStr,
            @PathVariable(ENTITY_TYPE) String entityType,
            @PathVariable(ENTITY_ID) String entityIdStr) throws ThingsboardException {
        checkParameter(OWNER_ID, ownerIdStr);
        checkParameter(ENTITY_TYPE, entityType);
        checkParameter(ENTITY_ID, entityIdStr);
        EntityId currentUserOwnerId = getCurrentUser().getOwnerId();
        CustomerId targetOwnerId = new CustomerId(UUID.fromString(ownerIdStr));
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);

        Customer targetOwner = customerService.findCustomerById(getCurrentUser().getTenantId(), targetOwnerId);
        Set<EntityId> targetOwnerOwners = ownersCacheService.getOwners(getCurrentUser().getTenantId(), targetOwnerId, targetOwner);
        if (!targetOwnerOwners.contains(currentUserOwnerId)) {
            // Customer/Tenant Changes Owner from Customer to Sub-Customer - OK.
            // Sub-Customer Changes Owner from Sub-Customer to Customer - NOT OK.
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        try {
            changeOwner(getCurrentUser().getTenantId(), targetOwnerId, entityId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    // INVALIDATE CACHES
    private void changeOwner(TenantId tenantId, EntityId targetOwnerId, EntityId entityId) throws ThingsboardException {
        try {
            switch (entityId.getEntityType()) {
                case DEVICE:
                    changeEntityOwner(tenantId, targetOwnerId, new DeviceId(entityId.getId()),
                            checkDeviceId(new DeviceId(entityId.getId()), Operation.CHANGE_OWNER),
                            deviceService::saveDevice);
                    break;
                case ASSET:
                    changeEntityOwner(tenantId, targetOwnerId, new AssetId(entityId.getId()),
                            checkAssetId(new AssetId(entityId.getId()), Operation.CHANGE_OWNER),
                            assetService::saveAsset);
                    break;
                case ENTITY_VIEW:
                    changeEntityOwner(tenantId, targetOwnerId, new EntityViewId(entityId.getId()),
                            checkEntityViewId(new EntityViewId(entityId.getId()), Operation.CHANGE_OWNER),
                            entityViewService::saveEntityView);
                    break;
                case CUSTOMER:
                    Set<EntityId> ownerIds = ownersCacheService.getChildOwners(getTenantId(), entityId);
                    if (!ownerIds.contains(targetOwnerId)) {
                        changeEntityOwner(tenantId, targetOwnerId, new CustomerId(entityId.getId()),
                                checkCustomerId(new CustomerId(entityId.getId()), Operation.CHANGE_OWNER),
                                customerService::saveCustomer);
                    } else {
                        // Making Sub-Customer as a Parent Customer - NOT OK.
                        throw new ThingsboardException("Owner of the Customer can't be changed to its Sub-Customer!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                    }
                    break;
                case USER:
                    UserId userId = new UserId(entityId.getId());
                    User user = checkUserId(new UserId(entityId.getId()), Operation.CHANGE_OWNER);
                    userPermissionsService.onUserUpdatedOrRemoved(user);
                    changeEntityOwner(tenantId, targetOwnerId, userId, user, userService::saveUser);
                    break;
                case DASHBOARD:
                    changeEntityOwner(tenantId, targetOwnerId, new DashboardId(entityId.getId()),
                            checkDashboardId(new DashboardId(entityId.getId()), Operation.CHANGE_OWNER),
                            dashboardService::saveDashboard);
                    break;
            }
        } catch (ExecutionException | InterruptedException e) {
            logEntityAction(entityId, null,
                    null, ActionType.ASSIGNED_TO_CUSTOMER, e);
            throw handleException(e);
        }
    }

    private <T extends HasOwnerId> void changeEntityOwner(TenantId tenantId, EntityId targetOwnerId, EntityId entityId, T entity, Consumer<T> saveFunction)
            throws ThingsboardException, ExecutionException, InterruptedException {
        if (entity.getOwnerId().equals(targetOwnerId)) {
            throw new ThingsboardException("Entity already belongs to this owner!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        List<EntityGroupId> entityGroupList = entityGroupService.findEntityGroupsForEntity(tenantId, entityId).get();
        for (EntityGroupId entityGroupId : entityGroupList) {
            entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, entityId);
        }

        entityGroupService.addEntityToEntityGroupAll(tenantId, targetOwnerId, entityId);

        entity.setOwnerId(targetOwnerId);
        saveFunction.accept(entity);
        ownersCacheService.clearOwners(entityId);
    }

}
