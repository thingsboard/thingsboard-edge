/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@Slf4j
@Component(value = "tenantAdminPermissions")
public class TenantAdminPermissions extends AbstractPermissions {

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private OwnersCacheService ownersCacheService;

    public TenantAdminPermissions() {
        super();
        put(Resource.PROFILE, genericPermissionChecker);
        put(Resource.ALARM, tenantStandaloneEntityPermissionChecker);
        put(Resource.ASSET, tenantGroupEntityPermissionChecker);
        put(Resource.DEVICE, tenantGroupEntityPermissionChecker);
        put(Resource.CUSTOMER, tenantGroupEntityPermissionChecker);
        put(Resource.DASHBOARD, tenantGroupEntityPermissionChecker);
        put(Resource.ENTITY_VIEW, tenantGroupEntityPermissionChecker);
        put(Resource.ROLE, tenantStandaloneEntityPermissionChecker);
        put(Resource.TENANT, tenantStandaloneEntityPermissionChecker);
        put(Resource.RULE_CHAIN, tenantStandaloneEntityPermissionChecker);
        put(Resource.USER, tenantGroupEntityPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, widgetsPermissionChecker);
        put(Resource.WIDGET_TYPE, widgetsPermissionChecker);
        put(Resource.CONVERTER, tenantStandaloneEntityPermissionChecker);
        put(Resource.INTEGRATION, tenantStandaloneEntityPermissionChecker);
        put(Resource.SCHEDULER_EVENT, tenantStandaloneEntityPermissionChecker);
        put(Resource.BLOB_ENTITY, tenantStandaloneEntityPermissionChecker);
        put(Resource.CUSTOMER_GROUP, tenantEntityGroupPermissionChecker);
        put(Resource.DEVICE_GROUP, tenantEntityGroupPermissionChecker);
        put(Resource.ASSET_GROUP, tenantEntityGroupPermissionChecker);
        put(Resource.USER_GROUP, tenantEntityGroupPermissionChecker);
        put(Resource.ENTITY_VIEW_GROUP, tenantEntityGroupPermissionChecker);
        put(Resource.DASHBOARD_GROUP, tenantEntityGroupPermissionChecker);
        put(Resource.WHITE_LABELING, tenantWhiteLabelingPermissionChecker);
        put(Resource.GROUP_PERMISSION, tenantStandaloneEntityPermissionChecker);
        put(Resource.AUDIT_LOG, genericPermissionChecker);
        put(Resource.DEVICE_PROFILE, tenantStandaloneEntityPermissionChecker);
        put(Resource.API_USAGE_STATE, tenantStandaloneEntityPermissionChecker);
    }

    public static final PermissionChecker tenantStandaloneEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            // This entity does not have groups, so we are checking only generic level permissions
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }
    };

    private final PermissionChecker tenantGroupEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) throws ThingsboardException {
            return hasPermission(user, operation, entityId, entity, null);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity, EntityGroupId entityGroupId) throws ThingsboardException {
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());

            if (entityGroupId != null) {
                if (!ownersCacheService.getOwners(user.getTenantId(), entityGroupId).contains(user.getOwnerId())) {
                    return false;
                }
            }

            if (user.getUserPermissions().hasGenericPermission(resource, operation)) {
                return true;
            }

            // Group permissions check
            if (!operation.isAllowedForGroupRole()) {
                return false;
            }

            if (entityGroupId != null) {
                if (user.getUserPermissions().hasGroupPermissions(entityGroupId, operation)) {
                    return true;
                }
            }

            if (entityId != null) {
                try {
                    List<EntityGroupId> entityGroupIds = entityGroupService.findEntityGroupsForEntity(entity.getTenantId(), entityId).get();
                    for (EntityGroupId groupId : entityGroupIds) {
                        if (user.getUserPermissions().hasGroupPermissions(groupId, operation)) {
                            if (operation.isAllowedForGroupOwnerOnly()) {
                                if (ownersCacheService.getOwners(user.getTenantId(), groupId).contains(user.getOwnerId())) {
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
                }
            }
            return false;
        }
    };

    private static final PermissionChecker widgetsPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                if (operation != Operation.READ) {
                    return false;
                }
            } else if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            Resource resource = Resource.resourceFromEntityType(entity.getEntityType());
            // This entity does not have groups, so we are checking only generic level permissions
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

    };

    private final PermissionChecker tenantEntityGroupPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) {
            Resource resource = Resource.groupResourceFromGroupType(entityGroup.getType());
            if (operation == Operation.CREATE) {
                return user.getUserPermissions().hasGenericPermission(resource, operation);
            }
            boolean isOwner = ownersCacheService.getOwners(user.getTenantId(), entityGroup.getId(), entityGroup).contains(user.getOwnerId());
            if (isOwner) {
                // This entity is a group, so we are checking group generic permission first
                if (user.getUserPermissions().hasGenericPermission(resource, operation)) {
                    return true;
                }
            }
            if (!operation.isAllowedForGroupRole()) {
                return false;
            }
            if (operation.isGroupOperationAllowedForGroupOwnerOnly()) {
                return false;
            }
            //Just in case, we are also checking specific group permission
            return user.getUserPermissions().hasGroupPermissions(entityGroup.getId(), operation);
        }

    };

    private final PermissionChecker tenantWhiteLabelingPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            if (!whiteLabelingService.isWhiteLabelingAllowed(user.getTenantId(), user.getTenantId())) {
                return false;
            } else {
                return user.getUserPermissions().hasGenericPermission(Resource.WHITE_LABELING, operation);
            }
        }

    };

    public static final PermissionChecker genericPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return user.getUserPermissions().hasGenericPermission(resource, operation);
        }

    };
}
