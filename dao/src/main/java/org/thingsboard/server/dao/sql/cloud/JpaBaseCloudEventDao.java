/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.cloud;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.CloudEventId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.cloud.CloudEventDao;
import org.thingsboard.server.dao.model.sql.CloudEventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
@Component
public class JpaBaseCloudEventDao extends JpaAbstractDao<CloudEventEntity, CloudEvent> implements CloudEventDao {

    private final UUID systemTenantId = NULL_UUID;
    private final Lock readWriteLock = new ReentrantLock();

    @Autowired
    private CloudEventRepository cloudEventRepository;

    @Autowired
    private CloudEventCleanupRepository cloudEventCleanupRepository;

    @Override
    protected Class<CloudEventEntity> getEntityClass() {
        return CloudEventEntity.class;
    }

    @Override
    protected JpaRepository<CloudEventEntity, UUID> getRepository() {
        return cloudEventRepository;
    }

    @Override
    public CloudEvent save(CloudEvent cloudEvent) {
        readWriteLock.lock();
        try {
            log.debug("Save cloud event [{}] ", cloudEvent);
            if (cloudEvent.getId() == null) {
                UUID timeBased = Uuids.timeBased();
                cloudEvent.setId(new CloudEventId(timeBased));
                cloudEvent.setCreatedTime(Uuids.unixTimestamp(timeBased));
            } else if (cloudEvent.getCreatedTime() == 0L) {
                UUID eventId = cloudEvent.getId().getId();
                if (eventId.version() == 1) {
                    cloudEvent.setCreatedTime(Uuids.unixTimestamp(eventId));
                } else {
                    cloudEvent.setCreatedTime(System.currentTimeMillis());
                }
            }

            return save(new CloudEventEntity(cloudEvent)).orElse(null);
        } finally {
            readWriteLock.unlock();
        }
    }

    @Override
    public PageData<CloudEvent> findCloudEvents(UUID tenantId, TimePageLink pageLink) {
        readWriteLock.lock();
        try {
            return DaoUtil.toPageData(
                    cloudEventRepository
                            .findEventsByTenantId(
                                    tenantId,
                                    pageLink.getStartTime(),
                                    pageLink.getEndTime(),
                                    DaoUtil.toPageable(pageLink)));
        } finally {
            readWriteLock.unlock();
        }
    }

    @Override
    public PageData<CloudEvent> findCloudEventsByEntityIdAndCloudEventActionAndCloudEventType(UUID tenantId,
                                                                                              UUID entityId,
                                                                                              CloudEventType cloudEventType,
                                                                                              String cloudEventAction,
                                                                                              TimePageLink pageLink) {
        return DaoUtil.toPageData(
                cloudEventRepository
                        .findEventsByTenantIdAndEntityIdAndCloudEventActionAndCloudEventType(
                                tenantId,
                                entityId,
                                cloudEventType,
                                cloudEventAction,
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void cleanupEvents(long eventsTtl) {
        log.info("Going to cleanup old cloud events using debug events ttl: {}s", eventsTtl);
        cloudEventCleanupRepository.cleanupEvents(eventsTtl);
    }

    public Optional<CloudEvent> save(CloudEventEntity entity) {
        log.debug("Save cloud event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system cloud event with predefined id {}", systemTenantId);
            entity.setTenantId(systemTenantId);
        }
        if (entity.getUuid() == null) {
            UUID timeBased = Uuids.timeBased();
            entity.setUuid(timeBased);
        }

        return Optional.of(DaoUtil.getData(cloudEventRepository.save(entity)));
    }
}
