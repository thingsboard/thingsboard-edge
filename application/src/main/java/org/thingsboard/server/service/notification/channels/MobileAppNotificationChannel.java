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
package org.thingsboard.server.service.notification.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.notification.FirebaseService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.settings.MobileAppNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.MobileAppDeliveryMethodNotificationTemplate;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.MOBILE_APP;

@Component
@RequiredArgsConstructor
@Slf4j
public class MobileAppNotificationChannel implements NotificationChannel<User, MobileAppDeliveryMethodNotificationTemplate> {

    private final FirebaseService firebaseService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final NotificationSettingsService notificationSettingsService;

    @Override
    public void sendNotification(User recipient, MobileAppDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        NotificationRequest request = ctx.getRequest();
        NotificationInfo info = request.getInfo();
        if (info != null && info.getDashboardId() != null) {
            ObjectNode additionalConfig = JacksonUtil.asObject(processedTemplate.getAdditionalConfig());
            ObjectNode onClick = JacksonUtil.asObject(additionalConfig.get("onClick"));
            if (onClick.get("enabled") == null || !Boolean.parseBoolean(onClick.get("enabled").asText())) {
                onClick.put("enabled", true);
                onClick.put("linkType", "DASHBOARD");
                onClick.put("setEntityIdInState", true);
                onClick.put("dashboardId", info.getDashboardId().toString());
                additionalConfig.set("onClick", onClick);
            }
            processedTemplate.setAdditionalConfig(additionalConfig);
        }
        Notification notification = Notification.builder()
                .requestId(request.getId())
                .recipientId(recipient.getId())
                .type(ctx.getNotificationType())
                .deliveryMethod(MOBILE_APP)
                .subject(processedTemplate.getSubject())
                .text(processedTemplate.getBody())
                .additionalConfig(processedTemplate.getAdditionalConfig())
                .info(info)
                .status(NotificationStatus.SENT)
                .build();
        notificationService.saveNotification(recipient.getTenantId(), notification);

        var mobileSessions = userService.findMobileSessions(recipient.getTenantId(), recipient.getId());
        if (mobileSessions.isEmpty()) {
            throw new IllegalArgumentException("User doesn't use the mobile app");
        }

        MobileAppNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(MOBILE_APP);
        String credentials = config.getFirebaseServiceAccountCredentials();
        Set<String> validTokens = new HashSet<>(mobileSessions.keySet());

        String subject = processedTemplate.getSubject();
        String body = processedTemplate.getBody();
        Map<String, String> data = getNotificationData(processedTemplate, ctx);
        int unreadCount = notificationService.countUnreadNotificationsByRecipientId(ctx.getTenantId(), MOBILE_APP, recipient.getId());
        for (String token : mobileSessions.keySet()) {
            try {
                firebaseService.sendMessage(ctx.getTenantId(), credentials, token, subject, body, data, unreadCount);
            } catch (FirebaseMessagingException e) {
                MessagingErrorCode errorCode = e.getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT || errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
                    validTokens.remove(token);
                    userService.removeMobileSession(recipient.getTenantId(), token);
                    log.debug("[{}][{}] Removed invalid FCM token due to {} {} ({})", recipient.getTenantId(), recipient.getId(), errorCode, e.getMessage(), token);
                    continue;
                }
                throw new RuntimeException("Failed to send message via FCM: " + e.getMessage(), e);
            }
        }
        if (validTokens.isEmpty()) {
            throw new IllegalArgumentException("User doesn't use the mobile app");
        }
    }

    private Map<String, String> getNotificationData(MobileAppDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) {
        Map<String, String> data = Optional.ofNullable(processedTemplate.getAdditionalConfig())
                .filter(JsonNode::isObject).map(JacksonUtil::toFlatMap).orElseGet(HashMap::new);
        NotificationInfo info = ctx.getRequest().getInfo();
        if (info == null) {
            return data;
        }
        Optional.ofNullable(info.getStateEntityId()).ifPresent(stateEntityId -> {
            data.put("stateEntityId", stateEntityId.getId().toString());
            data.put("stateEntityType", stateEntityId.getEntityType().name());
        });
        data.put("notificationType", ctx.getNotificationType().name());
        switch (ctx.getNotificationType()) {
            case ALARM:
            case ALARM_ASSIGNMENT:
            case ALARM_COMMENT:
                info.getTemplateData().forEach((key, value) -> {
                    data.put("info." + key, value);
                });
                break;
        }
        data.replaceAll((key, value) -> Strings.nullToEmpty(value));
        return data;
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(tenantId);
        if (!tenantId.isSysTenantId()) {
            var config = (MobileAppNotificationDeliveryMethodConfig) settings.getDeliveryMethodsConfigs().get(MOBILE_APP);
            if (config == null || config.isUseSystemSettings()) {
                settings = notificationSettingsService.findNotificationSettings(TenantId.SYS_TENANT_ID);
            }
        }
        if (!settings.getDeliveryMethodsConfigs().containsKey(MOBILE_APP)) {
            throw new RuntimeException("Push-notifications to mobile app are not configured");
        }
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return MOBILE_APP;
    }

}
