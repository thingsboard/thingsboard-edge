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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.gen.edge.v1.ConverterUpdateMsg;
import org.thingsboard.server.gen.edge.v1.IntegrationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseIntegrationEdgeTest extends AbstractEdgeTest {

    @Test
    public void testIntegrations() throws Exception {
        JsonNode baseUrlAttribute = JacksonUtil.toJsonNode("{\"baseUrl\": \"http://localhost:18080\"}");
        doPost("/api/plugins/telemetry/" + EntityType.EDGE.name() + "/" + edge.getId() + "/SERVER_SCOPE", baseUrlAttribute)
                .andExpect(status().isOk());

        ObjectNode converterConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode()
                .put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(converterConfiguration);
        converter.setEdgeTemplate(true);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        Integration integration = new Integration();
        integration.setName("Edge integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(savedConverter.getId());
        integration.setType(IntegrationType.HTTP);
        ObjectNode integrationConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        integrationConfiguration.putObject("metadata")
                .put("baseUrl", "${{baseUrl}}");
        integration.setConfiguration(integrationConfiguration);
        integration.setEdgeTemplate(true);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        // 1
        validateIntegrationAssignToEdge(savedIntegration, savedConverter);

        // 2
        validateIntegrationConfigurationUpdate(savedIntegration);

        // 3
        validateConverterConfigurationUpdate(savedConverter);

        // 4
        validateIntegrationDefaultConverterUpdate(savedIntegration);

        // 5
        validateIntegrationDownlinkConverterUpdate(savedIntegration);

        // 6
        validateAddingAndUpdateOfEdgeAttribute();

        // 7
        validateIntegrationUnassignFromEdge(savedIntegration);

        // 8
        validateRemoveOfIntegration(savedIntegration);
    }

    private void validateConverterConfigurationUpdate(Converter savedConverter) throws Exception {
        edgeImitator.expectMessageAmount(1);

        savedConverter.setName("My new converter updated");
        doPost("/api/converter", savedConverter, Converter.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<ConverterUpdateMsg> newConverterUpdateMsgOpt = edgeImitator.findMessageByType(ConverterUpdateMsg.class);
        Assert.assertTrue(newConverterUpdateMsgOpt.isPresent());
        ConverterUpdateMsg converterUpdateMsg = newConverterUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, converterUpdateMsg.getMsgType());
        Assert.assertEquals(savedConverter.getUuidId().getMostSignificantBits(), converterUpdateMsg.getIdMSB());
        Assert.assertEquals(savedConverter.getUuidId().getLeastSignificantBits(), converterUpdateMsg.getIdLSB());
        Assert.assertEquals(savedConverter.getName(), converterUpdateMsg.getName());
    }

    private void validateAddingAndUpdateOfEdgeAttribute() throws Exception {
        edgeImitator.expectMessageAmount(3);
        JsonNode httpsBaseUrlAttribute = JacksonUtil.toJsonNode("{\"baseUrl\": \"https://localhost\"}");
        doPost("/api/plugins/telemetry/" + EntityType.EDGE.name() + "/" + edge.getId() + "/SERVER_SCOPE", httpsBaseUrlAttribute)
                .andExpect(status().isOk());

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertTrue(integrationUpdateMsg.getConfiguration().contains("https://localhost/api/v1"));

        Assert.assertEquals(2, edgeImitator.findAllMessagesByType(ConverterUpdateMsg.class).size());

        edgeImitator.expectMessageAmount(3);
        JsonNode deviceHWUrlAttribute = JacksonUtil.toJsonNode("{\"deviceHW\": \"PCM-2230\"}");
        doPost("/api/plugins/telemetry/" + EntityType.EDGE.name() + "/" + edge.getId() + "/SERVER_SCOPE", deviceHWUrlAttribute)
                .andExpect(status().isOk());

        Assert.assertTrue(edgeImitator.waitForMessages());

        integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertTrue(integrationUpdateMsg.getConfiguration().contains("https://localhost/api/v1"));
        Assert.assertTrue(integrationUpdateMsg.getConfiguration().contains("PCM-2230"));

        Assert.assertEquals(2, edgeImitator.findAllMessagesByType(ConverterUpdateMsg.class).size());
    }

    private void validateIntegrationAssignToEdge(Integration savedIntegration, Converter savedConverter) throws Exception {
        edgeImitator.expectMessageAmount(2);

        doPost("/api/edge/" + edge.getUuidId()
                + "/integration/" + savedIntegration.getUuidId(), Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertEquals(savedIntegration.getUuidId().getMostSignificantBits(), integrationUpdateMsg.getIdMSB());
        Assert.assertEquals(savedIntegration.getUuidId().getLeastSignificantBits(), integrationUpdateMsg.getIdLSB());
        Assert.assertEquals(savedIntegration.getName(), integrationUpdateMsg.getName());
        Assert.assertTrue(integrationUpdateMsg.getConfiguration().contains("http://localhost:18080"));

        Optional<ConverterUpdateMsg> converterUpdateMsgOpt = edgeImitator.findMessageByType(ConverterUpdateMsg.class);
        Assert.assertTrue(converterUpdateMsgOpt.isPresent());
        ConverterUpdateMsg converterUpdateMsg = converterUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, converterUpdateMsg.getMsgType());
        Assert.assertEquals(savedConverter.getUuidId().getMostSignificantBits(), converterUpdateMsg.getIdMSB());
        Assert.assertEquals(savedConverter.getUuidId().getLeastSignificantBits(), converterUpdateMsg.getIdLSB());
        Assert.assertEquals(savedConverter.getName(), converterUpdateMsg.getName());
    }

    private void validateIntegrationConfigurationUpdate(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(2);

        ObjectNode updatedIntegrationConfig = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        updatedIntegrationConfig.putObject("metadata")
                .put("baseUrl", "${{baseUrl}}/api/v1")
                .put("deviceHW", "${{deviceHW}}");
        savedIntegration.setConfiguration(updatedIntegrationConfig);
        doPost("/api/integration", savedIntegration, Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertTrue(integrationUpdateMsg.getConfiguration().contains("http://localhost:18080/api/v1"));
    }

    private void validateIntegrationDefaultConverterUpdate(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(2);

        ObjectNode newConverterConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode()
                .put("decoder", "return {deviceName: 'Device B', deviceType: 'default'};");
        Converter converter = new Converter();
        converter.setName("My new converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(newConverterConfiguration);
        converter.setEdgeTemplate(true);
        Converter newSavedConverter = doPost("/api/converter", converter, Converter.class);

        savedIntegration.setDefaultConverterId(newSavedConverter.getId());
        doPost("/api/integration", savedIntegration, Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertEquals(savedIntegration.getUuidId().getMostSignificantBits(), integrationUpdateMsg.getIdMSB());
        Assert.assertEquals(savedIntegration.getUuidId().getLeastSignificantBits(), integrationUpdateMsg.getIdLSB());
        Assert.assertEquals(savedIntegration.getName(), integrationUpdateMsg.getName());

        Optional<ConverterUpdateMsg> newConverterUpdateMsgOpt = edgeImitator.findMessageByType(ConverterUpdateMsg.class);
        Assert.assertTrue(newConverterUpdateMsgOpt.isPresent());
        ConverterUpdateMsg converterUpdateMsg = newConverterUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, converterUpdateMsg.getMsgType());
        Assert.assertEquals(newSavedConverter.getUuidId().getMostSignificantBits(), converterUpdateMsg.getIdMSB());
        Assert.assertEquals(newSavedConverter.getUuidId().getLeastSignificantBits(), converterUpdateMsg.getIdLSB());
        Assert.assertEquals(newSavedConverter.getName(), converterUpdateMsg.getName());
    }

    private void validateIntegrationDownlinkConverterUpdate(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(3);

        ObjectNode downlinkConverterConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode()
                .put("encoder", "return {contentType: 'JSON', data: '\"{\"pin\": 1}\"'};");
        Converter downlinkConverter = new Converter();
        downlinkConverter.setName("My downlink converter");
        downlinkConverter.setType(ConverterType.DOWNLINK);
        downlinkConverter.setConfiguration(downlinkConverterConfiguration);
        downlinkConverter.setEdgeTemplate(true);
        Converter savedDownlinkConverter = doPost("/api/converter", downlinkConverter, Converter.class);

        savedIntegration.setDownlinkConverterId(savedDownlinkConverter.getId());
        doPost("/api/integration", savedIntegration, Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertEquals(savedIntegration.getUuidId().getMostSignificantBits(), integrationUpdateMsg.getIdMSB());
        Assert.assertEquals(savedIntegration.getUuidId().getLeastSignificantBits(), integrationUpdateMsg.getIdLSB());
        Assert.assertEquals(savedIntegration.getName(), integrationUpdateMsg.getName());

        List<ConverterUpdateMsg> downlinkConverterUpdateMsgs = edgeImitator.findAllMessagesByType(ConverterUpdateMsg.class);

        ConverterUpdateMsg downlinkConverterUpdateMsg = null;
        for (ConverterUpdateMsg converterUpdateMsg : downlinkConverterUpdateMsgs) {
            if (savedDownlinkConverter.getName().equals(converterUpdateMsg.getName())) {
                downlinkConverterUpdateMsg = converterUpdateMsg;
            }
        }
        Assert.assertNotNull(downlinkConverterUpdateMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, downlinkConverterUpdateMsg.getMsgType());
        Assert.assertEquals(savedDownlinkConverter.getUuidId().getMostSignificantBits(), downlinkConverterUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDownlinkConverter.getUuidId().getLeastSignificantBits(), downlinkConverterUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDownlinkConverter.getName(), downlinkConverterUpdateMsg.getName());

        edgeImitator.expectMessageAmount(1);

        downlinkConverterConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode()
                .put("encoder", "return {contentType: 'JSON', data: '\"{\"pin\": 3}\"'};");
        savedDownlinkConverter.setConfiguration(downlinkConverterConfiguration);
        doPost("/api/converter", savedDownlinkConverter, Converter.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<ConverterUpdateMsg> downlinkConverterUpdateMsgOpt = edgeImitator.findMessageByType(ConverterUpdateMsg.class);
        Assert.assertTrue(downlinkConverterUpdateMsgOpt.isPresent());
        downlinkConverterUpdateMsg = downlinkConverterUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, downlinkConverterUpdateMsg.getMsgType());
        Assert.assertEquals(savedDownlinkConverter.getUuidId().getMostSignificantBits(), downlinkConverterUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDownlinkConverter.getUuidId().getLeastSignificantBits(), downlinkConverterUpdateMsg.getIdLSB());
        Assert.assertEquals(JacksonUtil.OBJECT_MAPPER.writeValueAsString(downlinkConverterConfiguration), downlinkConverterUpdateMsg.getConfiguration());
    }

    private void validateIntegrationUnassignFromEdge(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(1);

        doDelete("/api/edge/" + edge.getUuidId()
                + "/integration/" + savedIntegration.getUuidId(), Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertEquals(savedIntegration.getUuidId().getMostSignificantBits(), integrationUpdateMsg.getIdMSB());
        Assert.assertEquals(savedIntegration.getUuidId().getLeastSignificantBits(), integrationUpdateMsg.getIdLSB());
    }

    private void validateRemoveOfIntegration(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/integration/" + savedIntegration.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));
    }
}
