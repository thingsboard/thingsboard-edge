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
package org.thingsboard.server.extensions.core.plugin.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.extensions.api.component.EmptyComponentConfiguration;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RestMsgHandler;
import org.thingsboard.server.extensions.api.plugins.handlers.RpcMsgHandler;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.handlers.WebsocketMsgHandler;
import org.thingsboard.server.extensions.core.action.telemetry.TelemetryPluginAction;
import org.thingsboard.server.extensions.core.plugin.telemetry.handlers.TelemetryRestMsgHandler;
import org.thingsboard.server.extensions.core.plugin.telemetry.handlers.TelemetryRpcMsgHandler;
import org.thingsboard.server.extensions.core.plugin.telemetry.handlers.TelemetryRuleMsgHandler;
import org.thingsboard.server.extensions.core.plugin.telemetry.handlers.TelemetryWebsocketMsgHandler;

@Plugin(name = "Telemetry Plugin", actions = {TelemetryPluginAction.class})
@Slf4j
public class TelemetryStoragePlugin extends AbstractPlugin<EmptyComponentConfiguration> {

    private final TelemetryRestMsgHandler restMsgHandler;
    private final TelemetryRuleMsgHandler ruleMsgHandler;
    private final TelemetryWebsocketMsgHandler websocketMsgHandler;
    private final TelemetryRpcMsgHandler rpcMsgHandler;
    private final SubscriptionManager subscriptionManager;

    public TelemetryStoragePlugin() {
        this.subscriptionManager = new SubscriptionManager();
        this.restMsgHandler = new TelemetryRestMsgHandler(subscriptionManager);
        this.ruleMsgHandler = new TelemetryRuleMsgHandler(subscriptionManager);
        this.websocketMsgHandler = new TelemetryWebsocketMsgHandler(subscriptionManager);
        this.rpcMsgHandler = new TelemetryRpcMsgHandler(subscriptionManager);
        this.subscriptionManager.setWebsocketHandler(this.websocketMsgHandler);
        this.subscriptionManager.setRpcHandler(this.rpcMsgHandler);
    }

    @Override
    public void init(EmptyComponentConfiguration configuration) {

    }

    @Override
    protected RestMsgHandler getRestMsgHandler() {
        return restMsgHandler;
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return ruleMsgHandler;
    }

    @Override
    protected WebsocketMsgHandler getWebsocketMsgHandler() {
        return websocketMsgHandler;
    }

    @Override
    protected RpcMsgHandler getRpcMsgHandler() {
        return rpcMsgHandler;
    }

    @Override
    public void onServerAdded(PluginContext ctx, ServerAddress server) {
        subscriptionManager.onClusterUpdate(ctx);
    }

    @Override
    public void onServerRemoved(PluginContext ctx, ServerAddress server) {
        subscriptionManager.onClusterUpdate(ctx);
    }


    @Override
    public void resume(PluginContext ctx) {
        log.info("Plugin activated!");
    }

    @Override
    public void suspend(PluginContext ctx) {
        log.info("Plugin suspended!");
    }

    @Override
    public void stop(PluginContext ctx) {
        subscriptionManager.clear();
        websocketMsgHandler.clear(ctx);
        log.info("Plugin stopped!");
    }
}
