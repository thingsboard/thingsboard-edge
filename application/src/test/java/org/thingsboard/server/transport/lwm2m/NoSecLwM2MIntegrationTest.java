/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredentials;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.service.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.rest.client.utils.RestJsonConverter.toTimeseries;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.INITIATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.QUEUED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;

public class NoSecLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    private final String OTA_TRANSPORT_CONFIGURATION = "{\n" +
            "  \"observeAttr\": {\n" +
            "    \"keyName\": {\n" +
            "      \"/5_1.0/0/3\": \"state\",\n" +
            "      \"/5_1.0/0/5\": \"updateResult\",\n" +
            "      \"/5_1.0/0/6\": \"pkgname\",\n" +
            "      \"/5_1.0/0/7\": \"pkgversion\",\n" +
            "      \"/5_1.0/0/9\": \"firmwareUpdateDeliveryMethod\",\n" +
            "      \"/9_1.0/0/0\": \"pkgname\",\n" +
            "      \"/9_1.0/0/1\": \"pkgversion\",\n" +
            "      \"/9_1.0/0/7\": \"updateState\",\n" +
            "      \"/9_1.0/0/9\": \"updateResult\"\n" +
            "    },\n" +
            "    \"observe\": [\n" +
            "      \"/5_1.0/0/3\",\n" +
            "      \"/5_1.0/0/5\",\n" +
            "      \"/5_1.0/0/6\",\n" +
            "      \"/5_1.0/0/7\",\n" +
            "      \"/5_1.0/0/9\",\n" +
            "      \"/9_1.0/0/0\",\n" +
            "      \"/9_1.0/0/1\",\n" +
            "      \"/9_1.0/0/7\",\n" +
            "      \"/9_1.0/0/9\"\n" +
            "    ],\n" +
            "    \"attribute\": [],\n" +
            "    \"telemetry\": [\n" +
            "      \"/5_1.0/0/3\",\n" +
            "      \"/5_1.0/0/5\",\n" +
            "      \"/5_1.0/0/6\",\n" +
            "      \"/5_1.0/0/7\",\n" +
            "      \"/5_1.0/0/9\",\n" +
            "      \"/9_1.0/0/0\",\n" +
            "      \"/9_1.0/0/1\",\n" +
            "      \"/9_1.0/0/7\",\n" +
            "      \"/9_1.0/0/9\"\n" +
            "    ],\n" +
            "    \"attributeLwm2m\": {}\n" +
            "  },\n" +
            "  \"bootstrap\": {\n" +
            "    \"servers\": {\n" +
            "      \"binding\": \"UQ\",\n" +
            "      \"shortId\": 123,\n" +
            "      \"lifetime\": 300,\n" +
            "      \"notifIfDisabled\": true,\n" +
            "      \"defaultMinPeriod\": 1\n" +
            "    },\n" +
            "    \"lwm2mServer\": {\n" +
            "      \"host\": \"localhost\",\n" +
            "      \"port\": 5685,\n" +
            "      \"serverId\": 123,\n" +
            "      \"securityMode\": \"NO_SEC\",\n" +
            "      \"serverPublicKey\": \"\",\n" +
            "      \"clientHoldOffTime\": 1,\n" +
            "      \"bootstrapServerAccountTimeout\": 0\n" +
            "    },\n" +
            "    \"bootstrapServer\": {\n" +
            "      \"host\": \"localhost\",\n" +
            "      \"port\": 5687,\n" +
            "      \"serverId\": 111,\n" +
            "      \"securityMode\": \"NO_SEC\",\n" +
            "      \"serverPublicKey\": \"\",\n" +
            "      \"clientHoldOffTime\": 1,\n" +
            "      \"bootstrapServerAccountTimeout\": 0\n" +
            "    }\n" +
            "  },\n" +
            "  \"clientLwM2mSettings\": {\n" +
            "    \"fwUpdateStrategy\": 1,\n" +
            "    \"swUpdateStrategy\": 1,\n" +
            "    \"clientOnlyObserveAfterConnect\": 1,\n" +
            "    \"powerMode\": \"PSM\",\n" +
            "    \"fwUpdateResource\": \"\",\n" +
            "    \"swUpdateResource\": \"\",\n" +
            "    \"compositeOperationsSupport\": false\n" +
            "  },\n" +
            "  \"type\": \"LWM2M\"\n" +
            "}";

    @Test
    public void testConnectAndObserveTelemetry() throws Exception {
        NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint(ENDPOINT);
        super.basicTestConnectionObserveTelemetry(SECURITY, clientCredentials, COAP_CONFIG, ENDPOINT);
    }

    @Test
    public void testFirmwareUpdateWithClientWithoutFirmwareInfo() throws Exception {
        createDeviceProfile(TRANSPORT_CONFIGURATION);
        NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint(ENDPOINT);
        Device device = createDevice(clientCredentials);

        OtaPackageInfo firmware = createFirmware();

        LwM2MTestClient client = new LwM2MTestClient(executor, ENDPOINT);
        client.init(SECURITY, COAP_CONFIG);

        Thread.sleep(1000);

        device.setFirmwareId(firmware.getId());

        device = doPost("/api/device", device, Device.class);

        Thread.sleep(1000);

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "fw_state")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));

        wsClient.send(mapper.writeValueAsString(wrapper));
        wsClient.waitForReply();

        wsClient.registerWaitForUpdate();

        String msg = wsClient.waitForUpdate();

        EntityDataUpdate update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("fw_state");
        Assert.assertEquals("FAILED", tsValue.getValue());
        client.destroy();
    }

    @Test
    public void testFirmwareUpdateByObject5() throws Exception {
        createDeviceProfile(OTA_TRANSPORT_CONFIGURATION);
        NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint("OTA_" + ENDPOINT);
        Device device = createDevice(clientCredentials);

        OtaPackageInfo firmware = createFirmware();

        LwM2MTestClient client = new LwM2MTestClient(executor, "OTA_" + ENDPOINT);
        client.init(SECURITY, COAP_CONFIG);

        Thread.sleep(1000);

        device.setFirmwareId(firmware.getId());

        device = doPost("/api/device", device, Device.class);

        Thread.sleep(4000);

        List<TsKvEntry> ts = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/values/timeseries?orderBy=ASC&keys=fw_state&startTs=0&endTs=" + System.currentTimeMillis(), new TypeReference<>() {
        }));

        List<OtaPackageUpdateStatus> statuses = ts.stream().map(KvEntry::getValueAsString).map(OtaPackageUpdateStatus::valueOf).collect(Collectors.toList());

        List<OtaPackageUpdateStatus> expectedStatuses = Arrays.asList(QUEUED, INITIATED, DOWNLOADING, DOWNLOADED, UPDATING, UPDATED);

        Assert.assertEquals(expectedStatuses, statuses);

        client.destroy();
    }

    @Test
    public void testSoftwareUpdateByObject9() throws Exception {
        createDeviceProfile(OTA_TRANSPORT_CONFIGURATION);
        NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint("OTA_" + ENDPOINT);
        Device device = createDevice(clientCredentials);

        OtaPackageInfo software = createSoftware();

        LwM2MTestClient client = new LwM2MTestClient(executor, "OTA_" + ENDPOINT);
        client.init(SECURITY, COAP_CONFIG);

        Thread.sleep(1000);

        device.setSoftwareId(software.getId());

        device = doPost("/api/device", device, Device.class);

        Thread.sleep(4000);

        List<TsKvEntry> ts = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/values/timeseries?orderBy=ASC&keys=sw_state&startTs=0&endTs=" + System.currentTimeMillis(), new TypeReference<>() {
        }));

        List<OtaPackageUpdateStatus> statuses = ts.stream().map(KvEntry::getValueAsString).map(OtaPackageUpdateStatus::valueOf).collect(Collectors.toList());

        List<OtaPackageUpdateStatus> expectedStatuses = Arrays.asList(QUEUED, INITIATED, DOWNLOADING, DOWNLOADING, DOWNLOADING, DOWNLOADED, VERIFIED, UPDATED);

        Assert.assertEquals(expectedStatuses, statuses);

        client.destroy();
    }

}
