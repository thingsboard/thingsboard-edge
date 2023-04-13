/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
