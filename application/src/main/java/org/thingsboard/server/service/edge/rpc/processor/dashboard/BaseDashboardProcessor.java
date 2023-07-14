/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Slf4j
public abstract class BaseDashboardProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, CustomerId customerId, DashboardUpdateMsg dashboardUpdateMsg) throws ThingsboardException {
        boolean created = false;
        dashboardCreationLock.lock();
        try {
            edgeSynchronizationManager.getSync().set(true);

            Dashboard dashboard = dashboardService.findDashboardById(tenantId, dashboardId);
            if (dashboard == null) {
                created = true;
                dashboard = new Dashboard();
                dashboard.setTenantId(tenantId);
                dashboard.setCreatedTime(Uuids.unixTimestamp(dashboardId.getId()));
            } else {
                changeOwnerIfRequired(tenantId, null, dashboardId);
            }
            dashboard.setTitle(dashboardUpdateMsg.getTitle());
            dashboard.setConfiguration(JacksonUtil.toJsonNode(dashboardUpdateMsg.getConfiguration()));
            dashboard.setCustomerId(customerId);
            dashboardValidator.validate(dashboard, Dashboard::getTenantId);
            if (created) {
                dashboard.setId(dashboardId);
            }
            Dashboard savedDashboard = dashboardService.saveDashboard(dashboard, false);
            if (created) {
                entityGroupService.addEntityToEntityGroupAll(savedDashboard.getTenantId(), savedDashboard.getOwnerId(), savedDashboard.getId());
            }
            safeAddToEntityGroup(tenantId, dashboardUpdateMsg, dashboardId);
        } finally {
            edgeSynchronizationManager.getSync().remove();
            dashboardCreationLock.unlock();
        }
        return created;
    }

    private void safeAddToEntityGroup(TenantId tenantId, DashboardUpdateMsg dashboardUpdateMsg, DashboardId dashboardId) {
        if (dashboardUpdateMsg.hasEntityGroupIdMSB() && dashboardUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(dashboardUpdateMsg.getEntityGroupIdMSB(),
                    dashboardUpdateMsg.getEntityGroupIdLSB());
            safeAddEntityToGroup(tenantId, new EntityGroupId(entityGroupUUID), dashboardId);
        }
    }
}
