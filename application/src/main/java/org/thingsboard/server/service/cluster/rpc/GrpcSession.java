/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;

import java.io.Closeable;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
@Slf4j
public final class GrpcSession implements Closeable {
    private final UUID sessionId;
    private final boolean client;
    private final GrpcSessionListener listener;
    private StreamObserver<ClusterAPIProtos.ClusterMessage> inputStream;
    private StreamObserver<ClusterAPIProtos.ClusterMessage> outputStream;

    private boolean connected;
    private ServerAddress remoteServer;

    public GrpcSession(GrpcSessionListener listener) {
        this(null, listener);
    }

    public GrpcSession(ServerAddress remoteServer, GrpcSessionListener listener) {
        this.sessionId = UUID.randomUUID();
        this.listener = listener;
        if (remoteServer != null) {
            this.client = true;
            this.connected = true;
            this.remoteServer = remoteServer;
        } else {
            this.client = false;
        }
    }

    public void initInputStream() {
        this.inputStream = new StreamObserver<ClusterAPIProtos.ClusterMessage>() {
            @Override
            public void onNext(ClusterAPIProtos.ClusterMessage clusterMessage) {
                if (!connected && clusterMessage.getMessageType() == ClusterAPIProtos.MessageType.CONNECT_RPC_MESSAGE) {
                    connected = true;
                    ServerAddress rpcAddress = new ServerAddress(clusterMessage.getServerAddress().getHost(), clusterMessage.getServerAddress().getPort());
                    remoteServer = new ServerAddress(rpcAddress.getHost(), rpcAddress.getPort());
                    listener.onConnected(GrpcSession.this);
                }
                if (connected) {
                    listener.onReceiveClusterGrpcMsg(GrpcSession.this, clusterMessage);
                }
            }

            @Override
            public void onError(Throwable t) {
                listener.onError(GrpcSession.this, t);
            }

            @Override
            public void onCompleted() {
                outputStream.onCompleted();
                listener.onDisconnected(GrpcSession.this);
            }
        };
    }

    public void initOutputStream() {
        if (client) {
            listener.onConnected(GrpcSession.this);
        }
    }

    public void sendMsg(ClusterAPIProtos.ClusterMessage msg) {
        outputStream.onNext(msg);
    }

    public void onError(Throwable t) {
        outputStream.onError(t);
    }

    @Override
    public void close() {
        try {
            outputStream.onCompleted();
        } catch (IllegalStateException e) {
            log.debug("[{}] Failed to close output stream: {}", sessionId, e.getMessage());
        }
    }
}
