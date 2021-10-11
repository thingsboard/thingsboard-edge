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
package org.thingsboard.server.transport.mqtt.attributes.updates;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public abstract class AbstractMqttAttributesUpdatesProtoIntegrationTest extends AbstractMqttAttributesUpdatesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Subscribe to attribute updates", "Gateway Test Subscribe to attribute updates", TransportPayloadType.PROTOBUF, null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        super.testSubscribeToAttributesUpdatesFromTheServer();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerOnShortTopic() throws Exception {
        super.testSubscribeToAttributesUpdatesFromTheServerOnShortTopic();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerOnShortJsonTopic() throws Exception {}

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerOnShortProtoTopic() throws Exception {
        processTestSubscribeToAttributesUpdates(MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerGateway() throws Exception {
        processGatewayTestSubscribeToAttributesUpdates();
    }

    protected void validateUpdateAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));

    }

    protected void validateDeleteAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("attribute5");

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("attribute5", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));

    }

    protected void validateGatewayUpdateAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());

        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);
        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName("Gateway Device Subscribe to attribute updates");
        gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(expectedAttributeUpdateNotificationMsg);

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));

    }

    protected void validateGatewayDeleteAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("attribute5");
        TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName("Gateway Device Subscribe to attribute updates");
        gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(attributeUpdateNotificationMsg);

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg();

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("attribute5", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));

    }

    protected byte[] getConnectPayloadBytes() {
        TransportApiProtos.ConnectMsg connectProto = getConnectProto();
        return connectProto.toByteArray();
    }

    private TransportApiProtos.ConnectMsg getConnectProto() {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName("Gateway Device Subscribe to attribute updates");
        builder.setDeviceType(TransportPayloadType.PROTOBUF.name());
        return builder.build();
    }

}
