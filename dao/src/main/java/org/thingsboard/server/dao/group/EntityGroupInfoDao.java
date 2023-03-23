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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityGroupInfoDao extends Dao<EntityGroupInfo> {

    PageData<EntityGroupInfo> findEntityGroupsByType(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType, PageLink pageLink);

    PageData<EntityGroupInfo> findEntityGroupsByOwnerIdsAndType(UUID tenantId, List<UUID> ownerIds, EntityType groupType, PageLink pageLink);

    PageData<EntityGroupInfo> findEntityGroupsByIds(UUID tenantId, List<UUID> entityGroupIds, PageLink pageLink);

    PageData<EntityGroupInfo> findEntityGroupsByTypeOrIds(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType,
                                                          List<UUID> entityGroupIds, PageLink pageLink);

    PageData<EntityGroupInfo> findEdgeEntityGroupsByOwnerIdAndType(UUID tenantId, UUID edgeId, UUID ownerId, EntityType ownerType, String relationType, PageLink pageLink);

    Optional<EntityGroupInfo> findEntityGroupByTypeAndName(UUID tenantId, UUID parentEntityId,
                                                           EntityType parentEntityType, EntityType groupType, String name);

}
