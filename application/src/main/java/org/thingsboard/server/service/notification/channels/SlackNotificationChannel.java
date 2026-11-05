// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.notification.channels;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.template.SlackDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.service.notification.EdgeNotificationRequest;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

import java.util.List;

/**
 * On the Edge the Slack channel is a thin client. The Slack bot token lives only on the Cloud (in the
 * {@code notifications} admin settings that no longer sync to the Edge), so the Edge packages the send into
 * an {@link EdgeNotificationRequest} and enqueues a SEND_NOTIFICATION cloud event; the Cloud resolves the
 * token and posts to Slack. Report attachments are still gathered on the Edge (report data is Edge-local)
 * and shipped with the request.
 */
@Component
@RequiredArgsConstructor
public class SlackNotificationChannel implements NotificationChannel<SlackConversation, SlackDeliveryMethodNotificationTemplate> {

    private final ReportService reportService;
    private final CloudEventService cloudEventService;

    @Override
    public void sendNotification(SlackConversation conversation, SlackDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        List<EdgeNotificationRequest.SlackFileData> files = null;
        NotificationRequestConfig requestConfig = ctx.getRequest().getAdditionalConfig();
        if (requestConfig != null && CollectionsUtil.isNotEmpty(requestConfig.getReports())) {
            files = requestConfig.getReports().stream()
                    .map(reportId -> {
                        ReportData reportData = reportService.getReportDataById(ctx.getTenantId(), reportId);
                        return new EdgeNotificationRequest.SlackFileData(reportData.getName(), null, reportData.getData());
                    })
                    .toList();
        }
        EdgeNotificationRequest request = EdgeNotificationRequest.builder()
                .method(EdgeNotificationRequest.NotificationMethod.SEND_SLACK)
                .conversationId(conversation.getId())
                .message(processedTemplate.getBody())
                .files(files)
                .build();

        cloudEventService.saveCloudEvent(ctx.getTenantId(), CloudEventType.TENANT, EdgeEventActionType.SEND_NOTIFICATION,
                ctx.getTenantId(), JacksonUtil.valueToTree(request), null);
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
