// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.channels;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.template.SlackDeliveryMethodNotificationTemplate;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.service.notification.EdgeNotificationRequest;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

/**
 * On the Edge the Slack channel is a thin client. The Slack bot token lives only on the Cloud (in the
 * {@code notifications} admin settings that no longer sync to the Edge), so the Edge packages the send into
 * an {@link EdgeNotificationRequest} and enqueues a SEND_NOTIFICATION cloud event; the Cloud resolves the
 * token and posts to Slack.
 */
@Component
@RequiredArgsConstructor
public class SlackNotificationChannel implements NotificationChannel<SlackConversation, SlackDeliveryMethodNotificationTemplate> {

    private final CloudEventService cloudEventService;

    @Override
    public void sendNotification(SlackConversation conversation, SlackDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        EdgeNotificationRequest request = EdgeNotificationRequest.builder()
                .method(EdgeNotificationRequest.NotificationMethod.SEND_SLACK)
                .conversationId(conversation.getId())
                .message(processedTemplate.getBody())
                .build();

        cloudEventService.saveCloudEvent(ctx.getTenantId(), CloudEventType.TENANT, EdgeEventActionType.SEND_NOTIFICATION,
                ctx.getTenantId(), JacksonUtil.valueToTree(request));
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
        // Slack config lives on the Cloud; the Edge delegates the send, so nothing to verify locally.
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.SLACK;
    }

}
