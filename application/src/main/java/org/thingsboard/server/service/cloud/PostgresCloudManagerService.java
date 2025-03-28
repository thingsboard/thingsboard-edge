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
                if (initialized) {
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
