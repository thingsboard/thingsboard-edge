/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {}

    @Override
    protected void launchUplinkProcessing() {}

    @PreDestroy
    private void onDestroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

}
