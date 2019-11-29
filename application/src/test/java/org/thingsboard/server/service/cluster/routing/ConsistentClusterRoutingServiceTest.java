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
package org.thingsboard.server.service.cluster.routing;

import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.cluster.ServerType;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.cluster.discovery.ServerInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ConsistentClusterRoutingServiceTest {

    private ConsistentClusterRoutingService clusterRoutingService;

    private DiscoveryService discoveryService;

    private String hashFunctionName = "murmur3_128";
    private Integer virtualNodesSize = 1024*64;
    private ServerAddress currentServer = new ServerAddress(" 100.96.1.0", 9001, ServerType.CORE);


    @Before
    public void setup() throws Exception {
        discoveryService = mock(DiscoveryService.class);
        clusterRoutingService = new ConsistentClusterRoutingService();
        ReflectionTestUtils.setField(clusterRoutingService, "discoveryService", discoveryService);
        ReflectionTestUtils.setField(clusterRoutingService, "hashFunctionName", hashFunctionName);
        ReflectionTestUtils.setField(clusterRoutingService, "virtualNodesSize", virtualNodesSize);
        when(discoveryService.getCurrentServer()).thenReturn(new ServerInstance(currentServer));
        List<ServerInstance> otherServers = new ArrayList<>();
        for (int i = 1; i < 30; i++) {
            otherServers.add(new ServerInstance(new ServerAddress(" 100.96." + i*2 + "." + i, 9001, ServerType.CORE)));
        }
        when(discoveryService.getOtherServers()).thenReturn(otherServers);
        clusterRoutingService.init();
    }

    @Test
    public void testDispersionOnMillionDevices() {
        List<DeviceId> devices = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            devices.add(new DeviceId(UUIDs.timeBased()));
        }

        testDevicesDispersion(devices);
    }

    @Test
    public void testDispersionOnDevicesFromFile() throws IOException {
        List<String> deviceIdsStrList = Files.readAllLines(Paths.get("/home/ashvayka/Downloads/deviceIds.out"));
        List<DeviceId> devices = deviceIdsStrList.stream().map(String::trim).filter(s -> !s.isEmpty()).map(UUIDConverter::fromString).map(DeviceId::new).collect(Collectors.toList());
        System.out.println("Devices: " + devices.size());
        testDevicesDispersion(devices);
        testDevicesDispersion(devices);
        testDevicesDispersion(devices);
        testDevicesDispersion(devices);
        testDevicesDispersion(devices);

    }

    private void testDevicesDispersion(List<DeviceId> devices) {
        long start = System.currentTimeMillis();
        Map<ServerAddress, Integer> map = new HashMap<>();
        for (DeviceId deviceId : devices) {
            ServerAddress address = clusterRoutingService.resolveById(deviceId).orElse(currentServer);
            map.put(address, map.getOrDefault(address, 0) + 1);
        }

        List<Map.Entry<ServerAddress, Integer>> data = map.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
        long end = System.currentTimeMillis();
        System.out.println("Size: " + virtualNodesSize + " Time: " + (end - start) + " Diff: " + (data.get(data.size() - 1).getValue() - data.get(0).getValue()));

        for (Map.Entry<ServerAddress, Integer> entry : data) {
//            System.out.println(entry.getKey().getHost() + ": " + entry.getValue());
        }

    }

}
