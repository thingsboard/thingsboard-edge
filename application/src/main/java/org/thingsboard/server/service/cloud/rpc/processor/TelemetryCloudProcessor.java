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
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.edge.v1.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class TelemetryCloudProcessor extends BaseCloudProcessor {

    private final Gson gson = new Gson();

    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreMsgProducer;

    @PostConstruct
    public void init() {
        tbCoreMsgProducer = producerProvider.getTbCoreMsgProducer();
    }

    public List<ListenableFuture<Void>> processTelemetryMsgFromCloud(TenantId tenantId, EntityDataProto entityData) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        EntityId entityId = constructEntityId(entityData);
        if ((entityData.hasPostAttributesMsg() || entityData.hasPostTelemetryMsg() || entityData.hasAttributesUpdatedMsg()) && entityId != null) {
            TbMsgMetaData metaData = constructBaseMsgMetadata(tenantId, entityId);
            metaData.putValue(DataConstants.MSG_SOURCE_KEY, DataConstants.CLOUD_MSG_SOURCE);
            if (entityData.hasPostAttributesMsg()) {
                result.add(processPostAttributes(tenantId, entityId, entityData.getPostAttributesMsg(), metaData));
            }
            if (entityData.hasAttributesUpdatedMsg()) {
                metaData.putValue("scope", entityData.getPostAttributeScope());
                result.add(processAttributesUpdate(tenantId, entityId, entityData.getAttributesUpdatedMsg(), metaData));
            }
            if (entityData.hasPostTelemetryMsg()) {
                result.add(processPostTelemetry(tenantId, entityId, entityData.getPostTelemetryMsg(), metaData));
            }
            if (EntityType.DEVICE.equals(entityId.getEntityType())) {
                DeviceId deviceId = new DeviceId(entityId.getId());

                TransportProtos.DeviceActivityProto deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastActivityTime(System.currentTimeMillis()).build();

                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
                tbCoreMsgProducer.send(tpi, new TbProtoQueueMsg<>(deviceId.getId(),
                        TransportProtos.ToCoreMsg.newBuilder().setDeviceActivityMsg(deviceActivityMsg).build()), null);
            }
        }
        if (entityData.hasAttributeDeleteMsg()) {
            result.add(processAttributeDeleteMsg(tenantId, entityId, entityData.getAttributeDeleteMsg(), entityData.getEntityType()));
        }
        return result;
    }

    private TbMsgMetaData constructBaseMsgMetadata(TenantId tenantId, EntityId entityId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        switch (entityId.getEntityType()) {
            case DEVICE:
                Device device = deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
                if (device != null) {
                    metaData.putValue("deviceName", device.getName());
                    metaData.putValue("deviceType", device.getType());
                }
                break;
            case ASSET:
                Asset asset = assetService.findAssetById(tenantId, new AssetId(entityId.getId()));
                if (asset != null) {
                    metaData.putValue("assetName", asset.getName());
                    metaData.putValue("assetType", asset.getType());
                }
                break;
            case ENTITY_VIEW:
                EntityView entityView = entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId()));
                if (entityView != null) {
                    metaData.putValue("entityViewName", entityView.getName());
                    metaData.putValue("entityViewType", entityView.getType());
                }
                break;
            case EDGE:
                Edge edge = edgeService.findEdgeById(tenantId, new EdgeId(entityId.getId()));
                if (edge != null) {
                    metaData.putValue("edgeName", edge.getName());
                    metaData.putValue("edgeType", edge.getType());
                }
                break;
            case ENTITY_GROUP:
                EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, new EntityGroupId(entityId.getId()));
                if (entityGroup != null) {
                    metaData.putValue("entityGroupName", entityGroup.getName());
                    metaData.putValue("entityGroupType", entityGroup.getType().name());
                }
                break;
            default:
                log.debug("Using empty metadata for entityId [{}]", entityId);
                break;
        }
        return metaData;
    }

    private EntityId constructEntityId(EntityDataProto entityData) {
        EntityType entityType = EntityType.valueOf(entityData.getEntityType());
        switch (entityType) {
            case DEVICE:
                return new DeviceId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ASSET:
                return new AssetId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ENTITY_VIEW:
                return new EntityViewId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case DASHBOARD:
                return new DashboardId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case TENANT:
                return new TenantId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case CUSTOMER:
                return new CustomerId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ENTITY_GROUP:
                return new EntityGroupId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case EDGE:
                return new EdgeId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            default:
                log.warn("Unsupported entity type [{}] during construct of entity id. EntityDataProto [{}]", entityData.getEntityType(), entityData);
                return null;
        }
    }

    private ListenableFuture<Void> processPostTelemetry(TenantId tenantId, EntityId entityId, TransportProtos.PostTelemetryMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
            metaData.putValue("ts", tsKv.getTs() + "");
            TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), entityId, metaData, gson.toJson(json));
            tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    futureToSet.setException(t);
                }
            });
        }
        return futureToSet;
    }

    private void logAttributesUpdated(TenantId tenantId, EntityId entityId, String scope, String attributes) {
        pushEntityActionToRuleEngine(tenantId, entityId, null, null, EdgeEventActionType.ATTRIBUTES_UPDATED, scope, attributes);
    }

    private ListenableFuture<Void> processPostAttributes(TenantId tenantId, EntityId entityId, TransportProtos.PostAttributeMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_ATTRIBUTES_REQUEST.name(), entityId, metaData, gson.toJson(json));
        tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                String errMsg = String.format("Can't process post attributes msg %s", msg);
                log.error(errMsg, t);
                futureToSet.setException(t);
            }
        });
        return futureToSet;
    }

    private ListenableFuture<Void> processAttributesUpdate(TenantId tenantId, EntityId entityId, TransportProtos.PostAttributeMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(json);
        ListenableFuture<List<String>> future = attributesService.save(tenantId, entityId, metaData.getValue("scope"), new ArrayList<>(attributes));
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<String> strings) {
                futureToSet.set(null);
                logAttributesUpdated(tenantId, entityId, metaData.getValue("scope"), gson.toJson(json));
            }

            @Override
            public void onFailure(Throwable t) {
                String errMsg = String.format("Can't process attributes update msg %s", msg);
                log.error(errMsg, t);
                futureToSet.setException(t);
            }
        }, dbCallbackExecutor);
        return futureToSet;
    }

    private ListenableFuture<Void> processAttributeDeleteMsg(TenantId tenantId, EntityId entityId,
                                                             AttributeDeleteMsg attributeDeleteMsg, String entityType) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        String scope = attributeDeleteMsg.getScope();
        List<String> attributeNames = attributeDeleteMsg.getAttributeNamesList();
        attributesService.removeAll(tenantId, entityId, scope, attributeNames);
        if (EntityType.DEVICE.name().equals(entityType)) {
            Set<AttributeKey> attributeKeys = new HashSet<>();
            for (String attributeName : attributeNames) {
                attributeKeys.add(new AttributeKey(scope, attributeName));
            }
            tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(
                    tenantId, (DeviceId) entityId, attributeKeys), new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    String errMsg = String.format("Can't process attributes delete msg %s", attributeDeleteMsg);
                    log.error(errMsg, t);
                    futureToSet.setException(t);
                }
            });
        }
        return futureToSet;
    }

    public UplinkMsg processTelemetryMessageMsgToCloud(CloudEvent cloudEvent) throws Exception {
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

        EdgeEventActionType actionType = EdgeEventActionType.valueOf(cloudEvent.getCloudEventAction());
        return constructEntityDataProtoMsg(entityId, actionType, JsonUtils.parse(JacksonUtil.OBJECT_MAPPER.writeValueAsString(cloudEvent.getEntityBody())));
    }


    private UplinkMsg constructEntityDataProtoMsg(EntityId entityId, EdgeEventActionType actionType, JsonElement entityData) {
        // TODO: voba - get rid of this dummy conversion
        EdgeEventActionType edgeEventActionType = EdgeEventActionType.valueOf(actionType.name());
        EntityDataProto entityDataProto = entityDataMsgConstructor.constructEntityDataMsg(entityId, edgeEventActionType, entityData);
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addEntityData(entityDataProto);
        return builder.build();
    }

    public UplinkMsg processAttributesRequestMsgToCloud(CloudEvent cloudEvent) {
        log.trace("Executing processAttributesRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            List<AttributesRequestMsg> allAttributesRequestMsg = new ArrayList<>();
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
                    .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                    .addAllAttributesRequestMsg(allAttributesRequestMsg);
            return builder.build();
        } catch (Exception e) {
            String errMsg = String.format("Can't send attribute request msg, entityId [%s], body [%s]", cloudEvent.getEntityId(), cloudEvent.getEntityBody());
            log.warn(errMsg, e);
            return null;
        }
    }

}
