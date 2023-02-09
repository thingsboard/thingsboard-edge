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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.tenant.SqlHelperService;
import org.thingsboard.migrator.tenant.Storage;
import org.thingsboard.migrator.tenant.Table;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mode", havingValue = "CASSANDRA_DATA_EXPORT")
public class CassandraTenantDataExporter implements ApplicationRunner {

    private final CassandraTemplate cassandraTemplate;
    private final SqlHelperService sqlHelperService;
    private final Storage storage;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Table> tables = Table.LATEST_KV.getReference().getValue();
        storage.newFile("ts_kv");
        try (Writer writer = storage.newWriter("ts_kv")) {
            for (Table table : tables) {
                Set<String> entities = sqlHelperService.getTenantEntities(table);
                for (String entityId : entities) {
                    getDataForEntityAndSave(table, UUID.fromString(entityId), writer);
                }
            }
        }
        System.exit(0);
    }

    private void getDataForEntityAndSave(Table table, UUID entityId, Writer writer) throws IOException {
        String entityType = table.toString();
        List<Map<String, Object>> partitions = cassandraTemplate.getCqlOperations().queryForList("SELECT partition, key FROM ts_kv_partitions_cf " +
                "WHERE entity_type = ? AND entity_id = ? ALLOW FILTERING", entityType, entityId);
        for (Map<String, Object> partitionInfo : partitions) {
            Long partition = (Long) partitionInfo.get("partition");
            String key = (String) partitionInfo.get("key");
            System.err.printf("EXPORTING PARTITION %s: %s %s %s\n", partition, entityType, entityId, key);

            String query = "SELECT * FROM ts_kv_cf WHERE entity_type = ? AND entity_id = ? " +
                    "AND partition = ? AND key = ? ORDER BY ts";
            ResultSet rows = cassandraTemplate.getCqlOperations().queryForResultSet(query, entityType, entityId, partition, key);
            for (Row row : rows) {
                Map<String, Object> data = new HashMap<>();
                for (ColumnDefinition columnDefinition : row.getColumnDefinitions()) {
                    data.put(columnDefinition.getName().toString(), row.getObject(columnDefinition.getName()));
                }
                storage.addToFile(writer, data);
            }
        }
    }

}
