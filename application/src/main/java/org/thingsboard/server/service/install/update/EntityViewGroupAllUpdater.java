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
package org.thingsboard.server.service.install.update;

import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;

class EntityViewGroupAllUpdater extends EntityGroupAllPaginatedUpdater<EntityViewId, EntityView> {

    private final EntityViewService entityViewService;

    public EntityViewGroupAllUpdater(EntityViewService entityViewService, CustomerService customerService,
                                     EntityGroupService entityGroupService, EntityGroup groupAll, boolean fetchAllTenantEntities) {
        super(customerService,
                entityGroupService,
                groupAll,
                fetchAllTenantEntities,
                (tenantId, pageLink) -> entityViewService.findEntityViewByTenantId(tenantId, pageLink),
                (tenantId, entityViewIds) -> entityViewService.findEntityViewsByTenantIdAndIdsAsync(tenantId, entityViewIds),
                entityId -> new EntityViewId(entityId.getId()),
                entityView -> entityView.getId());
        this.entityViewService = entityViewService;
    }


    @Override
    protected void unassignFromCustomer(EntityView entity) {
        entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        entityViewService.saveEntityView(entity);
    }

    @Override
    protected String getName() {
        return "Entity views group all updater";
    }

}
