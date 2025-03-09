/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.QueueStatsFields;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.QueueStatsEntity;
import org.thingsboard.server.dao.queue.QueueStatsDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;

@Slf4j
@Component
@SqlDao
public class JpaQueueStatsDao extends JpaAbstractDao<QueueStatsEntity, QueueStats> implements QueueStatsDao {

    @Autowired
    private QueueStatsRepository queueStatsRepository;

    @Override
    protected Class<QueueStatsEntity> getEntityClass() {
        return QueueStatsEntity.class;
    }

    @Override
    protected JpaRepository<QueueStatsEntity, UUID> getRepository() {
        return queueStatsRepository;
    }

    @Override
    public QueueStats findByTenantIdQueueNameAndServiceId(TenantId tenantId, String queueName, String serviceId) {
        return DaoUtil.getData(queueStatsRepository.findByTenantIdAndQueueNameAndServiceId(tenantId.getId(), queueName, serviceId));
    }

    @Override
    public PageData<QueueStats> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(queueStatsRepository.findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        queueStatsRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public List<QueueStats> findByIds(TenantId tenantId, List<QueueStatsId> queueStatsIds) {
        return DaoUtil.convertDataList(queueStatsRepository.findByTenantIdAndIdIn(tenantId.getId(), toUUIDs(queueStatsIds)));
    }

    @Override
    public List<QueueStatsFields> findNextBatch(UUID id, int batchSize) {
        return queueStatsRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.QUEUE_STATS;
    }

}
