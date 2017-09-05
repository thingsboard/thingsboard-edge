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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.parser.ParseException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.extensions.api.component.Processor;
import org.thingsboard.server.extensions.api.rules.*;
import org.thingsboard.server.extensions.core.utils.VelocityUtils;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Processor(name = "(Deprecated) Alarm Deduplication Processor", descriptor = "AlarmDeduplicationProcessorDescriptor.json",
        configuration = AlarmDeduplicationProcessorConfiguration.class)
@Slf4j
public class AlarmDeduplicationProcessor extends SimpleRuleLifecycleComponent
        implements RuleProcessor<AlarmDeduplicationProcessorConfiguration> {

    public static final String IS_NEW_ALARM = "isNewAlarm";
    private ObjectMapper mapper = new ObjectMapper();
    private AlarmDeduplicationProcessorConfiguration configuration;
    private Template alarmIdTemplate;
    private Template alarmBodyTemplate;

    @Override
    public void init(AlarmDeduplicationProcessorConfiguration configuration) {
        this.configuration = configuration;
        try {
            this.alarmIdTemplate = VelocityUtils.create(configuration.getAlarmIdTemplate(), "Alarm Id Template");
            this.alarmBodyTemplate = VelocityUtils.create(configuration.getAlarmBodyTemplate(), "Alarm Body Template");
        } catch (ParseException e) {
            log.error("Failed to create templates based on provided configuration!", e);
            throw new RuntimeException("Failed to create templates based on provided configuration!", e);
        }
    }

    @Override
    public RuleProcessingMetaData process(RuleContext ctx, ToDeviceActorMsg msg) throws RuleException {
        RuleProcessingMetaData md = new RuleProcessingMetaData();
        VelocityContext context = VelocityUtils.createContext(ctx.getDeviceMetaData(), msg.getPayload());
        String alarmId = VelocityUtils.merge(alarmIdTemplate, context);
        String alarmBody = VelocityUtils.merge(alarmBodyTemplate, context);
        Optional<Event> existingEvent = ctx.findEvent(DataConstants.ALARM, alarmId);
        if (!existingEvent.isPresent()) {
            Event event = new Event();
            event.setType(DataConstants.ALARM);
            event.setUid(alarmId);
            event.setBody(mapper.createObjectNode().put("body", alarmBody));
            Optional<Event> savedEvent = ctx.saveIfNotExists(event);
            if (savedEvent.isPresent()) {
                log.info("New Alarm detected: '{}'", alarmId);
                md.put(IS_NEW_ALARM, Boolean.TRUE);
                md.put("alarmId", alarmId);
                md.put("alarmBody", alarmBody);
                for (Object key : context.getKeys()) {
                    md.put(key.toString(), context.get(key.toString()));
                }
            }
        }
        return md;
    }
}
