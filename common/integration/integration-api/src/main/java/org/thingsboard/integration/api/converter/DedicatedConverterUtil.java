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
package org.thingsboard.integration.api.converter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DedicatedConverterUtil {

    public static final String TELEMETRY = "telemetry";
    public static final String VALUES = "values";
    public static final String TS = "ts";
    public static final String ATTRIBUTES = "attributes";
    public static final String DEFAULT_PROFILE = "default";

    private static final Gson GSON = new Gson();

    private DedicatedConverterUtil() {
    }

    public static DedicatedUplinkData parseUplinkData(DedicatedConverterConfig config, JsonObject src, UplinkMetaData metadata) {
        Map<String, Object> kvMap = new HashMap<>(metadata.getKvMap());

        JsonElement telemetry;
        if (src.has(TELEMETRY)) {
            telemetry = src.get(TELEMETRY);

            if (telemetry.isJsonArray()) {
                JsonObject telemetryFromMetadata = addKvs(new JsonObject(), kvMap, config.getTelemetry());
                if (!telemetryFromMetadata.isEmpty()) {
                    telemetry.getAsJsonArray().add(telemetryFromMetadata);
                }
            } else if (telemetry.isJsonObject()) {
                var obj = telemetry.getAsJsonObject();
                if (obj.has(VALUES)) {
                    var values = obj.get(VALUES).getAsJsonObject();
                    addKvs(values, kvMap, config.getTelemetry());
                } else {
                    addKvs(obj, kvMap, config.getTelemetry());
                }
            }
        } else {
            telemetry = addKvs(new JsonObject(), kvMap, config.getTelemetry());
        }

        JsonObject attributes;
        if (src.has(ATTRIBUTES)) {
            attributes = src.get(ATTRIBUTES).getAsJsonObject();
        } else {
            attributes = new JsonObject();
        }

        addKvs(attributes, kvMap, config.getAttributes());

        EntityType entityType = getProperty(src, "type", EntityType::valueOf, config::getType);
        String entityName = getProperty(src, "name", () -> processTemplate(config.getName(), kvMap));
        String profile = getProperty(src, "profile", () -> processTemplate(config.getProfile(), kvMap));
        if (profile == null) {
            profile = DEFAULT_PROFILE;
        }
        String label = getProperty(src, "label", () -> processTemplate(config.getLabel(), kvMap));
        String customer = getProperty(src, "customer", () -> processTemplate(config.getCustomer(), kvMap));
        String group = getProperty(src, "group", () -> processTemplate(config.getGroup(), kvMap));

        return DedicatedUplinkData.builder()
                .entityType(entityType)
                .name(entityName)
                .profile(profile)
                .label(label)
                .customer(customer)
                .group(group)
                .telemetry(telemetry)
                .attributes(attributes)
                .build();
    }

    private static String getProperty(JsonObject src, String key, Supplier<String> defaultValue) {
        return getProperty(src, key, Function.identity(), defaultValue);
    }

    private static <T> T getProperty(JsonObject src, String key, Function<String, T> mapper, Supplier<T> defaultValue) {
        JsonElement jsonValue = src.get(key);
        if (jsonValue != null && !jsonValue.isJsonNull()) {
            String value = src.get(key).getAsString();
            if (StringUtils.isNotEmpty(value)) {
                return mapper.apply(value);
            }
        }
        return defaultValue.get();
    }

    private static JsonObject addKvs(JsonObject kvsObj, Map<String, Object> kvMap, Set<String> keys) {
        if (CollectionsUtil.isNotEmpty(keys) && !kvMap.isEmpty()) {
            kvMap.entrySet().stream()
                    .filter(e -> keys.contains(e.getKey()) && !kvsObj.has(e.getKey()))
                    .forEach(e -> kvsObj.add(e.getKey(), GSON.toJsonTree(e.getValue())));
        }
        return kvsObj;
    }

    private static String processTemplate(String template, Map<String, Object> data) {
        if (StringUtils.isNotEmpty(template)) {
            String result = template;
            for (Map.Entry<String, Object> kv : data.entrySet()) {
                result = processVar(result, kv.getKey(), kv.getValue());
            }
            return result;
        }
        return null;
    }

    private static String processVar(String pattern, String key, Object val) {
        String stringValue;
        if (val instanceof String) {
            stringValue = (String) val;
        } else {
            stringValue = JacksonUtil.toString(val);
        }
        return pattern.replace(formatVarTemplate(key), stringValue);
    }

    private static String formatVarTemplate(String key) {
        return "$" + key;
    }
}
