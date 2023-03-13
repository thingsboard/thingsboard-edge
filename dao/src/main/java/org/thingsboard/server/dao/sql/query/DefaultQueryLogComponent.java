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
package org.thingsboard.server.dao.sql.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class DefaultQueryLogComponent implements QueryLogComponent {

    @Value("${sql.log_queries:false}")
    private boolean logSqlQueries;
    @Value("${sql.log_queries_threshold:5000}")
    private long logQueriesThreshold;

    @Override
    public void logQuery(QueryContext ctx, String query, long duration) {
        if (logSqlQueries && duration > logQueriesThreshold) {

            String sqlToUse = substituteParametersInSqlString(query, ctx);
            log.warn("SLOW QUERY took {} ms: {}", duration, sqlToUse);

        }
    }

    String substituteParametersInSqlString(String sql, SqlParameterSource paramSource) {

        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
        List<SqlParameter> declaredParams = NamedParameterUtils.buildSqlParameterList(parsedSql, paramSource);

        if (declaredParams.isEmpty()) {
            return sql;
        }

        for (SqlParameter parSQL: declaredParams) {
            String paramName = parSQL.getName();
            if (!paramSource.hasValue(paramName)) {
                continue;
            }

            Object value = paramSource.getValue(paramName);
            if (value instanceof SqlParameterValue) {
                value = ((SqlParameterValue)value).getValue();
            }

            if (!(value instanceof Iterable)) {

                String ValueForSQLQuery = getValueForSQLQuery(value);
                sql = sql.replace(":" + paramName, ValueForSQLQuery);
                continue;
            }

            //Iterable
            int count = 0;
            String valueArrayStr = "";

            for (Object valueTemp: (Iterable)value) {

                if (count > 0) {
                    valueArrayStr+=", ";
                }

                String valueForSQLQuery = getValueForSQLQuery(valueTemp);
                valueArrayStr += valueForSQLQuery;
                ++count;
            }

            sql = sql.replace(":" + paramName, valueArrayStr);

        }

        return sql;
    }

    String getValueForSQLQuery(Object valueParameter) {

        if (valueParameter instanceof String) {
            return "'" + ((String) valueParameter).replaceAll("'", "''") + "'";
        }

        if (valueParameter instanceof UUID) {
            return "'" + valueParameter + "'";
        }

        return String.valueOf(valueParameter);
    }
}
