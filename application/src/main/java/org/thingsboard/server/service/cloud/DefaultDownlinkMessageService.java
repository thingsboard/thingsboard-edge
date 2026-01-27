/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
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
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
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
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@TbCoreComponent
public class DefaultDownlinkMessageService implements DownlinkMessageService {

    private final Lock sequenceDependencyLock = new ReentrantLock();

    @Autowired
    private CloudContextComponent cloudCtx;

    public ListenableFuture<List<Void>> processDownlinkMsg(TenantId tenantId,
                                                           CustomerId edgeCustomerId,
                                                           DownlinkMsg downlinkMsg,
                                                           EdgeSettings currentEdgeSettings) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            log.debug("[{}] Starting process downlink msg. edgeCustomerId [{}], downlinkMsgId [{}],",
                    tenantId, edgeCustomerId, downlinkMsg.getDownlinkMsgId());
            log.trace("downlink msg body [{}]", StringUtils.truncate(downlinkMsg.toString(), 10000));
            if (downlinkMsg.hasSyncCompletedMsg()) {
                result.add(updateSyncRequiredState(tenantId, edgeCustomerId, currentEdgeSettings));
            }
            if (downlinkMsg.hasEdgeConfiguration()) {
                result.add(cloudCtx.getEdgeProcessor().processEdgeConfigurationMsgFromCloud(tenantId, downlinkMsg.getEdgeConfiguration()));
            }
            if (downlinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : downlinkMsg.getEntityDataList()) {
                    result.addAll(cloudCtx.getTelemetryProcessor().processTelemetryMsg(tenantId, entityData));
                }
            }
            if (downlinkMsg.getDeviceRpcCallMsgCount() > 0) {
                for (DeviceRpcCallMsg deviceRpcRequestMsg : downlinkMsg.getDeviceRpcCallMsgList()) {
                    result.add(cloudCtx.getDeviceProcessor().processDeviceRpcCallFromCloud(tenantId, deviceRpcRequestMsg));
                }
            }
            if (downlinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(processDeviceCredentialsRequestMsg(tenantId, deviceCredentialsRequestMsg));
                }
            }
            if (downlinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
                for (DeviceProfileUpdateMsg deviceProfileUpdateMsg : downlinkMsg.getDeviceProfileUpdateMsgList()) {
                    result.add(cloudCtx.getDeviceProfileProcessor().processDeviceProfileMsgFromCloud(tenantId, deviceProfileUpdateMsg));
                }
            }
            if (downlinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : downlinkMsg.getDeviceUpdateMsgList()) {
                    result.add(cloudCtx.getDeviceProcessor().processDeviceMsgFromCloud(tenantId, deviceUpdateMsg));
                }
            }
            if (downlinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(cloudCtx.getDeviceProcessor().processDeviceCredentialsMsgFromCloud(tenantId, deviceCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getAssetProfileUpdateMsgCount() > 0) {
                for (AssetProfileUpdateMsg assetProfileUpdateMsg : downlinkMsg.getAssetProfileUpdateMsgList()) {
                    result.add(cloudCtx.getAssetProfileProcessor().processAssetProfileMsgFromCloud(tenantId, assetProfileUpdateMsg));
                }
            }
            if (downlinkMsg.getAssetUpdateMsgCount() > 0) {
                for (AssetUpdateMsg assetUpdateMsg : downlinkMsg.getAssetUpdateMsgList()) {
                    result.add(cloudCtx.getAssetProcessor().processAssetMsgFromCloud(tenantId, assetUpdateMsg));
                }
            }
            if (downlinkMsg.getEntityViewUpdateMsgCount() > 0) {
                for (EntityViewUpdateMsg entityViewUpdateMsg : downlinkMsg.getEntityViewUpdateMsgList()) {
                    result.add(cloudCtx.getEntityViewProcessor().processEntityViewMsgFromCloud(tenantId, entityViewUpdateMsg));
                }
            }
            if (downlinkMsg.getRuleChainUpdateMsgCount() > 0) {
                for (RuleChainUpdateMsg ruleChainUpdateMsg : downlinkMsg.getRuleChainUpdateMsgList()) {
                    result.add(cloudCtx.getRuleChainProcessor().processRuleChainMsgFromCloud(tenantId, ruleChainUpdateMsg));
                }
            }
            if (downlinkMsg.getRuleChainMetadataUpdateMsgCount() > 0) {
                for (RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg : downlinkMsg.getRuleChainMetadataUpdateMsgList()) {
                    result.add(cloudCtx.getRuleChainProcessor().processRuleChainMetadataMsgFromCloud(tenantId, ruleChainMetadataUpdateMsg));
                }
            }
            if (downlinkMsg.getDashboardUpdateMsgCount() > 0) {
                for (DashboardUpdateMsg dashboardUpdateMsg : downlinkMsg.getDashboardUpdateMsgList()) {
                    result.add(cloudCtx.getDashboardProcessor().processDashboardMsgFromCloud(tenantId, dashboardUpdateMsg, edgeCustomerId));
                }
            }
            if (downlinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : downlinkMsg.getAlarmUpdateMsgList()) {
                    result.add(cloudCtx.getAlarmProcessor().processAlarmMsgFromCloud(tenantId, alarmUpdateMsg));
                }
            }
            if (downlinkMsg.getAlarmCommentUpdateMsgCount() > 0) {
                for (AlarmCommentUpdateMsg alarmCommentUpdateMsg : downlinkMsg.getAlarmCommentUpdateMsgList()) {
                    result.add(cloudCtx.getAlarmCommentProcessor().processAlarmCommentMsgFromCloud(tenantId, alarmCommentUpdateMsg));
                }
            }
            if (downlinkMsg.getCustomerUpdateMsgCount() > 0) {
                for (CustomerUpdateMsg customerUpdateMsg : downlinkMsg.getCustomerUpdateMsgList()) {
                    sequenceDependencyLock.lock();
                    try {
                        result.add(cloudCtx.getCustomerProcessor().processCustomerMsgFromCloud(tenantId, customerUpdateMsg));
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : downlinkMsg.getRelationUpdateMsgList()) {
                    result.add(cloudCtx.getRelationProcessor().processRelationMsgFromCloud(tenantId, relationUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetsBundleUpdateMsgCount() > 0) {
                for (WidgetsBundleUpdateMsg widgetsBundleUpdateMsg : downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                    result.add(cloudCtx.getWidgetsBundleProcessor().processWidgetsBundleMsgFromCloud(tenantId, widgetsBundleUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetTypeUpdateMsgCount() > 0) {
                for (WidgetTypeUpdateMsg widgetTypeUpdateMsg : downlinkMsg.getWidgetTypeUpdateMsgList()) {
                    result.add(cloudCtx.getWidgetTypeProcessor().processWidgetTypeMsgFromCloud(tenantId, widgetTypeUpdateMsg));
                }
            }
            if (downlinkMsg.getUserUpdateMsgCount() > 0) {
                for (UserUpdateMsg userUpdateMsg : downlinkMsg.getUserUpdateMsgList()) {
                    sequenceDependencyLock.lock();
                    try {
                        result.add(cloudCtx.getUserProcessor().processUserMsgFromCloud(tenantId, userUpdateMsg));
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getUserCredentialsUpdateMsgCount() > 0) {
                for (UserCredentialsUpdateMsg userCredentialsUpdateMsg : downlinkMsg.getUserCredentialsUpdateMsgList()) {
                    result.add(cloudCtx.getUserProcessor().processUserCredentialsMsgFromCloud(tenantId, userCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getAdminSettingsUpdateMsgCount() > 0) {
                for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : downlinkMsg.getAdminSettingsUpdateMsgList()) {
                    result.add(cloudCtx.getAdminSettingsProcessor().processAdminSettingsMsgFromCloud(tenantId, adminSettingsUpdateMsg));
                }
            }
            if (downlinkMsg.getOtaPackageUpdateMsgCount() > 0) {
                for (OtaPackageUpdateMsg otaPackageUpdateMsg : downlinkMsg.getOtaPackageUpdateMsgList()) {
                    result.add(cloudCtx.getOtaPackageProcessor().processOtaPackageMsgFromCloud(tenantId, otaPackageUpdateMsg));
                }
            }
            if (downlinkMsg.getQueueUpdateMsgCount() > 0) {
                for (QueueUpdateMsg queueUpdateMsg : downlinkMsg.getQueueUpdateMsgList()) {
                    result.add(cloudCtx.getQueueProcessor().processQueueMsgFromCloud(tenantId, queueUpdateMsg));
                }
            }
            if (downlinkMsg.getTenantProfileUpdateMsgCount() > 0) {
                for (TenantProfileUpdateMsg tenantProfileUpdateMsg : downlinkMsg.getTenantProfileUpdateMsgList()) {
                    result.add(cloudCtx.getTenantProfileProcessor().processTenantProfileMsgFromCloud(tenantId, tenantProfileUpdateMsg));
                }
            }
            if (downlinkMsg.getNotificationTemplateUpdateMsgCount() > 0) {
                for (NotificationTemplateUpdateMsg notificationTemplateUpdateMsg : downlinkMsg.getNotificationTemplateUpdateMsgList()) {
                    result.add(cloudCtx.getNotificationProcessor().processNotificationTemplateMsgFromCloud(tenantId, notificationTemplateUpdateMsg));
                }
            }
            if (downlinkMsg.getNotificationTargetUpdateMsgCount() > 0) {
                for (NotificationTargetUpdateMsg notificationTargetUpdateMsg : downlinkMsg.getNotificationTargetUpdateMsgList()) {
                    result.add(cloudCtx.getNotificationProcessor().processNotificationTargetMsgFromCloud(tenantId, notificationTargetUpdateMsg));
                }
            }
            if (downlinkMsg.getNotificationRuleUpdateMsgCount() > 0) {
                for (NotificationRuleUpdateMsg notificationRuleUpdateMsg : downlinkMsg.getNotificationRuleUpdateMsgList()) {
                    result.add(cloudCtx.getNotificationProcessor().processNotificationRuleMsgFromCloud(tenantId, notificationRuleUpdateMsg));
                }
            }
            if (downlinkMsg.getOAuth2ClientUpdateMsgCount() > 0) {
                for (OAuth2ClientUpdateMsg oAuth2ClientUpdateMsg : downlinkMsg.getOAuth2ClientUpdateMsgList()) {
                    result.add(cloudCtx.getOAuth2Processor().processOAuth2ClientMsgFromCloud(oAuth2ClientUpdateMsg));
                }
            }
            if (downlinkMsg.getOAuth2DomainUpdateMsgCount() > 0) {
                for (OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg : downlinkMsg.getOAuth2DomainUpdateMsgList()) {
                    result.add(cloudCtx.getOAuth2Processor().processDomainMsgFromCloud(oAuth2DomainUpdateMsg));
                }
            }
            if (downlinkMsg.getTenantUpdateMsgCount() > 0) {
                for (TenantUpdateMsg tenantUpdateMsg : downlinkMsg.getTenantUpdateMsgList()) {
                    result.add(cloudCtx.getTenantProcessor().processTenantMsgFromCloud(tenantUpdateMsg));
                }
            }
            if (downlinkMsg.getResourceUpdateMsgCount() > 0) {
                for (ResourceUpdateMsg resourceUpdateMsg : downlinkMsg.getResourceUpdateMsgList()) {
                    result.add(cloudCtx.getResourceProcessor().processResourceMsgFromCloud(tenantId, resourceUpdateMsg));
                }
            }
            if (downlinkMsg.getCalculatedFieldUpdateMsgCount() > 0) {
                for (CalculatedFieldUpdateMsg calculatedFieldUpdateMsg : downlinkMsg.getCalculatedFieldUpdateMsgList()) {
                    result.add(cloudCtx.getCalculatedFieldProcessor().processCalculatedFieldMsgFromCloud(tenantId, calculatedFieldUpdateMsg));
                }
            }

            log.trace("Finished processing DownlinkMsg {}", downlinkMsg.getDownlinkMsgId());
        } catch (Exception e) {
            log.error("Can't process downlink message [{}]", downlinkMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException("Can't process downlink message", e));
        }
        return Futures.allAsList(result);
    }

    private ListenableFuture<Void> updateSyncRequiredState(TenantId tenantId, CustomerId customerId, EdgeSettings currentEdgeSettings) {
        log.debug("Marking full sync required to false");
        if (currentEdgeSettings != null) {
            currentEdgeSettings.setFullSyncRequired(false);
            try {
                cloudCtx.getCloudEventService().saveCloudEvent(tenantId, CloudEventType.TENANT, EdgeEventActionType.ATTRIBUTES_REQUEST, tenantId, null);
                if (customerId != null && !EntityId.NULL_UUID.equals(customerId.getId())) {
                    cloudCtx.getCloudEventService().saveCloudEvent(tenantId, CloudEventType.CUSTOMER, EdgeEventActionType.ATTRIBUTES_REQUEST, customerId, null);
                }
            } catch (Exception e) {
                log.error("Failed to request attributes for tenant and customer entities", e);
            }
            return Futures.transform(cloudCtx.getEdgeSettingsService().saveEdgeSettings(tenantId, currentEdgeSettings),
                    result -> {
                        log.debug("Full sync required marked as false");
                        return null;
                    },
                    cloudCtx.getDbCallbackExecutorService());
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            return cloudCtx.getCloudEventService().saveCloudEventAsync(tenantId, CloudEventType.DEVICE, EdgeEventActionType.CREDENTIALS_UPDATED, deviceId, null);
        } else {
            return Futures.immediateFuture(null);
        }
    }

}
