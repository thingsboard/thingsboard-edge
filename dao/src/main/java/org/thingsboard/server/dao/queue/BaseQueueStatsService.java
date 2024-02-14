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
package org.thingsboard.server.dao.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("QueueStatsDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseQueueStatsService extends AbstractEntityService implements QueueStatsService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final QueueStatsDao queueStatsDao;

    private final DataValidator<QueueStats> queueStatsValidator;

    @Override
    public QueueStats save(TenantId tenantId, QueueStats queueStats) {
        log.trace("Executing save [{}]", queueStats);
        queueStatsValidator.validate(queueStats, QueueStats::getTenantId);
        return queueStatsDao.save(tenantId, queueStats);
    }

    @Override
    public QueueStats findQueueStatsById(TenantId tenantId, QueueStatsId queueStatsId) {
        log.trace("Executing findQueueStatsById [{}]", queueStatsId);
        validateId(queueStatsId, "Incorrect queueStatsId " + queueStatsId);
        return queueStatsDao.findById(tenantId, queueStatsId.getId());
    }

    @Override
    public QueueStats findByTenantIdAndNameAndServiceId(TenantId tenantId, String queueName, String serviceId) {
        log.trace("Executing findByTenantIdAndNameAndServiceId, tenantId: [{}], queueName: [{}], serviceId: [{}]", tenantId, queueName, serviceId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return queueStatsDao.findByTenantIdQueueNameAndServiceId(tenantId, queueName, serviceId);
    }

    @Override
    public List<QueueStats> findByTenantId(TenantId tenantId) {
        log.trace("Executing findByTenantId, tenantId: [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return queueStatsDao.findByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        queueStatsDao.deleteByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findQueueStatsById(tenantId, new QueueStatsId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.QUEUE_STATS;
    }
}
