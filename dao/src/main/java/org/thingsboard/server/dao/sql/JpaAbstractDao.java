/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.exception.EntityVersionMismatchException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
@SqlDao
public abstract class JpaAbstractDao<E extends BaseEntity<D>, D>
        extends JpaAbstractDaoListeningExecutorService
        implements Dao<D> {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public D save(TenantId tenantId, D domain) {
        return save(tenantId, domain, false);
    }

    private D save(TenantId tenantId, D domain, boolean flush) {
        E entity;
        try {
            entity = getEntityClass().getConstructor(domain.getClass()).newInstance(domain);
        } catch (Exception e) {
            log.error("Can't create entity for domain object {}", domain, e);
            throw new IllegalArgumentException("Can't create entity for domain object {" + domain + "}", e);
        }
        log.debug("Saving entity {}", entity);
        boolean isNew = entity.getUuid() == null;
        if (isNew) {
            UUID uuid = Uuids.timeBased();
            entity.setUuid(uuid);
            entity.setCreatedTime(Uuids.unixTimestamp(uuid));
        }
        try {
            entity = doSave(entity, isNew, flush);
        } catch (OptimisticLockException e) {
            throw new EntityVersionMismatchException(getEntityType(), e);
        }
        return DaoUtil.getData(entity);
    }

    protected E doSave(E entity, boolean isNew, boolean flush) {
        boolean flushed = false;
        EntityManager entityManager = getEntityManager();
        if (isNew) {
            if (entity instanceof HasVersion versionedEntity) {
                versionedEntity.setVersion(1L);
            }
            entityManager.persist(entity);
        } else {
            if (entity instanceof HasVersion versionedEntity) {
                if (versionedEntity.getVersion() == null) {
                    HasVersion existingEntity = entityManager.find(versionedEntity.getClass(), entity.getUuid());
                    if (existingEntity != null) {
                        /*
                         * manually resetting the version to latest to allow force overwrite of the entity
                         * */
                        versionedEntity.setVersion(existingEntity.getVersion());
                    } else {
                        return doSave(entity, true, flush);
                    }
                }
                versionedEntity = entityManager.merge(versionedEntity);
                /*
                 * by default, Hibernate doesn't issue an update query and thus version increment
                 * if the entity was not modified. to bypass this and always increment the version, we do it manually
                 * */
                versionedEntity.setVersion(versionedEntity.getVersion() + 1);
                /*
                 * flushing and then removing the entity from the persistence context so that it is not affected
                 * by next flushes (e.g. when a transaction is committed) to avoid double version increment
                 * */
                entityManager.flush();
                entityManager.detach(versionedEntity);
                flushed = true;
                entity = (E) versionedEntity;
            } else {
                entity = entityManager.merge(entity);
            }
        }
        if (flush && !flushed) {
            entityManager.flush();
        }
        return entity;
    }

    @Override
    @Transactional
    public D saveAndFlush(TenantId tenantId, D domain) {
        return save(tenantId, domain, true);
    }

    @Override
    public D findById(TenantId tenantId, UUID key) {
        log.debug("Get entity by key {}", key);
        Optional<E> entity = getRepository().findById(key);
        return DaoUtil.getData(entity);
    }

    @Override
    public ListenableFuture<D> findByIdAsync(TenantId tenantId, UUID key) {
        log.debug("Get entity by key async {}", key);
        return service.submit(() -> DaoUtil.getData(getRepository().findById(key)));
    }

    @Override
    public boolean existsById(TenantId tenantId, UUID key) {
        log.debug("Exists by key {}", key);
        return getRepository().existsById(key);
    }

    @Override
    public ListenableFuture<Boolean> existsByIdAsync(TenantId tenantId, UUID key) {
        log.debug("Exists by key async {}", key);
        return service.submit(() -> getRepository().existsById(key));
    }

    @Override
    @Transactional
    public void removeById(TenantId tenantId, UUID id) {
        JpaRepository<E, UUID> repository = getRepository();
        repository.deleteById(id);
        repository.flush();
        log.debug("Remove request: {}", id);
    }

    @Override
    @Transactional
    public void removeAllByIds(Collection<UUID> ids) {
        JpaRepository<E, UUID> repository = getRepository();
        ids.forEach(repository::deleteById);
        repository.flush();
    }

    @Override
    public List<D> find(TenantId tenantId) {
        List<E> entities = Lists.newArrayList(getRepository().findAll());
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<UUID> findIdsByTenantIdAndIdOffset(TenantId tenantId, UUID idOffset, int limit) {
        String query = "SELECT id FROM " + getEntityType().getTableName() + " WHERE " + getTenantIdColumn() + " = ? ";
        Object[] params;
        if (idOffset == null) {
            params = new Object[]{tenantId.getId(), limit};
        } else {
            query += " AND id > ? ";
            params = new Object[]{tenantId.getId(), idOffset, limit};
        }
        query += " ORDER BY id LIMIT ?";

        return getJdbcTemplate().queryForList(query, UUID.class, params);
    }

    @Override
    public List<TbPair<UUID, UUID>> findIdsByTenantIdAndIdOffsetAndExpired(UUID idOffset, int limit) {
        EntityType entityType = getEntityType();
        if (!entityType.isHasTtl()) {
            throw new IllegalArgumentException(entityType + " entity type does not support ttl!");
        }
        long now = System.currentTimeMillis();
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT tenant_id, id FROM");
        queryBuilder.append(entityType.getTableName());
        queryBuilder.append(" WHERE ");
        queryBuilder.append(ModelConstants.EXPIRATION_TIME);
        queryBuilder.append("< ?");

        Object[] params;
        if (idOffset == null) {
            params = new Object[]{now, limit};
        } else {
            queryBuilder.append(" AND id > ?");
            params = new Object[]{now, idOffset, limit};
        }
        queryBuilder.append(" ORDER BY id LIMIT ?");

        return getJdbcTemplate().query(
                queryBuilder.toString(),
                (rs, rowNum) -> TbPair.of(rs.getObject(getTenantIdColumn(), UUID.class), rs.getObject("id", UUID.class)),
                params);
    }

    protected String getTenantIdColumn() {
        return ModelConstants.TENANT_ID_COLUMN;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    protected abstract Class<E> getEntityClass();

    protected abstract JpaRepository<E, UUID> getRepository();

}
