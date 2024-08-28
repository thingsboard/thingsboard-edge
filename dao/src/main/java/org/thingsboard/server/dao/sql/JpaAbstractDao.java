/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.exception.EntityVersionMismatchException;
import org.thingsboard.server.common.data.id.TenantId;
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
            throw new EntityVersionMismatchException((getEntityType() != null ? getEntityType().getNormalName() : "Entity") + " was already changed by someone else", e);
        }
        return DaoUtil.getData(entity);
    }

    protected E doSave(E entity, boolean isNew, boolean flush) {
        // edge-only: we set version to null on Edge, not using optimistic locking
        if (entity instanceof HasVersion versionedEntity) {
            versionedEntity.setVersion(null);
            entity = (E) versionedEntity;
        }
        return getRepository().save(entity);
        // ... edge-only
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
