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

public class HttpIntegrationConfigPrototypes {

    private static final String CONFIG = " {\"baseUrl\":\"%s\"," +
            "\"replaceNoContentToOk\":true," +
            "\"enableSecurity\":false," +
            "\"downlinkUrl\":\"https://api.thingpark.com/thingpark/lrc/rest/downlink\"," +
            "\"loriotDownlinkUrl\":\"https://eu1.loriot.io/1/rest\"," +
            "\"createLoriotOutput\":false," +
            "\"sendDownlink\":false," +
            "\"server\":\"eu1\"," +
            "\"appId\":\"\"," +
            "\"enableSecurityNew\":false," +
            "\"asId\":\"\"," +
            "\"asIdNew\":\"\"," +
            "\"asKey\":\"\"," +
            "\"clientIdNew\":\"\"," +
            "\"clientSecret\":\"\"," +
            "\"maxTimeDiffInSeconds\":60," +
            "\"httpEndpoint\":\"\"," +
            "\"headersFilter\":{}," +
            "\"token\":\"\"," +
            "\"credentials\":{\"type\":\"basic\",\"email\":\"\",\"password\":\"\",\"token\":\"\"}," +
            "\"metadata\":{}}";

    private static final String CONFIG_SECURITY_ENABLED = " {\"baseUrl\":\"%s\"," +
            "\"replaceNoContentToOk\":true," +
            "\"enableSecurity\":true," +
            "\"downlinkUrl\":\"https://api.thingpark.com/thingpark/lrc/rest/downlink\"," +
            "\"loriotDownlinkUrl\":\"https://eu1.loriot.io/1/rest\"," +
            "\"createLoriotOutput\":false," +
            "\"sendDownlink\":false," +
            "\"server\":\"eu1\"," +
            "\"appId\":\"\"," +
            "\"enableSecurityNew\":false," +
            "\"asId\":\"\"," +
            "\"asIdNew\":\"\"," +
            "\"asKey\":\"\"," +
            "\"clientIdNew\":\"\"," +
            "\"clientSecret\":\"\"," +
            "\"maxTimeDiffInSeconds\":60," +
            "\"httpEndpoint\":\"\"," +
            "\"headersFilter\":{}," +
            "\"token\":\"\"," +
            "\"credentials\":{\"type\":\"basic\",\"email\":\"\",\"password\":\"\",\"token\":\"\"}," +
            "\"metadata\":{}}";

    private static final String CONFIG_SECURITY_ENABLED_WITH_TEST_HEADER = " {\"baseUrl\":\"%s\"," +
            "\"replaceNoContentToOk\":true," +
            "\"enableSecurity\":true," +
            "\"headersFilter\": {\"testHeader\": \"testValue\"}," +
            "\"downlinkUrl\":\"https://api.thingpark.com/thingpark/lrc/rest/downlink\"," +
            "\"loriotDownlinkUrl\":\"https://eu1.loriot.io/1/rest\"," +
            "\"createLoriotOutput\":false," +
            "\"sendDownlink\":false," +
            "\"server\":\"eu1\"," +
            "\"appId\":\"\"," +
            "\"enableSecurityNew\":false," +
            "\"asId\":\"\"," +
            "\"asIdNew\":\"\"," +
            "\"asKey\":\"\"," +
            "\"clientIdNew\":\"\"," +
            "\"clientSecret\":\"\"," +
            "\"maxTimeDiffInSeconds\":60," +
            "\"httpEndpoint\":\"\"," +
            "\"token\":\"\"," +
            "\"credentials\":{\"type\":\"basic\",\"email\":\"\",\"password\":\"\",\"token\":\"\"}," +
            "\"metadata\":{}}";

    private static final String CONFIG_SECURITY_ENABLED_TEST_HEADER2 = " {\"baseUrl\":\"%s\"," +
            "\"replaceNoContentToOk\":true," +
            "\"enableSecurity\":true," +
            "\"headersFilter\": {\"testHeader2\": \"testValue2\"}," +
            "\"downlinkUrl\":\"https://api.thingpark.com/thingpark/lrc/rest/downlink\"," +
            "\"loriotDownlinkUrl\":\"https://eu1.loriot.io/1/rest\"," +
            "\"createLoriotOutput\":false," +
            "\"sendDownlink\":false," +
            "\"server\":\"eu1\"," +
            "\"appId\":\"\"," +
            "\"enableSecurityNew\":false," +
            "\"asId\":\"\"," +
            "\"asIdNew\":\"\"," +
            "\"asKey\":\"\"," +
            "\"clientIdNew\":\"\"," +
            "\"clientSecret\":\"\"," +
            "\"maxTimeDiffInSeconds\":60," +
            "\"httpEndpoint\":\"\"," +
            "\"token\":\"\"," +
            "\"credentials\":{\"type\":\"basic\",\"email\":\"\",\"password\":\"\",\"token\":\"\"}," +
            "\"metadata\":{}}";

    public static JsonNode defaultConfig(String httpsUrl){
        return JacksonUtil.toJsonNode(String.format(CONFIG, httpsUrl));
    }

    public static JsonNode defaultConfigWithSecurityEnabled(String httpsUrl) {
        return JacksonUtil.toJsonNode(String.format(CONFIG_SECURITY_ENABLED, httpsUrl));
    }

    public static JsonNode defaultConfigWithSecurityHeader(String httpsUrl) {
        return JacksonUtil.toJsonNode(String.format(CONFIG_SECURITY_ENABLED_WITH_TEST_HEADER, httpsUrl));
    }

    public static JsonNode defaultConfigWithSecurityHeader2(String httpsUrl) {
        return JacksonUtil.toJsonNode(String.format(CONFIG_SECURITY_ENABLED_TEST_HEADER2, httpsUrl));
    }
}
