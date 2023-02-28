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
package org.thingsboard.integration.service.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.TbIntegrationEventProto;
import org.thingsboard.server.gen.integration.TbIntegrationTsDataProto;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

@RequiredArgsConstructor
@Service
@Slf4j
public class DefaultIntegrationApiService implements IntegrationApiService {

    private final StatsFactory statsFactory;
    private final PartitionService partitionService;
    private final TbQueueProducerProvider producerProvider;
    private final ExecutorService callbackExecutor = ThingsBoardExecutors.newWorkStealingPool(4, "integration-uplink-callback");

    protected MessagesStats tbCoreProducerStats;

    @PostConstruct
    public void init() {
        this.tbCoreProducerStats = statsFactory.createMessagesStats(StatsType.CORE.getName() + ".producer");
    }

    @PreDestroy
    public void destroy() {
        callbackExecutor.shutdownNow();
    }

    @Override
    public void sendUplinkData(Integration integration, IntegrationInfoProto proto, DeviceUplinkDataProto data, IntegrationCallback<Void> callback) {
        sendUplinkData(integration, proto, data, (b, d) -> b.setDeviceUplinkProto(d).build(), callback);
    }

    @Override
    public void sendUplinkData(Integration integration, IntegrationInfoProto proto, AssetUplinkDataProto data, IntegrationCallback<Void> callback) {
        sendUplinkData(integration, proto, data, (b, d) -> b.setAssetUplinkProto(d).build(), callback);
    }

    @Override
    public void sendUplinkData(Integration integration, IntegrationInfoProto proto, EntityViewDataProto data, IntegrationCallback<Void> callback) {
        sendUplinkData(integration, proto, data, (b, d) -> b.setEntityViewDataProto(d).build(), callback);
    }

    @Override
    public void sendUplinkData(Integration integration, IntegrationInfoProto proto, TbMsg data, IntegrationCallback<Void> callback) {
        sendUplinkData(integration, proto, data, (b, d) -> b.setCustomTbMsg(TbMsg.toByteString(data)).build(), callback);
    }

    @Override
    public void sendEventData(TenantId tenantId, EntityId entityId, TbIntegrationEventProto data, IntegrationCallback<Void> callback) {
        var producer = producerProvider.getTbCoreIntegrationMsgProducer();
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId).newByTopic(producer.getDefaultTopic());
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Pushing to topic {} message {}", tenantId, entityId, tpi.getFullTopicName(), data);
        }
        tbCoreProducerStats.incrementTotal();
        StatsTbQueueCallback wrappedCallback = new StatsTbQueueCallback(
                callback != null ? new IntegrationTbQueueCallback(callbackExecutor, callback) : null, tbCoreProducerStats);

        var msg = ToCoreIntegrationMsg.newBuilder().setEventProto(data).build();
        producer.send(tpi, new TbProtoQueueMsg<>(entityId.getId(), msg), wrappedCallback);
    }

    @Override
    public void sendTsData(TenantId tenantId, EntityId entityId, TbIntegrationTsDataProto tsData, IntegrationCallback<Void> callback) {
        var producer = producerProvider.getTbCoreIntegrationMsgProducer();
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId).newByTopic(producer.getDefaultTopic());
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Pushing to topic {} message {}", tenantId, entityId, tpi.getFullTopicName(), tsData);
        }
        tbCoreProducerStats.incrementTotal();
        StatsTbQueueCallback wrappedCallback = new StatsTbQueueCallback(
                callback != null ? new IntegrationTbQueueCallback(callbackExecutor, callback) : null, tbCoreProducerStats);

        var msg = ToCoreIntegrationMsg.newBuilder().setTsDataProto(tsData).build();
        producer.send(tpi, new TbProtoQueueMsg<>(entityId.getId(), msg), wrappedCallback);
    }

    public <T> void sendUplinkData(Integration integration, IntegrationInfoProto proto, T data,
                                   BiFunction<ToCoreIntegrationMsg.Builder, T, ToCoreIntegrationMsg> messageConstructor,
                                   IntegrationCallback<Void> callback) {
        var producer = producerProvider.getTbCoreIntegrationMsgProducer();
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, integration.getTenantId(), integration.getId()).newByTopic(producer.getDefaultTopic());
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Pushing to topic {} message {}", integration.getTenantId(), integration.getId(), tpi.getFullTopicName(), data);
        }
        tbCoreProducerStats.incrementTotal();
        StatsTbQueueCallback wrappedCallback = new StatsTbQueueCallback(
                callback != null ? new IntegrationTbQueueCallback(callbackExecutor, callback) : null, tbCoreProducerStats);

        var builder = ToCoreIntegrationMsg.newBuilder().setIntegration(proto);
        var msg = messageConstructor.apply(builder, data);
        producer.send(tpi, new TbProtoQueueMsg<>(integration.getId().getId(), msg), wrappedCallback);
    }

}
