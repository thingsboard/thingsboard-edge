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
package org.thingsboard.migrator.tenant.exporting;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.cql.CqlOperations;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.tenant.Storage;
import org.thingsboard.migrator.tenant.Table;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mode", havingValue = "CASSANDRA_DATA_EXPORT")
public class CassandraTenantDataExporter implements ApplicationRunner {

    private final CassandraTemplate cassandraTemplate;
    private CqlOperations cqlOperations;
    private final Storage storage;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        cqlOperations = cassandraTemplate.getCqlOperations();
        storage.newFile("ts_kv");
        try (Writer writer = storage.newWriter("ts_kv")) {
            storage.readAndProcess(Table.LATEST_KV, latestKvRow -> {
                getTsHistoryAndSave(latestKvRow, writer);
            });
        }
        System.exit(0);
    }

    @SneakyThrows
    private void getTsHistoryAndSave(Map<String, Object> latestKvRow, Writer writer) {
        String entityType = (String) latestKvRow.get("table_name");
        UUID entityId = (UUID) latestKvRow.get("entity_id");
        String key = (String) latestKvRow.get("key_name");

        List<Long> partitions = cqlOperations.queryForList("SELECT partition FROM ts_kv_partitions_cf " +
                "WHERE entity_type = ? AND entity_id = ? AND key = ?", Long.class, entityType, entityId, key);

        for (Long partition : partitions) {
            System.err.printf("EXPORTING PARTITION %s: %s %s %s\n", partition, entityType, entityId, key);

            String query = "SELECT * FROM ts_kv_cf WHERE entity_type = ? AND entity_id = ? AND key = ? " +
                    "AND partition = ? ORDER BY ts";
            ResultSet rows = cqlOperations.queryForResultSet(query, entityType, entityId, key, partition);
            for (Row row : rows) {
                Map<String, Object> data = new HashMap<>();
                for (ColumnDefinition columnDefinition : row.getColumnDefinitions()) {
                    String column = columnDefinition.getName().toString();
                    Object value = row.getObject(columnDefinition.getName());
                    if (column.endsWith("_v") && value == null) {
                        continue;
                    }
                    data.put(column, value);
                }
                storage.addToFile(writer, data);
            }
        }
    }

}
