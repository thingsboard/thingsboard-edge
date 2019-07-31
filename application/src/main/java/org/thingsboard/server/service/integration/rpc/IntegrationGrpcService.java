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

import com.google.common.io.Resources;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;
import org.thingsboard.server.service.cluster.discovery.ServerInstanceService;
import org.thingsboard.server.service.cluster.rpc.ClusterGrpcService;
import org.thingsboard.server.service.integration.IntegrationContextComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class IntegrationGrpcService extends IntegrationTransportGrpc.IntegrationTransportImplBase implements IntegrationRpcService {

    private final Map<IntegrationId, IntegrationGrpcSession> sessions = new ConcurrentHashMap<>();

    @Value("${integrations.rpc.port}")
    private int rpcPort;
    @Value("${integrations.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${integrations.rpc.ssl.cert}")
    private String certFileResource;
    @Value("${integrations.rpc.ssl.privateKey}")
    private String privateKeyResource;

    @Autowired
    private ServerInstanceService instanceService;
    @Autowired
    private IntegrationContextComponent ctx;
    @Autowired
    private RemoteIntegrationSessionService sessionsCache;

    private Server server;

    @PostConstruct
    public void init() {
        log.info("Initializing RPC service!");
        ServerBuilder builder = ServerBuilder.forPort(rpcPort).addService(this);
        if (sslEnabled) {
            try {
                File certFile = new File(Resources.getResource(certFileResource).toURI());
                File privateKeyFile = new File(Resources.getResource(privateKeyResource).toURI());
                builder.useTransportSecurity(certFile, privateKeyFile);
            } catch (Exception e) {
                log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
                throw new RuntimeException("Unable to set up SSL context!", e);
            }
        }
        server = builder.build();
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
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> responseObserver) {
        return new IntegrationGrpcSession(ctx, responseObserver, this::onIntegrationConnect, this::onIntegrationDisconnect).getInputStream();
    }

    private void onIntegrationConnect(IntegrationId integrationId, IntegrationGrpcSession integrationGrpcSession) {
        sessions.put(integrationId, integrationGrpcSession);
        sessionsCache.putIntegrationSession(integrationId, new IntegrationSession(instanceService.getSelf().getServerAddress()));
    }

    private void onIntegrationDisconnect(IntegrationId integrationId) {
        sessions.remove(integrationId);
        sessionsCache.removeIntegrationSession(integrationId);
    }

    @Override
    public void updateIntegration(Integration configuration) {
        //TODO: handle cluster mode
        for (Map.Entry<IntegrationId, IntegrationGrpcSession> entry : sessions.entrySet()) {
            if (entry.getValue().isConnected() && entry.getValue().getConfiguration().getId().equals(configuration.getId())) {
                entry.getValue().onConfigurationUpdate(configuration);
            }
        }
    }

    @Override
    public void updateConverter(Converter converter) {
        for (Map.Entry<IntegrationId, IntegrationGrpcSession> entry : sessions.entrySet()) {
            Integration configuration = entry.getValue().getConfiguration();
            if (entry.getValue().isConnected()
                    && (configuration.getDefaultConverterId().equals(converter.getId()) || configuration.getDownlinkConverterId().equals(converter.getId()))) {
                entry.getValue().onConverterUpdate(converter);
            }
        }
    }
}
