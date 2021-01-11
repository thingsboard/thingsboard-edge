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
package org.thingsboard.server.dao.group;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityGroupDao extends Dao<EntityGroup> {
    /**
     * Find entity groups by entity group Ids.
     *
     * @param tenantId the tenantId
     * @param entityGroupIds the entity group Ids
     * @return the list of entity group objects
     */
    ListenableFuture<List<EntityGroup>> findEntityGroupsByIdsAsync(UUID tenantId, List<UUID> entityGroupIds);

    ListenableFuture<List<EntityGroup>> findEntityGroupsByType(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, String relationType);

    ListenableFuture<PageData<EntityGroup>> findEntityGroupsByTypeAndPageLink
            (UUID tenantId, UUID parentEntityId, EntityType parentEntityType, String relationType, PageLink pageLink);

    ListenableFuture<List<EntityGroup>> findAllEntityGroups(UUID tenantId, UUID parentEntityId, EntityType parentEntityType);

    ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndName(UUID tenantId, UUID parentEntityId,
                                                                         EntityType parentEntityType, String relationType, String name);

    ListenableFuture<PageData<EntityId>> findGroupEntityIds(EntityType entityType, UUID groupId, PageLink pageLink);

    boolean isEntityInGroup(EntityId entityId, EntityGroupId entityGroupId);
}
