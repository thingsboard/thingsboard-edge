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
package org.thingsboard.server.dao.sql.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
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

import java.util.Map;

public class QuerySecurityContext {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final CustomerId customerId;

    private final EntityType entityType;

    private final MergedUserPermissions userPermissions;

    private final EntityFilter entityFilter;

    @Getter
    @Setter
    private final EntityId ownerId;

    @Getter
    @Setter
    private final EntityType entityGroupType;

    public QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, null, null);
    }

    public QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, EntityId ownerId) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, ownerId, null);
    }

    public QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, EntityType entityGroupType) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, null, entityGroupType);
    }

    private QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, EntityId ownerId, EntityType entityGroupType) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.entityType = entityType;
        this.userPermissions = userPermissions;
        this.entityFilter = entityFilter;
        this.ownerId = ownerId;
        this.entityGroupType = entityGroupType;
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
                ||  EntityFilterType.ENTITY_GROUP_NAME.equals(entityFilter.getType())
                ||  EntityFilterType.ENTITY_GROUP_LIST.equals(entityFilter.getType());
    }
}
