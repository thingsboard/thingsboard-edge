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
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.util.TbPair;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class AbstractConverterUnwrapper implements ConverterUnwrapper {

    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @Override
    public TbPair<byte[], UplinkMetaData<Object>> unwrap(byte[] payload, UplinkMetaData metadata) throws Exception {
        JsonNode payloadJson = JacksonUtil.fromBytes(payload);

        Map<String, Object> kvMap = new TreeMap<>(metadata.getKvMap());

        getKeysMapping().forEach((name, path) -> {
            if (path.isEmpty()) {
                return;
            }
            JsonNode value = payloadJson.at(path);
            if (!value.isMissingNode()) {
                kvMap.put(name, JacksonUtil.convertValue(value, Object.class));
            }
        });

        addGatewayAdditionalInfo(kvMap, payloadJson);

        postMapping(kvMap);
        kvMap.putAll(metadata.getKvMap());
        TbPair<byte[], ContentType> payloadPair = getPayload(payloadJson);
        UplinkMetaData<Object> mergedMetadata = new UplinkMetaData<>(payloadPair.getSecond(), kvMap);

        return TbPair.of(payloadPair.getFirst(), mergedMetadata);
    }

    protected void addGatewayAdditionalInfo(Map<String, Object> kvMap, JsonNode payloadJson) {
        JsonNode rxMetadataArray = payloadJson.at(getGatewayInfoPath());
        if (!rxMetadataArray.isEmpty()) {
            JsonNode rxMetadata = findByMaxRssi(rxMetadataArray);
            if (rxMetadata != null) {
                kvMap.put("rssi", rxMetadata.get("rssi").asInt());
                kvMap.put("snr", rxMetadata.get("snr").asDouble());
            }
        }
    }

    protected String getGatewayInfoPath() {
        return "";
    }

    protected JsonNode findByMaxRssi(JsonNode gwArray) {
        JsonNode result = null;
        int maxRssi = Integer.MIN_VALUE;

        for (JsonNode node : gwArray) {
            JsonNode rssiNode = node.get("rssi");
            if (rssiNode != null && rssiNode.isNumber()) {
                int rssi = rssiNode.asInt();

                if (rssi > maxRssi) {
                    maxRssi = rssi;
                    result = node;
                }
            }
        }
        return result;
    }

    protected abstract TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) throws Exception;

    protected void postMapping(Map<String, Object> kvMap) {
        long ts = 0;
        if (kvMap.containsKey("time")) {
            var timeTs = parseDateToTimestamp(kvMap.get("time").toString());
            kvMap.put("timeTs", timeTs);
            ts = timeTs;
        }
        if (ts == 0) {
            ts = System.currentTimeMillis();
        }
        kvMap.put("ts", ts);
    }

    protected long parseDateToTimestamp(String dateString) {
        try {
            return OffsetDateTime.parse(dateString).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    // Key is a name in metadata, Value is a JSON path to value in payload.
    protected abstract ImmutableMap<String, String> getKeysMapping();

    @Override
    public Set<String> getKeys() {
        return getKeysMapping().keySet();
    }

}
