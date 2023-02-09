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
package org.thingsboard.migrator.tenant.importing;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.tenant.Storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mode", havingValue = "CASSANDRA_DATA_IMPORT")
public class CassandraTenantDataImporter implements ApplicationRunner {

    private final Storage storage;
    private final CassandraTemplate cassandraTemplate;
    private final Map<PartitionKey, Set<Long>> partitions = new HashMap<>();

    @Value("${import.cassandra.ttl}")
    private int tsKvTtlDays;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        storage.readAndProcess("ts_kv", row -> {
            saveTsKv(row);
        });
        System.exit(0);
    }

    private void saveTsKv(Map<String, Object> row) {
        String entityType = (String) row.get("entity_type");
        UUID entityId = (UUID) row.get("entity_id");
        String key = (String) row.get("key");
        PartitionKey partitionKey = PartitionKey.of(entityType, entityId, key);
        Long partition = (Long) row.get("partition");
        long ttl = TimeUnit.DAYS.toSeconds(tsKvTtlDays);
        boolean newPartition = partitions.computeIfAbsent(partitionKey, k -> new HashSet<>()).add(partition);
        if (newPartition) {
            String query = "INSERT INTO test_ts_kv_partitions_cf " +
                    "(entity_type, entity_id, key, partition) VALUES (?, ?, ?, ?)";
            if (ttl > 0) {
                query += " USING TTL " + ttl;
            }
            cassandraTemplate.getCqlOperations().execute(query, entityType, entityId, key, partition);
            System.err.printf("EXECUTING CASSANDRA QUERY: (%s %s %s) %s\n", entityType, entityId, key, query);
        }

        Object longV = row.get("long_v");
        if (longV != null && !(longV instanceof Long)) {
            row.put("long_v", ((Number) longV).longValue());
        }
        Object dblV = row.get("dbl_v");
        if (dblV != null && !(dblV instanceof Double)) {
            row.put("dbl_v", ((Number) dblV).doubleValue());
        }

        String columnsStmt = String.join(", ", row.keySet());
        String valuesStmt = StringUtils.removeEnd("?,".repeat(row.size()), ",");
        String query = format("INSERT INTO test_ts_kv_cf (%s) VALUES (%s)", columnsStmt, valuesStmt);
        if (ttl > 0) {
            query += " USING TTL " + ttl;
        }

        System.err.printf("EXECUTING CASSANDRA QUERY: (%s %s %s) %s\n", entityType, entityId, key, query);
        cassandraTemplate.getCqlOperations().execute(query, row.values().toArray());
    }

    @Data(staticConstructor = "of")
    private static class PartitionKey {
        private final String entityType;
        private final UUID entityId;
        private final String key;
    }

}
