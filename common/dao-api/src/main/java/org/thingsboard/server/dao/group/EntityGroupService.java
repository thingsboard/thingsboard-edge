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
package org.thingsboard.server.dao.group;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;

import java.util.List;
import java.util.Optional;

public interface EntityGroupService {

    EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId);

    ListenableFuture<EntityGroup> findEntityGroupByIdAsync(TenantId tenantId, EntityGroupId entityGroupId);

    ListenableFuture<List<EntityGroup>> findEntityGroupByIdsAsync(TenantId tenantId, List<EntityGroupId> entityGroupIds);

    ListenableFuture<Boolean> checkEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityGroup entityGroup);

    ListenableFuture<Boolean> checkEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityGroupId entityGroupId, EntityType groupType);

    EntityGroup saveEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityGroup entityGroup);

    EntityGroup createEntityGroupAll(TenantId tenantId, EntityId parentEntityId, EntityType groupType);

    EntityGroup findOrCreateUserGroup(TenantId tenantId, EntityId parentEntityId, String groupName, String description);

    EntityGroup findOrCreateEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String groupName,
                                        String description, CustomerId publicCustomerId);

    Optional<EntityGroup> findOwnerEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String groupName);

    EntityGroup findOrCreateTenantUsersGroup(TenantId tenantId);

    EntityGroup findOrCreateTenantAdminsGroup(TenantId tenantId);

    EntityGroup findOrCreateCustomerUsersGroup(TenantId tenantId, CustomerId customerId, CustomerId parentCustomerId);

    EntityGroup findOrCreateCustomerAdminsGroup(TenantId tenantId, CustomerId customerId, CustomerId parentCustomerId);

    EntityGroup findOrCreatePublicUsersGroup(TenantId tenantId, CustomerId customerId);

    EntityGroup findOrCreateReadOnlyEntityGroupForCustomer(TenantId tenantId, CustomerId customerId, EntityType groupType);

    ListenableFuture<Optional<EntityGroup>> findPublicUserGroup(TenantId tenantId, CustomerId publicCustomerId);

    void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId);

    ListenableFuture<List<EntityGroup>> findAllEntityGroups(TenantId tenantId, EntityId parentEntityId);

    void deleteAllEntityGroups(TenantId tenantId, EntityId parentEntityId);

    ListenableFuture<List<EntityGroup>> findEntityGroupsByType(TenantId tenantId, EntityId parentEntityId, EntityType groupType);

    ListenableFuture<PageData<EntityGroup>> findEntityGroupsByTypeAndPageLink(TenantId tenantId, EntityId parentEntityId,
                                                                              EntityType groupType, PageLink pageLink);

    ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndName(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String name);

    void addEntityToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId);

    void addEntityToEntityGroupAll(TenantId tenantId, EntityId parentEntityId, EntityId entityId);

    void addEntitiesToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    void removeEntityFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId);

    void removeEntitiesFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    ShortEntityView findGroupEntity(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityGroupId entityGroupId, EntityId entityId);

    PageData<ShortEntityView> findGroupEntities(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityGroupId entityGroupId, PageLink pageLink);

    ListenableFuture<List<EntityId>> findAllEntityIds(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink);

    ListenableFuture<List<EntityGroupId>> findEntityGroupsForEntity(TenantId tenantId, EntityId entityId);

    boolean isEntityInGroup(EntityId entityId, EntityGroupId entityGroupId);

}
