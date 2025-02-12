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
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.nio.charset.StandardCharsets;

public class LoriotConverterWrapper extends AbstractConverterWrapper {

    private static final BiMap<String, String> KEYS_MAPPING;

    static {
        KEYS_MAPPING = new ImmutableBiMap.Builder<String, String>()
                .put("cmd", "cmd")
                .put("seqno", "seqno")
                .put("eui", "EUI")
                .put("ts", "ts")
                .put("ack", "ack")
                .put("bat", "bat")
                .put("fСnt", "fcnt")
                .put("fPort", "port")
                .put("offline", "offline")
                .put("frequency", "freq")
                .put("dr", "dr")
                .put("rssi", "rssi")
                .put("snr", "snr")
                .put("toa", "toa")
                .put("data", "data")
                .put("decoded", "decoded")
                .put("gws", "gws")
                .build();
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) {
        if (payloadJson.has("decoded")) {
            var decoded = payloadJson.get("decoded");
            return TbPair.of(JacksonUtil.writeValueAsBytes(decoded), ContentType.JSON);
        } else if (payloadJson.has("data")) {
            var data = payloadJson.get("data").textValue();
            return TbPair.of(data.getBytes(StandardCharsets.UTF_8), ContentType.TEXT);
        } else {
            return TbPair.of(EMPTY_BYTE_ARRAY, ContentType.BINARY);
        }
    }

    @Override
    protected BiMap<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }
}
