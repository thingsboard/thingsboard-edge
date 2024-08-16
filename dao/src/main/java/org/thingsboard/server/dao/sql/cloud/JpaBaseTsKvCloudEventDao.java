/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.CloudEventId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.cloud.TsKvCloudEventDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.TsKvCloudEventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaBaseTsKvCloudEventDao extends JpaAbstractDao<TsKvCloudEventEntity, CloudEvent> implements TsKvCloudEventDao {

    private final UUID systemTenantId = NULL_UUID;

    private final ScheduledLogExecutorComponent logExecutor;

    private final StatsFactory statsFactory;

    private final TsKvCloudEventRepository tsKvCloudEventRepository;

    private final BaseCloudEventInsertRepository<TsKvCloudEventEntity> tsKvCloudEventInsertRepository;

    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.cloud_events.batch_size:10000}")
    private int batchSize;

    @Value("${sql.cloud_events.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.cloud_events.stats_print_interval_ms:10000}")
    private long statsPrintIntervalMs;

    @Value("${sql.cloud_events.partition_size:24}")
    private int partitionSizeInHours;

    private static final String TABLE_NAME = ModelConstants.TS_KV_CLOUD_EVENT_COLUMN_FAMILY_NAME;

    private TbSqlBlockingQueueWrapper<TsKvCloudEventEntity, Void> queue;

    @Override
    protected Class<TsKvCloudEventEntity> getEntityClass() {
        return TsKvCloudEventEntity.class;
    }

    @Override
    protected JpaRepository<TsKvCloudEventEntity, UUID> getRepository() {
        return tsKvCloudEventRepository;
    }

    @PostConstruct
    private void init() {
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("TsKv Cloud Events")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("tskv.cloud.events")
                .batchSortEnabled(true)
                .build();
        Function<TsKvCloudEventEntity, Integer> hashcodeFunction = entity -> {
            if (entity.getEntityId() != null) {
                return entity.getEntityId().hashCode();
            } else {
                return NULL_UUID.hashCode();
            }
        };
        queue = new TbSqlBlockingQueueWrapper<>(params, hashcodeFunction, 1, statsFactory);
        queue.init(logExecutor, entities -> tsKvCloudEventInsertRepository.save(entities, TABLE_NAME),
                Comparator.comparing(TsKvCloudEventEntity::getTs)
        );
    }

    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    @Override
    public ListenableFuture<Void> saveAsync(CloudEvent cloudEvent) {
        log.debug("Save tsKv cloud event [{}] ", cloudEvent);
        if (cloudEvent.getId() == null) {
            UUID timeBased = Uuids.timeBased();
            cloudEvent.setId(new CloudEventId(timeBased));
            cloudEvent.setCreatedTime(Uuids.unixTimestamp(timeBased));
        } else if (cloudEvent.getCreatedTime() == 0L) {
            UUID eventId = cloudEvent.getId().getId();
            if (eventId.version() == 1) {
                cloudEvent.setCreatedTime(Uuids.unixTimestamp(eventId));
            } else {
                cloudEvent.setCreatedTime(System.currentTimeMillis());
            }
        }
        partitioningRepository.createPartitionIfNotExists(TABLE_NAME, cloudEvent.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
        return save(new TsKvCloudEventEntity(cloudEvent));
    }

    private ListenableFuture<Void> save(TsKvCloudEventEntity entity) {
        log.debug("Save cloud event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system tsKv cloud event with predefined id {}", systemTenantId);
            entity.setTenantId(systemTenantId);
        }
        if (entity.getUuid() == null) {
            entity.setUuid(Uuids.timeBased());
        }

        return addToQueue(entity);
    }

    private ListenableFuture<Void> addToQueue(TsKvCloudEventEntity entity) {
        return queue.add(entity);
    }

    @Override
    public PageData<CloudEvent> findTsKvCloudEvents(UUID tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        log.trace("Executing findTsKvCloudEvents [{}], [{}], [{}], [{}]", tenantId, seqIdStart, seqIdEnd, pageLink);
        List<SortOrder> sortOrders = new ArrayList<>();
        if (pageLink.getSortOrder() != null) {
            sortOrders.add(pageLink.getSortOrder());
        }
        sortOrders.add(new SortOrder("seqId"));
        return DaoUtil.toPageData(
                tsKvCloudEventRepository
                        .findEventsByTenantId(
                                tenantId,
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                seqIdStart,
                                seqIdEnd,
                                DaoUtil.toPageable(pageLink, sortOrders)));
    }

    @Override
    public void cleanupEvents(long ttl) {
        log.info("Going to cleanup old cloud events using debug events ttl: {}s", ttl);
        partitioningRepository.dropPartitionsBefore(TABLE_NAME, ttl, TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

}
