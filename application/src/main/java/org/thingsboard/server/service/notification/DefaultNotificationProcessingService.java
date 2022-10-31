/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.notification;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationProcessingService implements NotificationProcessingService {

    private final NotificationTargetService notificationTargetService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final AccessControlService accessControlService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final NotificationsSubscriptionService notificationsSubscriptionService;

    @Override
    public NotificationRequest processNotificationRequest(SecurityUser user, NotificationRequest notificationRequest) throws ThingsboardException {
        TenantId tenantId = user.getTenantId();
        List<UserId> recipientsIds = notificationTargetService.findRecipientsForNotificationTarget(tenantId, notificationRequest.getTargetId());
        List<User> recipients = new ArrayList<>();
        for (UserId recipientId : recipientsIds) {
            User recipient = userService.findUserById(tenantId, recipientId); // todo: add caching
            accessControlService.checkPermission(user, Resource.USER, Operation.READ, recipientId, recipient);
            recipients.add(recipient);
        }

        notificationRequest.setTenantId(tenantId);
        notificationRequest.setSenderId(user.getId());
        NotificationRequest savedNotificationRequest = notificationService.createNotificationRequest(tenantId, notificationRequest);

        // todo: delayed sending; check all delayed notification requests on start up, schedule send

        for (User recipient : recipients) {
            dbCallbackExecutorService.submit(() -> {
                Notification notification = createNotification(recipient, savedNotificationRequest);
                notificationsSubscriptionService.onNewNotification(recipient.getTenantId(), recipient.getId(), notification);
            });
        }

        return savedNotificationRequest;
    }

    @Override
    public void markNotificationAsRead(SecurityUser user, NotificationId notificationId) {
        Notification notification = notificationService.updateNotificationStatus(user.getTenantId(), notificationId, NotificationStatus.READ);
        notificationsSubscriptionService.onNotificationUpdated(user.getTenantId(), user.getId(), notification);
    }

    @Override
    public void deleteNotificationRequest(SecurityUser user, NotificationRequestId notificationRequestId) {
        notificationService.deleteNotificationRequest(user.getTenantId(), notificationRequestId);
        notificationsSubscriptionService.onNotificationRequestDeleted(user.getTenantId(), notificationRequestId);
    }

    private Notification createNotification(User recipient, NotificationRequest notificationRequest) {
        Notification notification = Notification.builder()
                .requestId(notificationRequest.getId())
                .recipientId(recipient.getId())
                .reason(notificationRequest.getNotificationReason())
                .text(formatNotificationText(notificationRequest.getTextTemplate(), recipient))
                .info(notificationRequest.getNotificationInfo())
                .severity(notificationRequest.getNotificationSeverity())
                .status(NotificationStatus.SENT)
                .build();
        notification = notificationService.createNotification(recipient.getTenantId(), notification);
        return notification;
    }

    private String formatNotificationText(String template, User recipient) {
        Map<String, String> context = Map.of(
                "recipientEmail", recipient.getEmail(),
                "recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()),
                "recipientLastName", Strings.nullToEmpty(recipient.getLastName())
        );
        return TbNodeUtils.processTemplate(template, context);
    }

    // handle markAsRead and deleteNotificationRequest - send UnreadNotificationsUpdate with updated list

}
