/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.service.api.IntegrationApiService;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.common.EventDeduplicationExecutor;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbIntegrationExecutorComponent;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@TbIntegrationExecutorComponent
@RequiredArgsConstructor
@Slf4j
public class IntegrationExecutorService extends TbApplicationEventListener<PartitionChangeEvent> {

    private final PartitionService partitionService;
    private final IntegrationApiService apiService;
    private final IntegrationManagerService integrationManagerService;
    private final ConcurrentMap<IntegrationType, EventDeduplicationExecutor<Set<TopicPartitionInfo>>> deduplicationMap = new ConcurrentHashMap<>();
    private ListeningExecutorService refreshExecutorService;

    Map<IntegrationId, Integration> integrationsMap = new ConcurrentHashMap<>();
    Map<ConverterId, Converter> convertersMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshExecutorService = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(4, "default-integration-refresh"));
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        IntegrationType integrationType = IntegrationType.valueOf(event.getServiceQueueKey().getServiceQueue().getQueue());
        deduplicationMap.computeIfAbsent(integrationType, it -> new EventDeduplicationExecutor<>(IntegrationExecutorService.class.getSimpleName(), refreshExecutorService,
                partitions -> refreshIntegrationsByType(integrationType, partitions)))
                .submit(event.getPartitions());
    }

    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return ServiceType.TB_INTEGRATION_EXECUTOR.equals(event.getServiceType());
    }

    private void refreshIntegrationsByType(IntegrationType integrationType, Set<TopicPartitionInfo> partitions) {
        log.info("[{}] managing {} partitions now.", integrationType, partitions);
        try {
            List<IntegrationInfo> activeIntegrationList = apiService.getActiveIntegrationList(integrationType);

            List<ListenableFuture<Integration>> integrationTasks = new ArrayList<>();
            for (IntegrationInfo integration : activeIntegrationList) {
                log.info("[{}] Received Integration: {}", integration.getType(), integration);
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_INTEGRATION_EXECUTOR, integrationType.name(), integration.getTenantId(), integration.getId());
                if (tpi.isMyPartition()) {
                    log.info("[{}] This integration is mine: {}", integration.getType(), integration.getName());
                    integrationTasks.add(apiService.getIntegration(integration.getTenantId(), integration.getId()));
                } else {
                    log.info("[{}] This integration is not mine: {}", integration.getType(), integration.getName());
                }
            }

            //TODO: handle if some of the fetch operations fail.
            ListenableFuture<List<Integration>> integrationsFuture = Futures.successfulAsList(integrationTasks);

            Map<ConverterId, TenantId> convertersIdMap = new HashMap<>();
            DonAsynchron.withCallback(integrationsFuture, integrations -> {
                integrations.forEach(i -> {
                    log.info("[{}][{}] Loaded integration: {}", i.getType(), i.getId(), i);
                    integrationsMap.put(i.getId(), i);
                    convertersIdMap.put(i.getDefaultConverterId(), i.getTenantId());
                    if (i.getDownlinkConverterId() != null) {
                        convertersIdMap.put(i.getDownlinkConverterId(), i.getTenantId());
                    }
                });

                List<ListenableFuture<Converter>> converterTasks = new ArrayList<>();
                for (var converterIdPair : convertersIdMap.entrySet()) {
                    converterTasks.add(apiService.getConverter(converterIdPair.getValue(), converterIdPair.getKey()));
                }

                ListenableFuture<List<Converter>> convertersFuture = Futures.successfulAsList(converterTasks);

                DonAsynchron.withCallback(convertersFuture, converters -> {
                    converters.forEach(c -> {
                        log.info("[{}][{}] Loaded converter: {}", c.getType(), c.getId(), c);
                        convertersMap.put(c.getId(), c);
                    });

                    initEverything();
                }, t -> log.warn("Failed to fetch converters: ", t), MoreExecutors.directExecutor());

            }, t -> log.warn("Failed to fetch integrations: ", t), MoreExecutors.directExecutor());
        } catch (Exception e) {
            log.warn("[{}] Failed to refresh the integrations", integrationType, e);
        }
    }

    private void initEverything() {
        for (Integration integration : integrationsMap.values()) {
            integrationManagerService.getOrCreateIntegration(integration, false);
        }
    }

}
