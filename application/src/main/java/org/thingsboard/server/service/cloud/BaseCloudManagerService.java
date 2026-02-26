/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
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
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.EdgeSettingsService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.edge.stats.CloudStatsCounterService;
import org.thingsboard.server.dao.edge.stats.CloudStatsKey;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.service.edge.rpc.EdgeGrpcSession.RATE_LIMIT_REACHED;

@Slf4j
public abstract class BaseCloudManagerService extends TbApplicationEventListener<PartitionChangeEvent> {

    protected static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";
    protected static final String QUEUE_SEQ_ID_OFFSET_ATTR_KEY = "queueSeqIdOffset";
    protected static final String QUEUE_TS_KV_START_TS_ATTR_KEY = "queueTsKvStartTs";
    protected static final String QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY = "queueTsKvSeqIdOffset";

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
    private CloudContextComponent cloudCtx;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    protected CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    private DownlinkMessageService downlinkMessageService;

    @Autowired
    private EdgeRpcClient edgeRpcClient;

    @Autowired
    private EdgeSettingsService edgeSettingsService;

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired(required = false)
    private CloudEventMigrationService cloudEventMigrationService;

    @Autowired
    protected CloudStatsCounterService statsCounterService;

    private ScheduledExecutorService uplinkExecutor;
    private ScheduledFuture<?> sendUplinkFuture;

    private ScheduledExecutorService shutdownExecutor;
    private ScheduledExecutorService reconnectExecutor;
    private ScheduledExecutorService connectExecutor;
    private ScheduledFuture<?> reconnectFuture;
    private ScheduledFuture<?> connectFuture;

    private EdgeSettings currentEdgeSettings;
    protected TenantId tenantId;
    private CustomerId customerId;

    private final ConcurrentMap<Integer, UplinkMsg> pendingMsgMap = new ConcurrentHashMap<>();
    private CountDownLatch latch;
    private SettableFuture<Boolean> sendUplinkFutureResult;

    private final Lock uplinkSendLock = new ReentrantLock();

    protected volatile boolean initialized;
    protected volatile boolean isGeneralProcessInProgress = false;
    protected volatile boolean syncInProgress = false;
    private volatile boolean sendingInProgress = false;
    private volatile boolean isRateLimitViolated = false;
    private volatile boolean initInProgress = false;
    private volatile boolean shouldPerformInitialSync = true;

