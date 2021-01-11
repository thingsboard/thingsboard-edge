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
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GroupIdsWrapper {

    @Getter
    private Map<GroupIdPermKey, Set<EntityGroupId>> groupIdsMap;

    public GroupIdsWrapper(MergedGroupTypePermissionInfo readPermissions, MergedGroupTypePermissionInfo attrPermissions, MergedGroupTypePermissionInfo tsPermissions) {
        groupIdsMap = new HashMap<>();

        Set<EntityGroupId> readGroups = getGroupsSet(readPermissions);
        Set<EntityGroupId> attrGroups = getGroupsSet(attrPermissions);
        Set<EntityGroupId> tsGroups = getGroupsSet(tsPermissions);

        for (EntityGroupId entityGroupId : readGroups) {
            add(entityGroupId, true, attrGroups.contains(entityGroupId), tsGroups.contains(entityGroupId));
        }
        for (EntityGroupId entityGroupId : attrGroups) {
            add(entityGroupId, readGroups.contains(entityGroupId), true, tsGroups.contains(entityGroupId));
        }
        for (EntityGroupId entityGroupId : tsGroups) {
            add(entityGroupId, readGroups.contains(entityGroupId), attrGroups.contains(entityGroupId), true);
        }
    }

    private void add(EntityGroupId id, boolean readFlag, boolean attrFlag, boolean tsFlag) {
        GroupIdPermKey key = new GroupIdPermKey(readFlag, attrFlag, tsFlag);
        groupIdsMap.computeIfAbsent(key, tmp -> new HashSet<>()).add(id);
    }

    private Set<EntityGroupId> getGroupsSet(MergedGroupTypePermissionInfo readPermissions) {
        return readPermissions.getEntityGroupIds() == null ? Collections.emptySet() : new HashSet<>(readPermissions.getEntityGroupIds());
    }
}
