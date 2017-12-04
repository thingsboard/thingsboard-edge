/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.extensions.core.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.msg.core.UpdateAttributesRequest;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.extensions.api.component.Processor;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.api.rules.RuleProcessor;
import org.thingsboard.server.extensions.core.filter.NashornJsFilterEvaluator;
import org.thingsboard.server.extensions.core.utils.VelocityUtils;

import javax.script.Bindings;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Processor(name = "Alarm Processor", descriptor = "AlarmProcessorDescriptor.json",
        configuration = AlarmProcessorConfiguration.class)
@Slf4j
public class AlarmProcessor implements RuleProcessor<AlarmProcessorConfiguration> {

    static final String IS_NEW_ALARM = "isNewAlarm";
    static final String IS_EXISTING_ALARM = "isExistingAlarm";
    static final String IS_CLEARED_ALARM = "isClearedAlarm";
    static final String IS_NEW_OR_CLEARED_ALARM = "isNewOrClearedAlarm";


    protected NashornJsFilterEvaluator newAlarmEvaluator;
    protected NashornJsFilterEvaluator clearAlarmEvaluator;

    private ObjectMapper mapper = new ObjectMapper();
    private AlarmProcessorConfiguration configuration;
    private AlarmStatus status;
    private AlarmSeverity severity;
    private Template alarmTypeTemplate;
    private Template alarmDetailsTemplate;


    @Override
    public void init(AlarmProcessorConfiguration configuration) {
        this.configuration = configuration;
        try {
            this.alarmTypeTemplate = VelocityUtils.create(configuration.getAlarmTypeTemplate(), "Alarm Type Template");
            this.alarmDetailsTemplate = VelocityUtils.create(configuration.getAlarmDetailsTemplate(), "Alarm Details Template");
            this.status = AlarmStatus.valueOf(configuration.getAlarmStatus());
            this.severity = AlarmSeverity.valueOf(configuration.getAlarmSeverity());
            initEvaluators();
        } catch (Exception e) {
            log.error("Failed to create templates based on provided configuration!", e);
            throw new RuntimeException("Failed to create templates based on provided configuration!", e);
        }
    }

    @Override
    public void resume() {
        initEvaluators();
        log.debug("Resume method was called, but no impl provided!");
    }

    @Override
    public void suspend() {
        destroyEvaluators();
        log.debug("Suspend method was called, but no impl provided!");
    }

    @Override
    public void stop() {
        destroyEvaluators();
        log.debug("Stop method was called, but no impl provided!");
    }

    @Override
    public RuleProcessingMetaData process(RuleContext ctx, ToDeviceActorMsg wrapper) throws RuleException {
        RuleProcessingMetaData md = new RuleProcessingMetaData();

        FromDeviceMsg msg = wrapper.getPayload();
        Bindings bindings = buildBindings(ctx, msg);

        boolean isActiveAlarm;
        boolean isClearedAlarm;

        VelocityContext context = VelocityUtils.createContext(ctx.getDeviceMetaData(), msg);
        for (Object key : context.getKeys()) {
            md.put(key.toString(), context.get(key.toString()));
        }

        try {
            isActiveAlarm = newAlarmEvaluator.execute(bindings);
            isClearedAlarm = clearAlarmEvaluator.execute(bindings);
        } catch (ScriptException e) {
            log.debug("[{}] Failed to evaluate alarm expressions!", ctx.getRuleId(), e);
            throw new RuleException("Failed to evaluate alarm expressions!", e);
        }

        if (!isActiveAlarm && !isClearedAlarm) {
            log.debug("[{}] Incoming message do not trigger alarm", ctx.getRuleId());
            return md;
        }

        Alarm existing;
        if (isActiveAlarm) {
            existing = processActiveAlarm(ctx, msg, md);
        } else {
            existing = processInactiveAlarm(ctx, md, context);
        }

        if (existing != null) {
            md.put("alarmId", existing.getId().getId());
            md.put("alarmType", existing.getType());
            md.put("alarmSeverity", existing.getSeverity());
            try {
                if (!StringUtils.isEmpty(existing.getDetails())) {
                    md.put("alarmDetails", mapper.writeValueAsString(existing.getDetails()));
                } else {
                    md.put("alarmDetails", "{}");
                }
            } catch (JsonProcessingException e) {
                throw new RuleException("Failed to serialize alarm details", e);
            }
        }

        return md;
    }

