/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.FutureCallback;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.gen.edge.EdgeRpcServiceGrpc;
import org.thingsboard.server.gen.edge.RequestMsg;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges.rpc", value = "enabled", havingValue = "true")
public class EdgeGrpcService extends EdgeRpcServiceGrpc.EdgeRpcServiceImplBase implements EdgeRpcService {

    private final Map<EdgeId, EdgeGrpcSession> sessions = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${edges.rpc.port}")
    private int rpcPort;
    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${edges.rpc.ssl.cert}")
    private String certFileResource;
    @Value("${edges.rpc.ssl.private_key}")
    private String privateKeyResource;
    @Value("${edges.state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    @Autowired
    private EdgeContextComponent ctx;

    @Autowired
    private TelemetrySubscriptionService tsSubService;

    private Server server;

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        log.info("Initializing Edge RPC service!");
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
        log.info("Going to start Edge RPC server using port: {}", rpcPort);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start Edge RPC server!", e);
            throw new RuntimeException("Failed to start Edge RPC server!");
        }
        log.info("Edge RPC service initialized!");
        executor = Executors.newSingleThreadExecutor();
        processHandleMessages();
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Override
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> outputStream) {
        return new EdgeGrpcSession(ctx, outputStream, this::onEdgeConnect, this::onEdgeDisconnect, mapper).getInputStream();
    }

    @Override
    public void updateEdge(Edge edge) {
        EdgeGrpcSession session = sessions.get(edge.getId());
        if (session != null && session.isConnected()) {
            session.onConfigurationUpdate(edge);
        }
    }

    @Override
    public void deleteEdge(EdgeId edgeId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null && session.isConnected()) {
            session.close();
            sessions.remove(edgeId);
        }
    }

    private void onEdgeConnect(EdgeId edgeId, EdgeGrpcSession edgeGrpcSession) {
        sessions.put(edgeId, edgeGrpcSession);
        save(edgeId, DefaultDeviceStateService.ACTIVITY_STATE, true);
        save(edgeId, DefaultDeviceStateService.LAST_CONNECT_TIME, System.currentTimeMillis());
    }

    private void processHandleMessages() {
        executor.submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    for (EdgeGrpcSession session : sessions.values()) {
                        session.processHandleMessages();
                    }
                } catch (Exception e) {
                    log.warn("Failed to process messages handling!", e);
                }
            }
        });
    }

    private void onEdgeDisconnect(EdgeId edgeId) {
        sessions.remove(edgeId);
        save(edgeId, DefaultDeviceStateService.ACTIVITY_STATE, false);
        save(edgeId, DefaultDeviceStateService.LAST_DISCONNECT_TIME, System.currentTimeMillis());
    }

    private void save(EdgeId edgeId, String key, long value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    TenantId.SYS_TENANT_ID, edgeId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry(key, value))),
                    new AttributeSaveCallback(edgeId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, edgeId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(edgeId, key, value));
        }
    }

    private void save(EdgeId edgeId, String key, boolean value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    TenantId.SYS_TENANT_ID, edgeId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))),
                    new AttributeSaveCallback(edgeId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, edgeId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(edgeId, key, value));
        }
    }

    private static class AttributeSaveCallback implements FutureCallback<Void> {
        private final EdgeId edgeId;
        private final String key;
        private final Object value;

        AttributeSaveCallback(EdgeId edgeId, String key, Object value) {
            this.edgeId = edgeId;
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@javax.annotation.Nullable Void result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", edgeId, key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", edgeId, key, value, t);
        }
    }
}
