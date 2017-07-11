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
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.group.ColumnConfiguration;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasUUID;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public interface EntityGroupService {

    EntityGroup findEntityGroupById(EntityGroupId entityGroupId);

    ListenableFuture<EntityGroup> findEntityGroupByIdAsync(EntityGroupId entityGroupId);

    EntityGroup saveEntityGroup(EntityId parentEntityId, EntityGroup entityGroup);

    EntityGroup createEntityGroupAll(EntityId parentEntityId, EntityType groupType);

    void deleteEntityGroup(EntityGroupId entityGroupId);

    ListenableFuture<List<EntityGroup>> findAllEntityGroups(EntityId parentEntityId);

    void deleteAllEntityGroups(EntityId parentEntityId);

    ListenableFuture<List<EntityGroup>> findEntityGroupsByType(EntityId parentEntityId, EntityType groupType);

    ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndName(EntityId parentEntityId, EntityType groupType, String name);

    void addEntityToEntityGroup(EntityGroupId entityGroupId, EntityId entityId);

    void addEntityToEntityGroupAll(EntityId parentEntityId, EntityId entityId);

    void addEntitiesToEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds);

    void removeEntityFromEntityGroup(EntityGroupId entityGroupId, EntityId entityId);

    void removeEntitiesFromEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds);

    EntityView findGroupEntity(EntityGroupId entityGroupId, EntityId entityId,
                               BiFunction<EntityView, List<EntityField>, EntityView> transformFunction);

    ListenableFuture<TimePageData<EntityView>> findEntities(EntityGroupId entityGroupId, TimePageLink pageLink,
                                                            BiFunction<EntityView, List<EntityField>, EntityView> transformFunction);

}
