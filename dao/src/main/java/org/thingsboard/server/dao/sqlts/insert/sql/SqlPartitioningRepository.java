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
package org.thingsboard.server.dao.sqlts.insert.sql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.timeseries.SqlPartition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Repository
@Slf4j
public class SqlPartitioningRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SELECT_PARTITIONS_STMT = "SELECT tablename from pg_tables WHERE schemaname = 'public' and tablename like concat(?, '_%')";

    private static final int PSQL_VERSION_14 = 140000;
    private volatile Integer currentServerVersion;

    private final Map<String, Map<Long, SqlPartition>> tablesPartitions = new ConcurrentHashMap<>();
    private final ReentrantLock partitionCreationLock = new ReentrantLock();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void save(SqlPartition partition) {
        jdbcTemplate.execute(partition.getQuery());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED) // executing non-transactionally, so that parent transaction is not aborted on partition save error
    public void createPartitionIfNotExists(String table, long entityTs, long partitionDurationMs) {
        long partitionStartTs = calculatePartitionStartTime(entityTs, partitionDurationMs);
        Map<Long, SqlPartition> partitions = tablesPartitions.computeIfAbsent(table, t -> new ConcurrentHashMap<>());
        if (!partitions.containsKey(partitionStartTs)) {
            SqlPartition partition = new SqlPartition(table, partitionStartTs, getPartitionEndTime(partitionStartTs, partitionDurationMs), Long.toString(partitionStartTs));
            partitionCreationLock.lock();
            try {
                if (partitions.containsKey(partitionStartTs)) return;
                log.info("Saving partition {}-{} for table {}", partition.getStart(), partition.getEnd(), table);
                save(partition);
                log.trace("Adding partition to map: {}", partition);
                partitions.put(partition.getStart(), partition);
            } catch (Exception e) {
                String error = ExceptionUtils.getRootCauseMessage(e);
                if (StringUtils.containsAny(error, "would overlap partition", "already exists")) {
                    partitions.put(partition.getStart(), partition);
                    log.debug("Couldn't save partition {}-{} for table {}: {}", partition.getStart(), partition.getEnd(), table, error);
                } else {
                    log.warn("Couldn't save partition {}-{} for table {}: {}", partition.getStart(), partition.getEnd(), table, error);
                }
            } finally {
                partitionCreationLock.unlock();
            }
        }
    }

    public long dropPartitionsBefore(String table, long ts, long partitionDurationMs) {
        List<Long> partitions = fetchPartitions(table);
        long lastDroppedPartitionEndTime = -1;
        for (Long partitionStartTime : partitions) {
            long partitionEndTime = getPartitionEndTime(partitionStartTime, partitionDurationMs);
            if (partitionEndTime < ts) {
                log.info("[{}] Detaching expired partition: [{}-{}]", table, partitionStartTime, partitionEndTime);
                boolean success = detachAndDropPartition(table, partitionStartTime);
                if (success) {
                    log.info("[{}] Detached expired partition: {}", table, partitionStartTime);
                    lastDroppedPartitionEndTime = Math.max(partitionEndTime, lastDroppedPartitionEndTime);
                }
            } else {
                log.debug("[{}] Skipping valid partition: {}", table, partitionStartTime);
            }
        }
        return lastDroppedPartitionEndTime;
    }

    public void cleanupPartitionsCache(String table, long expTime, long partitionDurationMs) {
        Map<Long, SqlPartition> partitions = tablesPartitions.get(table);
        if (partitions == null) return;
        partitions.keySet().removeIf(startTime -> getPartitionEndTime(startTime, partitionDurationMs) < expTime);
    }

    private boolean detachAndDropPartition(String table, long partitionTs) {
        Map<Long, SqlPartition> cachedPartitions = tablesPartitions.get(table);
        if (cachedPartitions != null) cachedPartitions.remove(partitionTs);

        String tablePartition = table + "_" + partitionTs;
        String detachPsqlStmtStr = "ALTER TABLE " + table + " DETACH PARTITION " + tablePartition;

        // hotfix of ERROR: partition "integration_debug_event_1678323600000" already pending detach in partitioned table "public.integration_debug_event"
        // https://github.com/thingsboard/thingsboard/issues/8271
        // if (getCurrentServerVersion() >= PSQL_VERSION_14) {
        //    detachPsqlStmtStr += " CONCURRENTLY";
        // }

        String dropStmtStr = "DROP TABLE " + tablePartition;
        try {
            jdbcTemplate.execute(detachPsqlStmtStr);
            jdbcTemplate.execute(dropStmtStr);
            return true;
        } catch (DataAccessException e) {
            log.error("[{}] Error occurred trying to detach and drop the partition {} ", table, partitionTs, e);
        }
        return false;
    }

    private static long getPartitionEndTime(long startTime, long partitionDurationMs) {
        return startTime + partitionDurationMs;
    }

    public List<Long> fetchPartitions(String table) {
        List<Long> partitions = new ArrayList<>();
        List<String> partitionsTables = jdbcTemplate.queryForList(SELECT_PARTITIONS_STMT, String.class, table);
        for (String partitionTableName : partitionsTables) {
            String partitionTsStr = partitionTableName.substring(table.length() + 1);
            try {
                partitions.add(Long.parseLong(partitionTsStr));
            } catch (NumberFormatException nfe) {
                log.warn("Failed to parse table name: {}", partitionTableName);
            }
        }
        return partitions;
    }

    public long calculatePartitionStartTime(long ts, long partitionDuration) {
        return ts - (ts % partitionDuration);
    }

    private synchronized int getCurrentServerVersion() {
        if (currentServerVersion == null) {
            try {
                currentServerVersion = jdbcTemplate.queryForObject("SELECT current_setting('server_version_num')", Integer.class);
            } catch (Exception e) {
                log.warn("Error occurred during fetch of the server version", e);
            }
            if (currentServerVersion == null) {
                currentServerVersion = 0;
            }
        }
        return currentServerVersion;
    }

}
