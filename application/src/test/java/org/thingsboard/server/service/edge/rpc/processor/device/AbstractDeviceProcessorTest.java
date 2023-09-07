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
package org.thingsboard.server.service.edge.rpc.processor.device;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessorTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.willReturn;

public abstract class AbstractDeviceProcessorTest extends BaseEdgeProcessorTest {

    @MockBean
    DeviceProfileService deviceProfileService;
    @MockBean
    DeviceService deviceService;

    @SpyBean
    DeviceMsgConstructor deviceMsgConstructor;
    @SpyBean
    DeviceProfileMsgConstructor deviceProfileMsgConstructor;

    protected DeviceId deviceId;
    protected DeviceProfileId deviceProfileId;
    protected DeviceProfile deviceProfile;

    @BeforeEach
    public void setUp() {
        edgeId = new EdgeId(UUID.randomUUID());
        tenantId = new TenantId(UUID.randomUUID());
        deviceId = new DeviceId(UUID.randomUUID());
        deviceProfileId = new DeviceProfileId(UUID.randomUUID());

        deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        deviceProfile.setName("DeviceProfile");
        deviceProfile.setDefault(true);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);

        Device device = new Device();
        device.setDeviceProfileId(deviceProfileId);
        device.setId(deviceId);
        device.setName("Device");
        device.setType(deviceProfile.getName());

        edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(EdgeEventActionType.ADDED);


        willReturn(device).given(deviceService).findDeviceById(tenantId, deviceId);
        willReturn(deviceProfile).given(deviceProfileService).findDeviceProfileById(tenantId, deviceProfileId);
        willReturn(new byte[]{0x00}).given(dataDecodingEncodingService).encode(deviceProfileData);

    }

    protected void updateDeviceProfileDefaultFields(long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                                                    long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        DashboardId dashboardId = getDashboardId(expectedDashboardIdMSB, expectedDashboardIdLSB);
        RuleChainId ruleChainId = getRuleChainId(expectedRuleChainIdMSB, expectedRuleChainIdLSB);

        deviceProfile.setDefaultDashboardId(dashboardId);
        deviceProfile.setDefaultEdgeRuleChainId(ruleChainId);

    }

    protected void verify(DownlinkMsg downlinkMsg, long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                          long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = downlinkMsg.getDeviceProfileUpdateMsgList().get(0);
        assertNotNull(deviceProfileUpdateMsg);
        Assertions.assertThat(deviceProfileUpdateMsg.getDefaultDashboardIdMSB()).isEqualTo(expectedDashboardIdMSB);
        Assertions.assertThat(deviceProfileUpdateMsg.getDefaultDashboardIdLSB()).isEqualTo(expectedDashboardIdLSB);
        Assertions.assertThat(deviceProfileUpdateMsg.getDefaultRuleChainIdMSB()).isEqualTo(expectedRuleChainIdMSB);
        Assertions.assertThat(deviceProfileUpdateMsg.getDefaultRuleChainIdLSB()).isEqualTo(expectedRuleChainIdLSB);
    }
}
