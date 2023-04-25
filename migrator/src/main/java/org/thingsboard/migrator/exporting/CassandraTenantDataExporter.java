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
package org.thingsboard.migrator.exporting;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.BaseMigrationService;
import org.thingsboard.migrator.Table;
import org.thingsboard.migrator.config.Modes;
import org.thingsboard.migrator.utils.CassandraService;
import org.thingsboard.migrator.utils.Storage;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mode", havingValue = Modes.CASSANDRA_TENANT_DATA_EXPORT)
public class CassandraTenantDataExporter extends BaseMigrationService {

    private final Storage storage;
    private final CassandraService cassandraService;

    public static final String TS_KV_FILE = "ts_kv.gz";

    private Writer writer;

    @Override
    protected void start() throws Exception {
        storage.newFile(TS_KV_FILE);
        writer = storage.newWriter(TS_KV_FILE, true);

        storage.readAndProcess(Table.LATEST_KV.getName(), false, latestKvRow -> {
            executor.submit(() -> {
                getTsHistoryAndSave(latestKvRow);
            });
        });
    }

    private void getTsHistoryAndSave(Map<String, Object> latestKvRow) {
        String entityType = (String) latestKvRow.get("table_name");
        UUID entityId = (UUID) latestKvRow.get("entity_id");
        String key = (String) latestKvRow.get("key_name");
        System.out.printf("Exporting data for %s %s (%s)\n", entityType, entityId, key);

        List<Long> partitions = cassandraService.query("SELECT partition FROM ts_kv_partitions_cf " +
                "WHERE entity_type = ? AND entity_id = ? AND key = ?", Long.class, entityType, entityId, key);
        for (Long partition : partitions) {
            String query = "SELECT * FROM ts_kv_cf WHERE entity_type = ? AND entity_id = ? AND key = ? " +
                    "AND partition = ? ORDER BY ts";
            ResultSet rows = cassandraService.query(query, entityType, entityId, key, partition);
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

    @Override
    protected void afterFinished() throws Exception {
        writer.close();
    }

}
