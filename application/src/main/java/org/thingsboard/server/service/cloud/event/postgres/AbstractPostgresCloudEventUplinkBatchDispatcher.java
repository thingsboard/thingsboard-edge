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
package org.thingsboard.server.service.cloud.event.postgres;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.edge.stats.CloudStatsCounterService;
import org.thingsboard.server.dao.edge.stats.CloudStatsKey;
import org.thingsboard.server.service.cloud.CloudEventFinder;
import org.thingsboard.server.service.cloud.event.sender.CloudEventUplinkSender;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractPostgresCloudEventUplinkBatchDispatcher {

    @Autowired
    private PostgresCloudEventUplinkRetriever cloudEventUplinkRetriever;
    @Autowired
    private CloudStatsCounterService statsCounterService;
    @Autowired
    private PostgresResetQueueOffsetEventHandler resetQueueOffsetEventHandler;
    @Autowired
    private EdgeInfoHolder edgeInfo;

    protected abstract CloudEventUplinkSender getCloudEventUplinkSender();

    public void processUplinkMessages(TimePageLink pageLink, Long queueSeqIdStart, String queueStartTsAttrKey, String queueSeqIdAttrKey, boolean isGeneralMsg, CloudEventFinder finder) {
        try {
            if (isGeneralMsg) {
                edgeInfo.setGeneralProcessInProgress(true);
            }
            PageData<CloudEvent> cloudEvents;
            boolean isInterrupted;
            do {
                if (!isGeneralMsg) {
                    waitForGeneralProcessingCompleteIfInProgress();
                }
                cloudEvents = finder.find(edgeInfo.getTenantId(), queueSeqIdStart, null, pageLink);
                if (cloudEvents.getData().isEmpty()) {
                    log.info("seqId column of table started new cycle. queueSeqIdStart={}, queueStartTsAttrKey={}, queueSeqIdAttrKey={}, isGeneralMsg={}",
                            queueSeqIdStart, queueStartTsAttrKey, queueSeqIdAttrKey, isGeneralMsg);
                    cloudEvents = cloudEventUplinkRetriever.findCloudEventsFromBeginning(edgeInfo.getTenantId(), pageLink, finder);
                }
                isInterrupted = getCloudEventUplinkSender().sendCloudEvents(cloudEvents.getData(), isGeneralMsg).get();
                if (!isInterrupted && cloudEvents.getTotalElements() > 0) {
                    CloudEvent latestCloudEvent = cloudEvents.getData().get(cloudEvents.getData().size() - 1);
                    try {
                        Long newStartTs = Uuids.unixTimestamp(latestCloudEvent.getUuidId());
                        resetQueueOffsetEventHandler.updateQueueStartTsSeqIdOffset(edgeInfo.getTenantId(), queueStartTsAttrKey, queueSeqIdAttrKey, newStartTs, latestCloudEvent.getSeqId());
                        log.info("Queue offset was updated [{}][{}][{}]", latestCloudEvent.getUuidId(), newStartTs, latestCloudEvent.getSeqId());
                    } catch (Exception e) {
                        log.error("Failed to update queue offset [{}]", latestCloudEvent);
                    }
                }
                if (!isInterrupted) {
                    pageLink = pageLink.nextPageLink();
                    if (cloudEvents.hasNext()) {
                        String queueName = isGeneralMsg ? "Cloud Event" : "TSKv Cloud Event";

                        long queueSize = Math.max(cloudEvents.getTotalElements() - ((long) pageLink.getPage() * pageLink.getPageSize()), 0);
                        statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_LAG, edgeInfo.getTenantId(), queueSize);
                        log.info("[{}] Uplink Processing Lag Stats: queue size = [{}], current page = [{}], total pages = [{}]",
                                queueName, queueSize, pageLink.getPage(), cloudEvents.getTotalPages());
                    }
                }
                if (!isGeneralMsg) {
                    waitForGeneralProcessingCompleteIfInProgress();
                }
                log.trace("processUplinkMessages state isInterrupted={},total={},hasNext={},isGeneralMsg={},isGeneralProcessInProgress={}",
                        isInterrupted, cloudEvents.getTotalElements(), cloudEvents.hasNext(), isGeneralMsg, edgeInfo.isGeneralProcessInProgress());
            } while (isInterrupted || cloudEvents.hasNext());
        } catch (Exception e) {
            log.error("Failed to process cloud event messages handling!", e);
        } finally {
            if (isGeneralMsg) {
                edgeInfo.setGeneralProcessInProgress(false);
            }
        }
    }

    private void waitForGeneralProcessingCompleteIfInProgress() {
        if (!edgeInfo.isGeneralProcessInProgress()) {
            return;
        }
        do {
            try {
                TimeUnit.MILLISECONDS.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        } while (edgeInfo.isGeneralProcessInProgress());
    }
}
