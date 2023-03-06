/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.kafka;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
public class TbKafkaConsumerStatsService {
    private final Set<String> monitoredGroups = ConcurrentHashMap.newKeySet();

    private final TbKafkaSettings kafkaSettings;
    private final TbKafkaConsumerStatisticConfig statsConfig;

    @Lazy
    @Autowired
    private PartitionService partitionService;

    private AdminClient adminClient;
    private Consumer<String, byte[]> consumer;
    private ScheduledExecutorService statsPrintScheduler;

    @PostConstruct
    public void init() {
        if (!statsConfig.getEnabled()) {
            return;
        }
        this.adminClient = AdminClient.create(kafkaSettings.toAdminProps());
        this.statsPrintScheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("kafka-consumer-stats"));

        Properties consumerProps = kafkaSettings.toConsumerProps(null);
        consumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-stats-loader-client");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-stats-loader-client-group");
        this.consumer = new KafkaConsumer<>(consumerProps);

        startLogScheduling();
    }

    private void startLogScheduling() {
        Duration timeoutDuration = Duration.ofMillis(statsConfig.getKafkaResponseTimeoutMs());
        statsPrintScheduler.scheduleWithFixedDelay(() -> {
            if (!isStatsPrintRequired()) {
                return;
            }
            for (String groupId : monitoredGroups) {
                try {
                    Map<TopicPartition, OffsetAndMetadata> groupOffsets = adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata()
                            .get(statsConfig.getKafkaResponseTimeoutMs(), TimeUnit.MILLISECONDS);
                    Map<TopicPartition, Long> endOffsets = consumer.endOffsets(groupOffsets.keySet(), timeoutDuration);

                    List<GroupTopicStats> lagTopicsStats = getTopicsStatsWithLag(groupOffsets, endOffsets);
                    if (!lagTopicsStats.isEmpty()) {
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < lagTopicsStats.size(); i++) {
                            builder.append(lagTopicsStats.get(i).toString());
                            if (i != lagTopicsStats.size() - 1) {
                                builder.append(", ");
                            }
                        }
                        log.info("[{}] Topic partitions with lag: [{}].", groupId, builder.toString());
                    }
                } catch (Exception e) {
                    log.warn("[{}] Failed to get consumer group stats. Reason - {}.", groupId, e.getMessage());
                    log.trace("Detailed error: ", e);
                }
            }

        }, statsConfig.getPrintIntervalMs(), statsConfig.getPrintIntervalMs(), TimeUnit.MILLISECONDS);
    }

    private boolean isStatsPrintRequired() {
        boolean isMyRuleEnginePartition = partitionService.resolve(ServiceType.TB_RULE_ENGINE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
        boolean isMyCorePartition = partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
        return log.isInfoEnabled() && (isMyRuleEnginePartition || isMyCorePartition);
    }

    private List<GroupTopicStats> getTopicsStatsWithLag(Map<TopicPartition, OffsetAndMetadata> groupOffsets, Map<TopicPartition, Long> endOffsets) {
        List<GroupTopicStats> consumerGroupStats = new ArrayList<>();
        for (TopicPartition topicPartition : groupOffsets.keySet()) {
            long endOffset = endOffsets.get(topicPartition);
            long committedOffset = groupOffsets.get(topicPartition).offset();
            long lag = endOffset - committedOffset;
            if (lag != 0) {
                GroupTopicStats groupTopicStats = GroupTopicStats.builder()
                        .topic(topicPartition.topic())
                        .partition(topicPartition.partition())
                        .committedOffset(committedOffset)
                        .endOffset(endOffset)
                        .lag(lag)
                        .build();
                consumerGroupStats.add(groupTopicStats);
            }
        }
        return consumerGroupStats;
    }

    public void registerClientGroup(String groupId) {
        if (statsConfig.getEnabled() && !StringUtils.isEmpty(groupId)) {
            monitoredGroups.add(groupId);
        }
    }

    public void unregisterClientGroup(String groupId) {
        if (statsConfig.getEnabled() && !StringUtils.isEmpty(groupId)) {
            monitoredGroups.remove(groupId);
        }
    }

    @PreDestroy
    public void destroy() {
        if (statsPrintScheduler != null) {
            statsPrintScheduler.shutdownNow();
        }
        if (adminClient != null) {
            adminClient.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }


    @Builder
    @Data
    private static class GroupTopicStats {
        private String topic;
        private int partition;
        private long committedOffset;
        private long endOffset;
        private long lag;

        @Override
        public String toString() {
            return "[" +
                    "topic=[" + topic + ']' +
                    ", partition=[" + partition + "]" +
                    ", committedOffset=[" + committedOffset + "]" +
                    ", endOffset=[" + endOffset + "]" +
                    ", lag=[" + lag + "]" +
                    "]";
        }
    }
}
