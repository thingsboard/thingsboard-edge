/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TbCountUniqueAggFunction implements TbAggFunction {

    private Set<String> items = new HashSet<>();

    @Override
    public void update(Optional<KvEntry> entry, double defaultValue) {
        if (entry.isPresent()) {
            items.add(entry.get().getValueAsString());
        }
    }

    @Override
    public Optional<JsonElement> result() {
        return Optional.of(new JsonPrimitive(items.size()));
    }
}
