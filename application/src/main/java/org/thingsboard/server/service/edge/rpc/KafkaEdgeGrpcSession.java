/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

@Slf4j
public class KafkaEdgeGrpcSession extends EdgeGrpcSession {

    private final TopicService topicService;
    private final TbCoreQueueFactory tbCoreQueueFactory;

    private final TbKafkaSettings kafkaSettings;
    private final TbKafkaTopicConfigs kafkaTopicConfigs;

    private volatile boolean isHighPriorityProcessing;

    @Getter
    private QueueConsumerManager<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer;

    private ExecutorService consumerExecutor;

    public KafkaEdgeGrpcSession(EdgeContextComponent ctx, TopicService topicService, TbCoreQueueFactory tbCoreQueueFactory,
                                TbKafkaSettings kafkaSettings, TbKafkaTopicConfigs kafkaTopicConfigs, StreamObserver<ResponseMsg> outputStream,
                                BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener, BiConsumer<Edge, UUID> sessionCloseListener,
                                ScheduledExecutorService sendDownlinkExecutorService, int maxInboundMessageSize, int maxHighPriorityQueueSizePerSession) {
        super(ctx, outputStream, sessionOpenListener, sessionCloseListener, sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
        this.topicService = topicService;
        this.tbCoreQueueFactory = tbCoreQueueFactory;
        this.kafkaSettings = kafkaSettings;
        this.kafkaTopicConfigs = kafkaTopicConfigs;
    }

    private void processMsgs(List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer) {
        log.trace("[{}][{}] starting processing edge events", tenantId, edge.getId());
        if (!isConnected() || isSyncInProgress() || isHighPriorityProcessing) {
            log.debug("[{}][{}] edge not connected, edge sync is not completed or high priority processing in progress, " +
                            "connected = {}, sync in progress = {}, high priority in progress = {}. Skipping iteration",
                    tenantId, edge.getId(), isConnected(), isSyncInProgress(), isHighPriorityProcessing);
            return;
        }
        List<EdgeEvent> edgeEvents = new ArrayList<>();
        for (TbProtoQueueMsg<ToEdgeEventNotificationMsg> msg : msgs) {
            EdgeEvent edgeEvent = ProtoUtils.fromProto(msg.getValue().getEdgeEventMsg());
            edgeEvents.add(edgeEvent);
        }
        List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(edgeEvents);
        try {
            boolean isInterrupted = sendDownlinkMsgsPack(downlinkMsgsPack).get();
            if (isInterrupted) {
                log.debug("[{}][{}] Send downlink messages task was interrupted", tenantId, edge.getId());
            } else {
                consumer.commit();
            }
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process downlink messages", tenantId, edge.getId(), e);
        }
    }

    @Override
    public ListenableFuture<Boolean> migrateEdgeEvents() throws Exception {
        return super.processEdgeEvents();
    }

    @Override
    public ListenableFuture<Boolean> processEdgeEvents() {
        if (consumer == null || (consumer.getConsumer() != null && consumer.getConsumer().isStopped())) {
            try {
                this.consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-event-consumer"));
                this.consumer = QueueConsumerManager.<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>builder()
                        .name("TB Edge events [" + edge.getId() + "]")
                        .msgPackProcessor(this::processMsgs)
                        .pollInterval(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval())
                        .consumerCreator(() -> tbCoreQueueFactory.createEdgeEventMsgConsumer(tenantId, edge.getId()))
                        .consumerExecutor(consumerExecutor)
                        .threadPrefix("edge-events-" + edge.getId())
                        .build();
                consumer.subscribe();
                consumer.launch();
            } catch (Exception e) {
                destroy();
                log.warn("[{}][{}] Failed to start edge event consumer", sessionId, edge.getId(), e);
            }
        }
        return Futures.immediateFuture(Boolean.FALSE);
    }

    @Override
    public void processHighPriorityEvents() {
        isHighPriorityProcessing = true;
        super.processHighPriorityEvents();
        isHighPriorityProcessing = false;
    }

    @Override
    public void destroy() {
        try {
            if (consumer != null) {
                consumer.stop();
            }
        } finally {
            consumer = null;
        }
        try {
            if (consumerExecutor != null) {
                consumerExecutor.shutdown();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void cleanUp() {
        String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edge.getId()).getTopic();
        TbKafkaAdmin kafkaAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getEdgeEventConfigs());
        kafkaAdmin.deleteTopic(topic);
        kafkaAdmin.deleteConsumerGroup(topic);
    }

}
