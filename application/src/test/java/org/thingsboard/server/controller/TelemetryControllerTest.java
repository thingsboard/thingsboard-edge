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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.query.EntityKeyType.TIME_SERIES;

@DaoSqlTest
public class TelemetryControllerTest extends AbstractControllerTest {

    @Test
    public void testConstraintValidator() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();
        String correctRequestBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", correctRequestBody, String.class, status().isOk());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", correctRequestBody, String.class, status().isOk());
        String invalidRequestBody = "{\"<object data=\\\"data:text/html,<script>alert(document)</script>\\\"></object>\": \"data\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", invalidRequestBody, String.class, status().isBadRequest());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", invalidRequestBody, String.class, status().isBadRequest());
    }

    @Test
    public void testDeleteAllTelemetryWithLatest() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(device.getId());

        getWsClient().subscribeLatestUpdate(List.of(new EntityKey(TIME_SERIES, "data")), filter);

        getWsClient().registerWaitForUpdate(1);

        long startTs = System.currentTimeMillis();

        String testBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", testBody, String.class, status().isOk());

        long endTs = System.currentTimeMillis();

        ObjectNode latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertNotNull(latest);
        var data = latest.get("data");
        Assert.assertNotNull(data);

        Assert.assertEquals("value", data.get(0).get("value").asText());

        ObjectNode timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertNotNull(timeseries);

        Assert.assertEquals("value", timeseries.get("data").get(0).get("value").asText());

        doDeleteAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/delete?keys=data&deleteAllDataForKeys=true", String.class);

        latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertTrue(latest.get("data").get(0).get("value").isNull());

        timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertTrue(timeseries.isEmpty());
    }

    @Test
    public void testDeleteAllTelemetryWithoutLatest() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(device.getId());

        getWsClient().subscribeLatestUpdate(List.of(new EntityKey(TIME_SERIES, "data")), filter);

        getWsClient().registerWaitForUpdate(1);

        long startTs = System.currentTimeMillis();

        String testBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", testBody, String.class, status().isOk());

        long endTs = System.currentTimeMillis();

        ObjectNode latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertNotNull(latest);

        Assert.assertEquals("value", latest.get("data").get(0).get("value").asText());

        ObjectNode timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertNotNull(timeseries);

        Assert.assertEquals("value", timeseries.get("data").get(0).get("value").asText());

        doDeleteAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/delete?keys=data&deleteAllDataForKeys=true&deleteLatest=false", String.class);

        latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertEquals("value", latest.get("data").get(0).get("value").asText());

        timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertTrue(timeseries.isEmpty());
    }

    private Device createDevice() throws Exception {
        String testToken = "TEST_TOKEN";

        Device device = new Device();
        device.setName("My device");
        device.setType("default");

        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(testToken);

        SaveDeviceWithCredentialsRequest saveRequest = new SaveDeviceWithCredentialsRequest(device, deviceCredentials);

        return readResponse(doPost("/api/device-with-credentials", saveRequest).andExpect(status().isOk()), Device.class);
    }
}
