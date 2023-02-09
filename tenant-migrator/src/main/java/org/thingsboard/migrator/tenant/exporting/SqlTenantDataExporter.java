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

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.tenant.SqlHelperService;
import org.thingsboard.migrator.tenant.Storage;
import org.thingsboard.migrator.tenant.Table;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mode", havingValue = "SQL_DATA_EXPORT")
public class SqlTenantDataExporter implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final Storage storage;
    private final SqlHelperService helperService;

    @Value("${export.tenant_id}")
    private UUID exportedTenantId;
    @Value("${export.batch_size}")
    private int batchSize;
    @Value("${skipped_tables}")
    private Set<Table> skippedTables;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (Table table : Table.values()) {
            if (skippedTables.contains(table)) {
                continue;
            }
            exportTableData(table, exportedTenantId);
        }
    }

    private void exportTableData(Table table, UUID tenantId) throws IOException {
        storage.newFile(table);
        String query;
        if (table.getCustomSelect() != null) {
            query = table.getCustomSelect().apply(tenantId);
        } else {
            query = format("SELECT * FROM %s WHERE ", table.getName());
        }

        if (table.getReference() == null) {
            query += format("%s.%s = '%s'", table.getName(), table.getTenantIdColumn(), tenantId);
        } else {
            Pair<String, List<Table>> reference = table.getReference();
            String referencingColumn = reference.getKey();
            List<Table> referencedTables = reference.getValue();

            for (Table referencedTable : referencedTables) {
                if (referencedTable.getReference() == null) {
                    query += format(" %s IN (SELECT %s.id FROM %s WHERE %s.%s = '%s') OR",
                            referencingColumn, referencedTable.getName(), referencedTable.getName(),
                            referencedTable.getName(), referencedTable.getTenantIdColumn(), tenantId);
                } else {
                    Pair<String, List<Table>> anotherReference = referencedTable.getReference();
                    String column = anotherReference.getKey();
                    List<Table> tables = anotherReference.getValue();
                    for (Table anotherReferencedTable : tables) {
                        query += format(" %s IN (SELECT %s.id FROM %s INNER JOIN %s ON %s.%s = %s.id WHERE %s.%s = '%s') OR",
                                referencingColumn, referencedTable.getName(), referencedTable.getName(),
                                anotherReferencedTable.getName(), referencedTable.getName(), column, anotherReferencedTable.getName(),
                                anotherReferencedTable.getName(), anotherReferencedTable.getTenantIdColumn(), tenantId);
                    }
                }
            }
            query = StringUtils.removeEnd(query, "OR");
        }
        query += " ORDER BY " + String.join(", ", table.getSortColumns());

        queryAndSave(table, query);
    }

    private void queryAndSave(Table table, String query) throws IOException {
        try (Writer writer = storage.newWriter(table)) {
            if (!table.isPartitioned()) {
                query(query, row -> {
                    try {
                        storage.addToFile(writer, row);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                helperService.getPartitions(table).forEach((partitionStart, partitionEnd) -> {
                    String tsFilter = format(" %s.%s >= %s AND %s.%s < %s AND ", table.getName(), table.getPartitionColumn(),
                            partitionStart, table.getName(), table.getPartitionColumn(), partitionEnd);
                    int afterWhere = StringUtils.indexOf(query, "WHERE") + 5;

                    query(query.substring(0, afterWhere) + tsFilter + query.substring(afterWhere), row -> {
                        try {
                            storage.addToFile(writer, row);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
            }
        }
    }

    private void query(String query, Consumer<Map<String, Object>> rowProcessor, Object... queryParams) {
        int batchIndex = 0;

        boolean hasNextBatch = true;
        while (hasNextBatch) {
            int offset = batchIndex * batchSize;
            String batchQuery = query + " LIMIT " + batchSize + " OFFSET " + offset;

            System.err.println("EXECUTING QUERY: " + batchQuery);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(batchQuery, queryParams);
            rows.forEach(rowProcessor);
            batchIndex++;
            if (rows.size() < batchSize) {
                hasNextBatch = false;
            }
        }
    }

}
