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
package org.thingsboard.server.service.housekeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.HousekeeperTaskProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@TbCoreComponent
@Service
@Slf4j
@ConditionalOnProperty(name = "queue.core.housekeeper.enabled", havingValue = "true", matchIfMissing = true)
public class HousekeeperReprocessingService {

    private final HousekeeperService housekeeperService;
    private final PartitionService partitionService;
    private final TbCoreQueueFactory queueFactory;
    private TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> producer;
    private TopicPartitionInfo submitTpi;

    @Value("${queue.core.housekeeper.reprocessing-start-delay-sec:300}")
    private int startDelay;
    @Value("${queue.core.housekeeper.task-reprocessing-delay-sec:3600}")
    private int reprocessingDelay;
    @Value("${queue.core.housekeeper.max-reprocessing-attempts:10}")
    private int maxReprocessingAttempts;
    @Value("${queue.core.housekeeper.poll-interval-ms:500}")
    private int pollInterval;

    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-reprocessing-consumer"));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("housekeeper-reprocessing-scheduler"));

    protected AtomicInteger cycle = new AtomicInteger();
    private boolean stopped;

    public HousekeeperReprocessingService(@Lazy HousekeeperService housekeeperService,
                                          PartitionService partitionService,
                                          TbCoreQueueFactory queueFactory) {
        this.housekeeperService = housekeeperService;
        this.partitionService = partitionService;
        this.queueFactory = queueFactory;
    }

    @PostConstruct
    private void init() {
        producer = queueFactory.createHousekeeperReprocessingMsgProducer();
        submitTpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                cycle.incrementAndGet();
                startReprocessing();
            } catch (Throwable e) {
                log.error("Unexpected error during reprocessing", e);
            }
        }, startDelay, reprocessingDelay, TimeUnit.SECONDS);
    }

    public void startReprocessing() {
        if (!partitionService.isMyPartition(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)) {
            return;
        }

        var consumer = queueFactory.createHousekeeperReprocessingMsgConsumer();
        consumer.subscribe();
        consumerExecutor.submit(() -> {
            log.info("Starting Housekeeper tasks reprocessing");
            long startTs = System.currentTimeMillis();
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToHousekeeperServiceMsg>> msgs = consumer.poll(pollInterval);
                    if (msgs.isEmpty() || msgs.stream().anyMatch(msg -> msg.getValue().getTask().getTs() >= startTs)) {
                        // it's not time yet to process the message
                        if (!consumer.isCommitSupported()) {
                            // resubmitting consumed messages if committing is not supported (for in-memory queue)
                            for (TbProtoQueueMsg<ToHousekeeperServiceMsg> msg : msgs) {
                                submit(msg.getKey(), msg.getValue());
                            }
                        }
                        break;
                    }

                    for (TbProtoQueueMsg<ToHousekeeperServiceMsg> msg : msgs) {
                        log.trace("Reprocessing task: {}", msg);
                        try {
                            reprocessTask(msg.getValue());
                        } catch (InterruptedException e) {
                            return;
                        } catch (Throwable e) {
                            log.error("Unexpected error during message reprocessing [{}]", msg, e);
                            submitForReprocessing(msg.getValue(), e);
                        }
                    }
                    consumer.commit();
                } catch (Throwable t) {
                    if (!consumer.isStopped()) {
                        log.warn("Failed to process messages from queue", t);
                        try {
                            Thread.sleep(pollInterval);
                        } catch (InterruptedException interruptedException) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", interruptedException);
                        }
                    }
                }
            }
            consumer.unsubscribe();
            log.info("Stopped Housekeeper tasks reprocessing");
        });
    }

    private void reprocessTask(ToHousekeeperServiceMsg msg) throws Exception {
        int attempt = msg.getTask().getAttempt();
        if (attempt > maxReprocessingAttempts) {
            if (cycle.get() == 1) { // only reprocessing tasks with exceeded failures on first cycle (after start-up)
                log.info("Trying to reprocess task with {} failed attempts: {}", attempt, msg);
            } else {
                // resubmitting msg to be processed on the next service start
                msg = msg.toBuilder()
                        .setTask(msg.getTask().toBuilder()
                                .setTs(getReprocessingTs())
                                .build())
                        .build();
                submit(UUID.randomUUID(), msg);
                return;
            }
        }

        housekeeperService.processTask(msg);
    }

    public void submitForReprocessing(ToHousekeeperServiceMsg msg, Throwable error) {
        HousekeeperTaskProto task = msg.getTask();

        int attempt = task.getAttempt() + 1;
        Set<String> errors = new LinkedHashSet<>(task.getErrorsList());
        errors.add(StringUtils.truncate(ExceptionUtils.getStackTrace(error), 1024));
        msg = msg.toBuilder()
                .setTask(task.toBuilder()
                        .setAttempt(attempt)
                        .clearErrors().addAllErrors(errors)
                        .setTs(getReprocessingTs())
                        .build())
                .build();

        log.trace("Submitting for reprocessing: {}", msg);
        submit(UUID.randomUUID(), msg); // reprocessing topic has single partition, so we don't care about the msg key

        if (task.getAttempt() >= maxReprocessingAttempts) {
            log.warn("Failed to process task in {} attempts: {}", task.getAttempt(), msg);
        }
    }

    private void submit(UUID key, ToHousekeeperServiceMsg msg) {
        producer.send(submitTpi, new TbProtoQueueMsg<>(key, msg), null);
    }

    private long getReprocessingTs() {
        return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis((long) (reprocessingDelay * 0.8)); // *0.8 so that msgs submitted just after finishing reprocessing are processed on the next cycle
    }

    @PreDestroy
    private void stop() throws Exception {
        stopped = true;
        scheduler.shutdownNow();
        consumerExecutor.shutdown();
        if (!consumerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            consumerExecutor.shutdownNow();
        }
        log.info("Stopped Housekeeper reprocessing service");
    }

}
