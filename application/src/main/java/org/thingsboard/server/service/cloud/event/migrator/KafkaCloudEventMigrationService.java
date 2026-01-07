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
package org.thingsboard.server.service.cloud.event.migrator;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.service.cloud.CloudEventFinder;
import org.thingsboard.server.service.cloud.PostgresCloudEventService;
import org.thingsboard.server.service.cloud.event.postgres.PostgresCloudEventUplinkRetriever;
import org.thingsboard.server.service.cloud.event.postgres.PostgresToKafkaCloudEventUplinkMigrationDispatcher;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.edge.EdgeConstants.QUEUE_SEQ_ID_OFFSET_ATTR_KEY;
import static org.thingsboard.server.common.data.edge.EdgeConstants.QUEUE_START_TS_ATTR_KEY;
import static org.thingsboard.server.common.data.edge.EdgeConstants.QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY;
import static org.thingsboard.server.common.data.edge.EdgeConstants.QUEUE_TS_KV_START_TS_ATTR_KEY;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCloudEventMigrationService implements CloudEventMigrationService {

    private final PostgresCloudEventService postgresCloudEventService;
    private final PostgresToKafkaCloudEventUplinkMigrationDispatcher postgresToKafkaMigrationEventsDispatcher;
    private final PostgresCloudEventUplinkRetriever postgresCloudEventUplinkRetriever;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final AttributesService attributesService;
    private final EdgeInfoHolder edgeInfo;

    private ExecutorService executor;
    private volatile boolean isMigrated = false;
    private volatile boolean isTsMigrated = false;

    @PostConstruct
    private void onInit() {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("postgres-cloud-migrator"));
    }

    @PreDestroy
    private void onDestroy() {
        shutdownSafely();
    }

    @Override
    public boolean isMigrationRequired() {
        return (!isMigrated || !isTsMigrated);
    }

    @Override
    public void migrateUnprocessedEventToKafka() {
        executor.submit(() -> {
            try {
                while (!Thread.interrupted()) {
                    if (!edgeInfo.isInitialized()) {
                        TimeUnit.SECONDS.sleep(1);
                        continue;
                    }

                    processMigration();

                    if (isMigrated && isTsMigrated) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to process messages handling!", e);
            } finally {
                shutdownSafely();
            }
        });
    }

    private void processMigration() throws Exception {
        if (!isMigrated) {
            CloudEventFinder finder = postgresCloudEventService::findCloudEvents;
            boolean isGeneralMsg = true;
            isMigrated = launchCloudEventProcessing(QUEUE_SEQ_ID_OFFSET_ATTR_KEY, QUEUE_START_TS_ATTR_KEY, isGeneralMsg, finder);
        }

        if (!isTsMigrated) {
            CloudEventFinder finder = postgresCloudEventService::findTsKvCloudEvents;
            boolean isGeneralMsg = false;
            isTsMigrated = launchCloudEventProcessing(QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY, QUEUE_TS_KV_START_TS_ATTR_KEY, isGeneralMsg, finder);
        }
    }

    private boolean launchCloudEventProcessing(String seqIdKey, String startTsKey, boolean isGeneralMsg, CloudEventFinder finder) throws Exception {
        Long queueSeqIdStart = getLongAttrByKey(edgeInfo.getTenantId(), seqIdKey).get();
        TimePageLink pageLink = postgresCloudEventUplinkRetriever.newCloudEventsAvailable(edgeInfo.getTenantId(), queueSeqIdStart, startTsKey, finder);

        if (pageLink != null) {
            postgresToKafkaMigrationEventsDispatcher.processUplinkMessages(pageLink, queueSeqIdStart, startTsKey, seqIdKey, isGeneralMsg, finder);
            return false;
        }

        return true;
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

    private void shutdownSafely() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
