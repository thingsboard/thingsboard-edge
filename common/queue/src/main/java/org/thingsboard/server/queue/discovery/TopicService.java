/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TopicService {

    @Value("${queue.prefix:}")
    private String prefix;

    private final ConcurrentMap<String, TopicPartitionInfo> tbCoreNotificationTopics = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TopicPartitionInfo> tbRuleEngineNotificationTopics = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TopicPartitionInfo> tbEdgeNotificationTopics = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TopicPartitionInfo> tbCalculatedFieldNotificationTopics = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TopicPartitionInfo> tbIntegrationExecutorNotificationTopics = new ConcurrentHashMap<>();
    private final ConcurrentReferenceHashMap<EdgeId, TopicPartitionInfo> tbEdgeEventsNotificationTopics = new ConcurrentReferenceHashMap<>();

    /**
     * Each Service should start a consumer for messages that target individual service instance based on serviceId.
     * This topic is likely to have single partition, and is always assigned to the service.
     * @param serviceType
     * @param serviceId
     * @return
     */
    public TopicPartitionInfo getNotificationsTopic(ServiceType serviceType, String serviceId) {
        return switch (serviceType) {
            case TB_CORE -> tbCoreNotificationTopics.computeIfAbsent(serviceId,
                    id -> buildNotificationsTopicPartitionInfo(serviceType, serviceId));
            case TB_RULE_ENGINE -> tbRuleEngineNotificationTopics.computeIfAbsent(serviceId,
                    id -> buildNotificationsTopicPartitionInfo(serviceType, serviceId));
            case TB_INTEGRATION_EXECUTOR -> tbIntegrationExecutorNotificationTopics.computeIfAbsent(serviceId,
                    id -> buildNotificationsTopicPartitionInfo(serviceType, serviceId));
            default -> buildNotificationsTopicPartitionInfo(serviceType, serviceId);
        };
    }

    private TopicPartitionInfo buildNotificationsTopicPartitionInfo(ServiceType serviceType, String serviceId) {
        return buildNotificationsTopicPartitionInfo(serviceType.name().toLowerCase(), serviceId);
    }

    private TopicPartitionInfo buildNotificationsTopicPartitionInfo(String serviceType, String serviceId) {
        return buildTopicPartitionInfo(serviceType + ".notifications." + serviceId, null, null, false);
    }

    public TopicPartitionInfo getEdgeNotificationsTopic(String serviceId) {
        return tbEdgeNotificationTopics.computeIfAbsent(serviceId, id -> buildEdgeNotificationsTopicPartitionInfo(serviceId));
    }

    private TopicPartitionInfo buildEdgeNotificationsTopicPartitionInfo(String serviceId) {
        return buildTopicPartitionInfo("tb_edge.notifications." + serviceId, null, null, false);
    }

    public TopicPartitionInfo getCalculatedFieldNotificationsTopic(String serviceId) {
        return tbCalculatedFieldNotificationTopics.computeIfAbsent(serviceId,
                id -> buildNotificationsTopicPartitionInfo("calculated_field", serviceId)); // FIXME alandiak - get rid of hardcode
    }

    public TopicPartitionInfo getEdgeEventNotificationsTopic(TenantId tenantId, EdgeId edgeId) {
        return tbEdgeEventsNotificationTopics.computeIfAbsent(edgeId, id -> buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId));
    }

    public TopicPartitionInfo buildEdgeEventNotificationsTopicPartitionInfo(TenantId tenantId, EdgeId edgeId) {
        return buildTopicPartitionInfo("tb_edge_event.notifications." + tenantId + "." + edgeId, null, null, false);
    }

    public TopicPartitionInfo buildTopicPartitionInfo(String topic, TenantId tenantId, Integer partition, boolean myPartition) {
        return new TopicPartitionInfo(buildTopicName(topic), tenantId, partition, myPartition);
    }

    public String buildTopicName(String topic) {
        return prefix.isBlank() ? topic : prefix + "." + topic;
    }

    public String buildConsumerGroupId(String servicePrefix, TenantId tenantId, String queueName, Integer partitionId) {
        return this.buildTopicName(
                servicePrefix + queueName
                + (tenantId.isSysTenantId() ? "" : ("-isolated-" + tenantId))
                + "-consumer"
                + suffix(partitionId));
    }

    String suffix(Integer partitionId) {
        return partitionId == null ? "" : "-" + partitionId;
    }

}
