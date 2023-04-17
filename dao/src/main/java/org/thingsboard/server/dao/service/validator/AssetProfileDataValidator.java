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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
public class AssetProfileDataValidator extends DataValidator<AssetProfile> {

    @Autowired
    private AssetProfileDao assetProfileDao;
    @Autowired
    @Lazy
    private AssetProfileService assetProfileService;
    @Autowired
    private TenantService tenantService;
    @Lazy
    @Autowired
    private QueueService queueService;
    @Autowired
    private RuleChainService ruleChainService;
    @Autowired
    private DashboardService dashboardService;

    @Override
    protected void validateDataImpl(TenantId tenantId, AssetProfile assetProfile) {
        if (StringUtils.isEmpty(assetProfile.getName())) {
            throw new DataValidationException("Asset profile name should be specified!");
        }
        if (assetProfile.getTenantId() == null) {
            throw new DataValidationException("Asset profile should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(assetProfile.getTenantId())) {
                throw new DataValidationException("Asset profile is referencing to non-existent tenant!");
            }
        }
        if (assetProfile.isDefault()) {
            AssetProfile defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
            if (defaultAssetProfile != null && !defaultAssetProfile.getId().equals(assetProfile.getId())) {
                throw new DataValidationException("Another default asset profile is present in scope of current tenant!");
            }
        }
        if (StringUtils.isNotEmpty(assetProfile.getDefaultQueueName())) {
            Queue queue = queueService.findQueueByTenantIdAndName(tenantId, assetProfile.getDefaultQueueName());
            if (queue == null) {
                throw new DataValidationException("Asset profile is referencing to non-existent queue!");
            }
        }

        if (assetProfile.getDefaultRuleChainId() != null) {
            RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, assetProfile.getDefaultRuleChainId());
            if (ruleChain == null) {
                throw new DataValidationException("Can't assign non-existent rule chain!");
            }
            if (!ruleChain.getTenantId().equals(assetProfile.getTenantId())) {
                throw new DataValidationException("Can't assign rule chain from different tenant!");
            }
        }

        if (assetProfile.getDefaultDashboardId() != null) {
            DashboardInfo dashboard = dashboardService.findDashboardInfoById(tenantId, assetProfile.getDefaultDashboardId());
            if (dashboard == null) {
                throw new DataValidationException("Can't assign non-existent dashboard!");
            }
            if (!dashboard.getTenantId().equals(assetProfile.getTenantId())) {
                throw new DataValidationException("Can't assign dashboard from different tenant!");
            }
        }
    }

    @Override
    protected AssetProfile validateUpdate(TenantId tenantId, AssetProfile assetProfile) {
        AssetProfile old = assetProfileDao.findById(assetProfile.getTenantId(), assetProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing asset profile!");
        }
        return old;
    }

}