    protected void establishRpcConnection() {
        if (connectFuture != null) {
            connectFuture.cancel(true);
            connectFuture = null;
        }
        if (connectExecutor == null) {
            connectExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-connect"));
        }
        connectFuture = connectExecutor.schedule(() -> {
            try {
                synchronized (this) {
                    if (!isSystemTenantPartitionMine()) {
                        onDestroy();
                        return;
                    }
                    if (!initialized && !initInProgress && validateRoutingKeyAndSecret()) {
                        initInProgress = true;
                        try {
                            log.info("Starting Cloud Edge service");
                            edgeRpcClient.connect(routingKey, routingSecret,
                                    this::onUplinkResponse,
                                    this::onEdgeUpdate,
                                    this::onDownlink,
                                    this::scheduleReconnect);
                            uplinkExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-uplink"));
                            reconnectExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-reconnect"));
                            launchUplinkProcessing();
                        } catch (Exception e) {
                            initInProgress = false;
                            log.error("Failed to establish connection to cloud", e);
                            connectExecutor.schedule(this::establishRpcConnection, reconnectTimeoutMs, TimeUnit.MILLISECONDS);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to establish Cloud Edge service", e);
            }
        }, reconnectTimeoutMs, TimeUnit.MILLISECONDS);
    }

    protected abstract void launchUplinkProcessing();

    protected abstract void onDestroy() throws InterruptedException;

    protected void resetQueueOffset() {
        updateQueueStartTsSeqIdOffset(tenantId, QUEUE_START_TS_ATTR_KEY, QUEUE_SEQ_ID_OFFSET_ATTR_KEY, System.currentTimeMillis(), 0L);
        updateQueueStartTsSeqIdOffset(tenantId, QUEUE_TS_KV_START_TS_ATTR_KEY, QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY, System.currentTimeMillis(), 0L);
    }

    protected void updateQueueStartTsSeqIdOffset(TenantId tenantId, String attrStartTsKey, String attrSeqIdKey, Long startTs, Long seqIdOffset) {
        log.trace("updateQueueStartTsSeqIdOffset [{}][{}][{}][{}]", attrStartTsKey, attrSeqIdKey, startTs, seqIdOffset);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(attrStartTsKey, startTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(attrSeqIdKey, seqIdOffset), System.currentTimeMillis())
        );
        try {
            attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes).get();
        } catch (Exception e) {
            log.error("Failed to update queueStartTsSeqIdOffset [{}][{}]", attrStartTsKey, attrSeqIdKey, e);
        }
    }

    protected void destroy() throws InterruptedException {
        initInProgress = false;
        initialized = false;

        if (shutdownExecutor != null) {
            shutdownExecutor.shutdownNow();
        }

        updateConnectivityStatus(false);

        String edgeId = currentEdgeSettings != null ? currentEdgeSettings.getEdgeId() : "";
        log.info("[{}] Starting destroying process", edgeId);
        try {
            edgeRpcClient.disconnect(false);
        } catch (Exception e) {
            log.error("Exception during disconnect", e);
        }

        if (uplinkExecutor != null && !uplinkExecutor.isShutdown()) {
            uplinkExecutor.shutdownNow();
            uplinkExecutor = null;
        }
        if (reconnectExecutor != null) {
            reconnectExecutor.shutdownNow();
            reconnectExecutor = null;
        }
        log.info("[{}] Destroy was successful", edgeId);
    }

    protected void processUplinkMessages(TimePageLink pageLink, Long queueSeqIdStart, String queueStartTsAttrKey, String queueSeqIdAttrKey, boolean isGeneralMsg, CloudEventFinder finder) {
        try {
            if (isGeneralMsg) {
                isGeneralProcessInProgress = true;
            }
            PageData<CloudEvent> cloudEvents;
            boolean isInterrupted;
            do {
                if (!isGeneralMsg) {
                    waitForGeneralProcessingCompleteIfInProgress();
                }
                cloudEvents = finder.find(tenantId, queueSeqIdStart, null, pageLink);
                if (cloudEvents.getData().isEmpty()) {
                    log.info("seqId column of table started new cycle. queueSeqIdStart={}, queueStartTsAttrKey={}, queueSeqIdAttrKey={}, isGeneralMsg={}",
                            queueSeqIdStart, queueStartTsAttrKey, queueSeqIdAttrKey, isGeneralMsg);
                    cloudEvents = findCloudEventsFromBeginning(tenantId, pageLink, finder);
                }
                isInterrupted = processCloudEvents(cloudEvents.getData(), isGeneralMsg).get();
                if (!isInterrupted && cloudEvents.getTotalElements() > 0) {
                    CloudEvent latestCloudEvent = cloudEvents.getData().get(cloudEvents.getData().size() - 1);
                    try {
                        Long newStartTs = Uuids.unixTimestamp(latestCloudEvent.getUuidId());
                        updateQueueStartTsSeqIdOffset(tenantId, queueStartTsAttrKey, queueSeqIdAttrKey, newStartTs, latestCloudEvent.getSeqId());
                        log.info("Queue offset was updated [{}][{}][{}]", latestCloudEvent.getUuidId(), newStartTs, latestCloudEvent.getSeqId());
                    } catch (Exception e) {
                        log.error("Failed to update queue offset [{}]", latestCloudEvent);
                    }
                }
                if (!isInterrupted) {
                    pageLink = pageLink.nextPageLink();
                    if (cloudEvents.hasNext()) {
                        String queueName = isGeneralMsg ? "Cloud Event" : "TSKv Cloud Event";

                        long queueSize = Math.max(cloudEvents.getTotalElements() - ((long) pageLink.getPage() * pageLink.getPageSize()), 0);
                        statsCounterService.setUplinkMsgsLag(tenantId, queueSize);
                        log.info("[{}] Uplink Processing Lag Stats: queue size = [{}], current page = [{}], total pages = [{}]",
                                queueName, queueSize, pageLink.getPage(), cloudEvents.getTotalPages());
                    }
                }
                if (!isGeneralMsg) {
                    waitForGeneralProcessingCompleteIfInProgress();
                }
                log.trace("processUplinkMessages state isInterrupted={},total={},hasNext={},isGeneralMsg={},isGeneralProcessInProgress={}",
                        isInterrupted, cloudEvents.getTotalElements(), cloudEvents.hasNext(), isGeneralMsg, isGeneralProcessInProgress);
            } while (isInterrupted || cloudEvents.hasNext());
        } catch (Exception e) {
            log.error("Failed to process cloud event messages handling!", e);
        } finally {
            if (isGeneralMsg) {
                isGeneralProcessInProgress = false;
            }
        }
    }

    private void waitForGeneralProcessingCompleteIfInProgress() {
        if (!isGeneralProcessInProgress) {
            return;
        }
        do {
            try {
                TimeUnit.MILLISECONDS.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        } while (isGeneralProcessInProgress);
    }

    protected TimePageLink newCloudEventsAvailable(TenantId tenantId, Long queueSeqIdStart, String key, CloudEventFinder finder) {
        try {
            long queueStartTs = getLongAttrByKey(tenantId, key).get();
            // Subtract MISORDERING_COMPENSATION_MILLIS to ensure no events are missed in a clustered environment.
            // While events are identified using seqId, we use partitioning for performance reasons
            // and partitioning is based on created_time.
            queueStartTs = queueStartTs > 0 ? queueStartTs - cloudCtx.getCloudEventStorageSettings().getMisorderingCompensationMillis() : 0;
            long queueEndTs = queueStartTs > 0 ? queueStartTs + TimeUnit.DAYS.toMillis(1) : System.currentTimeMillis();
            log.trace("newCloudEventsAvailable, queueSeqIdStart = {}, key = {}, queueStartTs = {}, queueEndTs = {}",
                    queueSeqIdStart, key, queueStartTs, queueEndTs);
            TimePageLink pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                    0, null, null, queueStartTs, queueEndTs);
            PageData<CloudEvent> cloudEvents = finder.find(tenantId, queueSeqIdStart, null, pageLink);
            if (cloudEvents.getData().isEmpty()) {
                if (queueSeqIdStart > cloudEventStorageSettings.getMaxReadRecordsCount()) {
                    // check if new cycle started (seq_id starts from '1')
                    cloudEvents = findCloudEventsFromBeginning(tenantId, pageLink, finder);
                    if (cloudEvents.getData().stream().anyMatch(ce -> ce.getSeqId() == 1)) {
                        log.info("newCloudEventsAvailable: new cycle started (seq_id starts from '1')!");
                        return pageLink;
                    }
                }
                while (queueEndTs < System.currentTimeMillis()) {
                    log.trace("newCloudEventsAvailable: queueEndTs < System.currentTimeMillis() [{}] [{}]", queueEndTs, System.currentTimeMillis());
                    queueStartTs = queueEndTs;
                    queueEndTs = queueEndTs + TimeUnit.DAYS.toMillis(1);
                    pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                            0, null, null, queueStartTs, queueEndTs);
                    cloudEvents = finder.find(tenantId, queueSeqIdStart, null, pageLink);
                    if (!cloudEvents.getData().isEmpty()) {
                        return pageLink;
                    }
                }
                return null;
            } else {
                return pageLink;
            }
        } catch (Exception e) {
            log.warn("Failed to check newCloudEventsAvailable!", e);
            return null;
        }
    }

