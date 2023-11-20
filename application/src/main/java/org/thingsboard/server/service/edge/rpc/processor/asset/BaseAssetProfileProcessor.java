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
package org.thingsboard.server.service.edge.rpc.processor.asset;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class BaseAssetProfileProcessor extends BaseEdgeProcessor {

    protected Pair<Boolean, Boolean> saveOrUpdateAssetProfile(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg, EdgeVersion edgeVersion) {
        boolean created = false;
        boolean assetProfileNameUpdated = false;
        assetCreationLock.lock();
        try {
            AssetProfile assetProfile = EdgeVersionUtils.isEdgeVersionOlderThan_3_6_2(edgeVersion)
                    ? createAssetProfile(tenantId, assetProfileId, assetProfileUpdateMsg)
                    : JacksonUtil.fromStringIgnoreUnknownProperties(assetProfileUpdateMsg.getEntity(), AssetProfile.class);
            if (assetProfile == null) {
                throw new RuntimeException("[{" + tenantId + "}] assetProfileUpdateMsg {" + assetProfileUpdateMsg + "} cannot be converted to asset profile");
            }
            AssetProfile assetProfileById = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
            if (assetProfileById == null) {
                created = true;
                assetProfile.setId(null);
            } else {
                assetProfile.setId(assetProfileId);
            }
            String assetProfileName = assetProfile.getName();
            AssetProfile assetProfileByName = assetProfileService.findAssetProfileByName(tenantId, assetProfileName);
            if (assetProfileByName != null && !assetProfileByName.getId().equals(assetProfileId)) {
                assetProfileName = assetProfileName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] Asset profile with name {} already exists. Renaming asset profile name to {}",
                        tenantId, assetProfile.getName(), assetProfileName);
                assetProfileNameUpdated = true;
            }
            assetProfile.setName(assetProfileName);

            RuleChainId ruleChainId = assetProfile.getDefaultRuleChainId();
            setDefaultRuleChainId(tenantId, assetProfile, created ? null : assetProfileById.getDefaultRuleChainId());
            setDefaultEdgeRuleChainId(assetProfile, ruleChainId, assetProfileUpdateMsg, edgeVersion);
            setDefaultDashboardId(tenantId, created ? null : assetProfileById.getDefaultDashboardId(), assetProfile, assetProfileUpdateMsg, edgeVersion);

            assetProfileValidator.validate(assetProfile, AssetProfile::getTenantId);
            if (created) {
                assetProfile.setId(assetProfileId);
            }
            assetProfileService.saveAssetProfile(assetProfile, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process asset profile update msg [{}]", tenantId, assetProfileUpdateMsg, e);
            throw e;
        } finally {
            assetCreationLock.unlock();
        }
        return Pair.of(created, assetProfileNameUpdated);
    }

    private AssetProfile createAssetProfile(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName(assetProfile.getName());
        assetProfile.setCreatedTime(Uuids.unixTimestamp(assetProfileId.getId()));
        assetProfile.setDefault(assetProfileUpdateMsg.getDefault());
        assetProfile.setDefaultQueueName(assetProfileUpdateMsg.hasDefaultQueueName() ? assetProfileUpdateMsg.getDefaultQueueName() : null);
        assetProfile.setDescription(assetProfileUpdateMsg.hasDescription() ? assetProfileUpdateMsg.getDescription() : null);
        assetProfile.setImage(assetProfileUpdateMsg.hasImage()
                ? new String(assetProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);
        return assetProfile;
    }

    protected abstract void setDefaultRuleChainId(TenantId tenantId, AssetProfile assetProfile, RuleChainId ruleChainId);

    protected abstract void setDefaultEdgeRuleChainId(AssetProfile assetProfile, RuleChainId ruleChainId, AssetProfileUpdateMsg assetProfileUpdateMsg, EdgeVersion edgeVersion);

    protected abstract void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, AssetProfile assetProfile, AssetProfileUpdateMsg assetProfileUpdateMsg, EdgeVersion edgeVersion);
}
