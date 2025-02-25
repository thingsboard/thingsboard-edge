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
package org.thingsboard.server.service.subscription;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultTbLocalSubscriptionServiceTest {

    ListAppender<ILoggingEvent> testLogAppender;
    TbLocalSubscriptionService subscriptionService;

    @BeforeEach
    public void setUp() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(DefaultTbLocalSubscriptionService.class);
        testLogAppender = new ListAppender<>();
        testLogAppender.start();
        logger.addAppender(testLogAppender);

        RateLimitService rateLimitService = mock();
        when(rateLimitService.checkRateLimit(eq(LimitedApi.WS_SUBSCRIPTIONS), any(Object.class), nullable(String.class))).thenReturn(true);
        PartitionService partitionService = mock();
        when(partitionService.resolve(any(), any(), any())).thenReturn(TopicPartitionInfo.builder().build());
        subscriptionService = new DefaultTbLocalSubscriptionService(mock(), mock(), mock(), partitionService, mock(), mock(), mock(), rateLimitService);
        ReflectionTestUtils.setField(subscriptionService, "serviceId", "serviceId");
    }

    @AfterEach
    public void tearDown() {
        if (testLogAppender != null) {
            testLogAppender.stop();
            Logger logger = (Logger) LoggerFactory.getLogger(DefaultTbLocalSubscriptionService.class);
            logger.detachAppender(testLogAppender);
        }
    }

    @Test
    public void addSubscriptionConcurrentModificationTest() throws Exception {
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        TenantId tenantId = new TenantId(UUID.randomUUID());
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        WebSocketSessionRef sessionRef = mock();
        ReflectionTestUtils.setField(subscriptionService, "subscriptionUpdateExecutor", executorService);

        List<ListenableFuture<?>> futures = new ArrayList<>();

        try {
            subscriptionService.onCoreStartupMsg(TransportProtos.CoreStartupMsg.newBuilder().addAllPartitions(List.of(0)).getDefaultInstanceForType());
            for (int i = 0; i < 50; i++) {
                futures.add(executorService.submit(() -> subscriptionService.addSubscription(createSubscription(tenantId, deviceId), sessionRef)));
            }
            Futures.allAsList(futures).get();
        } finally {
            executorService.shutdownNow();
        }

        List<ILoggingEvent> logs = testLogAppender.list;
        boolean exceptionLogged = logs.stream()
                .filter(event -> event.getThrowableProxy() != null)
                .map(event -> event.getThrowableProxy().getClassName())
                .anyMatch(log -> log.equals("java.util.ConcurrentModificationException"));

        assertFalse(exceptionLogged, "Detected ConcurrentModificationException!");
    }

    private TbSubscription<?> createSubscription(TenantId tenantId, EntityId entityId) {
        Map<String, Long> keys = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            keys.put(RandomStringUtils.randomAlphanumeric(5), 1L);
        }
        return TbAttributeSubscription.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .subscriptionId(1)
                .sessionId(RandomStringUtils.randomAlphanumeric(5))
                .keyStates(keys)
                .build();
    }
}
