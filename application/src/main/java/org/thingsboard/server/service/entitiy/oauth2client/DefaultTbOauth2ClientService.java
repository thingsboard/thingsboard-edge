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
package org.thingsboard.server.service.entitiy.oauth2client;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbOauth2ClientService extends AbstractTbEntityService implements TbOauth2ClientService {

    private final OAuth2ClientService oAuth2ClientService;

    @Override
    public OAuth2Client save(OAuth2Client oAuth2Client, User user) throws Exception {
        ActionType actionType = oAuth2Client.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = oAuth2Client.getTenantId();
        try {
            OAuth2Client savedClient = checkNotNull(oAuth2ClientService.saveOAuth2Client(tenantId, oAuth2Client));
            logEntityActionService.logEntityAction(tenantId, savedClient.getId(), savedClient, actionType, user);
            return savedClient;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.OAUTH2_CLIENT), oAuth2Client, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(OAuth2Client oAuth2Client, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = oAuth2Client.getTenantId();
        OAuth2ClientId oAuth2ClientId = oAuth2Client.getId();
        try {
            oAuth2ClientService.deleteOAuth2ClientById(tenantId, oAuth2ClientId);
            logEntityActionService.logEntityAction(tenantId, oAuth2ClientId, oAuth2Client, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, oAuth2ClientId, oAuth2Client, actionType, user, e);
            throw e;
        }
    }
}
