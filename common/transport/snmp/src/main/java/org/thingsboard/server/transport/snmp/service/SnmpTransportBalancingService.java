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
package org.thingsboard.server.transport.snmp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.event.SnmpTransportListChangedEvent;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@TbSnmpTransportComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class SnmpTransportBalancingService {
    private final PartitionService partitionService;
    private final ApplicationEventPublisher eventPublisher;
    private final SnmpTransportService snmpTransportService;

    private int snmpTransportsCount = 1;
    private Integer currentTransportPartitionIndex = 0;

    public void onServiceListChanged(ServiceListChangedEvent event) {
        log.trace("Got service list changed event: {}", event);
        recalculatePartitions(event.getOtherServices(), event.getCurrentService());
    }

    public boolean isManagedByCurrentTransport(UUID entityId) {
        boolean isManaged = resolvePartitionIndexForEntity(entityId) == currentTransportPartitionIndex;
        if (!isManaged) {
            log.trace("Entity {} is not managed by current SNMP transport node", entityId);
        }
        return isManaged;
    }

    private int resolvePartitionIndexForEntity(UUID entityId) {
        return partitionService.resolvePartitionIndex(entityId, snmpTransportsCount);
    }

    private void recalculatePartitions(List<ServiceInfo> otherServices, ServiceInfo currentService) {
        log.info("Recalculating partitions for SNMP transports");
        List<ServiceInfo> snmpTransports = Stream.concat(otherServices.stream(), Stream.of(currentService))
                .filter(service -> service.getTransportsList().contains(snmpTransportService.getName()))
                .sorted(Comparator.comparing(ServiceInfo::getServiceId))
                .collect(Collectors.toList());
        log.trace("Found SNMP transports: {}", snmpTransports);

        int previousCurrentTransportPartitionIndex = currentTransportPartitionIndex;
        int previousSnmpTransportsCount = snmpTransportsCount;

        if (!snmpTransports.isEmpty()) {
            for (int i = 0; i < snmpTransports.size(); i++) {
                if (snmpTransports.get(i).equals(currentService)) {
                    currentTransportPartitionIndex = i;
                    break;
                }
            }
            snmpTransportsCount = snmpTransports.size();
        }

        if (snmpTransportsCount != previousSnmpTransportsCount || currentTransportPartitionIndex != previousCurrentTransportPartitionIndex) {
            log.info("SNMP transports partitions have changed: transports count = {}, current transport partition index = {}", snmpTransportsCount, currentTransportPartitionIndex);
            eventPublisher.publishEvent(new SnmpTransportListChangedEvent());
        } else {
            log.info("SNMP transports partitions have not changed");
        }
    }

}
