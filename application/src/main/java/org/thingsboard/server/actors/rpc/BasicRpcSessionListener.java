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
package org.thingsboard.server.actors.rpc;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.cluster.ToAllNodesMsg;
import org.thingsboard.server.common.msg.core.ToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.extensions.api.device.ToDeviceActorNotificationMsg;
import org.thingsboard.server.extensions.api.plugins.msg.*;
import org.thingsboard.server.extensions.api.plugins.rpc.PluginRpcMsg;
import org.thingsboard.server.extensions.api.plugins.rpc.RpcMsg;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.cluster.rpc.GrpcSession;
import org.thingsboard.server.service.cluster.rpc.GrpcSessionListener;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class BasicRpcSessionListener implements GrpcSessionListener {

    public static final String SESSION_RECEIVED_SESSION_ACTOR_MSG = "{} session [{}] received session actor msg {}";
    private final ActorSystemContext context;
    private final ActorService service;
    private final ActorRef manager;
    private final ActorRef self;

    public BasicRpcSessionListener(ActorSystemContext context, ActorRef manager, ActorRef self) {
        this.context = context;
        this.service = context.getActorService();
        this.manager = manager;
        this.self = self;
    }

    @Override
    public void onConnected(GrpcSession session) {
        log.info("{} session started -> {}", getType(session), session.getRemoteServer());
        if (!session.isClient()) {
            manager.tell(new RpcSessionConnectedMsg(session.getRemoteServer(), session.getSessionId()), self);
        }
    }

    @Override
    public void onDisconnected(GrpcSession session) {
        log.info("{} session closed -> {}", getType(session), session.getRemoteServer());
        manager.tell(new RpcSessionDisconnectedMsg(session.isClient(), session.getRemoteServer()), self);
    }

    @Override
    public void onToPluginRpcMsg(GrpcSession session, ClusterAPIProtos.ToPluginRpcMessage msg) {
        if (log.isTraceEnabled()) {
            log.trace("{} session [{}] received plugin msg {}", getType(session), session.getRemoteServer(), msg);
        }
        service.onMsg(convert(session.getRemoteServer(), msg));
    }

    @Override
    public void onToDeviceActorRpcMsg(GrpcSession session, ClusterAPIProtos.ToDeviceActorRpcMessage msg) {
        log.trace("{} session [{}] received device actor msg {}", getType(session), session.getRemoteServer(), msg);
        service.onMsg((ToDeviceActorMsg) deserialize(msg.getData().toByteArray()));
    }

    @Override
    public void onToDeviceActorNotificationRpcMsg(GrpcSession session, ClusterAPIProtos.ToDeviceActorNotificationRpcMessage msg) {
        log.trace("{} session [{}] received device actor notification msg {}", getType(session), session.getRemoteServer(), msg);
        service.onMsg((ToDeviceActorNotificationMsg) deserialize(msg.getData().toByteArray()));
    }

    @Override
    public void onToDeviceSessionActorRpcMsg(GrpcSession session, ClusterAPIProtos.ToDeviceSessionActorRpcMessage msg) {
        log.trace(SESSION_RECEIVED_SESSION_ACTOR_MSG, getType(session), session.getRemoteServer(), msg);
        service.onMsg((ToDeviceSessionActorMsg) deserialize(msg.getData().toByteArray()));
    }

    @Override
    public void onToDeviceRpcRequestRpcMsg(GrpcSession session, ClusterAPIProtos.ToDeviceRpcRequestRpcMessage msg) {
        log.trace(SESSION_RECEIVED_SESSION_ACTOR_MSG, getType(session), session.getRemoteServer(), msg);
        service.onMsg(deserialize(session.getRemoteServer(), msg));
    }

    @Override
    public void onFromDeviceRpcResponseRpcMsg(GrpcSession session, ClusterAPIProtos.ToPluginRpcResponseRpcMessage msg) {
        log.trace(SESSION_RECEIVED_SESSION_ACTOR_MSG, getType(session), session.getRemoteServer(), msg);
        service.onMsg(deserialize(session.getRemoteServer(), msg));
    }

    @Override
    public void onToAllNodesRpcMessage(GrpcSession session, ClusterAPIProtos.ToAllNodesRpcMessage msg) {
        log.trace(SESSION_RECEIVED_SESSION_ACTOR_MSG, getType(session), session.getRemoteServer(), msg);
        service.onMsg((ToAllNodesMsg) deserialize(msg.getData().toByteArray()));
    }

    @Override
    public void onError(GrpcSession session, Throwable t) {
        log.warn("{} session got error -> {}", getType(session), session.getRemoteServer(), t);
        manager.tell(new RpcSessionClosedMsg(session.isClient(), session.getRemoteServer()), self);
        session.close();
    }

    private static String getType(GrpcSession session) {
        return session.isClient() ? "Client" : "Server";
    }

    private static PluginRpcMsg convert(ServerAddress serverAddress, ClusterAPIProtos.ToPluginRpcMessage msg) {
        ClusterAPIProtos.PluginAddress address = msg.getAddress();
        TenantId tenantId = new TenantId(toUUID(address.getTenantId()));
        PluginId pluginId = new PluginId(toUUID(address.getPluginId()));
        RpcMsg rpcMsg = new RpcMsg(serverAddress, msg.getClazz(), msg.getData().toByteArray());
        return new PluginRpcMsg(tenantId, pluginId, rpcMsg);
    }

    private static UUID toUUID(ClusterAPIProtos.Uid uid) {
        return new UUID(uid.getPluginUuidMsb(), uid.getPluginUuidLsb());
    }

    private static ToDeviceRpcRequestPluginMsg deserialize(ServerAddress serverAddress, ClusterAPIProtos.ToDeviceRpcRequestRpcMessage msg) {
        ClusterAPIProtos.PluginAddress address = msg.getAddress();
        TenantId pluginTenantId = new TenantId(toUUID(address.getTenantId()));
        PluginId pluginId = new PluginId(toUUID(address.getPluginId()));

        TenantId deviceTenantId = new TenantId(toUUID(msg.getDeviceTenantId()));
        DeviceId deviceId = new DeviceId(toUUID(msg.getDeviceId()));

        ToDeviceRpcRequestBody requestBody = new ToDeviceRpcRequestBody(msg.getMethod(), msg.getParams());
        ToDeviceRpcRequest request = new ToDeviceRpcRequest(toUUID(msg.getMsgId()), null, deviceTenantId, deviceId, msg.getOneway(), msg.getExpTime(), requestBody);

        return new ToDeviceRpcRequestPluginMsg(serverAddress, pluginId, pluginTenantId, request);
    }

    private static ToPluginRpcResponseDeviceMsg deserialize(ServerAddress serverAddress, ClusterAPIProtos.ToPluginRpcResponseRpcMessage msg) {
        ClusterAPIProtos.PluginAddress address = msg.getAddress();
        TenantId pluginTenantId = new TenantId(toUUID(address.getTenantId()));
        PluginId pluginId = new PluginId(toUUID(address.getPluginId()));

        RpcError error = !StringUtils.isEmpty(msg.getError()) ? RpcError.valueOf(msg.getError()) : null;
        FromDeviceRpcResponse response = new FromDeviceRpcResponse(toUUID(msg.getMsgId()), msg.getResponse(), error);
        return new ToPluginRpcResponseDeviceMsg(pluginId, pluginTenantId, response);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T deserialize(byte[] data) {
        return (T) SerializationUtils.deserialize(data);
    }

}
