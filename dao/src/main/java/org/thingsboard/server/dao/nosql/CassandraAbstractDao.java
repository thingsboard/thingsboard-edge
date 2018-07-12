/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.nosql;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.CodecNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.model.type.*;
import org.thingsboard.server.dao.util.BufferedRateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class CassandraAbstractDao {

    @Autowired
    protected CassandraCluster cluster;

    private ConcurrentMap<String, PreparedStatement> preparedStatementMap = new ConcurrentHashMap<>();

    @Autowired
    private BufferedRateLimiter rateLimiter;

    private Session session;

    private ConsistencyLevel defaultReadLevel;
    private ConsistencyLevel defaultWriteLevel;

    private Session getSession() {
        if (session == null) {
            session = cluster.getSession();
            defaultReadLevel = cluster.getDefaultReadConsistencyLevel();
            defaultWriteLevel = cluster.getDefaultWriteConsistencyLevel();
            CodecRegistry registry = session.getCluster().getConfiguration().getCodecRegistry();
            registerCodecIfNotFound(registry, new JsonCodec());
            registerCodecIfNotFound(registry, new DeviceCredentialsTypeCodec());
            registerCodecIfNotFound(registry, new AuthorityCodec());
            registerCodecIfNotFound(registry, new ComponentLifecycleStateCodec());
            registerCodecIfNotFound(registry, new ComponentTypeCodec());
            registerCodecIfNotFound(registry, new ConverterTypeCodec());
            registerCodecIfNotFound(registry, new IntegrationTypeCodec());
            registerCodecIfNotFound(registry, new ComponentScopeCodec());
            registerCodecIfNotFound(registry, new EntityTypeCodec());
        }
        return session;
    }

    protected PreparedStatement prepare(String query) {
        return preparedStatementMap.computeIfAbsent(query, i -> getSession().prepare(i));
    }

    private void registerCodecIfNotFound(CodecRegistry registry, TypeCodec<?> codec) {
        try {
            registry.codecFor(codec.getCqlType(), codec.getJavaType());
        } catch (CodecNotFoundException e) {
            registry.register(codec);
        }
    }

    protected ResultSet executeRead(Statement statement) {
        return execute(statement, defaultReadLevel);
    }

    protected ResultSet executeWrite(Statement statement) {
        return execute(statement, defaultWriteLevel);
    }

    protected ResultSetFuture executeAsyncRead(Statement statement) {
        return executeAsync(statement, defaultReadLevel);
    }

    protected ResultSetFuture executeAsyncWrite(Statement statement) {
        return executeAsync(statement, defaultWriteLevel);
    }

    private ResultSet execute(Statement statement, ConsistencyLevel level) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra statement {}", statementToString(statement));
        }
        return executeAsync(statement, level).getUninterruptibly();
    }

    private ResultSetFuture executeAsync(Statement statement, ConsistencyLevel level) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra async statement {}", statementToString(statement));
        }
        if (statement.getConsistencyLevel() == null) {
            statement.setConsistencyLevel(level);
        }
        return new RateLimitedResultSetFuture(getSession(), rateLimiter, statement);
    }

    private static String statementToString(Statement statement) {
        if (statement instanceof BoundStatement) {
            return ((BoundStatement)statement).preparedStatement().getQueryString();
        } else {
            return statement.toString();
        }
    }
}