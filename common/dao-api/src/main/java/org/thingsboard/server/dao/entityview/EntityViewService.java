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
package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;

/**
 * Created by Victor Basanets on 8/27/2017.
 */
public interface EntityViewService {

    EntityView saveEntityView(EntityView entityView);

    EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId);

    EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name);

    PageData<EntityView> findEntityViewByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EntityView> findEntityViewByTenantIdAndType(TenantId tenantId, PageLink pageLink, String type);

    PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndIdsAsync(TenantId tenantId, List<EntityViewId> entityViewIds);

    PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, PageLink pageLink, String type);

    ListenableFuture<List<EntityView>> findEntityViewsByQuery(TenantId tenantId, EntityViewSearchQuery query);

    ListenableFuture<EntityView> findEntityViewByIdAsync(TenantId tenantId, EntityViewId entityViewId);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);

    void deleteEntityView(TenantId tenantId, EntityViewId entityViewId);

    void deleteEntityViewsByTenantId(TenantId tenantId);

    void deleteEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<EntitySubtype>> findEntityViewTypesByTenantId(TenantId tenantId);

    PageData<EntityView> findEntityViewsByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<EntityView> findEntityViewsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    PageData<EntityView> findEntityViewsByEntityGroupIdsAndType(List<EntityGroupId> groupIds, String type, PageLink pageLink);

}
