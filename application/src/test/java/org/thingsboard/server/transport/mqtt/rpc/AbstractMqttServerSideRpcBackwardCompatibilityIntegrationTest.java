/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.mqtt.rpc;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;

@Slf4j
public abstract class AbstractMqttServerSideRpcBackwardCompatibilityIntegrationTest extends AbstractMqttServerSideRpcIntegrationTest {

    @After
    public void afterTest() throws Exception {
        super.processAfterTest();
    }

    @Test
    public void testServerMqttOneWayRpcWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true, false);
        processOneWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttOneWayRpcOnShortTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true, false);
        processOneWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    @Test
    public void testServerMqttOneWayRpcOnShortProtoTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true, false);
        processOneWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcWithEnabledJsonCompatibility() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, false, false);
        processProtoTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true, false);
        processJsonTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortTopic() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true, false);
        processProtoTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortProtoTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true, false);
        processProtoTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortJsonTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true, false);
        processJsonTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC);
    }

    @Test
    public void testGatewayServerMqttOneWayRpcWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true, false);
        processProtoOneWayRpcTestGateway("Gateway Device OneWay RPC Proto");
    }

    @Test
    public void testGatewayServerMqttTwoWayRpcWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true, false);
        processProtoTwoWayRpcTestGateway("Gateway Device TwoWay RPC Proto");
    }

}
