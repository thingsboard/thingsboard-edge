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
package org.thingsboard.server.extensions.core.action.template;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.parser.ParseException;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.FromDeviceRequestMsg;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
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
@Slf4j
public abstract class AbstractTemplatePluginAction<T extends TemplateActionConfiguration> extends SimpleRuleLifecycleComponent implements PluginAction<T> {
    protected T configuration;
    protected Template template;

    @Override
    public void init(T configuration) {
        this.configuration = configuration;
        try {
            this.template = VelocityUtils.create(configuration.getTemplate(), "Template");
        } catch (ParseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<RuleToPluginMsg> convert(RuleContext ctx, ToDeviceActorMsg msg, RuleProcessingMetaData deviceMsgMd) {
        FromDeviceRequestMsg payload;
        if (msg.getPayload() instanceof FromDeviceRequestMsg) {
            payload = (FromDeviceRequestMsg) msg.getPayload();
        } else {
            throw new IllegalArgumentException("Action does not support messages of type: " + msg.getPayload().getMsgType());
        }
        return buildRuleToPluginMsg(ctx, msg, payload);
    }

    @Override
    public Optional<ToDeviceMsg> convert(PluginToRuleMsg<?> response) {
        if (response instanceof ResponsePluginToRuleMsg) {
            return Optional.of(((ResponsePluginToRuleMsg) response).getPayload());
        }
        return Optional.empty();
    }

    protected String getMsgBody(RuleContext ctx, ToDeviceActorMsg msg) {
        log.trace("Creating context for: {} and payload {}", ctx.getDeviceMetaData(), msg.getPayload());
        VelocityContext context = VelocityUtils.createContext(ctx.getDeviceMetaData(), msg.getPayload());
        return VelocityUtils.merge(template, context);
    }

    abstract protected Optional<RuleToPluginMsg> buildRuleToPluginMsg(RuleContext ctx,
                                                                         ToDeviceActorMsg msg,
                                                                         FromDeviceRequestMsg payload);

    @Override
    public boolean isOneWayAction() {
        return !configuration.isSync();
    }
}
