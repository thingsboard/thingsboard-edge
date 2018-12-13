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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.security.model.SecurityUser;

@Slf4j
@Component(value="customerUserPermissions")
public class CustomerUserPermissions extends AbstractPermissions {

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private OwnersCacheService ownersCacheService;

    public CustomerUserPermissions() {
        super();
        put(Resource.ALARM, TenantAdminPermissions.tenantEntityPermissionChecker);
        put(Resource.ASSET, customerEntityPermissionChecker);
        put(Resource.DEVICE, customerEntityPermissionChecker);
        put(Resource.CUSTOMER, customerPermissionChecker);
        put(Resource.DASHBOARD, customerEntityPermissionChecker);
        put(Resource.ENTITY_VIEW, customerEntityPermissionChecker);
        put(Resource.ROLE, customerEntityPermissionChecker);
        put(Resource.USER, userPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, widgetsPermissionChecker);
        put(Resource.WIDGET_TYPE, widgetsPermissionChecker);
        put(Resource.SCHEDULER_EVENT, customerEditableEntityPermissionChecker);
        put(Resource.BLOB_ENTITY, customerEditableEntityPermissionChecker);
        put(Resource.CUSTOMER_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.DEVICE_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.ASSET_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.USER_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.ENTITY_VIEW_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.DASHBOARD_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.WHITE_LABELING, customerWhiteLabelingPermissionChecker);
    }

    private static final PermissionChecker customerEntityPermissionChecker =
            new PermissionChecker.GenericPermissionChecker(Operation.ALL) {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {

            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            if (!(entity instanceof HasCustomerId)) {
                return false;
            }
            if (!user.getCustomerId().equals(((HasCustomerId)entity).getCustomerId())) {
                return false;
            }
            return true;
        }
    };

    private static final PermissionChecker customerEditableEntityPermissionChecker =
            new PermissionChecker() {

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
                    if (!user.getTenantId().equals(entity.getTenantId())) {
                        return false;
                    }
                    if (!(entity instanceof HasCustomerId)) {
                        return false;
                    }
                    if (!user.getCustomerId().equals(((HasCustomerId)entity).getCustomerId())) {
                        return false;
                    }
                    return true;
                }
            };


    private static final PermissionChecker customerPermissionChecker =
            new PermissionChecker.GenericPermissionChecker<CustomerId, Customer>(Operation.ALL) {

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation, CustomerId entityId, Customer entity) {
                    if (!super.hasPermission(user, operation, entityId, entity)) {
                        return false;
                    }
                    if (user.getCustomerId().equals(entity.getParentCustomerId()) || user.getCustomerId().equals(entityId)) {
                        return true;
                    }
                    return false;
                }

            };

    private static final PermissionChecker customerDashboardPermissionChecker =
            new PermissionChecker.GenericPermissionChecker<DashboardId, DashboardInfo>(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY) {

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation, DashboardId dashboardId, DashboardInfo dashboard) {
                    if (!super.hasPermission(user, operation, dashboardId, dashboard)) {
                        return false;
                    }
                    if (!user.getTenantId().equals(dashboard.getTenantId())) {
                        return false;
                    }
                    if (!dashboard.isAssignedToCustomer(user.getCustomerId())) {
                        return false;
                    }
                    return true;
                }

            };

    private static final PermissionChecker userPermissionChecker = new PermissionChecker<UserId, User>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, UserId userId, User userEntity) {
            if (userEntity.getAuthority() != Authority.CUSTOMER_USER) {
                return false;
            }
            if (!user.getCustomerId().equals(userEntity.getCustomerId())) {
                return false;
            }
            return true;
        }

    };

    private static final PermissionChecker widgetsPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return true;
            }
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            return true;
        }

    };

    private final PermissionChecker customerEntityGroupPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroupId entityGroupId, EntityType groupType) {
            try {
                return entityGroupService.checkEntityGroup(user.getTenantId(), user.getCustomerId(), entityGroupId, groupType).get();
            } catch (Exception e) {
                log.error("Failed to check entity group permissions!", e);
            }
            return false;
        }

        @Override
        public boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) {
            if (operation == Operation.CREATE) {
                return true;
            }
            try {
                return entityGroupService.checkEntityGroup(user.getTenantId(), user.getCustomerId(), entityGroup).get();
            } catch (Exception e) {
                log.error("Failed to check entity group permissions!", e);
            }
            return false;
        }

    };

    private final PermissionChecker customerWhiteLabelingPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation) {
            return whiteLabelingService.isWhiteLabelingAllowed(user.getTenantId(), user.getCustomerId());
        }

    };

}
