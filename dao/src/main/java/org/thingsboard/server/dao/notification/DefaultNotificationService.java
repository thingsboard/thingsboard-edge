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
package org.thingsboard.server.dao.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationSeverity;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;
import org.thingsboard.server.exception.DataValidationException;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final NotificationRequestDao notificationRequestDao;
    private final NotificationDao notificationDao;

    private final NotificationRequestValidator notificationRequestValidator = new NotificationRequestValidator();

    @Override
    public NotificationRequest createNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        if (StringUtils.isBlank(notificationRequest.getNotificationReason())) {
            notificationRequest.setNotificationReason(NotificationRequest.GENERAL_NOTIFICATION_REASON);
        }
        if (notificationRequest.getNotificationSeverity() == null) {
            notificationRequest.setNotificationSeverity(NotificationSeverity.NORMAL);
        }
        notificationRequestValidator.validate(notificationRequest, NotificationRequest::getTenantId);
        return notificationRequestDao.save(tenantId, notificationRequest);
    }

    @Override
    public NotificationRequest findNotificationRequestById(TenantId tenantId, NotificationRequestId id) {
        return notificationRequestDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<NotificationRequest> findNotificationRequestsByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink) {
        return notificationRequestDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    // ON DELETE CASCADE is used: notifications for request are deleted as well
    @Override
    public void deleteNotificationRequest(TenantId tenantId, NotificationRequestId id) {
        notificationRequestDao.removeById(tenantId, id.getId());
    }

    @Override
    public Notification createNotification(TenantId tenantId, Notification notification) {
        if (notification.getId() != null) {
            throw new DataValidationException("Notification cannot be updated"); // tmp ?
        }
        return notificationDao.save(tenantId, notification);
    }

    @Transactional
    @Override
    public Notification updateNotificationStatus(TenantId tenantId, NotificationId notificationId, NotificationStatus status) {
        notificationDao.updateStatus(tenantId, notificationId, status);
        return notificationDao.findById(tenantId, notificationId.getId());
    }

    @Override
    public PageData<Notification> findNotificationsByUserIdAndReadStatusAndPageLink(TenantId tenantId, UserId userId, boolean unreadOnly, PageLink pageLink) {
        if (unreadOnly) {
            return notificationDao.findUnreadByUserIdAndPageLink(tenantId, userId, pageLink);
        } else {
            return notificationDao.findByUserIdAndPageLink(tenantId, userId, pageLink);
        }
    }

    @Override
    public PageData<Notification> findLatestUnreadNotificationsByUserId(TenantId tenantId, UserId userId, int limit) {
        SortOrder sortOrder = new SortOrder(EntityKeyMapping.CREATED_TIME, SortOrder.Direction.DESC);
        PageLink pageLink = new PageLink(limit, 0, null, sortOrder);
        return findNotificationsByUserIdAndReadStatusAndPageLink(tenantId, userId, true, pageLink);
    }

    private static class NotificationRequestValidator extends DataValidator<NotificationRequest> {

        @Override
        protected void validateDataImpl(TenantId tenantId, NotificationRequest notificationRequest) {
            if (notificationRequest.getId() != null) {
                throw new DataValidationException("Notification request cannot be changed once created");
            }
            if (notificationRequest.getSenderId() != null) {
                if (notificationRequest.getNotificationReason().equalsIgnoreCase(NotificationRequest.ALARM_NOTIFICATION_REASON)) {
                    throw new DataValidationException("'Alarm' notification reason is for internal usage");
                }
            }
        }

    }

}
