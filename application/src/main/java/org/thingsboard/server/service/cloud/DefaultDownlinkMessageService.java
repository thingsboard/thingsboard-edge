// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.thingsboard.server.gen.edge.v1.AiModelUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ApiKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ConverterUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceGroupOtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EncryptionKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.GroupPermissionProto;
import org.thingsboard.server.gen.edge.v1.IntegrationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ReportTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SecretUpdateMsg;
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
            log.debug("[{}] Starting process DownlinkMsg. downlinkMsgId [{}],",
                    tenantId, downlinkMsg.getDownlinkMsgId());
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
                    result.add(cloudCtx.getDashboardProcessor().processDashboardMsgFromCloud(tenantId, dashboardUpdateMsg));
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
            if (downlinkMsg.getSchedulerEventUpdateMsgCount() > 0) {
                for (SchedulerEventUpdateMsg schedulerEventUpdateMsg : downlinkMsg.getSchedulerEventUpdateMsgList()) {
                    result.add(cloudCtx.getSchedulerEventProcessor().processScheduleEventFromCloud(tenantId, schedulerEventUpdateMsg));
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
            if (downlinkMsg.getRoleMsgCount() > 0) {
                for (RoleProto roleProto : downlinkMsg.getRoleMsgList()) {
                    result.add(cloudCtx.getRoleProcessor().processRoleMsgFromCloud(tenantId, roleProto));
                }
            }
            if (downlinkMsg.getGroupPermissionMsgCount() > 0) {
                for (GroupPermissionProto groupPermissionProto : downlinkMsg.getGroupPermissionMsgList()) {
                    result.add(cloudCtx.getGroupPermissionProcessor().processGroupPermissionMsgFromCloud(tenantId, groupPermissionProto));
                }
            }
            if (downlinkMsg.getConverterMsgCount() > 0) {
                for (ConverterUpdateMsg converterUpdateMsg : downlinkMsg.getConverterMsgList()) {
                    result.add(cloudCtx.getConverterProcessor().processConverterMsgFromCloud(tenantId, converterUpdateMsg));
                }
            }
            if (downlinkMsg.getIntegrationMsgCount() > 0) {
                for (IntegrationUpdateMsg integrationUpdateMsg : downlinkMsg.getIntegrationMsgList()) {
                    result.add(cloudCtx.getIntegrationProcessor().processIntegrationMsgFromCloud(tenantId, integrationUpdateMsg));
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
                    sequenceDependencyLock.lock();
                    try {
                        result.add(cloudCtx.getOAuth2Processor().processDomainMsgFromCloud(oAuth2DomainUpdateMsg));
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getEntityGroupUpdateMsgCount() > 0) {
                for (EntityGroupUpdateMsg entityGroupUpdateMsg : downlinkMsg.getEntityGroupUpdateMsgList()) {
                    result.add(cloudCtx.getEntityGroupProcessor().processEntityGroupMsgFromCloud(tenantId, entityGroupUpdateMsg));
                }
            }
            if (downlinkMsg.hasCustomTranslationUpdateMsg()) {
                result.add(cloudCtx.getCustomTranslationProcessor().processCustomTranslationMsgFromCloud(tenantId, downlinkMsg.getCustomTranslationUpdateMsg()));
            }
            if (downlinkMsg.hasCustomMenuProto()) {
                result.add(cloudCtx.getCustomMenuProcessor().processCustomMenuMsgFromCloud(tenantId, downlinkMsg.getCustomMenuProto()));
            }
            if (downlinkMsg.hasWhiteLabelingProto()) {
                sequenceDependencyLock.lock();
                try {
                    result.add(cloudCtx.getWhiteLabelingProcessor().processWhiteLabelingMsgFromCloud(tenantId, downlinkMsg.getWhiteLabelingProto()));
                } finally {
                    sequenceDependencyLock.unlock();
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
            if (downlinkMsg.getDeviceGroupOtaPackageUpdateMsgCount() > 0) {
                for (DeviceGroupOtaPackageUpdateMsg deviceGroupOtaPackageUpdateMsg : downlinkMsg.getDeviceGroupOtaPackageUpdateMsgList()) {
                    result.add(cloudCtx.getDeviceProcessor().processDeviceGroupOtaPackageFromCloud(tenantId, deviceGroupOtaPackageUpdateMsg));
                }
            }
            if (downlinkMsg.getCalculatedFieldUpdateMsgCount() > 0) {
                for (CalculatedFieldUpdateMsg calculatedFieldUpdateMsg : downlinkMsg.getCalculatedFieldUpdateMsgList()) {
                    result.add(cloudCtx.getCalculatedFieldProcessor().processCalculatedFieldMsgFromCloud(tenantId, calculatedFieldUpdateMsg));
                }
            }
            if (downlinkMsg.getReportTemplateUpdateMsgCount() > 0) {
                for (ReportTemplateUpdateMsg reportTemplateUpdateMsg : downlinkMsg.getReportTemplateUpdateMsgList()) {
                    result.add(cloudCtx.getReportTemplateProcessor().processReportTemplateMsgFromCloud(tenantId, reportTemplateUpdateMsg));
                }
            }
            if (downlinkMsg.getAiModelUpdateMsgCount() > 0) {
                for (AiModelUpdateMsg aiModelUpdateMsg : downlinkMsg.getAiModelUpdateMsgList()) {
                    result.add(cloudCtx.getAiModelProcessor().processAiModelMsgFromCloud(tenantId, aiModelUpdateMsg));
                }
            }
            if (downlinkMsg.getApiKeyUpdateMsgCount() > 0) {
                for (ApiKeyUpdateMsg apiKeyUpdateMsg : downlinkMsg.getApiKeyUpdateMsgList()) {
                    result.add(cloudCtx.getApiKeyCloudProcessor().processApiKeyMsgFromCloud(tenantId, apiKeyUpdateMsg));
                }
            }
            if (downlinkMsg.getEncryptionKeyUpdateMsgCount() > 0) {
                for (EncryptionKeyUpdateMsg encryptionKeyUpdateMsg : downlinkMsg.getEncryptionKeyUpdateMsgList()) {
                    result.add(cloudCtx.getEncryptionKeyCloudProcessor().processEncryptionKeyMsgFromCloud(tenantId, encryptionKeyUpdateMsg));
                }
            }
            if (downlinkMsg.getSecretUpdateMsgCount() > 0) {
                for (SecretUpdateMsg secretMsg : downlinkMsg.getSecretUpdateMsgList()) {
                    result.add(cloudCtx.getSecretProcessor().processSecretMsgFromCloud(tenantId, secretMsg));
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
                cloudCtx.getCloudEventService().saveCloudEvent(tenantId, CloudEventType.TENANT, EdgeEventActionType.ATTRIBUTES_REQUEST, tenantId, null, null);
                if (customerId != null && !EntityId.NULL_UUID.equals(customerId.getId())) {
                    cloudCtx.getCloudEventService().saveCloudEvent(tenantId, CloudEventType.CUSTOMER, EdgeEventActionType.ATTRIBUTES_REQUEST, customerId, null, null);
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
            return cloudCtx.getCloudEventService().saveCloudEventAsync(tenantId, CloudEventType.DEVICE, EdgeEventActionType.CREDENTIALS_UPDATED, deviceId, null, null);
        } else {
            return Futures.immediateFuture(null);
        }
    }

}
