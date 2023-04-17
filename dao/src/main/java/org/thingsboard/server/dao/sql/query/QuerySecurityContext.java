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
package org.thingsboard.server.dao.sql.query;

import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityGroupListFilter;
import org.thingsboard.server.common.data.query.EntityGroupNameFilter;

import java.util.Collections;
import java.util.Map;

public class QuerySecurityContext {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final CustomerId customerId;

    private final EntityType entityType;

    @Getter
    private final boolean ignorePermissionCheck;

    private final MergedUserPermissions userPermissions;

    private final EntityFilter entityFilter;

    @Getter
    @Setter
    private final EntityId ownerId;

    @Getter
    @Setter
    private final EntityType entityGroupType;

    public QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, null, null, false);
    }

    public QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, boolean ignorePermissionCheck) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, null, null, ignorePermissionCheck);
    }

    public QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, EntityId ownerId, boolean ignorePermissionCheck) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, ownerId, null, ignorePermissionCheck);
    }

    public QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, EntityType entityGroupType, boolean ignorePermissionCheck) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, null, entityGroupType, ignorePermissionCheck);
    }

    private QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, EntityId ownerId, EntityType entityGroupType, boolean ignorePermissionCheck) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.entityType = entityType;
        this.userPermissions = ignorePermissionCheck ? new MergedUserPermissions(Collections.singletonMap(Resource.ALL, Collections.singleton(Operation.ALL)), Collections.emptyMap()) : userPermissions;
        this.entityFilter = entityFilter;
        this.ownerId = ownerId;
        this.entityGroupType = entityGroupType;
        this.ignorePermissionCheck = ignorePermissionCheck;
    }

    public boolean isTenantUser() {
        return customerId == null || customerId.isNullUid();
    }

    public boolean hasGeneric(Operation operation) {
        return userPermissions.hasGenericPermission(Resource.resourceFromEntityType(entityType), operation);
    }

    public MergedGroupTypePermissionInfo getMergedReadPermissionsByEntityType() {
        return userPermissions.getReadEntityPermissions().get(getResource());
    }

    public MergedGroupTypePermissionInfo getMergedReadGroupPermissionsByEntityType() {
        return userPermissions.getReadGroupPermissions().get(entityType);
    }

    public EntityType getEntityType() {
        EntityType entityType;
        if (entityFilter != null) {
            switch (entityFilter.getType()) {
                case ENTITY_GROUP_NAME:
                    entityType = ((EntityGroupNameFilter) entityFilter).getGroupType();
                    break;
                case ENTITIES_BY_GROUP_NAME:
                    entityType = ((EntitiesByGroupNameFilter) entityFilter).getGroupType();
                    break;
                case ENTITY_GROUP:
                    entityType = ((EntityGroupFilter) entityFilter).getGroupType();
                    break;
                default:
                    entityType = this.entityType;
            }
        } else {
            entityType = this.entityType;
        }
        return entityType;
    }

    private Resource getResource() {
        if (entityFilter != null) {
            switch (entityFilter.getType()) {
                case ENTITY_GROUP_NAME:
                    return Resource.groupResourceFromGroupType(((EntityGroupNameFilter) entityFilter).getGroupType());
                case ENTITY_GROUP_LIST:
                    return Resource.groupResourceFromGroupType(((EntityGroupListFilter) entityFilter).getGroupType());
            }
        }
        if (entityGroupType != null) {
            return Resource.groupResourceFromGroupType(entityGroupType);
        } else {
            return Resource.resourceFromEntityType(entityType);
        }
    }

    public MergedGroupTypePermissionInfo getMergedReadAttrPermissionsByEntityType() {
        return userPermissions.getReadAttrPermissions().get(getResource());
    }

    public MergedGroupTypePermissionInfo getMergedReadTsPermissionsByEntityType() {
        return userPermissions.getReadTsPermissions().get(getResource());
    }

    public Map<Resource, MergedGroupTypePermissionInfo> getMergedReadEntityPermissionsMap() {
        return userPermissions.getReadEntityPermissions();
    }

    public Map<Resource, MergedGroupTypePermissionInfo> getMergedReadAttrPermissionsMap() {
        return userPermissions.getReadAttrPermissions();
    }

    public Map<Resource, MergedGroupTypePermissionInfo> getMergedReadTsPermissionsMap() {
        return userPermissions.getReadTsPermissions();
    }

    public boolean isEntityGroup() {
        return EntityType.ENTITY_GROUP.equals(entityType)
                || EntityFilterType.ENTITY_GROUP_NAME.equals(entityFilter.getType())
                || EntityFilterType.ENTITY_GROUP_LIST.equals(entityFilter.getType());
    }

}
