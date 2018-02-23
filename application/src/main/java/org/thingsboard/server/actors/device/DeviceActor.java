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
package org.thingsboard.server.actors.device;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.rule.RulesProcessedMsg;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.tenant.RuleChainDeviceMsg;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.extensions.api.device.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.extensions.api.device.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.extensions.api.device.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.extensions.api.device.ToDeviceActorNotificationMsg;
import org.thingsboard.server.extensions.api.plugins.msg.*;

public class DeviceActor extends ContextAwareActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final DeviceActorMessageProcessor processor;

    private DeviceActor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.processor = new DeviceActorMessageProcessor(systemContext, logger, tenantId, deviceId);
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof RuleChainDeviceMsg) {
            processor.process(context(), (RuleChainDeviceMsg) msg);
        } else if (msg instanceof RulesProcessedMsg) {
            processor.onRulesProcessedMsg(context(), (RulesProcessedMsg) msg);
        } else if (msg instanceof ToDeviceActorMsg) {
            processor.process(context(), (ToDeviceActorMsg) msg);
        } else if (msg instanceof ToDeviceActorNotificationMsg) {
            if (msg instanceof DeviceAttributesEventNotificationMsg) {
                processor.processAttributesUpdate(context(), (DeviceAttributesEventNotificationMsg) msg);
            } else if (msg instanceof ToDeviceRpcRequestPluginMsg) {
                processor.processRpcRequest(context(), (ToDeviceRpcRequestPluginMsg) msg);
            } else if (msg instanceof DeviceCredentialsUpdateNotificationMsg){
                processor.processCredentialsUpdate();
            } else if (msg instanceof DeviceNameOrTypeUpdateMsg){
                processor.processNameOrTypeUpdate((DeviceNameOrTypeUpdateMsg) msg);
            }
        } else if (msg instanceof TimeoutMsg) {
            processor.processTimeout(context(), (TimeoutMsg) msg);
        } else if (msg instanceof ClusterEventMsg) {
            processor.processClusterEventMsg((ClusterEventMsg) msg);
        } else {
            logger.debug("[{}][{}] Unknown msg type.", tenantId, deviceId, msg.getClass().getName());
        }
    }

    public static class ActorCreator extends ContextBasedCreator<DeviceActor> {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;
        private final DeviceId deviceId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId, DeviceId deviceId) {
            super(context);
            this.tenantId = tenantId;
            this.deviceId = deviceId;
        }

        @Override
        public DeviceActor create() throws Exception {
            return new DeviceActor(context, tenantId, deviceId);
        }
    }

}
