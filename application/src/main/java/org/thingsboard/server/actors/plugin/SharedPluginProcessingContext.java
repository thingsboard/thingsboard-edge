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
package org.thingsboard.server.actors.plugin;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.controller.plugin.PluginWebSocketMsgEndpoint;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.extensions.api.device.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.extensions.api.plugins.msg.TimeoutMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequest;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequestPluginMsg;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import scala.concurrent.duration.Duration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Slf4j
public final class SharedPluginProcessingContext {
    final ActorRef parentActor;
    final ActorRef currentActor;
    final ActorSystemContext systemContext;
    final PluginWebSocketMsgEndpoint msgEndpoint;
    final AssetService assetService;
    final DeviceService deviceService;
    final RuleService ruleService;
    final PluginService pluginService;
    final CustomerService customerService;
    final TenantService tenantService;
    final TimeseriesService tsService;
    final AttributesService attributesService;
    final ClusterRpcService rpcService;
    final ClusterRoutingService routingService;
    final RelationService relationService;
    final ConverterService converterService;
    final IntegrationService integrationService;
    final AuditLogService auditLogService;
    final PluginId pluginId;
    final TenantId tenantId;

    public SharedPluginProcessingContext(ActorSystemContext sysContext, TenantId tenantId, PluginId pluginId,
                                         ActorRef parentActor, ActorRef self) {
        super();
        this.tenantId = tenantId;
        this.pluginId = pluginId;
        this.parentActor = parentActor;
        this.currentActor = self;
        this.systemContext = sysContext;
        this.msgEndpoint = sysContext.getWsMsgEndpoint();
        this.tsService = sysContext.getTsService();
        this.attributesService = sysContext.getAttributesService();
        this.assetService = sysContext.getAssetService();
        this.deviceService = sysContext.getDeviceService();
        this.rpcService = sysContext.getRpcService();
        this.routingService = sysContext.getRoutingService();
        this.ruleService = sysContext.getRuleService();
        this.pluginService = sysContext.getPluginService();
        this.customerService = sysContext.getCustomerService();
        this.tenantService = sysContext.getTenantService();
        this.relationService = sysContext.getRelationService();
        this.converterService = sysContext.getConverterService();
        this.integrationService = sysContext.getIntegrationService();
        this.auditLogService = sysContext.getAuditLogService();
    }

    public PluginId getPluginId() {
        return pluginId;
    }

    public TenantId getPluginTenantId() {
        return tenantId;
    }

    public void toDeviceActor(DeviceAttributesEventNotificationMsg msg) {
        forward(msg.getDeviceId(), msg, rpcService::tell);
    }

    public void sendRpcRequest(ToDeviceRpcRequest msg) {
        log.trace("[{}] Forwarding msg {} to device actor!", pluginId, msg);
        ToDeviceRpcRequestPluginMsg rpcMsg = new ToDeviceRpcRequestPluginMsg(pluginId, tenantId, msg);
        forward(msg.getDeviceId(), rpcMsg, rpcService::tell);
    }

    private <T> void forward(DeviceId deviceId, T msg, BiConsumer<ServerAddress, T> rpcFunction) {
        Optional<ServerAddress> instance = routingService.resolveById(deviceId);
        if (instance.isPresent()) {
            log.trace("[{}] Forwarding msg {} to remote device actor!", pluginId, msg);
            rpcFunction.accept(instance.get(), msg);
        } else {
            log.trace("[{}] Forwarding msg {} to local device actor!", pluginId, msg);
            parentActor.tell(msg, ActorRef.noSender());
        }
    }

    public void scheduleTimeoutMsg(TimeoutMsg msg) {
        log.debug("Scheduling msg {} with delay {} ms", msg, msg.getTimeout());
        systemContext.getScheduler().scheduleOnce(
                Duration.create(msg.getTimeout(), TimeUnit.MILLISECONDS),
                currentActor,
                msg,
                systemContext.getActorSystem().dispatcher(),
                ActorRef.noSender());

    }

    public void persistError(String method, Exception e) {
        systemContext.persistError(tenantId, pluginId, method, e);
    }

    public ActorRef self() {
        return currentActor;
    }
}
