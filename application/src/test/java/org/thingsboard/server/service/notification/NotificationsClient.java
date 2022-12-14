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
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.notification.AlarmOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationOriginatorType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class NotificationsClient extends NotificationApiWsClient {

    private NotificationsClient(String wsUrl, String token) throws Exception {
        super(wsUrl, token);
    }

    public static NotificationsClient newInstance(String username, String password) throws Exception {
        RestClient restClient = new RestClient("http://localhost:8080");
        restClient.login(username, password);
        NotificationsClient client = new NotificationsClient("ws://localhost:8080", restClient.getToken());
        client.connectBlocking();
        return client;
    }

    @Override
    public void onMessage(String s) {
        super.onMessage(s);
//        printNotificationsCount();
        printNotifications();
    }

    public void printNotifications() {
        System.out.println(StringUtils.repeat(System.lineSeparator(), 20));
        List<Notification> notifications = getNotifications();
        System.out.printf("   %s NEW MESSAGE%s\n\n", getUnreadCount(), notifications.size() > 1 ? "S" : "");
        notifications.forEach(notification -> {
            String notificationInfoStr = "";
            if (notification.getOriginatorType() == NotificationOriginatorType.ALARM) {
                AlarmOriginatedNotificationInfo info = (AlarmOriginatedNotificationInfo) notification.getInfo();
                notificationInfoStr = String.format("Alarm of type %s - %s severity - status: %s",
                        info.getAlarmType(), info.getAlarmSeverity(), info.getAlarmStatus());
            } else if (notification.getInfo() != null) {
                notificationInfoStr = Strings.nullToEmpty(notification.getInfo().getDescription());
            }
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String time = format.format(new Date(notification.getCreatedTime()));
//            System.out.printf("[%s] %-19s | %-30s | (%s)\n", time, notification.getReason(), notification.getText(), notificationInfoStr);
        });
        System.out.println(StringUtils.repeat(System.lineSeparator(), 5));
    }

    public void printNotificationsCount() {
        System.out.println();
        System.out.println();
        System.out.println();
        int unreadCount = getUnreadCount();
        System.out.printf("\r\r%s NEW MESSAGE%s", unreadCount, unreadCount > 1 ? "S" : "");
    }

    public static void main(String[] args) throws Exception {
        NotificationsClient client = NotificationsClient.newInstance("tenant@thingsboard.org", "tenant");
        client.subscribeForUnreadNotifications(5);
//        client.subscribeForUnreadNotificationsCount();
        new Scanner(System.in).nextLine();
    }
}
