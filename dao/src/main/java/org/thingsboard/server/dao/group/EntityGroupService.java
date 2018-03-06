/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

    ListenableFuture<Boolean> checkEntityGroup(EntityId parentEntityId, EntityGroup entityGroup);

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

    ListenableFuture<List<EntityGroupId>> findEntityGroupsForEntity(EntityId entityId);

}
