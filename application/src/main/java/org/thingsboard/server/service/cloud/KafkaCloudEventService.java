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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCloudEventProvider;
import org.thingsboard.server.queue.settings.TbQueueCloudEventSettings;
import org.thingsboard.server.queue.settings.TbQueueCloudEventTSSettings;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.gen.transport.TransportProtos.EdgeEventMsgProto;
import static org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;

@Service
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCloudEventService implements CloudEventService {
    private final DataValidator<CloudEvent> cloudEventValidator;
    private final TbQueueCloudEventSettings cloudEventSettings;
    private final TbQueueCloudEventTSSettings cloudEventTSSettings;
    private final TbCloudEventProvider tbCloudEventProvider;

    public KafkaCloudEventService(DataValidator<CloudEvent> cloudEventValidator,
                                  TbQueueCloudEventSettings cloudEventSettings,
                                  TbQueueCloudEventTSSettings cloudEventTSSettings,
                                  TbCloudEventProvider tbCloudEventProvider) {
        this.cloudEventValidator = cloudEventValidator;
        this.cloudEventSettings = cloudEventSettings;
        this.cloudEventTSSettings = cloudEventTSSettings;
        this.tbCloudEventProvider = tbCloudEventProvider;
    }

    @Override
    public void saveCloudEvent(TenantId tenantId, CloudEventType cloudEventType,
                               EdgeEventActionType cloudEventAction, EntityId entityId,
                               JsonNode entityBody, Long queueStartTs) throws ExecutionException, InterruptedException {

        saveCloudEventAsync(tenantId, cloudEventType, cloudEventAction, entityId, entityBody, queueStartTs).get();
    }

    @Override
    public ListenableFuture<Void> saveCloudEventAsync(TenantId tenantId, CloudEventType cloudEventType,
                                                      EdgeEventActionType cloudEventAction, EntityId entityId,
                                                      JsonNode entityBody, Long queueStartTs) {
        CloudEvent cloudEvent = new CloudEvent(
                tenantId,
                cloudEventAction,
                entityId != null ? entityId.getId() : null,
                cloudEventType,
                entityBody
        );

        return saveAsync(cloudEvent);
    }

    @Override
    public ListenableFuture<Void> saveAsync(CloudEvent cloudEvent) {
        cloudEventValidator.validate(cloudEvent, CloudEvent::getTenantId);

        return sendCloudEventToTopicAsync(cloudEvent, false);
    }

    @Override
    public ListenableFuture<Void> saveTsKvAsync(CloudEvent cloudEvent) {
        cloudEventValidator.validate(cloudEvent, CloudEvent::getTenantId);

        return sendCloudEventToTopicAsync(cloudEvent, true);
    }

    private ListenableFuture<Void> sendCloudEventToTopicAsync(CloudEvent cloudEvent, boolean isTS) {
        SettableFuture<Void> futureToSet = SettableFuture.create();

        CompletableFuture.runAsync(() -> {
            try {
                sendCloudEventToTopic(cloudEvent, isTS);
                futureToSet.set(null);
            } catch (Exception e) {
                futureToSet.setException(e);
            }
        });

        return futureToSet;
    }

    private void sendCloudEventToTopic(CloudEvent cloudEvent, boolean isTS) {
        TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> producer = chooseProducer(isTS);
        TopicPartitionInfo tpi = new TopicPartitionInfo(producer.getDefaultTopic(), cloudEvent.getTenantId(), 1, true);

        EdgeEventMsgProto cloudEventMsgProto = ProtoUtils.toProto(cloudEvent);
        ToCloudEventMsg toCloudEventMsg = ToCloudEventMsg.newBuilder().setCloudEventMsg(cloudEventMsgProto).build();

        UUID entityId = cloudEvent.getEntityId() == null ? UUID.fromString(cloudEvent.getEntityBody().get("from").get("id").asText()) : cloudEvent.getEntityId();

        TbProtoQueueMsg<ToCloudEventMsg> cloudEventMsg = new TbProtoQueueMsg<>(entityId, toCloudEventMsg);

        producer.send(tpi, cloudEventMsg, null);
    }

    private TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> chooseProducer(boolean isTS) {
        return isTS ? tbCloudEventProvider.getCloudEventTSMsgProducer() : tbCloudEventProvider.getCloudEventMsgProducer();
    }

    @Override
    public PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        return createPageData(tenantId, false, cloudEventSettings.getPollInterval());
    }

    @Override
    public PageData<CloudEvent> findTsKvCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        return createPageData(tenantId, true, cloudEventTSSettings.getPollInterval());
    }

    @NotNull
    private PageData<CloudEvent> createPageData(TenantId tenantId, boolean isTS, long pollInterval) {
        TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> consumer = chooseConsumer(isTS);
        TopicPartitionInfo tpi = new TopicPartitionInfo(consumer.getTopic(), tenantId, 1, true);

        subscribe(consumer, tpi);

        List<TbProtoQueueMsg<ToCloudEventMsg>> cloudMessages = consumer.poll(pollInterval);
        consumer.commit();

        List<CloudEvent> cloudEvents =
                cloudMessages.stream()
                        .map(msg -> ProtoUtils.fromProto(msg.getValue().getCloudEventMsg()))
                        .toList();

        return new PageData<>(cloudEvents, 0, cloudEvents.size(), false);
    }

    private void subscribe(TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> consumer, TopicPartitionInfo tpi) {
        if (!consumer.getFullTopicNames().contains(tpi.getFullTopicName())) {
            consumer.subscribe(Collections.singleton(tpi));
        }
    }

    @Override
    public void unsubscribeConsumers() {
        tbCloudEventProvider.getCloudEventMsgConsumer().unsubscribe();
        tbCloudEventProvider.getCloudEventTSMsgConsumer().unsubscribe();
    }

    private TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> chooseConsumer(boolean isTS) {
        return isTS ? tbCloudEventProvider.getCloudEventTSMsgConsumer() : tbCloudEventProvider.getCloudEventMsgConsumer();
    }

}
