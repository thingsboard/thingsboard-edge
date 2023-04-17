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

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Once application is ready or cluster topology changes, this Service will produce {@link PartitionChangeEvent}
 */
public interface PartitionService {

    TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId);

    TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId);

    boolean isMyPartition(ServiceType serviceType, TenantId tenantId, EntityId entityId);

    /**
     * Received from the Discovery service when network topology is changed.
     * @param currentService - current service information {@link org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo}
     * @param otherServices - all other discovered services {@link org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo}
     */
    void recalculatePartitions(TransportProtos.ServiceInfo currentService, List<TransportProtos.ServiceInfo> otherServices);

    /**
     * Get all active service ids by service type
     * @param serviceType to filter the list of services
     * @return list of all active services
     */
    Set<String> getAllServiceIds(ServiceType serviceType);

    Set<TransportProtos.ServiceInfo> getAllServices(ServiceType serviceType);

    Set<TransportProtos.ServiceInfo> getOtherServices(ServiceType serviceType);

    int resolvePartitionIndex(UUID entityId, int partitions);

    void removeTenant(TenantId tenantId);

    int countTransportsByType(String type);

    int getIntegrationExecutorPartitionsCount();

    void updateQueue(TransportProtos.QueueUpdateMsg queueUpdateMsg);

    void removeQueue(TransportProtos.QueueDeleteMsg queueDeleteMsg);
}
