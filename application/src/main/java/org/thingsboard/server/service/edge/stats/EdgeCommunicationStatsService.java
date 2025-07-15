/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
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
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EdgeCommunicationStatsService {
    private static final long IGNORE_SELF_STATS_DELTA = -1;

    private static final String CLOUD_EVENT_CONSUMER = "-cloud-event-consumer";
    private static final String CLOUD_EVENT_TS_CONSUMER = "-cloud-event-ts-consumer";

    private static final String UPLINK_MSGS_ADDED = "uplinkMsgsAdded";
    private static final String UPLINK_MSGS_PUSHED = "uplinkMsgsPushed";
    private static final String UPLINK_MSGS_PERMANENTLY_FAILED = "uplinkMsgsPermanentlyFailed";
    private static final String UPLINK_MSGS_TMP_FAILED = "uplinkMsgsTmpFailed";
    private static final String UPLINK_MSGS_LAG = "uplinkMsgsLag";

    @Autowired
    private EdgeSettingsService edgeSettingsService;
    @Autowired
    private TimeseriesService tsService;
    @Autowired
    private CloudEventService cloudEventService;
    @Autowired(required = false)
    private TbKafkaAdmin tbKafkaAdmin;
    @Autowired(required = false)
    private TopicService topicService;

    private TenantId tenantId;
    private EdgeId edgeId;

    @Value("${cloud.stats.enabled:true}")
    private boolean edgeStatsEnabled;
    @Value("${cloud.stats.ttl-days:7}")
    private int edgeStatsTtlDays;
    @Value("${cloud.stats.report-interval-millis:20000}")
    private long reportIntervalMillis;
    @Value("${service.type:monolith}")
    private String serviceType;

    private final EdgeMsgCounters uplinkCounters = new EdgeMsgCounters();

    @Scheduled(fixedDelayString = "${cloud.stats.report-interval-millis:20000}")
    public void reportStats() {
        log.debug("Reporting Edge communication stats...");
        try {
            if (!edgeStatsEnabled) {
                log.debug("Edge stats reporting is disabled by configuration.");
                return;
            }
            initTenantIdAndEdgeId();
            updateLagIfKafkaEnabled();

            // Exclude self-generated stats from uplink stats
            uplinkCounters.getMsgsAdded().addAndGet(IGNORE_SELF_STATS_DELTA);
            uplinkCounters.getMsgsPushed().addAndGet(IGNORE_SELF_STATS_DELTA);

            long ts = (System.currentTimeMillis() / reportIntervalMillis) * reportIntervalMillis;
            List<TsKvEntry> statsEntries = List.of(
                    entry(ts, UPLINK_MSGS_ADDED, uplinkCounters.getMsgsAdded().get()),
                    entry(ts, UPLINK_MSGS_PUSHED, uplinkCounters.getMsgsPushed().get()),
                    entry(ts, UPLINK_MSGS_PERMANENTLY_FAILED, uplinkCounters.getMsgsPermanentlyFailed().get()),
                    entry(ts, UPLINK_MSGS_TMP_FAILED, uplinkCounters.getMsgsTmpFailed().get()),
                    entry(ts, UPLINK_MSGS_LAG, uplinkCounters.getMsgsLag().get())
            );

            ObjectNode statsJson = JacksonUtil.newObjectNode();
            statsEntries.forEach(entry -> statsJson.put(entry.getKey(), entry.getValueAsString()));

            log.trace("Reported Edge communication stats: {}", statsJson);

            long telemetryTtlSeconds = TimeUnit.DAYS.toSeconds(edgeStatsTtlDays);
            tsService.save(tenantId, edgeId, statsEntries, telemetryTtlSeconds);

            cloudEventService.saveCloudEvent(
                    tenantId,
                    CloudEventType.EDGE,
                    EdgeEventActionType.TIMESERIES_UPDATED,
                    edgeId,
                    statsJson
            );

            log.info("Successfully saved cloud event with stats: {}", statsJson);
        } catch (Exception e) {
            log.warn("Failed to push telemetry TbMsg to Rule Engine", e);
        } finally {
            // clear counters for next interval
            uplinkCounters.clear();
        }
    }

    private void updateLagIfKafkaEnabled() {
        if (tbKafkaAdmin != null) {
            String cloudEventConsumerGroupId = topicService.buildTopicName(serviceType + CLOUD_EVENT_CONSUMER);
            String cloudEventTsConsumerGroupId = topicService.buildTopicName(serviceType + CLOUD_EVENT_TS_CONSUMER);
            setUplinkMsgsLag(tbKafkaAdmin.getTotalLagForGroups(cloudEventConsumerGroupId, cloudEventTsConsumerGroupId));
        }
    }

    private BasicTsKvEntry entry(long ts, String key, long value) {
        return new BasicTsKvEntry(ts, new LongDataEntry(key, value));
    }

    private void initTenantIdAndEdgeId() {
        if (tenantId == null || edgeId == null) {
            EdgeSettings edgeSettings = edgeSettingsService.findEdgeSettings();
            this.tenantId = TenantId.fromUUID(UUID.fromString(edgeSettings.getTenantId()));
            this.edgeId = new EdgeId(UUID.fromString(edgeSettings.getEdgeId()));
        }
    }

    public void addUplinkMsgsAdded(long value) {uplinkCounters.getMsgsAdded().addAndGet(value);}

    public void addUplinkMsgsPushed(long value) {uplinkCounters.getMsgsPushed().addAndGet(value);}

    public void addUplinkMsgsPermanentlyFailed(long value) {uplinkCounters.getMsgsPermanentlyFailed().addAndGet(value);}

    public void addUplinkMsgsTmpFailed(long value) {uplinkCounters.getMsgsTmpFailed().addAndGet(value);}

    public void setUplinkMsgsLag(long value) {uplinkCounters.getMsgsLag().set(value);}

}
