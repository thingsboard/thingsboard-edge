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
package org.thingsboard.server.service.entitiy.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbQueueService extends AbstractTbEntityService implements TbQueueService {

    private final QueueService queueService;
    private final TbClusterService tbClusterService;
    private final TbQueueAdmin tbQueueAdmin;

    @Override
    public Queue saveQueue(Queue queue) {
        boolean create = queue.getId() == null;
        return saveQueue(queue, create);
    }

    @Override
    public Queue saveQueue(Queue queue, boolean create) {
        Queue oldQueue;
        if (create) {
            oldQueue = null;
        } else {
            oldQueue = queueService.findQueueById(queue.getTenantId(), queue.getId());
        }

        Queue savedQueue = queueService.saveQueue(queue);
        createTopicsIfNeeded(savedQueue, oldQueue);
        tbClusterService.onQueuesUpdate(List.of(savedQueue));
        return savedQueue;
    }

    @Override
    public void deleteQueue(TenantId tenantId, QueueId queueId) {
        Queue queue = queueService.findQueueById(tenantId, queueId);
        queueService.deleteQueue(tenantId, queueId);
        tbClusterService.onQueuesDelete(List.of(queue));
    }

    @Override
    public void deleteQueueByQueueName(TenantId tenantId, String queueName) {
        Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, queueName);
        queueService.deleteQueue(tenantId, queue.getId());
        tbClusterService.onQueuesDelete(List.of(queue));
    }

    @Override
    public void updateQueuesByTenants(List<TenantId> tenantIds, TenantProfile newTenantProfile, TenantProfile
            oldTenantProfile) {
        boolean oldIsolated = oldTenantProfile != null && oldTenantProfile.isIsolatedTbRuleEngine();
        boolean newIsolated = newTenantProfile.isIsolatedTbRuleEngine();

        if (!oldIsolated && !newIsolated) {
            return;
        }

        if (newTenantProfile.equals(oldTenantProfile)) {
            return;
        }

        Map<String, TenantProfileQueueConfiguration> oldQueues;
        Map<String, TenantProfileQueueConfiguration> newQueues;

        if (oldIsolated) {
            oldQueues = oldTenantProfile.getProfileData().getQueueConfiguration().stream()
                    .collect(Collectors.toMap(TenantProfileQueueConfiguration::getName, q -> q));
        } else {
            oldQueues = Collections.emptyMap();
        }

        if (newIsolated) {
            newQueues = newTenantProfile.getProfileData().getQueueConfiguration().stream()
                    .collect(Collectors.toMap(TenantProfileQueueConfiguration::getName, q -> q));
        } else {
            newQueues = Collections.emptyMap();
        }

        List<String> toRemove = new ArrayList<>();
        List<String> toCreate = new ArrayList<>();
        List<String> toUpdate = new ArrayList<>();

        for (String oldQueue : oldQueues.keySet()) {
            if (!newQueues.containsKey(oldQueue)) {
                toRemove.add(oldQueue);
            }
        }

        for (String newQueue : newQueues.keySet()) {
            if (oldQueues.containsKey(newQueue)) {
                toUpdate.add(newQueue);
            } else {
                toCreate.add(newQueue);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] Handling profile queue config update: creating queues {}, updating {}, deleting {}. Affected tenants: {}",
                    newTenantProfile.getUuidId(), toCreate, toUpdate, toRemove, tenantIds);
        }

        List<Queue> updated = new ArrayList<>();
        List<Queue> deleted = new ArrayList<>();
        for (TenantId tenantId : tenantIds) {
            for (String name : toCreate) {
                updated.add(new Queue(tenantId, newQueues.get(name)));
            }

            for (String name : toUpdate) {
                Queue queue = new Queue(tenantId, newQueues.get(name));
                Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, name);
                if (foundQueue != null) {
                    queue.setId(foundQueue.getId());
                    queue.setCreatedTime(foundQueue.getCreatedTime());
                }
                if (!queue.equals(foundQueue)) {
                    updated.add(queue);
                    createTopicsIfNeeded(queue, foundQueue);
                }
            }

            for (String name : toRemove) {
                Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, name);
                deleted.add(queue);
            }
        }

        if (!updated.isEmpty()) {
            updated = updated.stream()
                    .map(queueService::saveQueue)
                    .collect(Collectors.toList());
            tbClusterService.onQueuesUpdate(updated);
        }
        if (!deleted.isEmpty()) {
            deleted.forEach(queue -> {
                queueService.deleteQueue(queue.getTenantId(), queue.getId());
            });
            tbClusterService.onQueuesDelete(deleted);
        }
    }

    private void createTopicsIfNeeded(Queue queue, Queue oldQueue) {
        int newPartitions = queue.getPartitions();
        int oldPartitions = oldQueue != null ? oldQueue.getPartitions() : 0;
        for (int i = oldPartitions; i < newPartitions; i++) {
            tbQueueAdmin.createTopicIfNotExists(
                    new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName(),
                    queue.getCustomProperties()
            );
        }
    }

}
