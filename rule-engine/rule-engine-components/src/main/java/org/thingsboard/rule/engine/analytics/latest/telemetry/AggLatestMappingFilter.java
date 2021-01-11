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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.thingsboard.server.common.data.DataConstants.*;

@Data
public class AggLatestMappingFilter {

    private List<String> clientAttributeNames;
    private List<String> sharedAttributeNames;
    private List<String> serverAttributeNames;
    private List<String> latestTsKeyNames;

    private String filterFunction;

    @JsonIgnore
    private Predicate<EntityId> filter;

    public Predicate<EntityId> getFilterFunction(TbContext ctx, Map<String, ScriptEngine> attributesScriptEngineMap) {
        if (filter == null) {
            filter = entityId -> {
                try {
                    Map<String,String> attributes = new HashMap<>();
                    prepareAttributes(ctx, attributes, entityId, CLIENT_SCOPE, clientAttributeNames, "cs_");
                    prepareAttributes(ctx, attributes, entityId, SHARED_SCOPE, sharedAttributeNames, "shared_");
                    prepareAttributes(ctx, attributes, entityId, SERVER_SCOPE, serverAttributeNames, "ss_");
                    prepareTimeseries(ctx, attributes, entityId, latestTsKeyNames);

                    ScriptEngine attributesScriptEngine = attributesScriptEngineMap.computeIfAbsent(filterFunction,
                            function -> ctx.getPeContext().createAttributesJsScriptEngine(function));

                    return attributesScriptEngine.executeAttributesFilter(attributes);
                } catch (Exception e) {
                    throw new RuntimeException("[" + entityId + "] Failed to execute attributes mapping filter!", e);
                }
            };
        }
        return filter;
    }

    private void prepareAttributes(TbContext ctx, Map<String,String> attributes, EntityId entityId, String scope, List<String> keys, String prefix) throws Exception {
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<AttributeKvEntry>> latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, scope, keys);
            latest.get().forEach(r -> {
                if (r.getValue() != null) {
                    attributes.put(prefix + r.getKey(), r.getValueAsString());
                }
            });
        }
    }

    private void prepareTimeseries(TbContext ctx, Map<String,String> attributes, EntityId entityId, List<String> keys) throws Exception {
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, keys);
            latest.get().forEach(r -> {
                if (r.getValue() != null) {
                    attributes.put(r.getKey(), r.getValueAsString());
                }
            });
        }
    }

}
