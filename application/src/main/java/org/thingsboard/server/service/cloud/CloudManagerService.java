/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Tenant;
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
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.cloud.rpc.CloudEventUtils;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityViewCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RelationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RuleChainCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TelemetryCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WidgetBundleCloudProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CloudManagerService extends BaseCloudEventService {

    private static final ReentrantLock uplinkMsgsPackLock = new ReentrantLock();
    private static final ReentrantLock pendingMsgsMapLock = new ReentrantLock();

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
    private RuleChainService ruleChainService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private DownlinkMessageService downlinkMessageService;

    @Autowired
    private EdgeRpcClient edgeRpcClient;

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
    private RuleChainCloudProcessor ruleChainProcessor;

    @Autowired
    private InstallScripts installScripts;

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

    private final Map<Integer, UplinkMsg> pendingMsgsMap = new HashMap<>();

    private TenantId tenantId;

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
                        TimePageLink pageLink =
                                CloudEventUtils.createCloudEventTimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(), queueStartTs);
                        PageData<CloudEvent> pageData;
                        UUID ifOffset = null;
                        boolean success = true;
                        do {
                            pageData = cloudEventService.findCloudEvents(tenantId, pageLink);
                            if (initialized && !pageData.getData().isEmpty()) {
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

    private boolean sendUplinkMsgsPack(List<UplinkMsg> uplinkMsgsPack) throws InterruptedException {
        uplinkMsgsPackLock.lock();
        try {
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
                if (!success || pendingMsgsMap.values().size() > 0) {
                    pendingMsgsMapLock.lock();
                    try {
                        log.warn("Failed to deliver the batch: {}", pendingMsgsMap.values());
                    } finally {
                        pendingMsgsMapLock.unlock();
                    }
                }
                if (initialized && (!success || pendingMsgsMap.values().size() > 0)) {
                    try {
                        Thread.sleep(cloudEventStorageSettings.getSleepIntervalBetweenBatches());
                    } catch (InterruptedException e) {
                        log.error("Error during sleep between batches", e);
                    }
                }
            } while (initialized && (!success || pendingMsgsMap.values().size() > 0));
            return success;
        } finally {
            uplinkMsgsPackLock.unlock();
        }
    }

    private List<UplinkMsg> convertToUplinkMsgsPack(List<CloudEvent> cloudEvents) {
        List<UplinkMsg> result = new ArrayList<>();
        for (CloudEvent cloudEvent : cloudEvents) {
            log.trace("Processing cloud event [{}]", cloudEvent);
            UplinkMsg uplinkMsg = null;
            try {
                EdgeEventActionType edgeEventAction = EdgeEventActionType.valueOf(cloudEvent.getCloudEventAction());
                switch (edgeEventAction) {
                    case UPDATED:
                    case ADDED:
                    case DELETED:
                    case ALARM_ACK:
                    case ALARM_CLEAR:
                    case CREDENTIALS_UPDATED:
                    case RELATION_ADD_OR_UPDATE:
                    case RELATION_DELETED:
                        uplinkMsg = processEntityMessage(this.tenantId, cloudEvent, edgeEventAction);
                        break;
                    case ATTRIBUTES_UPDATED:
                    case POST_ATTRIBUTES:
                    case ATTRIBUTES_DELETED:
                    case TIMESERIES_UPDATED:
                        uplinkMsg = telemetryProcessor.processTelemetryMessageMsgToCloud(cloudEvent);
                        break;
                    case ATTRIBUTES_REQUEST:
                        uplinkMsg = telemetryProcessor.processAttributesRequestMsgToCloud(cloudEvent);
                        break;
                    case RELATION_REQUEST:
                        uplinkMsg = relationProcessor.processRelationRequestMsgToCloud(cloudEvent);
                        break;
                    case RULE_CHAIN_METADATA_REQUEST:
                        uplinkMsg = ruleChainProcessor.processRuleChainMetadataRequestMsgToCloud(cloudEvent);
                        break;
                    case CREDENTIALS_REQUEST:
                        uplinkMsg = entityProcessor.processCredentialsRequestMsgToCloud(cloudEvent);
                        break;
                    case RPC_CALL:
                        uplinkMsg = deviceProcessor.processRpcCallResponseMsgToCloud(cloudEvent);
                        break;
                    case DEVICE_PROFILE_DEVICES_REQUEST:
                        uplinkMsg = deviceProfileProcessor.processDeviceProfileDevicesRequestMsgToCloud(cloudEvent);
                        break;
                    case WIDGET_BUNDLE_TYPES_REQUEST:
                        uplinkMsg = widgetBundleProcessor.processWidgetBundleTypesRequestMsgToCloud(cloudEvent);
                        break;
                    case ENTITY_VIEW_REQUEST:
                        uplinkMsg = entityViewProcessor.processEntityViewRequestMsgToCloud(cloudEvent);
                        break;
                }
            } catch (Exception e) {
                log.error("Exception during processing events from queue, skipping event [{}]", cloudEvent, e);
            }
            if (uplinkMsg != null) {
                result.add(uplinkMsg);
            }
        }
        return result;
    }

    private UplinkMsg processEntityMessage(TenantId tenantId, CloudEvent cloudEvent, EdgeEventActionType edgeEventAction)
            throws ExecutionException, InterruptedException {
        UpdateMsgType msgType = getResponseMsgType(EdgeEventActionType.valueOf(cloudEvent.getCloudEventAction()));
        log.trace("Executing processEntityMessage, cloudEvent [{}], edgeEventAction [{}], msgType [{}]", cloudEvent, edgeEventAction, msgType);
        switch (cloudEvent.getCloudEventType()) {
            case DEVICE:
                return deviceProcessor.processDeviceMsgToCloud(tenantId, cloudEvent, msgType, edgeEventAction);
            case ALARM:
                return alarmProcessor.processAlarmMsgToCloud(tenantId, cloudEvent, msgType, edgeEventAction);
            case RELATION:
                return relationProcessor.processRelationMsgToCloud(cloudEvent, msgType);
            default:
                log.warn("Unsupported cloud event type [{}]", cloudEvent);
                return null;
        }
    }

    private UpdateMsgType getResponseMsgType(EdgeEventActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED:
            case RELATION_ADD_OR_UPDATE:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED:
            case RELATION_DELETED:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
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
                pendingMsgsMapLock.lock();
                try {
                    pendingMsgsMap.remove(msg.getUplinkMsgId());
                } finally {
                    pendingMsgsMapLock.unlock();
                }
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
            boolean updatingEdge = this.currentEdgeSettings != null;

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
        UUID tenantUUID = new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB());
        this.tenantId = getOrCreateTenant(new TenantId(tenantUUID)).getTenantId();

        UUID customerUUID = new UUID(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
        CustomerId customerId = new CustomerId(customerUUID);

        EntityId ownerId = !customerId.isNullUid() ? customerId : tenantId;

        this.currentEdgeSettings = cloudEventService.findEdgeSettings(tenantId);
        EdgeSettings newEdgeSetting = constructEdgeSettings(edgeConfiguration);
        if (this.currentEdgeSettings == null || !this.currentEdgeSettings.getEdgeId().equals(newEdgeSetting.getEdgeId())) {
            cleanUp();
            this.currentEdgeSettings = newEdgeSetting;
        } else {
            log.trace("Using edge settings from DB {}", this.currentEdgeSettings);
        }

        // TODO: voba - should sync be executed in some other cases ???
        log.trace("Sending sync request, fullSyncRequired {}", this.currentEdgeSettings.isFullSyncRequired());
        edgeRpcClient.sendSyncRequestMsg(this.currentEdgeSettings.isFullSyncRequired());

        cloudEventService.saveEdgeSettings(tenantId, this.currentEdgeSettings);

        // TODO: voba - verify storage of edge entity
        saveEdge(edgeConfiguration);

        updateConnectivityStatus(true);

        initialized = true;
    }

    private void saveEdge(EdgeConfiguration edgeConfiguration) {
        Edge edge = new Edge();
        UUID edgeUUID = new UUID(edgeConfiguration.getEdgeIdMSB(), edgeConfiguration.getEdgeIdLSB());
        EdgeId edgeId = new EdgeId(edgeUUID);
        edge.setId(edgeId);
        UUID tenantUUID = new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB());
        edge.setTenantId(new TenantId(tenantUUID));
        // TODO: voba - can't assign edge to non-existing customer
        // UUID customerUUID = new UUID(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
        // edge.setCustomerId(new CustomerId(customerUUID));
        edge.setName(edgeConfiguration.getName());
        edge.setType(edgeConfiguration.getType());
        edge.setRoutingKey(edgeConfiguration.getRoutingKey());
        edge.setSecret(edgeConfiguration.getSecret());
        edge.setAdditionalInfo(JacksonUtil.toJsonNode(edgeConfiguration.getAdditionalInfo()));
        edgeService.saveEdge(edge, false);
        saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.ATTRIBUTES_REQUEST, edgeId, null);
        saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.RELATION_REQUEST, edgeId, null);
    }

    private void cleanUp() {
        log.debug("Starting clean up procedure");
        PageData<Tenant> tenants = tenantService.findTenants(new PageLink(Integer.MAX_VALUE));
        for (Tenant tenant : tenants.getData()) {
            cleanUpTenant(tenant);
        }

        Tenant systemTenant = new Tenant();
        systemTenant.setId(TenantId.SYS_TENANT_ID);
        systemTenant.setTitle("System");
        cleanUpTenant(systemTenant);

        log.debug("Clean up procedure successfully finished!");
    }

    private void cleanUpTenant(Tenant tenant) {
        log.debug("Removing entities for the tenant [{}][{}]", tenant.getTitle(), tenant.getId());
        userService.deleteTenantAdmins(tenant.getId());
        PageData<Customer> customers = customerService.findCustomersByTenantId(tenant.getId(), new PageLink(Integer.MAX_VALUE));
        if (customers != null && customers.getData() != null && !customers.getData().isEmpty()) {
            for (Customer customer : customers.getData()) {
                userService.deleteCustomerUsers(tenant.getId(), customer.getId());
            }
        }
        ruleChainService.deleteRuleChainsByTenantId(tenant.getId());
        entityViewService.deleteEntityViewsByTenantId(tenant.getId());
        deviceService.deleteDevicesByTenantId(tenant.getId());
        deviceProfileService.deleteDeviceProfilesByTenantId(tenant.getId());
        assetService.deleteAssetsByTenantId(tenant.getId());
        dashboardService.deleteDashboardsByTenantId(tenant.getId());
        adminSettingsService.deleteAdminSettingsByKey(tenant.getId(), "mail");
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenant.getId());
        cloudEventService.deleteCloudEventsByTenantId(tenant.getId());
        try {
            List<AttributeKvEntry> attributeKvEntries = attributesService.findAll(tenant.getId(), tenant.getId(), DataConstants.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            attributesService.removeAll(tenant.getId(), tenant.getId(), DataConstants.SERVER_SCOPE, attrKeys);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Unable to delete entity groups", e);
        }
    }

    private Tenant getOrCreateTenant(TenantId tenantId) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (tenant != null) {
            return tenant;
        }
        tenant = new Tenant();
        tenant.setTitle("Tenant");
        tenant.setId(tenantId);
        return tenantService.saveTenant(tenant, true);
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
        ListenableFuture<List<Void>> future = downlinkMessageService.processDownlinkMsg(tenantId, downlinkMsg, this.currentEdgeSettings, queueStartTs);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                log.trace("[{}] DownlinkMsg has been processed successfully! DownlinkMsgId {}", routingKey, downlinkMsg.getDownlinkMsgId());
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                        .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                        .setSuccess(true).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to process DownlinkMsg! DownlinkMsgId {}", routingKey, downlinkMsg.getDownlinkMsgId());
                String errorMsg = t.getMessage() != null ? t.getMessage() : "";
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                        .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                        .setSuccess(false).setErrorMsg(errorMsg).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }
        }, MoreExecutors.directExecutor());
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

