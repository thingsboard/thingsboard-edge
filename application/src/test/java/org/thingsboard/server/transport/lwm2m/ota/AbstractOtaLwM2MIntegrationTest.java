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
package org.thingsboard.server.transport.lwm2m.ota;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.rest.client.utils.RestJsonConverter.toTimeseries;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;

@Slf4j
@DaoSqlTest
public abstract class AbstractOtaLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    private final  String[] RESOURCES_OTA = new String[]{"3.xml", "5.xml", "9.xml"};
    protected static final String CLIENT_ENDPOINT_WITHOUT_FW_INFO = "WithoutFirmwareInfoDevice";
    protected static final String CLIENT_ENDPOINT_OTA5 = "Ota5_Device";
    protected static final String CLIENT_ENDPOINT_OTA9 = "Ota9_Device";
    protected List<OtaPackageUpdateStatus> expectedStatuses;


    protected final String OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA5 =

            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/5_1.2/0/3\": \"state\",\n" +
                    "      \"/5_1.2/0/5\": \"updateResult\",\n" +
                    "      \"/5_1.2/0/6\": \"pkgname\",\n" +
                    "      \"/5_1.2/0/7\": \"pkgversion\",\n" +
                    "      \"/5_1.2/0/9\": \"firmwareUpdateDeliveryMethod\"\n" +
                    "    },\n" +
                    "    \"observe\": [\n" +
                    "      \"/5_1.2/0/3\",\n" +
                    "      \"/5_1.2/0/5\",\n" +
                    "      \"/5_1.2/0/6\",\n" +
                    "      \"/5_1.2/0/7\",\n" +
                    "      \"/5_1.2/0/9\"\n" +
                    "    ],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/5_1.2/0/3\",\n" +
                    "      \"/5_1.2/0/5\",\n" +
                    "      \"/5_1.2/0/6\",\n" +
                    "      \"/5_1.2/0/7\",\n" +
                    "      \"/5_1.2/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    protected final String OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA9 =

            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/9_1.1/0/0\": \"pkgname\",\n" +
                    "      \"/9_1.1/0/1\": \"pkgversion\",\n" +
                    "      \"/9_1.1/0/7\": \"updateState\",\n" +
                    "      \"/9_1.1/0/9\": \"updateResult\"\n" +
                    "    },\n" +
                    "    \"observe\": [\n" +
                    "      \"/9_1.1/0/0\",\n" +
                    "      \"/9_1.1/0/1\",\n" +
                    "      \"/9_1.1/0/7\",\n" +
                    "      \"/9_1.1/0/9\"\n" +
                    "    ],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/9_1.1/0/0\",\n" +
                    "      \"/9_1.1/0/1\",\n" +
                    "      \"/9_1.1/0/7\",\n" +
                    "      \"/9_1.1/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    public AbstractOtaLwM2MIntegrationTest() {
        setResources(this.RESOURCES_OTA);
    }

    protected OtaPackageInfo createFirmware(String version, DeviceProfileId deviceProfileId) throws Exception {
        String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";

        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("My firmware");
        firmwareInfo.setVersion(version);

        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);

        MockMultipartFile testData = new MockMultipartFile("file", "filename.txt", "text/plain", new byte[]{1});

        return savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, "SHA256");
    }

    protected OtaPackageInfo createSoftware(DeviceProfileId deviceProfileId) throws Exception {
        String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";

        OtaPackageInfo swInfo = new OtaPackageInfo();
        swInfo.setDeviceProfileId(deviceProfileId);
        swInfo.setType(SOFTWARE);
        swInfo.setTitle("My sw");
        swInfo.setVersion("v1.0");

        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", swInfo, OtaPackageInfo.class);

        MockMultipartFile testData = new MockMultipartFile("file", "filename.txt", "text/plain", new byte[]{1});

        return savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, "SHA256");
    }

    protected OtaPackageInfo savaData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackageInfo.class);
    }


    protected Device getDeviceFromAPI(UUID deviceId) throws Exception {
        final Device device = doGet("/api/device/" + deviceId, Device.class);
        log.trace("Fetched device by API for deviceId {}, device is {}", deviceId, device);
        return device;
    }

    protected List<TsKvEntry> getFwSwStateTelemetryFromAPI(UUID deviceId, String type_state) throws Exception {
        final List<TsKvEntry> tsKvEntries = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?orderBy=ASC&keys=" + type_state + "&startTs=0&endTs=" + System.currentTimeMillis(), new TypeReference<>() {
        }));
        log.warn("Fetched telemetry by API for deviceId {}, list size {}, tsKvEntries {}", deviceId, tsKvEntries.size(), tsKvEntries);
        return tsKvEntries;
    }

    protected boolean predicateForStatuses(List<TsKvEntry> ts) {
        List<OtaPackageUpdateStatus> statuses = ts.stream()
                .sorted(Comparator.comparingLong(TsKvEntry::getTs))
                .map(KvEntry::getValueAsString)
                .map(OtaPackageUpdateStatus::valueOf)
                .collect(Collectors.toList());
        log.warn("{}", statuses);
        return statuses.containsAll(expectedStatuses);
    }
}
