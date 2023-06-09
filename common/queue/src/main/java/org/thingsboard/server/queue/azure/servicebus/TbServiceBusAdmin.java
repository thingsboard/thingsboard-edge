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
package org.thingsboard.server.queue.azure.servicebus;

import com.microsoft.azure.servicebus.management.ManagementClient;
import com.microsoft.azure.servicebus.management.QueueDescription;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.MessagingEntityAlreadyExistsException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TbServiceBusAdmin implements TbQueueAdmin {
    private final String MAX_SIZE = "maxSizeInMb";
    private final String MESSAGE_TIME_TO_LIVE = "messageTimeToLiveInSec";
    private final String LOCK_DURATION = "lockDurationInSec";

    private final Map<String, String> queueConfigs;
    private final Set<String> queues = ConcurrentHashMap.newKeySet();

    private final ManagementClient client;

    public TbServiceBusAdmin(TbServiceBusSettings serviceBusSettings, Map<String, String> queueConfigs) {
        this.queueConfigs = queueConfigs;

        ConnectionStringBuilder builder = new ConnectionStringBuilder(
                serviceBusSettings.getNamespaceName(),
                "queues",
                serviceBusSettings.getSasKeyName(),
                serviceBusSettings.getSasKey());

        client = new ManagementClient(builder);

        try {
            client.getQueues().forEach(queueDescription -> queues.add(queueDescription.getPath()));
        } catch (ServiceBusException | InterruptedException e) {
            log.error("Failed to get queues.", e);
            throw new RuntimeException("Failed to get queues.", e);
        }
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        if (queues.contains(topic)) {
            return;
        }

        try {
            QueueDescription queueDescription = new QueueDescription(topic);
            queueDescription.setRequiresDuplicateDetection(false);
            setQueueConfigs(queueDescription);

            client.createQueue(queueDescription);
            queues.add(topic);
        } catch (ServiceBusException | InterruptedException e) {
            if (e instanceof MessagingEntityAlreadyExistsException) {
                queues.add(topic);
                log.info("[{}] queue already exists.", topic);
            } else {
                log.error("Failed to create queue: [{}]", topic, e);
            }
        }
    }

    @Override
    public void deleteTopic(String topic) {
        if (queues.contains(topic)) {
            doDelete(topic);
        } else {
            try {
                if (client.getQueue(topic) != null) {
                    doDelete(topic);
                } else {
                    log.warn("Azure Service Bus Queue [{}] is not exist.", topic);
                }
            } catch (ServiceBusException | InterruptedException e) {
                log.error("Failed to delete Azure Service Bus queue [{}]", topic, e);
            }
        }
    }

    private void doDelete(String topic) {
        try {
            client.deleteTopic(topic);
        } catch (ServiceBusException | InterruptedException e) {
            log.error("Failed to delete Azure Service Bus queue [{}]", topic, e);
        }
    }

    private void setQueueConfigs(QueueDescription queueDescription) {
        queueConfigs.forEach((confKey, confValue) -> {
            switch (confKey) {
                case MAX_SIZE:
                    queueDescription.setMaxSizeInMB(Long.parseLong(confValue));
                    break;
                case MESSAGE_TIME_TO_LIVE:
                    queueDescription.setDefaultMessageTimeToLive(Duration.ofSeconds(Long.parseLong(confValue)));
                    break;
                case LOCK_DURATION:
                    queueDescription.setLockDuration(Duration.ofSeconds(Long.parseLong(confValue)));
                    break;
                default:
                    log.error("Unknown config: [{}]", confKey);
            }
        });
    }

    public void destroy() {
        try {
            client.close();
        } catch (IOException e) {
            log.error("Failed to close ManagementClient.");
        }
    }
}
