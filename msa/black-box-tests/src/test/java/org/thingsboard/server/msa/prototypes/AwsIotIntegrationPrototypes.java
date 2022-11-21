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

public class AwsIotIntegrationPrototypes {
    private static final String CONFIG_INTEGRATION = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"%s\",\n" +
            "    \"port\": 8883,\n" +
            "    \"clientId\": \"\",\n" +
            "    \"connectTimeoutSec\": 10,\n" +
            "    \"ssl\": true,\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"cert.PEM\",\n" +
            "      \"caCertFileName\": \"rootCA.pem\",\n" +
            "      \"caCert\": \"%s\",\n" +
            "      \"certFileName\": \"cert.crt\",\n" +
            "      \"cert\": \"%s\",\n" +
            "      \"privateKeyFileName\": \"private.key\",\n" +
            "      \"privateKey\": \"%s\",\n" +
            "      \"password\": \"\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"downlinkTopicPattern\": \"${topic}\",\n" +
            "  \"topicFilters\": [\n" +
            "    {\n" +
            "      \"filter\": \"sensors/+/temperature\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";

    public static JsonNode defaultConfig(String endpoint, String caCert, String cert, String privateKey){
        return JacksonUtil.toJsonNode(String.format(CONFIG_INTEGRATION, endpoint, caCert, cert, privateKey));
    }
}
