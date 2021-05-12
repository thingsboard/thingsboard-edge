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
package org.thingsboard.server.service.cloud.processor.uplink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceProfileDevicesRequestMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.service.cloud.constructor.AlarmUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.RelationUpdateMsgConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@Slf4j
@Component
public class UplinkProcessor {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private EntityDataMsgConstructor entityDataMsgConstructor;

    @Autowired
    private AlarmUpdateMsgConstructor alarmUpdateMsgConstructor;

    @Autowired
    private RelationUpdateMsgConstructor relationUpdateMsgConstructor;

    @Autowired
    private DeviceUpdateMsgConstructor deviceUpdateMsgConstructor;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    public UplinkMsg processEntityMessage(TenantId tenantId, CloudEvent cloudEvent, ActionType edgeEventAction) {
        UpdateMsgType msgType = getResponseMsgType(ActionType.valueOf(cloudEvent.getCloudEventAction()));
        log.trace("Executing processEntityMessage, cloudEvent [{}], edgeEventAction [{}], msgType [{}]", cloudEvent, edgeEventAction, msgType);
        switch (cloudEvent.getCloudEventType()) {
            case DEVICE:
                return processDevice(tenantId, cloudEvent, msgType, edgeEventAction);
            case ALARM:
                return processAlarm(tenantId, cloudEvent, msgType);
            case RELATION:
                return processRelation(cloudEvent, msgType);
            default:
                log.warn("Unsupported cloud event type [{}]", cloudEvent);
                return null;
        }
    }

    private UplinkMsg processDevice(TenantId tenantId, CloudEvent cloudEvent, UpdateMsgType msgType, ActionType edgeActionType) {
        try {
            DeviceId deviceId = new DeviceId(cloudEvent.getEntityId());
            UplinkMsg msg = null;
            switch (edgeActionType) {
                case ADDED:
                case UPDATED:
                    Device device = deviceService.findDeviceById(cloudEvent.getTenantId(), deviceId);
                    if (device != null) {
                        DeviceUpdateMsg deviceUpdateMsg =
                                deviceUpdateMsgConstructor.constructDeviceUpdatedMsg(msgType, device);
                        msg = UplinkMsg.newBuilder()
                                .addAllDeviceUpdateMsg(Collections.singletonList(deviceUpdateMsg)).build();
                    } else {
                        log.info("Skipping event as device was not found [{}]", cloudEvent);
                    }
                    break;
                case DELETED:
                    DeviceUpdateMsg deviceUpdateMsg =
                            deviceUpdateMsgConstructor.constructDeviceDeleteMsg(deviceId);
                    msg = UplinkMsg.newBuilder()
                            .addAllDeviceUpdateMsg(Collections.singletonList(deviceUpdateMsg)).build();
                    break;
                case CREDENTIALS_UPDATED:
                    DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
                    if (deviceCredentials != null) {
                        DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg =
                                deviceUpdateMsgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                        msg = UplinkMsg.newBuilder()
                                .addAllDeviceCredentialsUpdateMsg(Collections.singletonList(deviceCredentialsUpdateMsg)).build();
                    } else {
                        log.info("Skipping event as device credentials was not found [{}]", cloudEvent);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported edge action type [" + edgeActionType + "]");
            }
            return msg;
        } catch (Exception e) {
            log.error("Can't process device msg [{}] [{}]", cloudEvent, msgType, e);
            return null;
        }
    }

    private UplinkMsg processAlarm(TenantId tenantId, CloudEvent cloudEvent, UpdateMsgType msgType) {
        try {
            AlarmId alarmId = new AlarmId(cloudEvent.getEntityId());
            Alarm alarm = alarmService.findAlarmByIdAsync(cloudEvent.getTenantId(), alarmId).get();
            UplinkMsg msg = null;
            if (alarm != null) {
                AlarmUpdateMsg alarmUpdateMsg = alarmUpdateMsgConstructor.constructAlarmUpdatedMsg(tenantId, msgType, alarm);
                msg = UplinkMsg.newBuilder()
                        .addAllAlarmUpdateMsg(Collections.singletonList(alarmUpdateMsg)).build();
            } else {
                log.info("Skipping event as alarm was not found [{}]", cloudEvent);
            }
            return msg;
        } catch (Exception e) {
            log.error("Can't process alarm msg [{}] [{}]", cloudEvent, msgType, e);
            return null;
        }
    }

    private UplinkMsg processRelation(CloudEvent cloudEvent, UpdateMsgType msgType) {
        log.trace("Executing processRelation, cloudEvent [{}]", cloudEvent);
        UplinkMsg msg = null;
        try {
            EntityRelation entityRelation = mapper.convertValue(cloudEvent.getEntityBody(), EntityRelation.class);
            if (entityRelation != null) {
                RelationUpdateMsg relationUpdateMsg = relationUpdateMsgConstructor.constructRelationUpdatedMsg(msgType, entityRelation);
                msg = UplinkMsg.newBuilder()
                        .addAllRelationUpdateMsg(Collections.singletonList(relationUpdateMsg)).build();
            }
        } catch (Exception e) {
            log.error("Can't process relation msg [{}] [{}]", cloudEvent, msgType, e);
        }
        return msg;
    }

    private UpdateMsgType getResponseMsgType(ActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED:
            case RELATION_ADD_OR_UPDATE:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED:
            case RELATION_DELETED:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        }
    }

