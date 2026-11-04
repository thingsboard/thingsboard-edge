// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.BaseDashboardProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class DashboardCloudProcessor extends BaseDashboardProcessor {

    public ListenableFuture<Void> processDashboardMsgFromCloud(TenantId tenantId, DashboardUpdateMsg dashboardUpdateMsg) throws ThingsboardException {
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (dashboardUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    boolean created = super.saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg);
                    if (created) {
                        pushDashboardCreatedEventToRuleEngine(tenantId, dashboardId);
                        return requestForAdditionalData(tenantId, dashboardId);
                    }
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    if (dashboardUpdateMsg.hasEntityGroupIdMSB() && dashboardUpdateMsg.hasEntityGroupIdLSB()) {
                        UUID entityGroupUUID = safeGetUUID(dashboardUpdateMsg.getEntityGroupIdMSB(),
                                dashboardUpdateMsg.getEntityGroupIdLSB());
                        EntityGroupId entityGroupId =
                                new EntityGroupId(entityGroupUUID);
                        edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, dashboardId);
                    } else {
                        deleteDashboard(tenantId, dashboardId);
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
        Dashboard dashboard = edgeCtx.getDashboardService().findDashboardById(tenantId, dashboardId);
        pushEntityEventToRuleEngine(tenantId, dashboard, TbMsgType.ENTITY_CREATED);
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        DashboardId dashboardId = new DashboardId(cloudEvent.getEntityId());
        EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED, ADDED_TO_ENTITY_GROUP -> {
                Dashboard dashboard = edgeCtx.getDashboardService().findDashboardById(cloudEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    DashboardUpdateMsg dashboardUpdateMsg = EdgeMsgConstructorUtils.constructDashboardUpdatedMsg(msgType, dashboard, entityGroupId);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDashboardUpdateMsg(dashboardUpdateMsg).build();
                } else {
                    log.info("Skipping event as dashboard was not found [{}]", cloudEvent);
                }
            }
            case DELETED, REMOVED_FROM_ENTITY_GROUP -> {
                DashboardUpdateMsg dashboardUpdateMsg = EdgeMsgConstructorUtils.constructDashboardDeleteMsg(dashboardId, entityGroupId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDashboardUpdateMsg(dashboardUpdateMsg).build();
            }
        }
        return null;
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.DASHBOARD;
    }

}
