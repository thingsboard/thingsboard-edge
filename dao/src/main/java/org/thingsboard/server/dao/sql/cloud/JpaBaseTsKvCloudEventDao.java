/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.TsKvCloudEventEntity;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Primary
@Component
public class JpaBaseTsKvCloudEventDao extends AbstractJpaCloudEventDao<TsKvCloudEventEntity> {

    private final TsKvCloudEventRepository tsKvCloudEventRepository;
    private final BaseCloudEventInsertRepository<TsKvCloudEventEntity> tsKvCloudEventInsertRepository;

    public JpaBaseTsKvCloudEventDao(ScheduledLogExecutorComponent logExecutor,
                                    StatsFactory statsFactory,
                                    SqlPartitioningRepository partitioningRepository,
                                    TsKvCloudEventRepository tsKvCloudEventRepository,
                                    BaseCloudEventInsertRepository<TsKvCloudEventEntity> tsKvCloudEventInsertRepository) {
        super(logExecutor, statsFactory, partitioningRepository);
        this.tsKvCloudEventRepository = tsKvCloudEventRepository;
        this.tsKvCloudEventInsertRepository = tsKvCloudEventInsertRepository;
    }

    @Override
    protected BaseCloudEventRepository<TsKvCloudEventEntity, UUID> getRepository() {
        return tsKvCloudEventRepository;
    }

    @Override
    protected String getTableName() {
        return ModelConstants.TS_KV_CLOUD_EVENT_COLUMN_FAMILY_NAME;
    }

    @Override
    protected String getLogName() {
        return "TsKv Cloud Event";
    }

    @Override
    protected String getStatsNamePrefix() {
        return "tskv.cloud.events";
    }

    @Override
    protected void saveEntities(List<TsKvCloudEventEntity> entities) {
        tsKvCloudEventInsertRepository.save(entities, getTableName());
    }

    @Override
    protected Class<TsKvCloudEventEntity> getEntityClass() {
        return TsKvCloudEventEntity.class;
    }

    @Override
    protected TsKvCloudEventEntity createEntity(CloudEvent cloudEvent) {
        return new TsKvCloudEventEntity(cloudEvent);
    }

}
