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
package org.thingsboard.server.common.data.id;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventType;

import java.util.UUID;

/**
 * Created by ashvayka on 25.04.17.
 */
public class EntityIdFactory {

    public static EntityId getByTypeAndId(String type, String uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndId(EntityType type, String uuid) {
        return getByTypeAndUuid(type, UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndUuid(String type, UUID uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), uuid);
    }

    public static EntityId getByTypeAndUuid(EntityType type, String uuid) {
        return getByTypeAndUuid(type, UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndUuid(EntityType type, UUID uuid) {
        switch (type) {
            case TENANT:
                return TenantId.fromUUID(uuid);
            case CUSTOMER:
                return new CustomerId(uuid);
            case USER:
                return new UserId(uuid);
            case DASHBOARD:
                return new DashboardId(uuid);
            case DEVICE:
                return new DeviceId(uuid);
            case ASSET:
                return new AssetId(uuid);
            case CONVERTER:
                return new ConverterId(uuid);
            case INTEGRATION:
                return new IntegrationId(uuid);
            case ALARM:
                return new AlarmId(uuid);
            case ENTITY_GROUP:
                return new EntityGroupId(uuid);
            case RULE_CHAIN:
                return new RuleChainId(uuid);
            case RULE_NODE:
                return new RuleNodeId(uuid);
            case SCHEDULER_EVENT:
                return new SchedulerEventId(uuid);
            case BLOB_ENTITY:
                return new BlobEntityId(uuid);
            case ENTITY_VIEW:
                return new EntityViewId(uuid);
            case ROLE:
                return new RoleId(uuid);
            case GROUP_PERMISSION:
                return new GroupPermissionId(uuid);
            case WIDGETS_BUNDLE:
                return new WidgetsBundleId(uuid);
            case WIDGET_TYPE:
                return new WidgetTypeId(uuid);
            case DEVICE_PROFILE:
                return new DeviceProfileId(uuid);
            case ASSET_PROFILE:
                return new AssetProfileId(uuid);
            case TENANT_PROFILE:
                return new TenantProfileId(uuid);
            case API_USAGE_STATE:
                return new ApiUsageStateId(uuid);
            case TB_RESOURCE:
                return new TbResourceId(uuid);
            case OTA_PACKAGE:
                return new OtaPackageId(uuid);
            case EDGE:
                return new EdgeId(uuid);
            case RPC:
                return new RpcId(uuid);
            case QUEUE:
                return new QueueId(uuid);
        }
        throw new IllegalArgumentException("EntityType " + type + " is not supported!");
    }

    public static EntityId getByEdgeEventTypeAndUuid(EdgeEventType edgeEventType, UUID uuid) {
        switch (edgeEventType) {
            case TENANT:
                return new TenantId(uuid);
            case CUSTOMER:
                return new CustomerId(uuid);
            case USER:
                return new UserId(uuid);
            case DASHBOARD:
                return new DashboardId(uuid);
            case DEVICE:
                return new DeviceId(uuid);
            case DEVICE_PROFILE:
                return new DeviceProfileId(uuid);
            case ASSET_PROFILE:
                return new AssetProfileId(uuid);
            case ASSET:
                return new AssetId(uuid);
            case ALARM:
                return new AlarmId(uuid);
            case RULE_CHAIN:
                return new RuleChainId(uuid);
            case ENTITY_VIEW:
                return new EntityViewId(uuid);
            case WIDGETS_BUNDLE:
                return new WidgetsBundleId(uuid);
            case WIDGET_TYPE:
                return new WidgetTypeId(uuid);
            case OTA_PACKAGE:
                return new OtaPackageId(uuid);
            case QUEUE:
                return new QueueId(uuid);
            case EDGE:
                return new EdgeId(uuid);
            case SCHEDULER_EVENT:
                return new SchedulerEventId(uuid);
            case ENTITY_GROUP:
                return new EntityGroupId(uuid);
            case ROLE:
                return new RoleId(uuid);
            case GROUP_PERMISSION:
                return new GroupPermissionId(uuid);
            case INTEGRATION:
                return new IntegrationId(uuid);
            case CONVERTER:
                return new ConverterId(uuid);
        }
        throw new IllegalArgumentException("EdgeEventType " + edgeEventType + " is not supported!");
    }

    public static EntityId getByCloudEventTypeAndUuid(CloudEventType cloudEventType, UUID uuid) {
        switch (cloudEventType) {
            case EDGE:
                return new EdgeId(uuid);
            case TENANT:
                return new TenantId(uuid);
            case CUSTOMER:
                return new CustomerId(uuid);
            case USER:
                return new UserId(uuid);
            case DASHBOARD:
                return new DashboardId(uuid);
            case DEVICE:
                return new DeviceId(uuid);
            case DEVICE_PROFILE:
                return new DeviceProfileId(uuid);
            case ASSET:
                return new AssetId(uuid);
            case ALARM:
                return new AlarmId(uuid);
            case RULE_CHAIN:
                return new RuleChainId(uuid);
            case ENTITY_VIEW:
                return new EntityViewId(uuid);
            case ENTITY_GROUP:
                return new EntityGroupId(uuid);
            case WIDGETS_BUNDLE:
                return new WidgetsBundleId(uuid);
            case WIDGET_TYPE:
                return new WidgetTypeId(uuid);
            case SCHEDULER_EVENT:
                return new SchedulerEventId(uuid);
            case ROLE:
                return new RoleId(uuid);
            case GROUP_PERMISSION:
                return new GroupPermissionId(uuid);
        }
        throw new IllegalArgumentException("CloudEventType " + cloudEventType + " is not supported!");
    }
}
