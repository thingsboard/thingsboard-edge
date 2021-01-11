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
package org.thingsboard.server.dao.sql.group;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
public class JpaEntityGroupDao extends JpaAbstractDao<EntityGroupEntity, EntityGroup> implements EntityGroupDao {

    @Autowired
    EntityGroupRepository entityGroupRepository;

    @Override
    protected Class<EntityGroupEntity> getEntityClass() {
        return EntityGroupEntity.class;
    }

    @Override
    protected CrudRepository<EntityGroupEntity, UUID> getCrudRepository() {
        return entityGroupRepository;
    }

    @Override
    public ListenableFuture<List<EntityGroup>> findEntityGroupsByIdsAsync(UUID tenantId, List<UUID> entityGroupIds) {
        return service.submit(() -> DaoUtil.convertDataList(entityGroupRepository.findEntityGroupsByIdIn(entityGroupIds)));
    }

    @Override
    public ListenableFuture<List<EntityGroup>> findEntityGroupsByType(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, String relationType) {
        return service.submit(() -> DaoUtil.convertDataList(entityGroupRepository.findEntityGroupsByType(
                parentEntityId,
                parentEntityType.name(),
                relationType)));
    }

    @Override
    public ListenableFuture<PageData<EntityGroup>> findEntityGroupsByTypeAndPageLink(UUID tenantId, UUID parentEntityId,
                                                                                     EntityType parentEntityType, String relationType, PageLink pageLink) {
        return service.submit(() -> DaoUtil.toPageData(entityGroupRepository
                .findEntityGroupsByTypeAndPageLink(
                        parentEntityId,
                        parentEntityType.name(),
                        relationType,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink))));
    }

    @Override
    public ListenableFuture<List<EntityGroup>> findAllEntityGroups(UUID tenantId, UUID parentEntityId, EntityType parentEntityType) {
        return service.submit(() -> DaoUtil.convertDataList(entityGroupRepository.findAllEntityGroups(
                parentEntityId,
                parentEntityType.name())));
    }

    @Override
    public ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndName(UUID tenantId, UUID parentEntityId, EntityType parentEntityType,
                                                                                String relationType, String name) {
        return service.submit(() ->
                Optional.ofNullable(DaoUtil.getData(entityGroupRepository.findEntityGroupByTypeAndName(
                        parentEntityId,
                        parentEntityType.name(),
                        relationType,
                        name))));
    }

    @Override
    public ListenableFuture<PageData<EntityId>> findGroupEntityIds(EntityType entityType, UUID groupId, PageLink pageLink) {
        return service.submit(() -> {
            Page<UUID> page = entityGroupRepository.findGroupEntityIds(groupId, entityType.name(), DaoUtil.toPageable(pageLink));
            List<EntityId> entityIds = page.getContent().stream().map(id ->
                    EntityIdFactory.getByTypeAndUuid(entityType, id)).collect(Collectors.toList());
            return new PageData(entityIds, page.getTotalPages(), page.getTotalElements(), page.hasNext());
        });
    }

    @Override
    public boolean isEntityInGroup(EntityId entityId, EntityGroupId entityGroupId) {
        return entityGroupRepository.isEntityInGroup(entityId.getId(), entityGroupId.getId());
    }
}
