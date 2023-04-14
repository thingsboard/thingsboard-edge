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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;

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
                        "If alarm was not created, original message is returned. Otherwise new Message returned with type 'ALARM', Alarm object in 'msg' property and 'metadata' will contains one of those properties 'isNewAlarm/isExistingAlarm'. " +
                        "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                        "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateAlarmConfig",
        icon = "notifications_active"
)
public class TbCreateAlarmNode extends TbAbstractAlarmNode<TbCreateAlarmNodeConfiguration> {

    private List<String> relationTypes;
    private AlarmSeverity notDynamicAlarmSeverity;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx, configuration);
        if (!this.config.isDynamicSeverity()) {
            this.notDynamicAlarmSeverity = EnumUtils.getEnum(AlarmSeverity.class, this.config.getSeverity());
            if (this.notDynamicAlarmSeverity == null) {
                throw new TbNodeException("Incorrect Alarm Severity value: " + this.config.getSeverity());
            }
        }
    }


    @Override
    protected TbCreateAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        TbCreateAlarmNodeConfiguration nodeConfiguration = TbNodeUtils.convert(configuration, TbCreateAlarmNodeConfiguration.class);
        relationTypes = nodeConfiguration.getRelationTypes();
        return nodeConfiguration;
    }

    @Override
    protected ListenableFuture<TbAlarmResult> processAlarm(TbContext ctx, TbMsg msg) {
        String alarmType;
        final Alarm msgAlarm;

        if (!config.isUseMessageAlarmData()) {
            alarmType = TbNodeUtils.processPattern(this.config.getAlarmType(), msg);
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

        Alarm existingAlarm = ctx.getAlarmService().findLatestActiveByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), alarmType);
        if (existingAlarm == null || existingAlarm.getStatus().isCleared()) {
            return createNewAlarm(ctx, msg, msgAlarm);
        } else {
            return updateAlarm(ctx, msg, existingAlarm, msgAlarm);
        }
    }

    private Alarm getAlarmFromMessage(TbContext ctx, TbMsg msg) throws IOException {
        Alarm msgAlarm;
        msgAlarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
        msgAlarm.setTenantId(ctx.getTenantId());
        if (msgAlarm.getOriginator() == null) {
            msgAlarm.setOriginator(msg.getOriginator());
        }
        return msgAlarm;
    }

    private ListenableFuture<TbAlarmResult> createNewAlarm(TbContext ctx, TbMsg msg, Alarm msgAlarm) {
        ListenableFuture<JsonNode> asyncDetails;
        boolean buildDetails = !config.isUseMessageAlarmData() || config.isOverwriteAlarmDetails();
        if (buildDetails) {
            ctx.logJsEvalRequest();
            asyncDetails = buildAlarmDetails(ctx, msg, null);
        } else {
            asyncDetails = Futures.immediateFuture(null);
        }
        ListenableFuture<Alarm> asyncAlarm = Futures.transform(asyncDetails, details -> {
            if (buildDetails) {
                ctx.logJsEvalResponse();
            }
            Alarm newAlarm;
            if (msgAlarm != null) {
                newAlarm = msgAlarm;
                if (buildDetails) {
                    newAlarm.setDetails(details);
                }
            } else {
                newAlarm = buildAlarm(msg, details, ctx.getTenantId());
            }
            return newAlarm;
        }, MoreExecutors.directExecutor());
        ListenableFuture<AlarmApiCallResult> asyncCreated = Futures.transform(asyncAlarm,
                alarm -> ctx.getAlarmService().createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm)), ctx.getDbCallbackExecutor());
        return Futures.transform(asyncCreated, TbAlarmResult::fromAlarmResult, MoreExecutors.directExecutor());
    }

    private ListenableFuture<TbAlarmResult> updateAlarm(TbContext ctx, TbMsg msg, Alarm existingAlarm, Alarm msgAlarm) {
        ListenableFuture<JsonNode> asyncDetails;
        boolean buildDetails = !config.isUseMessageAlarmData() || config.isOverwriteAlarmDetails();
        if (buildDetails) {
            ctx.logJsEvalRequest();
            asyncDetails = buildAlarmDetails(ctx, msg, existingAlarm.getDetails());
        } else {
            asyncDetails = Futures.immediateFuture(null);
        }
        ListenableFuture<AlarmApiCallResult> asyncUpdated = Futures.transform(asyncDetails, details -> {
            if (buildDetails) {
                ctx.logJsEvalResponse();
            }
            if (msgAlarm != null) {
                existingAlarm.setSeverity(msgAlarm.getSeverity());
                existingAlarm.setPropagate(msgAlarm.isPropagate());
                existingAlarm.setPropagateToOwner(msgAlarm.isPropagateToOwner());
                existingAlarm.setPropagateToOwnerHierarchy(msgAlarm.isPropagateToOwnerHierarchy());
                existingAlarm.setPropagateToTenant(msgAlarm.isPropagateToTenant());
                existingAlarm.setPropagateRelationTypes(msgAlarm.getPropagateRelationTypes());
                if (buildDetails) {
                    existingAlarm.setDetails(details);
                } else {
                    existingAlarm.setDetails(msgAlarm.getDetails());
                }
            } else {
                existingAlarm.setSeverity(processAlarmSeverity(msg));
                existingAlarm.setPropagate(config.isPropagate());
                existingAlarm.setPropagateToOwner(config.isPropagateToOwner());
                existingAlarm.setPropagateToOwnerHierarchy(config.isPropagateToOwnerHierarchy());
                existingAlarm.setPropagateToTenant(config.isPropagateToTenant());
                existingAlarm.setPropagateRelationTypes(relationTypes);
                existingAlarm.setDetails(details);
            }
            existingAlarm.setEndTs(System.currentTimeMillis());
            return ctx.getAlarmService().updateAlarm(AlarmUpdateRequest.fromAlarm(existingAlarm));
        }, ctx.getDbCallbackExecutor());
        return Futures.transform(asyncUpdated, TbAlarmResult::fromAlarmResult, MoreExecutors.directExecutor());
    }

    private Alarm buildAlarm(TbMsg msg, JsonNode details, TenantId tenantId) {
        long ts = msg.getMetaDataTs();
        return Alarm.builder()
                .tenantId(tenantId)
                .originator(msg.getOriginator())
                .cleared(false)
                .acknowledged(false)
                .severity(this.config.isDynamicSeverity() ? processAlarmSeverity(msg) : notDynamicAlarmSeverity)
                .propagate(config.isPropagate())
                .propagateToOwner(config.isPropagateToOwner())
                .propagateToTenant(config.isPropagateToTenant())
                .propagateToOwnerHierarchy(config.isPropagateToOwnerHierarchy())
                .type(TbNodeUtils.processPattern(this.config.getAlarmType(), msg))
                .propagateRelationTypes(relationTypes)
                .startTs(ts)
                .endTs(ts)
                .details(details)
                .build();
    }

    private AlarmSeverity processAlarmSeverity(TbMsg msg) {
        AlarmSeverity severity = EnumUtils.getEnum(AlarmSeverity.class, TbNodeUtils.processPattern(this.config.getSeverity(), msg));
        if (severity == null) {
            throw new RuntimeException("Used incorrect pattern or Alarm Severity not included in message");
        }
        return severity;
    }

}