    private Alarm processActiveAlarm(RuleContext ctx, FromDeviceMsg msg, RuleProcessingMetaData md) throws RuleException {
        Alarm alarm = buildAlarm(ctx, msg);
        if (configuration.isNewAlarmFlag()) {
            Optional<Alarm> oldAlarmOpt = ctx.findLatestAlarm(alarm.getOriginator(), alarm.getType());
            if (oldAlarmOpt.isPresent() && !oldAlarmOpt.get().getStatus().isCleared()) {
                try {
                    ctx.clearAlarm(oldAlarmOpt.get().getId(), oldAlarmOpt.get().getEndTs()).get();
                } catch (Exception e) {
                    throw new RuleException("Failed to clear old alarm", e);
                }
            }
        }
        Alarm existing = ctx.createOrUpdateAlarm(alarm);
        if (existing.getStartTs() == alarm.getStartTs()) {
            log.debug("[{}][{}] New Active Alarm detected", ctx.getRuleId(), existing.getId());
            md.put(IS_NEW_ALARM, Boolean.TRUE);
            md.put(IS_NEW_OR_CLEARED_ALARM, Boolean.TRUE);
        } else {
            log.debug("[{}][{}] Existing Active Alarm detected", ctx.getRuleId(), existing.getId());
            md.put(IS_EXISTING_ALARM, Boolean.TRUE);
        }
        return existing;
    }

    private Alarm processInactiveAlarm(RuleContext ctx, RuleProcessingMetaData md, VelocityContext context) throws RuleException {
        Alarm existing = null;
        String alarmType = VelocityUtils.merge(alarmTypeTemplate, context);
        Optional<Alarm> alarm = ctx.findLatestAlarm(ctx.getDeviceMetaData().getDeviceId(), alarmType);
        if (alarm.isPresent()) {
            ctx.clearAlarm(alarm.get().getId(), System.currentTimeMillis());
            log.debug("[{}][{}] Existing Active Alarm cleared");
            md.put(IS_CLEARED_ALARM, Boolean.TRUE);
            md.put(IS_NEW_OR_CLEARED_ALARM, Boolean.TRUE);
            existing = alarm.get();
        }
        return existing;
    }

    private Alarm buildAlarm(RuleContext ctx, FromDeviceMsg msg) throws RuleException {
        VelocityContext context = VelocityUtils.createContext(ctx.getDeviceMetaData(), msg);
        String alarmType = VelocityUtils.merge(alarmTypeTemplate, context);
        String alarmDetails = VelocityUtils.merge(alarmDetailsTemplate, context);

        Alarm alarm = new Alarm();
        alarm.setOriginator(ctx.getDeviceMetaData().getDeviceId());
        alarm.setType(alarmType);

        alarm.setStatus(status);
        alarm.setSeverity(severity);
        alarm.setPropagate(configuration.isAlarmPropagateFlag());

        try {
            alarm.setDetails(mapper.readTree(alarmDetails));
        } catch (IOException e) {
            log.debug("[{}] Failed to parse alarm details {} as json string after evaluation.", ctx.getRuleId(), e);
            throw new RuleException("Failed to parse alarm details as json string after evaluation!", e);
        }
        return alarm;
    }

    private Bindings buildBindings(RuleContext ctx, FromDeviceMsg msg) {
        Bindings bindings = NashornJsFilterEvaluator.getAttributeBindings(ctx.getDeviceMetaData().getDeviceAttributes());
        if (msg != null) {
            switch (msg.getMsgType()) {
                case POST_ATTRIBUTES_REQUEST:
                    bindings = NashornJsFilterEvaluator.updateBindings(bindings, (UpdateAttributesRequest) msg);
                    break;
                case POST_TELEMETRY_REQUEST:
                    TelemetryUploadRequest telemetryMsg = (TelemetryUploadRequest) msg;
                    for (List<KvEntry> entries : telemetryMsg.getData().values()) {
                        bindings = NashornJsFilterEvaluator.toBindings(bindings, entries);
                    }
                    break;
                default:
                    break;
            }
        }
        return bindings;
    }

    private void initEvaluators() {
        newAlarmEvaluator = new NashornJsFilterEvaluator(configuration.getNewAlarmExpression());
        clearAlarmEvaluator = new NashornJsFilterEvaluator(configuration.getClearAlarmExpression());
    }

    private void destroyEvaluators() {
        if (newAlarmEvaluator != null) {
            newAlarmEvaluator.destroy();
        }
        if (clearAlarmEvaluator != null) {
            clearAlarmEvaluator.destroy();
        }
    }
}
