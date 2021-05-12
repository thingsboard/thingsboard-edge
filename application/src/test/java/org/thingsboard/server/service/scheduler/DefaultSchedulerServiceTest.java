package org.thingsboard.server.service.scheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.queue.TbClusterService;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSchedulerServiceTest {

    public static final int AWAIT_TIMEOUT = 10;
    @Mock
    TenantService tenantService;
    @Mock
    TbClusterService clusterService;
    @Mock
    PartitionService partitionService;
    @Mock
    SchedulerEventService schedulerEventService;

    DefaultSchedulerService schedulerService;

    @Before
    public void setUp() throws Exception {
        schedulerService = spy(new DefaultSchedulerService(tenantService, clusterService, partitionService, schedulerEventService));
        schedulerService.init();
    }

    @Test
    public void givenRepartitionEvents_whenManyOnTbApplicationEvent_thenProcessLatestEventOnly() throws InterruptedException {
        final int eventsCount = 6 * 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);

        willDoNothing().given(schedulerService).initStateFromDB(any());

        //lock pool until filling up the event queue
        schedulerService.queueExecutor.submit(() -> awaitLatchSafely(startLatch));

        for (int i = 1; i < eventsCount; i++) {
            //process event
            PartitionChangeEvent eventCore = mock(PartitionChangeEvent.class);
            willReturn(ServiceType.TB_CORE).given(eventCore).getServiceType();
            schedulerService.onTbApplicationEvent(eventCore);

            //skip event
            PartitionChangeEvent eventOther = mock(PartitionChangeEvent.class);
            willReturn(ServiceType.TB_RULE_ENGINE).given(eventOther).getServiceType();
            schedulerService.onTbApplicationEvent(eventOther);
        }

        //add latest to process
        final PartitionChangeEvent latestEvent = mock(PartitionChangeEvent.class);
        willReturn(ServiceType.TB_CORE).given(latestEvent).getServiceType();
        final Set<TopicPartitionInfo> latestTopics = mock(Set.class);
        willReturn(latestTopics).given(latestEvent).getPartitions();
        schedulerService.onTbApplicationEvent(latestEvent);

        assertThat(schedulerService.subscribeQueue.size(), is(eventsCount));

        //submit the last task to trigger finishLatch
        schedulerService.queueExecutor.submit(finishLatch::countDown);

        startLatch.countDown();
        assertThat("Await finish latch timeout", finishLatch.await(AWAIT_TIMEOUT, TimeUnit.SECONDS));

        verify(schedulerService, times(eventsCount * 2 - 1)).onTbApplicationEvent(any());
        verify(schedulerService, times(eventsCount)).getLatestPartitionsFromQueue();
        verify(schedulerService, times(1)).initStateFromDB(latestTopics);
    }

    void awaitLatchSafely(CountDownLatch latch) {
        try {
            assertThat("Await latch timeout", latch.await(AWAIT_TIMEOUT, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            assertThat("Await latch interrupted", false);
        }
    }

    @After
    public void tearDown() throws Exception {
        schedulerService.stop();
        assertThat(schedulerService.queueExecutor.isShutdown(), is(true));
    }
}