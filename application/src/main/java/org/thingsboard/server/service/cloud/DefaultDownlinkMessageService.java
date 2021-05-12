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
package org.thingsboard.server.service.cloud;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.gen.edge.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.GroupPermissionProto;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RoleProto;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.WidgetsBundleUpdateMsg;
import org.thingsboard.server.service.cloud.processor.downlink.AdminSettingsProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.AlarmProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.AssetProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.CustomerProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.DashboardProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.DeviceProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.DeviceProfileProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.EntityGroupProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.EntityViewProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.GroupPermissionProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.RelationProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.RoleProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.RuleChainProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.SchedulerEventProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.TelemetryProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.UserProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.WhiteLabelingProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.WidgetTypeProcessor;
import org.thingsboard.server.service.cloud.processor.downlink.WidgetsBundleProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class DefaultDownlinkMessageService extends BaseCloudEventService implements DownlinkMessageService {

    private final Lock sequenceDependencyLock = new ReentrantLock();

    @Autowired
    private CloudEventService cloudEventService;

    @Autowired
    private RuleChainProcessor ruleChainProcessor;

    @Autowired
    private TelemetryProcessor telemetryProcessor;

    @Autowired
    private DeviceProcessor deviceProcessor;

    @Autowired
    private DeviceProfileProcessor deviceProfileProcessor;

    @Autowired
    private AssetProcessor assetProcessor;

    @Autowired
    private EntityViewProcessor entityViewProcessor;

    @Autowired
    private RelationProcessor relationProcessor;

    @Autowired
    private DashboardProcessor dashboardProcessor;

    @Autowired
    private CustomerProcessor customerProcessor;

    @Autowired
    private AlarmProcessor alarmProcessor;

    @Autowired
    private UserProcessor userProcessor;

    @Autowired
    private EntityGroupProcessor entityGroupProcessor;

    @Autowired
    private SchedulerEventProcessor schedulerEventProcessor;

    @Autowired
    private RoleProcessor roleProcessor;

    @Autowired
    private GroupPermissionProcessor groupPermissionProcessor;

    @Autowired
    private WhiteLabelingProcessor whiteLabelingProcessor;

    @Autowired
    private WidgetsBundleProcessor widgetsBundleProcessor;

    @Autowired
    private WidgetTypeProcessor widgetTypeProcessor;

    @Autowired
    private AdminSettingsProcessor adminSettingsProcessor;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    private CustomerId customerId;

    public ListenableFuture<List<Void>> processDownlinkMsg(TenantId tenantId, DownlinkMsg downlinkMsg, EdgeSettings currentEdgeSettings) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            log.debug("onDownlink {}", downlinkMsg);
            if (downlinkMsg.hasSyncCompletedMsg()) {
                result.add(updateSyncRequiredState(tenantId, currentEdgeSettings));
            }
            if (downlinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : downlinkMsg.getEntityDataList()) {
                    result.addAll(telemetryProcessor.onTelemetryUpdate(tenantId, entityData));
                }
            }
            if (downlinkMsg.getDeviceRpcCallMsgCount() > 0) {
                for (DeviceRpcCallMsg deviceRpcRequestMsg : downlinkMsg.getDeviceRpcCallMsgList()) {
                    result.add(deviceProcessor.onDeviceRpcRequest(tenantId, deviceRpcRequestMsg));
                }
            }
            if (downlinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(processDeviceCredentialsRequestMsg(tenantId, deviceCredentialsRequestMsg));
                }
            }
            if (downlinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : downlinkMsg.getDeviceUpdateMsgList()) {
                    result.add(deviceProcessor.onDeviceUpdate(tenantId, customerId, deviceUpdateMsg, currentEdgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
                for (DeviceProfileUpdateMsg deviceProfileUpdateMsg : downlinkMsg.getDeviceProfileUpdateMsgList()) {
                    result.add(deviceProfileProcessor.onDeviceProfileUpdate(tenantId, deviceProfileUpdateMsg));
                }
            }
            if (downlinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(deviceProcessor.onDeviceCredentialsUpdate(tenantId, deviceCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getAssetUpdateMsgCount() > 0) {
                for (AssetUpdateMsg assetUpdateMsg : downlinkMsg.getAssetUpdateMsgList()) {
                    result.add(assetProcessor.onAssetUpdate(tenantId, customerId, assetUpdateMsg, currentEdgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getEntityViewUpdateMsgCount() > 0) {
                for (EntityViewUpdateMsg entityViewUpdateMsg : downlinkMsg.getEntityViewUpdateMsgList()) {
                    result.add(entityViewProcessor.onEntityViewUpdate(tenantId, customerId, entityViewUpdateMsg, currentEdgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getRuleChainUpdateMsgCount() > 0) {
                for (RuleChainUpdateMsg ruleChainUpdateMsg : downlinkMsg.getRuleChainUpdateMsgList()) {
                    result.add(ruleChainProcessor.onRuleChainUpdate(tenantId, ruleChainUpdateMsg));
                }
            }
            if (downlinkMsg.getRuleChainMetadataUpdateMsgCount() > 0) {
                for (RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg : downlinkMsg.getRuleChainMetadataUpdateMsgList()) {
                    result.add(ruleChainProcessor.onRuleChainMetadataUpdate(tenantId, ruleChainMetadataUpdateMsg));
                }
            }
            if (downlinkMsg.getDashboardUpdateMsgCount() > 0) {
                for (DashboardUpdateMsg dashboardUpdateMsg : downlinkMsg.getDashboardUpdateMsgList()) {
                    result.add(dashboardProcessor.onDashboardUpdate(tenantId, customerId, dashboardUpdateMsg, currentEdgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : downlinkMsg.getAlarmUpdateMsgList()) {
                    result.add(alarmProcessor.onAlarmUpdate(tenantId, alarmUpdateMsg));
                }
            }
            if (downlinkMsg.getCustomerUpdateMsgCount() > 0) {
                for (CustomerUpdateMsg customerUpdateMsg : downlinkMsg.getCustomerUpdateMsgList()) {
                    try {
                        sequenceDependencyLock.lock();
                        result.add(customerProcessor.onCustomerUpdate(tenantId, customerUpdateMsg, currentEdgeSettings.getCloudType()));
                        updateCustomerId(customerUpdateMsg);
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : downlinkMsg.getRelationUpdateMsgList()) {
                    result.add(relationProcessor.onRelationUpdate(tenantId, relationUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetsBundleUpdateMsgCount() > 0) {
                for (WidgetsBundleUpdateMsg widgetsBundleUpdateMsg : downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                    result.add(widgetsBundleProcessor.onWidgetsBundleUpdate(tenantId, widgetsBundleUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetTypeUpdateMsgCount() > 0) {
                for (WidgetTypeUpdateMsg widgetTypeUpdateMsg : downlinkMsg.getWidgetTypeUpdateMsgList()) {
                    result.add(widgetTypeProcessor.onWidgetTypeUpdate(tenantId, widgetTypeUpdateMsg));
                }
            }
            if (downlinkMsg.getUserUpdateMsgCount() > 0) {
                for (UserUpdateMsg userUpdateMsg : downlinkMsg.getUserUpdateMsgList()) {
                    try {
                        sequenceDependencyLock.lock();
                        result.add(userProcessor.onUserUpdate(tenantId, userUpdateMsg, currentEdgeSettings.getCloudType()));
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getUserCredentialsUpdateMsgCount() > 0) {
                for (UserCredentialsUpdateMsg userCredentialsUpdateMsg : downlinkMsg.getUserCredentialsUpdateMsgList()) {
                    result.add(userProcessor.onUserCredentialsUpdate(tenantId, userCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getEntityGroupUpdateMsgCount() > 0) {
                for (EntityGroupUpdateMsg entityGroupUpdateMsg : downlinkMsg.getEntityGroupUpdateMsgList()) {
                    result.add(entityGroupProcessor.onEntityGroupUpdate(tenantId, entityGroupUpdateMsg));
                }
            }
            if (downlinkMsg.getCustomTranslationMsgCount() > 0) {
                for (CustomTranslationProto customTranslationProto : downlinkMsg.getCustomTranslationMsgList()) {
                    result.add(whiteLabelingProcessor.onCustomTranslationUpdate(tenantId, customTranslationProto));
                }
            }
            if (downlinkMsg.getWhiteLabelingParamsCount() > 0) {
                for (WhiteLabelingParamsProto whiteLabelingParamsProto : downlinkMsg.getWhiteLabelingParamsList()) {
                    result.add(whiteLabelingProcessor.onWhiteLabelingParamsUpdate(tenantId, whiteLabelingParamsProto));
                }
            }
            if (downlinkMsg.getLoginWhiteLabelingParamsCount() > 0) {
                for (LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto : downlinkMsg.getLoginWhiteLabelingParamsList()) {
                    result.add(whiteLabelingProcessor.onLoginWhiteLabelingParamsUpdate(tenantId, loginWhiteLabelingParamsProto));
                }
            }
            if (downlinkMsg.getSchedulerEventUpdateMsgCount() > 0) {
                for (SchedulerEventUpdateMsg schedulerEventUpdateMsg : downlinkMsg.getSchedulerEventUpdateMsgList()) {
                    result.add(schedulerEventProcessor.onScheduleEventUpdate(tenantId, schedulerEventUpdateMsg));
                }
            }
            if (downlinkMsg.getAdminSettingsUpdateMsgCount() > 0) {
                for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : downlinkMsg.getAdminSettingsUpdateMsgList()) {
                    result.add(adminSettingsProcessor.onAdminSettingsUpdate(tenantId, adminSettingsUpdateMsg));
                }
            }
            if (downlinkMsg.getRoleMsgCount() > 0) {
                for (RoleProto roleProto : downlinkMsg.getRoleMsgList()) {
                    result.add(roleProcessor.onRoleUpdate(tenantId, roleProto));
                }
            }
            if (downlinkMsg.getGroupPermissionMsgCount() > 0) {
                for (GroupPermissionProto groupPermissionProto : downlinkMsg.getGroupPermissionMsgList()) {
                    result.add(groupPermissionProcessor.onGroupPermissionUpdate(tenantId, groupPermissionProto));
                }
            }
        } catch (Exception e) {
            log.error("Can't process downlink message [{}]", downlinkMsg, e);
        }
        return Futures.allAsList(result);
    }

    private ListenableFuture<Void> updateSyncRequiredState(TenantId tenantId, EdgeSettings currentEdgeSettings) {
        log.debug("Marking full sync required to false");
        if (currentEdgeSettings != null) {
            currentEdgeSettings.setFullSyncRequired(false);
            return Futures.transform(cloudEventService.saveEdgeSettings(tenantId, currentEdgeSettings),
                    result -> {
                        log.debug("Full sync required marked as false");
                        return null;
                    },
                    dbCallbackExecutorService);
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private void updateCustomerId(CustomerUpdateMsg customerUpdateMsg) {
        switch (customerUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                customerId = null;
                break;
        }
    }

    private ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            ListenableFuture<CloudEvent> future = saveCloudEvent(tenantId, CloudEventType.DEVICE, ActionType.CREDENTIALS_UPDATED, deviceId, null);
            return Futures.transform(future, cloudEvent -> null, dbCallbackExecutorService);
        }
        return Futures.immediateFuture(null);
    }

}
