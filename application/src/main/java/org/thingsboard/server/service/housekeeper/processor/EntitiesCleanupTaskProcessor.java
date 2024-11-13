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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.housekeeper.EntitiesCleanupHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.EntitiesDeletionHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitiesCleanupTaskProcessor extends HousekeeperTaskProcessor<EntitiesCleanupHousekeeperTask> {

    private final EntityDaoRegistry entityDaoRegistry;
    @Value("${queue.core.housekeeper.check-expiration-frequency:3600}")
    private long frequency;
    private ScheduledExecutorService executor;

    @PostConstruct
    public void init() {
        executor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("housekeeper-scheduler-" + getTaskType().name()));
        for (EntityType entityType : EntityType.values()) {
            if (entityType.isHasTtl()) {
                schedule(entityType);
            }
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

        UUID last = null;
        while (true) {
            List<TbPair<UUID, UUID>> pairs = entityDao.findIdsByTenantIdAndIdOffsetAndExpired(last, 128);

            if (pairs.isEmpty()) {
                break;
            }
            last = pairs.get(pairs.size() - 1).getSecond();

            pairs.stream()
                    .collect(Collectors.groupingBy(
                            pair -> TenantId.fromUUID(pair.getFirst()),
                            Collectors.mapping(TbPair::getSecond, Collectors.toList())))
                    .forEach((tenantId, entities) -> {
                        housekeeperClient.submitTask(new EntitiesDeletionHousekeeperTask(TenantId.SYS_TENANT_ID, entityType, entities));
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

}
