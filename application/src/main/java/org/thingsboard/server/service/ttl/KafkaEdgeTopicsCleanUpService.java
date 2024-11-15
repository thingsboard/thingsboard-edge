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
package org.thingsboard.server.service.ttl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.state.DefaultDeviceStateService;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && ${edges.enabled:true}")
public class KafkaEdgeTopicsCleanUpService {

    private final EdgeService edgeService;
    private final TenantService tenantService;
    private final AttributesService attributesService;

    private final TopicService topicService;
    private final PartitionService partitionService;

    private final TbKafkaSettings kafkaSettings;
    private final TbKafkaTopicConfigs kafkaTopicConfigs;

    @Value("${sql.ttl.edge_events.edge_events_ttl:2628000}")
    private long ttlSeconds;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("kafka-edge-topic-cleanup"));

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.edge_events.execution_interval_ms})}", fixedDelayString = "${sql.ttl.edge_events.execution_interval_ms}")
    public void cleanUp() {
        executorService.submit(() -> {
            PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 10_000);
            for (TenantId tenantId : tenants) {
                try {
                    cleanUp(tenantId);
                } catch (Exception e) {
                    log.warn("Failed to drop kafka topics for tenant {}", tenantId, e);
                }
            }
        });
    }

    private void cleanUp(TenantId tenantId) throws Exception {
        if (!partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
            return;
        }

        PageDataIterable<EdgeId> edgeIds = new PageDataIterable<>(link -> edgeService.findEdgeIdsByTenantId(tenantId, link), 1024);
        long currentTimeMillis = System.currentTimeMillis();
        long ttlMillis = TimeUnit.SECONDS.toChronoUnit().getDuration().multipliedBy(ttlSeconds).toMillis();

        for (EdgeId edgeId : edgeIds) {
            attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, DefaultDeviceStateService.LAST_CONNECT_TIME).get()
                    .flatMap(AttributeKvEntry::getLongValue)
                    .filter(lastConnectTime -> isTopicExpired(lastConnectTime, ttlMillis, currentTimeMillis))
                    .ifPresent(lastConnectTime -> {
                        String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId).getTopic();
                        TbKafkaAdmin kafkaAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getEdgeEventConfigs());
                        if (kafkaAdmin.isTopicEmpty(topic)) {
                            kafkaAdmin.deleteTopic(topic);
                            log.info("Removed outdated topic for tenant {} and edge with id {} older than {}",
                                    tenantId, edgeId, Date.from(Instant.ofEpochMilli(currentTimeMillis - ttlMillis)));
                        }
                    });
        }
    }

    private boolean isTopicExpired(long lastConnectTime, long ttlMillis, long currentTimeMillis) {
        return lastConnectTime + ttlMillis < currentTimeMillis;
    }

}
