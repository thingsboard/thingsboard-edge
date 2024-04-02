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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.BaseDashboardProcessor;

import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class DashboardCloudProcessor extends BaseDashboardProcessor {

    @Autowired
    private DashboardService dashboardService;

    public ListenableFuture<Void> processDashboardMsgFromCloud(TenantId tenantId,
                                                               DashboardUpdateMsg dashboardUpdateMsg,
                                                               CustomerId customerId,
                                                               Long queueStartTs) {
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (dashboardUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    boolean created = super.saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg, customerId);
                    if (created) {
                        pushDashboardCreatedEventToRuleEngine(tenantId, dashboardId);
                    }
                    return requestForAdditionalData(tenantId, dashboardId, queueStartTs);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Dashboard dashboardById = dashboardService.findDashboardById(tenantId, dashboardId);
                    if (dashboardById != null) {
                        dashboardService.deleteDashboard(tenantId, dashboardId);
                        pushDashboardDeletedEventToRuleEngine(tenantId, dashboardById);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(dashboardUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void pushDashboardCreatedEventToRuleEngine(TenantId tenantId, DashboardId dashboardId) {
        Dashboard dashboard = dashboardService.findDashboardById(tenantId, dashboardId);
        pushDashboardEventToRuleEngine(tenantId, dashboard, TbMsgType.ENTITY_CREATED);
    }

    private void pushDashboardDeletedEventToRuleEngine(TenantId tenantId, Dashboard dashboard) {
        pushDashboardEventToRuleEngine(tenantId, dashboard, TbMsgType.ENTITY_DELETED);
    }

    private void pushDashboardEventToRuleEngine(TenantId tenantId, Dashboard dashboard, TbMsgType msgType) {
        try {
            String dashboardAsString = JacksonUtil.toString(dashboard);
            pushEntityEventToRuleEngine(tenantId, dashboard.getId(), null, msgType, dashboardAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push dashboard action to rule engine: {}", tenantId, dashboard.getId(), msgType.name(), e);
        }
    }

    public UplinkMsg convertDashboardEventToUplink(CloudEvent cloudEvent, EdgeVersion edgeVersion) {
        DashboardId dashboardId = new DashboardId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER -> {
                Dashboard dashboard = dashboardService.findDashboardById(cloudEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    DashboardUpdateMsg dashboardUpdateMsg = ((DashboardMsgConstructor)
                            dashboardMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDashboardUpdatedMsg(msgType, dashboard);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDashboardUpdateMsg(dashboardUpdateMsg).build();
                } else {
                    log.info("Skipping event as dashboard was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                DashboardUpdateMsg dashboardUpdateMsg = ((DashboardMsgConstructor)
                        dashboardMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDashboardDeleteMsg(dashboardId);
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDashboardUpdateMsg(dashboardUpdateMsg).build();
            }
        }
        return msg;
    }

    @Override
    protected Dashboard constructDashboardFromUpdateMsg(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg) {
        return JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
    }

    @Override
    protected Set<ShortCustomerInfo> filterNonExistingCustomers(TenantId tenantId, Set<ShortCustomerInfo> assignedCustomers) {
        if (assignedCustomers != null && !assignedCustomers.isEmpty()) {
            assignedCustomers.removeIf(assignedCustomer ->
                    checkCustomerOnEdge(tenantId, assignedCustomer.getCustomerId()) == null);
        }
        return assignedCustomers;
    }

    private CustomerId checkCustomerOnEdge(TenantId tenantId, CustomerId customerId) {
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        return customer != null ? customer.getId() : null;
    }

}
