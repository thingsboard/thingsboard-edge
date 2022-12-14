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
package org.thingsboard.server.service.ws.notification.sub;

import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.service.subscription.TbSubscription;
import org.thingsboard.server.service.subscription.TbSubscriptionType;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Getter
public class NotificationsSubscription extends TbSubscription<NotificationsSubscriptionUpdate> {

    private final Map<UUID, Notification> latestUnreadNotifications = new HashMap<>();
    private final int limit;
    private final AtomicInteger totalUnreadCounter = new AtomicInteger();

    @Builder
    public NotificationsSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                                     BiConsumer<NotificationsSubscription, NotificationsSubscriptionUpdate> updateProcessor,
                                     int limit) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, TbSubscriptionType.NOTIFICATIONS, updateProcessor);
        this.limit = limit;
    }

    public UnreadNotificationsUpdate createFullUpdate() {
        return UnreadNotificationsUpdate.builder()
                .cmdId(getSubscriptionId())
                .notifications(getSortedNotifications())
                .totalUnreadCount(totalUnreadCounter.get())
                .build();
    }

    public List<Notification> getSortedNotifications() {
        return latestUnreadNotifications.values().stream()
                .sorted(Comparator.comparing(BaseData::getCreatedTime, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    public UnreadNotificationsUpdate createPartialUpdate(Notification notification) {
        return UnreadNotificationsUpdate.builder()
                .cmdId(getSubscriptionId())
                .update(notification)
                .totalUnreadCount(totalUnreadCounter.get())
                .build();
    }

    public UnreadNotificationsUpdate createCountUpdate() {
        return UnreadNotificationsUpdate.builder()
                .cmdId(getSubscriptionId())
                .totalUnreadCount(totalUnreadCounter.get())
                .build();
    }

}
