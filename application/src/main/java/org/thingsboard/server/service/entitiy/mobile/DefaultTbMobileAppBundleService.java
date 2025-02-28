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
package org.thingsboard.server.service.entitiy.mobile;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.dao.mobile.MobileAppBundleService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbMobileAppBundleService extends AbstractTbEntityService implements TbMobileAppBundleService {

    private final MobileAppBundleService mobileAppBundleService;

    @Override
    public MobileAppBundle save(MobileAppBundle mobileAppBundle, List<OAuth2ClientId> oauth2Clients, User user) throws Exception {
        ActionType actionType = mobileAppBundle.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = mobileAppBundle.getTenantId();
        try {
            MobileAppBundle savedMobileAppBundle = checkNotNull(mobileAppBundleService.saveMobileAppBundle(tenantId, mobileAppBundle));
            if (CollectionUtils.isNotEmpty(oauth2Clients)) {
                mobileAppBundleService.updateOauth2Clients(tenantId, savedMobileAppBundle.getId(), oauth2Clients);
            }
            logEntityActionService.logEntityAction(tenantId, savedMobileAppBundle.getId(), savedMobileAppBundle, actionType, user);
            return savedMobileAppBundle;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.MOBILE_APP), mobileAppBundle, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void updateOauth2Clients(MobileAppBundle mobileAppBundle, List<OAuth2ClientId> oAuth2ClientIds, User user) {
        ActionType actionType = ActionType.UPDATED;
        TenantId tenantId = mobileAppBundle.getTenantId();
        MobileAppBundleId mobileAppBundleId = mobileAppBundle.getId();
        try {
            mobileAppBundleService.updateOauth2Clients(tenantId, mobileAppBundleId, oAuth2ClientIds);
            logEntityActionService.logEntityAction(tenantId, mobileAppBundleId, mobileAppBundle, actionType, user, oAuth2ClientIds);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, mobileAppBundleId, mobileAppBundle, actionType, user, e, oAuth2ClientIds);
            throw e;
        }
    }

    @Override
    public void delete(MobileAppBundle mobileAppBundle, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = mobileAppBundle.getTenantId();
        MobileAppBundleId mobileAppBundleId = mobileAppBundle.getId();
        try {
            mobileAppBundleService.deleteMobileAppBundleById(tenantId, mobileAppBundleId);
            logEntityActionService.logEntityAction(tenantId, mobileAppBundleId, mobileAppBundle, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, mobileAppBundleId, mobileAppBundle, actionType, user, e);
            throw e;
        }
    }
}
