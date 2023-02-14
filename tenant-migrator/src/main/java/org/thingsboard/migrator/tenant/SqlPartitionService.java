/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.migrator.tenant;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@ConfigurationProperties
public class SqlPartitionService {

    private final JdbcTemplate jdbcTemplate;
    @Setter
    private Map<String, Integer> partitionSizes;

    private final Map<Table, Set<Long>> partitions = new HashMap<>();

    public void createPartition(Table table, Map<String, Object> row) {
        long partitionSize = getPartitionSize(table);
        long ts = (long) row.get(table.getPartitionColumn());
        long partitionStart = ts - (ts % partitionSize);
        long partitionEnd = partitionStart + partitionSize;

        boolean newPartition = partitions.computeIfAbsent(table, t -> new HashSet<>()).add(partitionStart);
        if (newPartition) {
            String query = format("CREATE TABLE IF NOT EXISTS %s_%s PARTITION OF %s FOR VALUES FROM (%s) TO (%s)",
                    table.getName(), partitionStart, table.getName(), partitionStart, partitionEnd);
            System.err.println("EXECUTING QUERY: " + query);
            jdbcTemplate.execute(query);
        }
    }

    public Map<Long, Long> getPartitions(Table table) {
        long partitionSize = getPartitionSize(table);
        return jdbcTemplate.queryForList("SELECT tablename FROM pg_tables " +
                        "WHERE tablename LIKE '" + table.getName() + "_%'", String.class).stream()
                .map(partition -> StringUtils.substringAfterLast(partition, "_"))
                .map(Long::parseLong)
                .collect(Collectors.toMap(startTs -> startTs, startTs -> startTs + partitionSize));
    }

    private long getPartitionSize(Table table) {
        return TimeUnit.HOURS.toMillis(partitionSizes.get(table.getPartitionSizeSettingsKey()));
    }

}
