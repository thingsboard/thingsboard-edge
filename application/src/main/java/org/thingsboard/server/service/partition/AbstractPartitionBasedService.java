/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.partition;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

@Slf4j
public abstract class AbstractPartitionBasedService<T extends EntityId> extends TbApplicationEventListener<PartitionChangeEvent> {

    protected final ConcurrentMap<TopicPartitionInfo, Set<T>> partitionedEntities = new ConcurrentHashMap<>();
    protected final ConcurrentMap<TopicPartitionInfo, List<ListenableFuture<?>>> partitionedFetchTasks = new ConcurrentHashMap<>();
    final Queue<Set<TopicPartitionInfo>> subscribeQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    protected PartitionService partitionService;
    protected ListeningScheduledExecutorService scheduledExecutor;

    abstract protected String getServiceName();

    abstract protected String getSchedulerExecutorName();

    abstract protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions);

    abstract protected void cleanupEntityOnPartitionRemoval(T entityId);

    public Set<T> getPartitionedEntities(TopicPartitionInfo tpi) {
        return partitionedEntities.get(tpi);
    }

    protected void init() {
        // Should be always single threaded due to absence of locks.
        scheduledExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName(getSchedulerExecutorName())));
    }

    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    protected void stop() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

    /**
     * DiscoveryService will call this event from the single thread (one-by-one).
     * Events order is guaranteed by DiscoveryService.
     * The only concurrency is expected from the [main] thread on Application started.
     * Async implementation. Locks is not allowed by design.
     * Any locks or delays in this module will affect DiscoveryService and entire system
     */
    @Override
    public void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (getServiceType().equals(partitionChangeEvent.getServiceType())) {
            log.debug("onTbApplicationEvent, processing event: {}", partitionChangeEvent);
            subscribeQueue.add(partitionChangeEvent.getPartitions());
            scheduledExecutor.submit(this::pollInitStateFromDB);
        }
    }

    protected void pollInitStateFromDB() {
        final Set<TopicPartitionInfo> partitions = getLatestPartitions();
        if (partitions == null) {
            log.debug("Nothing to do. Partitions are empty.");
            return;
        }
        initStateFromDB(partitions);
    }

    private void initStateFromDB(Set<TopicPartitionInfo> partitions) {
        try {
            log.info("[{}] CURRENT PARTITIONS: {}", getServiceName(), partitionedEntities.keySet());
            log.info("[{}] NEW PARTITIONS: {}", getServiceName(), partitions);

            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(partitionedEntities.keySet());

            log.info("[{}] ADDED PARTITIONS: {}", getServiceName(), addedPartitions);

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(partitionedEntities.keySet());
            removedPartitions.removeAll(partitions);

            log.info("[{}] REMOVED PARTITIONS: {}", getServiceName(), removedPartitions);

            boolean partitionListChanged = false;
            // We no longer manage current partition of entities;
            for (var partition : removedPartitions) {
                Set<T> entities = partitionedEntities.remove(partition);
                if (entities != null) {
                    entities.forEach(this::cleanupEntityOnPartitionRemoval);
                }
                List<ListenableFuture<?>> fetchTasks = partitionedFetchTasks.remove(partition);
                if (fetchTasks != null) {
                    fetchTasks.forEach(f -> f.cancel(false));
                }
                partitionListChanged = true;
            }

            onRepartitionEvent();

            addedPartitions.forEach(tpi -> partitionedEntities.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()));

            if (!addedPartitions.isEmpty()) {
                var fetchTasks = onAddedPartitions(addedPartitions);
                if (fetchTasks != null && !fetchTasks.isEmpty()) {
                    partitionedFetchTasks.putAll(fetchTasks);
                }
                partitionListChanged = true;
            }

            if (partitionListChanged) {
                List<ListenableFuture<?>> partitionFetchFutures = new ArrayList<>();
                partitionedFetchTasks.values().forEach(partitionFetchFutures::addAll);
                DonAsynchron.withCallback(Futures.allAsList(partitionFetchFutures), t -> logPartitions(), this::logFailure);
            }
        } catch (Throwable t) {
            log.warn("[{}] Failed to init entities state from DB", getServiceName(), t);
        }
    }

    private void logFailure(Throwable e) {
        if (e instanceof CancellationException) {
            //Probably this is fine and happens due to re-balancing.
            log.trace("Partition fetch task error", e);
        } else {
            log.error("Partition fetch task error", e);
        }

    }

    private void logPartitions() {
        log.info("[{}] Managing following partitions:", getServiceName());
        partitionedEntities.forEach((tpi, entities) -> {
            log.info("[{}][{}]: {} entities", getServiceName(), tpi.getFullTopicName(), entities.size());
        });
    }

    protected void onRepartitionEvent() {
    }

    private Set<TopicPartitionInfo> getLatestPartitions() {
        log.debug("getLatestPartitionsFromQueue, queue size {}", subscribeQueue.size());
        Set<TopicPartitionInfo> partitions = null;
        while (!subscribeQueue.isEmpty()) {
            partitions = subscribeQueue.poll();
            log.debug("polled from the queue partitions {}", partitions);
        }
        log.debug("getLatestPartitionsFromQueue, partitions {}", partitions);
        return partitions;
    }

}
