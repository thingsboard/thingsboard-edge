/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityGroupDao extends Dao<EntityGroup>, ExportableEntityDao<EntityGroupId, EntityGroup> {
    /**
     * Find entity groups by entity group Ids.
     *
     * @param tenantId the tenantId
     * @param entityGroupIds the entity group Ids
     * @return the list of entity group objects
     */
    ListenableFuture<List<EntityGroup>> findEntityGroupsByIdsAsync(UUID tenantId, List<UUID> entityGroupIds);

    ListenableFuture<List<EntityGroup>> findEntityGroupsByType(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType);

    ListenableFuture<PageData<EntityGroup>> findEntityGroupsByTypeAndPageLink
            (UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType, PageLink pageLink);

    PageData<EntityGroup> findEntityGroupsByTypeAndPageLink(UUID tenantId, EntityType groupType, PageLink pageLink);

    ListenableFuture<List<EntityGroup>> findAllEntityGroups(UUID tenantId, UUID parentEntityId, EntityType parentEntityType);

    Optional<EntityGroup> findEntityGroupByTypeAndName(UUID tenantId, UUID parentEntityId,
                                                       EntityType parentEntityType, EntityType groupType, String name);

    ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndNameAsync(UUID tenantId, UUID parentEntityId,
                                                                              EntityType parentEntityType, EntityType groupType, String name);

    ListenableFuture<PageData<EntityId>> findGroupEntityIds(EntityType entityType, UUID groupId, PageLink pageLink);

    PageData<EntityId> findGroupEntityIdsSync(EntityType entityType, UUID groupId, PageLink pageLink);

    PageData<EntityGroup> findEdgeEntityGroupsByType(UUID tenantId, UUID edgeId, String relationType, PageLink pageLink);

}
