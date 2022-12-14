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
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfigType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationTargetController extends BaseController {

    // FIXME: permission checks

    private final NotificationTargetService notificationTargetService;

    @PostMapping("/target")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTarget saveNotificationTarget(@RequestBody NotificationTarget notificationTarget,
                                                     @AuthenticationPrincipal SecurityUser user) throws Exception {
        checkEntity(notificationTarget.getId(), notificationTarget, Resource.NOTIFICATION_TARGET);
        if (!user.isSystemAdmin()) {
            NotificationTargetConfig targetConfig = notificationTarget.getConfiguration();
            if (targetConfig.getType() == NotificationTargetConfigType.SINGLE_USER ||
                    targetConfig.getType() == NotificationTargetConfigType.USER_LIST) {
                PageData<User> recipients = notificationTargetService.findRecipientsForNotificationTargetConfig(user.getTenantId(), notificationTarget.getConfiguration(), null);
                for (User recipient : recipients.getData()) {
                    accessControlService.checkPermission(user, Resource.USER, Operation.READ, recipient.getId(), recipient);
                }
            }
        }

        return doSaveAndLog(EntityType.NOTIFICATION_TARGET, notificationTarget, notificationTargetService::saveNotificationTarget);
    }

    @GetMapping("/target/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTarget getNotificationTargetById(@PathVariable UUID id) throws ThingsboardException {
        NotificationTargetId notificationTargetId = new NotificationTargetId(id);
        return checkEntityId(notificationTargetId, notificationTargetService::findNotificationTargetById, Operation.READ);
    }

    @PostMapping("/target/recipients")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<User> getRecipientsForNotificationTargetConfig(@RequestBody NotificationTarget notificationTarget,
                                                                   @RequestParam int pageSize,
                                                                   @RequestParam int page,
                                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, null, null, null);
        PageData<User> recipients = notificationTargetService.findRecipientsForNotificationTargetConfig(user.getTenantId(), notificationTarget.getConfiguration(), pageLink);
        if (!user.isSystemAdmin()) {
            for (User recipient : recipients.getData()) {
                accessControlService.checkPermission(user, Resource.USER, Operation.READ, recipient.getId(), recipient);
            }
        }
        return recipients;
    }

    @GetMapping("/targets")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationTarget> getNotificationTargets(@RequestParam int pageSize,
                                                               @RequestParam int page,
                                                               @RequestParam(required = false) String textSearch,
                                                               @RequestParam(required = false) String sortProperty,
                                                               @RequestParam(required = false) String sortOrder,
                                                               @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.NOTIFICATION_TARGET, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationTargetService.findNotificationTargetsByTenantId(user.getTenantId(), pageLink);
    }

    @DeleteMapping("/target/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationTarget(@PathVariable UUID id) throws Exception {
        NotificationTargetId notificationTargetId = new NotificationTargetId(id);
        NotificationTarget notificationTarget = checkEntityId(notificationTargetId, notificationTargetService::findNotificationTargetById, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_TARGET, notificationTarget, notificationTargetService::deleteNotificationTarget);
    }

}
