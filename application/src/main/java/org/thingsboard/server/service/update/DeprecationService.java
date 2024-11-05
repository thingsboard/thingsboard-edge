/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.GeneralNotificationInfo;
import org.thingsboard.server.common.data.notification.targets.platform.SystemAdministratorsFilter;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeprecationService {

    private final NotificationCenter notificationCenter;

    @Value("${queue.type}")
    private String queueType;

    @AfterStartUp(order = Integer.MAX_VALUE)
    public void checkDeprecation() {
        checkQueueTypeDeprecation();
    }

    private void checkQueueTypeDeprecation() {
        String queueTypeName;
        switch (queueType) {
            case "aws-sqs" -> queueTypeName = "AWS SQS";
            case "pubsub" -> queueTypeName = "PubSub";
            case "service-bus" -> queueTypeName = "Azure Service Bus";
            case "rabbitmq" -> queueTypeName = "RabbitMQ";
            default -> {
                return;
            }
        }

        log.warn("WARNING: {} queue type is deprecated and will be removed in ThingsBoard 4.0. Please migrate to Apache Kafka", queueTypeName);
        notificationCenter.sendGeneralWebNotification(TenantId.SYS_TENANT_ID, new SystemAdministratorsFilter(),
                DefaultNotifications.queueTypeDeprecation.toTemplate(), new GeneralNotificationInfo(Map.of(
                        "queueType", queueTypeName
                )));
    }

}
