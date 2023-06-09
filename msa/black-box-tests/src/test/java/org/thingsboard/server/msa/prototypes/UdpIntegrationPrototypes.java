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
package org.thingsboard.server.msa.prototypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;

public class UdpIntegrationPrototypes {

    private static final String JSON_INTEGRATION_CONFIG = "{\"clientConfiguration\":{" +
            "\"port\":%d," +
            "\"soBroadcast\":true," +
            "\"soRcvBuf\":64," +
            "\"cacheSize\":1000," +
            "\"timeToLiveInMinutes\":1440," +
            "\"handlerConfiguration\":{\"handlerType\":\"JSON\"}},\"metadata\":{}}";

    private static final String TEXT_INTEGRATION_CONFIG = "{\n" +
            "  \"metadata\": {},\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"port\": 11560,\n" +
            "    \"soBroadcast\": true,\n" +
            "    \"soRcvBuf\": 64,\n" +
            "    \"cacheSize\": 1000,\n" +
            "    \"timeToLiveInMinutes\": 1440,\n" +
            "    \"handlerConfiguration\": {\n" +
            "      \"handlerType\": \"TEXT\",\n" +
            "      \"charsetName\": \"UTF-8\",\n" +
            "      \"maxFrameLength\": 128\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final String BINARY_INTEGRATION_CONFIG = "{\n" +
            "  \"metadata\": {},\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"port\": 11560,\n" +
            "    \"soBroadcast\": true,\n" +
            "    \"soRcvBuf\": 64,\n" +
            "    \"cacheSize\": 1000,\n" +
            "    \"timeToLiveInMinutes\": 1440,\n" +
            "    \"handlerConfiguration\": {\n" +
            "      \"handlerType\": \"BINARY\",\n" +
            "      \"charsetName\": \"UTF-8\",\n" +
            "      \"maxFrameLength\": 128\n" +
            "    }\n" +
            "  }\n" +
            "}";

    public static JsonNode defaultJsonConfig(int port){
        return JacksonUtil.toJsonNode(String.format(JSON_INTEGRATION_CONFIG, port));
    }

    public static JsonNode defaultTextConfig(int port){
        return JacksonUtil.toJsonNode(String.format(TEXT_INTEGRATION_CONFIG, port));
    }

    public static JsonNode defaultBinaryConfig(int port){
        return JacksonUtil.toJsonNode(String.format(BINARY_INTEGRATION_CONFIG, port));
    }
}
