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
package org.thingsboard.server.dao.sql.attributes;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.Types;
import java.util.List;

@SqlDao
@HsqlDao
@Repository
@Transactional
public class HsqlAttributesInsertRepository extends AttributeKvInsertRepository {

    private static final String ON_BOOL_VALUE_UPDATE_SET_NULLS = " attribute_kv.str_v = null, attribute_kv.long_v = null, attribute_kv.dbl_v = null ";
    private static final String ON_STR_VALUE_UPDATE_SET_NULLS = " attribute_kv.bool_v = null, attribute_kv.long_v = null, attribute_kv.dbl_v = null ";
    private static final String ON_LONG_VALUE_UPDATE_SET_NULLS = " attribute_kv.str_v = null, attribute_kv.bool_v = null, attribute_kv.dbl_v = null ";
    private static final String ON_DBL_VALUE_UPDATE_SET_NULLS = " attribute_kv.str_v = null, attribute_kv.long_v = null, attribute_kv.bool_v = null ";

    private static final String INSERT_BOOL_STATEMENT = getInsertOrUpdateString(BOOL_V, ON_BOOL_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_STR_STATEMENT = getInsertOrUpdateString(STR_V, ON_STR_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_LONG_STATEMENT = getInsertOrUpdateString(LONG_V, ON_LONG_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_DBL_STATEMENT = getInsertOrUpdateString(DBL_V, ON_DBL_VALUE_UPDATE_SET_NULLS);

    private static final String INSERT_OR_UPDATE =
            "MERGE INTO attribute_kv USING(VALUES ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "A (entity_type, entity_id, attribute_type, attribute_key, str_v, long_v, dbl_v, bool_v, last_update_ts) " +
                    "ON (attribute_kv.entity_type=A.entity_type " +
                    "AND attribute_kv.entity_id=A.entity_id " +
                    "AND attribute_kv.attribute_type=A.attribute_type " +
                    "AND attribute_kv.attribute_key=A.attribute_key) " +
                    "WHEN MATCHED THEN UPDATE SET attribute_kv.str_v = A.str_v, attribute_kv.long_v = A.long_v, attribute_kv.dbl_v = A.dbl_v, attribute_kv.bool_v = A.bool_v, attribute_kv.last_update_ts = A.last_update_ts " +
                    "WHEN NOT MATCHED THEN INSERT (entity_type, entity_id, attribute_type, attribute_key, str_v, long_v, dbl_v, bool_v, last_update_ts) " +
                    "VALUES (A.entity_type, A.entity_id, A.attribute_type, A.attribute_key, A.str_v, A.long_v, A.dbl_v, A.bool_v, A.last_update_ts)";

    @Override
    public void saveOrUpdate(AttributeKvEntity entity) {
        processSaveOrUpdate(entity, INSERT_BOOL_STATEMENT, INSERT_STR_STATEMENT, INSERT_LONG_STATEMENT, INSERT_DBL_STATEMENT);
    }

    private static String getInsertOrUpdateString(String value, String nullValues) {
        return "MERGE INTO attribute_kv USING(VALUES :entity_type, :entity_id, :attribute_type, :attribute_key, :" + value + ", :last_update_ts) A (entity_type, entity_id, attribute_type, attribute_key, " + value + ", last_update_ts) ON (attribute_kv.entity_type=A.entity_type AND attribute_kv.entity_id=A.entity_id AND attribute_kv.attribute_type=A.attribute_type AND attribute_kv.attribute_key=A.attribute_key) WHEN MATCHED THEN UPDATE SET attribute_kv." + value + " = A." + value + ", attribute_kv.last_update_ts = A.last_update_ts," + nullValues + "WHEN NOT MATCHED THEN INSERT (entity_type, entity_id, attribute_type, attribute_key, " + value + ", last_update_ts) VALUES (A.entity_type, A.entity_id, A.attribute_type, A.attribute_key, A." + value + ", A.last_update_ts)";
    }


    @Override
    protected void saveOrUpdate(List<AttributeKvEntity> entities) {
        entities.forEach(entity -> {
            jdbcTemplate.update(INSERT_OR_UPDATE, ps -> {
                ps.setString(1, entity.getId().getEntityType().name());
                ps.setString(2, entity.getId().getEntityId());
                ps.setString(3, entity.getId().getAttributeType());
                ps.setString(4, entity.getId().getAttributeKey());
                ps.setString(5, entity.getStrValue());

                if (entity.getLongValue() != null) {
                    ps.setLong(6, entity.getLongValue());
                } else {
                    ps.setNull(6, Types.BIGINT);
                }

                if (entity.getDoubleValue() != null) {
                    ps.setDouble(7, entity.getDoubleValue());
                } else {
                    ps.setNull(7, Types.DOUBLE);
                }

                if (entity.getBooleanValue() != null) {
                    ps.setBoolean(8, entity.getBooleanValue());
                } else {
                    ps.setNull(8, Types.BOOLEAN);
                }

                ps.setLong(9, entity.getLastUpdateTs());
            });
        });
    }
}