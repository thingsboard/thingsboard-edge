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
package org.thingsboard.server.dao.sql.attributes;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.AbstractVersionedInsertRepository;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Repository
@Transactional
@SqlDao
public class AttributeKvInsertRepository extends AbstractVersionedInsertRepository<AttributeKvEntity> {

    private static final String BATCH_UPDATE = "UPDATE attribute_kv SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v =  cast(? AS json), last_update_ts = ?, version = nextval('attribute_kv_version_seq') " +
            "WHERE entity_id = ? and attribute_type =? and attribute_key = ? RETURNING version;";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, str_v, long_v, dbl_v, bool_v, json_v, last_update_ts, version) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?,  cast(? AS json), ?, nextval('attribute_kv_version_seq')) " +
                    "ON CONFLICT (entity_id, attribute_type, attribute_key) " +
                    "DO UPDATE SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v =  cast(? AS json), last_update_ts = ?, version = nextval('attribute_kv_version_seq') RETURNING version;";

    @Override
    protected void setOnBatchUpdateValues(PreparedStatement ps, int i, List<AttributeKvEntity> entities) throws SQLException {
        AttributeKvEntity kvEntity = entities.get(i);
        ps.setString(1, replaceNullChars(kvEntity.getStrValue()));

        if (kvEntity.getLongValue() != null) {
            ps.setLong(2, kvEntity.getLongValue());
        } else {
            ps.setNull(2, Types.BIGINT);
        }

        if (kvEntity.getDoubleValue() != null) {
            ps.setDouble(3, kvEntity.getDoubleValue());
        } else {
            ps.setNull(3, Types.DOUBLE);
        }

        if (kvEntity.getBooleanValue() != null) {
            ps.setBoolean(4, kvEntity.getBooleanValue());
        } else {
            ps.setNull(4, Types.BOOLEAN);
        }

        ps.setString(5, replaceNullChars(kvEntity.getJsonValue()));

        ps.setLong(6, kvEntity.getLastUpdateTs());
        ps.setObject(7, kvEntity.getId().getEntityId());
        ps.setInt(8, kvEntity.getId().getAttributeType());
        ps.setInt(9, kvEntity.getId().getAttributeKey());
    }

    @Override
    protected void setOnInsertOrUpdateValues(PreparedStatement ps, int i, List<AttributeKvEntity> insertEntities) throws SQLException {
        AttributeKvEntity kvEntity = insertEntities.get(i);
        ps.setObject(1, kvEntity.getId().getEntityId());
        ps.setInt(2, kvEntity.getId().getAttributeType());
        ps.setInt(3, kvEntity.getId().getAttributeKey());

        ps.setString(4, replaceNullChars(kvEntity.getStrValue()));
        ps.setString(10, replaceNullChars(kvEntity.getStrValue()));

        if (kvEntity.getLongValue() != null) {
            ps.setLong(5, kvEntity.getLongValue());
            ps.setLong(11, kvEntity.getLongValue());
        } else {
            ps.setNull(5, Types.BIGINT);
            ps.setNull(11, Types.BIGINT);
        }

        if (kvEntity.getDoubleValue() != null) {
            ps.setDouble(6, kvEntity.getDoubleValue());
            ps.setDouble(12, kvEntity.getDoubleValue());
        } else {
            ps.setNull(6, Types.DOUBLE);
            ps.setNull(12, Types.DOUBLE);
        }

        if (kvEntity.getBooleanValue() != null) {
            ps.setBoolean(7, kvEntity.getBooleanValue());
            ps.setBoolean(13, kvEntity.getBooleanValue());
        } else {
            ps.setNull(7, Types.BOOLEAN);
            ps.setNull(13, Types.BOOLEAN);
        }

        ps.setString(8, replaceNullChars(kvEntity.getJsonValue()));
        ps.setString(14, replaceNullChars(kvEntity.getJsonValue()));

        ps.setLong(9, kvEntity.getLastUpdateTs());
        ps.setLong(15, kvEntity.getLastUpdateTs());
    }

    @Override
    protected String getBatchUpdateQuery() {
        return BATCH_UPDATE;
    }

    @Override
    protected String getInsertOrUpdateQuery() {
        return INSERT_OR_UPDATE;
    }
}
