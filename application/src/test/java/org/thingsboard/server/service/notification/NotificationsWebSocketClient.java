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

import lombok.Getter;
import org.apache.commons.lang3.RandomUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationCmdsWrapper;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class NotificationsWebSocketClient extends TbTestWebSocketClient {

    private final Map<UUID, Notification> currentNotifications = new LinkedHashMap<>();
    @Getter
    private int totalUnreadCount;
    @Getter
    private UnreadNotificationsUpdate lastUpdate;

    public NotificationsWebSocketClient(String wsUrl, String token) throws URISyntaxException {
        super(new URI(wsUrl + "/api/ws/plugins/notifications?token=" + token));
    }

    public void subscribeForUnreadNotifications(int limit) {
        NotificationCmdsWrapper cmdsWrapper = new NotificationCmdsWrapper();
        cmdsWrapper.setUnreadSubCmd(new NotificationsSubCmd(newCmdId(), limit));
        sendCmd(cmdsWrapper);
    }

    public void markNotificationAsRead(UUID notificationId) {
        NotificationCmdsWrapper cmdsWrapper = new NotificationCmdsWrapper();
        cmdsWrapper.setMarkAsReadCmd(new MarkNotificationAsReadCmd(newCmdId(), notificationId));
        sendCmd(cmdsWrapper);
    }


    private void handleUpdate(UnreadNotificationsUpdate update) {
        totalUnreadCount = update.getTotalUnreadCount();
        if (update.getNotifications() != null) {
            currentNotifications.clear();
            currentNotifications.putAll(update.getNotifications().stream().collect(Collectors.toMap(IdBased::getUuidId, n -> n)));
        } else if (update.getUpdate() != null) {
            Notification notification = update.getUpdate();
            currentNotifications.put(notification.getUuidId(), notification);
        }
    }


    public void sendCmd(NotificationCmdsWrapper cmdsWrapper) {
        send(JacksonUtil.toString(cmdsWrapper));
    }

    @Override
    public void onMessage(String s) {
        UnreadNotificationsUpdate update = JacksonUtil.fromString(s, UnreadNotificationsUpdate.class);
        lastUpdate = update;
        handleUpdate(update);
        super.onMessage(s);
    }

    private static int newCmdId() {
        return RandomUtils.nextInt(1, 1000);
    }

}
