/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
