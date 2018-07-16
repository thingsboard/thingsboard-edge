/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.math.mapper.alarm;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.math.mapper.TbAbstractMapperNode;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.*;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "alarms count",
        configClazz = TbAlarmsCountNodeConfiguration.class,
        nodeDescription = "Periodically counts alarms for entities",
        nodeDetails = "Performs count of alarms for parent entities and child entities is specified with configurable period. " +
                "Generates 'POST_TELEMETRY_REQUEST' messages with alarm count values for each found entity.",
        inEnabled = false,
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbActionNodeAlarmsCountConfig",
        icon = "functions"
)

public class TbAlarmsCountNode extends TbAbstractMapperNode<TbAlarmsCountNodeConfiguration> {

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
        if (this.config.isCalculateForChildEntities()) {
            ListenableFuture<List<EntityId>> childEntityIdsFuture =
                    this.config.getParentEntitiesQuery().getChildEntitiesAsync(ctx, parentEntityId);
            try {
                entityIds.addAll(childEntityIdsFuture.get());
            } catch (Exception e) {
                TbMsg msg = new TbMsg(UUIDs.timeBased(), SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                        parentEntityId, new TbMsgMetaData(), TbMsgDataType.JSON,
                        "", null, null, 0L);
                ctx.tellFailure(msg, new RuntimeException("Failed to fetch child entities for parent entity [" + parentEntityId + "]", e));
            }
        }
        Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> result = new HashMap<>();
        entityIds.forEach(entityId -> {
            List<ListenableFuture<Optional<JsonObject>>> aggregateFutures = new ArrayList<>();
            this.config.getAlarmsCountMappings().
                    forEach(alarmsCountMapping -> aggregateFutures.add(alarmsCountMapping.countAlarms(ctx, entityId)));
            result.put(entityId, aggregateFutures);
        });
        return result;
    }

    @Override
    public void destroy() {
    }

}
