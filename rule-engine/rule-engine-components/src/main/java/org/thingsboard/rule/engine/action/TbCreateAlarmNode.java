/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.List;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create alarm", relationTypes = {"Created", "Updated", "False"},
        configClazz = TbCreateAlarmNodeConfiguration.class,
        nodeDescription = "Create or Update Alarm",
        nodeDetails =
                "Details - JS function that creates JSON object based on incoming message. This object will be added into Alarm.details field.\n" +
                        "Node output:\n" +
                        "If alarm was not created, original message is returned. Otherwise new Message returned with type 'ALARM', Alarm object in 'msg' property and 'matadata' will contains one of those properties 'isNewAlarm/isExistingAlarm'. " +
                        "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                        "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateAlarmConfig",
        icon = "notifications_active"
)
public class TbCreateAlarmNode extends TbAbstractAlarmNode<TbCreateAlarmNodeConfiguration> {

    private static ObjectMapper mapper = new ObjectMapper();
    private List<String> relationTypes;

    @Override
    protected TbCreateAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        TbCreateAlarmNodeConfiguration nodeConfiguration = TbNodeUtils.convert(configuration, TbCreateAlarmNodeConfiguration.class);
        relationTypes = nodeConfiguration.getRelationTypes();
        return nodeConfiguration;
    }

    @Override
    protected ListenableFuture<AlarmResult> processAlarm(TbContext ctx, TbMsg msg) {
        String alarmType;
        final Alarm msgAlarm;

        if (!config.isUseMessageAlarmData()) {
            alarmType = TbNodeUtils.processPattern(this.config.getAlarmType(), msg.getMetaData());
            msgAlarm = null;
        } else {
            try {
                msgAlarm = getAlarmFromMessage(ctx, msg);
                alarmType = msgAlarm.getType();
            } catch (IOException e) {
                ctx.tellFailure(msg, e);
                return null;
            }
        }

        ListenableFuture<Alarm> latest = ctx.getAlarmService().findLatestByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), alarmType);
        return Futures.transformAsync(latest, existingAlarm -> {
            if (existingAlarm == null || existingAlarm.getStatus().isCleared()) {
                return createNewAlarm(ctx, msg, msgAlarm);
            } else {
                return updateAlarm(ctx, msg, existingAlarm, msgAlarm);
            }
        }, ctx.getDbCallbackExecutor());

    }

    private Alarm getAlarmFromMessage(TbContext ctx, TbMsg msg) throws IOException {
        Alarm msgAlarm;
        msgAlarm = mapper.readValue(msg.getData(), Alarm.class);
        msgAlarm.setTenantId(ctx.getTenantId());
        if (msgAlarm.getOriginator() == null) {
            msgAlarm.setOriginator(msg.getOriginator());
        }
        if (msgAlarm.getStatus() == null) {
            msgAlarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        }
        return msgAlarm;
    }

    private ListenableFuture<AlarmResult> createNewAlarm(TbContext ctx, TbMsg msg, Alarm msgAlarm) {
        ListenableFuture<Alarm> asyncAlarm;
        if (msgAlarm != null) {
            asyncAlarm = Futures.immediateCheckedFuture(msgAlarm);
        } else {
            ctx.logJsEvalRequest();
            asyncAlarm = Futures.transform(buildAlarmDetails(ctx, msg, null),
                    details -> {
                        ctx.logJsEvalResponse();
                        return buildAlarm(msg, details, ctx.getTenantId());
                    });
        }
        ListenableFuture<Alarm> asyncCreated = Futures.transform(asyncAlarm,
                alarm -> ctx.getAlarmService().createOrUpdateAlarm(alarm), ctx.getDbCallbackExecutor());
        return Futures.transform(asyncCreated, alarm -> new AlarmResult(true, false, false, alarm));
    }

    private ListenableFuture<AlarmResult> updateAlarm(TbContext ctx, TbMsg msg, Alarm existingAlarm, Alarm msgAlarm) {
        ctx.logJsEvalRequest();
        ListenableFuture<Alarm> asyncUpdated = Futures.transform(buildAlarmDetails(ctx, msg, existingAlarm.getDetails()), (Function<JsonNode, Alarm>) details -> {
            ctx.logJsEvalResponse();
            if (msgAlarm != null) {
                existingAlarm.setSeverity(msgAlarm.getSeverity());
                existingAlarm.setPropagate(msgAlarm.isPropagate());
                existingAlarm.setPropagateRelationTypes(msgAlarm.getPropagateRelationTypes());
            } else {
                existingAlarm.setSeverity(config.getSeverity());
                existingAlarm.setPropagate(config.isPropagate());
                existingAlarm.setPropagateRelationTypes(relationTypes);
            }
            existingAlarm.setDetails(details);
            existingAlarm.setEndTs(System.currentTimeMillis());
            return ctx.getAlarmService().createOrUpdateAlarm(existingAlarm);
        }, ctx.getDbCallbackExecutor());

        return Futures.transform(asyncUpdated, a -> new AlarmResult(false, true, false, a));
    }

    private Alarm buildAlarm(TbMsg msg, JsonNode details, TenantId tenantId) {
        return Alarm.builder()
                .tenantId(tenantId)
                .originator(msg.getOriginator())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(config.getSeverity())
                .propagate(config.isPropagate())
                .type(TbNodeUtils.processPattern(this.config.getAlarmType(), msg.getMetaData()))
                .propagateRelationTypes(relationTypes)
                //todo-vp: alarm date should be taken from Message or current Time should be used?
//                .startTs(System.currentTimeMillis())
//                .endTs(System.currentTimeMillis())
                .details(details)
                .build();
    }

}
