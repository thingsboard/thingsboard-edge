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
package org.thingsboard.server.service.entitiy.dashboard;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbDashboardService extends AbstractTbEntityService implements TbDashboardService {

    private final DashboardService dashboardService;

    @Override
    public Dashboard save(Dashboard dashboard, List<EntityGroup> entityGroups, User user) throws Exception {
        ActionType actionType = dashboard.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = dashboard.getTenantId();
        try {
            Dashboard savedDashboard = checkNotNull(dashboardService.saveDashboard(dashboard));
            autoCommit(user, savedDashboard.getId());
            createOrUpdateGroupEntity(tenantId, savedDashboard, entityGroups, actionType, user);
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), dashboard, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(Dashboard dashboard, User user) {
        DashboardId dashboardId = dashboard.getId();
        TenantId tenantId = dashboard.getTenantId();
        try {
            List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, dashboardId);
            dashboardService.deleteDashboard(tenantId, dashboardId);
            notificationEntityService.notifyDeleteEntity(tenantId, dashboardId, dashboard, null,
                    ActionType.DELETED, relatedEdgeIds, user, dashboardId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), ActionType.DELETED, user, e, dashboardId.toString());
            throw e;
        }
    }
}
