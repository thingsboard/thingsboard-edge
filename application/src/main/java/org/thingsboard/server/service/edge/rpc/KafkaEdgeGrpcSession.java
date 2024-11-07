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
package org.thingsboard.server.service.edge.rpc;

import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
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
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Slf4j
public class KafkaEdgeGrpcSession extends AbstractEdgeGrpcSession<KafkaEdgeGrpcSession> {

    private ExecutorService consumerExecutor;
    private ScheduledExecutorService highPriorityExecutorService;

    private QueueConsumerManager<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer;

    public KafkaEdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream,
                                BiConsumer<EdgeId, KafkaEdgeGrpcSession> sessionOpenListener,
                                BiConsumer<Edge, UUID> sessionCloseListener,
                                ScheduledExecutorService sendDownlinkExecutorService,
                                int maxInboundMessageSize, int maxHighPriorityQueueSizePerSession) {
        super(ctx, outputStream, sessionOpenListener, sessionCloseListener, sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
    }

    protected void initConsumer(Supplier<TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>> edgeEventsConsumer, long schedulerPoolSize) {
        this.consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-event-consumer"));
        this.highPriorityExecutorService = Executors.newScheduledThreadPool((int) schedulerPoolSize, ThingsBoardThreadFactory.forName("edge-event-high-priority-scheduler"));
        this.consumer = QueueConsumerManager.<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>builder()
                .name("TB Edge events")
                .msgPackProcessor(this::processMsgs)
                .pollInterval(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval())
                .consumerCreator(edgeEventsConsumer)
                .consumerExecutor(consumerExecutor)
                .threadPrefix("edge-events")
                .build();
        scheduleCheckForHighPriorityEvent();
    }

    private void processMsgs(List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumer) {
        log.trace("[{}][{}] starting processing edge events", tenantId, sessionId);
        if (isConnected() && isSyncCompleted()) {

            if (!highPriorityQueue.isEmpty()) {
                processHighPriorityEvents();
            } else {
                List<EdgeEvent> edgeEvents = new ArrayList<>();
                for (TbProtoQueueMsg<ToEdgeEventNotificationMsg> msg : msgs) {
                    EdgeEvent edgeEvent = ProtoUtils.fromProto(msg.getValue().getEdgeEventMsg());
                    edgeEvents.add(edgeEvent);
                }
                List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(edgeEvents);
                try {
                    boolean isInterrupted = sendDownlinkMsgsPack(downlinkMsgsPack).get();
                    if (isInterrupted) {
                        log.debug("[{}][{}][{}] Send downlink messages task was interrupted", tenantId, edge.getId(), sessionId);
                    } else {
                        consumer.commit();
                    }
                } catch (Exception e) {
                    log.error("[{}] Failed to process all downlink messages", sessionId, e);
                }
            }
        } else {
            log.trace("[{}][{}] edge is not connected or sync is not completed. Skipping iteration", tenantId, sessionId);
        }
    }

    private void scheduleCheckForHighPriorityEvent() {
        highPriorityExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (isConnected() && isSyncCompleted()) {
                    if (!highPriorityQueue.isEmpty()) {
                        processHighPriorityEvents();
                    }
                }
            } catch (Exception e) {
                log.error("Error in processing high priority events", e);
            }
        }, 0, ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval() * 3, TimeUnit.MILLISECONDS);
    }

    public void startConsumers() {
        consumer.subscribe();
        consumer.launch();
    }

    @PreDestroy
    private void destroy() {
        consumer.stop();
        consumerExecutor.shutdown();
    }

    public void stopConsumer() {
        consumer.stop();
        consumerExecutor.shutdown();
    }

}
