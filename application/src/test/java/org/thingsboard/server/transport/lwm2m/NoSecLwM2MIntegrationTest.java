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

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.client.object.Security;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredentials;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.service.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MCredentials;

import java.util.Collections;
import java.util.List;

import static org.eclipse.leshan.client.object.Security.noSec;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NoSecLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    private final int PORT = 5685;
    private final Security SECURITY = noSec("coap://localhost:" + PORT, 123);
    private final NetworkConfig COAP_CONFIG = new NetworkConfig().setString("COAP_PORT", Integer.toString(PORT));
    private final String ENDPOINT = "deviceAEndpoint";

    @NotNull
    private Device createDevice() throws Exception {
        Device device = new Device();
        device.setName("Device A");
        device.setDeviceProfileId(deviceProfile.getId());
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);

        LwM2MCredentials noSecCredentials = new LwM2MCredentials();
        NoSecClientCredentials clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint(ENDPOINT);
        noSecCredentials.setClient(clientCredentials);
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(noSecCredentials));
        doPost("/api/device/credentials", deviceCredentials).andExpect(status().isOk());
        return device;
    }

    @Test
    public void testConnectAndObserveTelemetry() throws Exception {
        createDeviceProfile(TRANSPORT_CONFIGURATION);

        Device device = createDevice();

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));

        wsClient.send(mapper.writeValueAsString(wrapper));
        wsClient.waitForReply();

        wsClient.registerWaitForUpdate();
        LwM2MTestClient client = new LwM2MTestClient(executor, ENDPOINT);
        client.init(SECURITY, COAP_CONFIG);
        String msg = wsClient.waitForUpdate();

        EntityDataUpdate update = mapper.readValue(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        Assert.assertEquals(42, Long.parseLong(tsValue.getValue()));
        client.destroy();
    }

}
