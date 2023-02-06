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
package org.thingsboard.server.dao.sql.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;

import java.util.concurrent.TimeUnit;


@Slf4j
@Repository
public class SqlEventCleanupRepository extends JpaAbstractDaoListeningExecutorService implements EventCleanupRepository {

    @Autowired
    private EventPartitionConfiguration partitionConfiguration;
    @Autowired
    private SqlPartitioningRepository partitioningRepository;

    @Override
    public void cleanupEvents(long eventExpTime, boolean debug) {
        for (EventType eventType : EventType.values()) {
            if (eventType.isDebug() == debug) {
                cleanupEvents(eventType, eventExpTime);
            }
        }
    }

    @Override
    public void migrateEvents(long regularEventTs, long debugEventTs) {
        regularEventTs = Math.max(regularEventTs, 1480982400000L);
        debugEventTs = Math.max(debugEventTs, 1480982400000L);

        callMigrateFunctionByPartitions("regular", "migrate_regular_events", regularEventTs, partitionConfiguration.getRegularPartitionSizeInHours());
        callMigrateFunctionByPartitions("debug", "migrate_debug_events", debugEventTs, partitionConfiguration.getDebugPartitionSizeInHours());

        try {
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS migrate_regular_events(bigint, bigint, int)");
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS migrate_debug_events(bigint, bigint, int)");
            jdbcTemplate.execute("DROP TABLE IF EXISTS event");
        } catch (DataAccessException e) {
            log.error("Error occurred during drop of the `events` table", e);
            throw e;
        }
    }

    private void callMigrateFunctionByPartitions(String logTag, String functionName, long startTs, int partitionSizeInHours) {
        long currentTs = System.currentTimeMillis();
        var regularPartitionStepInMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        long numberOfPartitions = (currentTs - startTs) / regularPartitionStepInMs;
        if (numberOfPartitions > 1000) {
            log.error("Please adjust your {} events partitioning configuration. " +
                            "Configuration with partition size of {} hours and corresponding TTL will use {} (>1000) partitions which is not recommended!",
                    logTag, partitionSizeInHours, numberOfPartitions);
            throw new RuntimeException("Please adjust your " + logTag + " events partitioning configuration. " +
                    "Configuration with partition size of " + partitionSizeInHours + " hours and corresponding TTL will use " +
                    +numberOfPartitions + " (>1000) partitions which is not recommended!");
        }
        while (startTs < currentTs) {
            var endTs = startTs + regularPartitionStepInMs;
            log.info("Migrate {} events for time period: [{},{}]", logTag, startTs, endTs);
            callMigrateFunction(functionName, startTs, startTs + regularPartitionStepInMs, partitionSizeInHours);
            startTs = endTs;
        }
        log.info("Migrate {} events done.", logTag);
    }

    private void callMigrateFunction(String functionName, long startTs, long endTs, int partitionSizeInHours) {
        try {
            jdbcTemplate.update("CALL " + functionName + "(?, ?, ?)", startTs, endTs, partitionSizeInHours);
        } catch (DataAccessException e) {
            if (e.getMessage() == null || !e.getMessage().contains("relation \"event\" does not exist")) {
                log.error("[{}] SQLException occurred during execution of {} with parameters {} and {}", functionName, startTs, partitionSizeInHours, e);
                throw new RuntimeException(e);
            }
        }
    }

    private void cleanupEvents(EventType eventType, long eventExpTime) {
        partitioningRepository.dropPartitionsBefore(eventType.getTable(), eventExpTime, partitionConfiguration.getPartitionSizeInMs(eventType));
    }

}
