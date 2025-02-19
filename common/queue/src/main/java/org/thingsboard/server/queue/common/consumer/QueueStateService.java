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

import lombok.Getter;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class QueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> {

    private PartitionedQueueConsumerManager<S> stateConsumer;
    private PartitionedQueueConsumerManager<E> eventConsumer;

    @Getter
    private Set<TopicPartitionInfo> partitions;
    private final Lock lock = new ReentrantLock();

    public void init(PartitionedQueueConsumerManager<S> stateConsumer, PartitionedQueueConsumerManager<E> eventConsumer) {
        this.stateConsumer = stateConsumer;
        this.eventConsumer = eventConsumer;
    }

    public void update(Set<TopicPartitionInfo> newPartitions) {
        lock.lock();
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
            lock.unlock();
        }
        if (!removedPartitions.isEmpty()) {
            stateConsumer.removePartitions(removedPartitions);
            eventConsumer.removePartitions(removedPartitions.stream().map(tpi -> tpi.withTopic(eventConsumer.getTopic())).collect(Collectors.toSet()));
        }

        if (!addedPartitions.isEmpty()) {
            stateConsumer.addPartitions(addedPartitions, partition -> {
                lock.lock();
                try {
                    if (this.partitions.contains(partition)) {
                        eventConsumer.addPartitions(Set.of(partition.withTopic(eventConsumer.getTopic())));
                    }
                } finally {
                    lock.unlock();
                }
            });
        }
    }

}
