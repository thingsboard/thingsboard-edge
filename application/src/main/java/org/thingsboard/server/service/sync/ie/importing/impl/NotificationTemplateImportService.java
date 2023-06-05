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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class NotificationTemplateImportService extends BaseEntityImportService<NotificationTemplateId, NotificationTemplate, EntityExportData<NotificationTemplate>> {

    private final NotificationTemplateService notificationTemplateService;

    @Override
    protected void setOwner(TenantId tenantId, NotificationTemplate notificationTemplate, IdProvider idProvider) {
        notificationTemplate.setTenantId(tenantId);
    }

    @Override
    protected NotificationTemplate prepare(EntitiesImportCtx ctx, NotificationTemplate notificationTemplate, NotificationTemplate oldEntity, EntityExportData<NotificationTemplate> exportData, IdProvider idProvider) {
        return notificationTemplate;
    }

    @Override
    protected NotificationTemplate saveOrUpdate(EntitiesImportCtx ctx, NotificationTemplate notificationTemplate, EntityExportData<NotificationTemplate> exportData, IdProvider idProvider) {
        ConstraintValidator.validateFields(notificationTemplate);
        return notificationTemplateService.saveNotificationTemplate(ctx.getTenantId(), notificationTemplate);
    }

    @Override
    protected void onEntitySaved(User user, NotificationTemplate savedEntity, NotificationTemplate oldEntity) throws ThingsboardException {
        entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, null);
    }

    @Override
    protected NotificationTemplate deepCopy(NotificationTemplate notificationTemplate) {
        return new NotificationTemplate(notificationTemplate);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TEMPLATE;
    }

}
