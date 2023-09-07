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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.service.edge.rpc.constructor.AssetMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AssetProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessorTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.willReturn;

public abstract class AbstractAssetProcessorTest extends BaseEdgeProcessorTest {

    @MockBean
    AssetProfileService assetProfileService;
    @MockBean
    AssetService assetService;

    @SpyBean
    AssetMsgConstructor assetMsgConstructor;
    @SpyBean
    AssetProfileMsgConstructor assetProfileMsgConstructor;

    protected AssetId assetId;
    protected AssetProfileId assetProfileId;
    protected AssetProfile assetProfile;

    @BeforeEach
    public void setUp() {
        edgeId = new EdgeId(UUID.randomUUID());
        tenantId = new TenantId(UUID.randomUUID());
        assetId = new AssetId(UUID.randomUUID());
        assetProfileId = new AssetProfileId(UUID.randomUUID());

        assetProfile = new AssetProfile();
        assetProfile.setId(assetProfileId);
        assetProfile.setName("AssetProfile");
        assetProfile.setDefault(true);

        Asset asset = new Asset();
        asset.setAssetProfileId(assetProfileId);
        asset.setId(assetId);
        asset.setName("Asset");
        asset.setType(assetProfile.getName());

        edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(EdgeEventActionType.ADDED);


        willReturn(asset).given(assetService).findAssetById(tenantId, assetId);
        willReturn(assetProfile).given(assetProfileService).findAssetProfileById(tenantId, assetProfileId);
    }

    protected void updateAssetProfileDefaultFields(long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                                                    long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        DashboardId dashboardId = getDashboardId(expectedDashboardIdMSB, expectedDashboardIdLSB);
        RuleChainId ruleChainId = getRuleChainId(expectedRuleChainIdMSB, expectedRuleChainIdLSB);

        assetProfile.setDefaultDashboardId(dashboardId);
        assetProfile.setDefaultEdgeRuleChainId(ruleChainId);

    }

    protected void verify(DownlinkMsg downlinkMsg, long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                          long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        AssetProfileUpdateMsg assetProfileUpdateMsg = downlinkMsg.getAssetProfileUpdateMsgList().get(0);
        assertNotNull(assetProfileUpdateMsg);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultDashboardIdMSB()).isEqualTo(expectedDashboardIdMSB);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultDashboardIdLSB()).isEqualTo(expectedDashboardIdLSB);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultRuleChainIdMSB()).isEqualTo(expectedRuleChainIdMSB);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultRuleChainIdLSB()).isEqualTo(expectedRuleChainIdLSB);
    }
}
