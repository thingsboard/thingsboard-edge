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
package org.thingsboard.server.dao.sql.scheduler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.model.sql.SchedulerEventEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.scheduler.SchedulerEventDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class JpaSchedulerEventDao extends JpaAbstractSearchTextDao<SchedulerEventEntity, SchedulerEvent> implements SchedulerEventDao {

    @Autowired
    private RelationDao relationDao;

    @Autowired
    SchedulerEventRepository schedulerEventRepository;

    @Override
    protected Class<SchedulerEventEntity> getEntityClass() {
        return SchedulerEventEntity.class;
    }

    @Override
    protected CrudRepository<SchedulerEventEntity, UUID> getCrudRepository() {
        return schedulerEventRepository;
    }

    @Override
    public ListenableFuture<List<SchedulerEvent>> findSchedulerEventsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find scheduler events by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        ListenableFuture<PageData<EntityRelation>> relations =
                relationDao.findRelations(
                        new TenantId(tenantId),
                        new EdgeId(edgeId),
                        EntityRelation.CONTAINS_TYPE,
                        RelationTypeGroup.EDGE,
                        EntityType.SCHEDULER_EVENT,
                        pageLink);
        return Futures.transformAsync(relations, input -> {
            if (input != null && input.getData() != null) {
                List<ListenableFuture<SchedulerEvent>> schedulerEventFutures = new ArrayList<>(input.getData().size());
                for (EntityRelation relation : input.getData()) {
                    schedulerEventFutures.add(findByIdAsync(new TenantId(tenantId), relation.getTo().getId()));
                }
                return Futures.successfulAsList(schedulerEventFutures);
            } else {
                return Futures.immediateFuture(new ArrayList<>());
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return schedulerEventRepository.countByTenantId(tenantId.getId());
    }
}
