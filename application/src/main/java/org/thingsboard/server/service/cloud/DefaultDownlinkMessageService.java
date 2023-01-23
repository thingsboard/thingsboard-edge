/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ConverterUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.GroupPermissionProto;
import org.thingsboard.server.gen.edge.v1.IntegrationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.service.cloud.rpc.processor.AdminSettingsCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.ConverterCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.CustomerCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DashboardCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EdgeCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityGroupCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityViewCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.GroupPermissionCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.IntegrationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.OtaPackageCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.QueueCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RelationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RoleCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RuleChainCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.SchedulerEventCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TelemetryCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.UserCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WhiteLabelingCloudProcessor;
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
    private WhiteLabelingService whiteLabelingService;

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
    private EntityGroupCloudProcessor entityGroupProcessor;

    @Autowired
    private SchedulerEventCloudProcessor schedulerEventProcessor;

    @Autowired
    private RoleCloudProcessor roleProcessor;

    @Autowired
    private GroupPermissionCloudProcessor groupPermissionProcessor;

    @Autowired
    private WhiteLabelingCloudProcessor whiteLabelingProcessor;

    @Autowired
    private WidgetBundleCloudProcessor widgetsBundleProcessor;

    @Autowired
    private WidgetTypeCloudProcessor widgetTypeProcessor;

    @Autowired
    private AdminSettingsCloudProcessor adminSettingsProcessor;

    @Autowired
    private ConverterCloudProcessor converterProcessor;

    @Autowired
    private OtaPackageCloudProcessor otaPackageProcessor;

    @Autowired
    private QueueCloudProcessor queueCloudProcessor;

    @Autowired
    private IntegrationCloudProcessor integrationProcessor;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    private CustomerId customerId;

    public ListenableFuture<List<Void>> processDownlinkMsg(TenantId tenantId,
                                                           DownlinkMsg downlinkMsg,
                                                           EdgeSettings currentEdgeSettings,
                                                           Long queueStartTs) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            log.debug("[{}] Starting process DownlinkMsg. downlinkMsgId [{}],",
                    tenantId, downlinkMsg.getDownlinkMsgId());
            log.trace("DownlinkMsg Body {}", downlinkMsg);
            if (downlinkMsg.hasSyncCompletedMsg()) {
                result.add(updateSyncRequiredState(tenantId, currentEdgeSettings));
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
            if (downlinkMsg.getAssetProfileUpdateMsgCount() > 0) {
                for (AssetProfileUpdateMsg assetProfileUpdateMsg  : downlinkMsg.getAssetProfileUpdateMsgList()) {
                    result.add(assetProfileProcessor.processAssetProfileMsgFromCloud(tenantId, assetProfileUpdateMsg));
                }
            }
            if (downlinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(deviceProcessor.processDeviceCredentialsMsg(tenantId, deviceCredentialsUpdateMsg));
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
                    result.add(dashboardProcessor.processDashboardMsgFromCloud(tenantId, dashboardUpdateMsg, queueStartTs));
                }
            }
            if (downlinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : downlinkMsg.getAlarmUpdateMsgList()) {
                    result.add(alarmProcessor.processAlarmMsg(tenantId, alarmUpdateMsg));
                }
            }
            if (downlinkMsg.getCustomerUpdateMsgCount() > 0) {
                for (CustomerUpdateMsg customerUpdateMsg : downlinkMsg.getCustomerUpdateMsgList()) {
                    sequenceDependencyLock.lock();
                    try {
                        result.add(customerProcessor.processCustomerMsgFromCloud(tenantId, customerUpdateMsg));
                        updateCustomerId(tenantId, customerUpdateMsg);
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : downlinkMsg.getRelationUpdateMsgList()) {
                    result.add(relationProcessor.processRelationMsg(tenantId, relationUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetsBundleUpdateMsgCount() > 0) {
                for (WidgetsBundleUpdateMsg widgetsBundleUpdateMsg : downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                    result.add(widgetsBundleProcessor.processWidgetsBundleMsgFromCloud(tenantId, widgetsBundleUpdateMsg, queueStartTs));
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
            if (downlinkMsg.getEntityGroupUpdateMsgCount() > 0) {
                for (EntityGroupUpdateMsg entityGroupUpdateMsg : downlinkMsg.getEntityGroupUpdateMsgList()) {
                    result.add(entityGroupProcessor.processEntityGroupMsgFromCloud(tenantId, entityGroupUpdateMsg, queueStartTs));
                }
            }
            if (downlinkMsg.hasSystemCustomTranslationMsg()) {
                result.add(whiteLabelingProcessor.processCustomTranslationMsgFromCloud(tenantId, downlinkMsg.getSystemCustomTranslationMsg()));
            }
            if (downlinkMsg.hasTenantCustomTranslationMsg()) {
                result.add(whiteLabelingProcessor.processCustomTranslationMsgFromCloud(tenantId, downlinkMsg.getTenantCustomTranslationMsg()));
            }
            if (downlinkMsg.hasCustomerCustomTranslationMsg()) {
                result.add(whiteLabelingProcessor.processCustomTranslationMsgFromCloud(tenantId, downlinkMsg.getCustomerCustomTranslationMsg()));
            }
            if (downlinkMsg.hasSystemWhiteLabelingParams()) {
                result.add(whiteLabelingProcessor.processWhiteLabelingParamsMsgFromCloud(tenantId, downlinkMsg.getSystemWhiteLabelingParams()));
            }
            if (downlinkMsg.hasTenantWhiteLabelingParams()) {
                result.add(whiteLabelingProcessor.processWhiteLabelingParamsMsgFromCloud(tenantId, downlinkMsg.getTenantWhiteLabelingParams()));
            }
            if (downlinkMsg.hasCustomerWhiteLabelingParams()) {
                result.add(whiteLabelingProcessor.processWhiteLabelingParamsMsgFromCloud(tenantId, downlinkMsg.getCustomerWhiteLabelingParams()));
            }
            if (downlinkMsg.hasSystemLoginWhiteLabelingParams()) {
                result.add(whiteLabelingProcessor.processLoginWhiteLabelingParamsMsgFromCloud(tenantId, downlinkMsg.getSystemLoginWhiteLabelingParams()));
            }
            if (downlinkMsg.hasTenantLoginWhiteLabelingParams()) {
                result.add(whiteLabelingProcessor.processLoginWhiteLabelingParamsMsgFromCloud(tenantId, downlinkMsg.getTenantLoginWhiteLabelingParams()));
            }
            if (downlinkMsg.hasCustomerLoginWhiteLabelingParams()) {
                result.add(whiteLabelingProcessor.processLoginWhiteLabelingParamsMsgFromCloud(tenantId, downlinkMsg.getCustomerLoginWhiteLabelingParams()));
            }
            if (downlinkMsg.getSchedulerEventUpdateMsgCount() > 0) {
                for (SchedulerEventUpdateMsg schedulerEventUpdateMsg : downlinkMsg.getSchedulerEventUpdateMsgList()) {
                    result.add(schedulerEventProcessor.processScheduleEventFromCloud(tenantId, schedulerEventUpdateMsg));
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
            if (downlinkMsg.getRoleMsgCount() > 0) {
                for (RoleProto roleProto : downlinkMsg.getRoleMsgList()) {
                    result.add(roleProcessor.processRoleMsgFromCloud(tenantId, roleProto));
                }
            }
            if (downlinkMsg.getGroupPermissionMsgCount() > 0) {
                for (GroupPermissionProto groupPermissionProto : downlinkMsg.getGroupPermissionMsgList()) {
                    result.add(groupPermissionProcessor.processGroupPermissionMsgFromCloud(tenantId, groupPermissionProto));
                }
            }
            if (downlinkMsg.getConverterMsgCount() > 0) {
                for (ConverterUpdateMsg converterUpdateMsg : downlinkMsg.getConverterMsgList()) {
                    result.add(converterProcessor.processConverterMsgFromCloud(tenantId, converterUpdateMsg));
                }
            }
            if (downlinkMsg.getIntegrationMsgCount() > 0) {
                for (IntegrationUpdateMsg integrationUpdateMsg : downlinkMsg.getIntegrationMsgList()) {
                    result.add(integrationProcessor.processIntegrationMsgFromCloud(tenantId, integrationUpdateMsg));
                }
            }
            log.trace("Finished processing DownlinkMsg {}", downlinkMsg.getDownlinkMsgId());
        } catch (Exception e) {
            log.error("Can't process downlink message [{}]", downlinkMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException("Can't process downlink message", e));
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

    private void updateCustomerId(TenantId tenantId, CustomerUpdateMsg customerUpdateMsg) {
        switch (customerUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                customerId = null;
                break;
        }

        EntityId ownerId = customerId != null && !customerId.isNullUid() ? customerId : tenantId;
        whiteLabelingService.saveOrUpdateEdgeLoginWhiteLabelSettings(tenantId, ownerId);
    }

    private ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            return cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.DEVICE, EdgeEventActionType.CREDENTIALS_UPDATED, deviceId, null, null, 0L);
        } else {
            return Futures.immediateFuture(null);
        }
    }

}
