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
package org.thingsboard.server.dao.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface DashboardService extends EntityDaoService {
    
    Dashboard findDashboardById(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<Dashboard> findDashboardByIdAsync(TenantId tenantId, DashboardId dashboardId);

    DashboardInfo findDashboardInfoById(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<List<DashboardInfo>> findDashboardInfoByIdsAsync(TenantId tenantId, List<DashboardId> dashboardIds);

    Dashboard saveDashboard(Dashboard dashboard, boolean doValidate);

    Dashboard saveDashboard(Dashboard dashboard);

    Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    Dashboard unassignDashboardFromCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    void deleteDashboard(TenantId tenantId, DashboardId dashboardId);

    PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<DashboardInfo> findMobileDashboardsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    void deleteDashboardsByTenantId(TenantId tenantId);

    void deleteDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    PageData<DashboardInfo> findDashboardsByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<DashboardInfo> findDashboardsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    PageData<DashboardInfo> findMobileDashboardsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    List<Dashboard> exportDashboards(TenantId tenantId, EntityGroupId entityGroupId, TimePageLink pageLink) throws ThingsboardException;

    void importDashboards(TenantId tenantId, EntityGroupId entityGroupId, List<Dashboard> dashboards, boolean overwrite) throws ThingsboardException;

    DashboardInfo findFirstDashboardInfoByTenantIdAndName(TenantId tenantId, String name);

    List<Dashboard> findTenantDashboardsByTitle(TenantId tenantId, String title);

}
