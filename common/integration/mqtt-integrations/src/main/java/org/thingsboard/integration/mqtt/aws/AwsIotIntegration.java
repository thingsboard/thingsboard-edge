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
package org.thingsboard.integration.mqtt.aws;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
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
