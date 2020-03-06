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
package org.thingsboard.server.dao.sql.event;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.model.sql.EventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.Predicate;
import java.util.*;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.dao.DaoUtil.endTimeToId;
import static org.thingsboard.server.dao.DaoUtil.startTimeToId;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 5/3/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaBaseEventDao extends JpaAbstractSearchTimeDao<EventEntity, Event> implements EventDao {

    private final UUID systemTenantId = NULL_UUID;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventInsertRepository eventInsertRepository;

    @Override
    protected Class<EventEntity> getEntityClass() {
        return EventEntity.class;
    }

    @Override
    protected CrudRepository<EventEntity, String> getCrudRepository() {
        return eventRepository;
    }

    @Override
    public Event save(TenantId tenantId, Event event) {
        log.debug("Save event [{}] ", event);
        if (event.getId() == null) {
            event.setId(new EventId(UUIDs.timeBased()));
        }
        if (StringUtils.isEmpty(event.getUid())) {
            event.setUid(event.getId().toString());
        }
        return save(new EventEntity(event), false).orElse(null);
    }

    @Override
    public ListenableFuture<Event> saveAsync(Event event) {
        log.debug("Save event [{}] ", event);
        if (event.getId() == null) {
            event.setId(new EventId(UUIDs.timeBased()));
        }
        if (StringUtils.isEmpty(event.getUid())) {
            event.setUid(event.getId().toString());
        }
        return service.submit(() -> save(new EventEntity(event), false).orElse(null));
    }

    @Override
    public Optional<Event> saveIfNotExists(Event event) {
        return save(new EventEntity(event), true);
    }

    @Override
    public Event findEvent(UUID tenantId, EntityId entityId, String eventType, String eventUid) {
        return DaoUtil.getData(eventRepository.findByTenantIdAndEntityTypeAndEntityIdAndEventTypeAndEventUid(
                UUIDConverter.fromTimeUUID(tenantId), entityId.getEntityType(), UUIDConverter.fromTimeUUID(entityId.getId()), eventType, eventUid));
    }

    @Override
    public PageData<Event> findEvents(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                eventRepository
                        .findEventsByTenantIdAndEntityId(
                                fromTimeUUID(tenantId),
                                entityId.getEntityType(),
                                fromTimeUUID(entityId.getId()),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                startTimeToId(pageLink.getStartTime()),
                                endTimeToId(pageLink.getEndTime()),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Event> findEvents(UUID tenantId, EntityId entityId, String eventType, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                eventRepository
                        .findEventsByTenantIdAndEntityIdAndEventType(
                                fromTimeUUID(tenantId),
                                entityId.getEntityType(),
                                fromTimeUUID(entityId.getId()),
                                eventType,
                                startTimeToId(pageLink.getStartTime()),
                                endTimeToId(pageLink.getEndTime()),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<Event> findLatestEvents(UUID tenantId, EntityId entityId, String eventType, int limit) {
        List<EventEntity> latest = eventRepository.findLatestByTenantIdAndEntityTypeAndEntityIdAndEventType(
                UUIDConverter.fromTimeUUID(tenantId),
                entityId.getEntityType(),
                UUIDConverter.fromTimeUUID(entityId.getId()),
                eventType,
                new PageRequest(0, limit));
        return DaoUtil.convertDataList(latest);
    }

    public Optional<Event> save(EventEntity entity, boolean ifNotExists) {
        log.debug("Save event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system event with predefined id {}", systemTenantId);
            entity.setTenantId(UUIDConverter.fromTimeUUID(systemTenantId));
        }
        if (entity.getId() == null) {
            entity.setId(UUIDs.timeBased());
        }
        if (StringUtils.isEmpty(entity.getEventUid())) {
            entity.setEventUid(entity.getId().toString());
        }
        if (ifNotExists &&
                eventRepository.findByTenantIdAndEntityTypeAndEntityId(entity.getTenantId(), entity.getEntityType(), entity.getEntityId()) != null) {
            return Optional.empty();
        }
        return Optional.of(DaoUtil.getData(eventInsertRepository.saveOrUpdate(entity)));
    }
}
