/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
        //TODO: entities-version-merge
        put(Resource.ADMIN_SETTINGS, PermissionChecker.allowAllPermissionChecker);
        put(Resource.PROFILE, genericPermissionChecker);
        put(Resource.ALARM, tenantStandaloneEntityPermissionChecker);
        put(Resource.ASSET, tenantGroupEntityPermissionChecker);
        put(Resource.DEVICE, tenantGroupEntityPermissionChecker);
        put(Resource.CUSTOMER, tenantGroupEntityPermissionChecker);
        put(Resource.DASHBOARD, tenantGroupEntityPermissionChecker);
        put(Resource.ENTITY_VIEW, tenantGroupEntityPermissionChecker);
        put(Resource.EDGE, tenantGroupEntityPermissionChecker);
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
        put(Resource.EDGE_GROUP, tenantEntityGroupPermissionChecker);
        put(Resource.DASHBOARD_GROUP, tenantEntityGroupPermissionChecker);
        put(Resource.WHITE_LABELING, tenantWhiteLabelingPermissionChecker);
        put(Resource.GROUP_PERMISSION, tenantStandaloneEntityPermissionChecker);
        put(Resource.AUDIT_LOG, genericPermissionChecker);
        put(Resource.DEVICE_PROFILE, tenantStandaloneEntityPermissionChecker);
        put(Resource.ASSET_PROFILE, tenantStandaloneEntityPermissionChecker);
        put(Resource.API_USAGE_STATE, tenantStandaloneEntityPermissionChecker);
        put(Resource.TB_RESOURCE, tbResourcePermissionChecker);
        put(Resource.OTA_PACKAGE, tenantStandaloneEntityPermissionChecker);
        put(Resource.QUEUE, queuePermissionChecker);
        put(Resource.VERSION_CONTROL, genericPermissionChecker);
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

    private static final PermissionChecker tbResourcePermissionChecker = new PermissionChecker() {

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

    private static final PermissionChecker queuePermissionChecker = new PermissionChecker() {

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

}
