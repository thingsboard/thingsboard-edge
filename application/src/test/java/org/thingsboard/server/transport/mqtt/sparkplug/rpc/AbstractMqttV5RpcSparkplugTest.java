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
package org.thingsboard.server.transport.mqtt.sparkplug.rpc;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.exception.ThingsboardErrorCode.INVALID_ARGUMENTS;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DCMD;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NCMD;

@Slf4j
public abstract class AbstractMqttV5RpcSparkplugTest  extends AbstractMqttV5ClientSparkplugTest {

    private static final int metricBirthValue_Int32 = 123456;
    private static final String sparkplugRpcRequest = "{\"metricName\":\"" + metricBirthName_Int32 + "\",\"value\":" + metricBirthValue_Int32 + "}";

    @Test
    public void processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        String expected = "{\"result\":\"Success: " + SparkplugMessageType.NCMD.name() + "\"}";
        String actual = sendRPCSparkplug(NCMD.name(), sparkplugRpcRequest, savedGateway);
        await(alias + SparkplugMessageType.NCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(metricBirthValue_Int32 == mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
    }

    @Test
    public void processClientDeviceWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(1, ts);
        String expected = "{\"result\":\"Success: " + DCMD.name() + "\"}";
        String actual = sendRPCSparkplug(DCMD.name() , sparkplugRpcRequest, devices.get(0));
        await(alias + NCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(metricBirthValue_Int32 == mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
    }

    @Test
    public void processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InvalidTypeMessage_INVALID_ARGUMENTS() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        String invalidateTypeMessageName = "RCMD";
        String expected = "{\"result\":\"" + INVALID_ARGUMENTS + "\",\"error\":\"Failed to convert device RPC command to MQTT msg: " +
                invalidateTypeMessageName + "{\\\"metricName\\\":\\\"" + metricBirthName_Int32 + "\\\",\\\"value\\\":" + metricBirthValue_Int32 + "}\"}";
        String actual = sendRPCSparkplug(invalidateTypeMessageName, sparkplugRpcRequest, savedGateway);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InBirthNotHaveMetric_BAD_REQUEST_PARAMS() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        String metricNameBad = metricBirthName_Int32 + "_Bad";
        String sparkplugRpcRequestBad = "{\"metricName\":\"" + metricNameBad + "\",\"value\":" + metricBirthValue_Int32 + "}";
        String expected = "{\"result\":\"BAD_REQUEST_PARAMS\",\"error\":\"Failed send To Node Rpc Request: " +
                DCMD.name() + ". This node does not have a metricName: [" + metricNameBad + "]\"}";
        String actual = sendRPCSparkplug(DCMD.name(), sparkplugRpcRequestBad, savedGateway);
        Assert.assertEquals(expected, actual);
     }

    private String sendRPCSparkplug(String nameTypeMessage, String keyValue, Device device) throws Exception {
        String setRpcRequest = "{\"method\": \"" + nameTypeMessage + "\", \"params\": " + keyValue + "}";
        return doPostAsync("/api/plugins/rpc/twoway/" + device.getId().getId(), setRpcRequest, String.class, status().isOk());
    }

}
