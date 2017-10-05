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
package org.thingsboard.server.extensions.core.action.mail;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.parser.ParseException;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.component.Action;
import org.thingsboard.server.extensions.api.plugins.PluginAction;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.api.rules.SimpleRuleLifecycleComponent;
import org.thingsboard.server.extensions.core.utils.VelocityUtils;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Action(name = "Send Mail Action", descriptor = "SendMailActionDescriptor.json", configuration = SendMailActionConfiguration.class)
@Slf4j
public class SendMailAction extends SimpleRuleLifecycleComponent implements PluginAction<SendMailActionConfiguration> {

    private SendMailActionConfiguration configuration;
    private Optional<Template> fromTemplate;
    private Optional<Template> toTemplate;
    private Optional<Template> ccTemplate;
    private Optional<Template> bccTemplate;
    private Optional<Template> subjectTemplate;
    private Optional<Template> bodyTemplate;

    @Override
    public void init(SendMailActionConfiguration configuration) {
        this.configuration = configuration;
        try {
            fromTemplate = toTemplate(configuration.getFromTemplate(), "From Template");
            toTemplate = toTemplate(configuration.getToTemplate(), "To Template");
            ccTemplate = toTemplate(configuration.getCcTemplate(), "Cc Template");
            bccTemplate = toTemplate(configuration.getBccTemplate(), "Bcc Template");
            subjectTemplate = toTemplate(configuration.getSubjectTemplate(), "Subject Template");
            bodyTemplate = toTemplate(configuration.getBodyTemplate(), "Body Template");
        } catch (ParseException e) {
            log.error("Failed to create templates based on provided configuration!", e);
            throw new RuntimeException("Failed to create templates based on provided configuration!", e);
        }
    }

    private Optional<Template> toTemplate(String source, String name) throws ParseException {
        if (!StringUtils.isEmpty(source)) {
            return Optional.of(VelocityUtils.create(source, name));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<RuleToPluginMsg> convert(RuleContext ctx, ToDeviceActorMsg toDeviceActorMsg, RuleProcessingMetaData metadata) {
        String sendFlag = configuration.getSendFlag();
        if (StringUtils.isEmpty(sendFlag) || (Boolean) metadata.get(sendFlag).orElse(Boolean.FALSE)) {
            VelocityContext context = VelocityUtils.createContext(metadata);

            SendMailActionMsg.SendMailActionMsgBuilder builder = SendMailActionMsg.builder();
            fromTemplate.ifPresent(t -> builder.from(VelocityUtils.merge(t, context)));
            toTemplate.ifPresent(t -> builder.to(VelocityUtils.merge(t, context)));
            ccTemplate.ifPresent(t -> builder.cc(VelocityUtils.merge(t, context)));
            bccTemplate.ifPresent(t -> builder.bcc(VelocityUtils.merge(t, context)));
            subjectTemplate.ifPresent(t -> builder.subject(VelocityUtils.merge(t, context)));
            bodyTemplate.ifPresent(t -> builder.body(VelocityUtils.merge(t, context)));
            return Optional.of(new SendMailRuleToPluginActionMsg(toDeviceActorMsg.getTenantId(), toDeviceActorMsg.getCustomerId(), toDeviceActorMsg.getDeviceId(),
                    builder.build()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ToDeviceMsg> convert(PluginToRuleMsg<?> response) {
        if (response instanceof ResponsePluginToRuleMsg) {
            return Optional.of(((ResponsePluginToRuleMsg) response).getPayload());
        }
        return Optional.empty();
    }

    @Override
    public boolean isOneWayAction() {
        return true;
    }

}
