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
package org.thingsboard.server.queue.common.consumer;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class MainQueueConsumerManager<M extends TbQueueMsg, C extends QueueConfig> {

    @Getter
    protected final QueueKey queueKey;
    @Getter
    protected C config;
    protected final MsgPackProcessor<M, C> msgPackProcessor;
    protected final BiFunction<C, Integer, TbQueueConsumer<M>> consumerCreator;
    protected final ExecutorService consumerExecutor;
    protected final ScheduledExecutorService scheduler;
    protected final ExecutorService taskExecutor;
    protected final Consumer<Throwable> uncaughtErrorHandler;

    private final java.util.Queue<TbQueueConsumerManagerTask> tasks = new ConcurrentLinkedQueue<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Getter
    private volatile Set<TopicPartitionInfo> partitions;
    protected volatile ConsumerWrapper<M> consumerWrapper;
    protected volatile boolean stopped;

    @Builder
    public MainQueueConsumerManager(QueueKey queueKey, C config,
                                    MsgPackProcessor<M, C> msgPackProcessor,
                                    BiFunction<C, Integer, TbQueueConsumer<M>> consumerCreator,
                                    ExecutorService consumerExecutor,
                                    ScheduledExecutorService scheduler,
                                    ExecutorService taskExecutor,
                                    Consumer<Throwable> uncaughtErrorHandler) {
        this.queueKey = queueKey;
        this.config = config;
        this.msgPackProcessor = msgPackProcessor;
        this.consumerCreator = consumerCreator;
        this.consumerExecutor = consumerExecutor;
        this.scheduler = scheduler;
        this.taskExecutor = taskExecutor;
        this.uncaughtErrorHandler = uncaughtErrorHandler;
        if (config != null) {
            init(config);
        }
    }

    public void init(C config) {
        this.config = config;
        if (config.isConsumerPerPartition()) {
            this.consumerWrapper = new ConsumerPerPartitionWrapper();
        } else {
            this.consumerWrapper = new SingleConsumerWrapper();
        }
        log.debug("[{}] Initialized consumer for queue: {}", queueKey, config);
    }

    public void update(C config) {
        addTask(TbQueueConsumerManagerTask.configUpdate(config));
    }

    public void update(Set<TopicPartitionInfo> partitions) {
        addTask(TbQueueConsumerManagerTask.partitionChange(partitions));
    }

    protected void addTask(TbQueueConsumerManagerTask todo) {
        if (stopped) {
            return;
        }
        tasks.add(todo);
        log.trace("[{}] Added task: {}", queueKey, todo);
        tryProcessTasks();
    }

    private void tryProcessTasks() {
        taskExecutor.submit(() -> {
            if (lock.tryLock()) {
                try {
                    C newConfig = null;
                    Set<TopicPartitionInfo> newPartitions = null;
                    while (!stopped) {
                        TbQueueConsumerManagerTask task = tasks.poll();
                        if (task == null) {
                            break;
                        }
                        log.trace("[{}] Processing task: {}", queueKey, task);

                        if (task.getEvent() == QueueEvent.PARTITION_CHANGE) {
                            newPartitions = task.getPartitions();
                        } else if (task.getEvent() == QueueEvent.CONFIG_UPDATE) {
                            newConfig = (C) task.getConfig();
                        } else {
                            processTask(task);
                        }
                    }
                    if (stopped) {
                        return;
                    }
                    if (newConfig != null) {
                        doUpdate(newConfig);
                    }
                    if (newPartitions != null) {
                        doUpdate(newPartitions);
                    }
                } catch (Exception e) {
                    log.error("[{}] Failed to process tasks", queueKey, e);
                } finally {
                    lock.unlock();
                }
            } else {
                log.trace("[{}] Failed to acquire lock", queueKey);
                scheduler.schedule(this::tryProcessTasks, 1, TimeUnit.SECONDS);
            }
        });
    }

    protected void processTask(TbQueueConsumerManagerTask task) {
    }

    private void doUpdate(C newConfig) {
        log.info("[{}] Processing queue update: {}", queueKey, newConfig);
        var oldConfig = this.config;
        this.config = newConfig;
        if (log.isTraceEnabled()) {
            log.trace("[{}] Old queue configuration: {}", queueKey, oldConfig);
            log.trace("[{}] New queue configuration: {}", queueKey, newConfig);
        }

        if (oldConfig == null) {
            init(config);
        } else if (newConfig.isConsumerPerPartition() != oldConfig.isConsumerPerPartition()) {
            consumerWrapper.getConsumers().forEach(TbQueueConsumerTask::initiateStop);
            consumerWrapper.getConsumers().forEach(TbQueueConsumerTask::awaitCompletion);

            init(config);
            if (partitions != null) {
                doUpdate(partitions); // even if partitions number was changed, there can be no partition change event
            }
        } else {
            log.trace("[{}] Silently applied new config, because consumer-per-partition not changed", queueKey);
            // do nothing, because partitions change (if they changed) will be handled on PartitionChangeEvent,
            // and changes to other config values will be picked up by consumer on the fly,
            // and queue topic and name are immutable
        }
    }

    public void doUpdate(Set<TopicPartitionInfo> partitions) {
        this.partitions = partitions;
        consumerWrapper.updatePartitions(partitions);
    }

    private void launchConsumer(TbQueueConsumerTask<M> consumerTask) {
        log.info("[{}] Launching consumer", consumerTask.getKey());
        Future<?> consumerLoop = consumerExecutor.submit(() -> {
            ThingsBoardThreadFactory.updateCurrentThreadName(consumerTask.getKey().toString());
            consumerLoop(consumerTask.getConsumer());
            log.info("[{}] Consumer stopped", consumerTask.getKey());
        });
        consumerTask.setTask(consumerLoop);
    }

    private void consumerLoop(TbQueueConsumer<M> consumer) {
        try {
            while (!stopped && !consumer.isStopped()) {
                try {
                    List<M> msgs = consumer.poll(config.getPollInterval());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    processMsgs(msgs, consumer, config);
                } catch (Exception e) {
                    if (!consumer.isStopped()) {
                        log.warn("Failed to process messages from queue", e);
                        try {
                            Thread.sleep(config.getPollInterval());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                        }
                    }
                }
            }
            if (consumer.isStopped()) {
                consumer.unsubscribe();
            }
        } catch (Throwable t) {
            log.error("Failure in consumer loop", t);
            if (uncaughtErrorHandler != null) {
                uncaughtErrorHandler.accept(t);
            }
            consumer.unsubscribe();
        }
    }

    protected void processMsgs(List<M> msgs, TbQueueConsumer<M> consumer, C config) throws Exception {
        msgPackProcessor.process(msgs, consumer, config);
    }

    public void stop() {
        log.debug("[{}] Stopping consumers", queueKey);
        consumerWrapper.getConsumers().forEach(TbQueueConsumerTask::initiateStop);
        stopped = true;
    }

    public void awaitStop() {
        awaitStop(30);
    }

    public void awaitStop(int timeoutSec) {
        log.debug("[{}] Waiting for consumers to stop", queueKey);
        consumerWrapper.getConsumers().forEach(consumerTask -> consumerTask.awaitCompletion(timeoutSec));
        log.debug("[{}] Unsubscribed and stopped consumers", queueKey);
    }

    private static String partitionsToString(Collection<TopicPartitionInfo> partitions) {
        return partitions.stream().map(tpi -> tpi.getFullTopicName() + (tpi.isUseInternalPartition() ?
                        "[" + tpi.getPartition().orElse(-1) + "]" : ""))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public interface MsgPackProcessor<M extends TbQueueMsg, C extends QueueConfig> {
        void process(List<M> msgs, TbQueueConsumer<M> consumer, C config) throws Exception;
    }

    public interface ConsumerWrapper<M extends TbQueueMsg> {

        void updatePartitions(Set<TopicPartitionInfo> partitions);

        Collection<TbQueueConsumerTask<M>> getConsumers();

    }

    class ConsumerPerPartitionWrapper implements ConsumerWrapper<M> {
        private final Map<TopicPartitionInfo, TbQueueConsumerTask<M>> consumers = new HashMap<>();

        @Override
        public void updatePartitions(Set<TopicPartitionInfo> partitions) {
            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(consumers.keySet());

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(consumers.keySet());
            removedPartitions.removeAll(partitions);
            log.info("[{}] Added partitions: {}, removed partitions: {}", queueKey, partitionsToString(addedPartitions), partitionsToString(removedPartitions));

            removedPartitions.forEach((tpi) -> consumers.get(tpi).initiateStop());
            removedPartitions.forEach((tpi) -> consumers.remove(tpi).awaitCompletion());

            addedPartitions.forEach((tpi) -> {
                Integer partitionId = tpi.getPartition().orElse(-1);
                String key = queueKey + "-" + partitionId;
                TbQueueConsumerTask<M> consumer = new TbQueueConsumerTask<>(key, () -> consumerCreator.apply(config, partitionId));
                consumers.put(tpi, consumer);
                consumer.subscribe(Set.of(tpi));
                launchConsumer(consumer);
            });
        }

        @Override
        public Collection<TbQueueConsumerTask<M>> getConsumers() {
            return consumers.values();
        }
    }

    class SingleConsumerWrapper implements ConsumerWrapper<M> {
        private TbQueueConsumerTask<M> consumer;

        @Override
        public void updatePartitions(Set<TopicPartitionInfo> partitions) {
            log.info("[{}] New partitions: {}", queueKey, partitionsToString(partitions));
            if (partitions.isEmpty()) {
                if (consumer != null && consumer.isRunning()) {
                    consumer.initiateStop();
                    consumer.awaitCompletion();
                }
                consumer = null;
                return;
            }

            if (consumer == null) {
                consumer = new TbQueueConsumerTask<>(queueKey, () -> consumerCreator.apply(config, null)); // no partitionId passed
            }
            consumer.subscribe(partitions);
            if (!consumer.isRunning()) {
                launchConsumer(consumer);
            }
        }

        @Override
        public Collection<TbQueueConsumerTask<M>> getConsumers() {
            if (consumer == null) {
                return Collections.emptyList();
            }
            return List.of(consumer);
        }
    }
}
