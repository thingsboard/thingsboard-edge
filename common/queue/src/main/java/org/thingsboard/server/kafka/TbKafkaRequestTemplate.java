/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.kafka;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 25.09.18.
 */
@Slf4j
public class TbKafkaRequestTemplate<Request, Response> extends AbstractTbKafkaTemplate {

    private final TBKafkaProducerTemplate<Request> requestTemplate;
    private final TBKafkaConsumerTemplate<Response> responseTemplate;
    private final ConcurrentMap<UUID, ResponseMetaData<Response>> pendingRequests;
    private final boolean internalExecutor;
    private final ExecutorService executor;
    private final long maxRequestTimeout;
    private final long maxPendingRequests;
    private final long pollInterval;
    private volatile long tickTs = 0L;
    private volatile long tickSize = 0L;
    private volatile boolean stopped = false;

    @Builder
    public TbKafkaRequestTemplate(TBKafkaProducerTemplate<Request> requestTemplate,
                                  TBKafkaConsumerTemplate<Response> responseTemplate,
                                  long maxRequestTimeout,
                                  long maxPendingRequests,
                                  long pollInterval,
                                  ExecutorService executor) {
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

    public void init() {
        try {
            TBKafkaAdmin admin = new TBKafkaAdmin(this.requestTemplate.getSettings());
            CreateTopicsResult result = admin.createTopic(new NewTopic(responseTemplate.getTopic(), 1, (short) 1));
            result.all().get();
        } catch (Exception e) {
            if ((e instanceof TopicExistsException) || (e.getCause() != null && e.getCause() instanceof TopicExistsException)) {
                log.trace("[{}] Topic already exists. ", responseTemplate.getTopic());
            } else {
                log.info("[{}] Failed to create topic: {}", responseTemplate.getTopic(), e.getMessage(), e);
                throw new RuntimeException(e);
            }

        }
        this.requestTemplate.init();
        tickTs = System.currentTimeMillis();
        responseTemplate.subscribe();
        executor.submit(() -> {
            long nextCleanupMs = 0L;
            while (!stopped) {
                try {
                    ConsumerRecords<String, byte[]> responses = responseTemplate.poll(Duration.ofMillis(pollInterval));
                    if (responses.count() > 0) {
                        log.trace("Polling responses completed, consumer records count [{}]", responses.count());
                    }
                    responses.forEach(response -> {
                        log.trace("Received response to Kafka Template request: {}", response);
                        Header requestIdHeader = response.headers().lastHeader(TbKafkaSettings.REQUEST_ID_HEADER);
                        Response decodedResponse = null;
                        UUID requestId = null;
                        if (requestIdHeader == null) {
                            try {
                                decodedResponse = responseTemplate.decode(response);
                                requestId = responseTemplate.extractRequestId(decodedResponse);
                            } catch (IOException e) {
                                log.error("Failed to decode response", e);
                            }
                        } else {
                            requestId = bytesToUuid(requestIdHeader.value());
                        }
                        if (requestId == null) {
                            log.error("[{}] Missing requestId in header and body", response);
                        } else {
                            log.trace("[{}] Response received", requestId);
                            ResponseMetaData<Response> expectedResponse = pendingRequests.remove(requestId);
                            if (expectedResponse == null) {
                                log.trace("[{}] Invalid or stale request", requestId);
                            } else {
                                try {
                                    if (decodedResponse == null) {
                                        decodedResponse = responseTemplate.decode(response);
                                    }
                                    expectedResponse.future.set(decodedResponse);
                                } catch (IOException e) {
                                    expectedResponse.future.setException(e);
                                }
                            }
                        }
                    });
                    tickTs = System.currentTimeMillis();
                    tickSize = pendingRequests.size();
                    if (nextCleanupMs < tickTs) {
                        //cleanup;
                        pendingRequests.entrySet().forEach(kv -> {
                            if (kv.getValue().expTime < tickTs) {
                                ResponseMetaData<Response> staleRequest = pendingRequests.remove(kv.getKey());
                                if (staleRequest != null) {
                                    log.trace("[{}] Request timeout detected, expTime [{}], tickTs [{}]", kv.getKey(), staleRequest.expTime, tickTs);
                                    staleRequest.future.setException(new TimeoutException());
                                }
                            }
                        });
                        nextCleanupMs = tickTs + maxRequestTimeout;
                    }
                } catch (InterruptException ie) {
                    if (!stopped) {
                        log.warn("Fetching data from kafka was interrupted.", ie);
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

    public void stop() {
        stopped = true;
        if (internalExecutor) {
            executor.shutdownNow();
        }
    }

    public ListenableFuture<Response> post(String key, Request request) {
        if (tickSize > maxPendingRequests) {
            return Futures.immediateFailedFuture(new RuntimeException("Pending request map is full!"));
        }
        UUID requestId = UUID.randomUUID();
        List<Header> headers = new ArrayList<>(2);
        headers.add(new RecordHeader(TbKafkaSettings.REQUEST_ID_HEADER, uuidToBytes(requestId)));
        headers.add(new RecordHeader(TbKafkaSettings.RESPONSE_TOPIC_HEADER, stringToBytes(responseTemplate.getTopic())));
        SettableFuture<Response> future = SettableFuture.create();
        ResponseMetaData<Response> responseMetaData = new ResponseMetaData<>(tickTs + maxRequestTimeout, future);
        pendingRequests.putIfAbsent(requestId, responseMetaData);
        request = requestTemplate.enrich(request, responseTemplate.getTopic(), requestId);
        log.trace("[{}] Sending request, key [{}], expTime [{}]", requestId, key, responseMetaData.expTime);
        requestTemplate.send(key, request, headers, (metadata, exception) -> {
            if (exception != null) {
                log.trace("[{}] Failed to post the request", requestId, exception);
            } else {
                log.trace("[{}] Posted the request", requestId, metadata);
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
