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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LoriotConverterWrapper extends AbstractConverterWrapper {

    @Getter
    private static final Map<String, String> keys;

    static {
        keys = Map.ofEntries(
                Map.entry("cmd", "cmd"),
                Map.entry("seqno", "seqno"),
                Map.entry("eui", "EUI"),
                Map.entry("ts", "ts"),
                Map.entry("ack", "ack"),
                Map.entry("bat", "bat"),
                Map.entry("fСnt", "fcnt"),
                Map.entry("fPort", "port"),
                Map.entry("offline", "offline"),
                Map.entry("frequency", "freq"),
                Map.entry("dr", "dr"),
                Map.entry("rssi", "rssi"),
                Map.entry("snr", "snr"),
                Map.entry("toa", "toa"),
                Map.entry("data", "data"),
                Map.entry("decoded", "decoded"),
                Map.entry("gws", "gws")
        );
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) {
        if (payloadJson.has("data")) {
            var data = payloadJson.get("data").textValue();
            return TbPair.of(data.getBytes(StandardCharsets.UTF_8), ContentType.TEXT);
        } else if (payloadJson.has("decoded")) {
            var decoded = payloadJson.get("decoded");
            return TbPair.of(JacksonUtil.writeValueAsBytes(decoded), ContentType.JSON);
        } else {
            return TbPair.of(EMPTY_BYTE_ARRAY, ContentType.BINARY);
        }
    }
}
