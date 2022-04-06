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
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbCoreOrIntegrationExecutorComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

@Slf4j
@TbCoreOrIntegrationExecutorComponent
@Service
@RequiredArgsConstructor
public class DefaultClusterIntegrationService extends TbApplicationEventListener<PartitionChangeEvent> implements ClusterIntegrationService {

    private final IntegrationManagerService integrationManagerService;
    private final Map<IntegrationType, Queue<Set<TopicPartitionInfo>>> subscribeEventsMap = new ConcurrentHashMap<>();

    private volatile ListeningScheduledExecutorService queueExecutor;

    @PostConstruct
    public void init() {
        queueExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("scheduler-service")));
    }

    @PreDestroy
    public void stop() {
        if (queueExecutor != null) {
            queueExecutor.shutdownNow();
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        IntegrationType type = IntegrationType.valueOf(event.getServiceQueueKey().getServiceQueue().getQueue());
        subscribeEventsMap.computeIfAbsent(type, t -> new ConcurrentLinkedQueue<>()).add(event.getPartitions());
        queueExecutor.submit(() -> refreshIntegrationsByType(type));
    }

    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return ServiceType.TB_INTEGRATION_EXECUTOR.equals(event.getServiceType());
    }

    private void refreshIntegrationsByType(IntegrationType type) {
        //TODO: performance improvement - check if we received events for not supported integration types and filter them.
        Set<TopicPartitionInfo> partitions = getLatestPartitionsFromQueue(type);
        if (partitions != null) {
            integrationManagerService.refresh(type, partitions);
        }
    }

    Set<TopicPartitionInfo> getLatestPartitionsFromQueue(IntegrationType type) {
        var queue = subscribeEventsMap.get(type);
        log.debug("[{}] getLatestPartitionsFromQueue, queue size {}", type, queue.size());
        Set<TopicPartitionInfo> partitions = null;
        while (!queue.isEmpty()) {
            partitions = queue.poll();
            log.debug("[{}] polled from the queue partitions {}", type, partitions);
        }
        log.debug("[{}] getLatestPartitionsFromQueue, partitions {}", type, partitions);
        return partitions;
    }


}
