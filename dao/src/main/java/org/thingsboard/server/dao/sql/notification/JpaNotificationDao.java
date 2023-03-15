/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.notification;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.NotificationEntity;
import org.thingsboard.server.dao.notification.NotificationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationDao extends JpaAbstractDao<NotificationEntity, Notification> implements NotificationDao {

    private final NotificationRepository notificationRepository;
    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.notifications.partition_size:168}")
    private int partitionSizeInHours;

    @Override
    public Notification save(TenantId tenantId, Notification notification) {
        if (notification.getId() == null) {
            UUID uuid = Uuids.timeBased();
            notification.setId(new NotificationId(uuid));
            notification.setCreatedTime(Uuids.unixTimestamp(uuid));
            partitioningRepository.createPartitionIfNotExists(ModelConstants.NOTIFICATION_TABLE_NAME,
                    notification.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
        return super.save(tenantId, notification);
    }

    @Override
    public PageData<Notification> findUnreadByRecipientIdAndPageLink(TenantId tenantId, UserId recipientId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRepository.findByRecipientIdAndStatusNot(recipientId.getId(), NotificationStatus.READ, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Notification> findByRecipientIdAndPageLink(TenantId tenantId, UserId recipientId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRepository.findByRecipientId(recipientId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean updateStatusByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId, NotificationStatus status) {
        return notificationRepository.updateStatusByIdAndRecipientId(notificationId.getId(), recipientId.getId(), status) != 0;
    }

    @Override
    public int countUnreadByRecipientId(TenantId tenantId, UserId recipientId) {
        return notificationRepository.countByRecipientIdAndStatusNot(recipientId.getId(), NotificationStatus.READ);
    }

    @Override
    public PageData<Notification> findByRequestId(TenantId tenantId, NotificationRequestId notificationRequestId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRepository.findByRequestId(notificationRequestId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean deleteByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        return notificationRepository.deleteByIdAndRecipientId(notificationId.getId(), recipientId.getId()) != 0;
    }

    @Override
    public int updateStatusByRecipientId(TenantId tenantId, UserId recipientId, NotificationStatus status) {
        return notificationRepository.updateStatusByRecipientId(recipientId.getId(), status);
    }

    @Override
    protected Class<NotificationEntity> getEntityClass() {
        return NotificationEntity.class;
    }

    @Override
    protected JpaRepository<NotificationEntity, UUID> getRepository() {
        return notificationRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION;
    }

}
