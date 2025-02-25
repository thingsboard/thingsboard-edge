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
