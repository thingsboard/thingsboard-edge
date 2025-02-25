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
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
public class TbCountUniqueIntervalState extends TbBaseIntervalState {

    private Set<String> items = ConcurrentHashMap.newKeySet();

    public TbCountUniqueIntervalState(JsonElement stateJson) {
        for (JsonElement jsonElement : stateJson.getAsJsonArray()) {
            String elementAsString = jsonElement.getAsString();
            try {
                // try to fix bug with incorrect conversion JsonElement to text value
                elementAsString = normalizeJsonString(elementAsString);
                JsonNode jsonNode = JacksonUtil.toJsonNode(elementAsString);
                items.add(JacksonUtil.writeValueAsString(jsonNode));
            } catch (Exception e) {
                items.add(elementAsString);
            }
        }
    }

    private String normalizeJsonString(String jsonString) {
        String unescapedJson = StringEscapeUtils.unescapeJson(jsonString);
        while (unescapedJson.contains("\\\"")) {
            unescapedJson = unescapedJson.replace("\\\"", "\""); // Unescape backslash and quote
        }
        return unescapedJson.replaceAll("^\"+|\"+$", "")         // Remove surrounding quotes
                .replace("\\\\", "\\");                          // Unescape backslashes
    }

    @Override
    protected boolean doUpdate(JsonElement data) {
        return items.add(data.getAsString());
    }

    @Override
    public String toValueJson(Gson gson, String outputValueKey) {
        JsonObject json = new JsonObject();
        json.addProperty(outputValueKey, items.size());
        return gson.toJson(json);
    }

    @Override
    public String toStateJson(Gson gson) {
        JsonArray array = new JsonArray();
        items.forEach(item -> array.add(new JsonPrimitive(item)));
        return gson.toJson(array);
    }
}
