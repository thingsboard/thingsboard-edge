/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.mqtt.TbMqttNode;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL",
        "integrations.converters.library.enabled=true"
})
@Slf4j
@DaoSqlTest
public class SecretControllerTest extends AbstractControllerTest {

    private static final TypeReference<PageData<SecretInfo>> PAGE_DATA_SECRET_TYPE_REF = new TypeReference<>() {};

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void tearDown() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(100, 0));
        for (SecretInfo secretInfo : pageData.getData()) {
            doDelete("/api/secret/" + secretInfo.getId().getId()).andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveSecret() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Create Secret", "CreatePassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        PageData<SecretInfo> pageData2 = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData2.getData()).hasSize(1);
        assertThat(pageData2.getData().get(0)).isEqualTo(new SecretInfo(savedSecret));

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecret() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Update Secret", "UpdatePassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // update secret value
        secret = new Secret(savedSecret);
        secret.setValue("UpdatedPassword");

        SecretInfo updatedSecret = doPost("/api/secret", secret, SecretInfo.class);
        retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, updatedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(updatedSecret);

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteSecret_whenUsedInRuleNodeWithHasSecrets_thenReceiveDeleteResultError() throws Exception {
        String secretName = "MqttNodeSecret";
        Secret secret = constructSecret(secretName, "Password");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // create rule node with secret usage that uses 'hasSecrets' annotation:
        var ruleChain = createRuleChain("Rule Chain test mqtt", TbMqttNode.class.getName(), secretName);

        // delete secret
        String responseBody = doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        JsonNode node = JacksonUtil.toJsonNode(responseBody);
        assertFalse(node.get("success").asBoolean());

        JsonNode references = node.get("references");
        assertNotNull(references.get("RULE_CHAIN"));
        assertFalse(references.get("RULE_CHAIN").isEmpty());

        doDelete("/api/ruleChain/" + ruleChain.getId().toString()).andExpect(status().isOk());
        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteSecret_whenUsedInRuleNodeWithoutHasSecrets_thenReceiveDeleteResultSuccess() throws Exception {
        String secretName = "GetAttrNode";
        Secret secret = constructSecret(secretName, "Password");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // create rule node with secret usage that uses 'hasSecrets' annotation:
        var ruleChain = createRuleChain("Rule Chain test attr", TbGetAttributesNode.class.getName(), secretName);

        // delete secret
        doDelete("/api/ruleChain/" + ruleChain.getId().toString()).andExpect(status().isOk());
        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteSecretUsedInIntegration_thenReceiveDeleteResultError() throws Exception {
        String secretName = "MqttSecret";
        String placeholder = toSecretPlaceholder(secretName, SecretType.TEXT);

        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret(secretName, "Password");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        ConverterId converterId = createConverter();
        IntegrationId integrationId = createIntegration(converterId, placeholder);

        String responseBody = doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        JsonNode node = JacksonUtil.toJsonNode(responseBody);
        assertFalse(node.get("success").asBoolean());

        JsonNode references = node.get("references");
        assertNotNull(references.get("INTEGRATION"));
        assertFalse(references.get("INTEGRATION").isEmpty());

        doDelete("/api/integration/" + integrationId.getId().toString()).andExpect(status().isOk());
        doDelete("/api/converter/" + converterId.getId().toString()).andExpect(status().isOk());
        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecretUsedInIntegration_thenReceiveLifecycleEvent() throws Exception {
        String secretName = "MqttSecret";
        String placeholder = toSecretPlaceholder(secretName, SecretType.TEXT);

        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret(secretName, "Password");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        ConverterId converterId = createConverter();
        IntegrationId integrationId = createIntegration(converterId, placeholder);

        secret = new Secret(savedSecret);
        secret.setValue("UpdatedPassword");

        // broadcast update event for integration with secret on secret update
        testBroadcastEntityStateChangeEventTime(integrationId, tenantId, 1);

        SecretInfo updatedSecret = doPost("/api/secret", secret, SecretInfo.class);
        retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, updatedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(updatedSecret);

        doDelete("/api/integration/" + integrationId.getId().toString()).andExpect(status().isOk());
        doDelete("/api/converter/" + converterId.getId().toString()).andExpect(status().isOk());
        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecretNameProhibited() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Secret", "Prohibited");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        secret = new Secret(savedSecret);
        secret.setName("Updated Name");

        doPost("/api/secret", secret)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Can't update secret name!")));

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testFindSecretInfos() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        int expectedSize = 10;
        String namePrefix = "Test Create Secret_";
        for (int i = 0; i < expectedSize; i++) {
            doPost("/api/secret", constructSecret(namePrefix + i, "CreatePassword"), SecretInfo.class);
        }

        PageData<SecretInfo> pageData2 = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(expectedSize, 0));
        assertThat(pageData2.getData()).hasSize(expectedSize);

        List<UUID> toDelete = new ArrayList<>();

        for (int i = 0; i < expectedSize; i++) {
            SecretInfo secretInfo = pageData2.getData().get(i);
            assertThat(secretInfo.getName()).isEqualTo(namePrefix + i);
            toDelete.add(secretInfo.getUuidId());
        }

        toDelete.forEach(secret -> {
            try {
                doDelete("/api/secret/" + secret).andExpect(status().isOk());
                doGet("/api/secret/{id}/info", secret).andExpect(status().isNotFound());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Secret constructSecret(String name, String value) {
        Secret secret = new Secret();
        secret.setName(name);
        secret.setValue(value);
        secret.setType(SecretType.TEXT);
        return secret;
    }

    private ConverterId createConverter() {
        JsonNode converterConfiguration = JacksonUtil.newObjectNode().put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(converterConfiguration);
        converter = doPost("/api/converter", converter, Converter.class);
        return converter.getId();
    }

    private IntegrationId createIntegration(ConverterId converterId, String value) {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(converterId);
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(JacksonUtil.newObjectNode().putObject("metadata").put("key1", value));
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        Assert.assertNotNull(savedIntegration);
        Assert.assertNotNull(savedIntegration.getId());
        assertTrue(savedIntegration.getCreatedTime() > 0);
        return savedIntegration.getId();
    }

    private RuleChainId createRuleChain(String name, String clazz, String secretName) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        var result = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(result.getId());
        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("Test");
        ruleNode.setType(clazz);
        TbMqttNodeConfiguration config = new TbMqttNodeConfiguration();
        config.setHost(toSecretPlaceholder(secretName, SecretType.TEXT));
        ruleNode.setConfiguration(JacksonUtil.valueToTree(config));
        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
        return result.getId();
    }

    private String toSecretPlaceholder(String name, SecretType type) {
        return String.format("${secret:%s;type:%s}", name, type);
    }

}
