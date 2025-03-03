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
package org.thingsboard.server.service.cf.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@TbRuleEngineComponent
@Service
@Slf4j
@RequiredArgsConstructor
//TODO ashvayka: remove and use TenantEntityProfileCache in each CalculatedFieldManagerMessageProcessor;
public class DefaultCalculatedFieldEntityProfileCache extends TbApplicationEventListener<PartitionChangeEvent> implements CalculatedFieldEntityProfileCache {

    private static final Integer UNKNOWN = 0;
    private final ConcurrentMap<TenantId, TenantEntityProfileCache> tenantCache = new ConcurrentHashMap<>();
    private final PartitionService partitionService;
    private volatile List<Integer> myPartitions = Collections.emptyList();

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        myPartitions = event.getCfPartitions().stream()
                .filter(TopicPartitionInfo::isMyPartition)
                .map(tpi -> tpi.getPartition().orElse(UNKNOWN)).collect(Collectors.toList());
        //Naive approach that need to be improved.
        tenantCache.values().forEach(cache -> cache.setMyPartitions(myPartitions));
    }

    @Override
    public void add(TenantId tenantId, EntityId profileId, EntityId entityId) {
        var tpi = partitionService.resolve(QueueKey.CF, entityId);
        var partition = tpi.getPartition().orElse(UNKNOWN);
        tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache())
                .add(profileId, entityId, partition, tpi.isMyPartition());
    }

    @Override
    public void update(TenantId tenantId, EntityId oldProfileId, EntityId newProfileId, EntityId entityId) {
        var tpi = partitionService.resolve(QueueKey.CF, entityId);
        var partition = tpi.getPartition().orElse(UNKNOWN);
        var cache = tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache());
        //TODO: make this method atomic;
        cache.remove(oldProfileId, entityId);
        cache.add(newProfileId, entityId, partition, tpi.isMyPartition());
    }

    @Override
    public void evict(TenantId tenantId, EntityId entityId) {
        var cache = tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache());
        cache.removeEntityId(entityId);
    }

    @Override
    public Collection<EntityId> getMyEntityIdsByProfileId(TenantId tenantId, EntityId profileId) {
        return tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache()).getMyEntityIdsByProfileId(profileId);
    }

    @Override
    public int getEntityIdPartition(TenantId tenantId, EntityId entityId) {
        var tpi = partitionService.resolve(QueueKey.CF, entityId);
        return tpi.getPartition().orElse(UNKNOWN);
    }

}
