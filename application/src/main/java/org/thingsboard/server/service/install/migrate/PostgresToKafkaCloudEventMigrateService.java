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
package org.thingsboard.server.service.install.migrate;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventDao;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.cloud.TsKvCloudEventDao;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCloudEventProvider;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.service.cloud.PostgresUplinkMessageService.QUEUE_END_TS_LESS_THAN_CURRENT_TIME_MESSAGE;
import static org.thingsboard.server.service.cloud.QueueConstants.QUEUE_SEQ_ID_OFFSET_ATTR_KEY;
import static org.thingsboard.server.service.cloud.QueueConstants.QUEUE_START_TS_ATTR_KEY;

@Service
@Profile("install")
@Slf4j
public class PostgresToKafkaCloudEventMigrateService implements CloudEventMigrateService {
    private final TbCloudEventProvider tbCloudEventProvider;
    private final CloudEventDao cloudEventDao;
    private final TsKvCloudEventDao tsKvCloudEventDao;
    private final AttributesService attributesService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final CloudEventStorageSettings cloudEventStorageSettings;
    private final Tenant tenant;

    public PostgresToKafkaCloudEventMigrateService(TbCloudEventProvider tbCloudEventProvider, CloudEventDao cloudEventDao,
                                                   TsKvCloudEventDao tsKvCloudEventDao, AttributesService attributesService,
                                                   TenantService tenantService, DbCallbackExecutorService dbCallbackExecutorService, CloudEventService cloudEventService, CloudEventStorageSettings cloudEventStorageSettings) {
        this.tbCloudEventProvider = tbCloudEventProvider;
        this.cloudEventDao = cloudEventDao;
        this.tsKvCloudEventDao = tsKvCloudEventDao;
        this.attributesService = attributesService;
        this.dbCallbackExecutorService = dbCallbackExecutorService;
        tenant = new PageDataIterable<>(tenantService::findTenants, 10).next();
        this.cloudEventStorageSettings = cloudEventStorageSettings;
    }

    @Override
    public void migrateCloudEvent() {
        log.info("Migrate cloud event to kafka started");

        while (true) {
            PageData<CloudEvent> cloudEvents = getCloudEventFromDB(cloudEventDao);
            if (!cloudEvents.getData().isEmpty()) {
                cloudEvents.getData().forEach(this::sendCloudEvent);
            } else {
                break;
            }
        }

        log.info("Migrate cloud event to kafka finished");
    }

    @Override
    public void migrateCloudEventTS() {
        log.info("Migrate cloud event ts to kafka started");

        while (true) {
            PageData<CloudEvent> cloudEventsTS = getCloudEventFromDB(tsKvCloudEventDao);
            if (!cloudEventsTS.getData().isEmpty()) {
                cloudEventsTS.getData().forEach(this::sendCloudEventTS);
            } else {
                break;
            }
        }
        log.info("Migrate cloud event ts to kafka finished");
    }

