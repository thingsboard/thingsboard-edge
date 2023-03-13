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
package org.thingsboard.rule.engine.analytics.latest.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "alarms count",
        configClazz = TbAlarmsCountNodeV2Configuration.class,
        nodeDescription = "Counts alarms by msg originator",
        nodeDetails = "Performs count of alarms for originator and for propagation entities if specified. " +
                "Generates outgoing messages with alarm count values for each found entity. By default, an outgoing message generates with 'POST_TELEMETRY_REQUEST' type. " +
                "The type of the outgoing messages controls under \"<b>Output message type</b>\" configuration parameter.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbAnalyticsNodeAlarmsCountV2Config",
        icon = "functions"
)
public class TbAlarmsCountNodeV2 implements TbNode {

    private static final List<String> ALARM_FIELDS = List.of("originator", "severity", "status", "ackTs", "clearTs", "details");

    private TbAlarmsCountNodeV2Configuration config;
    private String queueName;
    private String outMsgType;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbAlarmsCountNodeV2Configuration.class);
        this.queueName = config.getQueueName();
        this.outMsgType = StringUtils.isNotBlank(config.getOutMsgType()) ? config.getOutMsgType() : SessionMsgType.POST_TELEMETRY_REQUEST.name();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String msgType = msg.getType();
        EntityType entityType = msg.getOriginator().getEntityType();

        Alarm alarm = null;
        var processAlarmsCount = false;
        if (msgType.equals(DataConstants.ENTITY_CREATED) || msgType.equals(DataConstants.ENTITY_UPDATED)) {
            if (entityType.equals(EntityType.ALARM)) {
                alarm = convertMsgDataToAlarm(msg);
                processAlarmsCount = true;
            } else {
                JsonNode jsonData = JacksonUtil.toJsonNode(msg.getData());
                var msgDataHasAlarmFields = ALARM_FIELDS.stream().allMatch(jsonData::has);
                if (msgDataHasAlarmFields) {
                    alarm = JacksonUtil.treeToValue(jsonData, AlarmInfo.class);
                    log.debug("[{}] Msg data was successfully parsed to alarm object {}", ctx.getTenantId(), alarm);
                    processAlarmsCount = true;
                }
            }
        } else if (msgType.equals(DataConstants.ALARM) || msgType.equals(DataConstants.ALARM_ACK) || msgType.equals(DataConstants.ALARM_CLEAR)) {
            alarm = convertMsgDataToAlarm(msg);
            processAlarmsCount = true;
        }

        if (processAlarmsCount) {
            process(ctx, msg, alarm);
        } else {
            ctx.tellSuccess(msg);
        }
    }

    private AlarmInfo convertMsgDataToAlarm(TbMsg msg) {
        return JacksonUtil.fromString(msg.getData(), AlarmInfo.class);
    }

    private void process(TbContext ctx, TbMsg msg, Alarm alarm) {
        if (alarm == null) {
            ctx.tellFailure(msg, new RuntimeException("Failed to process alarms count since the msg data could not be converted to alarm!"));
            return;
        }
        Map<EntityId, ObjectNode> result = new HashMap<>();
        getPropagationEntityIds(ctx, alarm).forEach(entityId -> result.put(entityId, countAlarms(ctx, entityId)));

        String dataTs = Long.toString(System.currentTimeMillis());

        result.forEach((entityId, data) -> {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("ts", dataTs);
            TbMsg newMsg = TbMsg.newMsg(queueName, outMsgType,
                    entityId, metaData, JacksonUtil.toString(data));
            ctx.enqueueForTellNext(newMsg, TbRelationTypes.SUCCESS);
        });
        ctx.ack(msg);
    }

    private Set<EntityId> getPropagationEntityIds(TbContext ctx, Alarm alarm) {
        if (config.isCountAlarmsForPropagationEntities() && (alarm.isPropagate() || alarm.isPropagateToOwner() || alarm.isPropagateToOwnerHierarchy() || alarm.isPropagateToTenant())) {
            Set<EntityId> propagationEntityIds = ctx.getAlarmService().getPropagationEntityIds(alarm, config.getPropagationEntityTypes());
            propagationEntityIds.add(alarm.getOriginator());
            return propagationEntityIds;
        } else {
            return Collections.singleton(alarm.getOriginator());
        }
    }

    private ObjectNode countAlarms(TbContext ctx, EntityId entityId) {
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
        AlarmQuery alarmQuery = new AlarmQuery(entityId, pageLink, null, null, null,false);
        List<Long> alarmCounts = ctx.getAlarmService().findAlarmCounts(ctx.getTenantId(), alarmQuery, filters);
        ObjectNode obj = JacksonUtil.newObjectNode();
        for (int i = 0; i < mappings.size(); i++) {
            obj.put(mappings.get(i).getTarget(), alarmCounts.get(i));
        }
        return obj;
    }

}
