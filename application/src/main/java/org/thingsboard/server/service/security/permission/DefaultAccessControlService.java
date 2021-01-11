/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.security.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class DefaultAccessControlService implements AccessControlService {

    private static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";

    private final Map<Authority, Permissions> authorityPermissions = new HashMap<>();

    public DefaultAccessControlService(
            @Qualifier("sysAdminPermissions") Permissions sysAdminPermissions,
            @Qualifier("tenantAdminPermissions") Permissions tenantAdminPermissions,
            @Qualifier("customerUserPermissions") Permissions customerUserPermissions) {
        authorityPermissions.put(Authority.SYS_ADMIN, sysAdminPermissions);
        authorityPermissions.put(Authority.TENANT_ADMIN, tenantAdminPermissions);
        authorityPermissions.put(Authority.CUSTOMER_USER, customerUserPermissions);
    }

    @Override
    public void checkPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource, true);
        if (!permissionChecker.hasPermission(user, resource, operation)) {
            genericOperationPermissionDenied(resource, operation);
        }
    }

    @Override
    public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource, false);
        if (permissionChecker != null) {
            return permissionChecker.hasPermission(user, resource, operation);
        }
        return false;
    }

    @Override
    public <I extends EntityId, T extends TenantEntity> void checkPermission(SecurityUser user, Resource resource,
                                                                             Operation operation, I entityId, T entity) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource, true);
        if (!permissionChecker.hasPermission(user, operation, entityId, entity)) {
            entityOperationPermissionDenied(resource, operation, entityId, entity);
        }
    }

    @Override
    public <I extends EntityId, T extends TenantEntity> void checkPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity, EntityGroupId entityGroupId) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource, true);
        if (!permissionChecker.hasPermission(user, operation, entityId, entity, entityGroupId)) {
            entityOperationPermissionDenied(resource, operation, entityId, entity);
        }
    }

    @Override
    public <I extends EntityId, T extends TenantEntity> boolean hasPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource, false);
        if (permissionChecker != null) {
            return permissionChecker.hasPermission(user, operation, entityId, entity);
        }
        return false;
    }

    @Override
    public void checkEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), Resource.groupResourceFromGroupType(entityGroup.getType()), true);
        if (!permissionChecker.hasEntityGroupPermission(user, operation, entityGroup)) {
            entityGroupOperationPermissionDenied(operation, entityGroup);
        }
    }

    @Override
    public boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), Resource.groupResourceFromGroupType(entityGroup.getType()), false);
        if (permissionChecker != null) {
            return permissionChecker.hasEntityGroupPermission(user, operation, entityGroup);
        }
        return false;
    }

    private PermissionChecker getPermissionChecker(Authority authority, Resource resource, boolean throwException) throws ThingsboardException {
        Permissions permissions = authorityPermissions.get(authority);
        if (permissions == null) {
            if (throwException) {
                permissionDenied();
            } else {
                return null;
            }
        }
        Optional<PermissionChecker> permissionChecker = permissions.getPermissionChecker(resource);
        if (!permissionChecker.isPresent()) {
            if (throwException) {
                permissionDenied();
            } else {
                return null;
            }
        }
        return permissionChecker.get();
    }

    private void permissionDenied() throws ThingsboardException {
        throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

    private void genericOperationPermissionDenied(Resource resource, Operation operation) throws ThingsboardException {
        throw new ThingsboardException("You don't have permission to perform '" + operation + "' operation with '" + resource + "' resource!",
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

    private <I extends EntityId, T extends TenantEntity>
        void entityOperationPermissionDenied(Resource resource, Operation operation, I entityId, T entity) throws ThingsboardException {
            EntityType entityType = entity != null ? entity.getEntityType() : entityId.getEntityType();
            String message = "You don't have permission to perform '" + operation + "' operation with " + entityType;
            if (entity instanceof HasName) {
                message += " '" + ((HasName)entity).getName() + "'";
            }
            message += "!";
            throw new ThingsboardException(message,
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

    private void entityGroupOperationPermissionDenied(Operation operation, EntityGroup entityGroup) throws ThingsboardException {
        throw new ThingsboardException("You don't have permission to perform '" + operation + "' operation with "+ entityGroup.getType() +" group '" + entityGroup.getName() + "'!",
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

}
