/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.ttl.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ttl.AbstractCleanUpService;

import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Slf4j
@Service
@ConditionalOnExpression("${sql.ttl.cloud_events.enabled:true} && ${sql.ttl.cloud_events.cloud_events_ttl:0} > 0")
public class CloudEventsCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.cloud_events.execution_interval_ms})}";

    @Value("${sql.ttl.cloud_events.cloud_events_ttl}")
    private long ttl;

    @Value("${sql.cloud_events.partition_size:24}")
    private int partitionSizeInHours;

    @Value("${sql.ttl.cloud_events.enabled}")
    private boolean ttlTaskExecutionEnabled;

    private final CloudEventService cloudEventService;

    private final SqlPartitioningRepository partitioningRepository;

    public CloudEventsCleanUpService(PartitionService partitionService, CloudEventService cloudEventService, SqlPartitioningRepository partitioningRepository) {
        super(partitionService);
        this.cloudEventService = cloudEventService;
        this.partitioningRepository = partitioningRepository;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION, fixedDelayString = "${sql.ttl.cloud_events.execution_interval_ms}")
    public void cleanUp() {
        long cloudEventsExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttl);
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            cloudEventService.cleanupEvents(cloudEventsExpTime);
        } else {
            partitioningRepository.cleanupPartitionsCache(ModelConstants.CLOUD_EVENT_COLUMN_FAMILY_NAME, cloudEventsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }
}
