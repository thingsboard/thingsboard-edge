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

import lombok.Getter;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class QueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> {

    private PartitionedQueueConsumerManager<S> stateConsumer;
    private PartitionedQueueConsumerManager<E> eventConsumer;

    @Getter
    private Set<TopicPartitionInfo> partitions;
    private final ReadWriteLock partitionsLock = new ReentrantReadWriteLock();

    public void init(PartitionedQueueConsumerManager<S> stateConsumer, PartitionedQueueConsumerManager<E> eventConsumer) {
        this.stateConsumer = stateConsumer;
        this.eventConsumer = eventConsumer;
    }

    public void update(Set<TopicPartitionInfo> newPartitions) {
        newPartitions = withTopic(newPartitions, stateConsumer.getTopic());
        var writeLock = partitionsLock.writeLock();
        writeLock.lock();
        Set<TopicPartitionInfo> oldPartitions = this.partitions != null ? this.partitions : Collections.emptySet();
        Set<TopicPartitionInfo> addedPartitions;
        Set<TopicPartitionInfo> removedPartitions;
        try {
            addedPartitions = new HashSet<>(newPartitions);
            addedPartitions.removeAll(oldPartitions);
            removedPartitions = new HashSet<>(oldPartitions);
            removedPartitions.removeAll(newPartitions);
            this.partitions = newPartitions;
        } finally {
            writeLock.unlock();
        }

        if (!removedPartitions.isEmpty()) {
            stateConsumer.removePartitions(removedPartitions);
            eventConsumer.removePartitions(withTopic(removedPartitions, eventConsumer.getTopic()));
        }

        if (!addedPartitions.isEmpty()) {
            stateConsumer.addPartitions(addedPartitions, partition -> {
                var readLock = partitionsLock.readLock();
                readLock.lock();
                try {
                    if (this.partitions.contains(partition)) {
                        eventConsumer.addPartitions(Set.of(partition.newByTopic(eventConsumer.getTopic())));
                    }
                } finally {
                    readLock.unlock();
                }
            });
        }
    }

    private Set<TopicPartitionInfo> withTopic(Set<TopicPartitionInfo> partitions, String topic) {
        return partitions.stream().map(tpi -> tpi.newByTopic(topic)).collect(Collectors.toSet());
    }

}
