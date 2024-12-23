/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@ConditionalOnExpression("'${queue.type:null}'!='kafka'")
public class PostgresCloudManagerService extends BaseCloudManagerService {

    public static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";
    public static final String QUEUE_SEQ_ID_OFFSET_ATTR_KEY = "queueSeqIdOffset";
    public static final String QUEUE_TS_KV_START_TS_ATTR_KEY = "queueTsKvStartTs";
    public static final String QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY = "queueTsKvSeqIdOffset";

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    private CloudEventService cloudEventService;

    private ExecutorService executor;
    private ExecutorService tsExecutor;

    @PostConstruct
    private void onInit() {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("postgres-cloud-manager"));
        tsExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("postgres-ts-cloud-manager"));
    }

    @PreDestroy
    private void onDestroy() throws InterruptedException {
        super.destroy();
        if (executor != null) {
            executor.shutdownNow();
        }
        if (tsExecutor != null) {
            tsExecutor.shutdownNow();
        }
    }

    @Override
    protected void launchUplinkProcessing() {
        executor.submit(() -> launchUplinkProcessing(QUEUE_START_TS_ATTR_KEY, QUEUE_SEQ_ID_OFFSET_ATTR_KEY, true,
                (TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink)
                        -> cloudEventService.findCloudEvents(tenantId, seqIdStart, seqIdEnd, pageLink)));
        tsExecutor.submit(() -> launchUplinkProcessing(QUEUE_TS_KV_START_TS_ATTR_KEY, QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY, false,
                (TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink)
                        -> cloudEventService.findTsKvCloudEvents(tenantId, seqIdStart, seqIdEnd, pageLink)));
    }

    private void launchUplinkProcessing(String queueStartTsAttrKey, String queueSeqIdAttrKey, boolean isGeneralMsg, CloudEventFinder finder) {
        while (!Thread.interrupted()) {
            try {
                if (initialized) {
                    if (isGeneralMsg) {
                        processUplinkMessages(queueStartTsAttrKey, queueSeqIdAttrKey, true, finder);
                    } else {
                        if (!isGeneralProcessInProgress) {
                            processUplinkMessages(queueStartTsAttrKey, queueSeqIdAttrKey, false, finder);
                        }
                    }
                } else {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                }
            } catch (Exception e) {
                log.warn("Failed to process messages handling!", e);
            }
        }
    }

    private void processUplinkMessages(String queueStartTsAttrKey, String queueSeqIdAttrKey, boolean isGeneralMsg, CloudEventFinder finder) {
        try {
            Long queueSeqIdStart = getLongAttrByKey(tenantId, queueSeqIdAttrKey).get();
            TimePageLink pageLink = newCloudEventsAvailable(tenantId, queueSeqIdStart, queueStartTsAttrKey, finder);
            if (pageLink != null) {
                try {
                    if (isGeneralMsg) {
                        isGeneralProcessInProgress = true;
                    }
                    PageData<CloudEvent> cloudEvents;
                    boolean isInterrupted;
                    do {
                        cloudEvents = finder.find(tenantId, queueSeqIdStart, null, pageLink);
                        if (cloudEvents.getData().isEmpty()) {
                            log.info("seqId column of table started new cycle");
                            cloudEvents = findCloudEventsFromBeginning(tenantId, pageLink, finder);
                        }
                        isInterrupted = sendCloudEvents(cloudEvents.getData()).get();
                        if (!isInterrupted && cloudEvents.getTotalElements() > 0) {
                            CloudEvent latestCloudEvent = cloudEvents.getData().get(cloudEvents.getData().size() - 1);
                            try {
                                Long newStartTs = Uuids.unixTimestamp(latestCloudEvent.getUuidId());
                                updateQueueStartTsSeqIdOffset(tenantId, queueStartTsAttrKey, queueSeqIdAttrKey, newStartTs, latestCloudEvent.getSeqId());
                                log.debug("Queue offset was updated [{}][{}][{}]", latestCloudEvent.getUuidId(), newStartTs, latestCloudEvent.getSeqId());
                            } catch (Exception e) {
                                log.error("Failed to update queue offset [{}]", latestCloudEvent);
                            }
                        }
                        if (!isInterrupted) {
                            pageLink = pageLink.nextPageLink();
                        }
                        if (!isGeneralMsg && isGeneralProcessInProgress) {
                            break;
                        }
                        log.trace("processUplinkMessages state {} {} {} {} {}", isInterrupted, cloudEvents.getTotalElements(), cloudEvents.hasNext(), isGeneralMsg, isGeneralProcessInProgress);
                    } while (isInterrupted || cloudEvents.hasNext());
                } finally {
                    if (isGeneralMsg) {
                        isGeneralProcessInProgress = false;
                    }
                }
            } else {
                log.trace("no new cloud events found {}", isGeneralMsg);
                try {
                    Thread.sleep(cloudEventStorageSettings.getNoRecordsSleepInterval());
                } catch (InterruptedException e) {
                    log.error("Error during sleep", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process TS messages handling!", e);
        }
    }

    private TimePageLink newCloudEventsAvailable(TenantId tenantId, Long queueSeqIdStart, String key, CloudEventFinder finder) {
        try {
            long queueStartTs = getLongAttrByKey(tenantId, key).get();
            long queueEndTs = queueStartTs > 0 ? queueStartTs + TimeUnit.DAYS.toMillis(1) : System.currentTimeMillis();
            TimePageLink pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                    0, null, null, queueStartTs, queueEndTs);
            PageData<CloudEvent> cloudEvents = finder.find(tenantId, queueSeqIdStart, null, pageLink);
            if (cloudEvents.getData().isEmpty()) {
                if (queueSeqIdStart > cloudEventStorageSettings.getMaxReadRecordsCount()) {
                    // check if new cycle started (seq_id starts from '1')
                    cloudEvents = findCloudEventsFromBeginning(tenantId, pageLink, finder);
                    if (cloudEvents.getData().stream().anyMatch(ce -> ce.getSeqId() == 1)) {
                        log.info("newCloudEventsAvailable: new cycle started (seq_id starts from '1')!");
                        return pageLink;
                    }
                }
                while (queueEndTs < System.currentTimeMillis()) {
                    log.trace("newCloudEventsAvailable: queueEndTs < System.currentTimeMillis() [{}] [{}]", queueEndTs, System.currentTimeMillis());
                    queueStartTs = queueEndTs;
                    queueEndTs = queueEndTs + TimeUnit.DAYS.toMillis(1);
                    pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                            0, null, null, queueStartTs, queueEndTs);
                    cloudEvents = finder.find(tenantId, queueSeqIdStart, null, pageLink);
                    if (!cloudEvents.getData().isEmpty()) {
                        return pageLink;
                    }
                }
                return null;
            } else {
                return pageLink;
            }
        } catch (Exception e) {
            log.warn("Failed to check newCloudEventsAvailable!", e);
            return null;
        }
    }

    private PageData<CloudEvent> findCloudEventsFromBeginning(TenantId tenantId, TimePageLink pageLink, CloudEventFinder finder) {
        long seqIdEnd = Integer.toUnsignedLong(cloudEventStorageSettings.getMaxReadRecordsCount());
        seqIdEnd = Math.max(seqIdEnd, 50L);
        return finder.find(tenantId, 0L, seqIdEnd, pageLink);
    }

    private ListenableFuture<Long> getLongAttrByKey(TenantId tenantId, String attrKey) {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attrKey);
        return Futures.transform(future, attributeKvEntryOpt -> {
            if (attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent()) {
                AttributeKvEntry attributeKvEntry = attributeKvEntryOpt.get();
                return attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
            } else {
                return 0L;
            }
        }, dbCallbackExecutorService);
    }

    @Override
    protected void resetQueueOffset() {
        updateQueueStartTsSeqIdOffset(tenantId, QUEUE_START_TS_ATTR_KEY, QUEUE_SEQ_ID_OFFSET_ATTR_KEY, System.currentTimeMillis(), 0L);
        updateQueueStartTsSeqIdOffset(tenantId, QUEUE_TS_KV_START_TS_ATTR_KEY, QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY, System.currentTimeMillis(), 0L);
    }

    private void updateQueueStartTsSeqIdOffset(TenantId tenantId, String attrStartTsKey, String attrSeqIdKey, Long startTs, Long seqIdOffset) {
        log.trace("updateQueueStartTsSeqIdOffset [{}][{}][{}][{}]", attrStartTsKey, attrSeqIdKey, startTs, seqIdOffset);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(attrStartTsKey, startTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(attrSeqIdKey, seqIdOffset), System.currentTimeMillis())
        );

        attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes);
    }

    @FunctionalInterface
    private interface CloudEventFinder {
        PageData<CloudEvent> find(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);
    }
}
