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
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.SingleUserNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.UserListNotificationTargetConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationTargetService implements NotificationTargetService {

    private final NotificationTargetDao notificationTargetDao;
    private final NotificationTargetValidator validator = new NotificationTargetValidator();

    @Override
    public NotificationTarget saveNotificationTarget(TenantId tenantId, NotificationTarget notificationTarget) {
        notificationTarget.setTenantId(tenantId);
        validator.validate(notificationTarget, NotificationTarget::getTenantId);
        return notificationTargetDao.save(tenantId, notificationTarget);
    }

    @Override
    public NotificationTarget findNotificationTargetById(TenantId tenantId, NotificationTargetId id) {
        return notificationTargetDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<NotificationTarget> findNotificationTargetsByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink) {
        return notificationTargetDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    @Override
    public List<UserId> findRecipientsForNotificationTarget(TenantId tenantId, NotificationTargetId notificationTargetId) {
        NotificationTarget notificationTarget = findNotificationTargetById(tenantId, notificationTargetId);
        NotificationTargetConfig configuration = notificationTarget.getConfiguration();
        List<UserId> recipients = new ArrayList<>();
        switch (configuration.getType()) {
            case SINGLE_USER:
                SingleUserNotificationTargetConfig singleUserNotificationTargetConfig = (SingleUserNotificationTargetConfig) configuration;
                recipients.add(singleUserNotificationTargetConfig.getUserId());
                break;
            case USER_LIST:
                UserListNotificationTargetConfig userListNotificationTargetConfig = (UserListNotificationTargetConfig) configuration;
                recipients.addAll(userListNotificationTargetConfig.getUsersIds());
                break;
        }
        return recipients;
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
