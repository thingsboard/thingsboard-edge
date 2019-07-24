/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
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
package org.thingsboard.server.service.cluster.rpc;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.actors.rpc.RpcBroadcastMsg;
import org.thingsboard.server.actors.rpc.RpcSessionCreateRequestMsg;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.gen.cluster.ClusterRpcServiceGrpc;
import org.thingsboard.server.service.cluster.discovery.ServerInstance;
import org.thingsboard.server.service.cluster.discovery.ServerInstanceService;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class ClusterGrpcService extends ClusterRpcServiceGrpc.ClusterRpcServiceImplBase implements ClusterRpcService {

    @Autowired
    private ServerInstanceService instanceService;

    @Autowired
    private DataDecodingEncodingService encodingService;

    private RpcMsgListener listener;

    private Server server;

    private ServerInstance instance;

    private ConcurrentMap<UUID, BlockingQueue<StreamObserver<ClusterAPIProtos.ClusterMessage>>> pendingSessionMap =
            new ConcurrentHashMap<>();

    public void init(RpcMsgListener listener) {
        this.listener = listener;
        log.info("Initializing RPC service!");
        instance = instanceService.getSelf();
        server = ServerBuilder.forPort(instance.getPort()).addService(this).build();
        log.info("Going to start RPC server using port: {}", instance.getPort());
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start RPC server!", e);
            throw new RuntimeException("Failed to start RPC server!");
        }
        log.info("RPC service initialized!");
    }

    @Override
    public void onSessionCreated(UUID msgUid, StreamObserver<ClusterAPIProtos.ClusterMessage> inputStream) {
        BlockingQueue<StreamObserver<ClusterAPIProtos.ClusterMessage>> queue = pendingSessionMap.remove(msgUid);
        if (queue != null) {
            try {
                queue.put(inputStream);
            } catch (InterruptedException e) {
                log.warn("Failed to report created session!");
                Thread.currentThread().interrupt();
            }
        } else {
            log.warn("Failed to lookup pending session!");
        }
    }

    @Override
    public StreamObserver<ClusterAPIProtos.ClusterMessage> handleMsgs(
            StreamObserver<ClusterAPIProtos.ClusterMessage> responseObserver) {
        log.info("Processing new session.");
        return createSession(new RpcSessionCreateRequestMsg(UUID.randomUUID(), null, responseObserver));
    }


    @PreDestroy
    public void stop() {
        if (server != null) {
            log.info("Going to onStop RPC server");
            server.shutdownNow();
            try {
                server.awaitTermination();
                log.info("RPC server stopped!");
            } catch (InterruptedException e) {
                log.warn("Failed to onStop RPC server!");
                Thread.currentThread().interrupt();
            }
        }
    }


    @Override
    public void broadcast(RpcBroadcastMsg msg) {
        listener.onBroadcastMsg(msg);
    }

    private StreamObserver<ClusterAPIProtos.ClusterMessage> createSession(RpcSessionCreateRequestMsg msg) {
        BlockingQueue<StreamObserver<ClusterAPIProtos.ClusterMessage>> queue = new ArrayBlockingQueue<>(1);
        pendingSessionMap.put(msg.getMsgUid(), queue);
        listener.onRpcSessionCreateRequestMsg(msg);
        try {
            StreamObserver<ClusterAPIProtos.ClusterMessage> observer = queue.take();
            log.info("Processed new session.");
            return observer;
        } catch (Exception e) {
            log.info("Failed to process session.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void tell(ClusterAPIProtos.ClusterMessage message) {
        listener.onSendMsg(message);
    }

    @Override
    public void tell(ServerAddress serverAddress, TbActorMsg actorMsg) {
        listener.onSendMsg(encodingService.convertToProtoDataMessage(serverAddress, actorMsg));
    }

    @Override
    public void tell(ServerAddress serverAddress, IntegrationDownlinkMsg msg) {
        ClusterAPIProtos.IntegrationDownlinkProto.Builder builder = ClusterAPIProtos.IntegrationDownlinkProto.newBuilder();
        builder.setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits());
        builder.setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits());

        builder.setIntegrationIdMSB(msg.getIntegrationId().getId().getMostSignificantBits());
        builder.setIntegrationIdLSB(msg.getIntegrationId().getId().getLeastSignificantBits());

        builder.setData(ByteString.copyFrom(TbMsg.toBytes(msg.getTbMsg())));

        tell(serverAddress, ClusterAPIProtos.MessageType.CLUSTER_INTEGRATION_DOWNLINK_MESSAGE, builder.build().toByteArray());
    }


    @Override
    public void tell(ServerAddress serverAddress, ClusterAPIProtos.MessageType msgType, byte[] data) {
        ClusterAPIProtos.ClusterMessage msg = ClusterAPIProtos.ClusterMessage
                .newBuilder()
                .setServerAddress(ClusterAPIProtos.ServerAddress
                        .newBuilder()
                        .setHost(serverAddress.getHost())
                        .setPort(serverAddress.getPort())
                        .build())
                .setMessageType(msgType)
                .setPayload(ByteString.copyFrom(data)).build();
        listener.onSendMsg(msg);
    }
}
