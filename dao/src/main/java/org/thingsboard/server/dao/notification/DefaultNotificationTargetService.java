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
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.targets.CustomerUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.SingleUserNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.UserListNotificationTargetConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationTargetService implements NotificationTargetService {

    private final NotificationTargetDao notificationTargetDao;
    private final UserService userService;
    private final NotificationTargetValidator validator = new NotificationTargetValidator();

    @Override
    public NotificationTarget saveNotificationTarget(TenantId tenantId, NotificationTarget notificationTarget) {
        validator.validate(notificationTarget, NotificationTarget::getTenantId);
        return notificationTargetDao.save(tenantId, notificationTarget);
    }

    @Override
    public NotificationTarget findNotificationTargetById(TenantId tenantId, NotificationTargetId id) {
        return notificationTargetDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<NotificationTarget> findNotificationTargetsByTenantId(TenantId tenantId, PageLink pageLink) {
        return notificationTargetDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    @Override
    public PageData<User> findRecipientsForNotificationTarget(TenantId tenantId, NotificationTargetId notificationTargetId, PageLink pageLink) {
        NotificationTarget notificationTarget = findNotificationTargetById(tenantId, notificationTargetId);
        NotificationTargetConfig configuration = notificationTarget.getConfiguration();
        return findRecipientsForNotificationTargetConfig(tenantId, configuration, pageLink);
    }

    @Override
    public PageData<User> findRecipientsForNotificationTargetConfig(TenantId tenantId, NotificationTargetConfig targetConfig, PageLink pageLink) {
        switch (targetConfig.getType()) {
            case SINGLE_USER: {
                UserId userId = new UserId(((SingleUserNotificationTargetConfig) targetConfig).getUserId());
                User user = userService.findUserById(tenantId, userId);
                return new PageData<>(List.of(user), 1, 1, false);
            }
            case USER_LIST: {
                List<User> users = ((UserListNotificationTargetConfig) targetConfig).getUsersIds().stream()
                        .map(UserId::new).map(userId -> userService.findUserById(tenantId, userId))
                        .collect(Collectors.toList());
                return new PageData<>(users, 1, users.size(), false);
            }
            case CUSTOMER_USERS: {
                if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    throw new IllegalArgumentException("Customer users target is not supported for system administrator");
                }
                CustomerId customerId = new CustomerId(((CustomerUsersNotificationTargetConfig) targetConfig).getCustomerId());
                return userService.findCustomerUsers(tenantId, customerId, pageLink);
            }
            case ALL_USERS: {
                if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    return userService.findUsersByTenantId(tenantId, pageLink);
                } else {
                    return userService.findUsers(TenantId.SYS_TENANT_ID, pageLink);
                }
            }
        }
        return new PageData<>();
    }

    @Override
    public void deleteNotificationTarget(TenantId tenantId, NotificationTargetId notificationTargetId) {
        notificationTargetDao.removeById(tenantId, notificationTargetId.getId());
        // todo: delete related notification requests (?)
    }

    private static class NotificationTargetValidator extends DataValidator<NotificationTarget> {

        @Override
        protected void validateDataImpl(TenantId tenantId, NotificationTarget notificationTarget) {
            super.validateDataImpl(tenantId, notificationTarget);
        }
    }

}
