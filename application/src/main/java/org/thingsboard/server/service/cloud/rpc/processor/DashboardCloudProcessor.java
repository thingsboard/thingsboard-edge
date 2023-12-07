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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
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

import java.util.UUID;

@Component
@Slf4j
public class DashboardCloudProcessor extends BaseDashboardProcessor {

    @Autowired
    private DashboardService dashboardService;

    public ListenableFuture<Void> processDashboardMsgFromCloud(TenantId tenantId,
                                                               DashboardUpdateMsg dashboardUpdateMsg,
                                                               Long queueStartTs) throws ThingsboardException {
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (dashboardUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    boolean created = super.saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg);
                    if (created) {
                        pushDashboardCreatedEventToRuleEngine(tenantId, dashboardId);
                    }
                    return requestForAdditionalData(tenantId, dashboardId, queueStartTs);
                case ENTITY_DELETED_RPC_MESSAGE:
                    if (dashboardUpdateMsg.hasEntityGroupIdMSB() && dashboardUpdateMsg.hasEntityGroupIdLSB()) {
                        UUID entityGroupUUID = safeGetUUID(dashboardUpdateMsg.getEntityGroupIdMSB(),
                                dashboardUpdateMsg.getEntityGroupIdLSB());
                        EntityGroupId entityGroupId =
                                new EntityGroupId(entityGroupUUID);
                        entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, dashboardId);
                    } else {
                        Dashboard dashboardById = dashboardService.findDashboardById(tenantId, dashboardId);
                        if (dashboardById != null) {
                            dashboardService.deleteDashboard(tenantId, dashboardId);
                            pushDashboardDeletedEventToRuleEngine(tenantId, dashboardById);
                        }
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
        EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
        switch (cloudEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ADDED_TO_ENTITY_GROUP:
                Dashboard dashboard = dashboardService.findDashboardById(cloudEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    DashboardUpdateMsg dashboardUpdateMsg = ((DashboardMsgConstructor)
                            dashboardMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDashboardUpdatedMsg(msgType, dashboard, entityGroupId);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDashboardUpdateMsg(dashboardUpdateMsg).build();
                } else {
                    log.info("Skipping event as dashboard was not found [{}]", cloudEvent);
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
                DashboardUpdateMsg dashboardUpdateMsg = ((DashboardMsgConstructor)
                        dashboardMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDashboardDeleteMsg(dashboardId, entityGroupId);
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDashboardUpdateMsg(dashboardUpdateMsg).build();
                break;
        }
        return msg;
    }

    @Override
    protected Dashboard constructDashboardFromUpdateMsg(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg) {
        return JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
    }
}
