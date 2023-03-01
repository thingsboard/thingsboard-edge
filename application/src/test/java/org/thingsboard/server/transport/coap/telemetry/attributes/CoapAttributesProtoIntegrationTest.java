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
package org.thingsboard.server.transport.coap.telemetry.attributes;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
@DaoSqlTest
public class CoapAttributesProtoIntegrationTest extends CoapAttributesIntegrationTest {

    @Before
    @Override
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Attributes device Proto")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testPushAttributes() throws Exception {
        DynamicSchema attributesSchema = getDynamicSchema();
        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNumber"), 42)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        DynamicMessage postAttributesMsg = postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("key1"), "value1")
                .setField(postAttributesMsgDescriptor.findFieldByName("key2"), true)
                .setField(postAttributesMsgDescriptor.findFieldByName("key3"), 3.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("key4"), 4)
                .setField(postAttributesMsgDescriptor.findFieldByName("key5"), jsonObject)
                .build();
        processAttributesTest(Arrays.asList("key1", "key2", "key3", "key4", "key5"), postAttributesMsg.toByteArray(), false);
    }

    @Test
    public void testPushAttributesWithExplicitPresenceProtoKeys() throws Exception {
        DynamicSchema attributesSchema = getDynamicSchema();
        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        DynamicMessage postAttributesMsg = postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("key1"), "")
                .setField(postAttributesMsgDescriptor.findFieldByName("key5"), jsonObject)
                .build();
        processAttributesTest(Arrays.asList("key1", "key5"), postAttributesMsg.toByteArray(), true);
    }

    private DynamicSchema getDynamicSchema() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof CoapDeviceProfileTransportConfiguration);
        CoapDeviceProfileTransportConfiguration coapTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
        CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapTransportConfiguration.getCoapDeviceTypeConfiguration();
        assertTrue(coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration);
        DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        String deviceAttributesProtoSchema = protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema();
        ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(deviceAttributesProtoSchema);
        return DynamicProtoUtils.getDynamicSchema(protoFileElement, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);
    }

}
