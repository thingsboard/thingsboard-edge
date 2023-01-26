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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TbMsgPackProcessingContextTest {

    public static final int TIMEOUT = 10;
    ExecutorService executorService;

    @After
    public void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void testHighConcurrencyCase() throws InterruptedException {
        //log.warn("preparing the test...");
        int msgCount = 1000;
        int parallelCount = 5;
        executorService = Executors.newFixedThreadPool(parallelCount, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope"));

        ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> messages = new ConcurrentHashMap<>(msgCount);
        for (int i = 0; i < msgCount; i++) {
            messages.put(UUID.randomUUID(), new TbProtoQueueMsg<>(UUID.randomUUID(), null));
        }
        TbRuleEngineSubmitStrategy strategyMock = mock(TbRuleEngineSubmitStrategy.class);
        when(strategyMock.getPendingMap()).thenReturn(messages);

        TbMsgPackProcessingContext context = new TbMsgPackProcessingContext(DataConstants.MAIN_QUEUE_NAME, strategyMock, false);
        for (UUID uuid : messages.keySet()) {
            final CountDownLatch readyLatch = new CountDownLatch(parallelCount);
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch finishLatch = new CountDownLatch(parallelCount);
            for (int i = 0; i < parallelCount; i++) {
                //final String taskName = "" + uuid + " " + i;
                executorService.submit(() -> {
                    //log.warn("ready {}", taskName);
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Assert.fail("failed to await");
                    }
                    //log.warn("go    {}", taskName);

                    context.onSuccess(uuid);

                    finishLatch.countDown();
                });
            }
            assertTrue(readyLatch.await(TIMEOUT, TimeUnit.SECONDS));
            Thread.yield();
            startLatch.countDown(); //run all-at-once submitted tasks
            assertTrue(finishLatch.await(TIMEOUT, TimeUnit.SECONDS));
        }
        assertTrue(context.await(TIMEOUT, TimeUnit.SECONDS));
        verify(strategyMock, times(msgCount)).onSuccess(any(UUID.class));
    }
}
