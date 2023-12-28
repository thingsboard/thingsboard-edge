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
package org.thingsboard.server.service.edge.rpc.processor.asset.profile;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@TbCoreComponent
public class AssetProfileEdgeProcessorV1 extends AssetProfileEdgeProcessor {

    @Override
    protected AssetProfile constructAssetProfileFromUpdateMsg(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName(assetProfileUpdateMsg.getName());
        assetProfile.setCreatedTime(Uuids.unixTimestamp(assetProfileId.getId()));
        assetProfile.setDefault(assetProfileUpdateMsg.getDefault());
        assetProfile.setDefaultQueueName(assetProfileUpdateMsg.hasDefaultQueueName() ? assetProfileUpdateMsg.getDefaultQueueName() : null);
        assetProfile.setDescription(assetProfileUpdateMsg.hasDescription() ? assetProfileUpdateMsg.getDescription() : null);
        assetProfile.setImage(assetProfileUpdateMsg.hasImage()
                ? new String(assetProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);
        return assetProfile;
    }

    @Override
    protected void setDefaultRuleChainId(TenantId tenantId, AssetProfile assetProfile, RuleChainId ruleChainId) {
        assetProfile.setDefaultRuleChainId(ruleChainId);
    }

    @Override
    protected void setDefaultEdgeRuleChainId(AssetProfile assetProfile, RuleChainId ruleChainId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        UUID defaultEdgeRuleChainUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultRuleChainIdMSB(), assetProfileUpdateMsg.getDefaultRuleChainIdLSB());
        assetProfile.setDefaultEdgeRuleChainId(defaultEdgeRuleChainUUID != null ? new RuleChainId(defaultEdgeRuleChainUUID) : ruleChainId);
    }

    @Override
    protected void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, AssetProfile assetProfile, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        UUID defaultDashboardUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultDashboardIdMSB(), assetProfileUpdateMsg.getDefaultDashboardIdLSB());
        assetProfile.setDefaultDashboardId(defaultDashboardUUID != null ? new DashboardId(defaultDashboardUUID) : dashboardId);
    }
}
