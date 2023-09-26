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
package org.thingsboard.server.queue.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_ADDED;
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ZkDiscoveryServiceTest {

    @Mock
    private TbServiceInfoProvider serviceInfoProvider;

    @Mock
    private PartitionService partitionService;

    @Mock
    private CuratorFramework client;

    @Mock
    private PathChildrenCache cache;

    @Mock
    private CuratorFramework curatorFramework;

    private ZkDiscoveryService zkDiscoveryService;

    private static final long RECALCULATE_DELAY = 100L;

    final TransportProtos.ServiceInfo currentInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-rule-engine-0").build();
    final ChildData currentData = new ChildData("/thingsboard/nodes/0000000010", null, currentInfo.toByteArray());
    final TransportProtos.ServiceInfo childInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-rule-engine-1").build();
    final ChildData childData = new ChildData("/thingsboard/nodes/0000000020", null, childInfo.toByteArray());

    @Before
    public void setup() {
        zkDiscoveryService = Mockito.spy(new ZkDiscoveryService(serviceInfoProvider, partitionService));
        ScheduledExecutorService zkExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("zk-discovery"));
        when(client.getState()).thenReturn(CuratorFrameworkState.STARTED);
        ReflectionTestUtils.setField(zkDiscoveryService, "stopped", false);
        ReflectionTestUtils.setField(zkDiscoveryService, "client", client);
        ReflectionTestUtils.setField(zkDiscoveryService, "cache", cache);
        ReflectionTestUtils.setField(zkDiscoveryService, "nodePath", "/thingsboard/nodes/0000000010");
        ReflectionTestUtils.setField(zkDiscoveryService, "zkExecutorService", zkExecutorService);
        ReflectionTestUtils.setField(zkDiscoveryService, "recalculateDelay", RECALCULATE_DELAY);
        ReflectionTestUtils.setField(zkDiscoveryService, "zkDir", "/thingsboard");

        when(serviceInfoProvider.getServiceInfo()).thenReturn(currentInfo);

        List<ChildData> dataList = new ArrayList<>();
        dataList.add(currentData);
        when(cache.getCurrentData()).thenReturn(dataList);
    }

    @Test
    public void restartNodeInTimeTest() throws Exception {
        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        verify(partitionService, never()).recalculatePartitions(any(), any());

        startNode(childData);

        verify(partitionService, never()).recalculatePartitions(any(), any());

        Thread.sleep(RECALCULATE_DELAY * 2);

        verify(partitionService, never()).recalculatePartitions(any(), any());

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());
    }

    @Test
    public void restartNodeNotInTimeTest() throws Exception {
        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        Thread.sleep(RECALCULATE_DELAY * 2);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(Collections.emptyList()));

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);
    }

    @Test
    public void startAnotherNodeDuringRestartTest() throws Exception {
        var anotherInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-transport").build();
        var anotherData = new ChildData("/thingsboard/nodes/0000000030", null, anotherInfo.toByteArray());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        startNode(anotherData);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(anotherInfo)));
        reset(partitionService);

        Thread.sleep(RECALCULATE_DELAY * 2);

        verify(partitionService, never()).recalculatePartitions(any(), any());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(anotherInfo, childInfo)));
    }

    private void startNode(ChildData data) throws Exception {
        cache.getCurrentData().add(data);
        zkDiscoveryService.childEvent(curatorFramework, new PathChildrenCacheEvent(CHILD_ADDED, data));
    }

    private void stopNode(ChildData data) throws Exception {
        cache.getCurrentData().remove(data);
        zkDiscoveryService.childEvent(curatorFramework, new PathChildrenCacheEvent(CHILD_REMOVED, data));
    }

}
