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
package org.thingsboard.server.service.housekeeper.processor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.housekeeper.EntitiesCleanupHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.EntitiesDeletionHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.tenant.TenantProfileService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.EntityType.BLOB_ENTITY;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitiesCleanupTaskProcessor extends HousekeeperTaskProcessor<EntitiesCleanupHousekeeperTask> {

    private final EntityType[] typesWithTtl = {BLOB_ENTITY};

    private final EntityDaoRegistry entityDaoRegistry;
    private final TenantProfileService tenantProfileService;
    @Value("${queue.core.housekeeper.check-expiration-frequency:3600}")
    private long frequency;
    @Value("${sql.ttl.blob_entities.enabled:false}")
    private boolean blobTtlEnabled;
    @Value("${sql.ttl.blob_entities.ttl:0}")
    private long blobTtlSec;
    private ScheduledExecutorService executor;

    @PostConstruct
    public void init() {
        executor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("housekeeper-scheduler-" + getTaskType().name()));
        for (EntityType entityType : typesWithTtl) {
            schedule(entityType);
        }
    }

    private void schedule(EntityType entityType) {
        executor.scheduleAtFixedRate(() ->
                housekeeperClient.submitTask(new EntitiesCleanupHousekeeperTask(entityType)), frequency, frequency, TimeUnit.SECONDS);
    }

    @Override
    public void process(EntitiesCleanupHousekeeperTask task) throws Exception {
        EntityType entityType = task.getEntityType();
        Dao<?> entityDao = entityDaoRegistry.getDao(entityType);
        PageDataIterable<TenantProfile> profilesIterator =
                new PageDataIterable<>(page -> tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, page), 128);
        for (TenantProfile tenantProfile : profilesIterator) {
            long ttl = getTtl(tenantProfile, entityType);
            if (ttl > 0) {
                process(tenantProfile.getUuidId(), entityType, entityDao, ttl);
            }
        }
    }

    private void process(UUID tenantProfileId, EntityType entityType, Dao<?> entityDao, long ttl) {
        UUID last = null;
        while (true) {
            List<TbPair<UUID, UUID>> pairs = entityDao.findIdsByTenantProfileIdAndIdOffsetAndExpired(tenantProfileId, last, 128, ttl);

            if (pairs.isEmpty()) {
                break;
            }
            last = pairs.get(pairs.size() - 1).getSecond();

            pairs.stream()
                    .collect(Collectors.groupingBy(
                            pair -> TenantId.fromUUID(pair.getFirst()),
                            Collectors.mapping(TbPair::getSecond, Collectors.toList())))
                    .forEach((tenantId, entities) -> {
                        housekeeperClient.submitTask(new EntitiesDeletionHousekeeperTask(tenantId, entityType, entities));
                        log.debug("[{}] Submitted task for deleting {} {}s", tenantId, entities.size(), entityType.getNormalName().toLowerCase());
                    });
        }
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.CLEANUP_ENTITIES;
    }

    @PreDestroy
    private void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public long getTtl(TenantProfile tenantProfile, EntityType entityType) {
        var configuration = tenantProfile.getDefaultProfileConfiguration();
        long ttlSec = switch (entityType) {
            case BLOB_ENTITY ->
                    computeTtl(blobTtlEnabled, blobTtlSec, TimeUnit.DAYS.toSeconds(configuration.getBlobEntityTtlDays()));
            default -> throw new IllegalArgumentException("Unsupported entity type: " + entityType);
        };
        return TimeUnit.SECONDS.toMillis(ttlSec);
    }

    private long computeTtl(boolean systemTtlEnabled, long systemTtl, long ttl) {
        if (systemTtlEnabled && systemTtl > 0) {
            if (ttl == 0) {
                ttl = systemTtl;
            } else {
                ttl = Math.min(systemTtl, ttl);
            }
        }
        return ttl;
    }

}
