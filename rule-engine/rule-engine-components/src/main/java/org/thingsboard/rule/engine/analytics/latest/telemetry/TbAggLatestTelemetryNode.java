/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNode;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "aggregate latest (deprecated)",
        configClazz = TbAggLatestTelemetryNodeConfiguration.class,
        version = 1,
        nodeDescription = "Periodically aggregates entities attributes or latest timeseries",
        nodeDetails = "Performs aggregation of attributes or latest timeseries fetched from child entities with configurable period. " +
                "Generates outgoing messages with aggregated values for each parent entity. By default, an outgoing message generates with 'POST_TELEMETRY_REQUEST' type. " +
                "The type of the outgoing messages controls under \"<b>Output message type</b>\" configuration parameter.",
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
