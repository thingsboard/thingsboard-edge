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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.thingsboard.server.common.msg.queue.TopicPartitionInfo.withTopic;

@Slf4j
public abstract class QueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> {

    protected final PartitionedQueueConsumerManager<E> eventConsumer;

    @Getter
    protected final Map<QueueKey, Set<TopicPartitionInfo>> partitions = new HashMap<>();
    protected final Set<TopicPartitionInfo> partitionsInProgress = ConcurrentHashMap.newKeySet();
    protected boolean initialized;

    protected final ReadWriteLock partitionsLock = new ReentrantReadWriteLock();

    protected QueueStateService(PartitionedQueueConsumerManager<E> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    public void update(QueueKey queueKey, Set<TopicPartitionInfo> newPartitions) {
        newPartitions = withTopic(newPartitions, eventConsumer.getTopic());
        var writeLock = partitionsLock.writeLock();
        writeLock.lock();
        Set<TopicPartitionInfo> oldPartitions = this.partitions.getOrDefault(queueKey, Collections.emptySet());
        Set<TopicPartitionInfo> addedPartitions;
        Set<TopicPartitionInfo> removedPartitions;
        try {
            addedPartitions = new HashSet<>(newPartitions);
            addedPartitions.removeAll(oldPartitions);
            removedPartitions = new HashSet<>(oldPartitions);
            removedPartitions.removeAll(newPartitions);
            this.partitions.put(queueKey, newPartitions);
        } finally {
            writeLock.unlock();
        }

        if (!removedPartitions.isEmpty()) {
            removePartitions(queueKey, removedPartitions);
        }

        if (!addedPartitions.isEmpty()) {
            addPartitions(queueKey, addedPartitions);
        }
        initialized = true;
    }

    protected void addPartitions(QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        eventConsumer.addPartitions(partitions);
    }

    protected void removePartitions(QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        eventConsumer.removePartitions(partitions);
    }

    public void delete(Set<TopicPartitionInfo> partitions) {
        if (partitions.isEmpty()) {
            return;
        }
        var writeLock = partitionsLock.writeLock();
        writeLock.lock();
        try {
            this.partitions.values().forEach(tpis -> tpis.removeAll(partitions));
        } finally {
            writeLock.unlock();
        }
        deletePartitions(partitions);
    }

    protected void deletePartitions(Set<TopicPartitionInfo> partitions) {
        eventConsumer.delete(withTopic(partitions, eventConsumer.getTopic()));
    }

    public Set<TopicPartitionInfo> getPartitionsInProgress() {
        return initialized ? partitionsInProgress : null;
    }

    public void stop() {
        eventConsumer.stop();
        eventConsumer.awaitStop();
    }

}
