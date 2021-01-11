/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.mqtt.aws;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.mqtt.MqttClientConfiguration;
import org.thingsboard.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.integration.mqtt.credentials.CertPemClientCredentials;
import org.thingsboard.integration.mqtt.credentials.MqttClientCredentials;

@Slf4j
public class AwsIotIntegration extends BasicMqttIntegration {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
    }

    @Override
    protected void setupConfiguration(MqttClientConfiguration mqttClientConfiguration) {
        mqttClientConfiguration.setPort(8883);
        mqttClientConfiguration.setCleanSession(true);
        MqttClientCredentials credentials = mqttClientConfiguration.getCredentials();
        if (credentials == null || !(credentials instanceof CertPemClientCredentials)) {
            throw new RuntimeException("Can't setup AWS IoT integration without AWS IoT Certificates!");
        }
        CertPemClientCredentials certPemClientCredentials = (CertPemClientCredentials)credentials;
        if (StringUtils.isEmpty(certPemClientCredentials.getCaCert()) ||
            StringUtils.isEmpty(certPemClientCredentials.getCert()) ||
            StringUtils.isEmpty(certPemClientCredentials.getPrivateKey())) {
            throw new RuntimeException("Can't setup AWS IoT integration. Required AWS IoT Certificates or Private Key is missing!");
        }
    }

}
