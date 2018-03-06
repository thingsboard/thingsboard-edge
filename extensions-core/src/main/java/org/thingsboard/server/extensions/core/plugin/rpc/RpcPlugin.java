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
package org.thingsboard.server.extensions.core.plugin.rpc;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.DefaultRuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.handlers.RestMsgHandler;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.FromDeviceRpcResponse;
import org.thingsboard.server.extensions.api.plugins.msg.TimeoutMsg;
import org.thingsboard.server.extensions.core.action.rpc.ServerSideRpcCallAction;
import org.thingsboard.server.extensions.core.plugin.rpc.handlers.RpcRestMsgHandler;
import org.thingsboard.server.extensions.core.plugin.rpc.handlers.RpcRuleMsgHandler;

/**
 * @author Andrew Shvayka
 */
@Plugin(name = "RPC Plugin", actions = {ServerSideRpcCallAction.class}, descriptor = "RpcPluginDescriptor.json", configuration = RpcPluginConfiguration.class)
@Slf4j
public class RpcPlugin extends AbstractPlugin<RpcPluginConfiguration> {

    private final RpcManager rpcManager;
    private final RpcRestMsgHandler restMsgHandler;

    public RpcPlugin() {
        this.rpcManager = new RpcManager();
        this.restMsgHandler = new RpcRestMsgHandler(rpcManager);
        this.rpcManager.setRestHandler(restMsgHandler);
    }

    @Override
    public void process(PluginContext ctx, FromDeviceRpcResponse msg) {
        rpcManager.process(ctx, msg);
    }

    @Override
    public void process(PluginContext ctx, TimeoutMsg<?> msg) {
        rpcManager.process(ctx, msg);
    }

    @Override
    protected RestMsgHandler getRestMsgHandler() {
        return restMsgHandler;
    }

    @Override
    public void init(RpcPluginConfiguration configuration) {
        restMsgHandler.setDefaultTimeout(configuration.getDefaultTimeout());
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return new RpcRuleMsgHandler();
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
}
