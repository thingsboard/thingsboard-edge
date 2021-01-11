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
package org.thingsboard.server.dao.sql.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.dao.entity.EntityQueryDao;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JpaEntityQueryDao implements EntityQueryDao {

    @Autowired
    private EntityQueryRepository entityQueryRepository;

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityCountQuery query) {
        return entityQueryRepository.countEntitiesByQuery(tenantId, customerId, userPermissions, query);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityDataQuery query) {
        return entityQueryRepository.findEntityDataByQuery(tenantId, customerId, userPermissions, query);
    }

    @Override
    public <T> PageData<T> findInCustomerHierarchyByRootCustomerIdOrOtherGroupIdsAndType(TenantId tenantId, CustomerId customerId, EntityType entityType, String type, List<EntityGroupId> groupIds, PageLink pageLink, Function<Map<String, Object>, T> rowMapping) {
        return entityQueryRepository.findInCustomerHierarchyByRootCustomerIdOrOtherGroupIdsAndType(tenantId, customerId, entityType, type, groupIds, pageLink, rowMapping);
    }
}
