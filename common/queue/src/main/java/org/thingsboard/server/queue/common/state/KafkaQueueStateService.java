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
package org.thingsboard.server.queue.common.state;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Set;

import static org.thingsboard.server.common.msg.queue.TopicPartitionInfo.withTopic;

@Slf4j
public class KafkaQueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> extends QueueStateService<E, S> {

    private final PartitionedQueueConsumerManager<S> stateConsumer;

    public KafkaQueueStateService(PartitionedQueueConsumerManager<E> eventConsumer, PartitionedQueueConsumerManager<S> stateConsumer) {
        super(eventConsumer);
        this.stateConsumer = stateConsumer;
    }

    @Override
    protected void addPartitions(QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        Set<TopicPartitionInfo> statePartitions = withTopic(partitions, stateConsumer.getTopic());
        partitionsInProgress.addAll(statePartitions);
        stateConsumer.addPartitions(statePartitions, statePartition -> {
            var readLock = partitionsLock.readLock();
            readLock.lock();
            try {
                partitionsInProgress.remove(statePartition);
                log.info("Finished partition {} (still in progress: {})", statePartition, partitionsInProgress);
                if (partitionsInProgress.isEmpty()) {
                    log.info("All partitions processed");
                }

                TopicPartitionInfo eventPartition = statePartition.withTopic(eventConsumer.getTopic());
                if (this.partitions.get(queueKey).contains(eventPartition)) {
                    eventConsumer.addPartitions(Set.of(eventPartition));
                }
            } finally {
                readLock.unlock();
            }
        });
    }

    @Override
    protected void removePartitions(QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        super.removePartitions(queueKey, partitions);
        stateConsumer.removePartitions(withTopic(partitions, stateConsumer.getTopic()));
    }

    @Override
    protected void deletePartitions(Set<TopicPartitionInfo> partitions) {
        super.deletePartitions(partitions);
        stateConsumer.delete(withTopic(partitions, stateConsumer.getTopic()));
    }

    @Override
    public void stop() {
        super.stop();
        stateConsumer.stop();
        stateConsumer.awaitStop();
    }

}
