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
package org.thingsboard.server.queue.common.consumer;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.AddPartitionsTask;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.DeletePartitionsTask;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.RemovePartitionsTask;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class PartitionedQueueConsumerManager<M extends TbQueueMsg> extends MainQueueConsumerManager<M, QueueConfig> {

    private final ConsumerPerPartitionWrapper consumerWrapper;
    private final TbQueueAdmin queueAdmin;
    @Getter
    private final String topic;

    @Builder(builderMethodName = "create") // not to conflict with super.builder()
    public PartitionedQueueConsumerManager(QueueKey queueKey, String topic, long pollInterval, MsgPackProcessor<M, QueueConfig> msgPackProcessor,
                                           BiFunction<QueueConfig, Integer, TbQueueConsumer<M>> consumerCreator, TbQueueAdmin queueAdmin,
                                           ExecutorService consumerExecutor, ScheduledExecutorService scheduler,
                                           ExecutorService taskExecutor, Consumer<Throwable> uncaughtErrorHandler) {
        super(queueKey, QueueConfig.of(true, pollInterval), msgPackProcessor, consumerCreator, consumerExecutor, scheduler, taskExecutor, uncaughtErrorHandler);
        this.topic = topic;
        this.consumerWrapper = (ConsumerPerPartitionWrapper) super.consumerWrapper;
        this.queueAdmin = queueAdmin;
    }

    @Override
    protected void processTask(TbQueueConsumerManagerTask task) {
        if (task instanceof AddPartitionsTask addPartitionsTask) {
            log.info("[{}] Added partitions: {}", queueKey, addPartitionsTask.partitions());
            consumerWrapper.addPartitions(addPartitionsTask.partitions(), addPartitionsTask.onStop(), addPartitionsTask.startOffsetProvider());
        } else if (task instanceof RemovePartitionsTask removePartitionsTask) {
            log.info("[{}] Removed partitions: {}", queueKey, removePartitionsTask.partitions());
            consumerWrapper.removePartitions(removePartitionsTask.partitions());
        } else if (task instanceof DeletePartitionsTask deletePartitionsTask) {
            log.info("[{}] Removing partitions and deleting topics: {}", queueKey, deletePartitionsTask.partitions());
            consumerWrapper.removePartitions(deletePartitionsTask.partitions());
            deletePartitionsTask.partitions().forEach(tpi -> {
                String topic = tpi.getFullTopicName();
                try {
                    queueAdmin.deleteTopic(topic);
                } catch (Throwable t) {
                    log.error("Failed to delete topic {}", topic, t);
                }
            });
        }
    }

    public void addPartitions(Set<TopicPartitionInfo> partitions) {
        addPartitions(partitions, null, null);
    }

    public void addPartitions(Set<TopicPartitionInfo> partitions, Consumer<TopicPartitionInfo> onStop, Function<String, Long> startOffsetProvider) {
        addTask(new AddPartitionsTask(partitions, onStop, startOffsetProvider));
    }

    public void removePartitions(Set<TopicPartitionInfo> partitions) {
        addTask(new RemovePartitionsTask(partitions));
    }

    public void delete(Set<TopicPartitionInfo> partitions) {
        addTask(new DeletePartitionsTask(partitions));
    }

}
