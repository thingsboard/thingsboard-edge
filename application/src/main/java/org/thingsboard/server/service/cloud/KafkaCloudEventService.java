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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
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
import org.thingsboard.server.dao.edge.stats.CloudStatsCounterService;
import org.thingsboard.server.dao.edge.stats.CloudStatsKey;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCloudEventProvider;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;

@Slf4j
@Service
@Primary
@AllArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCloudEventService implements CloudEventService {

    private final CloudStatsCounterService statsCounterService;
    private final TbCloudEventProvider tbCloudEventProvider;
    private final DataValidator<CloudEvent> cloudEventValidator;

    @Override
    public void saveCloudEvent(TenantId tenantId, CloudEventType cloudEventType,
                               EdgeEventActionType cloudEventAction, EntityId entityId,
                               JsonNode entityBody) throws ExecutionException, InterruptedException {
        saveCloudEventAsync(tenantId, cloudEventType, cloudEventAction, entityId, entityBody).get();
    }

    @Override
    public ListenableFuture<Void> saveCloudEventAsync(TenantId tenantId, CloudEventType cloudEventType,
                                                      EdgeEventActionType cloudEventAction, EntityId entityId,
                                                      JsonNode entityBody) {
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
        return saveCloudEventToTopic(cloudEvent, tbCloudEventProvider.getCloudEventMsgProducer());
    }

    @Override
    public ListenableFuture<Void> saveTsKvAsync(CloudEvent cloudEvent) {
        return saveCloudEventToTopic(cloudEvent, tbCloudEventProvider.getCloudEventTSMsgProducer());
    }

    private ListenableFuture<Void> saveCloudEventToTopic(CloudEvent cloudEvent, TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> producer) {
        cloudEventValidator.validate(cloudEvent, CloudEvent::getTenantId);
        log.trace("Save cloud event {}", cloudEvent);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        saveCloudEventToTopic(cloudEvent, producer, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_ADDED, cloudEvent.getTenantId(), 1);
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to send cloud event", t);
                futureToSet.setException(t);
            }
        });
        return futureToSet;
    }

    private void saveCloudEventToTopic(CloudEvent cloudEvent, TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> producer, TbQueueCallback callback) {
        TopicPartitionInfo tpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();

        ToCloudEventMsg toCloudEventMsg = ToCloudEventMsg.newBuilder()
                .setCloudEventMsg(ProtoUtils.toProto(cloudEvent))
                .build();

        producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), toCloudEventMsg), callback);
    }

    @Override
    public PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public PageData<CloudEvent> findTsKvCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        throw new RuntimeException("Not implemented!");
    }

}
