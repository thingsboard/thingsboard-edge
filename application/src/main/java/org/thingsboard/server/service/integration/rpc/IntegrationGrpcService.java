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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.rule.engine.api.util.DonAsynchron;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.gen.integration.ConnectRequestMsg;
import org.thingsboard.server.gen.integration.ConnectResponseCode;
import org.thingsboard.server.gen.integration.ConnectResponseMsg;
import org.thingsboard.server.gen.integration.ConverterConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class IntegrationGrpcService extends IntegrationTransportGrpc.IntegrationTransportImplBase implements IntegrationRpcService {

    public static final ObjectMapper mapper = new ObjectMapper();

    @Value("${integrations.remote.rpc.service.port}")
    private int rpcPort;
    @Value("${integrations.remote.rpc.service.cert}")
    private String certFileResource;
    @Value("${integrations.remote.rpc.service.privateKey}")
    private String privateKeyResource;

    @Autowired
    private PlatformIntegrationService platformIntegrationService;
    @Autowired
    private ConverterService converterService;

    private Server server;

    @PostConstruct
    public void init() {
        log.info("Initializing RPC service!");
        File certFile;
        File privateKeyFile;
        /*try {
            certFile = new File(Resources.getResource(certFileResource).toURI());
            privateKeyFile = new File(Resources.getResource(privateKeyResource).toURI());
        } catch (Exception e) {
            log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
            throw new RuntimeException("Unable to set up SSL context!", e);
        }*/
        server = ServerBuilder
                .forPort(rpcPort)
//                .useTransportSecurity(certFile, privateKeyFile)
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
        responseObserver.onNext((ConnectResponseMsg) validateConnect(request).getResult());
        responseObserver.onCompleted();
    }

    private DeferredResult<ConnectResponseMsg> validateConnect(ConnectRequestMsg request) {
        DeferredResult<ConnectResponseMsg> deferredResult = new DeferredResult<>();
        ListenableFuture<ThingsboardPlatformIntegration> future = platformIntegrationService.getIntegrationByRoutingKey(request.getIntegrationRoutingKey());
        DonAsynchron.withCallback(future, platformIntegration -> {
            Integration integration = platformIntegration.getConfiguration();
            if (integration.isRemote() && integration.getSecret().equals(request.getIntegrationSecret())) {
                try {
                    Converter defaultConverter = converterService.findConverterById(integration.getTenantId(),
                            integration.getDefaultConverterId());
                    ConverterConfigurationProto defaultConverterProto = ConverterConfigurationProto.newBuilder()
                            .setConverterIdMSB(integration.getDefaultConverterId().getId().getMostSignificantBits())
                            .setConverterIdLSB(integration.getDefaultConverterId().getId().getLeastSignificantBits())
                            .setConfiguration(mapper.writeValueAsString(defaultConverter.getConfiguration()))
                            .setAdditionalInfo(mapper.writeValueAsString(defaultConverter.getAdditionalInfo()))
                            .build();

                    ConverterConfigurationProto downLinkConverterProto = ConverterConfigurationProto.newBuilder().getDefaultInstanceForType();
                    if (integration.getDownlinkConverterId() != null) {
                        Converter downlinkConverter = converterService.findConverterById(integration.getTenantId(),
                                integration.getDownlinkConverterId());
                        downLinkConverterProto = ConverterConfigurationProto.newBuilder()
                                .setConverterIdMSB(integration.getDownlinkConverterId().getId().getMostSignificantBits())
                                .setConverterIdLSB(integration.getDownlinkConverterId().getId().getLeastSignificantBits())
                                .setConfiguration(mapper.writeValueAsString(downlinkConverter.getConfiguration()))
                                .setAdditionalInfo(mapper.writeValueAsString(downlinkConverter.getAdditionalInfo()))
                                .build();
                    }

                    IntegrationConfigurationProto proto = IntegrationConfigurationProto.newBuilder()
                            .setTenantIdMSB(integration.getTenantId().getId().getMostSignificantBits())
                            .setTenantIdLSB(integration.getTenantId().getId().getLeastSignificantBits())
                            .setUplinkConverter(defaultConverterProto)
                            .setDownlinkConverter(downLinkConverterProto)
                            .setName(integration.getName())
                            .setRoutingKey(integration.getRoutingKey())
                            .setType(integration.getType().toString())
                            .setDebugMode(integration.isDebugMode())
                            .setConfiguration(mapper.writeValueAsString(integration.getConfiguration()))
                            .setAdditionalInfo(mapper.writeValueAsString(integration.getAdditionalInfo()))
                            .build();

                    deferredResult.setResult(ConnectResponseMsg.newBuilder()
                            .setResponseCode(ConnectResponseCode.ACCEPTED)
                            .setErrorMsg("")
                            .setConfiguration(proto).build());
                } catch (Exception e) {
                    deferredResult.setResult(ConnectResponseMsg.newBuilder()
                            .setResponseCode(ConnectResponseCode.SERVER_UNAVAILABLE)
                            .setErrorMsg("Failed to construct integration configuration proto!")
                            .setConfiguration(IntegrationConfigurationProto.newBuilder().getDefaultInstanceForType()).build());
                }
            }
            deferredResult.setResult(ConnectResponseMsg.newBuilder()
                    .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                    .setErrorMsg("Failed to validate the integration!")
                    .setConfiguration(IntegrationConfigurationProto.newBuilder().getDefaultInstanceForType()).build());
        }, throwable -> {
            log.warn("Failed to find the integration![{}]", request.getIntegrationRoutingKey(), throwable);
            deferredResult.setResult(ConnectResponseMsg.newBuilder()
                    .setResponseCode(ConnectResponseCode.SERVER_UNAVAILABLE)
                    .setErrorMsg("Failed to find the integration!")
                    .setConfiguration(IntegrationConfigurationProto.newBuilder().getDefaultInstanceForType()).build());
        });
        return deferredResult;
    }
}
