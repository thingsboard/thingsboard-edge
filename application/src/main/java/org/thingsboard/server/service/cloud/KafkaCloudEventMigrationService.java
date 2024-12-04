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
import com.google.common.util.concurrent.SettableFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventDao;
import org.thingsboard.server.dao.cloud.TsKvCloudEventDao;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCloudEventProvider;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.service.cloud.PostgresUplinkMessageService.FAILED_TO_FIND_CLOUD_EVENTS;
import static org.thingsboard.server.service.cloud.PostgresUplinkMessageService.FAILED_TO_UPDATE_QUEUE_OFFSET_ERROR_MESSAGE;
import static org.thingsboard.server.service.cloud.PostgresUplinkMessageService.QUEUE_END_TS_LESS_THAN_CURRENT_TIME_MESSAGE;
import static org.thingsboard.server.service.cloud.PostgresUplinkMessageService.QUEUE_OFFSET_WAS_UPDATED_MESSAGE;
import static org.thingsboard.server.service.cloud.PostgresUplinkMessageService.STARTED_NEW_CYCLE_MESSAGE;
import static org.thingsboard.server.service.cloud.PostgresUplinkMessageService.UPDATE_QUEUE_START_TS_SEQ_ID_OFFSET_MESSAGE;
import static org.thingsboard.server.service.cloud.QueueConstants.QUEUE_SEQ_ID_OFFSET_ATTR_KEY;
import static org.thingsboard.server.service.cloud.QueueConstants.QUEUE_START_TS_ATTR_KEY;
import static org.thingsboard.server.service.cloud.QueueConstants.QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY;
import static org.thingsboard.server.service.cloud.QueueConstants.QUEUE_TS_KV_START_TS_ATTR_KEY;

