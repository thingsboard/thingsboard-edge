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
import com.google.common.collect.ImmutableMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.util.TbPair;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractConverterWrapper implements ConverterWrapper {

    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @Override
    public TbPair<byte[], UplinkMetaData> wrap(byte[] payload, UplinkMetaData metadata) {
        JsonNode payloadJson = JacksonUtil.fromBytes(payload);

        Map<String, String> kvMap = new HashMap<>(metadata.getKvMap());

        getKeysMapping().forEach((name, path) -> {
            JsonNode value = payloadJson.at(path);
            if (!value.isMissingNode()) {
                kvMap.put(name, value.toString());
            }
        });

        kvMap.putAll(metadata.getKvMap());
        TbPair<byte[], ContentType> payloadPair = getPayload(payloadJson);
        UplinkMetaData mergedMetadata = new UplinkMetaData(payloadPair.getSecond(), kvMap);

        return TbPair.of(payloadPair.getFirst(), mergedMetadata);
    }

    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) {
        var data = payloadJson.get("data").textValue();
        return TbPair.of(data.getBytes(StandardCharsets.UTF_8), ContentType.TEXT);
    }

    // Key is a name in metadata, Value is a JSON path to value in payload.
    protected abstract ImmutableMap<String, String> getKeysMapping();

    @Override
    public Set<String> getKeys() {
        return getKeysMapping().keySet();
    }

}
