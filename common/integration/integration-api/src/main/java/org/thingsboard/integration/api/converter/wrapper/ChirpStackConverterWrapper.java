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
package org.thingsboard.integration.api.converter.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Base64;

public class ChirpStackConverterWrapper extends AbstractConverterWrapper {

    private static final ImmutableMap<String, String> KEYS_MAPPING;

    static {
        KEYS_MAPPING = new ImmutableMap.Builder<String, String>()
                .put("deduplicationId", "/deduplicationId")
                .put("time", "/time")
                .put("tenantId", "/deviceInfo/tenantId")
                .put("tenantName", "/deviceInfo/tenantName")
                .put("applicationId", "/deviceInfo/applicationId")
                .put("applicationName", "/deviceInfo/applicationName")
                .put("deviceProfileId", "/deviceInfo/deviceProfileId")
                .put("deviceProfileName", "/deviceInfo/deviceProfileName")
                .put("deviceName", "/deviceInfo/deviceName")
                .put("eui", "/deviceInfo/devEui")
                .put("tags", "/deviceInfo/tags")
                .put("devAddr", "/devAddr")
                .put("adr", "/adr")
                .put("dr", "/dr")
                .put("fCnt", "/fCnt")
                .put("fPort", "/fPort")
                .put("confirmed", "/confirmed")
                .put("data", "/data")
                .put("decoded", "/object")
                .put("rxInfo", "/rxInfo")
                .put("frequency", "/txInfo/frequency")
                .put("bandwidth", "/txInfo/modulation/lora/bandwidth")
                .put("spreadingFactor", "/txInfo/modulation/lora/spreadingFactor")
                .put("codeRate", "/txInfo/modulation/lora/codeRate")
                .put("latitude", "/location/latitude")
                .put("longitude", "/location/longitude")
                .put("altitude", "/location/altitude")
                .put("rssi", "")
                .put("snr", "")
                .build();
    }

    @Override
    protected String getGatewayInfoPath() {
        return "/rxInfo";
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) {
        if (payloadJson.has("object")) {
            var decoded = payloadJson.get("object");
            return TbPair.of(JacksonUtil.writeValueAsBytes(decoded), ContentType.JSON);
        } else {
            var data = payloadJson.get("data").textValue();
            return TbPair.of(Base64.getDecoder().decode(data), ContentType.BINARY);
        }
    }

    @Override
    protected ImmutableMap<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }
}
