/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sqlts.insert.psql;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.EntityContainer;
import org.thingsboard.server.dao.sqlts.insert.InsertTsRepository;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SqlTsDao
@PsqlDao
@Repository
@Transactional
public class PsqlInsertTsRepository extends AbstractInsertRepository implements InsertTsRepository<TsKvEntity> {

    private static final String INSERT_INTO_TS_KV = "INSERT INTO ts_kv_";

    private static final String VALUES_ON_CONFLICT_DO_UPDATE = " (entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) VALUES (?, ?, ?, ?, ?, ?, ?, cast(? AS json)) " +
            "ON CONFLICT (entity_id, key, ts) DO UPDATE SET bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json);";

    @Override
    public void saveOrUpdate(List<EntityContainer<TsKvEntity>> entities) {
        Map<String, List<TsKvEntity>> partitionMap = new HashMap<>();
        for (EntityContainer<TsKvEntity> entityContainer : entities) {
            List<TsKvEntity> tsKvEntities = partitionMap.computeIfAbsent(entityContainer.getPartitionDate(), k -> new ArrayList<>());
            tsKvEntities.add(entityContainer.getEntity());
        }
        partitionMap.forEach((partition, entries) -> jdbcTemplate.batchUpdate(getInsertOrUpdateQuery(partition), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                TsKvEntity tsKvEntity = entries.get(i);
                ps.setObject(1, tsKvEntity.getEntityId());
                ps.setInt(2, tsKvEntity.getKey());
                ps.setLong(3, tsKvEntity.getTs());

                if (tsKvEntity.getBooleanValue() != null) {
                    ps.setBoolean(4, tsKvEntity.getBooleanValue());
                    ps.setBoolean(9, tsKvEntity.getBooleanValue());
                } else {
                    ps.setNull(4, Types.BOOLEAN);
                    ps.setNull(9, Types.BOOLEAN);
                }

                ps.setString(5, replaceNullChars(tsKvEntity.getStrValue()));
                ps.setString(10, replaceNullChars(tsKvEntity.getStrValue()));


                if (tsKvEntity.getLongValue() != null) {
                    ps.setLong(6, tsKvEntity.getLongValue());
                    ps.setLong(11, tsKvEntity.getLongValue());
                } else {
                    ps.setNull(6, Types.BIGINT);
                    ps.setNull(11, Types.BIGINT);
                }

                if (tsKvEntity.getDoubleValue() != null) {
                    ps.setDouble(7, tsKvEntity.getDoubleValue());
                    ps.setDouble(12, tsKvEntity.getDoubleValue());
                } else {
                    ps.setNull(7, Types.DOUBLE);
                    ps.setNull(12, Types.DOUBLE);
                }

                ps.setString(8, replaceNullChars(tsKvEntity.getJsonValue()));
                ps.setString(13, replaceNullChars(tsKvEntity.getJsonValue()));
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        }));
    }

    private String getInsertOrUpdateQuery(String partitionDate) {
        return INSERT_INTO_TS_KV + partitionDate + VALUES_ON_CONFLICT_DO_UPDATE;
    }
}
