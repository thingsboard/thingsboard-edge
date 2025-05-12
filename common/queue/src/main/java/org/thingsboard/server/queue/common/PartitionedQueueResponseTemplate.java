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
package org.thingsboard.server.queue.common;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
public class PartitionedQueueResponseTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> extends AbstractTbQueueTemplate {

    @Getter
    private final PartitionedQueueConsumerManager<Request> requestConsumer;
    private final TbQueueProducer<Response> responseProducer;

    private final TbQueueHandler<Request, Response> handler;
    private final long pollInterval;
    private final int maxPendingRequests;
    private final long requestTimeout;
    private final MessagesStats stats;

    private final ScheduledExecutorService scheduler;
    private final ExecutorService callbackExecutor;

    private final AtomicInteger pendingRequestCount = new AtomicInteger();

    @Builder
    public PartitionedQueueResponseTemplate(String key,
                                            TbQueueHandler<Request, Response> handler,
                                            String requestsTopic,
                                            Function<TopicPartitionInfo, TbQueueConsumer<Request>> consumerCreator,
                                            TbQueueProducer<Response> responseProducer,
                                            long pollInterval,
                                            long requestTimeout,
                                            int maxPendingRequests,
                                            ExecutorService consumerExecutor,
                                            ExecutorService callbackExecutor,
                                            ExecutorService consumerTaskExecutor,
                                            MessagesStats stats) {
        this.scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor(key + "-queue-response-template-scheduler");
        this.callbackExecutor = callbackExecutor;
        this.handler = handler;
        this.requestConsumer = PartitionedQueueConsumerManager.<Request>create()
                .queueKey(key + "-requests")
                .topic(requestsTopic)
                .pollInterval(pollInterval)
                .msgPackProcessor((requests, consumer, config) -> processRequests(requests, consumer))
                .consumerCreator((config, tpi) -> consumerCreator.apply(tpi))
                .consumerExecutor(consumerExecutor)
                .scheduler(scheduler)
                .taskExecutor(consumerTaskExecutor)
                .build();
        this.responseProducer = responseProducer;
        this.pollInterval = pollInterval;
        this.maxPendingRequests = maxPendingRequests;
        this.requestTimeout = requestTimeout;
        this.stats = stats;
    }

    private void processRequests(List<Request> requests, TbQueueConsumer<Request> consumer) {
        while (pendingRequestCount.get() >= maxPendingRequests) {
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                log.trace("Failed to wait until the server has capacity to handle new requests", e);
            }
        }

        requests.forEach(request -> {
            long currentTime = System.currentTimeMillis();
            long expireTs = bytesToLong(request.getHeaders().get(EXPIRE_TS_HEADER));
            if (expireTs >= currentTime) {
                byte[] requestIdHeader = request.getHeaders().get(REQUEST_ID_HEADER);
                if (requestIdHeader == null) {
                    log.error("[{}] Missing requestId in header", request);
                    return;
                }
                byte[] responseTopicHeader = request.getHeaders().get(RESPONSE_TOPIC_HEADER);
                if (responseTopicHeader == null) {
                    log.error("[{}] Missing response topic in header", request);
                    return;
                }
                UUID requestId = bytesToUuid(requestIdHeader);
                String responseTopic = bytesToString(responseTopicHeader);
                try {
                    pendingRequestCount.getAndIncrement();
                    stats.incrementTotal();
                    AsyncCallbackTemplate.withCallbackAndTimeout(handler.handle(request),
                            response -> {
                                pendingRequestCount.decrementAndGet();
                                response.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
                                responseProducer.send(TopicPartitionInfo.builder().topic(responseTopic).build(), response, null);
                                stats.incrementSuccessful();
                            },
                            e -> {
                                pendingRequestCount.decrementAndGet();
                                if (e.getCause() != null && e.getCause() instanceof TimeoutException) {
                                    log.warn("[{}] Timeout to process the request: {}", requestId, request, e);
                                } else {
                                    log.trace("[{}] Failed to process the request: {}", requestId, request, e);
                                }
                                stats.incrementFailed();
                            },
                            requestTimeout,
                            scheduler,
                            callbackExecutor);
                } catch (Throwable e) {
                    pendingRequestCount.decrementAndGet();
                    log.warn("[{}] Failed to process the request: {}", requestId, request, e);
                    stats.incrementFailed();
                }
            }
        });
        consumer.commit();
    }

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        requestConsumer.update(partitions);
    }

    public void stop() {
        if (requestConsumer != null) {
            requestConsumer.stop();
            requestConsumer.awaitStop();
        }
        if (responseProducer != null) {
            responseProducer.stop();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

}
