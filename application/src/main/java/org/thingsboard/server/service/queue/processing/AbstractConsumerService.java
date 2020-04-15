/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;
import org.thingsboard.server.service.queue.TbPackCallback;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractConsumerService<N extends com.google.protobuf.GeneratedMessageV3> implements ApplicationListener<PartitionChangeEvent> {

    protected volatile ExecutorService consumersExecutor;
    protected volatile ExecutorService notificationsConsumerExecutor;
    protected volatile boolean stopped = false;

    protected final ActorSystemContext actorContext;
    protected final DataDecodingEncodingService encodingService;

    protected final TbQueueConsumer<TbProtoQueueMsg<N>> nfConsumer;

    public AbstractConsumerService(ActorSystemContext actorContext, DataDecodingEncodingService encodingService, TbQueueConsumer<TbProtoQueueMsg<N>> nfConsumer) {
        this.actorContext = actorContext;
        this.encodingService = encodingService;
        this.nfConsumer = nfConsumer;
    }

    public void init(String mainConsumerThreadName, String nfConsumerThreadName) {
        this.consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(mainConsumerThreadName));
        this.notificationsConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(nfConsumerThreadName));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Subscribing to notifications: {}", nfConsumer.getTopic());
        this.nfConsumer.subscribe();
        launchNotificationsConsumer();
        launchMainConsumers();
    }

    protected abstract ServiceType getServiceType();

    protected abstract void launchMainConsumers();

    protected abstract void stopMainConsumers();

    protected abstract long getNotificationPollDuration();

    protected abstract long getNotificationPackProcessingTimeout();

    protected void launchNotificationsConsumer() {
        notificationsConsumerExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<N>> msgs = nfConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    ConcurrentMap<UUID, TbProtoQueueMsg<N>> pendingMap = msgs.stream().collect(
                            Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                    ConcurrentMap<UUID, TbProtoQueueMsg<N>> failedMap = new ConcurrentHashMap<>();
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    pendingMap.forEach((id, msg) -> {
                        log.trace("[{}] Creating notification callback for message: {}", id, msg.getValue());
                        TbCallback callback = new TbPackCallback<>(id, processingTimeoutLatch, pendingMap, failedMap);
                        try {
                            handleNotification(id, msg, callback);
                        } catch (Throwable e) {
                            log.warn("[{}] Failed to process notification: {}", id, msg, e);
                            callback.onFailure(e);
                        }
                    });
                    if (!processingTimeoutLatch.await(getNotificationPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
                        pendingMap.forEach((id, msg) -> log.warn("[{}] Timeout to process notification: {}", id, msg.getValue()));
                        failedMap.forEach((id, msg) -> log.warn("[{}] Failed to process notification: {}", id, msg.getValue()));
                    }
                    nfConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain notifications from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new notifications", e2);
                        }
                    }
                }
            }
            log.info("TB Notifications Consumer stopped.");
        });
    }

    protected abstract void handleNotification(UUID id, TbProtoQueueMsg<N> msg, TbCallback callback) throws Exception;

    @PreDestroy
    public void destroy() {
        stopped = true;

        stopMainConsumers();

        if (nfConsumer != null) {
            nfConsumer.unsubscribe();
        }

        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
        if (notificationsConsumerExecutor != null) {
            notificationsConsumerExecutor.shutdownNow();
        }
    }
}
