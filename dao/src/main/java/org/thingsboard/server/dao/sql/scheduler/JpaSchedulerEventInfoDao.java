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
package org.thingsboard.server.dao.sql.scheduler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.SchedulerEventInfoEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.scheduler.SchedulerEventInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUIDs;

@Component
@SqlDao
@Slf4j
public class JpaSchedulerEventInfoDao extends JpaAbstractSearchTextDao<SchedulerEventInfoEntity, SchedulerEventInfo> implements SchedulerEventInfoDao {

    @Autowired
    SchedulerEventInfoRepository schedulerEventInfoRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<SchedulerEventInfoEntity> getEntityClass() {
        return SchedulerEventInfoEntity.class;
    }

    @Override
    protected CrudRepository<SchedulerEventInfoEntity, String> getCrudRepository() {
        return schedulerEventInfoRepository;
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantId(UUID tenantId) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findByTenantId(
                        UUIDConverter.fromTimeUUID(tenantId)));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndType(UUID tenantId, String type) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findByTenantIdAndType(
                        UUIDConverter.fromTimeUUID(tenantId),
                        type));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndCustomerId(UUID tenantId, UUID customerId) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findByTenantIdAndCustomerId(
                        UUIDConverter.fromTimeUUID(tenantId),
                        UUIDConverter.fromTimeUUID(customerId)));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findByTenantIdAndCustomerIdAndType(
                        UUIDConverter.fromTimeUUID(tenantId),
                        UUIDConverter.fromTimeUUID(customerId),
                        type));
    }

    @Override
    public ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> schedulerEventIds) {
        return service.submit(() -> DaoUtil.convertDataList(schedulerEventInfoRepository.findSchedulerEventsByTenantIdAndIdIn(UUIDConverter.fromTimeUUID(tenantId), fromTimeUUIDs(schedulerEventIds))));
    }

    @Override
    public ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, TimePageLink pageLink) {
        log.debug("Try to find scheduler events by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findRelations(new TenantId(tenantId), new EdgeId(edgeId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EntityType.SCHEDULER_EVENT, pageLink);
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<SchedulerEventInfo>> schedulerEventFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                schedulerEventFutures.add(findByIdAsync(new TenantId(tenantId), relation.getTo().getId()));
            }
            return Futures.successfulAsList(schedulerEventFutures);
        }, MoreExecutors.directExecutor());
    }
}
