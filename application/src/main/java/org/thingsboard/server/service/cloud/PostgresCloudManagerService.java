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
package org.thingsboard.server.service.cloud;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@ConditionalOnExpression("'${queue.type:null}'!='kafka'")
public class PostgresCloudManagerService extends BaseCloudManagerService {

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private CloudEventService cloudEventService;

    private ExecutorService executor;
    private ExecutorService tsExecutor;

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (ServiceType.TB_CORE.equals(event.getServiceType())) {
            establishRpcConnection();
        }
    }

    @PostConstruct
    private void onInit() {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("postgres-cloud-manager"));
        tsExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("postgres-ts-cloud-manager"));
    }

    @PreDestroy
    protected void onDestroy() throws InterruptedException {
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
                if (initialized && !syncInProgress) {
                    if (isGeneralMsg || !isGeneralProcessInProgress) {
                        Long queueSeqIdStart = getLongAttrByKey(tenantId, queueSeqIdAttrKey).get();
                        TimePageLink pageLink = newCloudEventsAvailable(tenantId, queueSeqIdStart, queueStartTsAttrKey, finder);

                        if (pageLink != null) {
                            processUplinkMessages(pageLink, queueSeqIdStart, queueStartTsAttrKey, queueSeqIdAttrKey, isGeneralMsg, finder);
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

}
