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
package org.thingsboard.server.common.data.permission;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MergedUserPermissions {

    @Getter
    private final Map<Resource, Set<Operation>> genericPermissions;
    @Getter
    private final Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions;
    @Getter
    private final Map<EntityType, MergedGroupTypePermissionInfo> readGroupPermissions;

    @Getter
    private final Map<Resource, MergedGroupTypePermissionInfo> readEntityPermissions;
    @Getter
    private final Map<Resource, MergedGroupTypePermissionInfo> readAttrPermissions;
    @Getter
    private final Map<Resource, MergedGroupTypePermissionInfo> readTsPermissions;


    public MergedUserPermissions(Map<Resource, Set<Operation>> genericPermissions, Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions) {
        this.genericPermissions = genericPermissions;
        this.groupPermissions = groupPermissions;
        this.readGroupPermissions = new HashMap<>();
        this.readEntityPermissions = new HashMap<>();
        this.readAttrPermissions = new HashMap<>();
        this.readTsPermissions = new HashMap<>();
        for (EntityType groupType : EntityGroup.groupTypes) {
            Resource resource = Resource.groupResourceFromGroupType(groupType);
            boolean hasGenericRead = hasGenericPermission(resource, Operation.READ);
            this.readGroupPermissions.put(groupType, new MergedGroupTypePermissionInfo(new ArrayList<>(), hasGenericRead));
            this.readEntityPermissions.put(resource, new MergedGroupTypePermissionInfo(new ArrayList<>(), hasGenericRead));
            boolean hasGenericAttrRead = hasGenericPermission(resource, Operation.READ_ATTRIBUTES);
            this.readAttrPermissions.put(resource, new MergedGroupTypePermissionInfo(new ArrayList<>(), hasGenericAttrRead));
            boolean hasGenericTsRead = hasGenericPermission(resource, Operation.READ_TELEMETRY);
            this.readTsPermissions.put(resource, new MergedGroupTypePermissionInfo(new ArrayList<>(), hasGenericTsRead));
        }

        for (EntityType entityType : EntityType.values()) {
            Resource resource = Resource.resourceFromEntityType(entityType);
            if (resource != null) {
                boolean hasGenericRead = hasGenericPermission(resource, Operation.READ);
                this.readEntityPermissions.put(resource, new MergedGroupTypePermissionInfo(new ArrayList<>(), hasGenericRead));
                boolean hasGenericReadAttributes = hasGenericPermission(resource, Operation.READ_ATTRIBUTES);
                this.readAttrPermissions.put(resource, new MergedGroupTypePermissionInfo(new ArrayList<>(), hasGenericReadAttributes));
                boolean hasGenericReadTelemetry = hasGenericPermission(resource, Operation.READ_TELEMETRY);
                this.readTsPermissions.put(resource, new MergedGroupTypePermissionInfo(new ArrayList<>(), hasGenericReadTelemetry));
            }
        }

        this.groupPermissions.forEach((id, info) -> {
            if (checkOperation(info.getOperations(), Operation.READ)) {
                this.readGroupPermissions.get(info.getEntityType()).getEntityGroupIds().add(id);
                Resource entityResource = Resource.resourceFromEntityType(info.getEntityType());
                this.readEntityPermissions.get(entityResource).getEntityGroupIds().add(id);
                Resource groupResource = Resource.groupResourceFromGroupType(info.getEntityType());
                if (groupResource != null) {
                    this.readEntityPermissions.get(groupResource).getEntityGroupIds().add(id);
                }
            }
            if (checkOperation(info.getOperations(), Operation.READ_ATTRIBUTES)) {
                Resource entityResource = Resource.resourceFromEntityType(info.getEntityType());
                this.readAttrPermissions.get(entityResource).getEntityGroupIds().add(id);
                Resource groupResource = Resource.groupResourceFromGroupType(info.getEntityType());
                if (groupResource != null) {
                    this.readAttrPermissions.get(groupResource).getEntityGroupIds().add(id);
                }
            }
            if (checkOperation(info.getOperations(), Operation.READ_TELEMETRY)) {
                Resource entityResource = Resource.resourceFromEntityType(info.getEntityType());
                this.readTsPermissions.get(entityResource).getEntityGroupIds().add(id);
                Resource groupResource = Resource.groupResourceFromGroupType(info.getEntityType());
                if (groupResource != null) {
                    this.readTsPermissions.get(groupResource).getEntityGroupIds().add(id);
                }
            }
        });
    }

    public MergedGroupTypePermissionInfo getGroupPermissionsByEntityTypeAndOperation(EntityType entityType, Operation operation) {
        Resource resource = Resource.resourceFromEntityType(entityType);
        boolean hasGenericPermission = hasGenericPermission(resource, operation);
        List<EntityGroupId> groupIds = this.groupPermissions.entrySet().stream().filter((entry) ->
                entry.getValue().getEntityType().equals(entityType) && checkOperation(entry.getValue().getOperations(), operation)
        ).map(Map.Entry::getKey).collect(Collectors.toList());
        return new MergedGroupTypePermissionInfo(groupIds, hasGenericPermission);
    }

    public boolean hasGenericPermission(Resource resource, Operation operation) {
        return hasGenericResourcePermission(resource, operation) || hasGenericAllPermission(operation);
    }

    private boolean hasGenericAllPermission(Operation operation) {
        Set<Operation> operations = genericPermissions.get(Resource.ALL);
        return operations != null && checkOperation(operations, operation);
    }

    private boolean hasGenericResourcePermission(Resource resource, Operation operation) {
        Set<Operation> operations = genericPermissions.get(resource);
        return operations != null && checkOperation(operations, operation);
    }

    private boolean checkOperation(Set<Operation> operations, Operation operation) {
        return operations.contains(Operation.ALL) || operations.contains(operation);
    }

    public boolean hasGroupPermissions(EntityGroupId entityGroupId, Operation operation) {
        MergedGroupPermissionInfo permissionInfo = groupPermissions.get(entityGroupId);
        return permissionInfo != null && checkOperation(permissionInfo.getOperations(), operation);
    }
}
