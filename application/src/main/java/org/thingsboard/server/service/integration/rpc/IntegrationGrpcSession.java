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
package org.thingsboard.server.service.integration.rpc;

import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.gen.integration.ConnectResponseMsg;
import org.thingsboard.server.gen.integration.MessageType;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;

import java.io.Closeable;
import java.util.Map;
import java.util.UUID;

@Data
@Slf4j
public final class IntegrationGrpcSession implements Closeable {

    private final UUID sessionId;
    private final IntegrationGrpcService rpcService;
    private final Map<StreamObserver<ResponseMsg>, IntegrationGrpcSession> sessions;

    private TenantId tenantId;
    private Integration integration;
    private StreamObserver<RequestMsg> inputStream;
    private StreamObserver<ResponseMsg> outputStream;
    private boolean connected;

    public IntegrationGrpcSession(IntegrationGrpcService rpcService, StreamObserver<ResponseMsg> outputStream, Map<StreamObserver<ResponseMsg>, IntegrationGrpcSession> sessions) {
        this.sessionId = UUID.randomUUID();
        this.rpcService = rpcService;
        this.outputStream = outputStream;
        this.sessions = sessions;
        initInputStream();
    }

    private void initInputStream() {
        this.inputStream = new StreamObserver<RequestMsg>() {
            @Override
            public void onNext(RequestMsg requestMsg) {
                if (!connected && requestMsg.getMessageType().equals(MessageType.CONNECT_RPC_MESSAGE)) {
                    ConnectResponseMsg responseMsg = rpcService.validateConnect(requestMsg.getConnectRequestMsg(), IntegrationGrpcSession.this);
                    tenantId = new TenantId(new UUID(responseMsg.getConfiguration().getTenantIdMSB(), responseMsg.getConfiguration().getTenantIdLSB()));

                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(responseMsg)
                            .build());
                    connected = true;
                }
                if (connected) {
                    if (requestMsg.getMessageType().equals(MessageType.UPLINK_RPC_MESSAGE) && requestMsg.hasUplinkMsg()) {
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setUplinkResponseMsg(rpcService.processUplinkMsg(tenantId, integration.getId(), requestMsg.getUplinkMsg()))
                                .build());
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Failed to deliver message from client!", t);
            }

            @Override
            public void onCompleted() {
                sessions.remove(outputStream);
                outputStream.onCompleted();
            }
        };
    }

    @Override
    public void close() {
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}] Failed to close output stream: {}", sessionId, e.getMessage());
        }
    }
}
