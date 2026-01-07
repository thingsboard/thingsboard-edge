/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.stats;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.cloud.EdgeSettingsService;
import org.thingsboard.server.dao.edge.stats.CloudStatsCounterService;
import org.thingsboard.server.dao.edge.stats.CloudStatsKey;
import org.thingsboard.server.dao.edge.stats.MsgCounters;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.edge.stats.CloudStatsKey.UPLINK_MSGS_ADDED;
import static org.thingsboard.server.dao.edge.stats.CloudStatsKey.UPLINK_MSGS_LAG;
import static org.thingsboard.server.dao.edge.stats.CloudStatsKey.UPLINK_MSGS_PERMANENTLY_FAILED;
import static org.thingsboard.server.dao.edge.stats.CloudStatsKey.UPLINK_MSGS_PUSHED;
import static org.thingsboard.server.dao.edge.stats.CloudStatsKey.UPLINK_MSGS_TMP_FAILED;

@TbCoreComponent
@ConditionalOnProperty(prefix = "cloud.stats", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Service
@Slf4j
public class CloudStatsService {

    private static final String CLOUD_EVENT_CONSUMER = "-cloud-event-consumer";
    private static final String CLOUD_EVENT_TS_CONSUMER = "-cloud-event-ts-consumer";

    private final TimeseriesService tsService;
    private final CloudStatsCounterService statsCounterService;
    private final TopicService topicService;
    private final EdgeSettingsService edgeSettingsService;
    private final CloudEventService cloudEventService;
    private final Optional<KafkaAdmin> kafkaAdmin;

    private TenantId tenantId;
    private EdgeId edgeId;

    @Value("${cloud.stats.ttl:30}")
    private int cloudStatsTtlDays;
    @Value("${cloud.stats.report-interval-millis:600000}")
    private long reportIntervalMillis;
    @Value("${service.type:monolith}")
    private String serviceType;

    @Scheduled(
            fixedDelayString = "${cloud.stats.report-interval-millis:600000}",
            initialDelayString = "${cloud.stats.report-interval-millis:600000}"
    )
    public void reportStats() {
        if (!initTenantIdAndEdgeId()) {
            return;
        }

        log.debug("Reporting cloud communication stats...");
        long ts = (System.currentTimeMillis() / reportIntervalMillis) * reportIntervalMillis;
        MsgCounters counters = statsCounterService.getCounter(tenantId);
        kafkaAdmin.ifPresent(this::prepareUplinkLag);

        List<TsKvEntry> statsEntries = List.of(
                entry(ts, UPLINK_MSGS_ADDED.getKey(), counters.getMsgsAdded().get()),
                entry(ts, UPLINK_MSGS_PUSHED.getKey(), counters.getMsgsPushed().get()),
                entry(ts, UPLINK_MSGS_PERMANENTLY_FAILED.getKey(), counters.getMsgsPermanentlyFailed().get()),
                entry(ts, UPLINK_MSGS_TMP_FAILED.getKey(), counters.getMsgsTmpFailed().get()),
                entry(ts, UPLINK_MSGS_LAG.getKey(), counters.getMsgsLag().get())
        );

        saveTs(ts, statsEntries);
    }

    private boolean initTenantIdAndEdgeId() {
        if (tenantId != null && edgeId != null) {
            return true;
        }
        try {
            EdgeSettings edgeSettings = edgeSettingsService.findEdgeSettings();
            if (edgeSettings == null) {
                log.warn("EdgeSettings not found, skipping stats reporting.");
                return false;
            }
            tenantId = TenantId.fromUUID(UUID.fromString(edgeSettings.getTenantId()));
            edgeId = new EdgeId(UUID.fromString(edgeSettings.getEdgeId()));
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format in EdgeSettings, skipping stats reporting.", e);
            return false;
        }
    }

    private void prepareUplinkLag(KafkaAdmin kafkaAdmin) {
        String cloudEventConsumerGroupId = topicService.buildTopicName(serviceType + CLOUD_EVENT_CONSUMER);
        String cloudEventTsConsumerGroupId = topicService.buildTopicName(serviceType + CLOUD_EVENT_TS_CONSUMER);
        Set<String> groupIds = Set.of(cloudEventConsumerGroupId, cloudEventTsConsumerGroupId);

        Map<String, Long> groupIdToLag = kafkaAdmin.getTotalLagForGroupsBulk(groupIds);

        long lagCloudEvent = groupIdToLag.getOrDefault(cloudEventConsumerGroupId, 0L);
        long lagCloudEventTs = groupIdToLag.getOrDefault(cloudEventTsConsumerGroupId, 0L);

        statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_LAG, tenantId, lagCloudEvent + lagCloudEventTs);
    }

    private BasicTsKvEntry entry(long ts, String key, long value) {
        return new BasicTsKvEntry(ts, new LongDataEntry(key, value));
    }

    private void saveTs(long ts, List<TsKvEntry> statsEntries) {
        try {
            ObjectNode entityBody = buildStatsJson(ts, statsEntries);
            long telemetryTtlSeconds = TimeUnit.DAYS.toSeconds(cloudStatsTtlDays);
            tsService.save(tenantId, edgeId, statsEntries, telemetryTtlSeconds);

            CloudEvent cloudEvent = new CloudEvent(
                    tenantId,
                    EdgeEventActionType.TIMESERIES_UPDATED,
                    edgeId.getId(),
                    CloudEventType.EDGE,
                    entityBody
            );

            ListenableFuture<Void> future = cloudEventService.saveTsKvAsync(cloudEvent);

            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    log.trace("Successfully saved cloud event with stats: {}", statsEntries);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("Failed to save cloud event with stats: {}", statsEntries, t);
                }
            }, MoreExecutors.directExecutor());
        } finally {
            statsCounterService.clear();
        }
    }

    private ObjectNode buildStatsJson(long ts, List<TsKvEntry> statsEntries) {
        ObjectNode entityBody = JacksonUtil.newObjectNode();
        entityBody.put("ts", ts);
        ObjectNode data = JacksonUtil.newObjectNode();
        statsEntries.forEach(entry -> data.put(entry.getKey(), entry.getValueAsString()));
        entityBody.set("data", data);
        return entityBody;
    }

}
