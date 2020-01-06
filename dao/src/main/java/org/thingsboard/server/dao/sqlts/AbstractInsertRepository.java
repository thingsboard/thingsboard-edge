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
package org.thingsboard.server.dao.sqlts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.regex.Pattern;

@Repository
public abstract class AbstractInsertRepository {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));
    private static final String EMPTY_STR = "";

    protected static final String BOOL_V = "bool_v";
    protected static final String STR_V = "str_v";
    protected static final String LONG_V = "long_v";
    protected static final String DBL_V = "dbl_v";

    protected static final String TS_KV_LATEST_TABLE = "ts_kv_latest";
    protected static final String TS_KV_TABLE = "ts_kv";

    protected static final String HSQL_ON_BOOL_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_TABLE, BOOL_V);
    protected static final String HSQL_ON_STR_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_TABLE, STR_V);
    protected static final String HSQL_ON_LONG_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_TABLE, LONG_V);
    protected static final String HSQL_ON_DBL_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_TABLE, DBL_V);

    protected static final String HSQL_LATEST_ON_BOOL_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_LATEST_TABLE, BOOL_V);
    protected static final String HSQL_LATEST_ON_STR_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_LATEST_TABLE, STR_V);
    protected static final String HSQL_LATEST_ON_LONG_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_LATEST_TABLE, LONG_V);
    protected static final String HSQL_LATEST_ON_DBL_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_LATEST_TABLE, DBL_V);

    protected static final String PSQL_ON_BOOL_VALUE_UPDATE_SET_NULLS = "str_v = null, long_v = null, dbl_v = null";
    protected static final String PSQL_ON_STR_VALUE_UPDATE_SET_NULLS = "bool_v = null, long_v = null, dbl_v = null";
    protected static final String PSQL_ON_LONG_VALUE_UPDATE_SET_NULLS = "str_v = null, bool_v = null, dbl_v = null";
    protected static final String PSQL_ON_DBL_VALUE_UPDATE_SET_NULLS = "str_v = null, long_v = null, bool_v = null";

    @Value("${sql.remove_null_chars}")
    private boolean removeNullChars;

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    protected static String getInsertOrUpdateStringHsql(String tableName, String constraint, String value, String nullValues) {
        return "MERGE INTO " + tableName + " USING(VALUES :entity_type, :entity_id, :key, :ts, :" + value + ") A (entity_type, entity_id, key, ts, " + value + ") ON " + constraint + " WHEN MATCHED THEN UPDATE SET " + tableName + "." + value + " = A." + value + ", " + tableName + ".ts = A.ts," + nullValues + "WHEN NOT MATCHED THEN INSERT (entity_type, entity_id, key, ts, " + value + ") VALUES (A.entity_type, A.entity_id, A.key, A.ts, A." + value + ")";
    }

    protected static String getInsertOrUpdateStringPsql(String tableName, String constraint, String value, String nullValues) {
        return "INSERT INTO " + tableName + " (entity_type, entity_id, key, ts, " + value + ") VALUES (:entity_type, :entity_id, :key, :ts, :" + value + ") ON CONFLICT " + constraint + " DO UPDATE SET " + value + " = :" + value + ", ts = :ts," + nullValues;
    }

    private static String getHsqlNullValues(String tableName, String notNullValue) {
        switch (notNullValue) {
            case BOOL_V:
                return " " + tableName + ".str_v = null, " + tableName + ".long_v = null, " + tableName + ".dbl_v = null ";
            case STR_V:
                return " " + tableName + ".bool_v = null, " + tableName + ".long_v = null, " + tableName + ".dbl_v = null ";
            case LONG_V:
                return " " + tableName + ".str_v = null, " + tableName + ".bool_v = null, " + tableName + ".dbl_v = null ";
            case DBL_V:
                return " " + tableName + ".str_v = null, " + tableName + ".long_v = null, " + tableName + ".bool_v = null ";
            default:
                throw new RuntimeException("Unsupported insert value: [" + notNullValue + "]");
        }
    }

    protected String replaceNullChars(String strValue) {
        if (removeNullChars && strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }
}