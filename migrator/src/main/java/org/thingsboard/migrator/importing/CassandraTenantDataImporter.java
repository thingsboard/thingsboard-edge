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
package org.thingsboard.migrator.importing;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.BaseMigrationService;
import org.thingsboard.migrator.config.Modes;
import org.thingsboard.migrator.exporting.CassandraTenantDataExporter;
import org.thingsboard.migrator.utils.CassandraService;
import org.thingsboard.migrator.utils.Storage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mode", havingValue = Modes.CASSANDRA_TENANT_DATA_IMPORT)
public class CassandraTenantDataImporter extends BaseMigrationService {

    private final Storage storage;
    private final CassandraService cassandraService;

    private final Map<PartitionKey, Set<Long>> partitions = new WeakHashMap<>();

    @Value("${import.cassandra.ttl}")
    private int tsKvTtlDays;

    @Override
    protected void start() throws Exception {
        storage.readAndProcess(CassandraTenantDataExporter.TS_KV_FILE, true, row -> {
            try {
                saveTsKv(row);
            } catch (Exception e) {
                System.err.println("Failed to save row: " + row);
                throw e;
            }
        });
    }

    private void saveTsKv(Map<String, Object> row) {
        String entityType = (String) row.get("entity_type");
        UUID entityId = (UUID) row.get("entity_id");
        String key = (String) row.get("key");
        PartitionKey partitionKey = PartitionKey.of(entityType, entityId, key);
        Long partition = ((Number) row.get("partition")).longValue();
        row.put("partition", partition);
        long ttl = TimeUnit.DAYS.toSeconds(tsKvTtlDays);
        boolean newPartition = partitions.computeIfAbsent(partitionKey, k -> new HashSet<>()).add(partition);
        if (newPartition) {
            String query = "INSERT INTO ts_kv_partitions_cf " +
                    "(entity_type, entity_id, key, partition) VALUES (?, ?, ?, ?)";
            if (ttl > 0) {
                query += " USING TTL " + ttl;
            }
            String finalQuery = query;
            executor.submit(() -> {
                cassandraService.execute(finalQuery, entityType, entityId, key, partition);
            });
        }

        Object longV = row.get("long_v");
        if (longV != null && !(longV instanceof Long)) {
            row.put("long_v", ((Number) longV).longValue());
        }
        Object dblV = row.get("dbl_v");
        if (dblV != null && !(dblV instanceof Double)) {
            row.put("dbl_v", ((Number) dblV).doubleValue());
        }
        Number ts = (Number) row.get("ts");
        ts = ts.longValue();
        row.put("ts", ts);

        String columnsStmt = String.join(", ", row.keySet());
        String valuesStmt = StringUtils.removeEnd("?,".repeat(row.size()), ",");
        String query = format("INSERT INTO ts_kv_cf (%s) VALUES (%s)", columnsStmt, valuesStmt);
        if (ttl > 0) {
            query += " USING TTL " + ttl;
        }
        String finalQuery = query;
        executor.submit(() -> {
            cassandraService.execute(finalQuery, row.values().toArray());
            report(row);
        });
    }

    @Data(staticConstructor = "of")
    private static class PartitionKey {
        private final String entityType;
        private final UUID entityId;
        private final String key;
    }

}
