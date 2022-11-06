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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineNotificationService;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.telemetry.AbstractSubscriptionService;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DefaultNotificationSubscriptionService extends AbstractSubscriptionService implements NotificationSubscriptionService, RuleEngineNotificationService {

    private final NotificationTargetService notificationTargetService;
    private final NotificationService notificationService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final NotificationsTopicService notificationsTopicService;

    public DefaultNotificationSubscriptionService(TbClusterService clusterService, PartitionService partitionService,
                                                  NotificationTargetService notificationTargetService,
                                                  NotificationService notificationService,
                                                  DbCallbackExecutorService dbCallbackExecutorService,
                                                  NotificationsTopicService notificationsTopicService) {
        super(clusterService, partitionService);
        this.notificationTargetService = notificationTargetService;
        this.notificationService = notificationService;
        this.dbCallbackExecutorService = dbCallbackExecutorService;
        this.notificationsTopicService = notificationsTopicService;
    }

    @Override
    public NotificationRequest processNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        log.info("Processing notification request (tenant id: {}, notification target id: {})", tenantId, notificationRequest.getTargetId());
        notificationRequest.setTenantId(tenantId);
        if (notificationRequest.getAdditionalConfig() != null) {
            NotificationRequestConfig config = notificationRequest.getAdditionalConfig();
            if (config.getSendingDelayInMinutes() > 0) {
                notificationRequest.setStatus(NotificationRequestStatus.SCHEDULED);

                // todo: delayed sending; check all delayed notification requests on start up, schedule send
            }
        }
        if (notificationRequest.getStatus() == null) {
            notificationRequest.setStatus(NotificationRequestStatus.PROCESSED);
        }

        NotificationRequest savedNotificationRequest = notificationService.saveNotificationRequest(tenantId, notificationRequest);

        DaoUtil.processBatches(pageLink -> {
            return notificationTargetService.findRecipientsForNotificationTarget(tenantId, notificationRequest.getTargetId(), pageLink);
        }, 100, recipients -> {
            dbCallbackExecutorService.submit(() -> {
                log.debug("Sending notifications for request {} to recipients batch", savedNotificationRequest.getId());
                for (User recipient : recipients) {
                    try {
                        Notification notification = createNotification(recipient, savedNotificationRequest);
                        onNotificationUpdate(recipient.getTenantId(), recipient.getId(), notification, true);
                    } catch (Exception e) {
                        log.error("Failed to create notification for recipient {}", recipient.getId(), e);
                    }
                }
            });
        });

        return savedNotificationRequest;
    }

    @Override
    public void markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        boolean updated = notificationService.markNotificationAsRead(tenantId, recipientId, notificationId);
        if (updated) {
            log.debug("Marking notification {} as read (recipient id: {}, tenant id: {})", notificationId, recipientId, tenantId);
            Notification notification = notificationService.findNotificationById(tenantId, notificationId);
            onNotificationUpdate(tenantId, recipientId, notification, false);
        }
    }

    @Override
    public void deleteNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId) {
        log.debug("Deleting notification request {}", notificationRequestId);
        notificationService.deleteNotificationRequestById(tenantId, notificationRequestId);
        onNotificationRequestUpdate(tenantId, NotificationRequestUpdate.builder()
                .notificationRequestId(notificationRequestId)
                .deleted(true)
                .build());
    }

    @Override
    public void updateNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        log.debug("Updating notification request {}", notificationRequest.getId());
        notificationService.saveNotificationRequest(tenantId, notificationRequest);
        notificationService.updateNotificationsInfosByRequestId(tenantId, notificationRequest.getId(), notificationRequest.getNotificationInfo());
        onNotificationRequestUpdate(tenantId, NotificationRequestUpdate.builder()
                .notificationRequestId(notificationRequest.getId())
                .notificationInfo(notificationRequest.getNotificationInfo())
                .deleted(false)
                .build());
    }

    private Notification createNotification(User recipient, NotificationRequest notificationRequest) {
        log.trace("Creating notification for recipient {} (notification request id: {})", recipient.getId(), notificationRequest.getId());
        Notification notification = Notification.builder()
                .requestId(notificationRequest.getId())
                .recipientId(recipient.getId())
                .reason(notificationRequest.getNotificationReason())
                .text(formatNotificationText(notificationRequest.getTextTemplate(), recipient))
                .info(notificationRequest.getNotificationInfo())
                .severity(notificationRequest.getNotificationSeverity())
                .status(NotificationStatus.SENT)
                .build();
        return notificationService.saveNotification(recipient.getTenantId(), notification);
    }

    private String formatNotificationText(String template, User recipient) {
        Map<String, String> context = Map.of(
                "recipientEmail", recipient.getEmail(),
                "recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()),
                "recipientLastName", Strings.nullToEmpty(recipient.getLastName())
        );
        return TbNodeUtils.processTemplate(template, context);
    }

    private void onNotificationUpdate(TenantId tenantId, UserId recipientId, Notification notification, boolean isNew) {
        NotificationUpdate update = NotificationUpdate.builder()
                .notification(notification)
                .isNew(isNew)
                .build();
        log.trace("Submitting notification update for recipient {}: {}", recipientId, update);
        wsCallBackExecutor.submit(() -> {
            forwardToSubscriptionManagerService(tenantId, recipientId, subscriptionManagerService -> {
                subscriptionManagerService.onNotificationUpdate(tenantId, recipientId, update, TbCallback.EMPTY);
            }, () -> {
                return TbSubscriptionUtils.notificationUpdateToProto(tenantId, recipientId, update);
            });
        });
    }

    private void onNotificationRequestUpdate(TenantId tenantId, NotificationRequestUpdate update) {
        log.trace("Submitting notification request update: {}", update);
        wsCallBackExecutor.submit(() -> {
            TransportProtos.ToCoreMsg notificationRequestDeletedProto = TbSubscriptionUtils.notificationRequestUpdateToProto(tenantId, update);
            Set<String> coreServices = new HashSet<>(partitionService.getAllServiceIds(ServiceType.TB_CORE));
            for (String serviceId : coreServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                clusterService.pushMsgToCore(tpi, UUID.randomUUID(), notificationRequestDeletedProto, null);
            }
        });
    }

    @Override
    protected String getExecutorPrefix() {
        return "notification";
    }

}
