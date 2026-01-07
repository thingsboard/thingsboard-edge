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
package org.thingsboard.server.service.cloud.event.runner;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.service.cloud.CloudEventFinder;
import org.thingsboard.server.service.cloud.event.postgres.BasePostgresCloudEventUplinkBatchDispatcher;
import org.thingsboard.server.service.cloud.event.postgres.PostgresCloudEventUplinkRetriever;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
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
@ConditionalOnExpression("'${queue.type:null}'!='kafka'")
public class PostgresCloudEventUplinkProcessingRunner implements CloudEventUplinkProcessingRunner {

    private final BasePostgresCloudEventUplinkBatchDispatcher cloudEventUplinkBatchDispatcher;
    private final PostgresCloudEventUplinkRetriever cloudEventUplinkRetriever;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final CloudEventStorageSettings cloudEventStorageSettings;
    private final CloudEventService cloudEventService;
    private final AttributesService attributesService;
    private final EdgeInfoHolder edgeInfo;

    private ExecutorService executor;
    private ExecutorService tsExecutor;

    @Override
    public void init() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("postgres-cloud-manager"));
            executor.submit(() -> launchUplinkProcessing(QUEUE_START_TS_ATTR_KEY, QUEUE_SEQ_ID_OFFSET_ATTR_KEY, true,
                    cloudEventService::findCloudEvents));
        }
        if (tsExecutor == null) {
            tsExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("postgres-ts-cloud-manager"));
            tsExecutor.submit(() -> launchUplinkProcessing(QUEUE_TS_KV_START_TS_ATTR_KEY, QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY, false,
                    cloudEventService::findTsKvCloudEvents));
        }
    }

    @Override
    @PreDestroy
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
        if (tsExecutor != null && !tsExecutor.isShutdown()) {
            tsExecutor.shutdownNow();
            tsExecutor = null;
        }
    }

    private void launchUplinkProcessing(String queueStartTsAttrKey, String queueSeqIdAttrKey, boolean isGeneralMsg, CloudEventFinder finder) {
        while (!Thread.interrupted()) {
            try {
                if (edgeInfo.isInitialized() && !edgeInfo.isSyncInProgress()) {
                    if (isGeneralMsg || !edgeInfo.isGeneralProcessInProgress()) {
                        Long queueSeqIdStart = getLongAttrByKey(edgeInfo.getTenantId(), queueSeqIdAttrKey).get();
                        TimePageLink pageLink = cloudEventUplinkRetriever.newCloudEventsAvailable(edgeInfo.getTenantId(), queueSeqIdStart, queueStartTsAttrKey, finder);

                        if (pageLink != null) {
                            cloudEventUplinkBatchDispatcher.processUplinkMessages(pageLink, queueSeqIdStart, queueStartTsAttrKey, queueSeqIdAttrKey, isGeneralMsg, finder);
                        } else {
                            log.trace("no new cloud events found for queue, isGeneralMsg = {}", isGeneralMsg);
                            sleep();
                        }
                    }
                } else {
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (Exception e) {
                log.warn("Failed to process messages handling!", e);
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(cloudEventStorageSettings.getNoRecordsSleepInterval());
        } catch (InterruptedException e) {
            log.error("Error during sleep", e);
        }
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
}
