// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.cloud;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.model.sql.AbstractCloudEventEntity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@Transactional
@RequiredArgsConstructor
public class BaseCloudEventInsertRepository<T extends AbstractCloudEventEntity> {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    protected void save(List<T> entities, String tableName) {
        String insertQuery = "INSERT INTO " + tableName +
                " (id, created_time, entity_body, entity_id, cloud_event_type, cloud_event_action, tenant_id, ts) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;";
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jdbcTemplate.batchUpdate(insertQuery, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        T event = entities.get(i);
                        ps.setObject(1, event.getId());
                        ps.setLong(2, event.getCreatedTime());
                        ps.setString(3, event.getEntityBody() != null ? event.getEntityBody().toString() : null);
                        ps.setObject(4, event.getEntityId());
                        ps.setString(5, event.getCloudEventType().name());
                        ps.setString(6, event.getCloudEventAction().name());
                        ps.setObject(7, event.getTenantId());
                        ps.setLong(8, event.getTs());
                    }

                    @Override
                    public int getBatchSize() {
                        return entities.size();
                    }
                });
            }
        });
    }

}