@Slf4j
@Service
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCloudEventMigrationService implements CloudEventMigrationService {
    private final TbCloudEventProvider tbCloudEventProvider;
    private final CloudEventDao cloudEventDao;
    private final TsKvCloudEventDao tsKvCloudEventDao;
    private final AttributesService attributesService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final CloudEventStorageSettings cloudEventStorageSettings;
    @Setter
    private TenantId tenantId;
    @Getter
    private boolean isMigrated = false;

    public KafkaCloudEventMigrationService(TbCloudEventProvider tbCloudEventProvider, CloudEventDao cloudEventDao,
                                           TsKvCloudEventDao tsKvCloudEventDao, AttributesService attributesService,
                                           DbCallbackExecutorService dbCallbackExecutorService, CloudEventStorageSettings cloudEventStorageSettings) {
        this.tbCloudEventProvider = tbCloudEventProvider;
        this.cloudEventDao = cloudEventDao;
        this.tsKvCloudEventDao = tsKvCloudEventDao;
        this.attributesService = attributesService;
        this.dbCallbackExecutorService = dbCallbackExecutorService;
        this.cloudEventStorageSettings = cloudEventStorageSettings;
    }

    @Override
    public void migrateUnprocessedEventToKafka(TenantId tenantId) {
        this.tenantId = tenantId;

        migrateCloudEvent();
        migrateTS();
        isMigrated = true;
    }

    private void migrateCloudEvent() {
        log.info("Sync cloud event to kafka started");

        Long cloudEventsQueueSeqIdStart = getQueueSeqIdStart(QUEUE_SEQ_ID_OFFSET_ATTR_KEY);
        TimePageLink cloudEventsPageLink = newCloudEventsAvailable(QUEUE_START_TS_ATTR_KEY, cloudEventsQueueSeqIdStart, cloudEventDao);

        if (cloudEventsPageLink != null) {
            processCloudEvents(cloudEventDao, cloudEventsQueueSeqIdStart, cloudEventsPageLink, false);
        }

        log.info("Sync cloud event to kafka finished");
    }

    private void migrateTS() {
        log.info("Sync cloud event TS to kafka started");

        Long cloudEventsQueueSeqIdStart = getQueueSeqIdStart(QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY);
        TimePageLink cloudEventsPageLink = newCloudEventsAvailable(QUEUE_TS_KV_START_TS_ATTR_KEY, cloudEventsQueueSeqIdStart, tsKvCloudEventDao);

        if (cloudEventsPageLink != null) {
            processCloudEvents(tsKvCloudEventDao, cloudEventsQueueSeqIdStart, cloudEventsPageLink, true);
        }

        log.info("Sync cloud event TS to kafka finished");
    }

    private ListenableFuture<Long> getLongAttrByKey(String attrKey) {
        ListenableFuture<Optional<AttributeKvEntry>> future = attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attrKey);

        return Futures.transform(
                future,
                attributeKvEntryOpt ->
                        attributeExist(attributeKvEntryOpt) ? attributeKvEntryOpt.get().getLongValue().orElse(0L) : 0L,
                dbCallbackExecutorService
        );
    }

    private boolean attributeExist(Optional<AttributeKvEntry> attributeKvEntryOpt) {
        return attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent();
    }

    public TimePageLink newCloudEventsAvailable(String seqIdStart, Long queueSeqIdStart, TsKvCloudEventDao cloudEventsDao) {
        try {
            TimePageLink pageLink = prepareTimePageLink(seqIdStart);

            PageData<CloudEvent> cloudEvents = cloudEventsDao.findCloudEvents(tenantId.getId(), queueSeqIdStart, null, pageLink);

            if (cloudEvents.getData().isEmpty()) {
                return getLastTimePageLink(cloudEventsDao, queueSeqIdStart, pageLink);
            } else {
                return pageLink;
            }
        } catch (Exception e) {
            log.warn(FAILED_TO_FIND_CLOUD_EVENTS, e);
            return null;
        }
    }

    private TimePageLink prepareTimePageLink(String seqIdStart) {
        int maxReadRecordsCount = cloudEventStorageSettings.getMaxReadRecordsCount();
        long queueStartTs = getQueueSeqIdStart(seqIdStart);
        long queueEndTs = queueStartTs > 0 ? queueStartTs + TimeUnit.DAYS.toMillis(1) : System.currentTimeMillis();

        return new TimePageLink(maxReadRecordsCount, 0, null, null, queueStartTs, queueEndTs);
    }

    private TimePageLink getLastTimePageLink(TsKvCloudEventDao dao, Long queueSeqIdStart, TimePageLink pageLink) {
        PageData<CloudEvent> cloudEvents = findCloudEventsFromBeginning(dao, pageLink);

        if (cloudEvents.getData().stream().noneMatch(ce -> ce.getSeqId() == 1)) {
            return findFromQueueEndToToday(dao, queueSeqIdStart, pageLink.getEndTime());
        } else {
            log.info(STARTED_NEW_CYCLE_MESSAGE);
            return pageLink;
        }
    }

    private Long getQueueSeqIdStart(String attribute) {
        try {
            return getLongAttrByKey(attribute).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private PageData<CloudEvent> findCloudEventsFromBeginning(TsKvCloudEventDao dao, TimePageLink pageLink) {
        long seqIdEnd = Integer.toUnsignedLong(cloudEventStorageSettings.getMaxReadRecordsCount());
        seqIdEnd = Math.max(seqIdEnd, 50L);

        return dao.findCloudEvents(tenantId.getId(), 0L, seqIdEnd, pageLink);
    }

    private TimePageLink findFromQueueEndToToday(TsKvCloudEventDao dao, Long queueSeqIdStart, long queueEndTs) {
        long queueStartTs;

        while (queueEndTs < System.currentTimeMillis()) {
            log.trace(QUEUE_END_TS_LESS_THAN_CURRENT_TIME_MESSAGE + " [{}] [{}]", queueEndTs, System.currentTimeMillis());
            queueStartTs = queueEndTs;
            queueEndTs = queueEndTs + TimeUnit.DAYS.toMillis(1);
            TimePageLink pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                    0, null, null, queueStartTs, queueEndTs);

            PageData<CloudEvent> cloudEvents = dao.findCloudEvents(tenantId.getId(), queueSeqIdStart, null, pageLink);

            if (!cloudEvents.getData().isEmpty()) {
                return pageLink;
            }
        }

        return null;
    }

    public void processCloudEvents(TsKvCloudEventDao cloudEventDao, Long queueSeqIdStart, TimePageLink pageLink, boolean isTs) {
        PageData<CloudEvent> cloudEvents;

        do {
            cloudEvents = prepareCloudEvents(cloudEventDao, queueSeqIdStart, pageLink);

            sendCloudEvent(isTs, cloudEvents);

            pageLink = prepareNextPageLink(isTs, cloudEvents, pageLink);
        } while (cloudEvents.hasNext());
    }

    private PageData<CloudEvent> prepareCloudEvents(TsKvCloudEventDao cloudEventDao, Long queueSeqIdStart, TimePageLink pageLink) {
        PageData<CloudEvent> cloudEvents = cloudEventDao.findCloudEvents(tenantId.getId(), queueSeqIdStart, null, pageLink);

        if (cloudEvents.getData().isEmpty()) {
            return findCloudEventsFromBeginning(cloudEventDao, pageLink);
        }

        return cloudEvents;
    }

    private TimePageLink prepareNextPageLink(boolean isTs, PageData<CloudEvent> cloudEvents, TimePageLink pageLink) {
        if (cloudEvents.getTotalElements() > 0) {
            chooseAnotherStartTs(isTs, cloudEvents);
        }

        return pageLink.nextPageLink();
    }

    private void chooseAnotherStartTs(boolean isTs, PageData<CloudEvent> cloudEvents) {
        CloudEvent latestCloudEvent = cloudEvents.getData().get(cloudEvents.getData().size() - 1);

        try {
            Long newStartTs = Uuids.unixTimestamp(latestCloudEvent.getUuidId());
            updateQueueStartTsSeqIdOffset(isTs, newStartTs, latestCloudEvent.getSeqId());
            log.debug(QUEUE_OFFSET_WAS_UPDATED_MESSAGE + " [{}][{}][{}]", latestCloudEvent.getUuidId(), newStartTs, latestCloudEvent.getSeqId());
        } catch (Exception e) {
            log.error(FAILED_TO_UPDATE_QUEUE_OFFSET_ERROR_MESSAGE + " [{}]", latestCloudEvent);
        }
    }

    protected void updateQueueStartTsSeqIdOffset(boolean isTs, Long newStartTs, Long newSeqId) {
        String startTs = isTs ? QUEUE_TS_KV_START_TS_ATTR_KEY : QUEUE_START_TS_ATTR_KEY;
        String offset = isTs ? QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY : QUEUE_SEQ_ID_OFFSET_ATTR_KEY;

        log.trace(UPDATE_QUEUE_START_TS_SEQ_ID_OFFSET_MESSAGE + " [{}][{}][{}][{}]", startTs, offset, newStartTs, newSeqId);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(startTs, newStartTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(offset, newSeqId), System.currentTimeMillis())
        );

        attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes);
    }

    private void sendCloudEvent(boolean isTs, PageData<CloudEvent> events) {
        try {
            sendCloudEventToTopicAsync(isTs, events.getData()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private ListenableFuture<Void> sendCloudEventToTopicAsync(boolean isTs, List<CloudEvent> cloudEvents) {
        SettableFuture<Void> futureToSet = SettableFuture.create();

        CompletableFuture.runAsync(() -> {
            try {
                cloudEvents.forEach(cloudEvent -> sendCloudEventToTopic(isTs, cloudEvent));
                futureToSet.set(null);
            } catch (Exception e) {
                futureToSet.setException(e);
            }
        });

        return futureToSet;
    }

    private void sendCloudEventToTopic(boolean isTs, CloudEvent cloudEvent) {
        TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> producer = chooseProducer(isTs);
        TopicPartitionInfo tpi = new TopicPartitionInfo(producer.getDefaultTopic(), cloudEvent.getTenantId(), 1, true);

        TransportProtos.CloudEventMsgProto cloudEventMsgProto = ProtoUtils.toProto(cloudEvent);
        TransportProtos.ToCloudEventMsg toCloudEventMsg = TransportProtos.ToCloudEventMsg.newBuilder().setCloudEventMsg(cloudEventMsgProto).build();

        UUID entityId = cloudEvent.getEntityId() == null ? UUID.fromString(cloudEvent.getEntityBody().get("from").get("id").asText()) : cloudEvent.getEntityId();

        TbProtoQueueMsg<TransportProtos.ToCloudEventMsg> cloudEventMsg = new TbProtoQueueMsg<>(entityId, toCloudEventMsg);

        producer.send(tpi, cloudEventMsg, null);
    }

    private TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> chooseProducer(boolean isTS) {
        return isTS ? tbCloudEventProvider.getCloudEventTSMsgProducer() : tbCloudEventProvider.getCloudEventMsgProducer();
    }

}
