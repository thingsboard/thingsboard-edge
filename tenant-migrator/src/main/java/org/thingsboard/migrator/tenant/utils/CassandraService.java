/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.migrator.tenant.utils;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.cql.ArgumentPreparedStatementBinder;
import org.springframework.data.cassandra.core.cql.CachedPreparedStatementCreator;
import org.springframework.data.cassandra.core.cql.CqlOperations;
import org.springframework.data.cassandra.core.cql.ResultSetExtractor;
import org.springframework.data.cassandra.core.cql.RowMapperResultSetExtractor;
import org.springframework.data.cassandra.core.cql.SingleColumnRowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnExpression("'${mode}'.startsWith('CASSANDRA')")
public class CassandraService {

    private final CqlOperations cqlOperations;

    public CassandraService(CassandraTemplate cassandraTemplate) {
        this.cqlOperations = cassandraTemplate.getCqlOperations();
    }

    public void execute(String query, Object... args) {
        query(query, args);
    }

    public ResultSet query(String query, Object... args) {
        return query(query, rs -> rs, args);
    }

    public <T> List<T> query(String query, Class<T> type, Object... args) {
        return query(query, new RowMapperResultSetExtractor<>(SingleColumnRowMapper.newInstance(type)), args);
    }

    @SuppressWarnings("deprecation")
    private <T> T query(String query, ResultSetExtractor<T> resultSetExtractor, Object... args) {
        return cqlOperations.query(new CachedPreparedStatementCreator(query), new ArgumentPreparedStatementBinder(args), resultSetExtractor);
    }

}
