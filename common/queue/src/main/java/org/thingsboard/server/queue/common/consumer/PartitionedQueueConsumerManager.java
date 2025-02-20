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
import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.AddPartitionsTask;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.RemovePartitionsTask;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Slf4j
public class PartitionedQueueConsumerManager<M extends TbQueueMsg> extends MainQueueConsumerManager<M, QueueConfig> {

    private final ConsumerPerPartitionWrapper consumerWrapper;
    @Getter
    private final String topic;

    @Builder(builderMethodName = "create") // not to conflict with super.builder()
    public PartitionedQueueConsumerManager(QueueKey queueKey, String topic, long pollInterval, MsgPackProcessor<M, QueueConfig> msgPackProcessor,
                                           BiFunction<QueueConfig, Integer, TbQueueConsumer<M>> consumerCreator,
                                           ExecutorService consumerExecutor, ScheduledExecutorService scheduler,
                                           ExecutorService taskExecutor, Consumer<Throwable> uncaughtErrorHandler) {
        super(queueKey, QueueConfig.of(true, pollInterval), msgPackProcessor, consumerCreator, consumerExecutor, scheduler, taskExecutor, uncaughtErrorHandler);
        this.topic = topic;
        this.consumerWrapper = (ConsumerPerPartitionWrapper) super.consumerWrapper;
    }

    @Override
    protected void processTask(TbQueueConsumerManagerTask task) {
        if (task instanceof AddPartitionsTask addPartitionsTask) {
            log.info("[{}] Added partitions: {}", queueKey, partitionsToString(addPartitionsTask.partitions()));
            consumerWrapper.addPartitions(addPartitionsTask.partitions(), addPartitionsTask.onStop());
        } else if (task instanceof RemovePartitionsTask removePartitionsTask) {
            log.info("[{}] Removed partitions: {}", queueKey, partitionsToString(removePartitionsTask.partitions()));
            consumerWrapper.removePartitions(removePartitionsTask.partitions());
        }
    }

    public void addPartitions(Set<TopicPartitionInfo> partitions) {
        addPartitions(partitions, null);
    }

    public void addPartitions(Set<TopicPartitionInfo> partitions, Consumer<TopicPartitionInfo> onStop) {
        addTask(new AddPartitionsTask(partitions, onStop));
    }

    public void removePartitions(Set<TopicPartitionInfo> partitions) {
        addTask(new RemovePartitionsTask(partitions));
    }

}
