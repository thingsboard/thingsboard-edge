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
package org.thingsboard.server.service.notification.channels;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.slack.SlackService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.service.notification.NotificationProcessingContext;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.template.SlackDeliveryMethodNotificationTemplate;
import org.thingsboard.server.service.executors.ExternalCallExecutorService;

@Component
@RequiredArgsConstructor
public class SlackNotificationChannel implements NotificationChannel<SlackConversation, SlackDeliveryMethodNotificationTemplate> {

    private final SlackService slackService;
    private final NotificationSettingsService notificationSettingsService;
    private final ExternalCallExecutorService executor;

    @Override
    public ListenableFuture<Void> sendNotification(SlackConversation conversation, SlackDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) {
        SlackNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(NotificationDeliveryMethod.SLACK);
        return executor.submit(() -> {
            slackService.sendMessage(ctx.getTenantId(), config.getBotToken(), conversation.getId(), processedTemplate.getBody());
            return null;
        });
    }

    @Override
    public boolean check(TenantId tenantId) {
        NotificationSettings notificationSettings = notificationSettingsService.findNotificationSettings(tenantId);
        return notificationSettings.getDeliveryMethodsConfigs().containsKey(NotificationDeliveryMethod.SLACK);
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.SLACK;
    }

}
