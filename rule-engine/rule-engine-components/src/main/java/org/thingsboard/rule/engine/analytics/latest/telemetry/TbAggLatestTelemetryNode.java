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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNode;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "aggregate latest",
        configClazz = TbAggLatestTelemetryNodeConfiguration.class,
        nodeDescription = "Periodically aggregates entities attributes or latest timeseries",
        nodeDetails = "Performs aggregation of attributes or latest timeseries fetched from child entities with configurable period. " +
                "Generates 'POST_TELEMETRY_REQUEST' messages with aggregated values for each parent entity.",
        inEnabled = false,
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbAnalyticsNodeAggregateLatestConfig",
        icon = "functions"
)

public class TbAggLatestTelemetryNode extends TbAbstractLatestNode<TbAggLatestTelemetryNodeConfiguration> {

    private static final String TB_AGG_LATEST_NODE_MSG = "TbAggLatestNodeMsg";

    private Map<String, ScriptEngine> attributesScriptEngineMap = new ConcurrentHashMap<>();

    @Override
    protected TbAggLatestTelemetryNodeConfiguration loadMapperNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAggLatestTelemetryNodeConfiguration.class);
    }

    @Override
    protected String tickMessageType() {
        return TB_AGG_LATEST_NODE_MSG;
    }

    @Override
    protected Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> doParentAggregations(TbContext ctx, EntityId parentEntityId) {
        ListenableFuture<List<EntityId>> childEntityIds =
                this.config.getParentEntitiesQuery().getChildEntitiesAsync(ctx, parentEntityId);
        List<ListenableFuture<Optional<JsonObject>>> aggregateFutures = new ArrayList<>();
        this.config.getAggMappings().forEach(aggMapping -> aggregateFutures.add(aggMapping.aggregate(ctx, attributesScriptEngineMap, childEntityIds)));
        Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> result = new HashMap<>();
        result.put(parentEntityId, aggregateFutures);
        return result;
    }

    @Override
    public void destroy() {
        if (this.attributesScriptEngineMap != null) {
            for (ScriptEngine se : this.attributesScriptEngineMap.values()) {
                se.destroy();
            }
            this.attributesScriptEngineMap.clear();
        }
    }

}
