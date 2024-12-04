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
import com.drew.lang.annotations.Nullable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class PostgresUplinkMessageService extends BaseUplinkMessageService {
    public static final String QUEUE_END_TS_LESS_THAN_CURRENT_TIME_MESSAGE = "newCloudEventsAvailable: queueEndTs < System.currentTimeMillis()";
    public static final String FAILED_TO_FIND_CLOUD_EVENTS = "Failed to find cloudEvents";
    public static final String STARTED_NEW_CYCLE_MESSAGE = "newCloudEventsAvailable: new cycle started (seq_id starts from '1')!";
    public static final String QUEUE_OFFSET_WAS_UPDATED_MESSAGE = "Queue offset was updated";
    public static final String FAILED_TO_UPDATE_QUEUE_OFFSET_ERROR_MESSAGE = "Failed to update queue offset";
    public static final String UPDATE_QUEUE_START_TS_SEQ_ID_OFFSET_MESSAGE = "updateQueueStartTsSeqIdOffset";
    private static final String TABLE_STARTED_NEW_CYCLE_MESSAGE = "seqId column of {} table started new cycle";

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    protected CloudEventService cloudEventService;

    public void processHandleMessages(TenantId tenantId) throws Exception {
        Long cloudEventsQueueSeqIdStart = getQueueSeqIdStart(tenantId).get();
        TimePageLink cloudEventsPageLink = newCloudEventsAvailable(tenantId, cloudEventsQueueSeqIdStart);

        if (cloudEventsPageLink != null) {
            processCloudEvents(tenantId, cloudEventsQueueSeqIdStart, cloudEventsPageLink);
        }
    }

    @Nullable
    public TimePageLink newCloudEventsAvailable(TenantId tenantId, Long queueSeqIdStart) {
        try {
            TimePageLink pageLink = prepareTimePageLink(tenantId);
            PageData<CloudEvent> cloudEvents = findCloudEvents(tenantId, queueSeqIdStart, null, pageLink);

            if (cloudEvents.getData().isEmpty()) {
                return getLastTimePageLink(tenantId, queueSeqIdStart, pageLink);
            } else {
                return pageLink;
            }
        } catch (Exception e) {
            log.warn(FAILED_TO_FIND_CLOUD_EVENTS, e);
            return null;
        }
    }

    private TimePageLink prepareTimePageLink(TenantId tenantId) throws InterruptedException, ExecutionException {
        int maxReadRecordsCount = cloudEventStorageSettings.getMaxReadRecordsCount();
        long queueStartTs = getQueueStartTs(tenantId).get();
        long queueEndTs = queueStartTs > 0 ? queueStartTs + TimeUnit.DAYS.toMillis(1) : System.currentTimeMillis();

        return new TimePageLink(maxReadRecordsCount, 0, null, null, queueStartTs, queueEndTs);
    }

    @Nullable
    private TimePageLink getLastTimePageLink(TenantId tenantId, Long queueSeqIdStart, TimePageLink pageLink) {
        PageData<CloudEvent> cloudEvents = findCloudEventsFromBeginning(tenantId, pageLink);

        if (cloudEvents.getData().stream().noneMatch(ce -> ce.getSeqId() == 1)) {
            return findFromQueueEndToToday(tenantId, queueSeqIdStart, pageLink.getEndTime());
        } else {
            log.info(STARTED_NEW_CYCLE_MESSAGE);
            return pageLink;
        }
    }

    private PageData<CloudEvent> findCloudEventsFromBeginning(TenantId tenantId, TimePageLink pageLink) {
        long seqIdEnd = Integer.toUnsignedLong(cloudEventStorageSettings.getMaxReadRecordsCount());
        seqIdEnd = Math.max(seqIdEnd, 50L);

        return findCloudEvents(tenantId, 0L, seqIdEnd, pageLink);
    }

    @Nullable
    private TimePageLink findFromQueueEndToToday(TenantId tenantId, Long queueSeqIdStart, long queueEndTs) {
        long queueStartTs;

        while (queueEndTs < System.currentTimeMillis()) {
            log.trace(QUEUE_END_TS_LESS_THAN_CURRENT_TIME_MESSAGE + " [{}] [{}]", queueEndTs, System.currentTimeMillis());
            queueStartTs = queueEndTs;
            queueEndTs = queueEndTs + TimeUnit.DAYS.toMillis(1);
            TimePageLink pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                    0, null, null, queueStartTs, queueEndTs);

            PageData<CloudEvent> cloudEvents = findCloudEvents(tenantId, queueSeqIdStart, null, pageLink);

            if (!cloudEvents.getData().isEmpty()) {
                return pageLink;
            }
        }

        return null;
    }

    public void processCloudEvents(TenantId tenantId, Long queueSeqIdStart, TimePageLink pageLink) {
        PageData<CloudEvent> cloudEvents;

        do {
            cloudEvents = prepareCloudEvents(tenantId, queueSeqIdStart, pageLink);

            sendCloudEvents(cloudEvents);

            pageLink = prepareNextPageLink(tenantId, cloudEvents, pageLink);
        } while (isProcessContinue(tenantId, cloudEvents));
    }

    private boolean isProcessContinue(TenantId tenantId, PageData<CloudEvent> cloudEvents) {
        return super.isProcessContinue(tenantId) && cloudEvents.hasNext();
    }

    private PageData<CloudEvent> prepareCloudEvents(TenantId tenantId, Long queueSeqIdStart, TimePageLink pageLink) {
        PageData<CloudEvent> cloudEvents = findCloudEvents(tenantId, queueSeqIdStart, null, pageLink);

        if (cloudEvents.getData().isEmpty()) {
            log.info(TABLE_STARTED_NEW_CYCLE_MESSAGE, getTableName());
            return findCloudEventsFromBeginning(tenantId, pageLink);
        }

        return cloudEvents;
    }

    private TimePageLink prepareNextPageLink(TenantId tenantId, PageData<CloudEvent> cloudEvents, TimePageLink pageLink) {
        if (cloudEvents.getTotalElements() > 0) {
            chooseAnotherStartTs(tenantId, cloudEvents);
        }

        return pageLink.nextPageLink();
    }

    private void chooseAnotherStartTs(TenantId tenantId, PageData<CloudEvent> cloudEvents) {
        CloudEvent latestCloudEvent = cloudEvents.getData().get(cloudEvents.getData().size() - 1);

        try {
            Long newStartTs = Uuids.unixTimestamp(latestCloudEvent.getUuidId());
            updateQueueStartTsSeqIdOffset(tenantId, newStartTs, latestCloudEvent.getSeqId());
            log.debug(QUEUE_OFFSET_WAS_UPDATED_MESSAGE + " [{}][{}][{}]", latestCloudEvent.getUuidId(), newStartTs, latestCloudEvent.getSeqId());
        } catch (Exception e) {
            log.error(FAILED_TO_UPDATE_QUEUE_OFFSET_ERROR_MESSAGE + " [{}]", latestCloudEvent);
        }
    }

    protected ListenableFuture<Long> getLongAttrByKey(TenantId tenantId, String attrKey) {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attrKey);

        return Futures.transform(
                future,
                attributeKvEntryOpt -> attributeExist(attributeKvEntryOpt) ? attributeKvEntryOpt.get().getLongValue().orElse(0L) : 0L,
                dbCallbackExecutorService
        );
    }

    private boolean attributeExist(Optional<AttributeKvEntry> attributeKvEntryOpt) {
        return attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent();
    }

    protected void updateQueueStartTsSeqIdOffset(TenantId tenantId, String attrStartTsKey, String attrSeqIdKey, Long startTs, Long seqIdOffset) {
        log.trace(UPDATE_QUEUE_START_TS_SEQ_ID_OFFSET_MESSAGE + " [{}][{}][{}][{}]", attrStartTsKey, attrSeqIdKey, startTs, seqIdOffset);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(attrStartTsKey, startTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(attrSeqIdKey, seqIdOffset), System.currentTimeMillis())
        );

        attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes);
    }

    protected abstract String getTableName();

    protected abstract ListenableFuture<Long> getQueueSeqIdStart(TenantId tenantId);

    protected abstract PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);

    protected abstract ListenableFuture<Long> getQueueStartTs(TenantId tenantId);

    protected abstract void updateQueueStartTsSeqIdOffset(TenantId tenantId, Long newStartTs, Long newSeqId);

}
