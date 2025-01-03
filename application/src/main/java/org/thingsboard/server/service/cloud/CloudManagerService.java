/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cloud;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.cloud.rpc.processor.CustomerCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EdgeCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantCloudProcessor;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@TbCoreComponent
public class CloudManagerService extends TbApplicationEventListener<PartitionChangeEvent> {

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";
    private static final String QUEUE_SEQ_ID_OFFSET_ATTR_KEY = "queueSeqIdOffset";
    private static final String QUEUE_TS_KV_START_TS_ATTR_KEY = "queueTsKvStartTs";
    private static final String QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY = "queueTsKvSeqIdOffset";

    private final AtomicReference<ScheduledExecutorService> rpcSchedulerRef = new AtomicReference<>();

    private final AtomicBoolean rpcConnectionScheduled = new AtomicBoolean(false);


    @Value("${cloud.routingKey}")
    private String routingKey;

    @Value("${cloud.secret}")
    private String routingSecret;

    @Value("${cloud.reconnect_timeout}")
    private long reconnectTimeoutMs;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private CloudEventService cloudEventService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private GeneralUplinkMessageService generalUplinkMessageService;

    @Autowired
    private TsUplinkMessageService tsUplinkMessageService;

    @Autowired
    private DownlinkMessageService downlinkMessageService;

    @Autowired
    private EdgeRpcClient edgeRpcClient;

    @Autowired
    private EdgeCloudProcessor edgeCloudProcessor;

    @Autowired
    private TenantCloudProcessor tenantProcessor;

    @Autowired
    private CustomerCloudProcessor customerProcessor;

    @Autowired
    private ConfigurableApplicationContext context;

    private EdgeSettings currentEdgeSettings;

    private Long queueStartTs;

    private ExecutorService executor;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService reconnectScheduler;
    private ScheduledExecutorService shutdownScheduler;

    private volatile boolean initialized;
    private volatile boolean initInProgress;
    private volatile boolean syncInProgress = false;

