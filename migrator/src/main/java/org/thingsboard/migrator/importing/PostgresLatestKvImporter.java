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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.migrator.BaseMigrationService;
import org.thingsboard.migrator.config.Modes;
import org.thingsboard.migrator.exporting.CassandraLatestKvExporter;
import org.thingsboard.migrator.utils.Storage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mode", havingValue = Modes.POSTGRES_LATEST_KV_IMPORT)
public class PostgresLatestKvImporter extends BaseMigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Storage storage;

    @Value("${import.sql.delay_between_queries}")
    private int delayBetweenQueries;
    @Value("${import.sql.ignore_conflicts}")
    private boolean ignoreConflicts;

    private Map<String, String> columns;

    @Override
    protected void start() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                storage.readAndProcess(CassandraLatestKvExporter.LATEST_KV_FILE, true, row -> {
                    saveRow(row);
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void saveRow(Map<String, Object> row) {
        prepareRow(row);

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
            String valueType = columns.get(column);
            valuesStatement += "::" + valueType;
        }

        String query = format("INSERT INTO ts_kv_latest (%s) VALUES (%s)", columnsStatement, valuesStatement);
        if (ignoreConflicts) {
            query += " ON CONFLICT DO NOTHING";
        }
        jdbcTemplate.update(query, row.values().toArray());
        report(row);
        try {
            TimeUnit.MILLISECONDS.sleep(delayBetweenQueries);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareRow(Map<String, Object> row) {
        String keyName = (String) row.remove("key_name");
        Integer keyId = jdbcTemplate.queryForList("SELECT key_id FROM ts_kv_dictionary WHERE key = ?", Integer.class, keyName).stream().findFirst().orElse(null);
        if (keyId == null) {
            jdbcTemplate.update("INSERT INTO ts_kv_dictionary (key) VALUES (?)", keyName);
            keyId = jdbcTemplate.queryForObject("SELECT key_id FROM ts_kv_dictionary WHERE key = ?", Integer.class, keyName);
            System.err.println("Inserted key '" + keyName + "' into ts_kv_dictionary (new key id: " + keyId + ")");
        }
        row.put("key", keyId);

        if (columns == null) {
            columns = jdbcTemplate.queryForList("SELECT column_name, udt_name FROM information_schema.columns " +
                            "WHERE table_schema = 'public' AND table_name = 'ts_kv_latest'").stream()
                    .collect(Collectors.toMap(vals -> vals.get("column_name").toString(), vals -> vals.get("udt_name").toString()));
        }
        row.keySet().removeIf(column -> !columns.containsKey(column));
    }

}
