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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.NoSecLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DeviceProfileDataValidator.class)
class DeviceProfileDataValidatorTest {

    private static final String OBSERVE_ATTRIBUTES_WITHOUT_PARAMS =
            "    {\n" +
                    "    \"keyName\": {},\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    public static final String CLIENT_LWM2M_SETTINGS =
            "     {\n" +
                    "    \"edrxCycle\": null,\n" +
                    "    \"powerMode\": \"DRX\",\n" +
                    "    \"fwUpdateResource\": null,\n" +
                    "    \"fwUpdateStrategy\": 1,\n" +
                    "    \"psmActivityTimer\": null,\n" +
                    "    \"swUpdateResource\": null,\n" +
                    "    \"swUpdateStrategy\": 1,\n" +
                    "    \"pagingTransmissionWindow\": null,\n" +
                    "    \"clientOnlyObserveAfterConnect\": 1\n" +
                    "  }";

    private static final String msgErrorLwm2mRange = "LwM2M Server ShortServerId must be in range [1 - 65534]!";
    private static final String msgErrorBsRange = "Bootstrap Server ShortServerId must be in range [0 - 65535]!";
    private static final String msgErrorNotNull = " Server ShortServerId must not be null!";
    private static final String host = "localhost";
    private static final String hostBs = "localhost";

    private static final int port = 5685;
    private static final int portBs = 5687;

    @MockBean
    DeviceProfileDao deviceProfileDao;
    @MockBean
    DeviceProfileService deviceProfileService;
    @MockBean
    DeviceDao deviceDao;
    @MockBean
    TenantService tenantService;
    @MockBean
    QueueService queueService;
    @MockBean
    RuleChainService ruleChainService;
    @MockBean
    DashboardService dashboardService;
    @SpyBean
    DeviceProfileDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @Test
    void testValidateNameInvocation() {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName("default");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        DeviceProfileData data = new DeviceProfileData();
        data.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(data);
        deviceProfile.setTenantId(tenantId);

        validator.validateDataImpl(tenantId, deviceProfile);
        verify(validator).validateString("Device profile name", deviceProfile.getName());
    }

    @Test
    void testValidateDeviceProfile_Lwm2mBootstrap_ShortServerId_Ok() {
        Integer shortServerId = 123;
        Integer shortServerIdBs = 0;
        DeviceProfile deviceProfile = getDeviceProfile(shortServerId, shortServerIdBs);

        validator.validateDataImpl(tenantId, deviceProfile);
        verify(validator).validateString("Device profile name", deviceProfile.getName());
    }

    @Test
    void testValidateDeviceProfile_Lwm2mShortServerId_Ok_BootstrapShortServerId_null_Error() {
        verifyValidationError(123, null, "Bootstrap" + msgErrorNotNull);
    }

    @Test
    void testValidateDeviceProfile_Lwm2mShortServerId_Ok_BootstrapShortServerId_More_65535_Error() {
        verifyValidationError(123, 65536, msgErrorBsRange);
    }

    @Test
    void testValidateDeviceProfile_Lwm2mShortServerId_Ok_BootstrapShortServerId_Less_0_Error() {
        verifyValidationError(123, -1, msgErrorBsRange);
    }

    @Test
    void testValidateDeviceProfile_Lwm2mShortServerId_null_Error_BootstrapShortServerId_Ok() {
        verifyValidationError(null, 1, "LwM2M" + msgErrorNotNull);
    }

    @Test
    void testValidateDeviceProfile_Lwm2mShortServerId_More_65534_Error_BootstrapShortServerId_Ok() {
        verifyValidationError(65535, 111, msgErrorLwm2mRange);
    }

    @Test
    void testValidateDeviceProfile_Lwm2mShortServerId_Less_1_Error_BootstrapShortServerId_Ok() {
        verifyValidationError(0, 111, msgErrorLwm2mRange);
    }

    private DeviceProfile getDeviceProfile(Integer shortServerId, Integer shortServerIdBs) {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration =
                getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsNoSec(shortServerId, shortServerIdBs));
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName("default");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        DeviceProfileData data = new DeviceProfileData();
        data.setTransportConfiguration(transportConfiguration);
        deviceProfile.setProfileData(data);
        deviceProfile.setTenantId(tenantId);
        return deviceProfile;
    }

    private Lwm2mDeviceProfileTransportConfiguration getTransportConfiguration(String observeAttr, List<LwM2MBootstrapServerCredential> bootstrapServerCredentials) {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
        TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(observeAttr, TelemetryMappingConfiguration.class);
        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setBootstrapServerUpdateEnable(true);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);
        transportConfiguration.setBootstrap(bootstrapServerCredentials);
        return transportConfiguration;
    }

    private List<LwM2MBootstrapServerCredential> getBootstrapServerCredentialsNoSec(Integer shortServerId, Integer shortServerIdBs) {
        List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        bootstrap.add(getBootstrapServerCredentialNoSec(false, shortServerId, shortServerIdBs));
        bootstrap.add(getBootstrapServerCredentialNoSec(true, shortServerId, shortServerIdBs));
        return bootstrap;
    }

    private AbstractLwM2MBootstrapServerCredential getBootstrapServerCredentialNoSec(boolean isBootstrap, Integer shortServerId, Integer shortServerIdBs) {
        AbstractLwM2MBootstrapServerCredential bootstrapServerCredential = new NoSecLwM2MBootstrapServerCredential();
        bootstrapServerCredential.setServerPublicKey("");
        bootstrapServerCredential.setShortServerId(isBootstrap ? shortServerIdBs : shortServerId);
        bootstrapServerCredential.setBootstrapServerIs(isBootstrap);
        bootstrapServerCredential.setHost(isBootstrap ? hostBs : host);
        bootstrapServerCredential.setPort(isBootstrap ? portBs : port);
        return bootstrapServerCredential;
    }

    private void verifyValidationError(Integer shortServerId, Integer shortServerIdBs, String msgError) {
        DeviceProfile deviceProfile = getDeviceProfile(shortServerId, shortServerIdBs);
        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, deviceProfile))
                .hasMessageContaining(msgError);

    }

}
