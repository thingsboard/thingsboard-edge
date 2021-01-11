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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class AggLatestMapping {

    private String source;
    private String sourceScope;
    private double defaultValue;
    private String target;
    private MathFunction aggFunction;
    private AggLatestMappingFilter filter;

    public ListenableFuture<Optional<JsonObject>> aggregate(TbContext ctx, Map<String, ScriptEngine> attributesScriptEngineMap,
                                            ListenableFuture<List<EntityId>> entityIds) {
        ListenableFuture<List<EntityId>> filteredEntityIds =
                filter != null
                        ? Futures.transform(entityIds, ids -> {
                                List<EntityId> filtered = ids.stream().filter(filter.getFilterFunction(ctx, attributesScriptEngineMap)).collect(Collectors.toList());
                                return filtered;
                            }, ctx.getJsExecutor())
                        : entityIds;
        return Futures.transform(filteredEntityIds, ids -> {
            TbAggFunction aggregation = TbAggFunctionFactory.createAggFunction(aggFunction);
            ids.forEach(entityId -> {
                Optional<KvEntry> entry = aggregation.fetchAttrValue() ? fetchValue(ctx, entityId) : Optional.empty();
                aggregation.update(entry, defaultValue);
            });
            Optional<JsonElement> result = aggregation.result();
            if (result.isPresent()) {
                JsonObject obj = new JsonObject();
                obj.add(target, result.get());
                return Optional.of(obj);
            } else {
                return Optional.empty();
            }
        }, ctx.getDbCallbackExecutor());
    }

    private Optional<KvEntry> fetchValue(TbContext ctx, EntityId entityId) {
        try {
            if ("LATEST_TELEMETRY".equals(sourceScope)) {
                ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, Collections.singletonList(source));
                List<TsKvEntry> latestTs = latest.get();
                if (latestTs != null && !latestTs.isEmpty() && latestTs.get(0).getValue() != null) {
                    return Optional.of(latestTs.get(0));
                }
            } else {
                ListenableFuture<Optional<AttributeKvEntry>> latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, sourceScope, source);
                Optional<AttributeKvEntry> latestAttr = latest.get();
                if (latestAttr.isPresent()) {
                    return Optional.of(latestAttr.get());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch value of attribute/telemetry telemetry [" + source + "] of entity [" + entityId + "]", e);
        }
        return Optional.empty();
    }

}
