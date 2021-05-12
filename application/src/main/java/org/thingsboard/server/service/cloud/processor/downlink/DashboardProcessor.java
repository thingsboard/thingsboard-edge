/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.processor.downlink;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.CloudType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class DashboardProcessor extends BaseProcessor {

    private final Lock dashboardCreationLock = new ReentrantLock();

    @Autowired
    private DashboardService dashboardService;

    public ListenableFuture<Void> onDashboardUpdate(TenantId tenantId, CustomerId customerId, DashboardUpdateMsg dashboardUpdateMsg, CloudType cloudType) {
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        switch (dashboardUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    dashboardCreationLock.lock();

                    boolean created = false;
                    Dashboard dashboard = dashboardService.findDashboardById(tenantId, dashboardId);
                    if (dashboard == null) {
                        dashboard = new Dashboard();
                        dashboard.setId(dashboardId);
                        dashboard.setCreatedTime(Uuids.unixTimestamp(dashboardId.getId()));
                        dashboard.setTenantId(tenantId);
                        created = true;
                    }
                    dashboard.setTitle(dashboardUpdateMsg.getTitle());
                    dashboard.setConfiguration(JacksonUtil.toJsonNode(dashboardUpdateMsg.getConfiguration()));

                    CustomerId dashboardCustomerId = safeSetCustomerId(dashboardUpdateMsg, cloudType, dashboard);
                    Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
                    if (created) {
                        entityGroupService.addEntityToEntityGroupAll(savedDashboard.getTenantId(), savedDashboard.getOwnerId(), savedDashboard.getId());
                    }
                    addToEntityGroup(tenantId, customerId, dashboardUpdateMsg, cloudType, dashboardId, dashboardCustomerId);
                } finally {
                    dashboardCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                UUID entityGroupUUID = safeGetUUID(dashboardUpdateMsg.getEntityGroupIdMSB(), dashboardUpdateMsg.getEntityGroupIdLSB());
                if (entityGroupUUID != null) {
                    EntityGroupId entityGroupId =
                            new EntityGroupId(entityGroupUUID);
                    entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, dashboardId);
                } else {
                    Dashboard dashboardById = dashboardService.findDashboardById(tenantId, dashboardId);
                    if (dashboardById != null) {
                        dashboardService.deleteDashboard(tenantId, dashboardId);
                    }
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type" + dashboardUpdateMsg.getMsgType()));
        }

        return Futures.transform(requestForAdditionalData(tenantId, dashboardUpdateMsg.getMsgType(), dashboardId), future -> null, dbCallbackExecutor);
    }

    private void addToEntityGroup(TenantId tenantId, CustomerId customerId, DashboardUpdateMsg dashboardUpdateMsg, CloudType cloudType, DashboardId dashboardId, CustomerId dashboardCustomerId) {
        if (CloudType.CE.equals(cloudType)) {
            if (dashboardCustomerId != null && dashboardCustomerId.equals(customerId)) {
                EntityGroup customerDashboardsEntityGroup =
                        entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(tenantId, customerId, EntityType.DASHBOARD);
                entityGroupService.addEntityToEntityGroup(tenantId, customerDashboardsEntityGroup.getId(), dashboardId);
            }
            if ((dashboardCustomerId == null || dashboardCustomerId.isNullUid()) &&
                    (customerId != null && !customerId.isNullUid())) {
                EntityGroup customerDashboardsEntityGroup =
                        entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(tenantId, customerId, EntityType.DASHBOARD);
                entityGroupService.removeEntityFromEntityGroup(tenantId, customerDashboardsEntityGroup.getId(), dashboardId);
            }
        } else {
            UUID entityGroupUUID = safeGetUUID(dashboardUpdateMsg.getEntityGroupIdMSB(), dashboardUpdateMsg.getEntityGroupIdLSB());
            if (entityGroupUUID != null) {
                EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                addEntityToGroup(tenantId, entityGroupId, dashboardId);
            }
        }
    }

    private CustomerId safeSetCustomerId(DashboardUpdateMsg dashboardUpdateMsg, CloudType cloudType, Dashboard dashboard) {
        CustomerId dashboardCustomerId = safeGetCustomerId(dashboardUpdateMsg.getCustomerIdMSB(), dashboardUpdateMsg.getCustomerIdLSB());
        if (CloudType.PE.equals(cloudType)) {
            dashboard.setCustomerId(dashboardCustomerId);
        }
        return dashboardCustomerId;
    }
}
