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
package org.thingsboard.integration.rpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.exception.IntegrationConnectionException;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.gen.integration.ConnectRequestMsg;
import org.thingsboard.server.gen.integration.ConnectResponseCode;
import org.thingsboard.server.gen.integration.ConnectResponseMsg;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;
import org.thingsboard.server.gen.integration.MessageType;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.gen.integration.UplinkResponseMsg;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Slf4j
public class IntegrationGrpcClient implements IntegrationRpcClient {

    @Value("${rpc.host}")
    private String rpcHost;
    @Value("${rpc.port}")
    private int rpcPort;
    @Value("${rpc.timeout}")
    private int timeoutSecs;
    @Value("${rpc.cert}")
    private String certResource;

    @Autowired
    private EventStorage eventStorage;

    private ManagedChannel channel;
    private StreamObserver<RequestMsg> inputStream;

    @Override
    public void connect(String integrationKey, String integrationSecret, Consumer<IntegrationConfigurationProto> onSuccess, Consumer<Exception> onError) {
        try {
            channel = NettyChannelBuilder
                    .forAddress(rpcHost, rpcPort)
//                    .sslContext(GrpcSslContexts.forClient().trustManager(new File(Resources.getResource(certResource).toURI())).build())
                    .usePlaintext()
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize channel!", e);
            throw new RuntimeException(e);
        }
        IntegrationTransportGrpc.IntegrationTransportStub stub = IntegrationTransportGrpc.newStub(channel);
        log.info("[{}] Sending a connect request to the TB!", integrationKey);
        this.inputStream = stub.handleMsgs(initOutputStream(integrationKey, onSuccess, onError));
        this.inputStream.onNext(RequestMsg.newBuilder()
                .setMessageType(MessageType.CONNECT_RPC_MESSAGE)
                .setConnectRequestMsg(ConnectRequestMsg.newBuilder().setIntegrationRoutingKey(integrationKey).setIntegrationSecret(integrationSecret).build())
                .build());
    }

    private StreamObserver<ResponseMsg> initOutputStream(String integrationKey, Consumer<IntegrationConfigurationProto> onSuccess, Consumer<Exception> onError) {
        return new StreamObserver<ResponseMsg>() {
            @Override
            public void onNext(ResponseMsg responseMsg) {
                if (responseMsg.hasConnectResponseMsg()) {
                    ConnectResponseMsg connectResponseMsg = responseMsg.getConnectResponseMsg();
                    if (connectResponseMsg.getResponseCode().equals(ConnectResponseCode.ACCEPTED)) {
                        log.info("[{}]: {}", integrationKey, connectResponseMsg.getConfiguration());
                        onSuccess.accept(connectResponseMsg.getConfiguration());
                    } else {
                        log.error("[{}] Failed to establish the connection! Code: {}. Error message: {}.", integrationKey, connectResponseMsg.getResponseCode(), connectResponseMsg.getErrorMsg());
                        onError.accept(new IntegrationConnectionException("Failed to establish the connection! Response code: " + connectResponseMsg.getResponseCode().name()));
                    }
                } else if (responseMsg.hasUplinkResponseMsg()) {
                    UplinkResponseMsg msg = responseMsg.getUplinkResponseMsg();
                    if (msg.getSuccess()) {
                        log.debug("[{}] Msg has been processed successfully! {}", integrationKey, msg);
                    } else {
                        log.error("[{}] Msg processing failed! Error msg: {}", integrationKey, msg.getErrorMsg());
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                if (t.getClass().isInstance(IntegrationConnectionException.class)) {
                    log.error("[{}] Failed to establish the connection!", integrationKey, t);
                    onError.accept(new RuntimeException(t));
                }
            }

            @Override
            public void onCompleted() {
                log.debug("[{}] The rpc session was completed!", integrationKey);
            }
        };
    }

    @Override
    public void disconnect() throws InterruptedException {
        if (channel != null) {
            channel.shutdown().awaitTermination(timeoutSecs, TimeUnit.SECONDS);
        }
    }

    @Override
    public void handleMsgs() {
        /*this.inputStream.onNext(RequestMsg.newBuilder()
                .setMessageType(MessageType.UPLINK_RPC_MESSAGE)
                .setUplinkMsg(UplinkMsg.newBuilder()
                        .setUplinkMsgId(0)
                        .addDeviceData(DeviceUplinkDataProto.newBuilder()
                                .setDeviceName("test")
                                .setDeviceType("test")
                                .setPostTelemetryMsg(PostTelemetryMsg.newBuilder()
                                        .addTsKvList(TsKvListProto.newBuilder()
                                                .setTs(System.currentTimeMillis())
                                                .addKv(KeyValueProto.newBuilder()
                                                        .setKey("key")
                                                        .setType(KeyValueType.STRING_V)
                                                        .setStringV("value1")
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build());*/
        // TODO: 7/4/19 use CountDownLatch?
        List<UplinkMsg> uplinkMsgList = eventStorage.readCurrentBatch();
        for (UplinkMsg msg : uplinkMsgList) {
            this.inputStream.onNext(RequestMsg.newBuilder()
                    .setMessageType(MessageType.UPLINK_RPC_MESSAGE)
                    .setUplinkMsg(msg)
                    .build());
        }
    }

}
