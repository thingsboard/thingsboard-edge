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
package org.thingsboard.server.dao.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;

public interface DashboardService {
    
    Dashboard findDashboardById(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<Dashboard> findDashboardByIdAsync(TenantId tenantId, DashboardId dashboardId);

    DashboardInfo findDashboardInfoById(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<List<DashboardInfo>> findDashboardInfoByIdsAsync(TenantId tenantId, List<DashboardId> dashboardIds);

    Dashboard saveDashboard(Dashboard dashboard);

    Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    Dashboard unassignDashboardFromCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    void deleteDashboard(TenantId tenantId, DashboardId dashboardId);

    PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink);

    void deleteDashboardsByTenantId(TenantId tenantId);

    void deleteDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    PageData<DashboardInfo> findDashboardsByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<DashboardInfo> findDashboardsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    List<Dashboard> exportDashboards(TenantId tenantId, EntityGroupId entityGroupId, TimePageLink pageLink) throws ThingsboardException;

    void importDashboards(TenantId tenantId, EntityGroupId entityGroupId, List<Dashboard> dashboards, boolean overwrite) throws ThingsboardException;

}
