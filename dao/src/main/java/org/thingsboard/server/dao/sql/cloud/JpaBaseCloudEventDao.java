// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
