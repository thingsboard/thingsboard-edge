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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.cloud.EdgeSettingsService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EdgeCommunicationStatsService {
    private static final long IGNORE_SELF_STATS_DELTA = -1;

    private static final String UPLINK_MSGS_ADDED = "uplinkMsgsAdded";
    private static final String UPLINK_MSGS_PUSHED = "uplinkMsgsPushed";
    private static final String UPLINK_MSGS_PERMANENTLY_FAILED = "uplinkMsgsPermanentlyFailed";
    private static final String UPLINK_MSGS_TMP_FAILED = "uplinkMsgsTmpFailed";
    private static final String UPLINK_MSGS_LAG = "uplinkMsgsLag";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private EdgeSettingsService edgeSettingsService;
    @Autowired
    private TimeseriesService tsService;
    @Autowired
    private TbClusterService tbClusterService;
    @Autowired(required = false)
    private TbKafkaAdmin tbKafkaAdmin;
    @Autowired(required = false)
    private TopicService topicService;

    private TenantId tenantId;
    private EdgeId edgeId;

    @Value("${edge.stats.enabled:true}")
    private boolean edgeStatsEnabled;
    @Value("${edge.stats.ttl-days:7}")
    private int edgeStatsTtlDays;
    @Value("${edge.stats.report-interval-millis:20000}")
    private long reportIntervalMillis;

    private final EdgeMsgCounters uplinkCounters = new EdgeMsgCounters();

    @Scheduled(fixedDelayString = "${edge.stats.report-interval-millis:20000}")
    public void reportStats() {
        log.debug("Reporting Edge communication stats...");
        try {
            if (!edgeStatsEnabled) {
                log.debug("Edge stats reporting is disabled by configuration.");
                return;
            }
            initTenantIdAndEdgeId();
            TopicPartitionInfo topic = topicService.getEdgeEventNotificationsTopic(tenantId, edgeId);
            if (topic != null) {
                String groupId = topic.getTopic();
                long uplinkLag = tbKafkaAdmin.getTotalLagForGroup(groupId);
                uplinkCounters.getMsgsLag().set(uplinkLag);
            }
            // Exclude self-generated stats TbMsg from uplink stats
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

            log.trace("Reported Edge communication stats: {}",
                    statsEntries.stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValueAsString())
                            .collect(Collectors.joining(", "))
            );

            long telemetryTtlSeconds = TimeUnit.DAYS.toSeconds(edgeStatsTtlDays);
            tsService.save(tenantId, edgeId, statsEntries, telemetryTtlSeconds);

            Map<String, String> statsMap = statsEntries.stream()
                    .collect(Collectors.toMap(TsKvEntry::getKey, TsKvEntry::getValueAsString));
            String statsJson = OBJECT_MAPPER.writeValueAsString(statsMap);

            TbMsg tbMsg = TbMsg.newMsg()
                    .type(TbMsgType.POST_TELEMETRY_REQUEST)
                    .originator(edgeId)
                    .dataType(TbMsgDataType.JSON)
                    .data(statsJson)
                    .build();

            tbClusterService.pushMsgToRuleEngine(tenantId, edgeId, tbMsg, null);
            log.info("Successfully pushed telemetry TbMsg to Rule Engine: {}", statsJson);
        } catch (Exception e) {
            log.warn("Failed to push telemetry TbMsg to Rule Engine", e);
        }

        // clear counters for next interval
        uplinkCounters.clear();
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

    public void incrementUplinkMsgsAdded(long count) {uplinkCounters.getMsgsAdded().addAndGet(count);}

    public void incrementUplinkMsgsPushed(long count) {uplinkCounters.getMsgsPushed().addAndGet(count);}

    public void incrementUplinkMsgsPermanentlyFailed(long count) {uplinkCounters.getMsgsPermanentlyFailed().addAndGet(count);}

    public void incrementUplinkMsgsTmpFailed(long count) {uplinkCounters.getMsgsTmpFailed().addAndGet(count);}

    public void setUplinkMsgsLag(long count) {uplinkCounters.getMsgsLag().set(count);}

}
