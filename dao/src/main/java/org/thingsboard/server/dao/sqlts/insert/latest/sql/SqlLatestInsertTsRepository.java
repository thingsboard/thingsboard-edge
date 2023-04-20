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
package org.thingsboard.server.dao.sqlts.insert.latest.sql;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@SqlTsLatestAnyDao
@Repository
@Transactional
@SqlDao
public class SqlLatestInsertTsRepository extends AbstractInsertRepository implements InsertLatestTsRepository {

    @Value("${sql.ts_latest.update_by_latest_ts:true}")
    private Boolean updateByLatestTs;

    private static final String BATCH_UPDATE =
            "UPDATE ts_kv_latest SET ts = ?, bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json) WHERE entity_id = ? AND key = ?";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO ts_kv_latest (entity_id, key, ts, bool_v, str_v, long_v, dbl_v,  json_v) VALUES(?, ?, ?, ?, ?, ?, ?, cast(? AS json)) " +
                    "ON CONFLICT (entity_id, key) DO UPDATE SET ts = ?, bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json)";

    private static final String BATCH_UPDATE_BY_LATEST_TS = BATCH_UPDATE + " AND ts_kv_latest.ts <= ?";

    private static final String INSERT_OR_UPDATE_BY_LATEST_TS = INSERT_OR_UPDATE + " WHERE ts_kv_latest.ts <= ?";

    @Override
    public void saveOrUpdate(List<TsKvLatestEntity> entities) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                String batchUpdateQuery = updateByLatestTs ? BATCH_UPDATE_BY_LATEST_TS : BATCH_UPDATE;
                String insertOrUpdateQuery = updateByLatestTs ? INSERT_OR_UPDATE_BY_LATEST_TS : INSERT_OR_UPDATE;

                int[] result = jdbcTemplate.batchUpdate(batchUpdateQuery, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        TsKvLatestEntity tsKvLatestEntity = entities.get(i);
                        ps.setLong(1, tsKvLatestEntity.getTs());

                        if (tsKvLatestEntity.getBooleanValue() != null) {
                            ps.setBoolean(2, tsKvLatestEntity.getBooleanValue());
                        } else {
                            ps.setNull(2, Types.BOOLEAN);
                        }

                        ps.setString(3, replaceNullChars(tsKvLatestEntity.getStrValue()));

                        if (tsKvLatestEntity.getLongValue() != null) {
                            ps.setLong(4, tsKvLatestEntity.getLongValue());
                        } else {
                            ps.setNull(4, Types.BIGINT);
                        }

                        if (tsKvLatestEntity.getDoubleValue() != null) {
                            ps.setDouble(5, tsKvLatestEntity.getDoubleValue());
                        } else {
                            ps.setNull(5, Types.DOUBLE);
                        }

                        ps.setString(6, replaceNullChars(tsKvLatestEntity.getJsonValue()));

                        ps.setObject(7, tsKvLatestEntity.getEntityId());
                        ps.setInt(8, tsKvLatestEntity.getKey());
                        if (updateByLatestTs) {
                            ps.setLong(9, tsKvLatestEntity.getTs());
                        }
                    }

                    @Override
                    public int getBatchSize() {
                        return entities.size();
                    }
                });

                int updatedCount = 0;
                for (int i = 0; i < result.length; i++) {
                    if (result[i] == 0) {
                        updatedCount++;
                    }
                }

                List<TsKvLatestEntity> insertEntities = new ArrayList<>(updatedCount);
                for (int i = 0; i < result.length; i++) {
                    if (result[i] == 0) {
                        insertEntities.add(entities.get(i));
                    }
                }

                jdbcTemplate.batchUpdate(insertOrUpdateQuery, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        TsKvLatestEntity tsKvLatestEntity = insertEntities.get(i);
                        ps.setObject(1, tsKvLatestEntity.getEntityId());
                        ps.setInt(2, tsKvLatestEntity.getKey());

                        ps.setLong(3, tsKvLatestEntity.getTs());
                        ps.setLong(9, tsKvLatestEntity.getTs());
                        if (updateByLatestTs) {
                            ps.setLong(15, tsKvLatestEntity.getTs());
                        }

                        if (tsKvLatestEntity.getBooleanValue() != null) {
                            ps.setBoolean(4, tsKvLatestEntity.getBooleanValue());
                            ps.setBoolean(10, tsKvLatestEntity.getBooleanValue());
                        } else {
                            ps.setNull(4, Types.BOOLEAN);
                            ps.setNull(10, Types.BOOLEAN);
                        }

                        ps.setString(5, replaceNullChars(tsKvLatestEntity.getStrValue()));
                        ps.setString(11, replaceNullChars(tsKvLatestEntity.getStrValue()));

                        if (tsKvLatestEntity.getLongValue() != null) {
                            ps.setLong(6, tsKvLatestEntity.getLongValue());
                            ps.setLong(12, tsKvLatestEntity.getLongValue());
                        } else {
                            ps.setNull(6, Types.BIGINT);
                            ps.setNull(12, Types.BIGINT);
                        }

                        if (tsKvLatestEntity.getDoubleValue() != null) {
                            ps.setDouble(7, tsKvLatestEntity.getDoubleValue());
                            ps.setDouble(13, tsKvLatestEntity.getDoubleValue());
                        } else {
                            ps.setNull(7, Types.DOUBLE);
                            ps.setNull(13, Types.DOUBLE);
                        }

                        ps.setString(8, replaceNullChars(tsKvLatestEntity.getJsonValue()));
                        ps.setString(14, replaceNullChars(tsKvLatestEntity.getJsonValue()));
                    }

                    @Override
                    public int getBatchSize() {
                        return insertEntities.size();
                    }
                });
            }
        });
    }
}