    @NotNull
    private PageData<CloudEvent> getCloudEventFromDB(TsKvCloudEventDao tsKvCloudEventDao) {
        try {
            int maxReadRecordsCount = cloudEventStorageSettings.getMaxReadRecordsCount();
            Long queueSeqIdStart = getSeqId(QUEUE_SEQ_ID_OFFSET_ATTR_KEY).get();
            long queueStartTs = getSeqId(QUEUE_START_TS_ATTR_KEY).get();
            long queueEndTs = queueStartTs > 0 ? queueStartTs + TimeUnit.DAYS.toMillis(1) : System.currentTimeMillis();
            TimePageLink pageLink =
                    new TimePageLink(maxReadRecordsCount, 0, null, null, queueStartTs, queueEndTs);

            PageData<CloudEvent> cloudEvents = tsKvCloudEventDao.findCloudEvents(tenant.getTenantId().getId(), queueSeqIdStart, null, pageLink);

            if (cloudEvents.getData().isEmpty()) {
                // check if new cycle started (seq_id starts from '1')
                cloudEvents = getCloudEventFromBeginning(tsKvCloudEventDao, pageLink, queueEndTs, cloudEvents, queueSeqIdStart);
            }

            return cloudEvents;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private ListenableFuture<Long> getSeqId(String attribute) {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(tenant.getTenantId(), tenant.getTenantId(), AttributeScope.SERVER_SCOPE, attribute);

        return Futures.transform(
                future,
                attributeKvEntryOpt -> attributeExist(attributeKvEntryOpt) ? attributeKvEntryOpt.get().getLongValue().orElse(0L) : 0L,
                dbCallbackExecutorService
        );
    }

    private boolean attributeExist(Optional<AttributeKvEntry> attributeKvEntryOpt) {
        return attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent();
    }

    private PageData<CloudEvent> getCloudEventFromBeginning(TsKvCloudEventDao tsKvCloudEventDao, TimePageLink pageLink, long queueEndTs, PageData<CloudEvent> cloudEvents, Long queueSeqIdStart) {
        long seqIdEnd = Integer.toUnsignedLong(cloudEventStorageSettings.getMaxReadRecordsCount());
        seqIdEnd = Math.max(seqIdEnd, 50L);

        PageData<CloudEvent> cloudEventsTemp = tsKvCloudEventDao.findCloudEvents(tenant.getTenantId().getId(), 0L, seqIdEnd, pageLink);

        if (cloudEventsTemp.getData().stream().noneMatch(ce -> ce.getSeqId() == 1)) {
            long queueStartTs2;

            while (queueEndTs < System.currentTimeMillis()) {
                log.trace(QUEUE_END_TS_LESS_THAN_CURRENT_TIME_MESSAGE + " [{}] [{}]", queueEndTs, System.currentTimeMillis());
                queueStartTs2 = queueEndTs;
                queueEndTs = queueEndTs + TimeUnit.DAYS.toMillis(1);
                TimePageLink pageLink2 = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                        0, null, null, queueStartTs2, queueEndTs);

                cloudEvents = tsKvCloudEventDao.findCloudEvents(tenant.getTenantId().getId(), queueSeqIdStart, null, pageLink2);

                if (!cloudEvents.getData().isEmpty()) {
                    break;
                }
            }
        }
        return cloudEvents;
    }

    private void sendCloudEvent(CloudEvent event) {
        try {
            sendCloudEventToTopicAsync(event, false).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendCloudEventTS(CloudEvent event) {
        try {
            sendCloudEventToTopicAsync(event, true).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private ListenableFuture<Void> sendCloudEventToTopicAsync(CloudEvent cloudEvent, boolean isTS) {
        SettableFuture<Void> futureToSet = SettableFuture.create();

        CompletableFuture.runAsync(() -> {
            try {
                sendCloudEventToTopic(cloudEvent, isTS);
                futureToSet.set(null);
            } catch (Exception e) {
                futureToSet.setException(e);
            }
        });

        return futureToSet;
    }

    private void sendCloudEventToTopic(CloudEvent cloudEvent, boolean isTS) {
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> producer = chooseProducer(isTS);
        TopicPartitionInfo tpi = new TopicPartitionInfo(producer.getDefaultTopic(), cloudEvent.getTenantId(), 1, true);

        TransportProtos.EdgeEventMsgProto cloudEventMsgProto = ProtoUtils.toProto(cloudEvent);
        TransportProtos.ToCloudEventMsg toCloudEventMsg = TransportProtos.ToCloudEventMsg.newBuilder().setCloudEventMsg(cloudEventMsgProto).build();

        UUID entityId = cloudEvent.getEntityId() == null ? UUID.fromString(cloudEvent.getEntityBody().get("from").get("id").asText()) : cloudEvent.getEntityId();

        TbProtoQueueMsg<TransportProtos.ToCloudEventMsg> cloudEventMsg = new TbProtoQueueMsg<>(entityId, toCloudEventMsg);

        producer.send(tpi, cloudEventMsg, null);
    }

    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> chooseProducer(boolean isTS) {
        return isTS ? tbCloudEventProvider.getCloudEventTSMsgProducer() : tbCloudEventProvider.getCloudEventMsgProducer();
    }

}
