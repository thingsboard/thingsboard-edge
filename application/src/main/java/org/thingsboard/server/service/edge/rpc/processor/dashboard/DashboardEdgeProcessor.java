/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructor;

import java.util.Set;
import java.util.UUID;

@Slf4j
public abstract class DashboardEdgeProcessor extends BaseDashboardProcessor implements DashboardProcessor {

    @Override
    public ListenableFuture<Void> processDashboardMsgFromEdge(TenantId tenantId, Edge edge, DashboardUpdateMsg dashboardUpdateMsg) {
        log.trace("[{}] executing processDashboardMsgFromEdge [{}] from edge [{}]", tenantId, dashboardUpdateMsg, edge.getId());
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (dashboardUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Dashboard dashboardToDelete = dashboardService.findDashboardById(tenantId, dashboardId);
                    if (dashboardToDelete != null) {
                        dashboardService.unassignDashboardFromEdge(tenantId, dashboardId, edge.getId());
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(dashboardUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed dashboard violated {}", tenantId, dashboardUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private void saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg, Edge edge) {
        boolean created = super.saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg,
                edge.getCustomerId());
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), dashboardId);
            pushDashboardCreatedEventToRuleEngine(tenantId, edge, dashboardId);
            dashboardService.assignDashboardToEdge(tenantId, dashboardId, edge.getId());
        }
    }

    private void pushDashboardCreatedEventToRuleEngine(TenantId tenantId, Edge edge, DashboardId dashboardId) {
        try {
            Dashboard dashboard = dashboardService.findDashboardById(tenantId, dashboardId);
            String dashboardAsString = JacksonUtil.toString(dashboard);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, null);
            pushEntityEventToRuleEngine(tenantId, dashboardId, null, TbMsgType.ENTITY_CREATED, dashboardAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push dashboard action to rule engine: {}", tenantId, dashboardId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    @Override
    public DownlinkMsg convertDashboardEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        DashboardId dashboardId = new DashboardId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED, ASSIGNED_TO_EDGE, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER -> {
                Dashboard dashboard = dashboardService.findDashboardById(edgeEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DashboardUpdateMsg dashboardUpdateMsg = ((DashboardMsgConstructor)
                            dashboardMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDashboardUpdatedMsg(msgType, dashboard);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDashboardUpdateMsg(dashboardUpdateMsg)
                            .build();
                }
            }
            case DELETED, UNASSIGNED_FROM_EDGE -> {
                DashboardUpdateMsg dashboardUpdateMsg = ((DashboardMsgConstructor)
                        dashboardMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDashboardDeleteMsg(dashboardId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDashboardUpdateMsg(dashboardUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    @Override
    protected Set<ShortCustomerInfo> filterNonExistingCustomers(TenantId tenantId, Set<ShortCustomerInfo> assignedCustomers) {
        // do nothing on cloud
        return assignedCustomers;
    }

}
