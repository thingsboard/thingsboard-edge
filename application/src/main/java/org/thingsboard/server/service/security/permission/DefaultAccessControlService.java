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
package org.thingsboard.server.service.security.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.*;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class DefaultAccessControlService implements AccessControlService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
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
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource);
        if (!permissionChecker.hasPermission(user, operation)) {
            permissionDenied();
        }
    }

    @Override
    public <I extends EntityId, T extends HasTenantId> void checkPermission(SecurityUser user, Resource resource,
                                                                                            Operation operation, I entityId, T entity) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource);
        if (!permissionChecker.hasPermission(user, operation, entityId, entity)) {
            permissionDenied();
        }
    }

    @Override
    public void checkEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), Resource.groupResourceFromGroupType(entityGroup.getType()));
        if (!permissionChecker.hasEntityGroupPermission(user, operation, entityGroup)) {
            permissionDenied();
        }
    }

    @Override
    public void checkEntityGroupPermission(SecurityUser user, Operation operation, EntityGroupId entityGroupId, EntityType groupType) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), Resource.groupResourceFromGroupType(groupType));
        if (!permissionChecker.hasEntityGroupPermission(user, operation, entityGroupId, groupType)) {
            permissionDenied();
        }
    }

    private PermissionChecker getPermissionChecker(Authority authority, Resource resource) throws ThingsboardException {
        Permissions permissions = authorityPermissions.get(authority);
        if (permissions == null) {
            permissionDenied();
        }
        Optional<PermissionChecker> permissionChecker = permissions.getPermissionChecker(resource);
        if (!permissionChecker.isPresent()) {
            permissionDenied();
        }
        return permissionChecker.get();
    }

    private void permissionDenied() throws ThingsboardException {
        throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

}
