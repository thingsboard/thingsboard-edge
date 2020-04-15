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
package org.thingsboard.server.queue.common;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Slf4j
public class DefaultTbQueueRequestTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> extends AbstractTbQueueTemplate
        implements TbQueueRequestTemplate<Request, Response> {

    private final TbQueueAdmin queueAdmin;
    private final TbQueueProducer<Request> requestTemplate;
    private final TbQueueConsumer<Response> responseTemplate;
    private final ConcurrentMap<UUID, DefaultTbQueueRequestTemplate.ResponseMetaData<Response>> pendingRequests;
    private final boolean internalExecutor;
    private final ExecutorService executor;
    private final long maxRequestTimeout;
    private final long maxPendingRequests;
    private final long pollInterval;
    private volatile long tickTs = 0L;
    private volatile long tickSize = 0L;
    private volatile boolean stopped = false;

    @Builder
    public DefaultTbQueueRequestTemplate(TbQueueAdmin queueAdmin,
                                         TbQueueProducer<Request> requestTemplate,
                                         TbQueueConsumer<Response> responseTemplate,
                                         long maxRequestTimeout,
                                         long maxPendingRequests,
                                         long pollInterval,
                                         ExecutorService executor) {
        this.queueAdmin = queueAdmin;
        this.requestTemplate = requestTemplate;
        this.responseTemplate = responseTemplate;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.maxRequestTimeout = maxRequestTimeout;
        this.maxPendingRequests = maxPendingRequests;
        this.pollInterval = pollInterval;
        if (executor != null) {
            internalExecutor = false;
            this.executor = executor;
        } else {
            internalExecutor = true;
            this.executor = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    public void init() {
        queueAdmin.createTopicIfNotExists(responseTemplate.getTopic());
        this.requestTemplate.init();
        tickTs = System.currentTimeMillis();
        responseTemplate.subscribe();
        executor.submit(() -> {
            long nextCleanupMs = 0L;
            while (!stopped) {
                try {
                    List<Response> responses = responseTemplate.poll(pollInterval);
                    if (responses.size() > 0) {
                        log.trace("Polling responses completed, consumer records count [{}]", responses.size());
                    } else {
                        continue;
                    }
                    responses.forEach(response -> {
                        byte[] requestIdHeader = response.getHeaders().get(REQUEST_ID_HEADER);
                        UUID requestId;
                        if (requestIdHeader == null) {
                            log.error("[{}] Missing requestId in header and body", response);
                        } else {
                            requestId = bytesToUuid(requestIdHeader);
                            log.trace("[{}] Response received: {}", requestId, response);
                            ResponseMetaData<Response> expectedResponse = pendingRequests.remove(requestId);
                            if (expectedResponse == null) {
                                log.trace("[{}] Invalid or stale request", requestId);
                            } else {
                                expectedResponse.future.set(response);
                            }
                        }
                    });
                    responseTemplate.commit();
                    tickTs = System.currentTimeMillis();
                    tickSize = pendingRequests.size();
                    if (nextCleanupMs < tickTs) {
                        //cleanup;
                        pendingRequests.forEach((key, value) -> {
                            if (value.expTime < tickTs) {
                                ResponseMetaData<Response> staleRequest = pendingRequests.remove(key);
                                if (staleRequest != null) {
                                    log.trace("[{}] Request timeout detected, expTime [{}], tickTs [{}]", key, staleRequest.expTime, tickTs);
                                    staleRequest.future.setException(new TimeoutException());
                                }
                            }
                        });
                        nextCleanupMs = tickTs + maxRequestTimeout;
                    }
                } catch (Throwable e) {
                    log.warn("Failed to obtain responses from queue.", e);
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new responses", e2);
                    }
                }
            }
        });
    }

    @Override
    public void stop() {
        stopped = true;

        if (responseTemplate != null) {
            responseTemplate.unsubscribe();
        }

        if (requestTemplate != null) {
            requestTemplate.stop();
        }

        if (internalExecutor) {
            executor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<Response> send(Request request) {
        if (tickSize > maxPendingRequests) {
            return Futures.immediateFailedFuture(new RuntimeException("Pending request map is full!"));
        }
        UUID requestId = UUID.randomUUID();
        request.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
        request.getHeaders().put(RESPONSE_TOPIC_HEADER, stringToBytes(responseTemplate.getTopic()));
        request.getHeaders().put(REQUEST_TIME, longToBytes(System.currentTimeMillis()));
        SettableFuture<Response> future = SettableFuture.create();
        ResponseMetaData<Response> responseMetaData = new ResponseMetaData<>(tickTs + maxRequestTimeout, future);
        pendingRequests.putIfAbsent(requestId, responseMetaData);
        log.trace("[{}] Sending request, key [{}], expTime [{}]", requestId, request.getKey(), responseMetaData.expTime);
        requestTemplate.send(TopicPartitionInfo.builder().topic(requestTemplate.getDefaultTopic()).build(), request, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.trace("[{}] Request sent: {}", requestId, metadata);
            }

            @Override
            public void onFailure(Throwable t) {
                pendingRequests.remove(requestId);
                future.setException(t);
            }
        });
        return future;
    }

    private static class ResponseMetaData<T> {
        private final long expTime;
        private final SettableFuture<T> future;

        ResponseMetaData(long ts, SettableFuture<T> future) {
            this.expTime = ts;
            this.future = future;
        }
    }

}
