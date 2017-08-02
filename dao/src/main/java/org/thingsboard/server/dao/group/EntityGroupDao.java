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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;

public interface EntityGroupDao extends Dao<EntityGroup> {

    EntityGroup save(EntityGroup entityGroup);

    ListenableFuture<List<EntityGroup>> findAllEntityGroups(EntityId parentEntityId);

    ListenableFuture<List<EntityGroup>> findEntityGroupsByType(EntityId parentEntityId, EntityType groupType);

    ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndName(EntityId parentEntityId, EntityType groupType, String name);

    ListenableFuture<List<EntityId>> findEntityIds(EntityGroupId entityGroupId, EntityType groupType, TimePageLink pageLink);

}