    public UplinkMsg processRelationRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processRelationRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            RelationRequestMsg relationRequestMsg = RelationRequestMsg.newBuilder()
                    .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                    .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                    .setEntityType(entityId.getEntityType().name())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllRelationRequestMsg(Collections.singletonList(relationRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't send relation request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    public UplinkMsg processRuleChainMetadataRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processRuleChainMetadataRequest, cloudEvent [{}]", cloudEvent);
        EntityId ruleChainId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg = RuleChainMetadataRequestMsg.newBuilder()
                    .setRuleChainIdMSB(ruleChainId.getId().getMostSignificantBits())
                    .setRuleChainIdLSB(ruleChainId.getId().getLeastSignificantBits())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllRuleChainMetadataRequestMsg(Collections.singletonList(ruleChainMetadataRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't send rule chain metadata request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    public UplinkMsg processCredentialsRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processCredentialsRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            UplinkMsg msg = null;
            switch (entityId.getEntityType()) {
                case USER:
                    UserCredentialsRequestMsg userCredentialsRequestMsg = UserCredentialsRequestMsg.newBuilder()
                            .setUserIdMSB(entityId.getId().getMostSignificantBits())
                            .setUserIdLSB(entityId.getId().getLeastSignificantBits())
                            .build();
                    msg = UplinkMsg.newBuilder()
                            .addAllUserCredentialsRequestMsg(Collections.singletonList(userCredentialsRequestMsg))
                            .build();
                    break;
                case DEVICE:
                    DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                            .setDeviceIdMSB(entityId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(entityId.getId().getLeastSignificantBits())
                            .build();
                    msg = UplinkMsg.newBuilder()
                            .addAllDeviceCredentialsRequestMsg(Collections.singletonList(deviceCredentialsRequestMsg))
                            .build();
                    break;
                default:
                    log.info("Skipping event as entity type doesn't supported [{}]", cloudEvent);
            }
            return msg;
        } catch (Exception e) {
            log.warn("Can't send credentials request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    public UplinkMsg processGroupEntitiesRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processGroupEntitiesRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityGroupId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        String type = cloudEvent.getEntityBody().get("type").asText();
        try {
            EntityGroupRequestMsg entityGroupEntitiesRequestMsg = EntityGroupRequestMsg.newBuilder()
                    .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                    .setType(type)
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllEntityGroupEntitiesRequestMsg(Collections.singletonList(entityGroupEntitiesRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't group entities credentials request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    public UplinkMsg processRpcCallResponse(CloudEvent cloudEvent) {
        log.trace("Executing processRpcCallResponse, cloudEvent [{}]", cloudEvent);
        UplinkMsg msg = null;
        try {
            DeviceId deviceId = new DeviceId(cloudEvent.getEntityId());
            DeviceRpcCallMsg rpcResponseMsg = deviceUpdateMsgConstructor.constructDeviceRpcResponseMsg(deviceId, cloudEvent.getEntityBody());
            msg = UplinkMsg.newBuilder()
                    .addAllDeviceRpcCallMsg(Collections.singletonList(rpcResponseMsg)).build();
        } catch (Exception e) {
            log.error("Can't process RPC response msg [{}]", cloudEvent, e);
        }
        return msg;
    }

    public UplinkMsg processDeviceProfileDevicesRequest(CloudEvent cloudEvent) {
        log.trace("Executing processDeviceProfileDevicesRequest, cloudEvent [{}]", cloudEvent);
        EntityId deviceProfileId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            DeviceProfileDevicesRequestMsg deviceProfileDevicesRequestMsg = DeviceProfileDevicesRequestMsg.newBuilder()
                    .setDeviceProfileIdMSB(deviceProfileId.getId().getMostSignificantBits())
                    .setDeviceProfileIdLSB(deviceProfileId.getId().getLeastSignificantBits())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllDeviceProfileDevicesRequestMsg(Collections.singletonList(deviceProfileDevicesRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Failed process device profiles devices request, entityId [{}]", cloudEvent.getEntityId(), e);
            return null;
        }
    }

    public UplinkMsg processWidgetBundleTypesRequest(CloudEvent cloudEvent) {
        log.trace("Executing processWidgetBundleTypesRequest, cloudEvent [{}]", cloudEvent);
        EntityId widgetBundleId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg = WidgetBundleTypesRequestMsg.newBuilder()
                    .setWidgetBundleIdMSB(widgetBundleId.getId().getMostSignificantBits())
                    .setWidgetBundleIdLSB(widgetBundleId.getId().getLeastSignificantBits())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllWidgetBundleTypesRequestMsg(Collections.singletonList(widgetBundleTypesRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Failed process widget bundle types request, entityId [{}]", cloudEvent.getEntityId(), e);
            return null;
        }
    }

    public UplinkMsg processEntityViewRequest(CloudEvent cloudEvent) {
        log.trace("Executing processEntityViewRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            EntityViewsRequestMsg entityViewsRequestMsg = EntityViewsRequestMsg.newBuilder()
                    .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                    .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                    .setEntityType(entityId.getEntityType().name())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllEntityViewsRequestMsg(Collections.singletonList(entityViewsRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't send relation request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    public UplinkMsg processEntityGroupPermissionsRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processEntityGroupPermissionsRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityGroupId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        String type = cloudEvent.getEntityBody().get("type").asText();
        try {
            EntityGroupRequestMsg entityGroupPermissionsRequestMsg = EntityGroupRequestMsg.newBuilder()
                    .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                    .setType(type)
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllEntityGroupPermissionsRequestMsg(Collections.singletonList(entityGroupPermissionsRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Failed process group permissions request, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    public UplinkMsg processTelemetryMessage(CloudEvent cloudEvent) {
        try {
            log.trace("Executing processTelemetryMessage, cloudEvent [{}]", cloudEvent);
            EntityId entityId;
            switch (cloudEvent.getCloudEventType()) {
                case DEVICE:
                    entityId = new DeviceId(cloudEvent.getEntityId());
                    break;
                case ASSET:
                    entityId = new AssetId(cloudEvent.getEntityId());
                    break;
                case ENTITY_VIEW:
                    entityId = new EntityViewId(cloudEvent.getEntityId());
                    break;
                case DASHBOARD:
                    entityId = new DashboardId(cloudEvent.getEntityId());
                    break;
                case ENTITY_GROUP:
                    entityId = new EntityGroupId(cloudEvent.getEntityId());
                    break;
                default:
                    throw new IllegalAccessException("Unsupported cloud event type [" + cloudEvent.getCloudEventType() + "]");
            }

            ActionType actionType = ActionType.valueOf(cloudEvent.getCloudEventAction());
            return constructEntityDataProtoMsg(entityId, actionType, JsonUtils.parse(mapper.writeValueAsString(cloudEvent.getEntityBody())));
        } catch (Exception e) {
            log.warn("Can't convert telemetry data msg, cloudEvent [{}]", cloudEvent, e);
            return null;
        }
    }

    private UplinkMsg constructEntityDataProtoMsg(EntityId entityId, ActionType actionType, JsonElement entityData) {
        EntityDataProto entityDataProto = entityDataMsgConstructor.constructEntityDataMsg(entityId, actionType, entityData);
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .addAllEntityData(Collections.singletonList(entityDataProto));
        return builder.build();
    }

    public UplinkMsg processAttributesRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processAttributesRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            ArrayList<AttributesRequestMsg> allAttributesRequestMsg = new ArrayList<>();
            AttributesRequestMsg serverAttributesRequestMsg = AttributesRequestMsg.newBuilder()
                    .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                    .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                    .setEntityType(entityId.getEntityType().name())
                    .setScope(DataConstants.SERVER_SCOPE)
                    .build();
            allAttributesRequestMsg.add(serverAttributesRequestMsg);
            if (EntityType.DEVICE.equals(entityId.getEntityType())) {
                AttributesRequestMsg sharedAttributesRequestMsg = AttributesRequestMsg.newBuilder()
                        .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                        .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                        .setEntityType(entityId.getEntityType().name())
                        .setScope(DataConstants.SHARED_SCOPE)
                        .build();
                allAttributesRequestMsg.add(sharedAttributesRequestMsg);
            }
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllAttributesRequestMsg(allAttributesRequestMsg);
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't send attribute request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }
}
