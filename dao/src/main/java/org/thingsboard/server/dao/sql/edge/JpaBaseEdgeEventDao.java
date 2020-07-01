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
package org.thingsboard.server.dao.sql.edge;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
@Component
@SqlDao
public class JpaBaseEdgeEventDao extends JpaAbstractSearchTimeDao<EdgeEventEntity, EdgeEvent> implements EdgeEventDao {

    private final UUID systemTenantId = NULL_UUID;

    @Autowired
    private EdgeEventRepository edgeEventRepository;

    @Override
    protected Class<EdgeEventEntity> getEntityClass() {
        return EdgeEventEntity.class;
    }

    @Override
    protected CrudRepository<EdgeEventEntity, String> getCrudRepository() {
        return edgeEventRepository;
    }

    @Override
    public ListenableFuture<EdgeEvent> saveAsync(EdgeEvent edgeEvent) {
        log.debug("Save edge event [{}] ", edgeEvent);
        if (edgeEvent.getId() == null) {
            edgeEvent.setId(new EdgeEventId(UUIDs.timeBased()));
        }
        return service.submit(() -> save(new EdgeEventEntity(edgeEvent)).orElse(null));
    }

    @Override
    public List<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, TimePageLink pageLink) {
        Specification<EdgeEventEntity> timeSearchSpec = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "id");
        Specification<EdgeEventEntity> fieldsSpec = getEntityFieldsSpec(tenantId, edgeId);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(0, pageLink.getLimit(), sortDirection, ID_PROPERTY);
        return DaoUtil.convertDataList(edgeEventRepository.findAll(Specification.where(timeSearchSpec).and(fieldsSpec), pageable).getContent());
    }

    public Optional<EdgeEvent> save(EdgeEventEntity entity) {
        log.debug("Save edge event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system edge event with predefined id {}", systemTenantId);
            entity.setTenantId(UUIDConverter.fromTimeUUID(systemTenantId));
        }
        if (entity.getUuid() == null) {
            entity.setUuid(UUIDs.timeBased());
        }
        return Optional.of(DaoUtil.getData(edgeEventRepository.save(entity)));
    }

    private Specification<EdgeEventEntity> getEntityFieldsSpec(UUID tenantId, EdgeId edgeId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(tenantId));
                predicates.add(tenantIdPredicate);
            }
            if (edgeId != null) {
                Predicate entityIdPredicate = criteriaBuilder.equal(root.get("edgeId"), UUIDConverter.fromTimeUUID(edgeId.getId()));
                predicates.add(entityIdPredicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
        };
    }
}
