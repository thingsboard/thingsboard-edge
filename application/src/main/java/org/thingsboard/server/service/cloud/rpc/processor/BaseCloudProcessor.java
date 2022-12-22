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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.CloudUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.edge.rpc.constructor.AlarmMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RelationMsgConstructor;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.entitiy.queue.TbQueueService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class BaseCloudProcessor {

    protected static final Lock deviceCreationLock = new ReentrantLock();

    protected static final Lock assetCreationLock = new ReentrantLock();

    protected static final Lock widgetCreationLock = new ReentrantLock();

    protected final Lock customerCreationLock = new ReentrantLock();

    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    @Autowired
    protected TbNotificationEntityService notificationEntityService;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected TimeseriesService timeseriesService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected TbDeviceProfileCache deviceProfileCache;

    @Autowired
    protected TbAssetProfileCache assetProfileCache;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected DeviceProfileService deviceProfileService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    protected ApiUsageStateService apiUsageStateService;

    @Autowired
    protected TbCoreDeviceRpcService tbCoreDeviceRpcService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected OtaPackageService otaPackageService;

    @Autowired
    protected QueueService queueService;

    @Autowired
    protected EntityGroupService entityGroupService;

    @Autowired
    protected RoleService roleService;

    @Autowired
    protected TbQueueService tbQueueService;

    @Autowired
    protected CloudEventService cloudEventService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected AdminSettingsService adminSettingsService;

    @Autowired
    protected EdgeService edgeService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    protected OtaPackageStateService otaPackageStateService;

    @Autowired
    protected PartitionService partitionService;

    @Autowired
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    @Autowired
    protected DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    protected WhiteLabelingService whiteLabelingService;

    @Autowired
    protected CustomTranslationService customTranslationService;

    @Autowired
    protected GroupPermissionService groupPermissionService;

    @Autowired
    protected EntityDataMsgConstructor entityDataMsgConstructor;

    @Autowired
    protected AlarmMsgConstructor alarmMsgConstructor;

    @Autowired
    protected RelationMsgConstructor relationMsgConstructor;

    @Autowired
    protected DeviceMsgConstructor deviceMsgConstructor;

    @Autowired
    protected DataValidator<Device> deviceValidator;

    @Autowired
    protected DataValidator<Asset> assetValidator;

    @Autowired
    protected DataValidator<Role> roleValidator;

    @Autowired
    protected DataValidator<Converter> converterValidator;

    @Autowired
    protected DataValidator<Integration> integrationValidator;

    @Autowired
    protected DataValidator<Tenant> tenantValidator;

    protected ListenableFuture<Void> requestForAdditionalData(TenantId tenantId, UpdateMsgType updateMsgType, EntityId entityId, Long queueStartTs) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(updateMsgType) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(updateMsgType)) {
            CloudEventType cloudEventType = CloudUtils.getCloudEventTypeByEntityType(entityId.getEntityType());

            log.info("Adding ATTRIBUTES_REQUEST/RELATION_REQUEST {} {}", entityId, cloudEventType);
            futures.add(cloudEventService.saveCloudEventAsync(tenantId, cloudEventType,
                    EdgeEventActionType.ATTRIBUTES_REQUEST, entityId, null, null, queueStartTs));
            futures.add(cloudEventService.saveCloudEventAsync(tenantId, cloudEventType,
                    EdgeEventActionType.RELATION_REQUEST, entityId, null, null, queueStartTs));
            if (CloudEventType.DEVICE.equals(cloudEventType) || CloudEventType.ASSET.equals(cloudEventType)) {
                futures.add(cloudEventService.saveCloudEventAsync(tenantId, cloudEventType,
                        EdgeEventActionType.ENTITY_VIEW_REQUEST, entityId, null, null, queueStartTs));
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutor);
    }

    protected void addEntityToGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        if (entityGroupId != null && !ModelConstants.NULL_UUID.equals(entityGroupId.getId())) {
            ListenableFuture<EntityGroup> entityGroupFuture = entityGroupService.findEntityGroupByIdAsync(tenantId, entityGroupId);
            Futures.addCallback(entityGroupFuture, new FutureCallback<EntityGroup>() {
                @Override
                public void onSuccess(EntityGroup entityGroup) {
                    if (entityGroup != null) {
                        entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, entityId);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("[{}] Failed to add entity to group: {}", entityId, t.getMessage(), t);
                }
            }, dbCallbackExecutor);
        }
    }

    protected UUID safeGetUUID(long mSB, long lSB) {
        return mSB != 0 && lSB != 0 ? new UUID(mSB, lSB) : null;
    }

    protected CustomerId safeGetCustomerId(long mSB, long lSB) {
        CustomerId customerId = null;
        UUID customerUUID = safeGetUUID(mSB, lSB);
        if (customerUUID != null) {
            customerId = new CustomerId(customerUUID);
        }
        return customerId;
    }

    protected EntityId safeGetOwnerId(TenantId tenantId, String ownerEntityTypeStr, long mSB, long lSB) {
        EntityType ownerEntityType = EntityType.valueOf(ownerEntityTypeStr);
        if (EntityType.CUSTOMER.equals(ownerEntityType)) {
            return new CustomerId(new UUID(mSB, lSB));
        } else {
            return tenantId;
        }
    }

    protected ListenableFuture<Void> handleUnsupportedMsgType(UpdateMsgType msgType) {
        String errMsg = String.format("Unsupported msg type %s", msgType);
        log.error(errMsg);
        return Futures.immediateFailedFuture(new RuntimeException(errMsg));
    }

    protected UpdateMsgType getUpdateMsgType(EdgeEventActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED:
            case ASSIGNED_TO_EDGE:
            case RELATION_ADD_OR_UPDATE:
            case ADDED_TO_ENTITY_GROUP:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
            case RELATION_DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        }
    }

    protected EntityId constructEntityId(String entityTypeStr, long entityIdMSB, long entityIdLSB) {
        EntityType entityType = EntityType.valueOf(entityTypeStr);
        switch (entityType) {
            case DEVICE:
                return new DeviceId(new UUID(entityIdMSB, entityIdLSB));
            case ASSET:
                return new AssetId(new UUID(entityIdMSB, entityIdLSB));
            case ENTITY_VIEW:
                return new EntityViewId(new UUID(entityIdMSB, entityIdLSB));
            case DASHBOARD:
                return new DashboardId(new UUID(entityIdMSB, entityIdLSB));
            case TENANT:
                return TenantId.fromUUID(new UUID(entityIdMSB, entityIdLSB));
            case CUSTOMER:
                return new CustomerId(new UUID(entityIdMSB, entityIdLSB));
            case USER:
                return new UserId(new UUID(entityIdMSB, entityIdLSB));
            case EDGE:
                return new EdgeId(new UUID(entityIdMSB, entityIdLSB));
            case ENTITY_GROUP:
                return new EntityGroupId(new UUID(entityIdMSB, entityIdLSB));
            default:
                log.warn("Unsupported entity type [{}] during construct of entity id. entityIdMSB [{}], entityIdLSB [{}]",
                        entityTypeStr, entityIdMSB, entityIdLSB);
                return null;
        }
    }
}
