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
package org.thingsboard.server.extensions.core.action.rpc;

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
 * Created by ashvayka on 14.09.17.
 */
@Action(name = "Server Side RPC Call Action", descriptor = "ServerSideRpcCallActionDescriptor.json", configuration = ServerSideRpcCallActionConfiguration.class)
@Slf4j
public class ServerSideRpcCallAction extends SimpleRuleLifecycleComponent implements PluginAction<ServerSideRpcCallActionConfiguration> {

    private ServerSideRpcCallActionConfiguration configuration;
    private Optional<Template> deviceIdTemplate;
    private Optional<Template> fromDeviceRelationTemplate;
    private Optional<Template> toDeviceRelationTemplate;
    private Optional<Template> rpcCallMethodTemplate;
    private Optional<Template> rpcCallBodyTemplate;

    @Override
    public void init(ServerSideRpcCallActionConfiguration configuration) {
        this.configuration = configuration;
        try {
            deviceIdTemplate = toTemplate(configuration.getDeviceIdTemplate(), "Device Id Template");
            fromDeviceRelationTemplate = toTemplate(configuration.getFromDeviceRelationTemplate(), "From Device Relation Template");
            toDeviceRelationTemplate = toTemplate(configuration.getToDeviceRelationTemplate(), "To Device Relation Template");
            rpcCallMethodTemplate = toTemplate(configuration.getRpcCallMethodTemplate(), "RPC Call Method Template");
            rpcCallBodyTemplate = toTemplate(configuration.getRpcCallBodyTemplate(), "RPC Call Body Template");
        } catch (ParseException e) {
            log.error("Failed to create templates based on provided configuration!", e);
            throw new RuntimeException("Failed to create templates based on provided configuration!", e);
        }
    }

    @Override
    public Optional<RuleToPluginMsg<?>> convert(RuleContext ctx, ToDeviceActorMsg toDeviceActorMsg, RuleProcessingMetaData metadata) {
        String sendFlag = configuration.getSendFlag();
        if (StringUtils.isEmpty(sendFlag) || (Boolean) metadata.get(sendFlag).orElse(Boolean.FALSE)) {
            VelocityContext context = VelocityUtils.createContext(metadata);

            ServerSideRpcCallActionMsg.ServerSideRpcCallActionMsgBuilder builder = ServerSideRpcCallActionMsg.builder();

            deviceIdTemplate.ifPresent(t -> builder.deviceId(VelocityUtils.merge(t, context)));
            fromDeviceRelationTemplate.ifPresent(t -> builder.fromDeviceRelation(VelocityUtils.merge(t, context)));
            toDeviceRelationTemplate.ifPresent(t -> builder.toDeviceRelation(VelocityUtils.merge(t, context)));
            rpcCallMethodTemplate.ifPresent(t -> builder.rpcCallMethod(VelocityUtils.merge(t, context)));
            rpcCallBodyTemplate.ifPresent(t -> builder.rpcCallBody(VelocityUtils.merge(t, context)));
            builder.rpcCallTimeoutInSec(configuration.getRpcCallTimeoutInSec());
            return Optional.of(new ServerSideRpcCallRuleToPluginActionMsg(toDeviceActorMsg.getTenantId(), toDeviceActorMsg.getCustomerId(), toDeviceActorMsg.getDeviceId(),
                    builder.build()));
        } else {
            return Optional.empty();
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
