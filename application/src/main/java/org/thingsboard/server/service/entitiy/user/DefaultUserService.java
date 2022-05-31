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
package org.thingsboard.server.service.entitiy.user;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.thingsboard.server.controller.UserController.ACTIVATE_URL_PATTERN;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultUserService extends AbstractTbEntityService implements TbUserService {

    private final MailService mailService;
    private final SystemSecurityService systemSecurityService;
    private final UserPermissionsService userPermissionsService;

    @Override
    public User save(TenantId tenantId, CustomerId customerId, Authority authority, User tbUser, boolean sendActivationMail,
                     HttpServletRequest request, EntityGroupId entityGroupId, EntityGroup entityGroup,
                     SecurityUser user) throws ThingsboardException {
        ActionType actionType = tbUser.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            boolean sendEmail = tbUser.getId() == null && sendActivationMail;
            User savedUser = checkNotNull(userService.saveUser(tbUser));

            // Add Tenant Admins to 'Tenant Administrators' user group if created by Sys Admin
            if (tbUser.getId() == null && authority == Authority.SYS_ADMIN) {
                EntityGroup admins = entityGroupService.findOrCreateTenantAdminsGroup(tenantId);
                entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, admins.getId(), savedUser.getId());
                notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, customerId, savedUser.getId(),
                        savedUser, user, ActionType.ADDED_TO_ENTITY_GROUP, false, null);
            } else if (entityGroup != null && tbUser.getId() == null) {
                entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, savedUser.getId());

                notificationEntityService.notifyAddToEntityGroup(tenantId, savedUser.getId(), savedUser, customerId,
                        entityGroupId, user, savedUser.getId().toString(), entityGroupId.toString(), entityGroup.getName());
            }

            if (sendEmail) {
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
                String baseUrl = systemSecurityService.getBaseUrl(authority, tenantId, customerId, request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                String email = savedUser.getEmail();
                try {
                    mailService.sendActivationEmail(tenantId, activateUrl, email);
                } catch (ThingsboardException e) {
                    userService.deleteUser(tenantId, savedUser.getId());
                    throw e;
                }
            }

            userPermissionsService.onUserUpdatedOrRemoved(savedUser);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, customerId, savedUser.getId(),
                    savedUser, user, actionType, true, null);
            return savedUser;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.USER),
                    tbUser, user, actionType, false, e);
            throw handleException(e);
        }
    }

    @Override
    public void delete(TenantId tenantId, CustomerId customerId, User tbUser, SecurityUser user) throws ThingsboardException {
        UserId userId = tbUser.getId();

        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(tenantId, userId);

            userService.deleteUser(tenantId, userId);

            userPermissionsService.onUserUpdatedOrRemoved(user);
            notificationEntityService.notifyDeleteEntity(tenantId, userId, tbUser, customerId,
                    ActionType.DELETED,  relatedEdgeIds, user, userId.toString());

        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.USER),
                    null, user, ActionType.DELETED, false, e, userId.toString());
            throw handleException(e);
        }
    }
}
