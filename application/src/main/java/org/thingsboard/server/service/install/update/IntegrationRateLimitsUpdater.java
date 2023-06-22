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
package org.thingsboard.server.service.install.update;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.tenant.TenantProfileService;

@Component
@Profile("install")
@RequiredArgsConstructor
public class IntegrationRateLimitsUpdater extends PaginatedUpdater<String, TenantProfile> {

    private final TenantProfileService tenantProfileService;

    @Value("#{ environment.getProperty('TB_INTEGRATION_RATE_LIMITS_ENABLED') ?: environment.getProperty('integrations.rate_limits.enabled') ?: 'false' }")
    protected boolean rateLimitsEnabled;
    @Value("#{ environment.getProperty('TB_INTEGRATION_RATE_LIMITS_TENANT') ?: environment.getProperty('integrations.rate_limits.tenant') ?: '1000:1,20000:60' }")
    protected String msgsPerTenantRateLimit;
    @Value("#{ environment.getProperty('TB_INTEGRATION_RATE_LIMITS_DEVICE') ?: environment.getProperty('integrations.rate_limits.device') ?: '10:1,300:60' }")
    protected String msgsPerDeviceRateLimit;

    @Override
    protected PageData<TenantProfile> findEntities(String id, PageLink pageLink) {
        if (!rateLimitsEnabled) {
            return PageData.emptyPageData();
        }
        return tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    protected void updateEntity(TenantProfile tenantProfile) {
        DefaultTenantProfileConfiguration profileConfiguration = tenantProfile.getDefaultProfileConfiguration();
        if (StringUtils.isNotEmpty(msgsPerTenantRateLimit)) {
            profileConfiguration.setIntegrationMsgsPerTenantRateLimit(msgsPerTenantRateLimit);
        }
        if (StringUtils.isNotEmpty(msgsPerDeviceRateLimit)) {
            profileConfiguration.setIntegrationMsgsPerDeviceRateLimit(msgsPerDeviceRateLimit);
        }
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
    }

    @Override
    protected String getName() {
        return "Integration rate limits updater";
    }

    @Override
    protected boolean forceReportTotal() {
        return true;
    }

}
