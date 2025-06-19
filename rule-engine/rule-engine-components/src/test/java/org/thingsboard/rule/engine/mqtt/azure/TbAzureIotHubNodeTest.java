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
package org.thingsboard.rule.engine.mqtt.azure;

import io.netty.handler.codec.mqtt.MqttVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.credentials.CertPemCredentials;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;
import org.thingsboard.server.common.data.StringUtils;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
public class TbAzureIotHubNodeTest extends AbstractRuleNodeUpgradeTest {

    private TbAzureIotHubNode azureIotHubNode;
    private TbAzureIotHubNodeConfiguration azureIotHubNodeConfig;

    @Mock
    protected TbContext ctxMock;
    @Mock
    protected MqttClient mqttClientMock;

    @BeforeEach
    public void setUp() {
        azureIotHubNode = spy(new TbAzureIotHubNode());
        azureIotHubNodeConfig = new TbAzureIotHubNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(azureIotHubNodeConfig.getTopicPattern()).isEqualTo("devices/<device_id>/messages/events/");
        assertThat(azureIotHubNodeConfig.getHost()).isEqualTo("<iot-hub-name>.azure-devices.net");
        assertThat(azureIotHubNodeConfig.getPort()).isEqualTo(8883);
        assertThat(azureIotHubNodeConfig.getConnectTimeoutSec()).isEqualTo(10);
        assertThat(azureIotHubNodeConfig.getClientId()).isNull();
        assertThat(azureIotHubNodeConfig.isAppendClientIdSuffix()).isFalse();
        assertThat(azureIotHubNodeConfig.isRetainedMessage()).isFalse();
        assertThat(azureIotHubNodeConfig.isCleanSession()).isTrue();
        assertThat(azureIotHubNodeConfig.isSsl()).isTrue();
        assertThat(azureIotHubNodeConfig.isParseToPlainText()).isFalse();
        assertThat(azureIotHubNodeConfig.getProtocolVersion()).isEqualTo(MqttVersion.MQTT_3_1_1);
        assertThat(azureIotHubNodeConfig.getCredentials()).isInstanceOf(AzureIotHubSasCredentials.class);
    }

    @Test
    public void verifyPrepareMqttClientConfigMethodWithAzureIotHubSasCredentials() throws Exception {
        AzureIotHubSasCredentials credentials = new AzureIotHubSasCredentials();
        credentials.setSasKey("testSasKey");
        credentials.setCaCert("test-ca-cert.pem");
        azureIotHubNodeConfig.setCredentials(credentials);

        willReturn(mqttClientMock).given(azureIotHubNode).initAzureClient(any());
        azureIotHubNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(azureIotHubNodeConfig)));

        MqttClientConfig mqttClientConfig = new MqttClientConfig();
        azureIotHubNode.prepareMqttClientConfig(mqttClientConfig);

        assertThat(mqttClientConfig.getUsername()).isEqualTo(AzureIotHubUtil.buildUsername(azureIotHubNodeConfig.getHost(), mqttClientConfig.getClientId()));
        assertThat(StringUtils.substringBefore(mqttClientConfig.getPassword(), "&sig=")) // not verifying the signature part because it is time-dependent
                .isEqualTo(StringUtils.substringBefore(AzureIotHubUtil.buildSasToken(azureIotHubNodeConfig.getHost(), credentials.getSasKey()), "&sig="));
    }

    @Test
    public void givenPemCredentialsAndSuccessfulConnectResult_whenInit_thenOk() throws Exception {
        CertPemCredentials credentials = new CertPemCredentials();
        credentials.setCaCert("test-ca-cert.pem");
        credentials.setPassword("test-password");
        azureIotHubNodeConfig.setCredentials(credentials);

        willReturn(mqttClientMock).given(azureIotHubNode).initAzureClient(any());

        assertThatNoException().isThrownBy(
                () -> azureIotHubNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(azureIotHubNodeConfig))));

        var mqttNodeConfiguration = (TbMqttNodeConfiguration) ReflectionTestUtils.getField(azureIotHubNode, "mqttNodeConfiguration");
        assertThat(mqttNodeConfiguration).isNotNull();
        assertThat(mqttNodeConfiguration.getPort()).isEqualTo(8883);
        assertThat(mqttNodeConfiguration.isCleanSession()).isTrue();
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"topicPattern\":\"devices/<device_id>/messages/events/\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"sas\",\"sasKey\":\"sasKey\",\"caCert\":null,\"caCertFileName\":null}}}",
                        true,
                        "{\"topicPattern\":\"devices/<device_id>/messages/events/\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"sas\",\"sasKey\":\"sasKey\",\"caCert\":null,\"caCertFileName\":null}, \"protocolVersion\":\"MQTT_3_1_1\"}\"}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"topicPattern\":\"devices/<device_id>/messages/events/\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"sas\",\"sasKey\":\"sasKey\",\"caCert\":null,\"caCertFileName\":null}, \"protocolVersion\":\"MQTT_3_1_1\"}\"}",
                        false,
                        "{\"topicPattern\":\"devices/<device_id>/messages/events/\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"sas\",\"sasKey\":\"sasKey\",\"caCert\":null,\"caCertFileName\":null}, \"protocolVersion\":\"MQTT_3_1_1\"}\"}")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return azureIotHubNode;
    }

}
