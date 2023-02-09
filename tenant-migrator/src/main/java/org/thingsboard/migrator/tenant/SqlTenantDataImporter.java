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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "import")
public class SqlTenantDataImporter {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Storage storage;

    @Setter
    private Map<String, Integer> partitionSizes;
    @Value("${skipped_tables}")
    private Set<Table> skippedTables;

    private final Map<Table, Map<String, String>> columns = new HashMap<>();
    private final Map<Table, Set<Long>> partitions = new HashMap<>();

    public void importTenant() {
        System.err.println("Partition sizes: " + partitionSizes);
        transactionTemplate.executeWithoutResult(status -> {
            for (Table table : Table.values()) {
                if (skippedTables.contains(table)) {
                    continue;
                }

                importTableData(table);
            }
        });
        System.err.println("FINISHED SUCCESSFULLY");
    }

    @SneakyThrows
    private void importTableData(Table table) {
        storage.readAndProcess(table, row -> {
            row = prepareRow(table, row);
            if (table.isPartitioned()) {
                createPartition(table, row);
            }

            String columnsStatement = "";
            String valuesStatement = "";
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String column = entry.getKey();
                Object value = entry.getValue();

                if (!columnsStatement.isEmpty()) {
                    columnsStatement += ",";
                }
                columnsStatement += column;

                if (!valuesStatement.isEmpty()) {
                    valuesStatement += ",";
                }
                valuesStatement += "?";
                if (value instanceof JsonNode) {
                    entry.setValue(value.toString());
                }
                String valueType = columns.get(table).get(column);
                valuesStatement += "::" + valueType;
            }

            String query = format("INSERT INTO %s (%s) VALUES (%s)", table.getName(), columnsStatement, valuesStatement);
            System.err.println("EXECUTING QUERY: " + query);
            jdbcTemplate.update(query, row.values().toArray());
        });
    }

    private Map<String, Object> prepareRow(Table table, Map<String, Object> row) {
        if (table == Table.TENANT) {
            UUID defaultTenantProfile = jdbcTemplate.queryForList("SELECT id FROM tenant_profile WHERE is_default = TRUE", UUID.class).get(0);
            row = new LinkedHashMap<>(row);
            row.put("tenant_profile_id", defaultTenantProfile);
        } else if (table == Table.LATEST_KV) {
            String keyName = (String) row.remove("key_name");
            Integer keyId = jdbcTemplate.queryForList("SELECT key_id FROM ts_kv_dictionary WHERE key = ?", Integer.class, keyName).stream().findFirst().orElse(null);
            if (keyId == null) {
                jdbcTemplate.update("INSERT INTO ts_kv_dictionary (key) VALUES (?)", keyName);
                keyId = jdbcTemplate.queryForObject("SELECT key_id FROM ts_kv_dictionary WHERE key = ?", Integer.class, keyName);
            }
            Object oldKey = row.put("key", keyId);
            System.err.println("Replaced old keyId " + oldKey + " with newly created " + keyId);
        } else if (table == Table.GROUP_PERMISSION) {
            UUID roleId = (UUID) row.get("role_id");
            Boolean roleExists = jdbcTemplate.queryForObject("SELECT EXISTS (SELECT * FROM role WHERE id = ?)", Boolean.class, roleId);
            if (!roleExists) {
                // happens when system role is used (e.g. 'Tenant Administrators')
                String roleName = (String) row.remove("role_name");
                System.err.println("Role for id " + roleId + " does not exist. Finding by name " + roleName);
                Map<String, Object> role = jdbcTemplate.queryForList("SELECT * FROM role WHERE name = ?", roleName).stream()
                        .findFirst().orElse(null);
                if (role == null) {
                    throw new IllegalArgumentException("Role not found for name " + roleName);
                }
                row.put("role_id", role.get("id"));
            }
        }
        Map<String, String> existingColumns = columns.computeIfAbsent(table, t -> {
            return jdbcTemplate.queryForList("SELECT column_name, udt_name FROM information_schema.columns " +
                            "WHERE table_schema = 'public' AND table_name = '" + table.getName() + "'").stream()
                    .collect(Collectors.toMap(vals -> vals.get("column_name").toString(), vals -> vals.get("udt_name").toString()));
        });
        row.keySet().removeIf(column -> {
            boolean unknownColumn = !existingColumns.containsKey(column);
            if (unknownColumn) {
                System.err.println("Unknown column " + column + " for table " + table.getName() + ". Skipping");
            }
            return unknownColumn;
        });
        return row;
    }

    private void createPartition(Table table, Map<String, Object> row) {
        long partitionSize = TimeUnit.HOURS.toMillis(partitionSizes.get(table.getPartitionSizeSettingsKey()));
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

}
