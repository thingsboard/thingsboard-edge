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
package org.thingsboard.server.service.entitiy.domain;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbDomainService extends AbstractTbEntityService implements TbDomainService {

    private final DomainService domainService;

    @Override
    public Domain save(Domain domain, List<OAuth2ClientId> oAuth2Clients, User user) throws Exception {
        ActionType actionType = domain.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = domain.getTenantId();
        try {
            Domain savedDomain = checkNotNull(domainService.saveDomain(tenantId, domain));
            if (CollectionUtils.isNotEmpty(oAuth2Clients)) {
                domainService.updateOauth2Clients(domain.getTenantId(), savedDomain.getId(), oAuth2Clients);
            }
            logEntityActionService.logEntityAction(tenantId, savedDomain.getId(), savedDomain, actionType, user, oAuth2Clients);
            return savedDomain;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DOMAIN), domain, actionType, user, e, oAuth2Clients);
            throw e;
        }
    }

    @Override
    public void updateOauth2Clients(Domain domain, List<OAuth2ClientId> oAuth2ClientIds, User user) {
        ActionType actionType = ActionType.UPDATED;
        TenantId tenantId = domain.getTenantId();
        DomainId domainId = domain.getId();
        try {
            domainService.updateOauth2Clients(tenantId, domainId, oAuth2ClientIds);
            logEntityActionService.logEntityAction(tenantId, domainId, domain, actionType, user, oAuth2ClientIds);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, domainId, domain, actionType, user, e, oAuth2ClientIds);
            throw e;
        }
    }

    @Override
    public void delete(Domain domain, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = domain.getTenantId();
        DomainId domainId = domain.getId();
        try {
            domainService.deleteDomainById(tenantId, domainId);
            logEntityActionService.logEntityAction(tenantId, domainId, domain, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, domainId, domain, actionType, user, e);
            throw e;
        }
    }

}
