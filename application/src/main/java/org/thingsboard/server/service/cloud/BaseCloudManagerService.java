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
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.cloud.EdgeSettingsService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.CustomerCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DashboardCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EdgeCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityViewCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RelationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.ResourceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TelemetryCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WidgetBundleCloudProcessor;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.service.edge.rpc.EdgeGrpcSession.RATE_LIMIT_REACHED;

@Slf4j
public abstract class BaseCloudManagerService {

    private static final int MAX_SEND_UPLINK_ATTEMPTS = 3;

    @Value("${cloud.routingKey}")
    private String routingKey;

    @Value("${cloud.secret}")
    private String routingSecret;

    @Value("${cloud.reconnect_timeout}")
    private long reconnectTimeoutMs;

    @Value("${cloud.uplink_pack_timeout_sec:60}")
    private long uplinkPackTimeoutSec;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    protected CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private TelemetrySubscriptionService tsSubService;

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
    private CloudEventService cloudEventService;

    @Autowired
    private EdgeSettingsService edgeSettingsService;

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private RelationCloudProcessor relationProcessor;

    @Autowired
    private DeviceCloudProcessor deviceProcessor;

    @Autowired
    private DeviceProfileCloudProcessor deviceProfileProcessor;

    @Autowired
    private AlarmCloudProcessor alarmProcessor;

    @Autowired
    private TelemetryCloudProcessor telemetryProcessor;

    @Autowired
    private WidgetBundleCloudProcessor widgetBundleProcessor;

    @Autowired
    private EntityViewCloudProcessor entityViewProcessor;

    @Autowired
    private DashboardCloudProcessor dashboardProcessor;

    @Autowired
    private AssetCloudProcessor assetProcessor;

    @Autowired
    private AssetProfileCloudProcessor assetProfileProcessor;

    @Autowired
    private ResourceCloudProcessor resourceCloudProcessor;

    @Autowired(required = false)
    private CloudEventMigrationService cloudEventMigrationService;

    private ScheduledExecutorService uplinkExecutor;
    private ScheduledFuture<?> sendUplinkFuture;
    private ScheduledExecutorService shutdownExecutor;

    private EdgeSettings currentEdgeSettings;
    protected TenantId tenantId;
    private CustomerId customerId;

    private final ConcurrentMap<Integer, UplinkMsg> pendingMsgMap = new ConcurrentHashMap<>();
    private CountDownLatch latch;
    private SettableFuture<Boolean> sendUplinkFutureResult;

