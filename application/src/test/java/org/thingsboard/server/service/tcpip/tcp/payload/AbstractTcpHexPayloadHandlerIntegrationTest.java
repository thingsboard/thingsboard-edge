/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.tcpip.tcp.payload;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.service.tcpip.tcp.AbstractTcpHandlerIntegrationTest;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractTcpHexPayloadHandlerIntegrationTest extends AbstractTcpHandlerIntegrationTest {

    private static final String CONVERTER_NAME = "Tcp Hex Message Converter Test";
    private static final String INTEGRATION_NAME = "Tcp Hex Integration Test";
    private static final String RESOURCE_FILE_NAME = "tcp_hex_uplink_converter.json";
    private static final int PORT = 12001;

    private Device savedDevice;

    public AbstractTcpHexPayloadHandlerIntegrationTest() {
        super(PORT);
    }

    @Before
    public void beforeTest() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test Tcp Tenant");
        loginSysAdmin();
        this.tenant = doPost("/api/tenant", tenant, Tenant.class);
        assertNotNull(this.tenant);

        loginTenantAdmin();
        InputStream resourceAsStream = ObjectNode.class.getClassLoader().getResourceAsStream(RESOURCE_FILE_NAME);
        ObjectNode jsonFile = mapper.readValue(resourceAsStream, ObjectNode.class);
        assertNotNull(jsonFile);
        if (jsonFile.has("configuration")) {
            converter = createConverter(CONVERTER_NAME, jsonFile.get("configuration"));
        }
        assertNotNull(converter);

        integration = createIntegration(INTEGRATION_NAME, IntegrationType.TCP);
        Assert.assertNotNull(integration);
        Assert.assertNotNull(integration.getId());
        Assert.assertTrue(integration.getCreatedTime() > 0);
        Assert.assertEquals(converter.getId(), integration.getDefaultConverterId());
        Assert.assertNotNull(integration.getRoutingKey());
        Thread.sleep(10000);

        client.connect(port);
        Assert.assertTrue(client.isConnected());
    }

    @After
    public void afterTest() throws Exception {
        doDelete("/api/integration/" + integration.getId().getId().toString());
        doDelete("/api/converter/" + converter.getId().getId().toString());
        doDelete("/api/tenant/" + tenant.getTenantId().getId().toString());
        doDelete("/api/device/" + savedDevice.getId().getId().toString());
    }

    @Override
    protected ObjectNode createHandlerConfiguration() {
        return mapper.createObjectNode()
                .put("handlerType", "JSON")
                .put("maxFrameLength", 37);
    }

    @Test
    public void testCreatingDeviceAsResultReceivingMessageFirstTimeFromDevice() throws Exception {
        String payload = "01014294292BE0E50000000000000000FBE61D5D56268710211F249E150319000100000901";
        String deviceNameStr = Long.toString(Long.parseLong(payload.substring(2, 16), 16));
        doGet("/api/tenant/devices?deviceName=" + deviceNameStr)
                .andExpect(status().isNotFound());
        client.sendData(Hex.decodeHex(payload.toCharArray()));
        Thread.sleep(10000);
        savedDevice = doGet("/api/tenant/devices?deviceName=" + deviceNameStr, Device.class);
        Assert.assertNotNull(savedDevice);
    }

    @Test
    public void testDeviceAsResultReceivingMessageFirstTimeFromDevice() throws Exception {
        String payload = generatePayload();
        String deviceNameStr = Long.toString(Long.parseLong(payload.substring(2, 16), 16));
        doGet("/api/tenant/devices?deviceName=" + deviceNameStr)
                .andExpect(status().isNotFound());
        client.sendData(Hex.decodeHex(payload.toCharArray()));
        Thread.sleep(10000);
        savedDevice = doGet("/api/tenant/devices?deviceName=" + deviceNameStr, Device.class);
        Assert.assertNotNull(savedDevice);
    }

    private String generatePayload() throws DecoderException {
        String deviceNameHexString = Long.toHexString(354679090045157L);
        String reversedTimestampHexString = reverseOrderByPairs(
                Long.toHexString((long) (System.currentTimeMillis() / Math.pow(10, 3)))
        );
        String reversedVoltageHexString = reverseOrderByPairs(Long.toHexString(86L));
        String reversedTemperatureHexString = reverseOrderByPairs(Long.toHexString(38L));
        String reversedLatitudeHexString = reverseOrderByPairs(Long.toHexString(522260615L));
        String reversedLongitudeHexString = reverseOrderByPairs("0" + Long.toHexString(51748388L));
        String reversedAltitudeHexString = reverseOrderByPairs("00" + Long.toHexString(25L));
        String reversedSpeedHexString = reverseOrderByPairs("000" + Long.toHexString(1L));
        String reversedSatellitesObservedHexString = reverseOrderByPairs("0" + Long.toHexString(0L));
        String reversedTimedToFirstFixHexString = reverseOrderByPairs("0" + Long.toHexString(9L));

        return "010" + deviceNameHexString + "0000000000000000"
                + reversedTimestampHexString
                + reversedVoltageHexString
                + reversedTemperatureHexString
                + reversedLatitudeHexString
                + reversedLongitudeHexString
                + reversedAltitudeHexString
                + reversedSpeedHexString
                + reversedSatellitesObservedHexString
                + reversedTimedToFirstFixHexString + "01";
    }

    private String reverseOrderByPairs(String str) throws DecoderException {
        byte[] decodedHex = Hex.decodeHex(str.toCharArray());
        ArrayUtils.reverse(decodedHex);
        return Hex.encodeHexString(decodedHex);
    }
}
