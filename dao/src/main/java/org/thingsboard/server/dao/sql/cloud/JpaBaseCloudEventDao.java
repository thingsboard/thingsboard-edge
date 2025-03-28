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
package org.thingsboard.server.dao.sql.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.cloud.CloudEventDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.CloudEventEntity;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JpaBaseCloudEventDao extends AbstractJpaCloudEventDao<CloudEventEntity> implements CloudEventDao {

    private final CloudEventRepository cloudEventRepository;
    private final BaseCloudEventInsertRepository<CloudEventEntity> cloudEventInsertRepository;

    public JpaBaseCloudEventDao(ScheduledLogExecutorComponent logExecutor,
                                StatsFactory statsFactory,
                                SqlPartitioningRepository partitioningRepository,
                                CloudEventRepository cloudEventRepository,
                                BaseCloudEventInsertRepository<CloudEventEntity> cloudEventInsertRepository) {
        super(logExecutor, statsFactory, partitioningRepository);
        this.cloudEventRepository = cloudEventRepository;
        this.cloudEventInsertRepository = cloudEventInsertRepository;
    }

    @Override
    protected BaseCloudEventRepository<CloudEventEntity, UUID> getRepository() {
        return cloudEventRepository;
    }

    @Override
    protected String getTableName() {
        return ModelConstants.CLOUD_EVENT_COLUMN_FAMILY_NAME;
    }

    @Override
    protected String getLogName() {
        return "Cloud Event";
    }

    @Override
    protected String getStatsNamePrefix() {
        return "cloud.events";
    }

    @Override
    protected void saveEntities(List<CloudEventEntity> entities) {
        cloudEventInsertRepository.save(entities, getTableName());
    }

    @Override
    protected Class<CloudEventEntity> getEntityClass() {
        return CloudEventEntity.class;
    }

    @Override
    protected CloudEventEntity createEntity(CloudEvent cloudEvent) {
        return new CloudEventEntity(cloudEvent);
    }

    @Override
    public long countEventsByTenantIdAndEntityIdAndActionAndTypeAndStartTimeAndEndTime(UUID tenantId, UUID entityId, CloudEventType cloudEventType, EdgeEventActionType cloudEventAction, Long startTime, Long endTime) {
        return cloudEventRepository.countEventsByTenantIdAndEntityIdAndActionAndTypeAndStartTimeAndEndTime(
                tenantId, entityId, cloudEventType, cloudEventAction, startTime, endTime);
    }

}
