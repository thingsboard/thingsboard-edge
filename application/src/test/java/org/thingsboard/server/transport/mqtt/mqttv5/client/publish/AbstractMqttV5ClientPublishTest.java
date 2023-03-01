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
package org.thingsboard.server.transport.mqtt.mqttv5.client.publish;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.packet.MqttPubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_PUBACK;

public abstract class AbstractMqttV5ClientPublishTest extends AbstractMqttIntegrationTest {

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    protected static final String INVALID_PAYLOAD_VALUES_STR = "\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    protected void processClientPublishToCorrectTopicTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        IMqttToken publishResult = client.publishAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, PAYLOAD_VALUES_STR.getBytes());
        MqttWireMessage response = publishResult.getResponse();

        Assert.assertEquals(MESSAGE_TYPE_PUBACK, response.getType());

        MqttPubAck pubAckMsg = (MqttPubAck) response;

        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, pubAckMsg.getReturnCode());

        client.disconnect();
    }

    protected void processClientPublishToWrongTopicTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        IMqttToken iMqttToken = client.publishAndWait("wrong/topic/", PAYLOAD_VALUES_STR.getBytes());
        Assert.assertEquals(MESSAGE_TYPE_PUBACK,iMqttToken.getResponse().getType());
        MqttPubAck pubAck = (MqttPubAck) iMqttToken.getResponse();
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_TOPIC_NAME_INVALID, pubAck.getReturnCode());

        client.disconnect();
    }

    protected void processClientPublishWithInvalidPayloadTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        IMqttToken iMqttToken = client.publishAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, INVALID_PAYLOAD_VALUES_STR.getBytes());
        Assert.assertEquals(MESSAGE_TYPE_PUBACK,iMqttToken.getResponse().getType());
        MqttPubAck pubAck = (MqttPubAck) iMqttToken.getResponse();
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_PAYLOAD_FORMAT_INVALID, pubAck.getReturnCode());

        client.disconnect();
    }

}