    protected PageData<CloudEvent> findCloudEventsFromBeginning(TenantId tenantId, TimePageLink pageLink, CloudEventFinder finder) {
        long seqIdEnd = Integer.toUnsignedLong(cloudEventStorageSettings.getMaxReadRecordsCount());
        seqIdEnd = Math.max(seqIdEnd, 50L);
        return finder.find(tenantId, 0L, seqIdEnd, pageLink);
    }

    protected ListenableFuture<Long> getLongAttrByKey(TenantId tenantId, String attrKey) {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attrKey);
        return Futures.transform(future, attributeKvEntryOpt -> {
            if (attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent()) {
                AttributeKvEntry attributeKvEntry = attributeKvEntryOpt.get();
                return attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
            } else {
                return 0L;
            }
        }, dbCallbackExecutorService);
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
                    statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_PUSHED, tenantId, 1);
                    pendingMsgMap.remove(msg.getUplinkMsgId());
                    log.debug("uplink msg has been processed successfully! {}", msg);
                } else {
                    statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_TMP_FAILED, tenantId, 1);
                    if (msg.getErrorMsg().contains(RATE_LIMIT_REACHED)) {
                        log.warn("uplink msg processing failed! {}", RATE_LIMIT_REACHED);
                        isRateLimitViolated = true;
                    } else {
                        log.error("uplink msg processing failed! Error msg: {}", msg.getErrorMsg());
                    }
                }
                latch.countDown();
            }
        } catch (Exception e) {
            log.error("Can't process uplink msg response [{}]", msg, e);
        }
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        try {
            interruptPreviousSendUplinkMsgsTask();
            if (reconnectFuture != null) {
                reconnectFuture.cancel(true);
                reconnectFuture = null;
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
        initInProgress = false;
    }

    private void initAndUpdateEdgeSettings(EdgeConfiguration edgeConfiguration) throws Exception {
        if (!isSystemTenantPartitionMine()) {
            return;
        }
        tenantId = TenantId.fromUUID(new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB()));

        currentEdgeSettings = edgeSettingsService.findEdgeSettings();
        EdgeSettings newEdgeSettings = constructEdgeSettings(edgeConfiguration);

        if (currentEdgeSettings != null && !newEdgeSettings.getEdgeId().equals(currentEdgeSettings.getEdgeId())) {
            cloudCtx.getTenantProcessor().cleanUp();
            resetQueueOffset();
            currentEdgeSettings = null;
        }

        if (currentEdgeSettings == null) {
            currentEdgeSettings = newEdgeSettings;
        } else {
            log.trace("Using edge settings from DB {}", this.currentEdgeSettings);
            currentEdgeSettings.setName(newEdgeSettings.getName());
            currentEdgeSettings.setType(newEdgeSettings.getType());
            currentEdgeSettings.setRoutingKey(newEdgeSettings.getRoutingKey());
        }

        cloudCtx.getTenantProcessor().createTenantIfNotExists(tenantId);
        boolean edgeCustomerIdUpdated = setOrUpdateCustomerId(edgeConfiguration);
        if (edgeCustomerIdUpdated) {
            cloudCtx.getCustomerProcessor().createCustomerIfNotExists(tenantId, edgeConfiguration);
        }

        if (shouldPerformInitialSync) {
            log.trace("Sending sync request, fullSyncRequired {}", currentEdgeSettings.isFullSyncRequired());
            edgeRpcClient.sendSyncRequestMsg(currentEdgeSettings.isFullSyncRequired());
            syncInProgress = true;
            shouldPerformInitialSync = false;
        }

        edgeSettingsService.saveEdgeSettings(tenantId, currentEdgeSettings);

        saveOrUpdateEdge(tenantId, edgeConfiguration);

        updateConnectivityStatus(true);

        if (cloudEventMigrationService != null && (!cloudEventMigrationService.isMigrated() || !cloudEventMigrationService.isTsMigrated())) {
            cloudEventMigrationService.migrateUnprocessedEventToKafka();
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

    private void saveOrUpdateEdge(TenantId tenantId, EdgeConfiguration edgeConfiguration) throws Exception {
        EdgeId edgeId = getEdgeId(edgeConfiguration);
        cloudCtx.getEdgeProcessor().processEdgeConfigurationMsgFromCloud(tenantId, edgeConfiguration);
        cloudCtx.getCloudEventService().saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.ATTRIBUTES_REQUEST, edgeId, null);
        cloudCtx.getCloudEventService().saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.RELATION_REQUEST, edgeId, null);
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
        if (syncInProgress && downlinkMsg.hasSyncCompletedMsg()) {
            log.trace("[{}] downlinkMsg hasSyncCompletedMsg = true", downlinkMsg);
            syncInProgress = false;
        }
        ListenableFuture<List<Void>> future = downlinkMessageService.processDownlinkMsg(tenantId, customerId, downlinkMsg, this.currentEdgeSettings);
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

        if (reconnectFuture == null) {
            reconnectFuture = reconnectExecutor.scheduleAtFixedRate(() -> {
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
        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityId(tenantId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new LongDataEntry(key, value))
                .notifyDevice(true)
                .callback(new AttributeSaveCallback(key, value))
                .build());
    }

    private void save(String key, boolean value) {
        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityId(tenantId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new BooleanDataEntry(key, value))
                .notifyDevice(true)
                .callback(new AttributeSaveCallback(key, value))
                .build());
    }

    private record AttributeSaveCallback(String key, Object value) implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
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
                        cloudCtx.getTelemetryProcessor().convertTelemetryEventToUplink(cloudEvent.getTenantId(), cloudEvent);
                case ATTRIBUTES_REQUEST -> cloudCtx.getTelemetryProcessor().convertAttributesRequestEventToUplink(cloudEvent);
                case RELATION_REQUEST -> cloudCtx.getRelationProcessor().convertRelationRequestEventToUplink(cloudEvent);
                case CALCULATED_FIELD_REQUEST -> cloudCtx.getCalculatedFieldProcessor().convertCalculatedFieldRequestEventToUplink(cloudEvent);
                case RPC_CALL -> cloudCtx.getDeviceProcessor().convertRpcCallEventToUplink(cloudEvent);
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
        return cloudCtx.getProcessor(cloudEvent.getType()).convertCloudEventToUplink(cloudEvent);
    }

    protected ListenableFuture<Boolean> processCloudEvents(List<CloudEvent> cloudEvents, boolean isGeneralMsg) {
        uplinkSendLock.lock();
        try {
            if (!isGeneralMsg && isGeneralProcessInProgress) {
                return Futures.immediateFuture(true);
            }
            interruptPreviousSendUplinkMsgsTask();
            sendUplinkFutureResult = SettableFuture.create();

            log.trace("[{}] event(s) are going to be converted.", cloudEvents.size());
            List<UplinkMsg> uplinkMsgPack = cloudEvents.stream()
                    .map(this::convertEventToUplink)
                    .filter(Objects::nonNull)
                    .toList();

            if (uplinkMsgPack.isEmpty()) {
                return Futures.immediateFuture(false);
            }

            processMsgPack(uplinkMsgPack, isGeneralMsg);
        } finally {
            uplinkSendLock.unlock();
        }
        return sendUplinkFutureResult;
    }

    private void interruptPreviousSendUplinkMsgsTask() {
        if (sendUplinkFutureResult != null) {
            try {
                // wait before interrupting sending previous uplink pack
                sendUplinkFutureResult.get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {}

            if (!sendUplinkFutureResult.isDone()) {
                log.debug("[{}] Stopping send uplink future now!", tenantId);
                sendUplinkFutureResult.set(true);
            }
        }
        if (sendUplinkFuture != null && !sendUplinkFuture.isCancelled()) {
            sendUplinkFuture.cancel(true);
            sendUplinkFuture = null;
        }
    }

    private void processMsgPack(List<UplinkMsg> uplinkMsgPack, boolean isGeneralMsg) {
        pendingMsgMap.clear();
        uplinkMsgPack.forEach(msg -> pendingMsgMap.put(msg.getUplinkMsgId(), msg));
        sendUplinkFuture = uplinkExecutor.schedule(() -> {
            try {
                int attempt = 1;
                boolean success;
                do {
                    log.trace("[{}] uplink msg(s) are going to be send.", pendingMsgMap.values().size());

                    long startTime = System.currentTimeMillis();
                    success = sendUplinkMsgPack(new LinkedBlockingQueue<>(pendingMsgMap.values())) && pendingMsgMap.isEmpty();

                    if (!success) {
                        String batchPrefix = isGeneralMsg ? "General" : "Timeseries";
                        log.warn("Failed to deliver {} batch (size: {}) on attempt {}", batchPrefix, pendingMsgMap.values().size(), attempt);
                        log.trace("Entities in failed batch: {}", pendingMsgMap.values());
                        try {
                            Thread.sleep(cloudEventStorageSettings.getSleepIntervalBetweenBatches());

                            if (isRateLimitViolated) {
                                isRateLimitViolated = false;
                                TimeUnit.SECONDS.sleep(60);
                            }
                        } catch (InterruptedException e) {
                            log.error("Error during sleep between batches or on rate limit violation", e);
                        }
                    } else {
                        log.info("Sending of [{}] uplink msg(s) took {} ms.", uplinkMsgPack.size(), System.currentTimeMillis() - startTime);
                    }

                    attempt++;

                    if (isGeneralMsg && attempt > MAX_SEND_UPLINK_ATTEMPTS) {
                        log.warn("Failed to deliver the batch: after {} attempts. Next messages are going to be discarded {}",
                                MAX_SEND_UPLINK_ATTEMPTS, pendingMsgMap.values());
                        statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_PERMANENTLY_FAILED, tenantId, pendingMsgMap.size());
                        sendUplinkFutureResult.set(false);
                        return;
                    }
                } while (!success);
                sendUplinkFutureResult.set(false);
            } catch (Exception e) {
                sendUplinkFutureResult.set(true);
                log.error("Error during send uplink msg", e);
            }
        }, 0L, TimeUnit.MILLISECONDS);
    }

    private boolean sendUplinkMsgPack(LinkedBlockingQueue<UplinkMsg> orderedPendingMsgQueue) {
        sendingInProgress = true;
        try {
            latch = new CountDownLatch(pendingMsgMap.values().size());
            orderedPendingMsgQueue.forEach(this::sendUplinkMsg);

            return latch.await(uplinkPackTimeoutSec, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Interrupted while waiting for latch, isGeneralProcessInProgress={}", isGeneralProcessInProgress, e);
            for (UplinkMsg value : pendingMsgMap.values()) {
                log.warn("Message not send due to exception: {}", value);
            }
            return false;
        } finally {
            sendingInProgress = false;
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
            statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_PERMANENTLY_FAILED, tenantId, 1);
            pendingMsgMap.remove(uplinkMsg.getUplinkMsgId());
            latch.countDown();
        }
    }

    private boolean isSystemTenantPartitionMine() {
        return partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
    }

    @FunctionalInterface
    protected interface CloudEventFinder {
        PageData<CloudEvent> find(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);

    }

}
