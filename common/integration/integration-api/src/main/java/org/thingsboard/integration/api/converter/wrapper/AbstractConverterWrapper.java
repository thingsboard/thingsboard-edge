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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.DedicatedConverterConfig;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.util.TbPair;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractConverterWrapper implements ConverterWrapper {

    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @Override
    public TbPair<byte[], UplinkMetaData> wrap(DedicatedConverterConfig config, byte[] payload, UplinkMetaData metadata) {
        JsonNode payloadJson = JacksonUtil.fromBytes(payload);
        Map<String, String> payloadKvMap = readPayloadFields((ObjectNode) payloadJson, new HashMap<>());

        Map<String, String> kvMap = getKeys().entrySet().stream()
                .filter(e -> payloadKvMap.containsKey(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> payloadKvMap.get(e.getValue())));

        kvMap.putAll(metadata.getKvMap());
        TbPair<byte[], ContentType> payloadPair = getPayload(payloadJson);
        UplinkMetaData mergedMetadata = new UplinkMetaData(payloadPair.getSecond(), kvMap);

        return TbPair.of(payloadPair.getFirst(), mergedMetadata);
    }

    private Map<String, String> readPayloadFields(ObjectNode payload, Map<String, String> kvMap) {
        payload.properties().forEach(e -> {
            JsonNode node = e.getValue();
            if (node.isObject() && !getKeys().containsValue(e.getKey())) {
                readPayloadFields((ObjectNode) node, kvMap);
            } else {
                kvMap.put(e.getKey(), node.toString());
            }
        });
        return kvMap;
    }

    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) {
        var data = payloadJson.get("data").textValue();
        return TbPair.of(data.getBytes(StandardCharsets.UTF_8), ContentType.TEXT);
    }

}
