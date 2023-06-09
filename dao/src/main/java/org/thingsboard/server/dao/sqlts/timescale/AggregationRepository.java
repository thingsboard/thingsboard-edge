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
package org.thingsboard.server.dao.sqlts.timescale;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvEntity;
import org.thingsboard.server.dao.util.TimescaleDBTsOrTsLatestDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@Repository
@TimescaleDBTsOrTsLatestDao
public class AggregationRepository {

    public static final String FIND_AVG = "findAvg";
    public static final String FIND_MAX = "findMax";
    public static final String FIND_MIN = "findMin";
    public static final String FIND_SUM = "findSum";
    public static final String FIND_COUNT = "findCount";

    public static final String FROM_WHERE_CLAUSE = "FROM ts_kv tskv WHERE " +
            "tskv.entity_id = cast(:entityId AS uuid) " +
            "AND tskv.key= cast(:entityKey AS int) " +
            "AND tskv.ts >= :startTs AND tskv.ts < :endTs " +
            "GROUP BY tskv.entity_id, tskv.key, tsBucket " +
            "ORDER BY tskv.entity_id, tskv.key, tsBucket";

    public static final String FIND_AVG_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "SUM(COALESCE(tskv.long_v, 0)) AS longValue, " +
            "SUM(COALESCE(tskv.dbl_v, 0.0)) AS doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "null AS strValue, 'AVG' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_MAX_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "MAX(COALESCE(tskv.long_v, -9223372036854775807)) AS longValue, " +
            "MAX(COALESCE(tskv.dbl_v, -1.79769E+308)) as doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "MAX(tskv.str_v) AS strValue, 'MAX' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_MIN_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "MIN(COALESCE(tskv.long_v, 9223372036854775807)) AS longValue, " +
            "MIN(COALESCE(tskv.dbl_v, 1.79769E+308)) as doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "MIN(tskv.str_v) AS strValue, 'MIN' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_SUM_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "SUM(COALESCE(tskv.long_v, 0)) AS longValue, SUM(COALESCE(tskv.dbl_v, 0.0)) AS doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "null AS strValue, null AS jsonValue, 'SUM' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_COUNT_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "SUM(CASE WHEN tskv.bool_v IS NULL THEN 0 ELSE 1 END) AS booleanValueCount, " +
            "SUM(CASE WHEN tskv.str_v IS NULL THEN 0 ELSE 1 END) AS strValueCount, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longValueCount, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleValueCount, " +
            "SUM(CASE WHEN tskv.json_v IS NULL THEN 0 ELSE 1 END) AS jsonValueCount, " +
            "MAX(tskv.ts) AS maxAggTs ";

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findAvg(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_AVG);
    }

    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findMax(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_MAX);
    }

    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findMin(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_MIN);
    }

    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findSum(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_SUM);
    }

    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findCount(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_COUNT);
    }

    private List getResultList(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs, String query) {
        return entityManager.createNamedQuery(query)
                .setParameter("entityId", entityId)
                .setParameter("entityKey", entityKey)
                .setParameter("timeBucket", timeBucket)
                .setParameter("startTs", startTs)
                .setParameter("endTs", endTs)
                .getResultList();
    }

}
