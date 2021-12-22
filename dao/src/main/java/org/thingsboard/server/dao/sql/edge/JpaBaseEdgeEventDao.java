/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
@Component
public class JpaBaseEdgeEventDao extends JpaAbstractSearchTextDao<EdgeEventEntity, EdgeEvent> implements EdgeEventDao {

    private final UUID systemTenantId = NULL_UUID;

    private final ConcurrentMap<EdgeId, Lock> readWriteLocks = new ConcurrentHashMap<>();

    @Autowired
    private EdgeEventRepository edgeEventRepository;

    @Override
    protected Class<EdgeEventEntity> getEntityClass() {
        return EdgeEventEntity.class;
    }

    @Override
    protected CrudRepository<EdgeEventEntity, UUID> getCrudRepository() {
        return edgeEventRepository;
    }

    @Override
    public EdgeEvent save(EdgeEvent edgeEvent) {
        final Lock readWriteLock = readWriteLocks.computeIfAbsent(edgeEvent.getEdgeId(), id -> new ReentrantLock());
        readWriteLock.lock();
        try {
            log.debug("Save edge event [{}] ", edgeEvent);
            if (edgeEvent.getId() == null) {
                UUID timeBased = Uuids.timeBased();
                edgeEvent.setId(new EdgeEventId(timeBased));
                edgeEvent.setCreatedTime(Uuids.unixTimestamp(timeBased));
            } else if (edgeEvent.getCreatedTime() == 0L) {
                UUID eventId = edgeEvent.getId().getId();
                if (eventId.version() == 1) {
                    edgeEvent.setCreatedTime(Uuids.unixTimestamp(eventId));
                } else {
                    edgeEvent.setCreatedTime(System.currentTimeMillis());
                }
            }
            if (StringUtils.isEmpty(edgeEvent.getUid())) {
                edgeEvent.setUid(edgeEvent.getId().toString());
            }
            return save(new EdgeEventEntity(edgeEvent)).orElse(null);
        } finally {
            readWriteLock.unlock();
        }
    }

    @Override
    public PageData<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate) {
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(new SortOrder("id", SortOrder.Direction.ASC));
        if (pageLink.getSortOrder() != null) {
            sortOrders.add(pageLink.getSortOrder());
        }
        final Lock readWriteLock = readWriteLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
        readWriteLock.lock();
        try {
            if (withTsUpdate) {
                return DaoUtil.toPageData(
                        edgeEventRepository
                                .findEdgeEventsByTenantIdAndEdgeId(
                                        tenantId,
                                        edgeId.getId(),
                                        Objects.toString(pageLink.getTextSearch(), ""),
                                        pageLink.getStartTime(),
                                        pageLink.getEndTime(),
                                        DaoUtil.toPageable(pageLink, sortOrders)));
            } else {
                return DaoUtil.toPageData(
                        edgeEventRepository
                                .findEdgeEventsByTenantIdAndEdgeIdWithoutTimeseriesUpdated(
                                        tenantId,
                                        edgeId.getId(),
                                        Objects.toString(pageLink.getTextSearch(), ""),
                                        pageLink.getStartTime(),
                                        pageLink.getEndTime(),
                                        DaoUtil.toPageable(pageLink, sortOrders)));

            }
        } finally {
            readWriteLock.unlock();
        }
    }

    @Override
    public void cleanupEvents(long ttl) {
        log.info("Going to cleanup old edge events using ttl: {}s", ttl);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("call cleanup_edge_events_by_ttl(?,?)")) {
            stmt.setLong(1, ttl);
            stmt.setLong(2, 0);
            stmt.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(1));
            stmt.execute();
            printWarnings(stmt);
            try (ResultSet resultSet = stmt.getResultSet()) {
                resultSet.next();
                log.info("Total edge events removed by TTL: [{}]", resultSet.getLong(1));
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during edge events TTL task execution ", e);
        }
    }

    public Optional<EdgeEvent> save(EdgeEventEntity entity) {
        log.debug("Save edge event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system edge event with predefined id {}", systemTenantId);
            entity.setTenantId(systemTenantId);
        }
        if (entity.getUuid() == null) {
            entity.setUuid(Uuids.timeBased());
        }
        return Optional.of(DaoUtil.getData(edgeEventRepository.save(entity)));
    }
}
