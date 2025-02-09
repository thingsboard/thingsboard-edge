/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.Getter;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Base64;
import java.util.Map;

public class ChirpStackConverterWrapper extends AbstractConverterWrapper {

    @Getter
    private static final Map<String, String> keys;

    static {
        keys = Map.ofEntries(
                Map.entry("deduplicationId", "deduplicationId"),
                Map.entry("time", "time"),
                Map.entry("tenantId", "tenantId"),
                Map.entry("tenantName", "tenantName"),
                Map.entry("applicationId", "applicationId"),
                Map.entry("applicationName", "applicationName"),
                Map.entry("deviceProfileId", "deviceProfileId"),
                Map.entry("deviceProfileName", "deviceProfileName"),
                Map.entry("deviceName", "deviceName"),
                Map.entry("eui", "devEui"),
                Map.entry("tags", "tags"),
                Map.entry("devAddr", "devAddr"),
                Map.entry("adr", "adr"),
                Map.entry("dr", "dr"),
                Map.entry("fCnt", "fCnt"),
                Map.entry("fPort", "fPort"),
                Map.entry("confirmed", "confirmed"),
                Map.entry("data", "data"),
                Map.entry("rxInfo", "rxInfo"),
                Map.entry("frequency", "frequency"),
                Map.entry("bandwidth", "bandwidth"),
                Map.entry("spreadingFactor", "spreadingFactor"),
                Map.entry("codeRate", "codeRate")
        );
    }

    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) {
        var data = payloadJson.get("data").textValue();
        return TbPair.of(Base64.getDecoder().decode(data), ContentType.JSON);
    }
}