    private TenantId tenantId;
    private CustomerId customerId;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        scheduleRpcConnection();
    }

    @Override
    @SneakyThrows
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (ServiceType.TB_CORE.equals(event.getServiceType())) {
            synchronized (this) {
                boolean isMyPartition = isSystemTenantPartitionMine();
                if (isMyPartition && !initialized) {
                    scheduleRpcConnection();
                } else if (initialized || initInProgress) {
                    destroy();
                }
            }
        }
    }

    private void scheduleRpcConnection() {
        ScheduledExecutorService rpcScheduler = getOrCreateRpcScheduler();
        if (rpcConnectionScheduled.compareAndSet(false, true)) {
            rpcScheduler.schedule(() -> {
                try {
                    establishRpcConnection();
                } finally {
                    initInProgress = false;
                    rpcConnectionScheduled.set(false);
                }
            }, 60, TimeUnit.SECONDS);
        }
    }

    private void establishRpcConnection() {
        if (isSystemTenantPartitionMine() && !initialized && !initInProgress && validateRoutingKeyAndSecret()) {
            initInProgress = true;
            try {
                log.info("Starting Cloud Edge service");
                edgeRpcClient.connect(routingKey, routingSecret,
                        this::onUplinkResponse,
                        this::onEdgeUpdate,
                        this::onDownlink,
                        this::scheduleReconnect);
                executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("cloud-manager"));
                reconnectScheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-reconnect"));
                processHandleMessages();
            } catch (Exception e) {
                scheduleRpcConnection();
            }
        }
    }

    private boolean validateRoutingKeyAndSecret() {
        if (StringUtils.isBlank(routingKey) || StringUtils.isBlank(routingSecret)) {
            shutdownScheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-shutdown"));
            shutdownScheduler.scheduleAtFixedRate(() -> log.error(
                    "Routing Key and Routing Secret must be provided! " +
                            "Please configure Routing Key and Routing Secret in the tb-edge.yml file " +
                            "or add CLOUD_ROUTING_KEY and CLOUD_ROUTING_SECRET variable to the tb-edge.conf file. " +
                            "ThingsBoard Edge is not going to connect to cloud!"), 0, 10, TimeUnit.SECONDS);
            return false;
        }
        return true;
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        initInProgress = false;
        initialized = false;

        if (shutdownScheduler != null && !shutdownScheduler.isShutdown()) {
            shutdownScheduler.shutdownNow();
        }

        updateConnectivityStatus(false);

        String edgeId = currentEdgeSettings != null ? currentEdgeSettings.getEdgeId() : "";
        log.info("[{}] Starting destroying process", edgeId);
        try {
            edgeRpcClient.disconnect(false);
        } catch (Exception e) {
            log.error("Exception during disconnect", e);
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        ScheduledExecutorService rpcScheduler = rpcSchedulerRef.get();
        if (rpcScheduler != null && !rpcScheduler.isShutdown()) {
            rpcScheduler.shutdownNow();
        }
        if (reconnectScheduler != null && !reconnectScheduler.isShutdown()) {
            reconnectScheduler.shutdownNow();
        }
        log.info("[{}] Destroy was successful", edgeId);
    }

    private void processHandleMessages() {
        executor.submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    if (initialized) {
                        generalUplinkMessageService.processHandleMessages(tenantId);
                        tsUplinkMessageService.processHandleMessages(tenantId);
                        try {
                            Thread.sleep(cloudEventStorageSettings.getNoRecordsSleepInterval());
                        } catch (InterruptedException e) {
                            log.error("Error during sleep", e);
                        }
                    } else {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                    }
                } catch (Exception e) {
                    log.warn("Failed to process messages handling!", e);
                }
            }
        });
    }

    private void updateQueueStartTsSeqIdOffset(String attrStartTsKey, String attrSeqIdKey, Long startTs, Long seqIdOffset) {
        log.trace("updateQueueStartTsSeqIdOffset [{}][{}]", startTs, seqIdOffset);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(attrStartTsKey, startTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(attrSeqIdKey, seqIdOffset), System.currentTimeMillis()));
        attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes);
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        generalUplinkMessageService.onUplinkResponse(msg);
        tsUplinkMessageService.onUplinkResponse(msg);
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        try {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
                scheduledFuture = null;
            }

            if ("CE".equals(edgeConfiguration.getCloudType())) {
                initAndUpdateEdgeSettings(edgeConfiguration);
            } else {
                new Thread(() -> {
                    log.error("Terminating application. CE edge can be connected only to CE server version...");
                    int exitCode = -1;
                    int appExitCode = exitCode;
                    try {
                        appExitCode = SpringApplication.exit(context, () -> exitCode);
                    } finally {
                        System.exit(appExitCode);
                    }
                }, "Shutdown Thread").start();
            }
        } catch (Exception e) {
            log.error("Can't process edge configuration message [{}]", edgeConfiguration, e);
            scheduleRpcConnection();
        }
    }

    private void initAndUpdateEdgeSettings(EdgeConfiguration edgeConfiguration) throws Exception {
        this.tenantId = TenantId.fromUUID(new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB()));

        this.currentEdgeSettings = cloudEventService.findEdgeSettings(this.tenantId);
        EdgeSettings newEdgeSettings = constructEdgeSettings(edgeConfiguration);
        if (this.currentEdgeSettings == null || !this.currentEdgeSettings.getEdgeId().equals(newEdgeSettings.getEdgeId())) {
            tenantProcessor.cleanUp();
            this.currentEdgeSettings = newEdgeSettings;
            updateQueueStartTsSeqIdOffset(QUEUE_START_TS_ATTR_KEY, QUEUE_SEQ_ID_OFFSET_ATTR_KEY, System.currentTimeMillis(), 0L);
            updateQueueStartTsSeqIdOffset(QUEUE_TS_KV_START_TS_ATTR_KEY, QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY, System.currentTimeMillis(), 0L);
        } else {
            log.trace("Using edge settings from DB {}", this.currentEdgeSettings);
        }

        queueStartTs = generalUplinkMessageService.getQueueStartTs(tenantId).get();
        tenantProcessor.createTenantIfNotExists(this.tenantId, queueStartTs);
        boolean edgeCustomerIdUpdated = setOrUpdateCustomerId(edgeConfiguration);
        if (edgeCustomerIdUpdated) {
            customerProcessor.createCustomerIfNotExists(this.tenantId, edgeConfiguration);
        }
        // TODO: voba - should sync be executed in some other cases ???
        log.trace("Sending sync request, fullSyncRequired {}", this.currentEdgeSettings.isFullSyncRequired());
        edgeRpcClient.sendSyncRequestMsg(this.currentEdgeSettings.isFullSyncRequired());
        this.syncInProgress = true;

        cloudEventService.saveEdgeSettings(tenantId, this.currentEdgeSettings);

        saveOrUpdateEdge(tenantId, edgeConfiguration);

        updateConnectivityStatus(true);

        initialized = true;
        initInProgress = false;
    }

    private boolean setOrUpdateCustomerId(EdgeConfiguration edgeConfiguration) {
        EdgeId edgeId = getEdgeId(edgeConfiguration);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        CustomerId previousCustomerId = null;
        if (edge != null) {
            previousCustomerId = edge.getCustomerId();
        }
        if (edgeConfiguration.getCustomerIdMSB() != 0 && edgeConfiguration.getCustomerIdLSB() != 0) {
            UUID customerUUID = new UUID(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
            this.customerId = new CustomerId(customerUUID);
            return !this.customerId.equals(previousCustomerId);
        } else {
            this.customerId = null;
            return false;
        }
    }

    private EdgeId getEdgeId(EdgeConfiguration edgeConfiguration) {
        UUID edgeUUID = new UUID(edgeConfiguration.getEdgeIdMSB(), edgeConfiguration.getEdgeIdLSB());
        return new EdgeId(edgeUUID);
    }

    private void saveOrUpdateEdge(TenantId tenantId, EdgeConfiguration edgeConfiguration) throws ExecutionException, InterruptedException {
        EdgeId edgeId = getEdgeId(edgeConfiguration);
        edgeCloudProcessor.processEdgeConfigurationMsgFromCloud(tenantId, edgeConfiguration);
        cloudEventService.saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.ATTRIBUTES_REQUEST, edgeId, null, queueStartTs);
        cloudEventService.saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.RELATION_REQUEST, edgeId, null, queueStartTs);
    }

    private EdgeSettings constructEdgeSettings(EdgeConfiguration edgeConfiguration) {
        EdgeSettings edgeSettings = new EdgeSettings();
        UUID edgeUUID = new UUID(edgeConfiguration.getEdgeIdMSB(), edgeConfiguration.getEdgeIdLSB());
        edgeSettings.setEdgeId(edgeUUID.toString());
        UUID tenantUUID = new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB());
        edgeSettings.setTenantId(tenantUUID.toString());
        edgeSettings.setName(edgeConfiguration.getName());
        edgeSettings.setType(edgeConfiguration.getType());
        edgeSettings.setRoutingKey(edgeConfiguration.getRoutingKey());
        edgeSettings.setFullSyncRequired(true);
        return edgeSettings;
    }

    private void onDownlink(DownlinkMsg downlinkMsg) {
        boolean edgeCustomerIdUpdated = updateCustomerIdIfRequired(downlinkMsg);
        if (this.syncInProgress && downlinkMsg.hasSyncCompletedMsg()) {
            log.trace("[{}] downlinkMsg hasSyncCompletedMsg = true", downlinkMsg);
            this.syncInProgress = false;
        }
        ListenableFuture<List<Void>> future =
                downlinkMessageService.processDownlinkMsg(tenantId, customerId, downlinkMsg, this.currentEdgeSettings, queueStartTs);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                log.trace("[{}] DownlinkMsg has been processed successfully! DownlinkMsgId {}", routingKey, downlinkMsg.getDownlinkMsgId());
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                        .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                        .setSuccess(true).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
                if (downlinkMsg.hasEdgeConfiguration()) {
                    if (edgeCustomerIdUpdated && !syncInProgress) {
                        log.info("Edge customer id has been updated. Sending sync request...");
                        edgeRpcClient.sendSyncRequestMsg(false);
                        syncInProgress = true;
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to process DownlinkMsg! DownlinkMsgId {}", routingKey, downlinkMsg.getDownlinkMsgId());
                String errorMsg = EdgeUtils.createErrorMsgFromRootCauseAndStackTrace(t);
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                        .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                        .setSuccess(false).setErrorMsg(errorMsg).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }
        }, MoreExecutors.directExecutor());
    }

    private boolean updateCustomerIdIfRequired(DownlinkMsg downlinkMsg) {
        if (downlinkMsg.hasEdgeConfiguration()) {
            return setOrUpdateCustomerId(downlinkMsg.getEdgeConfiguration());
        } else {
            return false;
        }
    }

    private void updateConnectivityStatus(boolean activityState) {
        if (tenantId != null) {
            save(DefaultDeviceStateService.ACTIVITY_STATE, activityState);
            if (activityState) {
                save(DefaultDeviceStateService.LAST_CONNECT_TIME, System.currentTimeMillis());
            } else {
                save(DefaultDeviceStateService.LAST_DISCONNECT_TIME, System.currentTimeMillis());
            }
        }
    }

    private void scheduleReconnect(Exception e) {
        initialized = false;

        updateConnectivityStatus(false);

        if (scheduledFuture == null) {
            scheduledFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
                log.info("Trying to reconnect due to the error: {}!", e.getMessage());
                try {
                    edgeRpcClient.disconnect(true);
                } catch (Exception ex) {
                    log.error("Exception during disconnect: {}", ex.getMessage());
                }
                try {
                    edgeRpcClient.connect(routingKey, routingSecret,
                            this::onUplinkResponse,
                            this::onEdgeUpdate,
                            this::onDownlink,
                            this::scheduleReconnect);
                } catch (Exception ex) {
                    log.error("Exception during connect: {}", ex.getMessage());
                }
            }, reconnectTimeoutMs, reconnectTimeoutMs, TimeUnit.MILLISECONDS);
        }
    }

    private void save(String key, long value) {
        tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, tenantId, AttributeScope.SERVER_SCOPE, key, value, new AttributeSaveCallback(key, value));
    }

    private void save(String key, boolean value) {
        tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, tenantId, AttributeScope.SERVER_SCOPE, key, value, new AttributeSaveCallback(key, value));
    }

    private boolean isSystemTenantPartitionMine() {
        return partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
    }

    private ScheduledExecutorService getOrCreateRpcScheduler() {
        ScheduledExecutorService scheduler = rpcSchedulerRef.get();
        if (scheduler == null || scheduler.isShutdown()) {
            ScheduledExecutorService newScheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-rpc-connect"));
            if (rpcSchedulerRef.compareAndSet(scheduler, newScheduler)) {
                return newScheduler;
            } else {
                newScheduler.shutdown();
                return rpcSchedulerRef.get();
            }
        }
        return scheduler;
    }

    private static class AttributeSaveCallback implements FutureCallback<Void> {

        private final String key;
        private final Object value;

        AttributeSaveCallback(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@javax.annotation.Nullable Void result) {
            log.trace("Successfully updated attribute [{}] with value [{}]", key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to update attribute [{}] with value [{}]", key, value, t);
        }

    }

}
