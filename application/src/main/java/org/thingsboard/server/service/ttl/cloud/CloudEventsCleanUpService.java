// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ttl.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.cloud.CloudEventDao;
import org.thingsboard.server.dao.cloud.TsKvCloudEventDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ttl.AbstractCleanUpService;

import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Slf4j
@Service
@ConditionalOnExpression("${sql.ttl.cloud_events.enabled:true} && ${sql.ttl.cloud_events.cloud_events_ttl:0} > 0 && '${queue.type:null}' != 'kafka'")
public class CloudEventsCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.cloud_events.execution_interval_ms})}";

    @Value("${sql.ttl.cloud_events.cloud_events_ttl}")
    private long ttl;

    @Value("${sql.cloud_events.partition_size:24}")
    private int partitionSizeInHours;

    @Value("${sql.ttl.cloud_events.enabled}")
    private boolean ttlTaskExecutionEnabled;


    private final CloudEventDao cloudEventDao;
    private final TsKvCloudEventDao tsKvCloudEventDao;
    private final SqlPartitioningRepository partitioningRepository;


    public CloudEventsCleanUpService(PartitionService partitionService,
                                     CloudEventDao cloudEventDao,
                                     TsKvCloudEventDao tsKvCloudEventDao,
                                     SqlPartitioningRepository partitioningRepository) {
        super(partitionService);
        this.cloudEventDao = cloudEventDao;
        this.tsKvCloudEventDao = tsKvCloudEventDao;
        this.partitioningRepository = partitioningRepository;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION, fixedDelayString = "${sql.ttl.cloud_events.execution_interval_ms}")
    public void cleanUp() {
        long cloudEventsExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttl);
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            cloudEventDao.cleanupEvents(cloudEventsExpTime);
            tsKvCloudEventDao.cleanupEvents(cloudEventsExpTime);
        } else {
            // clean up 'cloud_event'
            partitioningRepository.cleanupPartitionsCache(ModelConstants.CLOUD_EVENT_COLUMN_FAMILY_NAME, cloudEventsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
            // clean up 'ts_kv_cloud_event'
            partitioningRepository.cleanupPartitionsCache(ModelConstants.TS_KV_CLOUD_EVENT_COLUMN_FAMILY_NAME, cloudEventsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

}
