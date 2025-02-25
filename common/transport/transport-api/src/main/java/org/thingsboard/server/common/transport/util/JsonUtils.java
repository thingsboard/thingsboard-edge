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
package org.thingsboard.server.common.transport.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;

import java.util.List;
import java.util.Map;

public class JsonUtils {

    public static JsonObject getJsonObject(List<KeyValueProto> tsKv) {
        JsonObject json = new JsonObject();
        for (KeyValueProto kv : tsKv) {
            switch (kv.getType()) {
                case BOOLEAN_V:
                    json.addProperty(kv.getKey(), kv.getBoolV());
                    break;
                case LONG_V:
                    json.addProperty(kv.getKey(), kv.getLongV());
                    break;
                case DOUBLE_V:
                    json.addProperty(kv.getKey(), kv.getDoubleV());
                    break;
                case STRING_V:
                    json.addProperty(kv.getKey(), kv.getStringV());
                    break;
                case JSON_V:
                    json.add(kv.getKey(), JsonParser.parseString(kv.getJsonV()));
                    break;
            }
        }
        return json;
    }
    public static JsonElement parse(Object value) {
        if (value instanceof Integer) {
            return new JsonPrimitive((Integer) value);
        } else if (value instanceof Long) {
            return new JsonPrimitive((Long) value);
        } else if (value instanceof String) {
            return JsonParser.parseString((String) value);
        } else if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        } else if (value instanceof Double) {
            return new JsonPrimitive((Double) value);
        } else if (value instanceof Float) {
            return new JsonPrimitive((Float) value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + value.getClass().getSimpleName());
        }
    }

    public static JsonObject convertToJsonObject(Map<String,?> map) {
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            jsonObject.add(entry.getKey(), parse(entry.getValue()));
        }

        return jsonObject;
    }
}
