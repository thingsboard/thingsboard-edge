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
package org.thingsboard.rule.engine.analytics.latest.alarm;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNode;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.*;
import java.util.function.Predicate;

@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "alarms count",
        configClazz = TbAlarmsCountNodeConfiguration.class,
        nodeDescription = "Periodically counts alarms for entities",
        nodeDetails = "Performs count of alarms for parent entities and child entities if specified with configurable period. " +
                "Generates 'POST_TELEMETRY_REQUEST' messages with alarm count values for each found entity.",
        inEnabled = false,
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbAnalyticsNodeAlarmsCountConfig",
        icon = "functions"
)

public class TbAlarmsCountNode extends TbAbstractLatestNode<TbAlarmsCountNodeConfiguration> {

    private static final String TB_ALARMS_COUNT_NODE_MSG = "TbAlarmsCountNodeMsg";

    @Override
    protected TbAlarmsCountNodeConfiguration loadMapperNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAlarmsCountNodeConfiguration.class);
    }

    @Override
    protected String tickMessageType() {
        return TB_ALARMS_COUNT_NODE_MSG;
    }

    @Override
    protected Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> doParentAggregations(TbContext ctx, EntityId parentEntityId) {
        List<EntityId> entityIds = new ArrayList<>();
        entityIds.add(parentEntityId);
        if (this.config.isCountAlarmsForChildEntities()) {
            ListenableFuture<List<EntityId>> childEntityIdsFuture =
                    this.config.getParentEntitiesQuery().getChildEntitiesAsync(ctx, parentEntityId);
            try {
                entityIds.addAll(childEntityIdsFuture.get());
            } catch (Exception e) {
                TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                        parentEntityId, new TbMsgMetaData(), "");
                ctx.enqueueForTellFailure(msg, "Failed to fetch child entities for parent entity [" + parentEntityId + "]");
            }
        }
        Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> result = new HashMap<>();
        entityIds.forEach(entityId -> {
            List<ListenableFuture<Optional<JsonObject>>> aggregateFutures = new ArrayList<>();
            JsonObject data = countAlarms(ctx, entityId);
            aggregateFutures.add(Futures.immediateFuture(Optional.of(data)));
            result.put(entityId, aggregateFutures);
        });
        return result;
    }

    private JsonObject countAlarms(TbContext ctx, EntityId entityId) {
        List<AlarmsCountMapping> mappings = this.config.getAlarmsCountMappings();
        List<AlarmFilter> filters = new ArrayList<>();
        for (AlarmsCountMapping mapping : mappings) {
            filters.add(mapping.createAlarmFilter());
        }
        long interval = 0;
        for (AlarmsCountMapping mapping : mappings) {
            if (mapping.getLatestInterval() == 0) {
                interval = 0;
                break;
            } else {
                interval = Math.max(interval, mapping.getLatestInterval());
            }
        }
        TimePageLink pageLink;
        PageLink alarmSearchPageLink = new PageLink(Integer.MAX_VALUE);
        if (interval > 0) {
            pageLink = new TimePageLink(alarmSearchPageLink, System.currentTimeMillis() - interval, null);
        } else {
            pageLink = new TimePageLink(alarmSearchPageLink, null, null);
        }
        AlarmQuery alarmQuery = new AlarmQuery(entityId, pageLink, null, null, false, null);
        List<Long> alarmCounts = ctx.getAlarmService().findAlarmCounts(ctx.getTenantId(), alarmQuery, filters);
        JsonObject obj = new JsonObject();
        for (int i = 0; i < mappings.size(); i++) {
            obj.addProperty(mappings.get(i).getTarget(), alarmCounts.get(i));
        }
        return obj;
    }

    @Override
    public void destroy() {
    }

}
