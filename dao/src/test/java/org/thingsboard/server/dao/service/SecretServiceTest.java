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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.SecretUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.secret.SecretUtilService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DaoSqlTest
public class SecretServiceTest extends AbstractServiceTest {

    @Autowired
    SecretService secretService;

    @Autowired
    RuleChainService ruleChainService;

    @Autowired
    ConverterService converterService;

    @Autowired
    IntegrationService integrationService;

    @Autowired
    TenantProfileService tenantProfileService;

    @Autowired
    EventService eventService;

    @MockBean
    SecretUtilService secretUtilService;

    @MockBean
    SecretConfigurationService secretConfigurationService;

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantProfileService.deleteTenantProfiles(tenantId);
    }

    @Test
    public void testSaveSecret() {
        String password = "Password";
        Secret secret = constructSecret(tenantId, "Test Secret", password);
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // check secret info
        SecretInfo retrievedInfo = secretService.findSecretInfoById(tenantId, savedSecret.getId());
        assertThat(retrievedInfo).isEqualTo(new SecretInfo(savedSecret));

        // update encrypted value
        savedSecret.setValue("NewPassword");
        savedSecret = secretService.saveSecret(tenantId, savedSecret);
        retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // delete secret
        secretService.deleteSecret(tenantId, savedSecret, false);
        assertThat(secretService.findSecretById(tenantId, savedSecret.getId())).isNull();
    }

    @Test
    public void testUpdateSecretInfoDescription_thenValueShouldFetchedFromOldSecret() {
        String password = "Password";
        when(secretUtilService.encrypt(any(), any(), any())).thenReturn(password.getBytes(StandardCharsets.UTF_8));

        Secret secret = constructSecret(tenantId, "Test Secret", password);
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // check secret info
        SecretInfo retrievedInfo = secretService.findSecretInfoById(tenantId, savedSecret.getId());
        assertThat(retrievedInfo).isEqualTo(new SecretInfo(savedSecret));

        // update description, value should be equal
        retrievedInfo.setDescription("New description for secret");
        Secret updatedSecret = secretService.saveSecret(tenantId, new Secret(retrievedInfo));
        assertThat(savedSecret.getValue()).isEqualTo(updatedSecret.getValue());

        // delete secret
        secretService.deleteSecret(tenantId, savedSecret, false);
        assertThat(secretService.findSecretById(tenantId, savedSecret.getId())).isNull();
    }

    @Test
    public void testUpdateSecretName_thenReceiveDataValidationException() {
        Secret secret = constructSecret(tenantId, "Test Validation Exception", "Validation");
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        savedSecret.setName("Updated Validation Exception");

        Assertions.assertThrows(DataValidationException.class, () -> secretService.saveSecret(tenantId, savedSecret));
    }

    @Test
    public void testFindSecretByName() {
        String name = "Test Secret Password";
        Secret secret = constructSecret(tenantId, name, "test");
        Secret savedSecret = secretService.saveSecret(tenantId, secret);
        assertThat(savedSecret.getName()).isEqualTo(name);

        Secret retrieved = secretService.findSecretByName(tenantId, name);
        assertThat(savedSecret).isEqualTo(retrieved);

        secretService.deleteSecret(tenantId, savedSecret, false);
    }

    @Test
    public void testGetTenantSecrets() {
        List<Secret> secrets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Secret savedSecret = secretService.saveSecret(tenantId, constructSecret(tenantId, "Name_" + i, "Password"));
            secrets.add(savedSecret);
        }
        PageData<SecretInfo> retrieved = secretService.findSecretInfosByTenantId(tenantId, new PageLink(10, 0));
        List<SecretInfo> secretInfos = secrets.stream().map(SecretInfo::new).toList();
        assertThat(retrieved.getData()).containsOnlyOnceElementsOf(secretInfos);

        secrets.forEach(secret -> secretService.deleteSecret(tenantId, secret, false));
        retrieved = secretService.findSecretInfosByTenantId(tenantId, new PageLink(10, 0));
        assertThat(retrieved.getData().size()).isEqualTo(0);
    }

    @Test
    public void testDeleteSecret_whenUsedInRuleNodeWithHasSecrets_thenReceiveDataValidationException() {
        String secretName = "MqttNodeSecret";
        Secret secret = constructSecret(tenantId, secretName, "Password");
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // create rule node with secret usage that uses 'hasSecrets' annotation:
        createRuleNode("org.thingsboard.rule.engine.mqtt.TbMqttNode", secretName, savedSecret.getType());

        // delete secret
        TbSecretDeleteResult result = secretService.deleteSecret(tenantId, savedSecret, false);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReferences()).containsKey(EntityType.RULE_CHAIN.name());
        assertThat(result.getReferences().get(EntityType.RULE_CHAIN.name())).isNotEmpty();
        assertThat(result.getReferences().get(EntityType.RULE_CHAIN.name()).get(0)).isInstanceOf(RuleChain.class);
    }

    @Test
    public void testDeleteSecret_whenUsedInRuleNodeWithoutHasSecrets_thenReceiveFailureDeleteResult() {
        String secretName = "MqttNodeSecret";
        Secret secret = constructSecret(tenantId, secretName, "Password");
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // create rule node with secret usage that DO NOT use 'hasSecrets' annotation:
        createRuleNode("org.thingsboard.rule.engine.edge.TbMsgPushToEdgeNode", secretName, savedSecret.getType());

        // delete secret
        secretService.deleteSecret(tenantId, savedSecret, false);
    }

    @Test
    public void testDeleteSecret_whenUsedInIntegration_thenReceiveFailureDeleteResult() {
        String secretName = "IntegrationSecret";
        Secret secret = constructSecret(tenantId, secretName, "Password");
        Secret savedSecret = secretService.saveSecret(tenantId, secret);

        Secret retrievedSecret = secretService.findSecretById(tenantId, savedSecret.getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // create converter and integration with secret usage in configuration:
        ObjectNode configuration = JacksonUtil.newObjectNode().putObject("metadata").put("password", SecretUtil.toSecretPlaceholder(secretName, secret.getType()));
        String integrationName = "My integration";
        createIntegration(integrationName, configuration, createAndGetConvertedId());

        // delete secret
        TbSecretDeleteResult result = secretService.deleteSecret(tenantId, savedSecret, false);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReferences()).containsKey(EntityType.INTEGRATION.name());
        assertThat(result.getReferences().get(EntityType.INTEGRATION.name())).isNotEmpty();
        assertThat(result.getReferences().get(EntityType.INTEGRATION.name()).get(0)).isInstanceOf(Integration.class);

        Integration integration = (Integration) result.getReferences().get(EntityType.INTEGRATION.name()).get(0);
        assertThat(integration.getName()).isEqualTo(integrationName);
        assertThat(integration.getConfiguration().toString()).contains(SecretUtil.toSecretPlaceholder(secretName, savedSecret.getType()));
    }

    private Secret constructSecret(TenantId tenantId, String name, String value) {
        Secret secret = new Secret();
        secret.setTenantId(tenantId);
        secret.setName(name);
        secret.setType(SecretType.TEXT);
        secret.setValue(value);
        return secret;
    }

    private Integration createIntegration(String name, JsonNode configuration, ConverterId converterId) {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName(name);
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.MQTT);
        integration.setConfiguration(configuration);
        return integrationService.saveIntegration(integration);
    }

    private ConverterId createAndGetConvertedId() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(JacksonUtil.newObjectNode().put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};"));
        return converterService.saveConverter(converter).getId();
    }

    private JsonNode createRuleNode(String ruleNodeType, String secretName, SecretType secretType) {
        // create rule node with secret usage:
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("Rule Chain #1");
        ruleChain.setType(RuleChainType.CORE);
        ruleChain = ruleChainService.saveRuleChain(ruleChain);

        // rule node that uses 'hasSecrets' annotation
        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("Input rule node 1");
        ruleNode.setType(ruleNodeType);
        var configuration = JacksonUtil.newObjectNode();
        configuration.put("ruleChainId", ruleChain.getUuidId().toString());
        configuration.put("password", SecretUtil.toSecretPlaceholder(secretName, secretType));
        ruleNode.setRuleChainId(ruleChain.getId());
        ruleNode.setConfiguration(configuration);

        ruleChainService.saveRuleNode(tenantId, ruleNode);
        return configuration;
    }

}
