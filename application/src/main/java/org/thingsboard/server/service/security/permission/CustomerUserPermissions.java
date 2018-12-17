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
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
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

import java.util.List;

@Slf4j
@Component(value="customerUserPermissions")
public class CustomerUserPermissions extends AbstractPermissions {

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private OwnersCacheService ownersCacheService;

    public CustomerUserPermissions() {
        super();
        put(Resource.ALARM, TenantAdminPermissions.tenantStandaloneEntityPermissionChecker);
        put(Resource.ASSET, customerGroupEntityPermissionChecker);
        put(Resource.DEVICE, customerGroupEntityPermissionChecker);
        put(Resource.CUSTOMER, customerGroupEntityPermissionChecker);
        put(Resource.DASHBOARD, customerGroupEntityPermissionChecker);
        put(Resource.ENTITY_VIEW, customerGroupEntityPermissionChecker);
        put(Resource.ROLE, customerStandaloneEntityPermissionChecker);
        put(Resource.USER, customerGroupEntityPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, widgetsPermissionChecker);
        put(Resource.WIDGET_TYPE, widgetsPermissionChecker);
        put(Resource.SCHEDULER_EVENT, customerStandaloneEntityPermissionChecker);
        put(Resource.BLOB_ENTITY, customerStandaloneEntityPermissionChecker);
        put(Resource.CUSTOMER_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.DEVICE_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.ASSET_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.USER_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.ENTITY_VIEW_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.DASHBOARD_GROUP, customerEntityGroupPermissionChecker);
        put(Resource.WHITE_LABELING, customerWhiteLabelingPermissionChecker);
    }


    public final PermissionChecker customerStandaloneEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public  boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            if (!(entity instanceof HasOwnerId)) {
                return false;
            }
            if (ownersCacheService.getOwners(user.getTenantId(), entityId, ((HasOwnerId)entity)).contains(user.getOwnerId())) {
                Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
                // This entity does not have groups, so we are checking only generic level permissions
                return user.getUserPermissions().hasGenericPermission(resource, operation);
            } else {
                return false;
            }
        }
    };

    public final PermissionChecker customerGroupEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) throws ThingsboardException {
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            if (!(entity instanceof HasOwnerId)) {
                return false;
            }
            if (entity.getEntityType() == EntityType.CUSTOMER && user.getCustomerId().equals(entityId) ||
                    ownersCacheService.getOwners(user.getTenantId(), entityId, ((HasOwnerId)entity)).contains(user.getOwnerId())) {
                Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
                // This entity does have groups, so we are checking generic level permissions and then group specific permissions
                if (user.getUserPermissions().hasGenericPermission(resource, operation)) {
                    return true;
                } else if (entityId != null) {
                    try {
                        List<EntityGroupId> entityGroupIds = entityGroupService.findEntityGroupsForEntity(entity.getTenantId(), entityId).get();
                        for (EntityGroupId entityGroupId : entityGroupIds) {
                            if (user.getUserPermissions().hasGroupPermissions(entityGroupId, operation)) {
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
                    }
                }
            }
            return false;
        }
    };

    private static final PermissionChecker widgetsPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            if (!super.hasPermission(user, resource, operation)) {
                return false;
            }
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() != null && !entity.getTenantId().isNullUid() &&
                    !user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            // This entity does not have groups, so we are checking only generic level permissions
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }
    };

    private final PermissionChecker customerEntityGroupPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroupId entityGroupId, EntityType groupType) {

            Resource resource = Resource.groupResourceFromGroupType(groupType);
            if (ownersCacheService.getOwners(user.getTenantId(), entityGroupId).contains(user.getOwnerId())) {
                // This entity is a group, so we are checking group generic permission first
                return user.getUserPermissions().hasGenericPermission(resource, operation);
            }
            if (!operation.isAllowedForGroupRole()) {
                return false;
            }
            //Just in case, we are also checking specific group permission
            return user.getUserPermissions().hasGroupPermissions(entityGroupId, operation);
        }

        @Override
        public boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) {

            Resource resource = Resource.groupResourceFromGroupType(entityGroup.getType());
            if (operation == Operation.CREATE) {
                return user.getUserPermissions().hasGenericPermission(resource, operation);
            }
            if (ownersCacheService.getOwners(user.getTenantId(), entityGroup.getId(), entityGroup).contains(user.getOwnerId())) {
                // This entity is a group, so we are checking group generic permission first
                return user.getUserPermissions().hasGenericPermission(resource, operation);
            }
            if (!operation.isAllowedForGroupRole()) {
                return false;
            }
            //Just in case, we are also checking specific group permission
            return user.getUserPermissions().hasGroupPermissions(entityGroup.getId(), operation);
        }

    };

    private final PermissionChecker customerWhiteLabelingPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            if (!whiteLabelingService.isWhiteLabelingAllowed(user.getTenantId(), user.getCustomerId())) {
                return false;
            } else {
                return user.getUserPermissions().hasGenericPermission(Resource.WHITE_LABELING, operation)
                        && user.getUserPermissions().hasGenericPermission(Resource.CUSTOMER, Operation.READ_ATTRIBUTES)
                        && user.getUserPermissions().hasGenericPermission(Resource.CUSTOMER, Operation.WRITE_ATTRIBUTES);
            }
        }

    };

}
