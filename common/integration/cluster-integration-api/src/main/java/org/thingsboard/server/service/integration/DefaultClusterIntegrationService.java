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
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreIntegrationExecutorQueueFactory;
import org.thingsboard.server.queue.settings.TbQueueIntegrationNotificationSettings;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreOrIntegrationExecutorComponent;
import org.thingsboard.server.queue.util.TbPackCallback;
import org.thingsboard.server.queue.util.TbPackProcessingContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@TbCoreOrIntegrationExecutorComponent
@Service
@RequiredArgsConstructor
public class DefaultClusterIntegrationService extends TbApplicationEventListener<PartitionChangeEvent> implements ClusterIntegrationService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueIntegrationNotificationSettings integrationNotificationSettings;
    private final TbCoreIntegrationExecutorQueueFactory queueFactory;
    private final IntegrationManagerService integrationManagerService;
    private final DataDecodingEncodingService encodingService;
    private final Map<IntegrationType, Queue<Set<TopicPartitionInfo>>> subscribeEventsMap = new ConcurrentHashMap<>();

    private volatile ExecutorService consumersExecutor;
    private volatile ExecutorService notificationsConsumerExecutor;
    private volatile ListeningScheduledExecutorService queueExecutor;
    private final ConcurrentMap<IntegrationType, TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>>> consumers = new ConcurrentHashMap<>();
    private volatile TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> nfConsumer;

    private volatile boolean stopped = false;
    private volatile List<IntegrationType> supportedIntegrationTypes;

    @PostConstruct
    public void init() {
        supportedIntegrationTypes = serviceInfoProvider.getSupportedIntegrationTypes();
        queueExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("scheduler-service")));
        notificationsConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ie-nf-consumer"));
        consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("ie-downlink-consumer"));
        nfConsumer = queueFactory.createToIntegrationExecutorNotificationsMsgConsumer();
        for (IntegrationType integrationType : supportedIntegrationTypes) {
            consumers.computeIfAbsent(integrationType, queueName -> queueFactory.createToIntegrationExecutorDownlinkMsgConsumer(integrationType));
        }
    }

    @PreDestroy
    public void stop() {
        stopped = true;
        if (nfConsumer != null) {
            nfConsumer.unsubscribe();
        }
        consumers.values().forEach(TbQueueConsumer::unsubscribe);
        if (queueExecutor != null) {
            queueExecutor.shutdownNow();
        }
        if (notificationsConsumerExecutor != null) {
            notificationsConsumerExecutor.shutdownNow();
        }
        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        IntegrationType type = IntegrationType.valueOf(event.getQueueKey().getQueueName());
        subscribeEventsMap.computeIfAbsent(type, t -> new ConcurrentLinkedQueue<>()).add(event.getPartitions());
        queueExecutor.submit(() -> refreshIntegrationsByType(type));
        consumers.get(type).subscribe(event.getPartitions());
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        boolean supported = !supportedIntegrationTypes.isEmpty();

        if (supported || serviceInfoProvider.isService(ServiceType.TB_CORE)) {
            log.info("Subscribing to notifications: {}", nfConsumer.getTopic());
            this.nfConsumer.subscribe();
            launchNotificationsConsumer();
        }

        if (supported) {
            log.info("Launch main consumers");
            launchMainConsumers();
        }
    }

    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return ServiceType.TB_INTEGRATION_EXECUTOR.equals(event.getServiceType());
    }

    protected void launchMainConsumers() {
        consumers.forEach((integrationType, consumer) -> launchConsumer(consumer, integrationType));
    }

    void launchConsumer(TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> consumer, IntegrationType integrationType) {
        consumersExecutor.execute(() -> consumerLoop(consumer, integrationType));
    }

    void consumerLoop(TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> consumer, IntegrationType integrationType) {
        ThingsBoardThreadFactory.updateCurrentThreadName(integrationType.name());
        long pollDuration = integrationNotificationSettings.getPollInterval();
        long processingTimeout = integrationNotificationSettings.getPackProcessingTimeout();
        while (!stopped && !consumer.isStopped()) {
            try {
                List<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> msgs = consumer.poll(pollDuration);
                if (msgs.isEmpty()) {
                    continue;
                }
                ConcurrentMap<UUID, TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> pendingMap = msgs.stream().collect(
                        Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                TbPackProcessingContext<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> ctx = new TbPackProcessingContext<>(
                        processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                pendingMap.forEach((id, msg) -> {
                    log.trace("[{}] Creating downlink callback for message: {}", id, msg.getValue());
                    TbCallback callback = new TbPackCallback<>(id, ctx);
                    try {
                        handleDownlink(id, msg, callback);
                    } catch (Throwable e) {
                        log.warn("[{}] Failed to process notification: {}", id, msg, e);
                        callback.onFailure(e);
                    }
                });
                if (!processingTimeoutLatch.await(processingTimeout, TimeUnit.MILLISECONDS)) {
                    ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process downlink: {}", id, msg.getValue()));
                    ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process downlink: {}", id, msg.getValue()));
                }
                consumer.commit();
            } catch (Exception e) {
                if (!stopped) {
                    log.warn("Failed to obtain downlink messages from queue.", e);
                    try {
                        Thread.sleep(pollDuration);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new downlink messages", e2);
                    }
                }
            }
        }
        log.info("TB Integration Downlink Consumer stopped.");
    }

    protected void launchNotificationsConsumer() {
        notificationsConsumerExecutor.submit(() -> {
            long pollDuration = integrationNotificationSettings.getPollInterval();
            long processingTimeout = integrationNotificationSettings.getPackProcessingTimeout();
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> msgs = nfConsumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> pendingMap = msgs.stream().collect(
                            Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    TbPackProcessingContext<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    pendingMap.forEach((id, msg) -> {
                        log.trace("[{}] Creating notification callback for message: {}", id, msg.getValue());
                        TbCallback callback = new TbPackCallback<>(id, ctx);
                        try {
                            handleNotification(id, msg, callback);
                        } catch (Throwable e) {
                            log.warn("[{}] Failed to process notification: {}", id, msg, e);
                            callback.onFailure(e);
                        }
                    });
                    if (!processingTimeoutLatch.await(processingTimeout, TimeUnit.MILLISECONDS)) {
                        ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process notification: {}", id, msg.getValue()));
                        ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process notification: {}", id, msg.getValue()));
                    }
                    nfConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain notifications from queue.", e);
                        try {
                            Thread.sleep(pollDuration);
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new notifications", e2);
                        }
                    }
                }
            }
            log.info("TB Integration Notifications Consumer stopped.");
        });
    }


    private void handleDownlink(UUID id, TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg> msg, TbCallback callback) {
        log.trace("Received downlink: {}", msg);
        var downlinkMsg = msg.getValue();
        if (downlinkMsg.hasDownlinkMsg()) {
            integrationManagerService.handleDownlink(downlinkMsg.getDownlinkMsg(), callback);
        } else if (downlinkMsg.hasValidationRequestMsg()) {
            integrationManagerService.handleValidationRequest(downlinkMsg.getValidationRequestMsg(), callback);
        } else {
            callback.onSuccess();
        }
    }

    private void handleNotification(UUID id, TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg> msg, TbCallback callback) {
        ToIntegrationExecutorNotificationMsg nf = msg.getValue();
        if (!nf.getComponentLifecycleMsg().isEmpty()) {
            handleComponentLifecycleMsg(id, nf.getComponentLifecycleMsg());
            callback.onSuccess();
        }
    }

    protected void handleComponentLifecycleMsg(UUID id, ByteString nfMsg) {
        Optional<TbActorMsg> actorMsgOpt = encodingService.decode(nfMsg.toByteArray());
        if (actorMsgOpt.isPresent()) {
            TbActorMsg actorMsg = actorMsgOpt.get();
            if (actorMsg instanceof ComponentLifecycleMsg) {
                ComponentLifecycleMsg componentLifecycleMsg = (ComponentLifecycleMsg) actorMsg;
                log.info("[{}][{}][{}] Received Lifecycle event: {}", componentLifecycleMsg.getTenantId(),
                        componentLifecycleMsg.getEntityId().getEntityType(),
                        componentLifecycleMsg.getEntityId(), componentLifecycleMsg.getEvent());
                integrationManagerService.handleComponentLifecycleMsg(componentLifecycleMsg);
            }
            log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg);
        }
    }

    private void refreshIntegrationsByType(IntegrationType type) {
        try {
            Set<TopicPartitionInfo> partitions = getLatestPartitionsFromQueue(type);
            if (partitions != null) {
                integrationManagerService.refresh(type, partitions);
            }
        } catch (Throwable t) {
            log.warn("[{}] Failed to refresh integrations", type, t);
        }
    }

    private Set<TopicPartitionInfo> getLatestPartitionsFromQueue(IntegrationType type) {
        var queue = subscribeEventsMap.get(type);
        log.debug("[{}] getLatestPartitionsFromQueue, queue size {}", type, queue.size());
        Set<TopicPartitionInfo> partitions = null;
        while (!queue.isEmpty()) {
            partitions = queue.poll();
            log.debug("[{}] polled from the queue partitions {}", type, partitions);
        }
        log.debug("[{}] getLatestPartitionsFromQueue, partitions {}", type, partitions);
        return partitions;
    }

}
