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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.settings.TbQueueIntegrationExecutorSettings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class HashPartitionServiceTest {

    public static final int ITERATIONS = 1000000;
    public static final int SERVER_COUNT = 3;
    private HashPartitionService clusterRoutingService;

    private TbServiceInfoProvider discoveryService;
    private TenantRoutingInfoService routingInfoService;
    private ApplicationEventPublisher applicationEventPublisher;
    private QueueRoutingInfoService queueRoutingInfoService;
    @SpyBean
    private TbQueueIntegrationExecutorSettings integrationExecutorSettings;

    private String hashFunctionName = "murmur3_128";

    @Before
    public void setup() throws Exception {
        discoveryService = mock(TbServiceInfoProvider.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        routingInfoService = mock(TenantRoutingInfoService.class);
        queueRoutingInfoService = mock(QueueRoutingInfoService.class);
        integrationExecutorSettings = spy(TbQueueIntegrationExecutorSettings.class);
        clusterRoutingService = new HashPartitionService(discoveryService,
                routingInfoService,
                applicationEventPublisher,
                queueRoutingInfoService,
                integrationExecutorSettings);

        ReflectionTestUtils.setField(clusterRoutingService, "coreTopic", "tb.core");
        ReflectionTestUtils.setField(clusterRoutingService, "corePartitions", 10);
        ReflectionTestUtils.setField(clusterRoutingService, "vcTopic", "tb.vc");
        ReflectionTestUtils.setField(clusterRoutingService, "vcPartitions", 10);
        ReflectionTestUtils.setField(clusterRoutingService, "integrationPartitions", 3);
        ReflectionTestUtils.setField(clusterRoutingService, "hashFunctionName", hashFunctionName);
        ReflectionTestUtils.setField(integrationExecutorSettings, "downlinkTopic", "tb_ie.downlink");
        ServiceInfo currentServer = ServiceInfo.newBuilder()
                .setServiceId("tb-core-0")
                .addAllServiceTypes(Collections.singletonList(ServiceType.TB_CORE.name()))
                .build();
//        when(queueService.resolve(Mockito.any(), Mockito.anyString())).thenAnswer(i -> i.getArguments()[1]);
//        when(discoveryService.getServiceInfo()).thenReturn(currentServer);
        List<ServiceInfo> otherServers = new ArrayList<>();
        for (int i = 1; i < SERVER_COUNT; i++) {
            otherServers.add(ServiceInfo.newBuilder()
                    .setServiceId("tb-rule-" + i)
                    .addAllServiceTypes(Collections.singletonList(ServiceType.TB_CORE.name()))
                    .build());
        }

        clusterRoutingService.init();
        clusterRoutingService.partitionsInit();
        clusterRoutingService.recalculatePartitions(currentServer, otherServers);
    }

    @Test
    public void testPartitionsCreatedWithCorrectName() {
        clusterRoutingService.getPartitionTopicsMap().forEach((queueKey, s) -> {
            if (queueKey.getType().equals(ServiceType.TB_INTEGRATION_EXECUTOR)) {
                Assert.assertEquals("tb_ie.downlink" + "." + queueKey.getQueueName().toLowerCase(), s);
            }
        });
    }

    @Test
    public void testDispersionOnMillionDevices() {
        List<DeviceId> devices = new ArrayList<>();
        for (int i = 0; i < ITERATIONS; i++) {
            devices.add(new DeviceId(Uuids.timeBased()));
        }
        testDevicesDispersion(devices);
    }

    private void testDevicesDispersion(List<DeviceId> devices) {
        long start = System.currentTimeMillis();
        Map<Integer, Integer> map = new HashMap<>();
        for (DeviceId deviceId : devices) {
            TopicPartitionInfo address = clusterRoutingService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, deviceId);
            Integer partition = address.getPartition().get();
            map.put(partition, map.getOrDefault(partition, 0) + 1);
        }

        checkDispersion(start, map, ITERATIONS, 5.0);
    }

    @SneakyThrows
    @Test
    public void testDispersionOnResolveByPartitionIdx() {
        int serverCount = 5;
        int tenantCount = 1000;
        int queueCount = 3;
        int partitionCount = 3;

        List<ServiceInfo> services = new ArrayList<>();

        for (int i = 0; i < serverCount; i++) {
            services.add(ServiceInfo.newBuilder().setServiceId("RE-" + i).build());
        }

        long start = System.currentTimeMillis();
        Map<String, Integer> map = new HashMap<>();
        services.forEach(s -> map.put(s.getServiceId(), 0));

        Random random = new Random();
        long ts = new SimpleDateFormat("dd-MM-yyyy").parse("06-12-2016").getTime() - TimeUnit.DAYS.toMillis(tenantCount);
        for (int tenantIndex = 0; tenantIndex < tenantCount; tenantIndex++) {
            TenantId tenantId = new TenantId(Uuids.startOf(ts));
            ts += TimeUnit.DAYS.toMillis(1) + random.nextInt(1000);
            for (int queueIndex = 0; queueIndex < queueCount; queueIndex++) {
                QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, "queue" + queueIndex, tenantId);
                for (int partition = 0; partition < partitionCount; partition++) {
                    ServiceInfo serviceInfo = clusterRoutingService.resolveByPartitionIdx(services, queueKey, partition);
                    String serviceId = serviceInfo.getServiceId();
                    map.put(serviceId, map.get(serviceId) + 1);
                }
            }
        }

        checkDispersion(start, map, tenantCount * queueCount * partitionCount, 10.0);
    }

    private <T> void checkDispersion(long start, Map<T, Integer> map, int iterations, double maxDiffPercent) {
        List<Map.Entry<T, Integer>> data = map.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
        long end = System.currentTimeMillis();
        double ideal = ((double) iterations) / map.size();
        double diff = Math.max(data.get(data.size() - 1).getValue() - ideal, ideal - data.get(0).getValue());
        double diffPercent = (diff / ideal) * 100.0;
        System.out.println("Time: " + (end - start) + " Diff: " + diff + "(" + String.format("%f", diffPercent) + "%)");
        for (Map.Entry<T, Integer> entry : data) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        Assert.assertTrue(diffPercent < maxDiffPercent);
    }

    @Test
    public void testPartitionsAssignmentWithDedicatedServers() {
        int isolatedProfilesCount = 5;
        int tenantsCountPerProfile = 100;
        int dedicatedServerSetsCount = 3;
        int serversCountPerSet = 3;
        int profilesPerSet = (int) Math.ceil((double) isolatedProfilesCount / dedicatedServerSetsCount);

        List<TenantProfileId> isolatedTenantProfiles = Stream.generate(() -> new TenantProfileId(UUID.randomUUID()))
                .limit(isolatedProfilesCount).collect(Collectors.toList());
        Map<TenantId, TenantProfileId> tenants = new HashMap<>();
        for (TenantProfileId tenantProfileId : isolatedTenantProfiles) {
            for (int i = 0; i < tenantsCountPerProfile; i++) {
                tenants.put(new TenantId(UUID.randomUUID()), tenantProfileId);
            }
        }

        List<Queue> queues = new ArrayList<>();
        Queue systemQueue = new Queue();
        systemQueue.setTenantId(TenantId.SYS_TENANT_ID);
        systemQueue.setName("Main");
        systemQueue.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        systemQueue.setPartitions(10);
        systemQueue.setId(new QueueId(UUID.randomUUID()));
        queues.add(systemQueue);
        tenants.forEach((tenantId, profileId) -> {
            Queue isolatedQueue = new Queue();
            isolatedQueue.setTenantId(tenantId);
            isolatedQueue.setName("Main");
            isolatedQueue.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
            isolatedQueue.setPartitions(2);
            isolatedQueue.setId(new QueueId(UUID.randomUUID()));
            queues.add(isolatedQueue);
            when(routingInfoService.getRoutingInfo(eq(tenantId))).thenReturn(new TenantRoutingInfo(tenantId, profileId, true));
        });
        when(queueRoutingInfoService.getAllQueuesRoutingInfo()).thenReturn(queues.stream()
                .map(QueueRoutingInfo::new).collect(Collectors.toList()));

        List<ServiceInfo> ruleEngines = new ArrayList<>();
        Map<TenantProfileId, List<ServiceInfo>> dedicatedServers = new HashMap<>();
        int serviceId = 0;
        for (int i = 0; i < serversCountPerSet; i++) {
            ServiceInfo commonServer = ServiceInfo.newBuilder()
                    .setServiceId("tb-rule-engine-" + serviceId)
                    .addAllServiceTypes(List.of(ServiceType.TB_RULE_ENGINE.name()))
                    .build();
            ruleEngines.add(commonServer);
            serviceId++;
        }
        for (int i = 0; i < dedicatedServerSetsCount; i++) {
            List<TenantProfileId> assignedProfiles = ListUtils.partition(isolatedTenantProfiles, profilesPerSet).get(i);
            for (int j = 0; j < serversCountPerSet; j++) {
                ServiceInfo dedicatedServer = ServiceInfo.newBuilder()
                        .setServiceId("tb-rule-engine-" + serviceId)
                        .addAllServiceTypes(List.of(ServiceType.TB_RULE_ENGINE.name()))
                        .addAllAssignedTenantProfiles(assignedProfiles.stream().map(UUIDBased::toString).collect(Collectors.toList()))
                        .build();
                ruleEngines.add(dedicatedServer);
                serviceId++;

                for (TenantProfileId assignedProfileId : assignedProfiles) {
                    dedicatedServers.computeIfAbsent(assignedProfileId, p -> new ArrayList<>()).add(dedicatedServer);
                }
            }
        }

        Map<QueueKey, Map<ServiceInfo, List<Integer>>> serversPartitions = new HashMap<>();
        clusterRoutingService.init();
        for (ServiceInfo ruleEngine : ruleEngines) {
            List<ServiceInfo> other = new ArrayList<>(ruleEngines);
            other.removeIf(serviceInfo -> serviceInfo.getServiceId().equals(ruleEngine.getServiceId()));

            clusterRoutingService.recalculatePartitions(ruleEngine, other);
            clusterRoutingService.myPartitions.forEach((queueKey, partitions) -> {
                serversPartitions.computeIfAbsent(queueKey, k -> new HashMap<>()).put(ruleEngine, partitions);
            });
        }
        assertThat(serversPartitions.keySet()).containsAll(queues.stream().map(queue -> new QueueKey(ServiceType.TB_RULE_ENGINE, queue)).collect(Collectors.toList()));

        serversPartitions.forEach((queueKey, partitionsPerServer) -> {
            if (queueKey.getTenantId().isSysTenantId()) {
                partitionsPerServer.forEach((server, partitions) -> {
                    assertThat(server.getAssignedTenantProfilesCount()).as("system queues are not assigned to dedicated servers").isZero();
                });
            } else {
                List<ServiceInfo> responsibleServers = dedicatedServers.get(tenants.get(queueKey.getTenantId()));
                partitionsPerServer.forEach((server, partitions) -> {
                    assertThat(server.getAssignedTenantProfilesCount()).as("isolated queues are only assigned to dedicated servers").isPositive();
                    assertThat(responsibleServers).contains(server);
                });
            }

            List<Integer> allPartitions = partitionsPerServer.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            assertThat(allPartitions).doesNotHaveDuplicates();
        });
    }

    @Test
    public void testIsManagedByCurrentServiceCheck() {
        TenantProfileId isolatedProfileId = new TenantProfileId(UUID.randomUUID());
        when(discoveryService.getAssignedTenantProfiles()).thenReturn(Set.of(isolatedProfileId.getId())); // dedicated server
        TenantProfileId regularProfileId = new TenantProfileId(UUID.randomUUID());

        TenantId isolatedTenantId = new TenantId(UUID.randomUUID());
        when(routingInfoService.getRoutingInfo(eq(isolatedTenantId))).thenReturn(new TenantRoutingInfo(isolatedTenantId, isolatedProfileId, true));
        TenantId regularTenantId = new TenantId(UUID.randomUUID());
        when(routingInfoService.getRoutingInfo(eq(regularTenantId))).thenReturn(new TenantRoutingInfo(regularTenantId, regularProfileId, false));

        assertThat(clusterRoutingService.isManagedByCurrentService(isolatedTenantId)).isTrue();
        assertThat(clusterRoutingService.isManagedByCurrentService(regularTenantId)).isFalse();


        when(discoveryService.getAssignedTenantProfiles()).thenReturn(Collections.emptySet()); // common server

        assertThat(clusterRoutingService.isManagedByCurrentService(isolatedTenantId)).isTrue();
        assertThat(clusterRoutingService.isManagedByCurrentService(regularTenantId)).isTrue();
    }

}
