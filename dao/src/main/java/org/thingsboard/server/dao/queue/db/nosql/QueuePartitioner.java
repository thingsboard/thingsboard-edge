/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.queue.db.nosql;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.queue.db.repository.ProcessedPartitionRepository;
import org.thingsboard.server.dao.timeseries.TsPartitionDate;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@NoSqlDao
public class QueuePartitioner {

    private final TsPartitionDate tsFormat;
    private ProcessedPartitionRepository processedPartitionRepository;
    private Clock clock = Clock.systemUTC();

    public QueuePartitioner(@Value("${cassandra.queue.partitioning}") String partitioning,
                            ProcessedPartitionRepository processedPartitionRepository) {
        this.processedPartitionRepository = processedPartitionRepository;
        Optional<TsPartitionDate> partition = TsPartitionDate.parse(partitioning);
        if (partition.isPresent()) {
            tsFormat = partition.get();
        } else {
            log.warn("Incorrect configuration of partitioning {}", partitioning);
            throw new RuntimeException("Failed to parse partitioning property: " + partitioning + "!");
        }
    }

    public long getPartition(long ts) {
        //TODO: use TsPartitionDate.truncateTo?
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return tsFormat.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public List<Long> findUnprocessedPartitions(UUID nodeId, long clusteredHash) {
        Optional<Long> lastPartitionOption = processedPartitionRepository.findLastProcessedPartition(nodeId, clusteredHash);
        long lastPartition = lastPartitionOption.orElse(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7));
        List<Long> unprocessedPartitions = Lists.newArrayList();

        LocalDateTime current = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastPartition), ZoneOffset.UTC);
        LocalDateTime end = LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC)
                .plus(1L, tsFormat.getTruncateUnit());

        while (current.isBefore(end)) {
            current = current.plus(1L, tsFormat.getTruncateUnit());
            unprocessedPartitions.add(tsFormat.truncatedTo(current).toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        return unprocessedPartitions;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void checkProcessedPartitions() {
        //todo-vp: we need to implement this
    }
}
