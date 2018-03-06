/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.dao.sql.audit;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.annotation.PreDestroy;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.springframework.data.jpa.domain.Specifications.where;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;

@Component
@SqlDao
public class JpaAuditLogDao extends JpaAbstractDao<AuditLogEntity, AuditLog> implements AuditLogDao {

    private ListeningExecutorService insertService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    protected Class<AuditLogEntity> getEntityClass() {
        return AuditLogEntity.class;
    }

    @Override
    protected CrudRepository<AuditLogEntity, String> getCrudRepository() {
        return auditLogRepository;
    }

    @PreDestroy
    void onDestroy() {
        insertService.shutdown();
    }

    @Override
    public ListenableFuture<Void> saveByTenantId(AuditLog auditLog) {
        return insertService.submit(() -> {
            save(auditLog);
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndEntityId(AuditLog auditLog) {
        return insertService.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndCustomerId(AuditLog auditLog) {
        return insertService.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndUserId(AuditLog auditLog) {
        return insertService.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> savePartitionsByTenantId(AuditLog auditLog) {
        return insertService.submit(() -> null);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        return findAuditLogs(tenantId, entityId, null, null, pageLink);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndCustomerId(UUID tenantId, CustomerId customerId, TimePageLink pageLink) {
        return findAuditLogs(tenantId, null, customerId, null, pageLink);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndUserId(UUID tenantId, UserId userId, TimePageLink pageLink) {
        return findAuditLogs(tenantId, null, null, userId, pageLink);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantId(UUID tenantId, TimePageLink pageLink) {
        return findAuditLogs(tenantId, null, null, null, pageLink);
    }

    private List<AuditLog> findAuditLogs(UUID tenantId, EntityId entityId, CustomerId customerId, UserId userId, TimePageLink pageLink) {
        Specification<AuditLogEntity> timeSearchSpec = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "id");
        Specification<AuditLogEntity> fieldsSpec = getEntityFieldsSpec(tenantId, entityId, customerId, userId);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, ID_PROPERTY);
        return DaoUtil.convertDataList(auditLogRepository.findAll(where(timeSearchSpec).and(fieldsSpec), pageable).getContent());
    }

    private Specification<AuditLogEntity> getEntityFieldsSpec(UUID tenantId, EntityId entityId, CustomerId customerId, UserId userId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(tenantId));
                predicates.add(tenantIdPredicate);
            }
            if (entityId != null) {
                Predicate entityTypePredicate = criteriaBuilder.equal(root.get("entityType"), entityId.getEntityType());
                predicates.add(entityTypePredicate);
                Predicate entityIdPredicate = criteriaBuilder.equal(root.get("entityId"), UUIDConverter.fromTimeUUID(entityId.getId()));
                predicates.add(entityIdPredicate);
            }
            if (customerId != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(customerId.getId()));
                predicates.add(tenantIdPredicate);
            }
            if (userId != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("userId"), UUIDConverter.fromTimeUUID(userId.getId()));
                predicates.add(tenantIdPredicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
        };
    }
}
