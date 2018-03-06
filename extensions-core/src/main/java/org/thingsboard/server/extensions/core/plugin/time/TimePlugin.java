/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.extensions.core.plugin.time;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.core.ToServerRpcRequestMsg;
import org.thingsboard.server.common.msg.core.ToServerRpcResponseMsg;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.RpcResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.core.action.rpc.RpcPluginAction;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Andrew Shvayka
 */
@Plugin(name = "Time Plugin", actions = {RpcPluginAction.class},
        descriptor = "TimePluginDescriptor.json", configuration = TimePluginConfiguration.class)
@Slf4j
public class TimePlugin extends AbstractPlugin<TimePluginConfiguration> implements RuleMsgHandler {

    private DateTimeFormatter formatter;
    private String format;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (msg.getPayload() instanceof ToServerRpcRequestMsg) {
            ToServerRpcRequestMsg request = (ToServerRpcRequestMsg) msg.getPayload();

            String reply;
            if (!StringUtils.isEmpty(format)) {
                reply = "\"" + formatter.format(ZonedDateTime.now()) + "\"";
            } else {
                reply = Long.toString(System.currentTimeMillis());
            }
            ToServerRpcResponseMsg response = new ToServerRpcResponseMsg(request.getRequestId(), "{\"time\":" + reply + "}");
            ctx.reply(new RpcResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId, response));
        } else {
            throw new RuntimeException("Not supported msg type: " + msg.getPayload().getClass() + "!");
        }
    }

    @Override
    public void init(TimePluginConfiguration configuration) {
        format = configuration.getTimeFormat();
        if (!StringUtils.isEmpty(format)) {
            formatter = DateTimeFormatter.ofPattern(format);
        }
    }

    @Override
    public void resume(PluginContext ctx) {
        //Do nothing
    }

    @Override
    public void suspend(PluginContext ctx) {
        //Do nothing
    }

    @Override
    public void stop(PluginContext ctx) {
        //Do nothing
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return this;
    }
}
