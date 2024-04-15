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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.cluster.TbClusterService;
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
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventService;
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
import org.thingsboard.server.service.cloud.rpc.processor.EntityCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityViewCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RelationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.ResourceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RuleChainCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TelemetryCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WidgetBundleCloudProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class CloudManagerService {

    private static final ReentrantLock uplinkMsgsPackLock = new ReentrantLock();

    private static final int MAX_UPLINK_ATTEMPTS = 10; // max number of attemps to send downlink message if edge connected

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";
    private static final String QUEUE_SEQ_ID_OFFSET_ATTR_KEY = "queueSeqIdOffset";
    private static final String RATE_LIMIT_REACHED = "Rate limit reached";

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
    private AttributesService attributesService;

    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private DownlinkMessageService downlinkMessageService;

    @Autowired
    private EdgeRpcClient edgeRpcClient;

    @Autowired
    private EdgeCloudProcessor edgeCloudProcessor;

    @Autowired
    private RelationCloudProcessor relationProcessor;

    @Autowired
    private DeviceCloudProcessor deviceProcessor;

    @Autowired
    private DeviceProfileCloudProcessor deviceProfileProcessor;

    @Autowired
    private AlarmCloudProcessor alarmProcessor;

    @Autowired
    private EntityCloudProcessor entityProcessor;

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
    private RuleChainCloudProcessor ruleChainProcessor;

    @Autowired
    private TenantCloudProcessor tenantProcessor;

    @Autowired
    private CustomerCloudProcessor customerProcessor;

    @Autowired
    private ResourceCloudProcessor resourceCloudProcessor;

    @Autowired
    private CloudEventService cloudEventService;

    @Autowired
    private ConfigurableApplicationContext context;

    private CountDownLatch latch;

    private EdgeSettings currentEdgeSettings;

    private Long queueStartTs;

    private ExecutorService executor;
    private ScheduledExecutorService reconnectScheduler;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService shutdownExecutor;
    private volatile boolean isRateLimitViolated = false;
    private volatile boolean initialized;
    private volatile boolean syncInProgress = false;

    private final ConcurrentMap<Integer, UplinkMsg> pendingMsgsMap = new ConcurrentHashMap<>();

    private TenantId tenantId;
    private CustomerId customerId;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (validateRoutingKeyAndSecret()) {
            log.info("Starting Cloud Edge service");
            edgeRpcClient.connect(routingKey, routingSecret,
                    this::onUplinkResponse,
                    this::onEdgeUpdate,
                    this::onDownlink,
                    this::scheduleReconnect);
            executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("cloud-manager"));
            reconnectScheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-reconnect"));
            processHandleMessages();
        }
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

    @PreDestroy
    public void destroy() throws InterruptedException {
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
        if (executor != null) {
            executor.shutdownNow();
        }
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdownNow();
        }
        log.info("[{}] Destroy was successful", edgeId);
    }

    private void processHandleMessages() {
        executor.submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    if (initialized) {
                        queueStartTs = getQueueStartTs().get();
                        Long queueSeqIdStart = getQueueSeqIdStart().get();
                        TimePageLink pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                                0, null, null, queueStartTs, System.currentTimeMillis());
                        if (newCloudEventsAvailable(queueSeqIdStart, pageLink)) {
                            PageData<CloudEvent> cloudEvents;
                            boolean success = true;
                            CloudEvent latestCloudEvent = null;
                            do {
                                cloudEvents = cloudEventService.findCloudEvents(tenantId, queueSeqIdStart, null, pageLink);
                                if (initialized) {
                                    if (cloudEvents.getData().isEmpty()) {
                                        log.info("seqId column of cloud_event table started new cycle");
                                        cloudEvents = findCloudEventsFromBeginning(pageLink);
                                    }
                                    log.trace("[{}] event(s) are going to be converted.", cloudEvents.getData().size());
                                    List<UplinkMsg> uplinkMsgsPack = convertToUplinkMsgsPack(cloudEvents.getData());
                                    if (!uplinkMsgsPack.isEmpty()) {
                                        success = sendUplinkMsgsPack(uplinkMsgsPack);
                                    } else {
                                        success = true;
                                    }
                                    if (!cloudEvents.getData().isEmpty()) {
                                        latestCloudEvent = cloudEvents.getData().get(cloudEvents.getData().size() - 1);
                                    }
                                    if (success) {
                                        pageLink = pageLink.nextPageLink();
                                    }
                                }
                            } while (initialized && (!success || cloudEvents.hasNext()));
                            if (latestCloudEvent != null) {
                                try {
                                    Long newStartTs = Uuids.unixTimestamp(latestCloudEvent.getUuidId());
                                    updateQueueStartTsSeqIdOffset(newStartTs, latestCloudEvent.getSeqId());
                                    log.debug("Queue offset was updated [{}][{}][{}]", latestCloudEvent.getUuidId(), newStartTs, latestCloudEvent.getSeqId());
                                } catch (Exception e) {
                                    log.error("Failed to update queue offset [{}]", latestCloudEvent);
                                }
                            }
                        }
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

    private boolean newCloudEventsAvailable(Long queueSeqIdStart, TimePageLink pageLink) {
        PageData<CloudEvent> cloudEvents = cloudEventService.findCloudEvents(tenantId, queueSeqIdStart, null, pageLink);
        if (cloudEvents.getData().isEmpty()) {
            // check if new cycle started (seq_id starts from '1')
            cloudEvents = findCloudEventsFromBeginning(pageLink);
            return cloudEvents.getData().stream().anyMatch(ce -> ce.getSeqId() == 1);
        } else {
            return true;
        }
    }

    private PageData<CloudEvent> findCloudEventsFromBeginning(TimePageLink pageLink) {
        long seqIdEnd = Integer.toUnsignedLong(cloudEventStorageSettings.getMaxReadRecordsCount());
        seqIdEnd = Math.max(seqIdEnd, 50L);
        return cloudEventService.findCloudEvents(tenantId, 0L, seqIdEnd, pageLink);
    }

    private boolean sendUplinkMsgsPack(List<UplinkMsg> uplinkMsgsPack) throws InterruptedException {
        uplinkMsgsPackLock.lock();
        try {
            int attempt = 1;
            boolean success;
            LinkedBlockingQueue<UplinkMsg> orderedPendingMsgsQueue = new LinkedBlockingQueue<>();
            pendingMsgsMap.clear();
            uplinkMsgsPack.forEach(msg -> {
                pendingMsgsMap.put(msg.getUplinkMsgId(), msg);
                orderedPendingMsgsQueue.add(msg);
            });
            do {
                log.trace("[{}] uplink msg(s) are going to be send.", pendingMsgsMap.values().size());
                latch = new CountDownLatch(pendingMsgsMap.values().size());
                for (UplinkMsg uplinkMsg : orderedPendingMsgsQueue) {
                    if (edgeRpcClient.getServerMaxInboundMessageSize() != 0 && uplinkMsg.getSerializedSize() > edgeRpcClient.getServerMaxInboundMessageSize()) {
                        log.error("Uplink msg size [{}] exceeds server max inbound message size [{}]. Skipping this message. " +
                                        "Please increase value of EDGES_RPC_MAX_INBOUND_MESSAGE_SIZE env variable on the server and restart it." +
                                        "Message {}",
                                uplinkMsg.getSerializedSize(), edgeRpcClient.getServerMaxInboundMessageSize(), uplinkMsg);
                        pendingMsgsMap.remove(uplinkMsg.getUplinkMsgId());
                        latch.countDown();
                    } else {
                        edgeRpcClient.sendUplinkMsg(uplinkMsg);
                    }
                }
                success = latch.await(uplinkPackTimeoutSec, TimeUnit.SECONDS);
                success = success && pendingMsgsMap.isEmpty();
                if (!success) {
                    log.warn("Failed to deliver the batch: {}, attempt: {}", pendingMsgsMap.values(), attempt);
                }
                if (initialized && !success) {
                    try {
                        Thread.sleep(cloudEventStorageSettings.getSleepIntervalBetweenBatches());
                    } catch (InterruptedException e) {
                        log.error("Error during sleep between batches", e);
                    }
                }
                if (initialized && !success && isRateLimitViolated) {
                    isRateLimitViolated = false;
                    try {
                        TimeUnit.SECONDS.sleep(60);
                    } catch (InterruptedException e) {
                        log.error("Error during sleep on rate limit violation", e);
                    }
                }
                attempt++;
                if (attempt > MAX_UPLINK_ATTEMPTS) {
                    log.warn("Failed to deliver the batch after {} attempts. Next messages are going to be discarded {}",
                            MAX_UPLINK_ATTEMPTS, pendingMsgsMap.values());
                    return true;
                }
            } while (initialized && !success);
            return success;
        } finally {
            uplinkMsgsPackLock.unlock();
        }
    }

    private List<UplinkMsg> convertToUplinkMsgsPack(List<CloudEvent> cloudEvents) {
        List<UplinkMsg> result = new ArrayList<>();
        for (CloudEvent cloudEvent : cloudEvents) {
            log.trace("Converting cloud event [{}]", cloudEvent);
            UplinkMsg uplinkMsg = null;
            try {
                switch (cloudEvent.getAction()) {
                    case UPDATED, ADDED, DELETED, ALARM_ACK, ALARM_CLEAR, ALARM_DELETE, CREDENTIALS_UPDATED, RELATION_ADD_OR_UPDATE, RELATION_DELETED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER, ADDED_COMMENT, UPDATED_COMMENT, DELETED_COMMENT ->
                            uplinkMsg = convertEntityEventToUplink(this.tenantId, cloudEvent);
                    case ATTRIBUTES_UPDATED, POST_ATTRIBUTES, ATTRIBUTES_DELETED, TIMESERIES_UPDATED ->
                            uplinkMsg = telemetryProcessor.convertTelemetryEventToUplink(this.tenantId, cloudEvent);
                    case ATTRIBUTES_REQUEST -> uplinkMsg = telemetryProcessor.convertAttributesRequestEventToUplink(cloudEvent);
                    case RELATION_REQUEST -> uplinkMsg = relationProcessor.convertRelationRequestEventToUplink(cloudEvent);
                    case RULE_CHAIN_METADATA_REQUEST -> uplinkMsg = ruleChainProcessor.convertRuleChainMetadataRequestEventToUplink(cloudEvent);
                    case CREDENTIALS_REQUEST -> uplinkMsg = entityProcessor.convertCredentialsRequestEventToUplink(cloudEvent);
                    case RPC_CALL -> uplinkMsg = deviceProcessor.convertRpcCallEventToUplink(cloudEvent);
                    case WIDGET_BUNDLE_TYPES_REQUEST -> uplinkMsg = widgetBundleProcessor.convertWidgetBundleTypesRequestEventToUplink(cloudEvent);
                    case ENTITY_VIEW_REQUEST -> uplinkMsg = entityViewProcessor.convertEntityViewRequestEventToUplink(cloudEvent);
                }
            } catch (Exception e) {
                log.error("Exception during converting events from queue, skipping event [{}]", cloudEvent, e);
            }
            if (uplinkMsg != null) {
                result.add(uplinkMsg);
            }
        }
        return result;
    }

    private UplinkMsg convertEntityEventToUplink(TenantId tenantId, CloudEvent cloudEvent) {
        log.trace("Executing convertEntityEventToUplink, cloudEvent [{}], edgeEventAction [{}]", cloudEvent, cloudEvent.getAction());
        EdgeVersion edgeVersion = EdgeVersion.V_LATEST;
        switch (cloudEvent.getType()) {
            case DEVICE:
                return deviceProcessor.convertDeviceEventToUplink(tenantId, cloudEvent, edgeVersion);
            case DEVICE_PROFILE:
                return deviceProfileProcessor.convertDeviceProfileEventToUplink(cloudEvent, edgeVersion);
            case ALARM:
                return alarmProcessor.convertAlarmEventToUplink(cloudEvent, edgeVersion);
            case ALARM_COMMENT:
                return alarmProcessor.convertAlarmCommentEventToUplink(cloudEvent, edgeVersion);
            case ASSET:
                return assetProcessor.convertAssetEventToUplink(cloudEvent, edgeVersion);
            case ASSET_PROFILE:
                return assetProfileProcessor.convertAssetProfileEventToUplink(cloudEvent, edgeVersion);
            case DASHBOARD:
                return dashboardProcessor.convertDashboardEventToUplink(cloudEvent, edgeVersion);
            case ENTITY_VIEW:
                return entityViewProcessor.convertEntityViewEventToUplink(cloudEvent, edgeVersion);
            case RELATION:
                return relationProcessor.convertRelationEventToUplink(cloudEvent, edgeVersion);
            case TB_RESOURCE:
                return resourceCloudProcessor.convertResourceEventToUplink(cloudEvent, edgeVersion);
            default:
                log.warn("Unsupported cloud event type [{}]", cloudEvent);
                return null;
        }
    }

    private ListenableFuture<Long> getQueueStartTs() {
        return getLongAttrByKey(QUEUE_START_TS_ATTR_KEY);
    }

    private ListenableFuture<Long> getQueueSeqIdStart() {
        return getLongAttrByKey(QUEUE_SEQ_ID_OFFSET_ATTR_KEY);
    }

    private ListenableFuture<Long> getLongAttrByKey(String attrKey) {
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

    private void updateQueueStartTsSeqIdOffset(Long startTs, Long seqIdOffset) {
        log.trace("updateQueueStartTsSeqIdOffset [{}][{}]", startTs, seqIdOffset);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, startTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_SEQ_ID_OFFSET_ATTR_KEY, seqIdOffset), System.currentTimeMillis()));
        attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes);
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        try {
            if (msg.getSuccess()) {
                pendingMsgsMap.remove(msg.getUplinkMsgId());
                log.debug("[{}] Msg has been processed successfully! {}", routingKey, msg);
            } else if (msg.getErrorMsg().contains(RATE_LIMIT_REACHED)) {
                log.warn("[{}] Msg processing failed! {}", routingKey, RATE_LIMIT_REACHED);
                isRateLimitViolated = true;
            } else {
                log.error("[{}] Msg processing failed! Error msg: {}", routingKey, msg.getErrorMsg());
            }
            latch.countDown();
        } catch (Exception e) {
            log.error("Can't process uplink response message [{}]", msg, e);
        }
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
        }
    }

    private void initAndUpdateEdgeSettings(EdgeConfiguration edgeConfiguration) throws Exception {
        this.tenantId = new TenantId(new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB()));

        this.currentEdgeSettings = cloudEventService.findEdgeSettings(this.tenantId);
        EdgeSettings newEdgeSettings = constructEdgeSettings(edgeConfiguration);
        if (this.currentEdgeSettings == null || !this.currentEdgeSettings.getEdgeId().equals(newEdgeSettings.getEdgeId())) {
            tenantProcessor.cleanUp();
            this.currentEdgeSettings = newEdgeSettings;
        } else {
            log.trace("Using edge settings from DB {}", this.currentEdgeSettings);
        }

        queueStartTs = getQueueStartTs().get();
        tenantProcessor.createTenantIfNotExists(this.tenantId, queueStartTs);
        boolean edgeCustomerIdUpdated = setOrUpdateCustomerId(edgeConfiguration);
        if (edgeCustomerIdUpdated) {
            customerProcessor.createCustomerIfNotExists(this.tenantId, edgeConfiguration);
        }
        // TODO: voba - should sync be executed in some other cases ???
        log.trace("Sending sync request, fullSyncRequired {}, edgeCustomerIdUpdated {}", this.currentEdgeSettings.isFullSyncRequired(), edgeCustomerIdUpdated);
        edgeRpcClient.sendSyncRequestMsg(this.currentEdgeSettings.isFullSyncRequired() | edgeCustomerIdUpdated);
        this.syncInProgress = true;

        cloudEventService.saveEdgeSettings(tenantId, this.currentEdgeSettings);

        saveOrUpdateEdge(tenantId, edgeConfiguration);

        updateConnectivityStatus(true);

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
