/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.entity;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;

import java.util.Optional;

public interface EntityService {

    Optional<String> fetchEntityName(TenantId tenantId, EntityId entityId);

    Optional<CustomerId> fetchEntityCustomerId(TenantId tenantId, EntityId entityId);

    long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityCountQuery query);

    PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityDataQuery query);

    <T extends GroupEntity<? extends EntityId>> PageData<T> findUserEntities(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions,
                                                                             EntityType entityType, Operation operation, String type, PageLink pageLink);

    <T extends GroupEntity<? extends EntityId>> PageData<T> findUserEntities(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions,
                                                                             EntityType entityType, Operation operation, String type, PageLink pageLink, boolean mobile);

}
