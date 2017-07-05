/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.group;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;

public interface EntityGroupService {

    EntityGroup findEntityGroupById(EntityGroupId entityGroupId);

    ListenableFuture<EntityGroup> findEntityGroupByIdAsync(EntityGroupId entityGroupId);

    EntityGroup saveEntityGroup(EntityId parentEntityId, EntityGroup entityGroup);

    EntityGroup createEntityGroupAll(EntityId parentEntityId, EntityType groupType);

    void deleteEntityGroup(EntityGroupId entityGroupId);

    ListenableFuture<List<EntityGroup>> findAllEntityGroups(EntityId parentEntityId);

    void deleteAllEntityGroups(EntityId parentEntityId);

    ListenableFuture<List<EntityGroup>> findEntityGroupsByType(EntityId parentEntityId, EntityType groupType);

    void addEntityToEntityGroup(EntityGroupId entityGroupId, EntityId entityId);

    void addEntityToEntityGroupAll(EntityId parentEntityId, EntityId entityId);

    void addEntitiesToEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds);

    void removeEntityFromEntityGroup(EntityGroupId entityGroupId, EntityId entityId);

    void removeEntitiesFromEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds);

    <T extends BaseData<? extends UUIDBased>> ListenableFuture<TimePageData<T>> findEntities(EntityGroupId entityGroupId,
                                                                                             EntityType groupType, TimePageLink pageLink,
                                                                                             Function<EntityId, T> transformFunction);

}
