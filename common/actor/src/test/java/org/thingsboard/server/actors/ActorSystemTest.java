/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.actors;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ActorSystemTest {

    public static final String ROOT_DISPATCHER = "root-dispatcher";
    private static final int _100K = 100 * 1024;
    public static final int TIMEOUT_AWAIT_MAX_SEC = 100;

    private volatile TbActorSystem actorSystem;
    private volatile ExecutorService submitPool;
    private ExecutorService executor;
    private int parallelism;

    @Before
    public void initActorSystem() {
        int cores = Runtime.getRuntime().availableProcessors();
        parallelism = Math.max(2, cores / 2);
        TbActorSystemSettings settings = new TbActorSystemSettings(5, parallelism, 42);
        actorSystem = new DefaultTbActorSystem(settings);
        submitPool = Executors.newFixedThreadPool(parallelism, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-submit-test-scope")); //order guaranteed
    }

    @After
    public void shutdownActorSystem() {
        actorSystem.stop();
        submitPool.shutdownNow();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void test1actorsAnd100KMessages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(1, _100K, 1);
    }

    @Test
    public void test10actorsAnd100KMessages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(10, _100K, 1);
    }

    @Test
    public void test100KActorsAnd1Messages5timesSingleThread() throws InterruptedException {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(_100K, 1, 5);
    }

    @Test
    public void test100KActorsAnd1Messages5times() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(_100K, 1, 5);
    }

    @Test
    public void test100KActorsAnd10Messages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(_100K, 10, 1);
    }

    @Test
    public void test1KActorsAnd1KMessages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(1000, 1000, 10);
    }

    @Test
    public void testNoMessagesAfterDestroy() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx1 = getActorTestCtx(1);
        ActorTestCtx testCtx2 = getActorTestCtx(1);

        TbActorRef actorId1 = actorSystem.createRootActor(ROOT_DISPATCHER, new SlowInitActor.SlowInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx1));
        TbActorRef actorId2 = actorSystem.createRootActor(ROOT_DISPATCHER, new SlowInitActor.SlowInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx2));

        actorId1.tell(new IntTbActorMsg(42));
        actorId2.tell(new IntTbActorMsg(42));
        actorSystem.stop(actorId1);

        Assert.assertTrue(testCtx2.getLatch().await(1, TimeUnit.SECONDS));
        Assert.assertFalse(testCtx1.getLatch().await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testOneActorCreated() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx1 = getActorTestCtx(1);
        ActorTestCtx testCtx2 = getActorTestCtx(1);
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        final CountDownLatch initLatch = new CountDownLatch(1);
        final CountDownLatch actorsReadyLatch = new CountDownLatch(2);
        submitPool.submit(() -> {
            actorSystem.createRootActor(ROOT_DISPATCHER, new SlowCreateActor.SlowCreateActorCreator(actorId, testCtx1, initLatch));
            actorsReadyLatch.countDown();
        });
        submitPool.submit(() -> {
            actorSystem.createRootActor(ROOT_DISPATCHER, new SlowCreateActor.SlowCreateActorCreator(actorId, testCtx2, initLatch));
            actorsReadyLatch.countDown();
        });

        initLatch.countDown(); //replacement for Thread.wait(500) in the SlowCreateActorCreator
        Assert.assertTrue(actorsReadyLatch.await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));

        actorSystem.tell(actorId, new IntTbActorMsg(42));

        Assert.assertTrue(testCtx1.getLatch().await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));
        Assert.assertFalse(testCtx2.getLatch().await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testActorCreatorCalledOnce() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx = getActorTestCtx(1);
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        final int actorsCount = 1000;
        final CountDownLatch initLatch = new CountDownLatch(1);
        final CountDownLatch actorsReadyLatch = new CountDownLatch(actorsCount);
        for (int i = 0; i < actorsCount; i++) {
            submitPool.submit(() -> {
                actorSystem.createRootActor(ROOT_DISPATCHER, new SlowCreateActor.SlowCreateActorCreator(actorId, testCtx, initLatch));
                actorsReadyLatch.countDown();
            });
        }
        initLatch.countDown();
        Assert.assertTrue(actorsReadyLatch.await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));

        actorSystem.tell(actorId, new IntTbActorMsg(42));

        Assert.assertTrue(testCtx.getLatch().await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));
        //One for creation and one for message
        Assert.assertEquals(2, testCtx.getInvocationCount().get());
    }

    @Test
    public void testFailedInit() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx1 = getActorTestCtx(1);
        ActorTestCtx testCtx2 = getActorTestCtx(1);

        TbActorRef actorId1 = actorSystem.createRootActor(ROOT_DISPATCHER, new FailedToInitActor.FailedToInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx1, 1, 3000));
        TbActorRef actorId2 = actorSystem.createRootActor(ROOT_DISPATCHER, new FailedToInitActor.FailedToInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx2, 2, 1));

        actorId1.tell(new IntTbActorMsg(42));
        actorId2.tell(new IntTbActorMsg(42));

        Assert.assertFalse(testCtx1.getLatch().await(2, TimeUnit.SECONDS));
        Assert.assertTrue(testCtx2.getLatch().await(1, TimeUnit.SECONDS));
        Assert.assertTrue(testCtx1.getLatch().await(3, TimeUnit.SECONDS));
    }


    public void testActorsAndMessages(int actorsCount, int msgNumber, int times) throws InterruptedException {
        Random random = new Random();
        int[] randomIntegers = new int[msgNumber];
        long sumTmp = 0;
        for (int i = 0; i < msgNumber; i++) {
            int tmp = random.nextInt();
            randomIntegers[i] = tmp;
            sumTmp += tmp;
        }
        long expected = sumTmp;

        List<ActorTestCtx> testCtxes = new ArrayList<>();

        List<TbActorRef> actorRefs = new ArrayList<>();
        for (int actorIdx = 0; actorIdx < actorsCount; actorIdx++) {
            ActorTestCtx testCtx = getActorTestCtx(msgNumber);
            actorRefs.add(actorSystem.createRootActor(ROOT_DISPATCHER, new TestRootActor.TestRootActorCreator(
                    new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx)));
            testCtxes.add(testCtx);
        }

        for (int t = 0; t < times; t++) {
            long start = System.nanoTime();
            for (int i = 0; i < msgNumber; i++) {
                int tmp = randomIntegers[i];
                submitPool.execute(() -> actorRefs.forEach(actorId -> actorId.tell(new IntTbActorMsg(tmp))));
            }
            log.info("Submitted all messages");
            testCtxes.forEach(ctx -> {
                try {
                    boolean success = ctx.getLatch().await(1, TimeUnit.MINUTES);
                    if (!success) {
                        log.warn("Failed: {}, {}", ctx.getActual().get(), ctx.getInvocationCount().get());
                    }
                    Assert.assertTrue(success);
                    Assert.assertEquals(expected, ctx.getActual().get());
                    Assert.assertEquals(msgNumber, ctx.getInvocationCount().get());
                    ctx.clear();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            long duration = System.nanoTime() - start;
            log.info("Time spend: {}ns ({} ms)", duration, TimeUnit.NANOSECONDS.toMillis(duration));
        }
    }

    private ActorTestCtx getActorTestCtx(int i) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicLong actual = new AtomicLong();
        AtomicInteger invocations = new AtomicInteger();
        return new ActorTestCtx(countDownLatch, invocations, i, actual);
    }
}
