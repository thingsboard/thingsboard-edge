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
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Slf4j
public abstract class BaseDashboardProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg) throws ThingsboardException {
        boolean created = false;
        Dashboard dashboard = constructDashboardFromUpdateMsg(tenantId, dashboardId, dashboardUpdateMsg);
        if (dashboard == null) {
            throw new RuntimeException("[{" + tenantId + "}] dashboardUpdateMsg {" + dashboardUpdateMsg + "} cannot be converted to dashboard");
        }
        Dashboard dashboardById = dashboardService.findDashboardById(tenantId, dashboardId);
        if (dashboardById == null) {
            created = true;
            dashboard.setId(null);
        } else {
            dashboard.setId(dashboardId);
            changeOwnerIfRequired(tenantId, null, dashboardId);
        }
        // EDGE PE only: check if customer exists
        if (isCustomerNotExists(tenantId, dashboard.getCustomerId())) {
            dashboard.setCustomerId(null);
        }
        dashboardValidator.validate(dashboard, Dashboard::getTenantId);
        if (created) {
            dashboard.setId(dashboardId);
        }
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard, false);
        if (created) {
            entityGroupService.addEntityToEntityGroupAll(savedDashboard.getTenantId(), savedDashboard.getOwnerId(), savedDashboard.getId());
        }
        safeAddToEntityGroup(tenantId, dashboardUpdateMsg, dashboardId);
        return created;
    }

    private void safeAddToEntityGroup(TenantId tenantId, DashboardUpdateMsg dashboardUpdateMsg, DashboardId dashboardId) {
        if (dashboardUpdateMsg.hasEntityGroupIdMSB() && dashboardUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(dashboardUpdateMsg.getEntityGroupIdMSB(),
                    dashboardUpdateMsg.getEntityGroupIdLSB());
            safeAddEntityToGroup(tenantId, new EntityGroupId(entityGroupUUID), dashboardId);
        }
    }

    protected abstract Dashboard constructDashboardFromUpdateMsg(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg);
}
