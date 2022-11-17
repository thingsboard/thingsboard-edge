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
package org.thingsboard.server.msa.prototypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;

public class MQTTIntegrationPrototypes {
    private static final String CONFIG_INTEGRATION = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"%s\",\n" +
            "    \"port\":%d ,\n" +
            "    \"cleanSession\": true,\n" +
            "    \"ssl\": false,\n" +
            "    \"connectTimeoutSec\": 30,\n" +
            "    \"clientId\": \"\",\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"anonymous\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"downlinkTopicPattern\": \"%s\",\n" +
            "  \"topicFilters\": [\n" +
            "    {\n" +
            "      \"filter\": \"tb/mqtt/device\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";

    private static final String CONFIG_INTEGRATION_WITH_BASIC_CREDS = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"%s\",\n" +
            "    \"port\":%d ,\n" +
            "    \"cleanSession\": true,\n" +
            "    \"ssl\": false,\n" +
            "    \"connectTimeoutSec\": 30,\n" +
            "    \"clientId\": \"\",\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"basic\",\n" +
            "      \"username\": \"username\",\n" +
            "      \"password\": \"pass\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"downlinkTopicPattern\": \"%s\",\n" +
            "  \"topicFilters\": [\n" +
            "    {\n" +
            "      \"filter\": \"tb/mqtt/device\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";

    private static final String CONFIG_INTEGRATION_WITH_PEM_CREDS = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"%s\",\n" +
            "    \"port\":%d ,\n" +
            "    \"cleanSession\": true,\n" +
            "    \"ssl\": false,\n" +
            "    \"connectTimeoutSec\": 30,\n" +
            "    \"clientId\": \"\",\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"cert.PEM\",\n" +
            "      \"caCert\": \"%s\",\n" +
            "      \"caCertFileName\": \"caCertFileName.pem\",\n" +
            "      \"cert\": \"%s\",\n" +
            "      \"certFileName\": \"certFileName.pem\",\n" +
            "      \"privateKey\": \"%s\",\n" +
            "      \"privateKeyFileName\": \"privateKeyFileName.pem\",\n" +
            "      \"privateKeyPassword\": \"12345\",\n" +
            "    }\n" +
            "  },\n" +
            "  \"downlinkTopicPattern\": \"%s\",\n" +
            "  \"topicFilters\": [\n" +
            "    {\n" +
            "      \"filter\": \"tb/mqtt/device\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";

    public static JsonNode defaultConfig(String serviceName, int servicePort, String topic){
        return JacksonUtil.toJsonNode(String.format(CONFIG_INTEGRATION, serviceName, servicePort, topic));
    }

    public static JsonNode configWithBasicCreds(String serviceName, int servicePort, String topic){
        return JacksonUtil.toJsonNode(String.format(CONFIG_INTEGRATION_WITH_BASIC_CREDS, serviceName, servicePort, topic));
    }

    public static JsonNode configWithPemCreds(String serviceName, int servicePort, String topic, String pem){
        return JacksonUtil.toJsonNode(String.format(CONFIG_INTEGRATION_WITH_PEM_CREDS, serviceName, servicePort, topic, pem ,pem, pem));
    }
}
