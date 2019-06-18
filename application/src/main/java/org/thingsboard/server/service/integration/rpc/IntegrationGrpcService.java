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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.gen.integration.ConnectRequestMsg;
import org.thingsboard.server.gen.integration.ConnectResponseCode;
import org.thingsboard.server.gen.integration.ConnectResponseMsg;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class IntegrationGrpcService extends IntegrationTransportGrpc.IntegrationTransportImplBase implements IntegrationRpcService {

    @Value("${integrations.remote.rpc.port}")
    private int rpcPort;

    private Server server;

    @PostConstruct
    public void init() {
        log.info("Initializing RPC service!");
        server = ServerBuilder
                .forPort(rpcPort)
                .useTransportSecurity(new File("certChainFile.pem"), new File("privateKeyFile.pem")) // TODO: 6/18/19 improve
                .addService(this)
                .build();
        log.info("Going to start RPC server using port: {}", rpcPort);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start RPC server!", e);
            throw new RuntimeException("Failed to start RPC server!");
        }
        log.info("RPC service initialized!");
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Override
    public void connect(ConnectRequestMsg request, StreamObserver<ConnectResponseMsg> responseObserver) {
        responseObserver.onNext(validateConnect(request));
        responseObserver.onCompleted();
    }

    private ConnectResponseMsg validateConnect(ConnectRequestMsg request) {
        IntegrationId integrationId = new IntegrationId(new UUID(request.getIntegrationIdMSB(), request.getIntegrationIdLSB()));
        String secret = request.getIntegrationSecret();

        return ConnectResponseMsg.newBuilder()
                .setResponseCode(ConnectResponseCode.ACCEPTED)
                .setErrorMsg("")
                .setConfiguration(IntegrationConfigurationProto.newBuilder().getDefaultInstanceForType()).build();
    }
}
