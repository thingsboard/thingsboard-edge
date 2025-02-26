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
package org.thingsboard.server.transport.lwm2m.ota.sql;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.transport.lwm2m.ota.AbstractOtaLwM2MIntegrationTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.INITIATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.QUEUED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

@Slf4j
public class Ota9LwM2MIntegrationTest extends AbstractOtaLwM2MIntegrationTest {

    /**
     * => Start -> INITIAL (State=0) -> DOWNLOAD STARTED;
     * => PKG / URI Write -> DOWNLOAD STARTED (Res=1 (Downloading) && State=1) -> DOWNLOADED
     * => PKG Written -> DOWNLOADED (Res=1 Initial && State=2) -> DELIVERED;
     * => PKG integrity verified -> DELIVERED  (Res=3 (Successfully Downloaded and package integrity verified) && State=3) -> INSTALLED;
     * => Install -> INSTALLED (Res=2 SW successfully installed) && State=4) -> Start
     *
     * */
    @Test
    public void testSoftwareUpdateByObject9() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA9, getBootstrapServerCredentialsNoSec(NONE));
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + this.CLIENT_ENDPOINT_OTA9, transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(this.CLIENT_ENDPOINT_OTA9));
        final Device device = createLwm2mDevice(deviceCredentials, this.CLIENT_ENDPOINT_OTA9, deviceProfile.getId());
        createNewClient(SECURITY_NO_SEC, null, false, this.CLIENT_ENDPOINT_OTA9, device.getId().getId().toString());
        awaitObserveReadAll(4, device.getId().getId().toString());

        device.setSoftwareId(createSoftware(deviceProfile.getId()).getId());
        final Device savedDevice = doPost("/api/device", device, Device.class); //sync call

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        expectedStatuses = List.of(
                QUEUED, INITIATED, DOWNLOADING, DOWNLOADING, DOWNLOADING, DOWNLOADED, VERIFIED, UPDATED);
        List<TsKvEntry> ts = await("await on timeseries")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getFwSwStateTelemetryFromAPI(device.getId().getId(), "sw_state"), this::predicateForStatuses);
        log.warn("Object9: Got the ts: {}", ts);
    }
}
