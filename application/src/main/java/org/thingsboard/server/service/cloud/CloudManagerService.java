/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.CustomerCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EdgeCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityGroupCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityViewCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.GroupPermissionCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RelationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RuleChainCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TelemetryCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WidgetBundleCloudProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Value("${cloud.routingKey}")
    private String routingKey;

    @Value("${cloud.secret}")
    private String routingSecret;

    @Value("${cloud.reconnect_timeout}")
    private long reconnectTimeoutMs;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

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
    private RuleChainCloudProcessor ruleChainProcessor;

    @Autowired
    private GroupPermissionCloudProcessor groupPermissionProcessor;

    @Autowired
    private EntityGroupCloudProcessor entityGroupProcessor;

    @Autowired
    private TenantCloudProcessor tenantProcessor;

    @Autowired
    private CustomerCloudProcessor customerProcessor;

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
                        TimePageLink pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                                0, null, null, queueStartTs, System.currentTimeMillis());
                        if (newCloudEventsAvailable(pageLink)) {
                            try {
                                // sleep 1 second to make sure that sql queue write is completed for other events
                                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                            } catch (Exception ignored) {}
                            PageData<CloudEvent> pageData;
                            UUID ifOffset = null;
                            boolean success = true;
                            do {
                                pageData = cloudEventService.findCloudEvents(tenantId, pageLink);
                                if (initialized) {
                                    log.trace("[{}] event(s) are going to be converted.", pageData.getData().size());
                                    List<UplinkMsg> uplinkMsgsPack = convertToUplinkMsgsPack(pageData.getData());
                                    success = sendUplinkMsgsPack(uplinkMsgsPack);
                                    ifOffset = pageData.getData().get(pageData.getData().size() - 1).getUuidId();
                                    if (success) {
                                        pageLink = pageLink.nextPageLink();
                                    }
                                }
                            } while (initialized && (!success || pageData.hasNext()));
                            if (ifOffset != null) {
                                Long newStartTs = Uuids.unixTimestamp(ifOffset);
                                try {
                                    updateQueueStartTs(newStartTs);
                                    log.debug("Queue offset was updated [{}][{}]", ifOffset, newStartTs);
                                } catch (Exception e) {
                                    log.error("[{}] Failed to update queue offset [{}]", ifOffset, e);
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

    private boolean newCloudEventsAvailable(TimePageLink pageLink) {
        PageData<CloudEvent> cloudEvents = cloudEventService.findCloudEvents(tenantId, pageLink);
        return !cloudEvents.getData().isEmpty();
    }

    private boolean sendUplinkMsgsPack(List<UplinkMsg> uplinkMsgsPack) throws InterruptedException {
        uplinkMsgsPackLock.lock();
        try {
            int attempt = 1;
            boolean success;
            pendingMsgsMap.clear();
            uplinkMsgsPack.forEach(msg -> pendingMsgsMap.put(msg.getUplinkMsgId(), msg));
            do {
                log.trace("[{}] uplink msg(s) are going to be send.", pendingMsgsMap.values().size());
                latch = new CountDownLatch(pendingMsgsMap.values().size());
                List<UplinkMsg> copy = new ArrayList<>(pendingMsgsMap.values());
                for (UplinkMsg uplinkMsg : copy) {
                    edgeRpcClient.sendUplinkMsg(uplinkMsg);
                }
                success = latch.await(10, TimeUnit.SECONDS);
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
                    case UPDATED:
                    case ADDED:
                    case DELETED:
                    case ALARM_ACK:
                    case ALARM_CLEAR:
                    case CREDENTIALS_UPDATED:
                    case RELATION_ADD_OR_UPDATE:
                    case RELATION_DELETED:
                    case ADDED_TO_ENTITY_GROUP:
                    case REMOVED_FROM_ENTITY_GROUP:
                        uplinkMsg = convertEntityEventToUplink(this.tenantId, cloudEvent);
                        break;
                    case ATTRIBUTES_UPDATED:
                    case POST_ATTRIBUTES:
                    case ATTRIBUTES_DELETED:
                    case TIMESERIES_UPDATED:
                        uplinkMsg = telemetryProcessor.convertTelemetryEventToUplink(cloudEvent);
                        break;
                    case ATTRIBUTES_REQUEST:
                        uplinkMsg = telemetryProcessor.convertAttributesRequestEventToUplink(cloudEvent);
                        break;
                    case RELATION_REQUEST:
                        uplinkMsg = relationProcessor.convertRelationRequestEventToUplink(cloudEvent);
                        break;
                    case RULE_CHAIN_METADATA_REQUEST:
                        uplinkMsg = ruleChainProcessor.convertRuleChainMetadataRequestEventToUplink(cloudEvent);
                        break;
                    case CREDENTIALS_REQUEST:
                        uplinkMsg = entityProcessor.convertCredentialsRequestEventToUplink(cloudEvent);
                        break;
                    case GROUP_ENTITIES_REQUEST:
                        uplinkMsg = entityGroupProcessor.processGroupEntitiesRequestMsgToCloud(cloudEvent);
                        break;
                    case GROUP_PERMISSIONS_REQUEST:
                        uplinkMsg = groupPermissionProcessor.processEntityGroupPermissionsRequestMsgToCloud(cloudEvent);
                        break;
                    case RPC_CALL:
                        uplinkMsg = deviceProcessor.convertRpcCallEventToUplink(cloudEvent);
                        break;
                    case WIDGET_BUNDLE_TYPES_REQUEST:
                        uplinkMsg = widgetBundleProcessor.convertWidgetBundleTypesRequestEventToUplink(cloudEvent);
                        break;
                    case ENTITY_VIEW_REQUEST:
                        uplinkMsg = entityViewProcessor.convertEntityViewRequestEventToUplink(cloudEvent);
                        break;
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
        switch (cloudEvent.getType()) {
            case DEVICE:
                return deviceProcessor.convertDeviceEventToUplink(tenantId, cloudEvent);
            case ALARM:
                return alarmProcessor.convertAlarmEventToUplink(cloudEvent);
            case RELATION:
                return relationProcessor.convertRelationEventToUplink(cloudEvent);
            default:
                log.warn("Unsupported cloud event type [{}]", cloudEvent);
                return null;
        }
    }

    private ListenableFuture<Long> getQueueStartTs() {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(tenantId, tenantId, DataConstants.SERVER_SCOPE, QUEUE_START_TS_ATTR_KEY);
        return Futures.transform(future, attributeKvEntryOpt -> {
            if (attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent()) {
                AttributeKvEntry attributeKvEntry = attributeKvEntryOpt.get();
                return attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
            } else {
                return 0L;
            }
        }, dbCallbackExecutorService);
    }

    private void updateQueueStartTs(Long newStartTs) throws ExecutionException, InterruptedException {
        log.trace("updating QueueStartTs [{}]", newStartTs);
        List<AttributeKvEntry> attributes = Collections.singletonList(
                new BaseAttributeKvEntry(
                        new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs),
                        System.currentTimeMillis()));
        attributesService.save(tenantId, tenantId, DataConstants.SERVER_SCOPE, attributes).get();
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        try {
            if (msg.getSuccess()) {
                pendingMsgsMap.remove(msg.getUplinkMsgId());
                log.debug("[{}] Msg has been processed successfully! {}", routingKey, msg);
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

            if ("PE".equals(edgeConfiguration.getCloudType())) {
                initAndUpdateEdgeSettings(edgeConfiguration);
            } else {
                new Thread(() -> {
                    log.error("Terminating application. PE edge can be connected only to PE server version...");
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

        // TODO: voba - check this
        UUID customerUUID = new UUID(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
        CustomerId customerId = new CustomerId(customerUUID);
        EntityId ownerId = !customerId.isNullUid() ? customerId : tenantId;
        whiteLabelingService.saveOrUpdateEdgeLoginWhiteLabelSettings(tenantId, ownerId);

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

    private void saveOrUpdateEdge(TenantId tenantId, EdgeConfiguration edgeConfiguration) throws Exception {
        EdgeId edgeId = getEdgeId(edgeConfiguration);
        edgeCloudProcessor.processEdgeConfigurationMsgFromCloud(tenantId, edgeConfiguration);
        cloudEventService.saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.ATTRIBUTES_REQUEST, edgeId, null, null, queueStartTs);
        cloudEventService.saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.RELATION_REQUEST, edgeId, null, null, queueStartTs);
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
            this.syncInProgress = false;
        }
        ListenableFuture<List<Void>> future =
                downlinkMessageService.processDownlinkMsg(tenantId, downlinkMsg, this.currentEdgeSettings, queueStartTs);
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
                        edgeRpcClient.sendSyncRequestMsg(true, false);
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
        tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, tenantId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(key, value));
    }

    private void save(String key, boolean value) {
        tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, tenantId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(key, value));
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

