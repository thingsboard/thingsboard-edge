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
package org.thingsboard.server.service.system;

import com.google.protobuf.ProtocolStringList;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TbCoreComponent
@Service
@RequiredArgsConstructor
public class DefaultSystemInfoService implements SystemInfoService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;

    @Value("${zk.enabled:false}")
    private boolean zkEnabled;

    @Override
    @SneakyThrows
    public SystemInfo getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        TransportProtos.ServiceInfo serviceInfo = serviceInfoProvider.getServiceInfo();
        List<TransportProtos.ServiceInfo> currentOtherServices = partitionService.getCurrentOtherServices();

        if (zkEnabled) {
            Map<String, String> serviceInfos = new HashMap<>();
            addServiceInfo(serviceInfos, serviceInfo);
            currentOtherServices.forEach(otherInfo -> addServiceInfo(serviceInfos, otherInfo));
            systemInfo.setServiceInfos(serviceInfos);
        } else {
            systemInfo.setMonolith(true);
            systemInfo.setMemUsage(getMemoryUsage());

            systemInfo.setCpuUsage((int) (getCpuUsage() * 100) / 100.0);
            systemInfo.setFreeDiscSpace(getFreeDiscSpace());
        }

        return systemInfo;
    }

    private void addServiceInfo(Map<String, String> serviceInfos, TransportProtos.ServiceInfo serviceInfo) {
        ProtocolStringList serviceTypes = serviceInfo.getServiceTypesList();
        serviceInfos.put(serviceInfo.getServiceId(), serviceTypes.size() > 1 ? "MONOLITH" : serviceTypes.get(0));
    }

    private long getMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }

    private double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        return osBean.getSystemLoadAverage();
    }

    private long getFreeDiscSpace() {
        File file = new File("/");
        return file.getFreeSpace();
    }
}
