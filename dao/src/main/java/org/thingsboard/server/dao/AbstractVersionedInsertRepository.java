/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.dao.model.ModelConstants.VERSION_COLUMN;

public abstract class AbstractVersionedInsertRepository<T> extends AbstractInsertRepository {

    public List<Long> saveOrUpdate(List<T> entities) {
        return transactionTemplate.execute(status -> {
            List<Long> seqNumbers = new ArrayList<>(entities.size());

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int[] updateResult = onBatchUpdate(entities, keyHolder);

            List<Map<String, Object>> seqNumbersList = keyHolder.getKeyList();

            int notUpdatedCount = entities.size() - seqNumbersList.size();

            List<Integer> toInsertIndexes = new ArrayList<>(notUpdatedCount);
            List<T> insertEntities = new ArrayList<>(notUpdatedCount);
            for (int i = 0, keyHolderIndex = 0; i < updateResult.length; i++) {
                if (updateResult[i] == 0) {
                    insertEntities.add(entities.get(i));
                    seqNumbers.add(null);
                    toInsertIndexes.add(i);
                } else {
                    seqNumbers.add((Long) seqNumbersList.get(keyHolderIndex).get(VERSION_COLUMN));
                    keyHolderIndex++;
                }
            }

            if (insertEntities.isEmpty()) {
                return seqNumbers;
            }

            int[] insertResult = onInsertOrUpdate(insertEntities, keyHolder);

            seqNumbersList = keyHolder.getKeyList();

            for (int i = 0, keyHolderIndex = 0; i < insertResult.length; i++) {
                if (insertResult[i] != 0) {
                    seqNumbers.set(toInsertIndexes.get(i), (Long) seqNumbersList.get(keyHolderIndex).get(VERSION_COLUMN));
                    keyHolderIndex++;
                }
            }

            return seqNumbers;
        });
    }

    private int[] onBatchUpdate(List<T> entities, KeyHolder keyHolder) {
        return jdbcTemplate.batchUpdate(new SequencePreparedStatementCreator(getBatchUpdateQuery()), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                setOnBatchUpdateValues(ps, i, entities);
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        }, keyHolder);
    }

    private int[] onInsertOrUpdate(List<T> insertEntities, KeyHolder keyHolder) {
        return jdbcTemplate.batchUpdate(new SequencePreparedStatementCreator(getInsertOrUpdateQuery()), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                setOnInsertOrUpdateValues(ps, i, insertEntities);
            }

            @Override
            public int getBatchSize() {
                return insertEntities.size();
            }
        }, keyHolder);
    }

    protected abstract void setOnBatchUpdateValues(PreparedStatement ps, int i, List<T> entities) throws SQLException;

    protected abstract void setOnInsertOrUpdateValues(PreparedStatement ps, int i, List<T> entities) throws SQLException;

    protected abstract String getBatchUpdateQuery();

    protected abstract String getInsertOrUpdateQuery();

    private record SequencePreparedStatementCreator(String sql) implements PreparedStatementCreator, SqlProvider {

        private static final String[] COLUMNS = {VERSION_COLUMN};

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement(sql, COLUMNS);
        }

        @Override
        public String getSql() {
            return this.sql;
        }
    }
}
