/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.group;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public interface EntityGroupService {

    EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId);

    ListenableFuture<EntityGroup> findEntityGroupByIdAsync(TenantId tenantId, EntityGroupId entityGroupId);

    ListenableFuture<Boolean> checkEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityGroup entityGroup);

    ListenableFuture<Boolean> checkEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityGroupId entityGroupId, EntityType groupType);

    EntityGroup saveEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityGroup entityGroup);

    EntityGroup createEntityGroupAll(TenantId tenantId, EntityId parentEntityId, EntityType groupType);

    EntityGroup getOrCreateAdminsUserGroup(TenantId tenantId, EntityId parentEntityId);

    void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId);

    ListenableFuture<List<EntityGroup>> findAllEntityGroups(TenantId tenantId, EntityId parentEntityId);

    void deleteAllEntityGroups(TenantId tenantId, EntityId parentEntityId);

    ListenableFuture<List<EntityGroup>> findEntityGroupsByType(TenantId tenantId, EntityId parentEntityId, EntityType groupType);

    ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndName(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String name);

    void addEntityToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId);

    void addEntityToEntityGroupAll(TenantId tenantId, EntityId parentEntityId, EntityId entityId);

    void addEntitiesToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    void removeEntityFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId);

    void removeEntitiesFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    ShortEntityView findGroupEntity(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId,
                               BiFunction<ShortEntityView, List<EntityField>, ShortEntityView> transformFunction);

    ListenableFuture<TimePageData<ShortEntityView>> findEntities(TenantId tenantId, EntityGroupId entityGroupId, TimePageLink pageLink,
                                                            BiFunction<ShortEntityView, List<EntityField>, ShortEntityView> transformFunction);

    ListenableFuture<List<EntityId>> findAllEntityIds(TenantId tenantId, EntityGroupId entityGroupId, TimePageLink pageLink);

    ListenableFuture<List<EntityGroupId>> findEntityGroupsForEntity(TenantId tenantId, EntityId entityId);

}
