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
package org.thingsboard.server.dao.sql.edge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


@Repository
@Transactional
public class EdgeEventInsertRepository {

    private static final String INSERT =
            "INSERT INTO edge_event (id, created_time, edge_id, edge_event_type, edge_event_uid, entity_id, edge_event_action, body, tenant_id, ts, entity_group_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT DO NOTHING;";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    protected void save(List<EdgeEventEntity> entities) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jdbcTemplate.batchUpdate(INSERT, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        EdgeEventEntity edgeEvent = entities.get(i);
                        ps.setObject(1, edgeEvent.getId());
                        ps.setLong(2, edgeEvent.getCreatedTime());
                        ps.setObject(3, edgeEvent.getEdgeId());
                        ps.setString(4, edgeEvent.getEdgeEventType().name());
                        ps.setString(5, edgeEvent.getEdgeEventUid());
                        ps.setObject(6, edgeEvent.getEntityId());
                        ps.setString(7, edgeEvent.getEdgeEventAction().name());
                        ps.setString(8, edgeEvent.getEntityBody() != null
                                ? edgeEvent.getEntityBody().toString()
                                : null);
                        ps.setObject(9, edgeEvent.getTenantId());
                        ps.setLong(10, edgeEvent.getTs());
                        ps.setObject(11, edgeEvent.getEntityGroupId());
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
