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
package org.thingsboard.server.dao.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultNotificationTemplateService extends AbstractEntityService implements NotificationTemplateService, EntityDaoService {

    private final NotificationTemplateDao notificationTemplateDao;
    private final NotificationRequestDao notificationRequestDao;

    @Override
    public NotificationTemplate findNotificationTemplateById(TenantId tenantId, NotificationTemplateId id) {
        return notificationTemplateDao.findById(tenantId, id.getId());
    }

    @Override
    public NotificationTemplate saveNotificationTemplate(TenantId tenantId, NotificationTemplate notificationTemplate) {
        NotificationType notificationType = notificationTemplate.getNotificationType();
        if (notificationTemplate.getId() != null) {
            NotificationTemplate oldNotificationTemplate = findNotificationTemplateById(tenantId, notificationTemplate.getId());
            if (oldNotificationTemplate != null && notificationType != oldNotificationTemplate.getNotificationType()) {
                throw new IllegalArgumentException("Notification type cannot be updated");
            }
        } else {
            if (notificationType.isSystem()) {
                int systemTemplatesCount = countNotificationTemplatesByTenantIdAndNotificationTypes(tenantId, List.of(notificationType));
                if (systemTemplatesCount > 0) {
                    throw new IllegalArgumentException("There can only be one notification template of this type");
                }
            }
        }
        try {
            NotificationTemplate savedTemplate = notificationTemplateDao.saveAndFlush(tenantId, notificationTemplate);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedTemplate.getId())
                    .created(notificationTemplate.getId() == null).build());
            return savedTemplate;
        } catch (Exception e) {
            checkConstraintViolation(e, Map.of(
                    "uq_notification_template_name", "Notification template with such name already exists"
            ));
            throw e;
        }
    }

    @Override
    public PageData<NotificationTemplate> findNotificationTemplatesByTenantIdAndNotificationTypes(TenantId tenantId, List<NotificationType> notificationTypes, PageLink pageLink) {
        return notificationTemplateDao.findByTenantIdAndNotificationTypesAndPageLink(tenantId, notificationTypes, pageLink);
    }

    @Override
    public Optional<NotificationTemplate> findTenantOrSystemNotificationTemplate(TenantId tenantId, NotificationType notificationType) {
        return findNotificationTemplateByTenantIdAndType(tenantId, notificationType)
                .or(() -> findNotificationTemplateByTenantIdAndType(TenantId.SYS_TENANT_ID, notificationType));
    }

    @Override
    public Optional<NotificationTemplate> findNotificationTemplateByTenantIdAndType(TenantId tenantId, NotificationType notificationType) {
        return findNotificationTemplatesByTenantIdAndNotificationTypes(tenantId, List.of(notificationType), new PageLink(1)).getData()
                .stream().findFirst();
    }

    @Override
    public Optional<NotificationTemplate> findNotificationTemplateByTenantIdAndName(TenantId tenantId, String name) {
        return Optional.ofNullable(notificationTemplateDao.findByTenantIdAndName(tenantId.getId(), name));
    }

    @Override
    public int countNotificationTemplatesByTenantIdAndNotificationTypes(TenantId tenantId, Collection<NotificationType> notificationTypes) {
        return notificationTemplateDao.countByTenantIdAndNotificationTypes(tenantId, notificationTypes);
    }

    @Override
    public void deleteNotificationTemplateById(TenantId tenantId, NotificationTemplateId id) {
        deleteEntity(tenantId, id, false);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        if (!force) {
            if (notificationRequestDao.existsByTenantIdAndStatusAndTemplateId(tenantId, NotificationRequestStatus.SCHEDULED, (NotificationTemplateId) id)) {
                throw new IllegalArgumentException("Notification template is referenced by scheduled notification request");
            }
            if (tenantId.isSysTenantId()) {
                NotificationTemplate notificationTemplate = findNotificationTemplateById(tenantId, (NotificationTemplateId) id);
                if (notificationTemplate.getNotificationType().isSystem()) {
                    throw new IllegalArgumentException("System notification template cannot be deleted");
                }
            }
        }
        try {
            notificationTemplateDao.removeById(tenantId, id.getId());
        } catch (Exception e) {
            checkConstraintViolation(e, Map.of(
                    "fk_notification_rule_template_id", "Notification template is referenced by notification rule"
            ));
            throw e;
        }
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(id).build());
    }

    @Override
    public void deleteNotificationTemplatesByTenantId(TenantId tenantId) {
        notificationTemplateDao.removeByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteNotificationTemplatesByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findNotificationTemplateById(tenantId, new NotificationTemplateId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TEMPLATE;
    }

}
