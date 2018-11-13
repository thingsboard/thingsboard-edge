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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.wrapper.EntityResultSet;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Slf4j
public abstract class CassandraAbstractModelDao<E extends BaseEntity<D>, D> extends CassandraAbstractDao implements Dao<D> {

    protected abstract Class<E> getColumnFamilyClass();

    protected abstract String getColumnFamilyName();

    protected E updateSearchTextIfPresent(E entity) {
        return entity;
    }

    protected Mapper<E> getMapper() {
        return cluster.getMapper(getColumnFamilyClass());
    }

    protected List<E> findListByStatement(TenantId tenantId, Statement statement) {
        List<E> list = Collections.emptyList();
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSet resultSet = executeRead(tenantId, statement);
            Result<E> result = getMapper().map(resultSet);
            if (result != null) {
                list = result.all();
            }
        }
        return list;
    }

    protected ListenableFuture<List<D>> findListByStatementAsync(TenantId tenantId, Statement statement) {
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSetFuture resultSetFuture = executeAsyncRead(tenantId, statement);
            return Futures.transform(resultSetFuture, new Function<ResultSet, List<D>>() {
                @Nullable
                @Override
                public List<D> apply(@Nullable ResultSet resultSet) {
                    Result<E> result = getMapper().map(resultSet);
                    if (result != null) {
                        List<E> entities = result.all();
                        return DaoUtil.convertDataList(entities);
                    } else {
                        return Collections.emptyList();
                    }
                }
            });
        }
        return Futures.immediateFuture(Collections.emptyList());
    }

    protected E findOneByStatement(TenantId tenantId, Statement statement) {
        E object = null;
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSet resultSet = executeRead(tenantId, statement);
            Result<E> result = getMapper().map(resultSet);
            if (result != null) {
                object = result.one();
            }
        }
        return object;
    }

    protected ListenableFuture<D> findOneByStatementAsync(TenantId tenantId, Statement statement) {
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSetFuture resultSetFuture = executeAsyncRead(tenantId, statement);
            return Futures.transform(resultSetFuture, new Function<ResultSet, D>() {
                @Nullable
                @Override
                public D apply(@Nullable ResultSet resultSet) {
                    Result<E> result = getMapper().map(resultSet);
                    if (result != null) {
                        E entity = result.one();
                        return DaoUtil.getData(entity);
                    } else {
                        return null;
                    }
                }
            });
        }
        return Futures.immediateFuture(null);
    }

    protected Statement getSaveQuery(E dto) {
        return getMapper().saveQuery(dto);
    }

    protected EntityResultSet<E> saveWithResult(TenantId tenantId, E entity) {
        log.debug("Save entity {}", entity);
        if (entity.getId() == null) {
            entity.setId(UUIDs.timeBased());
        } else if (isDeleteOnSave()) {
            removeById(tenantId, entity.getId());
        }
        Statement saveStatement = getSaveQuery(entity);
        saveStatement.setConsistencyLevel(cluster.getDefaultWriteConsistencyLevel());
        ResultSet resultSet = executeWrite(tenantId, saveStatement);
        return new EntityResultSet<>(resultSet, entity);
    }

    protected boolean isDeleteOnSave() {
        return true;
    }

    @Override
    public D save(TenantId tenantId, D domain) {
        E entity;
        try {
            entity = getColumnFamilyClass().getConstructor(domain.getClass()).newInstance(domain);
        } catch (Exception e) {
            log.error("Can't create entity for domain object {}", domain, e);
            throw new IllegalArgumentException("Can't create entity for domain object {" + domain + "}", e);
        }
        entity = updateSearchTextIfPresent(entity);
        log.debug("Saving entity {}", entity);
        entity = saveWithResult(tenantId, entity).getEntity();
        return DaoUtil.getData(entity);
    }

    @Override
    public D findById(TenantId tenantId, UUID key) {
        log.debug("Get entity by key {}", key);
        Select.Where query = select().from(getColumnFamilyName()).where(eq(ModelConstants.ID_PROPERTY, key));
        log.trace("Execute query {}", query);
        E entity = findOneByStatement(tenantId, query);
        return DaoUtil.getData(entity);
    }

    @Override
    public ListenableFuture<D> findByIdAsync(TenantId tenantId, UUID key) {
        log.debug("Get entity by key {}", key);
        Select.Where query = select().from(getColumnFamilyName()).where(eq(ModelConstants.ID_PROPERTY, key));
        log.trace("Execute query {}", query);
        return findOneByStatementAsync(tenantId, query);
    }

    @Override
    public boolean removeById(TenantId tenantId, UUID key) {
        Statement delete = QueryBuilder.delete().all().from(getColumnFamilyName()).where(eq(ModelConstants.ID_PROPERTY, key));
        log.debug("Remove request: {}", delete.toString());
        return executeWrite(tenantId, delete).wasApplied();
    }

    @Override
    public List<D> find(TenantId tenantId) {
        log.debug("Get all entities from column family {}", getColumnFamilyName());
        List<E> entities = findListByStatement(tenantId, QueryBuilder.select().all().from(getColumnFamilyName()).setConsistencyLevel(cluster.getDefaultReadConsistencyLevel()));
        return DaoUtil.convertDataList(entities);
    }
    
}