    protected volatile boolean initialized;
    protected volatile boolean isGeneralProcessInProgress = false;
    private volatile boolean sendingInProgress = false;
    private volatile boolean syncInProgress = false;
    private volatile boolean isRateLimitViolated = false;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (validateRoutingKeyAndSecret()) {
            log.info("Starting Cloud Edge service");
            edgeRpcClient.connect(routingKey, routingSecret,
                    this::onUplinkResponse,
                    this::onEdgeUpdate,
                    this::onDownlink,
                    this::scheduleReconnect);
            uplinkExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-uplink"));
            launchUplinkProcessing();
        }
    }

    protected abstract void launchUplinkProcessing();
    protected abstract void resetQueueOffset();

    protected void destroy() throws InterruptedException {
        if (shutdownExecutor != null) {
            shutdownExecutor.shutdown();
        }

        updateConnectivityStatus(false);

        String edgeId = currentEdgeSettings != null ? currentEdgeSettings.getEdgeId() : "";
        log.info("[{}] Starting destroying process", edgeId);
        try {
            edgeRpcClient.disconnect(false);
        } catch (Exception e) {
            log.error("Exception during disconnect", e);
        }

        if (uplinkExecutor != null) {
            uplinkExecutor.shutdownNow();
        }
        log.info("[{}] Destroy was successful", edgeId);
    }

    private boolean validateRoutingKeyAndSecret() {
        if (StringUtils.isBlank(routingKey) || StringUtils.isBlank(routingSecret)) {
            shutdownExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-shutdown"));
            shutdownExecutor.scheduleAtFixedRate(() -> log.error(
                    "Routing Key and Routing Secret must be provided! " +
                            "Please configure Routing Key and Routing Secret in the tb-edge.yml file " +
                            "or add CLOUD_ROUTING_KEY and CLOUD_ROUTING_SECRET variable to the tb-edge.conf file. " +
                            "ThingsBoard Edge is not going to connect to cloud!"), 0, 10, TimeUnit.SECONDS);
            return false;
        }
        return true;
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        try {
            if (sendingInProgress) {
                if (msg.getSuccess()) {
                    pendingMsgMap.remove(msg.getUplinkMsgId());
                    log.debug("Msg has been processed successfully! {}", msg);
                } else {
                    if (msg.getErrorMsg().contains(RATE_LIMIT_REACHED)) {
                        log.warn("Msg processing failed! {}", RATE_LIMIT_REACHED);
                        isRateLimitViolated = true;
                    } else {
                        log.error("Msg processing failed! Error msg: {}", msg.getErrorMsg());
                    }
                }
                latch.countDown();
            }
        } catch (Exception e) {
            log.error("Can't process uplink response message [{}]", msg, e);
        }
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        try {
            if (sendUplinkFuture != null) {
                sendUplinkFuture.cancel(true);
                sendUplinkFuture = null;
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
        }
    }

    private void initAndUpdateEdgeSettings(EdgeConfiguration edgeConfiguration) throws Exception {
        this.tenantId = new TenantId(new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB()));

        this.currentEdgeSettings = edgeSettingsService.findEdgeSettings(this.tenantId);
        EdgeSettings newEdgeSettings = constructEdgeSettings(edgeConfiguration);
        if (this.currentEdgeSettings == null || !this.currentEdgeSettings.getEdgeId().equals(newEdgeSettings.getEdgeId())) {
            tenantProcessor.cleanUp();
            this.currentEdgeSettings = newEdgeSettings;
            resetQueueOffset();
        } else {
            log.trace("Using edge settings from DB {}", this.currentEdgeSettings);
        }

        tenantProcessor.createTenantIfNotExists(this.tenantId);
        boolean edgeCustomerIdUpdated = setOrUpdateCustomerId(edgeConfiguration);
        if (edgeCustomerIdUpdated) {
            customerProcessor.createCustomerIfNotExists(this.tenantId, edgeConfiguration);
        }
        // TODO: voba - should sync be executed in some other cases ???
        log.trace("Sending sync request, fullSyncRequired {}", this.currentEdgeSettings.isFullSyncRequired());
        edgeRpcClient.sendSyncRequestMsg(this.currentEdgeSettings.isFullSyncRequired());
        this.syncInProgress = true;

        edgeSettingsService.saveEdgeSettings(tenantId, this.currentEdgeSettings);

        saveOrUpdateEdge(tenantId, edgeConfiguration);

        updateConnectivityStatus(true);

        if (cloudEventMigrationService != null && !cloudEventMigrationService.isMigrated()) {
            cloudEventMigrationService.migrateUnprocessedEventToKafka(tenantId);
        }

        initialized = true;
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
        cloudEventService.saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.ATTRIBUTES_REQUEST, edgeId, null);
        cloudEventService.saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.RELATION_REQUEST, edgeId, null);
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
                downlinkMessageService.processDownlinkMsg(tenantId, customerId, downlinkMsg, this.currentEdgeSettings);
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

        if (sendUplinkFuture == null) {
            sendUplinkFuture = uplinkExecutor.scheduleAtFixedRate(() -> {
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

    private record AttributeSaveCallback(String key, Object value) implements FutureCallback<Void> {

        @Override
            public void onSuccess(@javax.annotation.Nullable Void result) {
                log.trace("Successfully updated attribute [{}] with value [{}]", key, value);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to update attribute [{}] with value [{}]", key, value, t);
            }

        }

    private UplinkMsg convertEventToUplink(CloudEvent cloudEvent) {
        log.trace("Converting cloud event [{}]", cloudEvent);
        try {
            return switch (cloudEvent.getAction()) {
                case UPDATED, ADDED, DELETED, ALARM_ACK, ALARM_CLEAR, ALARM_DELETE, CREDENTIALS_UPDATED,
                     RELATION_ADD_OR_UPDATE, RELATION_DELETED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER,
                     ADDED_COMMENT, UPDATED_COMMENT, DELETED_COMMENT -> convertEntityEventToUplink(cloudEvent);
                case ATTRIBUTES_UPDATED, POST_ATTRIBUTES, ATTRIBUTES_DELETED, TIMESERIES_UPDATED ->
                        telemetryProcessor.convertTelemetryEventToUplink(cloudEvent.getTenantId(), cloudEvent);
                case ATTRIBUTES_REQUEST -> telemetryProcessor.convertAttributesRequestEventToUplink(cloudEvent);
                case RELATION_REQUEST -> relationProcessor.convertRelationRequestEventToUplink(cloudEvent);
                case RPC_CALL -> deviceProcessor.convertRpcCallEventToUplink(cloudEvent);
                case WIDGET_BUNDLE_TYPES_REQUEST -> widgetBundleProcessor.convertWidgetBundleTypesRequestEventToUplink(cloudEvent);
                case ENTITY_VIEW_REQUEST -> entityViewProcessor.convertEntityViewRequestEventToUplink(cloudEvent);
                default -> {
                    log.warn("Unsupported action type [{}]", cloudEvent);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("Exception during converting events from queue, skipping event [{}]", cloudEvent, e);
            return null;
        }
    }

    private UplinkMsg convertEntityEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertEntityEventToUplink cloudEvent [{}], edgeEventAction [{}]", cloudEvent, cloudEvent.getAction());
        EdgeVersion edgeVersion = EdgeVersion.V_LATEST;

        return switch (cloudEvent.getType()) {
            case DEVICE -> deviceProcessor.convertDeviceEventToUplink(cloudEvent.getTenantId(), cloudEvent, edgeVersion);
            case DEVICE_PROFILE -> deviceProfileProcessor.convertDeviceProfileEventToUplink(cloudEvent, edgeVersion);
            case ALARM -> alarmProcessor.convertAlarmEventToUplink(cloudEvent, edgeVersion);
            case ALARM_COMMENT -> alarmProcessor.convertAlarmCommentEventToUplink(cloudEvent, edgeVersion);
            case ASSET -> assetProcessor.convertAssetEventToUplink(cloudEvent, edgeVersion);
            case ASSET_PROFILE -> assetProfileProcessor.convertAssetProfileEventToUplink(cloudEvent, edgeVersion);
            case DASHBOARD -> dashboardProcessor.convertDashboardEventToUplink(cloudEvent, edgeVersion);
            case ENTITY_VIEW -> entityViewProcessor.convertEntityViewEventToUplink(cloudEvent, edgeVersion);
            case RELATION -> relationProcessor.convertRelationEventToUplink(cloudEvent, edgeVersion);
            case TB_RESOURCE -> resourceCloudProcessor.convertResourceEventToUplink(cloudEvent, edgeVersion);
            default -> {
                log.warn("Unsupported cloud event type [{}]", cloudEvent);
                yield null;
            }
        };
    }

    protected ListenableFuture<Boolean> sendCloudEvents(List<CloudEvent> cloudEvents) {
        interruptPreviousSendUplinkMsgsTask();
        sendUplinkFutureResult = SettableFuture.create();

        log.trace("[{}] event(s) are going to be converted.", cloudEvents.size());
        List<UplinkMsg> uplinkMsgPack = cloudEvents.stream()
                .map(this::convertEventToUplink)
                .filter(Objects::nonNull)
                .toList();

        if (uplinkMsgPack.isEmpty()) {
            return Futures.immediateFuture(true);
        }

        processMsgPack(uplinkMsgPack);
        return sendUplinkFutureResult;
    }

    private void interruptPreviousSendUplinkMsgsTask() {
        if (sendUplinkFutureResult != null && !sendUplinkFutureResult.isDone()) {
            log.debug("[{}] Stopping send uplink future now!", tenantId);
            sendUplinkFutureResult.set(true);
        }
        if (sendUplinkFuture != null && !sendUplinkFuture.isCancelled()) {
            sendUplinkFuture.cancel(true);
        }
    }

    private void processMsgPack(List<UplinkMsg> uplinkMsgPack) {
        pendingMsgMap.clear();
        uplinkMsgPack.forEach(msg -> pendingMsgMap.put(msg.getUplinkMsgId(), msg));
        LinkedBlockingQueue<UplinkMsg> orderedPendingMsgQueue = new LinkedBlockingQueue<>(pendingMsgMap.values());
        sendUplinkFuture = uplinkExecutor.schedule(() -> {
            int attempt = 1;
            boolean success;
            do {
                log.trace("[{}] uplink msg(s) are going to be send.", pendingMsgMap.values().size());

                success = sendUplinkMsgPack(orderedPendingMsgQueue) && pendingMsgMap.isEmpty();

                if (!success) {
                    log.warn("Failed to deliver the batch: {}, attempt: {}", pendingMsgMap.values(), attempt);
                    try {
                        Thread.sleep(cloudEventStorageSettings.getSleepIntervalBetweenBatches());

                        if (isRateLimitViolated) {
                            isRateLimitViolated = false;
                            TimeUnit.SECONDS.sleep(60);
                        }
                    } catch (InterruptedException e) {
                        log.error("Error during sleep between batches or on rate limit violation", e);
                    }
                }

                attempt++;

                if (attempt > MAX_SEND_UPLINK_ATTEMPTS) {
                    log.warn("Failed to deliver the batch: after {} attempts. Next messages are going to be discarded {}",
                            MAX_SEND_UPLINK_ATTEMPTS, pendingMsgMap.values());
                    sendUplinkFutureResult.set(true);
                    return;
                }
            } while (!success);
            sendUplinkFutureResult.set(false);
        }, 0L, TimeUnit.MILLISECONDS);
    }

    private boolean sendUplinkMsgPack(LinkedBlockingQueue<UplinkMsg> orderedPendingMsgQueue) {
        try {
            boolean success;

            sendingInProgress = true;
            latch = new CountDownLatch(pendingMsgMap.values().size());
            orderedPendingMsgQueue.forEach(this::sendUplinkMsg);

            success = latch.await(uplinkPackTimeoutSec, TimeUnit.SECONDS);
            sendingInProgress = false;

            return success;
        } catch (InterruptedException e) {
            log.error("sendUplinkMsgPack throw InterruptedException", e);
            throw new RuntimeException("Interrupted while waiting for latch. " + e);
        }
    }

    private void sendUplinkMsg(UplinkMsg uplinkMsg) {
        if (edgeRpcClient.getServerMaxInboundMessageSize() == 0 ||
                uplinkMsg.getSerializedSize() <= edgeRpcClient.getServerMaxInboundMessageSize()) {
            edgeRpcClient.sendUplinkMsg(uplinkMsg);
        } else {
            log.error("Uplink msg size [{}] exceeds server max inbound message size [{}]. Skipping this message. " +
                    "Please increase value of EDGES_RPC_MAX_INBOUND_MESSAGE_SIZE env variable on the server and restart it. Message {}",
                    uplinkMsg.getSerializedSize(), edgeRpcClient.getServerMaxInboundMessageSize(), uplinkMsg);
            pendingMsgMap.remove(uplinkMsg.getUplinkMsgId());
            latch.countDown();
        }
    }
}