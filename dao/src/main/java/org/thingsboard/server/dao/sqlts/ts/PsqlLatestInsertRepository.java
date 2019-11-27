/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sqlts.ts;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.AbstractLatestInsertRepository;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.SqlTsDao;

@SqlTsDao
@PsqlDao
@Repository
@Transactional
public class PsqlLatestInsertRepository extends AbstractLatestInsertRepository {

    private static final String TS_KV_LATEST_CONSTRAINT = "(entity_type, entity_id, key)";

    private static final String INSERT_OR_UPDATE_BOOL_STATEMENT = getInsertOrUpdateStringPsql(TS_KV_LATEST_TABLE, TS_KV_LATEST_CONSTRAINT, BOOL_V, PSQL_ON_BOOL_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_OR_UPDATE_STR_STATEMENT = getInsertOrUpdateStringPsql(TS_KV_LATEST_TABLE, TS_KV_LATEST_CONSTRAINT, STR_V, PSQL_ON_STR_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_OR_UPDATE_LONG_STATEMENT = getInsertOrUpdateStringPsql(TS_KV_LATEST_TABLE, TS_KV_LATEST_CONSTRAINT, LONG_V, PSQL_ON_LONG_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_OR_UPDATE_DBL_STATEMENT = getInsertOrUpdateStringPsql(TS_KV_LATEST_TABLE, TS_KV_LATEST_CONSTRAINT, DBL_V, PSQL_ON_DBL_VALUE_UPDATE_SET_NULLS);

    @Override
    public void saveOrUpdate(TsKvLatestEntity entity) {
        processSaveOrUpdate(entity, INSERT_OR_UPDATE_BOOL_STATEMENT, INSERT_OR_UPDATE_STR_STATEMENT, INSERT_OR_UPDATE_LONG_STATEMENT, INSERT_OR_UPDATE_DBL_STATEMENT);
    }

    @Override
    protected void saveOrUpdateBoolean(TsKvLatestEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("entity_type", entity.getEntityType().name())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("key", entity.getKey())
                .setParameter("ts", entity.getTs())
                .setParameter("bool_v", entity.getBooleanValue())
                .executeUpdate();
    }

    @Override
    protected void saveOrUpdateString(TsKvLatestEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("entity_type", entity.getEntityType().name())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("key", entity.getKey())
                .setParameter("ts", entity.getTs())
                .setParameter("str_v", replaceNullChars(entity.getStrValue()))
                .executeUpdate();
    }

    @Override
    protected void saveOrUpdateLong(TsKvLatestEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("entity_type", entity.getEntityType().name())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("key", entity.getKey())
                .setParameter("ts", entity.getTs())
                .setParameter("long_v", entity.getLongValue())
                .executeUpdate();
    }

    @Override
    protected void saveOrUpdateDouble(TsKvLatestEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("entity_type", entity.getEntityType().name())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("key", entity.getKey())
                .setParameter("ts", entity.getTs())
                .setParameter("dbl_v", entity.getDoubleValue())
                .executeUpdate();
    }
}