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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.rule.engine.api.msg.DeviceEdgeUpdateMsg;
import org.thingsboard.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasRuleEngineProfile;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.FromDeviceRPCResponseProto;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationDownlinkMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.MultipleTbQueueCallbackWrapper;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTbClusterService implements TbClusterService {

    @Value("${cluster.stats.enabled:false}")
    private boolean statsEnabled;
    @Value("${edges.enabled}")
    protected boolean edgesEnabled;
    @Value("${service.type:monolith}")
    private String serviceType;

    private final AtomicInteger toCoreMsgs = new AtomicInteger(0);
    private final AtomicInteger toCoreNfs = new AtomicInteger(0);
    private final AtomicInteger toIntegrationExecutorNfs = new AtomicInteger(0);
    private final AtomicInteger toRuleEngineMsgs = new AtomicInteger(0);
    private final AtomicInteger toRuleEngineNfs = new AtomicInteger(0);
    private final AtomicInteger toTransportNfs = new AtomicInteger(0);

    @Autowired
    @Lazy
    private PartitionService partitionService;

    @Autowired
    @Lazy
    private TbQueueProducerProvider producerProvider;

    @Autowired
    @Lazy
    private OtaPackageStateService otaPackageStateService;

    private final NotificationsTopicService notificationsTopicService;
    private final DataDecodingEncodingService encodingService;
    private final TbDeviceProfileCache deviceProfileCache;
    private final TbAssetProfileCache assetProfileCache;
    private final GatewayNotificationsService gatewayNotificationsService;
    private final EntityGroupService entityGroupService;
    private final EdgeService edgeService;
    private final DbCallbackExecutorService dbCallbackExecutor;

    @Override
    public void pushMsgToCore(TenantId tenantId, EntityId entityId, ToCoreMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), callback);
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToCore(TopicPartitionInfo tpi, UUID msgId, ToCoreMsg msg, TbQueueCallback callback) {
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(msgId, msg), callback);
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToCore(ToDeviceActorNotificationMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, msg.getTenantId(), msg.getDeviceId());
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        byte[] msgBytes = encodingService.encode(msg);
        ToCoreMsg toCoreMsg = ToCoreMsg.newBuilder().setToDeviceActorNotificationMsg(ByteString.copyFrom(msgBytes)).build();
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(msg.getDeviceId().getId(), toCoreMsg), callback);
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToVersionControl(TenantId tenantId, TransportProtos.ToVersionControlServiceMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_VC_EXECUTOR, tenantId, tenantId);
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        producerProvider.getTbVersionControlMsgProducer().send(tpi, new TbProtoQueueMsg<>(tenantId.getId(), msg), callback);
        //TODO: ashvayka
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushNotificationToCore(String serviceId, IntegrationDownlinkMsg downlink, TbQueueCallback callback) {
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
        IntegrationDownlinkMsgProto.Builder builder = IntegrationDownlinkMsgProto.newBuilder()
                .setTenantIdMSB(downlink.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(downlink.getTenantId().getId().getLeastSignificantBits())
                .setIntegrationIdMSB(downlink.getIntegrationId().getId().getMostSignificantBits())
                .setIntegrationIdLSB(downlink.getIntegrationId().getId().getLeastSignificantBits())
                .setData(TbMsg.toByteString(downlink.getTbMsg()));
        ToCoreNotificationMsg msg = ToCoreNotificationMsg.newBuilder().setIntegrationDownlinkMsg(builder).build();
        producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(downlink.getTbMsg().getId(), msg), callback);
        toCoreNfs.incrementAndGet();
    }

    @Override
    public void pushNotificationToCore(String serviceId, FromDeviceRpcResponse response, TbQueueCallback callback) {
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
        log.trace("PUSHING msg: {} to core: {}", response, tpi);
        var builder = prepareRPCResponseProto(response);
        ToCoreNotificationMsg msg = ToCoreNotificationMsg.newBuilder().setFromDeviceRpcResponse(builder).build();
        producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(response.getId(), msg), callback);
        toCoreNfs.incrementAndGet();
    }

    @Override
    public void pushNotificationToCore(String targetServiceId, TransportProtos.RestApiCallResponseMsgProto responseMsgProto, TbQueueCallback callback) {
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, targetServiceId);
        ToCoreNotificationMsg msg = ToCoreNotificationMsg.newBuilder().setRestApiCallResponseMsg(responseMsgProto).build();
        producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), callback);
        toCoreNfs.incrementAndGet();
    }

    @Override
    public void pushMsgToRuleEngine(TopicPartitionInfo tpi, UUID msgId, ToRuleEngineMsg msg, TbQueueCallback callback) {
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        producerProvider.getRuleEngineMsgProducer().send(tpi, new TbProtoQueueMsg<>(msgId, msg), callback);
        toRuleEngineMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbMsg tbMsg, TbQueueCallback callback) {
        pushMsgToRuleEngine(tenantId, entityId, tbMsg, false, callback);
    }

    @Override
    public void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbMsg tbMsg, boolean useQueueFromTbMsg, TbQueueCallback callback) {
        if (tenantId == null || tenantId.isNullUid()) {
            if (entityId.getEntityType().equals(EntityType.TENANT)) {
                tenantId = TenantId.fromUUID(entityId.getId());
            } else {
                log.warn("[{}][{}] Received invalid message: {}", tenantId, entityId, tbMsg);
                return;
            }
        } else {
            HasRuleEngineProfile ruleEngineProfile = getRuleEngineProfileForEntityOrElseNull(tenantId, entityId);
            if (ruleEngineProfile != null) {
                tbMsg = transformMsg(tbMsg, ruleEngineProfile, useQueueFromTbMsg);
            }
        }
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, entityId);
        log.trace("PUSHING msg: {} to:{}", tbMsg, tpi);
        ToRuleEngineMsg msg = ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg)).build();
        producerProvider.getRuleEngineMsgProducer().send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
        toRuleEngineMsgs.incrementAndGet();
    }

    private HasRuleEngineProfile getRuleEngineProfileForEntityOrElseNull(TenantId tenantId, EntityId entityId) {
        if (entityId.getEntityType().equals(EntityType.DEVICE)) {
            return deviceProfileCache.get(tenantId, new DeviceId(entityId.getId()));
        } else if (entityId.getEntityType().equals(EntityType.DEVICE_PROFILE)) {
            return deviceProfileCache.get(tenantId, new DeviceProfileId(entityId.getId()));
        } else if (entityId.getEntityType().equals(EntityType.ASSET)) {
            return assetProfileCache.get(tenantId, new AssetId(entityId.getId()));
        } else if (entityId.getEntityType().equals(EntityType.ASSET_PROFILE)) {
            return assetProfileCache.get(tenantId, new AssetProfileId(entityId.getId()));
        }
        return null;
    }

    private TbMsg transformMsg(TbMsg tbMsg, HasRuleEngineProfile ruleEngineProfile, boolean useQueueFromTbMsg) {
        if (ruleEngineProfile != null) {
            RuleChainId targetRuleChainId = ruleEngineProfile.getDefaultRuleChainId();
            String targetQueueName = useQueueFromTbMsg ? tbMsg.getQueueName() : ruleEngineProfile.getDefaultQueueName();

            boolean isRuleChainTransform = targetRuleChainId != null && !targetRuleChainId.equals(tbMsg.getRuleChainId());
            boolean isQueueTransform = targetQueueName != null && !targetQueueName.equals(tbMsg.getQueueName());

            if (isRuleChainTransform && isQueueTransform) {
                tbMsg = TbMsg.transformMsg(tbMsg, targetRuleChainId, targetQueueName);
            } else if (isRuleChainTransform) {
                tbMsg = TbMsg.transformMsg(tbMsg, targetRuleChainId);
            } else if (isQueueTransform) {
                tbMsg = TbMsg.transformMsg(tbMsg, targetQueueName);
            }
        }
        return tbMsg;
    }

    @Override
    public void pushNotificationToRuleEngine(String serviceId, FromDeviceRpcResponse response, TbQueueCallback callback) {
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceId);
        log.trace("PUSHING msg: {} to rule engine: {}", response, tpi);
        var builder = prepareRPCResponseProto(response);
        ToRuleEngineNotificationMsg msg = ToRuleEngineNotificationMsg.newBuilder().setFromDeviceRpcResponse(builder).build();
        producerProvider.getRuleEngineNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(response.getId(), msg), callback);
        toRuleEngineNfs.incrementAndGet();
    }

    @Override
    public void pushNotificationToTransport(String serviceId, ToTransportMsg response, TbQueueCallback callback) {
        if (serviceId == null || serviceId.isEmpty()) {
            log.trace("pushNotificationToTransport: skipping message without serviceId [{}], (ToTransportMsg) response [{}]", serviceId, response);
            if (callback != null) {
                callback.onSuccess(null); //callback that message already sent, no useful payload expected
            }
            return;
        }
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        producerProvider.getTransportNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), response), callback);
        toTransportNfs.incrementAndGet();
    }

    @Override
    public void broadcastEntityStateChangeEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state) {
        log.trace("[{}] Processing {} state change event: {}", tenantId, entityId.getEntityType(), state);
        broadcast(new ComponentLifecycleMsg(tenantId, entityId, state));
    }

    @Override
    public void onDeviceProfileChange(DeviceProfile deviceProfile, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(deviceProfile.getTenantId(), deviceProfile.getId(), deviceProfile, callback);
    }

    @Override
    public void onTenantProfileChange(TenantProfile tenantProfile, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(TenantId.SYS_TENANT_ID, tenantProfile.getId(), tenantProfile, callback);
    }

    @Override
    public void onTenantChange(Tenant tenant, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(TenantId.SYS_TENANT_ID, tenant.getId(), tenant, callback);
    }

    @Override
    public void onApiStateChange(ApiUsageState apiUsageState, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(apiUsageState.getTenantId(), apiUsageState.getId(), apiUsageState, callback);
        broadcast(new ComponentLifecycleMsg(apiUsageState.getTenantId(), apiUsageState.getId(), ComponentLifecycleEvent.UPDATED));
    }

    @Override
    public void onDeviceProfileDelete(DeviceProfile entity, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(entity.getTenantId(), entity.getId(), entity.getName(), callback);
    }

    @Override
    public void onTenantProfileDelete(TenantProfile entity, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(TenantId.SYS_TENANT_ID, entity.getId(), entity.getName(), callback);
    }

    @Override
    public void onTenantDelete(Tenant entity, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(TenantId.SYS_TENANT_ID, entity.getId(), entity.getName(), callback);
    }

    @Override
    public void onDeviceDeleted(Device device, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(device.getTenantId(), device.getId(), device.getName(), callback);
        sendDeviceStateServiceEvent(device.getTenantId(), device.getId(), false, false, true);
        broadcastEntityStateChangeEvent(device.getTenantId(), device.getId(), ComponentLifecycleEvent.DELETED);
    }

    @Override
    public void onResourceChange(TbResource resource, TbQueueCallback callback) {
        TenantId tenantId = resource.getTenantId();
        log.trace("[{}][{}][{}] Processing change resource", tenantId, resource.getResourceType(), resource.getResourceKey());
        TransportProtos.ResourceUpdateMsg resourceUpdateMsg = TransportProtos.ResourceUpdateMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setResourceType(resource.getResourceType().name())
                .setResourceKey(resource.getResourceKey())
                .build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setResourceUpdateMsg(resourceUpdateMsg).build();
        broadcast(transportMsg, callback);
    }

    @Override
    public void onResourceDeleted(TbResource resource, TbQueueCallback callback) {
        log.trace("[{}] Processing delete resource", resource);
        TransportProtos.ResourceDeleteMsg resourceUpdateMsg = TransportProtos.ResourceDeleteMsg.newBuilder()
                .setTenantIdMSB(resource.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(resource.getTenantId().getId().getLeastSignificantBits())
                .setResourceType(resource.getResourceType().name())
                .setResourceKey(resource.getResourceKey())
                .build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setResourceDeleteMsg(resourceUpdateMsg).build();
        broadcast(transportMsg, callback);
    }

    public <T> void broadcastEntityChangeToTransport(TenantId tenantId, EntityId entityid, T entity, TbQueueCallback callback) {
        String entityName = (entity instanceof HasName) ? ((HasName) entity).getName() : entity.getClass().getName();
        log.trace("[{}][{}][{}] Processing [{}] change event", tenantId, entityid.getEntityType(), entityid.getId(), entityName);
        TransportProtos.EntityUpdateMsg entityUpdateMsg = TransportProtos.EntityUpdateMsg.newBuilder()
                .setEntityType(entityid.getEntityType().name())
                .setData(ByteString.copyFrom(encodingService.encode(entity))).build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setEntityUpdateMsg(entityUpdateMsg).build();
        broadcast(transportMsg, callback);
    }

    private void broadcastEntityDeleteToTransport(TenantId tenantId, EntityId entityId, String name, TbQueueCallback callback) {
        log.trace("[{}][{}][{}] Processing [{}] delete event", tenantId, entityId.getEntityType(), entityId.getId(), name);
        TransportProtos.EntityDeleteMsg entityDeleteMsg = TransportProtos.EntityDeleteMsg.newBuilder()
                .setEntityType(entityId.getEntityType().name())
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setEntityDeleteMsg(entityDeleteMsg).build();
        broadcast(transportMsg, callback);
    }

    private void broadcast(ToTransportMsg transportMsg, TbQueueCallback callback) {
        TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> toTransportNfProducer = producerProvider.getTransportNotificationsMsgProducer();
        Set<String> tbTransportServices = partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT);
        TbQueueCallback proxyCallback = callback != null ? new MultipleTbQueueCallbackWrapper(tbTransportServices.size(), callback) : null;
        for (String transportServiceId : tbTransportServices) {
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transportServiceId);
            toTransportNfProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), transportMsg), proxyCallback);
            toTransportNfs.incrementAndGet();
        }
    }

    @Override
    public void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId) {
        log.trace("[{}] Processing edge {} event update ", tenantId, edgeId);
        EdgeEventUpdateMsg msg = new EdgeEventUpdateMsg(tenantId, edgeId);
        byte[] msgBytes = encodingService.encode(msg);
        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setEdgeEventUpdateMsg(ByteString.copyFrom(msgBytes)).build();
        pushEdgeSyncMsgToCore(edgeId, toCoreMsg);
    }

    @Override
    public void pushEdgeSyncRequestToCore(ToEdgeSyncRequest toEdgeSyncRequest) {
        log.trace("[{}] Processing edge sync request {} ", toEdgeSyncRequest.getTenantId(), toEdgeSyncRequest);
        byte[] msgBytes = encodingService.encode(toEdgeSyncRequest);
        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setToEdgeSyncRequestMsg(ByteString.copyFrom(msgBytes)).build();
        pushEdgeSyncMsgToCore(toEdgeSyncRequest.getEdgeId(), toCoreMsg);
    }

    @Override
    public void pushEdgeSyncResponseToCore(FromEdgeSyncResponse fromEdgeSyncResponse) {
        log.trace("[{}] Processing edge sync response {}", fromEdgeSyncResponse.getTenantId(), fromEdgeSyncResponse);
        byte[] msgBytes = encodingService.encode(fromEdgeSyncResponse);
        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setFromEdgeSyncResponseMsg(ByteString.copyFrom(msgBytes)).build();
        pushEdgeSyncMsgToCore(fromEdgeSyncResponse.getEdgeId(), toCoreMsg);
    }

    private void pushEdgeSyncMsgToCore(EdgeId edgeId, ToCoreNotificationMsg toCoreMsg) {
        TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNfProducer = producerProvider.getTbCoreNotificationsMsgProducer();
        Set<String> tbCoreServices = partitionService.getAllServiceIds(ServiceType.TB_CORE);
        for (String serviceId : tbCoreServices) {
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
            toCoreNfProducer.send(tpi, new TbProtoQueueMsg<>(edgeId.getId(), toCoreMsg), null);
            toCoreNfs.incrementAndGet();
        }
    }

    private void broadcast(ComponentLifecycleMsg msg) {
        byte[] msgBytes = encodingService.encode(msg);
        TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> toRuleEngineProducer = producerProvider.getRuleEngineNotificationsMsgProducer();
        Set<String> tbRuleEngineServices = partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE);
        EntityType entityType = msg.getEntityId().getEntityType();

        boolean toIntegrationExecutor = entityType.equals(EntityType.CONVERTER) || entityType.equals(EntityType.INTEGRATION);

        if (entityType.equals(EntityType.TENANT) || toIntegrationExecutor) {
            TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> toIeNfProducer = producerProvider.getTbIntegrationExecutorNotificationsMsgProducer();
            Set<String> tbIeServices = partitionService.getAllServiceIds(ServiceType.TB_INTEGRATION_EXECUTOR);
            for (String serviceId : tbIeServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_INTEGRATION_EXECUTOR, serviceId);
                ToIntegrationExecutorNotificationMsg toIeMsg = ToIntegrationExecutorNotificationMsg.newBuilder().setComponentLifecycleMsg(ByteString.copyFrom(msgBytes)).build();
                toIeNfProducer.send(tpi, new TbProtoQueueMsg<>(msg.getEntityId().getId(), toIeMsg), null);
                toIntegrationExecutorNfs.incrementAndGet();
            }
        }

        boolean toCore = entityType.equals(EntityType.TENANT) ||
                entityType.equals(EntityType.TENANT_PROFILE) ||
                entityType.equals(EntityType.DEVICE_PROFILE) ||
                entityType.equals(EntityType.ASSET_PROFILE) ||
                entityType.equals(EntityType.API_USAGE_STATE) ||
                (entityType.equals(EntityType.DEVICE) && msg.getEvent() == ComponentLifecycleEvent.UPDATED) ||
                entityType.equals(EntityType.ENTITY_VIEW) ||
                msg.getEntityId().getEntityType().equals(EntityType.EDGE);

        boolean toRuleEngine = !toIntegrationExecutor;

        if (toCore) {
            TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNfProducer = producerProvider.getTbCoreNotificationsMsgProducer();
            Set<String> tbCoreServices = partitionService.getAllServiceIds(ServiceType.TB_CORE);
            for (String serviceId : tbCoreServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setComponentLifecycleMsg(ByteString.copyFrom(msgBytes)).build();
                toCoreNfProducer.send(tpi, new TbProtoQueueMsg<>(msg.getEntityId().getId(), toCoreMsg), null);
                toCoreNfs.incrementAndGet();
            }
            // No need to push notifications twice
            tbRuleEngineServices.removeAll(tbCoreServices);
        }
        if (toRuleEngine) {
            for (String serviceId : tbRuleEngineServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceId);
                ToRuleEngineNotificationMsg toRuleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setComponentLifecycleMsg(ByteString.copyFrom(msgBytes)).build();
                toRuleEngineProducer.send(tpi, new TbProtoQueueMsg<>(msg.getEntityId().getId(), toRuleEngineMsg), null);
                toRuleEngineNfs.incrementAndGet();
            }
        }
    }

    @Scheduled(fixedDelayString = "${cluster.stats.print_interval_ms}")
    public void printStats() {
        if (statsEnabled) {
            int toCoreMsgCnt = toCoreMsgs.getAndSet(0);
            int toCoreNfsCnt = toCoreNfs.getAndSet(0);
            int toIeNfsCnt = toIntegrationExecutorNfs.getAndSet(0);
            int toRuleEngineMsgsCnt = toRuleEngineMsgs.getAndSet(0);
            int toRuleEngineNfsCnt = toRuleEngineNfs.getAndSet(0);
            int toTransportNfsCnt = toTransportNfs.getAndSet(0);
            if (toCoreMsgCnt > 0 || toCoreNfsCnt > 0 || toIeNfsCnt > 0 || toRuleEngineMsgsCnt > 0 || toRuleEngineNfsCnt > 0 || toTransportNfsCnt > 0) {
                log.info("To TbCore: [{}] messages [{}] notifications; To TbRuleEngine: [{}] messages [{}] notifications; To Transport: [{}] notifications; To Integration Executor: [{}] notifications",
                        toCoreMsgCnt, toCoreNfsCnt, toRuleEngineMsgsCnt, toRuleEngineNfsCnt, toTransportNfsCnt, toIeNfsCnt);
            }
        }
    }

    private void sendDeviceStateServiceEvent(TenantId tenantId, DeviceId deviceId, boolean added, boolean updated, boolean deleted) {
        TransportProtos.DeviceStateServiceMsgProto.Builder builder = TransportProtos.DeviceStateServiceMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setDeviceIdMSB(deviceId.getId().getMostSignificantBits());
        builder.setDeviceIdLSB(deviceId.getId().getLeastSignificantBits());
        builder.setAdded(added);
        builder.setUpdated(updated);
        builder.setDeleted(deleted);
        TransportProtos.DeviceStateServiceMsgProto msg = builder.build();
        pushMsgToCore(tenantId, deviceId, TransportProtos.ToCoreMsg.newBuilder().setDeviceStateServiceMsg(msg).build(), null);
    }

    @Override
    public void onDeviceUpdated(Device device, Device old) {
        onDeviceUpdated(device, old, true);
    }

    @Override
    public void onDeviceUpdated(Device device, Device old, boolean notifyEdge) {
        var created = old == null;
        broadcastEntityChangeToTransport(device.getTenantId(), device.getId(), device, null);
        if (old != null) {
            boolean deviceNameChanged = !device.getName().equals(old.getName());
            if (deviceNameChanged) {
                gatewayNotificationsService.onDeviceUpdated(device, old);
            }
            if (deviceNameChanged || !device.getType().equals(old.getType())) {
                pushMsgToCore(new DeviceNameOrTypeUpdateMsg(device.getTenantId(), device.getId(), device.getName(), device.getType()), null);
            }
        }
        broadcastEntityStateChangeEvent(device.getTenantId(), device.getId(), created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        sendDeviceStateServiceEvent(device.getTenantId(), device.getId(), created, !created, false);
        otaPackageStateService.update(device);
        if (!created && notifyEdge) {
            sendNotificationMsgToEdge(device.getTenantId(), null, device.getId(), null, null, EdgeEventActionType.UPDATED);
        }
    }

    @Override
    public void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body,
                                          EdgeEventType type, EdgeEventActionType action) {
        sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, type, action, null, null);
    }

    @Override
    public void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body,
                                          EdgeEventType type, EdgeEventActionType action,
                                          EntityType entityGroupType, EntityGroupId entityGroupId) {
        if (!edgesEnabled) {
            return;
        }
        if (type == null) {
            if (entityId != null) {
                type = EdgeUtils.getEdgeEventTypeByEntityType(entityId.getEntityType());
            } else {
                log.trace("[{}] entity id and type are null. Ignoring this notification", tenantId);
                return;
            }
            if (type == null) {
                log.trace("[{}] edge event type is null. Ignoring this notification [{}]", tenantId, entityId);
                return;
            }
        }
        TransportProtos.EdgeNotificationMsgProto.Builder builder = TransportProtos.EdgeNotificationMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setType(type.name());
        builder.setAction(action.name());
        if (entityId != null) {
            builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
            builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
            builder.setEntityType(entityId.getEntityType().name());
        }
        if (edgeId != null) {
            builder.setEdgeIdMSB(edgeId.getId().getMostSignificantBits());
            builder.setEdgeIdLSB(edgeId.getId().getLeastSignificantBits());
        }
        if (body != null) {
            builder.setBody(body);
        }
        if (entityGroupType != null) {
            builder.setEntityGroupType(entityGroupType.name());
        }
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits());
            builder.setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        TransportProtos.EdgeNotificationMsgProto msg = builder.build();
        log.trace("[{}] sending notification to edge service {}", tenantId.getId(), msg);
        pushMsgToCore(tenantId, entityId != null ? entityId : tenantId, TransportProtos.ToCoreMsg.newBuilder().setEdgeNotificationMsg(msg).build(), null);

        if (entityId != null && (EntityType.DEVICE.equals(entityGroupType) || EntityType.DEVICE.equals(entityId.getEntityType()))) {
            pushDeviceUpdateMessage(tenantId, edgeId, entityId, action, entityGroupType, entityGroupId);
        }
    }

    private void pushDeviceUpdateMessage(TenantId tenantId, EdgeId edgeId, EntityId entityId, EdgeEventActionType action,
                                         EntityType entityGroupType, EntityGroupId entityGroupId) {
        switch (entityId.getEntityType()) {
            case ENTITY_GROUP:
                if (EntityType.DEVICE.equals(entityGroupType)) {
                    switch (action) {
                        case ASSIGNED_TO_EDGE:
                            pushDeviceUpdateMessageByEntityGroupId(tenantId, new EntityGroupId(entityId.getId()), edgeId);
                            break;
                        case UNASSIGNED_FROM_EDGE:
                            pushDeviceUpdateMessageByEntityGroupId(tenantId, new EntityGroupId(entityId.getId()), null);
                            break;
                    }
                }
                break;
            case DEVICE:
                switch (action) {
                    case ADDED_TO_ENTITY_GROUP:
                    case REMOVED_FROM_ENTITY_GROUP:
                        EdgeId relatedEdgeId = findRelatedEdgeIdIfAny(tenantId, entityGroupId);
                        log.trace("{} Going to send edge update notification for device actor, device id {}, edge id {}", tenantId, entityId, relatedEdgeId);
                        pushMsgToCore(new DeviceEdgeUpdateMsg(tenantId, new DeviceId(entityId.getId()), relatedEdgeId), null);
                        break;
                }
        }
    }

    private void pushDeviceUpdateMessageByEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, EdgeId edgeId) {
        ListenableFuture<List<EntityId>> entityIdsFuture = entityGroupService.findAllEntityIds(tenantId, entityGroupId, new PageLink(Integer.MAX_VALUE));
        Futures.addCallback(entityIdsFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<EntityId> entityIds) {
                if (entityIds != null && !entityIds.isEmpty()) {
                    for (EntityId entityId : entityIds) {
                        log.trace("{} Going to send edge update notification for device actor, device id {}, edge id {}", tenantId, entityId, edgeId);
                        pushMsgToCore(new DeviceEdgeUpdateMsg(tenantId, new DeviceId(entityId.getId()), edgeId), null);
                    }
                } else {
                    log.trace("{} No entities found for the provided entity group {}", tenantId, entityGroupId);
                }
            }

            @Override
            public void onFailure(Throwable th) {
                log.error("[{}] Failed to find all entity ids, entityGroupId = {}", tenantId, entityGroupId, th);
            }
        }, dbCallbackExecutor);
    }

    private EdgeId findRelatedEdgeIdIfAny(TenantId tenantId, EntityGroupId entityGroupId) {
        PageLink pageLink = new PageLink(1);
        PageData<EdgeId> pageData;
        pageData = edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(tenantId, Collections.singletonList(entityGroupId), EntityType.DEVICE, pageLink);
        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
            return pageData.getData().get(0);
        } else {
            return null;
        }
    }

    private static FromDeviceRPCResponseProto.Builder prepareRPCResponseProto(FromDeviceRpcResponse response) {
        FromDeviceRPCResponseProto.Builder builder = FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(response.getId().getMostSignificantBits())
                .setRequestIdLSB(response.getId().getLeastSignificantBits())
                .setError(response.getError().isPresent() ? response.getError().get().ordinal() : -1);
        response.getResponse().ifPresent(builder::setResponse);
        return builder;
    }

    @Override
    public void onQueueChange(Queue queue) {
        log.trace("[{}][{}] Processing queue change [{}] event", queue.getTenantId(), queue.getId(), queue.getName());

        TransportProtos.QueueUpdateMsg queueUpdateMsg = TransportProtos.QueueUpdateMsg.newBuilder()
                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                .setQueueIdMSB(queue.getId().getId().getMostSignificantBits())
                .setQueueIdLSB(queue.getId().getId().getLeastSignificantBits())
                .setQueueName(queue.getName())
                .setQueueTopic(queue.getTopic())
                .setPartitions(queue.getPartitions())
                .build();

        ToRuleEngineNotificationMsg ruleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setQueueUpdateMsg(queueUpdateMsg).build();
        ToCoreNotificationMsg coreMsg = ToCoreNotificationMsg.newBuilder().setQueueUpdateMsg(queueUpdateMsg).build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setQueueUpdateMsg(queueUpdateMsg).build();
        doSendQueueNotifications(ruleEngineMsg, coreMsg, transportMsg);
    }

    @Override
    public void onQueueDelete(Queue queue) {
        log.trace("[{}][{}] Processing queue delete [{}] event", queue.getTenantId(), queue.getId(), queue.getName());

        TransportProtos.QueueDeleteMsg queueDeleteMsg = TransportProtos.QueueDeleteMsg.newBuilder()
                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                .setQueueIdMSB(queue.getId().getId().getMostSignificantBits())
                .setQueueIdLSB(queue.getId().getId().getLeastSignificantBits())
                .setQueueName(queue.getName())
                .build();

        ToRuleEngineNotificationMsg ruleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setQueueDeleteMsg(queueDeleteMsg).build();
        ToCoreNotificationMsg coreMsg = ToCoreNotificationMsg.newBuilder().setQueueDeleteMsg(queueDeleteMsg).build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setQueueDeleteMsg(queueDeleteMsg).build();
        doSendQueueNotifications(ruleEngineMsg, coreMsg, transportMsg);
    }

    private void doSendQueueNotifications(ToRuleEngineNotificationMsg ruleEngineMsg, ToCoreNotificationMsg coreMsg, ToTransportMsg transportMsg) {
        Set<TransportProtos.ServiceInfo> tbRuleEngineServices = partitionService.getAllServices(ServiceType.TB_RULE_ENGINE);
        for (TransportProtos.ServiceInfo ruleEngineService : tbRuleEngineServices) {
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngineService.getServiceId());
            producerProvider.getRuleEngineNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), ruleEngineMsg), null);
            toRuleEngineNfs.incrementAndGet();
        }
        if (!serviceType.equals("monolith")) {
            Set<TransportProtos.ServiceInfo> tbCoreServices = partitionService.getAllServices(ServiceType.TB_CORE);
            for (TransportProtos.ServiceInfo coreService : tbCoreServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, coreService.getServiceId());
                producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), coreMsg), null);
                toCoreNfs.incrementAndGet();
            }
            Set<TransportProtos.ServiceInfo> tbTransportServices = partitionService.getAllServices(ServiceType.TB_TRANSPORT);
            for (TransportProtos.ServiceInfo transportService : tbTransportServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transportService.getServiceId());
                producerProvider.getTransportNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), transportMsg), null);
                toTransportNfs.incrementAndGet();
            }
        }
    }
}
