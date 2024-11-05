/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.id.EntityGroupId;
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
    private static final String METHOD_CANNOT_BE_USED_FOR_THIS_SERVICE = "Method cannot be used for this service";
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
                               JsonNode entityBody, EntityGroupId entityGroupId, Long queueStartTs) throws ExecutionException, InterruptedException {

        saveCloudEventAsync(tenantId, cloudEventType, cloudEventAction, entityId, entityBody, entityGroupId, queueStartTs).get();
    }

    @Override
    public ListenableFuture<Void> saveCloudEventAsync(TenantId tenantId, CloudEventType cloudEventType,
                                                      EdgeEventActionType cloudEventAction, EntityId entityId,
                                                      JsonNode entityBody, EntityGroupId entityGroupId, Long queueStartTs) {
        CloudEvent cloudEvent = new CloudEvent(
                tenantId,
                cloudEventAction,
                entityId != null ? entityId.getId() : null,
                cloudEventType,
                entityBody,
                entityGroupId != null ? entityGroupId.getId() : null
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

    @Override
    public void commit(boolean isTS) {
        chooseConsumer(isTS).commit();
    }

    private TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> chooseConsumer(boolean isTS) {
        return isTS ? tbCloudEventProvider.getCloudEventTSMsgConsumer() : tbCloudEventProvider.getCloudEventMsgConsumer();
    }

    @Override
    public void cleanupEvents(long ttl) {
        throw new UnsupportedOperationException(METHOD_CANNOT_BE_USED_FOR_THIS_SERVICE);
    }

}
