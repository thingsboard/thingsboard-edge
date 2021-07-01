/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.scheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.queue.TbClusterService;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Mock
    OtaPackageStateService firmwareStateService;
    @Mock
    DeviceService deviceService;
    @Mock
    DeviceProfileService deviceProfileService;
    @Mock
    EntityGroupService entityGroupService;
    @Mock
    DeviceGroupOtaPackageService deviceGroupOtaPackageService;
    @Mock
    OtaPackageService otaPackageService;

    DefaultSchedulerService schedulerService;

    final Tenant sysTenant = new Tenant(TenantId.SYS_TENANT_ID);
    final TopicPartitionInfo tpiForSysTenant = new TopicPartitionInfo("tb_core", null, 1, true);

    @Before
    public void setUp() throws Exception {
        schedulerService = spy(new DefaultSchedulerService(
                tenantService, clusterService, partitionService, schedulerEventService,
                firmwareStateService, deviceService, deviceProfileService, entityGroupService, deviceGroupOtaPackageService, otaPackageService)
        );
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

    @Test
    public void givenPartitionsFirstEvent_whenInitStateFromDBFirstTime_thenVerifyTenantAdded() {
        //given
        willReturn(singletonList(sysTenant)).given(schedulerService).getAllTenants();

        final Set<TopicPartitionInfo> partitions = unmodifiableSet(new HashSet(asList(
                new TopicPartitionInfo("tb_core", null, 0, true),
                new TopicPartitionInfo("tb_core", null, 1, true),
                new TopicPartitionInfo("tb_core", null, 2, true)
        )));

        willReturn(tpiForSysTenant).given(partitionService).resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);

        assertThat(schedulerService.firstRun, is(true));

        //when
        schedulerService.initStateFromDB(partitions);

        //then
        assertThat(schedulerService.firstRun, is(false));
        assertThat(schedulerService.partitionedTenants.size(), is(partitions.size()));

        partitions.forEach(tpi -> assertThat(schedulerService.partitionedTenants.get(tpi), notNullValue()));
        partitions.stream().filter(tpi -> !tpi.equals(tpiForSysTenant))
                .forEach(tpi -> assertThat(schedulerService.partitionedTenants.get(tpi), is(empty())));

        assertThat(schedulerService.partitionedTenants.get(tpiForSysTenant), notNullValue());
        assertThat(schedulerService.partitionedTenants.get(tpiForSysTenant).size(), is(1)); //fix the issue that have been prevented cleanup on partition delete
        verify(schedulerService, times(1)).addToPartitionedTenants(sysTenant, tpiForSysTenant);
        verify(schedulerService, times(1)).addToPartitionedTenants(any(), any());
    }

    @Test
    public void givenPartitionsSecondEvent_whenInitStateFromDBFirstTime_thenVerifyTenantRemoved() {
        //given first event from previous test
        givenPartitionsFirstEvent_whenInitStateFromDBFirstTime_thenVerifyTenantAdded();
        assertThat(schedulerService.firstRun, is(false));
        verify(schedulerService, never()).removeEvents(any(), any());

        //given
        final Set<TopicPartitionInfo> secondEventPartitions = unmodifiableSet(new HashSet(asList(
                new TopicPartitionInfo("tb_core", null, 0, true),
                //new TopicPartitionInfo("tb_core", null, 1, true), //have to remove tenant
                new TopicPartitionInfo("tb_core", null, 2, true),
                new TopicPartitionInfo("tb_core", null, 4, true),
                new TopicPartitionInfo("tb_core", null, 6, true),
                new TopicPartitionInfo("tb_core", null, 8, true)
        )));

        //when
        schedulerService.initStateFromDB(secondEventPartitions);

        //then
        assertThat(schedulerService.partitionedTenants.size(), is(secondEventPartitions.size()));
        secondEventPartitions.forEach(tpi -> assertThat(schedulerService.partitionedTenants.get(tpi), is(empty())));
        assertThat(schedulerService.partitionedTenants.get(tpiForSysTenant), nullValue());
        verify(schedulerService, times(1)).removeEvents(tpiForSysTenant, sysTenant.getId());
        verify(schedulerService, times(1)).removeEvents(any(), any());
    }

    @After
    public void tearDown() throws Exception {
        schedulerService.stop();
        assertThat(schedulerService.queueExecutor.isShutdown(), is(true));
    }
}