/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
public class TbCountUniqueIntervalState extends TbBaseIntervalState {

    private Set<String> items = new HashSet<>();

    public TbCountUniqueIntervalState(JsonElement stateJson) {
        stateJson.getAsJsonArray().forEach(jsonElement -> items.add(jsonElement.toString()));
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
