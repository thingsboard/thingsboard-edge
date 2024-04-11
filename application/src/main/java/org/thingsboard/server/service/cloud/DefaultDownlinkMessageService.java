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
package org.thingsboard.server.service.cloud;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2UpdateMsg;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.service.cloud.rpc.processor.AdminSettingsCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.CustomerCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DashboardCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EdgeCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityViewCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.NotificationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.OAuth2CloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.OtaPackageCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.QueueCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RelationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.ResourceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RuleChainCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TelemetryCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.UserCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WidgetBundleCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WidgetTypeCloudProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class DefaultDownlinkMessageService implements DownlinkMessageService {

    private final Lock sequenceDependencyLock = new ReentrantLock();

    @Autowired
    private CloudEventService cloudEventService;

    @Autowired
    private EdgeCloudProcessor edgeCloudProcessor;

    @Autowired
    private RuleChainCloudProcessor ruleChainProcessor;

    @Autowired
    private TelemetryCloudProcessor telemetryProcessor;

    @Autowired
    private DeviceCloudProcessor deviceProcessor;

    @Autowired
    private DeviceProfileCloudProcessor deviceProfileProcessor;

    @Autowired
    private AssetProfileCloudProcessor assetProfileProcessor;

    @Autowired
    private AssetCloudProcessor assetProcessor;

    @Autowired
    private EntityViewCloudProcessor entityViewProcessor;

    @Autowired
    private RelationCloudProcessor relationProcessor;

    @Autowired
    private DashboardCloudProcessor dashboardProcessor;

    @Autowired
    private CustomerCloudProcessor customerProcessor;

    @Autowired
    private AlarmCloudProcessor alarmProcessor;

    @Autowired
    private UserCloudProcessor userProcessor;

    @Autowired
    private WidgetBundleCloudProcessor widgetsBundleProcessor;

    @Autowired
    private WidgetTypeCloudProcessor widgetTypeProcessor;

    @Autowired
    private AdminSettingsCloudProcessor adminSettingsProcessor;

    @Autowired
    private OtaPackageCloudProcessor otaPackageProcessor;

    @Autowired
    private QueueCloudProcessor queueCloudProcessor;

    @Autowired
    private TenantCloudProcessor tenantCloudProcessor;

    @Autowired
    private TenantProfileCloudProcessor tenantProfileCloudProcessor;

    @Autowired
    private ResourceCloudProcessor tbResourceCloudProcessor;

    @Autowired
    private NotificationCloudProcessor notificationCloudProcessor;

    @Autowired
    private OAuth2CloudProcessor oAuth2CloudProcessor;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    public ListenableFuture<List<Void>> processDownlinkMsg(TenantId tenantId,
                                                           CustomerId edgeCustomerId,
                                                           DownlinkMsg downlinkMsg,
                                                           EdgeSettings currentEdgeSettings,
                                                           Long queueStartTs) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            log.debug("[{}] Starting process DownlinkMsg. edgeCustomerId [{}], downlinkMsgId [{}],",
                    tenantId, edgeCustomerId, downlinkMsg.getDownlinkMsgId());
            log.trace("DownlinkMsg Body {}", downlinkMsg);
            if (downlinkMsg.hasSyncCompletedMsg()) {
                result.add(updateSyncRequiredState(tenantId, edgeCustomerId, currentEdgeSettings, queueStartTs));
            }
            if (downlinkMsg.hasEdgeConfiguration()) {
                result.add(edgeCloudProcessor.processEdgeConfigurationMsgFromCloud(tenantId, downlinkMsg.getEdgeConfiguration()));
            }
            if (downlinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : downlinkMsg.getEntityDataList()) {
                    result.addAll(telemetryProcessor.processTelemetryMsg(tenantId, entityData));
                }
            }
            if (downlinkMsg.getDeviceRpcCallMsgCount() > 0) {
                for (DeviceRpcCallMsg deviceRpcRequestMsg : downlinkMsg.getDeviceRpcCallMsgList()) {
                    result.add(deviceProcessor.processDeviceRpcCallFromCloud(tenantId, deviceRpcRequestMsg));
                }
            }
            if (downlinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(processDeviceCredentialsRequestMsg(tenantId, deviceCredentialsRequestMsg));
                }
            }
            if (downlinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
                for (DeviceProfileUpdateMsg deviceProfileUpdateMsg : downlinkMsg.getDeviceProfileUpdateMsgList()) {
                    result.add(deviceProfileProcessor.processDeviceProfileMsgFromCloud(tenantId, deviceProfileUpdateMsg));
                }
            }
            if (downlinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : downlinkMsg.getDeviceUpdateMsgList()) {
                    result.add(deviceProcessor.processDeviceMsgFromCloud(tenantId, deviceUpdateMsg, queueStartTs));
                }
            }
            if (downlinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(deviceProcessor.processDeviceCredentialsMsgFromCloud(tenantId, deviceCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getAssetProfileUpdateMsgCount() > 0) {
                for (AssetProfileUpdateMsg assetProfileUpdateMsg  : downlinkMsg.getAssetProfileUpdateMsgList()) {
                    result.add(assetProfileProcessor.processAssetProfileMsgFromCloud(tenantId, assetProfileUpdateMsg));
                }
            }
            if (downlinkMsg.getAssetUpdateMsgCount() > 0) {
                for (AssetUpdateMsg assetUpdateMsg : downlinkMsg.getAssetUpdateMsgList()) {
                    result.add(assetProcessor.processAssetMsgFromCloud(tenantId, assetUpdateMsg, queueStartTs));
                }
            }
            if (downlinkMsg.getEntityViewUpdateMsgCount() > 0) {
                for (EntityViewUpdateMsg entityViewUpdateMsg : downlinkMsg.getEntityViewUpdateMsgList()) {
                    result.add(entityViewProcessor.processEntityViewMsgFromCloud(tenantId, entityViewUpdateMsg, queueStartTs));
                }
            }
            if (downlinkMsg.getRuleChainUpdateMsgCount() > 0) {
                for (RuleChainUpdateMsg ruleChainUpdateMsg : downlinkMsg.getRuleChainUpdateMsgList()) {
                    result.add(ruleChainProcessor.processRuleChainMsgFromCloud(tenantId, ruleChainUpdateMsg, queueStartTs));
                }
            }
            if (downlinkMsg.getRuleChainMetadataUpdateMsgCount() > 0) {
                for (RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg : downlinkMsg.getRuleChainMetadataUpdateMsgList()) {
                    result.add(ruleChainProcessor.processRuleChainMetadataMsgFromCloud(tenantId, ruleChainMetadataUpdateMsg));
                }
            }
            if (downlinkMsg.getDashboardUpdateMsgCount() > 0) {
                for (DashboardUpdateMsg dashboardUpdateMsg : downlinkMsg.getDashboardUpdateMsgList()) {
                    result.add(dashboardProcessor.processDashboardMsgFromCloud(tenantId, dashboardUpdateMsg, edgeCustomerId, queueStartTs));
                }
            }
            if (downlinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : downlinkMsg.getAlarmUpdateMsgList()) {
                    result.add(alarmProcessor.processAlarmMsgFromCloud(tenantId, alarmUpdateMsg));
                }
            }
            if (downlinkMsg.getAlarmCommentUpdateMsgCount() > 0) {
                for (AlarmCommentUpdateMsg alarmCommentUpdateMsg : downlinkMsg.getAlarmCommentUpdateMsgList()) {
                    result.add(alarmProcessor.processAlarmCommentMsgFromCloud(tenantId, alarmCommentUpdateMsg));
                }
            }
            if (downlinkMsg.getCustomerUpdateMsgCount() > 0) {
                for (CustomerUpdateMsg customerUpdateMsg : downlinkMsg.getCustomerUpdateMsgList()) {
                    sequenceDependencyLock.lock();
                    try {
                        result.add(customerProcessor.processCustomerMsgFromCloud(tenantId, customerUpdateMsg, queueStartTs));
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : downlinkMsg.getRelationUpdateMsgList()) {
                    result.add(relationProcessor.processRelationMsgFromCloud(tenantId, relationUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetsBundleUpdateMsgCount() > 0) {
                for (WidgetsBundleUpdateMsg widgetsBundleUpdateMsg : downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                    result.add(widgetsBundleProcessor.processWidgetsBundleMsgFromCloud(tenantId, widgetsBundleUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetTypeUpdateMsgCount() > 0) {
                for (WidgetTypeUpdateMsg widgetTypeUpdateMsg : downlinkMsg.getWidgetTypeUpdateMsgList()) {
                    result.add(widgetTypeProcessor.processWidgetTypeMsgFromCloud(tenantId, widgetTypeUpdateMsg));
                }
            }
            if (downlinkMsg.getUserUpdateMsgCount() > 0) {
                for (UserUpdateMsg userUpdateMsg : downlinkMsg.getUserUpdateMsgList()) {
                    sequenceDependencyLock.lock();
                    try {
                        result.add(userProcessor.processUserMsgFromCloud(tenantId, userUpdateMsg, queueStartTs));
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getUserCredentialsUpdateMsgCount() > 0) {
                for (UserCredentialsUpdateMsg userCredentialsUpdateMsg : downlinkMsg.getUserCredentialsUpdateMsgList()) {
                    result.add(userProcessor.processUserCredentialsMsgFromCloud(tenantId, userCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getAdminSettingsUpdateMsgCount() > 0) {
                for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : downlinkMsg.getAdminSettingsUpdateMsgList()) {
                    result.add(adminSettingsProcessor.processAdminSettingsMsgFromCloud(tenantId, adminSettingsUpdateMsg));
                }
            }
            if (downlinkMsg.getOtaPackageUpdateMsgCount() > 0) {
                for (OtaPackageUpdateMsg otaPackageUpdateMsg : downlinkMsg.getOtaPackageUpdateMsgList()) {
                    result.add(otaPackageProcessor.processOtaPackageMsgFromCloud(tenantId, otaPackageUpdateMsg));
                }
            }
            if (downlinkMsg.getQueueUpdateMsgCount() > 0) {
                for (QueueUpdateMsg queueUpdateMsg : downlinkMsg.getQueueUpdateMsgList()) {
                    result.add(queueCloudProcessor.processQueueMsgFromCloud(tenantId, queueUpdateMsg));
                }
            }
            if (downlinkMsg.getTenantProfileUpdateMsgCount() > 0) {
                for (TenantProfileUpdateMsg tenantProfileUpdateMsg : downlinkMsg.getTenantProfileUpdateMsgList()) {
                    result.add(tenantProfileCloudProcessor.processTenantProfileMsgFromCloud(tenantId, tenantProfileUpdateMsg));
                }
            }
            if (downlinkMsg.getNotificationTemplateUpdateMsgCount() > 0) {
                for (NotificationTemplateUpdateMsg notificationTemplateUpdateMsg : downlinkMsg.getNotificationTemplateUpdateMsgList()) {
                    result.add(notificationCloudProcessor.processNotificationTemplateMsgFromCloud(tenantId, notificationTemplateUpdateMsg));
                }
            }
            if (downlinkMsg.getNotificationTargetUpdateMsgCount() > 0) {
                for (NotificationTargetUpdateMsg notificationTargetUpdateMsg : downlinkMsg.getNotificationTargetUpdateMsgList()) {
                    result.add(notificationCloudProcessor.processNotificationTargetMsgFromCloud(tenantId, notificationTargetUpdateMsg));
                }
            }
            if (downlinkMsg.getNotificationRuleUpdateMsgCount() > 0) {
                for (NotificationRuleUpdateMsg notificationRuleUpdateMsg : downlinkMsg.getNotificationRuleUpdateMsgList()) {
                    result.add(notificationCloudProcessor.processNotificationRuleMsgFromCloud(tenantId, notificationRuleUpdateMsg));
                }
            }
            if (downlinkMsg.getOAuth2UpdateMsgCount() > 0) {
                for (OAuth2UpdateMsg oAuth2UpdateMsg : downlinkMsg.getOAuth2UpdateMsgList()) {
                    result.add(oAuth2CloudProcessor.processOAuth2MsgFromCloud(oAuth2UpdateMsg));
                }
            }
            if (downlinkMsg.getTenantUpdateMsgCount() > 0) {
                for (TenantUpdateMsg tenantUpdateMsg : downlinkMsg.getTenantUpdateMsgList()) {
                    result.add(tenantCloudProcessor.processTenantMsgFromCloud(tenantUpdateMsg));
                }
            }
            if (downlinkMsg.getResourceUpdateMsgCount() > 0) {
                for (ResourceUpdateMsg resourceUpdateMsg : downlinkMsg.getResourceUpdateMsgList()) {
                    result.add(tbResourceCloudProcessor.processResourceMsgFromCloud(tenantId, resourceUpdateMsg));
                }
            }
            log.trace("Finished processing DownlinkMsg {}", downlinkMsg.getDownlinkMsgId());
        } catch (Exception e) {
            log.error("Can't process downlink message [{}]", downlinkMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException("Can't process downlink message", e));
        }
        return Futures.allAsList(result);
    }

    private ListenableFuture<Void> updateSyncRequiredState(TenantId tenantId, CustomerId customerId, EdgeSettings currentEdgeSettings, Long queueStartTs) {
        log.debug("Marking full sync required to false");
        if (currentEdgeSettings != null) {
            currentEdgeSettings.setFullSyncRequired(false);
            try {
                cloudEventService.saveCloudEvent(tenantId, CloudEventType.TENANT, EdgeEventActionType.ATTRIBUTES_REQUEST, tenantId, null, queueStartTs);
                if (customerId != null && !EntityId.NULL_UUID.equals(customerId.getId())) {
                    cloudEventService.saveCloudEvent(tenantId, CloudEventType.CUSTOMER, EdgeEventActionType.ATTRIBUTES_REQUEST, customerId, null, queueStartTs);
                }
            } catch (Exception e) {
                log.error("Failed to request attributes for tenant and customer entities", e);
            }
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

    private ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            return cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.DEVICE, EdgeEventActionType.CREDENTIALS_UPDATED, deviceId, null, 0L);
        } else {
            return Futures.immediateFuture(null);
        }
    }

}
