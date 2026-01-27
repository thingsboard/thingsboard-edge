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
package org.thingsboard.server.service.cloud;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCloudEventMigrationService extends BaseCloudManagerService implements CloudEventMigrationService {

    @Autowired
    private CloudEventService kafkaEventService;

    @Autowired
    @Qualifier("postgresCloudEventService")
    private CloudEventService postgresCloudEventService;

    @Getter
    private volatile boolean isMigrated = false;

    @Getter
    private volatile boolean isTsMigrated = false;

    private ExecutorService executor;

    @PostConstruct
    private void onInit() {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("postgres-cloud-migrator"));
    }

    @Override
    public void migrateUnprocessedEventToKafka() {
        executor.submit(() -> {
            try {
                while (!Thread.interrupted()) {
                    if (!initialized) {
                        TimeUnit.SECONDS.sleep(1);
                        continue;
                    }

                    processMigration();

                    if (isMigrated && isTsMigrated) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to process messages handling!", e);
            } finally {
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdown();
                }
            }
        });
    }

    private void processMigration() throws Exception {
        if (!isMigrated) {
            CloudEventFinder finder = (tenantId, seqIdStart, seqIdEnd, link) -> postgresCloudEventService.findCloudEvents(tenantId, seqIdStart, seqIdEnd, link);
            isMigrated = launchCloudEventProcessing(QUEUE_SEQ_ID_OFFSET_ATTR_KEY, QUEUE_START_TS_ATTR_KEY, true, finder);
        }

        if (!isTsMigrated) {
            CloudEventFinder finder = (tenantId, seqIdStart, seqIdEnd, link) -> postgresCloudEventService.findTsKvCloudEvents(tenantId, seqIdStart, seqIdEnd, link);
            isTsMigrated = launchCloudEventProcessing(QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY, QUEUE_TS_KV_START_TS_ATTR_KEY, false, finder);
        }
    }

    private boolean launchCloudEventProcessing(String seqIdKey, String startTsKey, boolean isGeneralMsg, CloudEventFinder finder) throws Exception {
        Long queueSeqIdStart = getLongAttrByKey(tenantId, seqIdKey).get();
        TimePageLink pageLink = newCloudEventsAvailable(tenantId, queueSeqIdStart, startTsKey, finder);

        if (pageLink != null) {
            processUplinkMessages(pageLink, queueSeqIdStart, startTsKey, seqIdKey, isGeneralMsg, finder);
            return false;
        }

        return true;
    }

    @Override
    protected ListenableFuture<Boolean> processCloudEvents(List<CloudEvent> cloudEvents, boolean isGeneralMsg) {
        for (CloudEvent cloudEvent : cloudEvents) {
            if (isGeneralMsg) {
                kafkaEventService.saveAsync(cloudEvent);
            } else {
                kafkaEventService.saveTsKvAsync(cloudEvent);
            }
        }
        return Futures.immediateFuture(Boolean.FALSE);
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {}

    @Override
    protected void launchUplinkProcessing() {}

    @PreDestroy
    protected void onDestroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

}
