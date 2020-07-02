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
package org.thingsboard.server.service.cloud;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityGroupEntitiesRequestMsg;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.cloud.constructor.AlarmUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.cloud.processor.AlarmUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.AssetUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.CustomerUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.DashboardUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.DeviceUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.EntityGroupUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.EntityViewUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.RelationUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.RuleChainUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.SchedulerEventUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.UserUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.WhiteLabelingUpdateProcessor;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.user.UserLoaderService;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.gen.edge.UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;

@Service
@Slf4j
public class CloudManagerService {

    private final Gson gson = new Gson();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";

    @Value("${cloud.routingKey}")
    private String routingKey;

    @Value("${cloud.secret}")
    private String routingSecret;

    @Value("${cloud.reconnect_timeout}")
    private long reconnectTimeoutMs;

    @Autowired
    private CloudNotificationService cloudNotificationService;

    @Autowired
    private CloudEventService cloudEventService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private UserLoaderService userLoaderService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    private RuleChainUpdateProcessor ruleChainUpdateProcessor;

    @Autowired
    private DeviceUpdateProcessor deviceUpdateProcessor;

    @Autowired
    private AssetUpdateProcessor assetUpdateProcessor;

    @Autowired
    private EntityViewUpdateProcessor entityViewUpdateProcessor;

    @Autowired
    private RelationUpdateProcessor relationUpdateProcessor;

    @Autowired
    private DashboardUpdateProcessor dashboardUpdateProcessor;

    @Autowired
    private CustomerUpdateProcessor customerUpdateProcessor;

    @Autowired
    private AlarmUpdateProcessor alarmUpdateProcessor;

    @Autowired
    private UserUpdateProcessor userUpdateProcessor;

    @Autowired
    private EntityGroupUpdateProcessor entityGroupUpdateProcessor;

    @Autowired
    private SchedulerEventUpdateProcessor schedulerEventUpdateProcessor;

    @Autowired
    private WhiteLabelingUpdateProcessor whiteLabelingUpdateProcessor;

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private DeviceUpdateMsgConstructor deviceUpdateMsgConstructor;

    @Autowired
    private AlarmUpdateMsgConstructor alarmUpdateMsgConstructor;

    @Autowired
    private EntityDataMsgConstructor entityDataMsgConstructor;

    @Autowired
    private EdgeRpcClient edgeRpcClient;

    private CountDownLatch latch;

    private ExecutorService executor;
    private ScheduledExecutorService reconnectScheduler;
    private ScheduledFuture<?> scheduledFuture;
    private volatile boolean initialized;

    private TenantId tenantId;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Starting Cloud Edge service");
        edgeRpcClient.connect(routingKey, routingSecret,
                this::onUplinkResponse,
                this::onEdgeUpdate,
                this::onEntityUpdate,
                this::onDownlink,
                this::scheduleReconnect);
        executor = Executors.newSingleThreadExecutor();
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        setTenantId();
        cleanUp();
        processHandleMessages();
    }

    private void cleanUp() {
        userService.deleteTenantAdmins(tenantId);
        TextPageData<Customer> customers = customerService.findCustomersByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
        if (customers != null && customers.getData() != null && !customers.getData().isEmpty()) {
            for (Customer customer : customers.getData()) {
                userService.deleteCustomerUsers(tenantId, customer.getId());
            }
        }
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
        entityViewService.deleteEntityViewsByTenantId(tenantId);
        dashboardService.deleteDashboardsByTenantId(tenantId);
        whiteLabelingService.saveSystemLoginWhiteLabelingParams(new LoginWhiteLabelingParams());
        whiteLabelingService.saveTenantWhiteLabelingParams(tenantId, new WhiteLabelingParams());
        customTranslationService.saveTenantCustomTranslation(tenantId, new CustomTranslation());

        ListenableFuture<List<EntityGroup>> entityGroupsFuture = entityGroupService.findAllEntityGroups(tenantId, tenantId);
        try {
            List<EntityGroup> entityGroups = entityGroupsFuture.get();
            entityGroups.stream()
                    .filter(e -> !e.getName().equals(EntityGroup.GROUP_ALL_NAME))
                    .filter(e -> !e.getName().equals(EntityGroup.GROUP_TENANT_USERS_NAME))
                    .forEach(entityGroup -> entityGroupService.deleteEntityGroup(tenantId, entityGroup.getId()));
        } catch (InterruptedException | ExecutionException e) {
            log.error("Unable to delete entity groups", e);
        }
    }

    private void setTenantId() {
        // TODO: voba - refactor for a single tenant approach
        TextPageData<Tenant> tenants = tenantService.findTenants(new TextPageLink(1));
        if (tenants.getData() != null && !tenants.getData().isEmpty()) {
            tenantId = tenants.getData().get(0).getId();
        }
        if (tenantId == null) {
            Tenant savedTenant = createTenant();
            tenantId = savedTenant.getTenantId();
        }
    }

    private Tenant createTenant() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Tenant");
        return tenantService.saveTenant(tenant);
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        edgeRpcClient.disconnect();
        if (executor != null) {
            executor.shutdownNow();
        }
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdownNow();
        }
    }

    private void processHandleMessages() {
        executor.submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    if (initialized) {
                        Long queueStartTs = getQueueStartTs().get();
                        TimePageLink pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(), queueStartTs, null, true);
                        TimePageData<CloudEvent> pageData;
                        UUID ifOffset = null;
                        boolean success = true;
                        do {
                            pageData = cloudNotificationService.findCloudEvents(tenantId, pageLink);
                            if (initialized && !pageData.getData().isEmpty()) {
                                log.trace("[{}] event(s) are going to be processed.", pageData.getData().size());
                                latch = new CountDownLatch(pageData.getData().size());
                                for (CloudEvent cloudEvent : pageData.getData()) {
                                    log.trace("Processing cloud event [{}]", cloudEvent);
                                    try {
                                        ActionType edgeEventAction = ActionType.valueOf(cloudEvent.getCloudEventAction());
                                        switch (edgeEventAction) {
                                            case UPDATED:
                                            case ADDED:
                                            case DELETED:
                                            case ALARM_ACK:
                                            case ALARM_CLEAR:
                                            case CREDENTIALS_UPDATED:
                                                processEntityMessage(cloudEvent, edgeEventAction);
                                                break;
                                            case ATTRIBUTES_UPDATED:
                                            case ATTRIBUTES_DELETED:
                                            case TIMESERIES_UPDATED:
                                                processTelemetryMessage(cloudEvent);
                                                break;
                                            case ATTRIBUTES_REQUEST:
                                                processAttributesRequest(cloudEvent);
                                                break;
                                            case RELATION_REQUEST:
                                                processRelationRequest(cloudEvent);
                                                break;
                                            case RULE_CHAIN_METADATA_REQUEST:
                                                processRuleChainMetadataRequest(cloudEvent);
                                                break;
                                            case CREDENTIALS_REQUEST:
                                                processCredentialsRequest(cloudEvent);
                                                break;
                                            case GROUP_ENTITIES_REQUEST:
                                                processGroupEntitiesRequest(cloudEvent);
                                                break;
                                        }
                                    } catch (Exception e) {
                                        log.error("Exception during processing records from queue", e);
                                    }
                                    ifOffset = cloudEvent.getUuidId();
                                }
                                success = latch.await(10, TimeUnit.SECONDS);
                                if (!success) {
                                    log.warn("Failed to deliver the batch: {}", pageData.getData());
                                }
                            }
                            if (initialized && (!success || pageData.hasNext())) {
                                try {
                                    Thread.sleep(cloudEventStorageSettings.getSleepIntervalBetweenBatches());
                                } catch (InterruptedException e) {
                                    log.error("Error during sleep between batches", e);
                                }
                                if (success) {
                                    pageLink = pageData.getNextPageLink();
                                }
                            }
                        } while (initialized && (!success || pageData.hasNext()));

                        if (ifOffset != null) {
                            Long newStartTs = UUIDs.unixTimestamp(ifOffset);
                            updateQueueStartTs(newStartTs);
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
        }, dbCallbackExecutor);
    }

    private void updateQueueStartTs(Long newStartTs) {
        newStartTs = ++newStartTs; // increments ts by 1 - next cloud event search starts from current offset + 1
        List<AttributeKvEntry> attributes = Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()));
        attributesService.save(tenantId, tenantId, DataConstants.SERVER_SCOPE, attributes);
    }

    private void processTelemetryMessage(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processTelemetryMessage, cloudEvent [{}]", cloudEvent);
        EntityId entityId = null;
        switch (cloudEvent.getCloudEventType()) {
            case DEVICE:
                entityId = new DeviceId(cloudEvent.getEntityId());
                break;
            case ASSET:
                entityId = new AssetId(cloudEvent.getEntityId());
                break;
            case ENTITY_VIEW:
                entityId = new EntityViewId(cloudEvent.getEntityId());
                break;
            case DASHBOARD:
                entityId = new DashboardId(cloudEvent.getEntityId());
                break;
        }
        if (entityId != null) {
            log.debug("Sending telemetry data msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody());
            try {
                ActionType actionType = ActionType.valueOf(cloudEvent.getCloudEventAction());
                JsonNode data = cloudEvent.getEntityBody().get("data");
                long ts = cloudEvent.getEntityBody().get("ts").asLong();
                UplinkMsg msg = constructEntityDataProtoMsg(entityId, actionType, JsonUtils.parse(mapper.writeValueAsString(data)), ts);
                edgeRpcClient.sendUplinkMsg(msg);
            } catch (Exception e) {
                log.warn("Can't send telemetry data msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            }
        }
    }

    private void processEntityMessage(CloudEvent cloudEvent, ActionType edgeEventAction) {
        UpdateMsgType msgType = getResponseMsgType(ActionType.valueOf(cloudEvent.getCloudEventAction()));
        log.trace("Executing processEntityMessage, cloudEvent [{}], edgeEventAction [{}], msgType [{}]", cloudEvent, edgeEventAction, msgType);
        switch (cloudEvent.getCloudEventType()) {
            case DEVICE:
                processDevice(cloudEvent, msgType, edgeEventAction);
                break;
//            case ASSET:
//                processAsset(cloudEvent, msgType, edgeEventAction);
//                break;
//            case ENTITY_VIEW:
//                processEntityView(cloudEvent, msgType, edgeEventAction);
//                break;
//            case DASHBOARD:
//                processDashboard(cloudEvent, msgType, edgeEventAction);
//                break;
//            case RULE_CHAIN:
//                processRuleChain(cloudEvent, msgType, edgeEventAction);
//                break;
//            case RULE_CHAIN_METADATA:
//                processRuleChainMetadata(cloudEvent, msgType);
//                break;
            case ALARM:
                processAlarm(cloudEvent, msgType);
                break;
//            case USER:
//                processUser(cloudEvent, msgType, edgeEventAction);
//                break;
//            case RELATION:
//                processRelation(cloudEvent, msgType);
//                break;
        }
    }

    private void processDevice(CloudEvent cloudEvent, UpdateMsgType msgType, ActionType edgeActionType) {
        try {
            DeviceId deviceId = new DeviceId(cloudEvent.getEntityId());
            UplinkMsg msg = null;
            switch (edgeActionType) {
                case ADDED:
                case UPDATED:
                    Device device = deviceService.findDeviceById(cloudEvent.getTenantId(), deviceId);
                    if (device != null) {
                        DeviceUpdateMsg deviceUpdateMsg =
                                deviceUpdateMsgConstructor.constructDeviceUpdatedMsg(msgType, device);
                        msg = UplinkMsg.newBuilder()
                                .addAllDeviceUpdateMsg(Collections.singletonList(deviceUpdateMsg)).build();
                    }
                    break;
                case DELETED:
                    DeviceUpdateMsg deviceUpdateMsg =
                            deviceUpdateMsgConstructor.constructDeviceDeleteMsg(deviceId);
                    msg = UplinkMsg.newBuilder()
                            .addAllDeviceUpdateMsg(Collections.singletonList(deviceUpdateMsg)).build();
                    break;
                case CREDENTIALS_UPDATED:
                    DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
                    if (deviceCredentials != null) {
                        DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg =
                                deviceUpdateMsgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                        msg = UplinkMsg.newBuilder()
                                .addAllDeviceCredentialsUpdateMsg(Collections.singletonList(deviceCredentialsUpdateMsg)).build();
                    }
                    break;
            }
            if (msg != null) {
                edgeRpcClient.sendUplinkMsg(msg);
            }
        } catch (Exception e) {
            log.error("Can't process device msg [{}] [{}]", cloudEvent, msgType, e);
        }
    }

    private void processAlarm(CloudEvent cloudEvent, UpdateMsgType msgType) {
        try {
            AlarmId alarmId = new AlarmId(cloudEvent.getEntityId());
            Alarm alarm = alarmService.findAlarmByIdAsync(cloudEvent.getTenantId(), alarmId).get();
            if (alarm != null) {
                AlarmUpdateMsg alarmUpdateMsg = alarmUpdateMsgConstructor.constructAlarmUpdatedMsg(tenantId, msgType, alarm);
                UplinkMsg msg = UplinkMsg.newBuilder()
                        .addAllAlarmUpdateMsg(Collections.singletonList(alarmUpdateMsg)).build();
                edgeRpcClient.sendUplinkMsg(msg);
            }
        } catch (Exception e) {
            log.error("Can't process alarm msg [{}] [{}]", cloudEvent, msgType, e);
        }
    }

    private void processAttributesRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processAttributesRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        log.debug("Sending attribute request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody());
        try {
            AttributesRequestMsg attributesRequestMsg = AttributesRequestMsg.newBuilder()
                    .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                    .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                    .setEntityType(entityId.getEntityType().name())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllAttributesRequestMsg(Collections.singletonList(attributesRequestMsg));
            edgeRpcClient.sendUplinkMsg(builder.build());
        } catch (Exception e) {
            log.warn("Can't send attribute request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
        }
    }

    private void processRelationRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processRelationRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        log.debug("Sending relation request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody());
        try {
            RelationRequestMsg relationRequestMsg = RelationRequestMsg.newBuilder()
                    .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                    .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                    .setEntityType(entityId.getEntityType().name())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllRelationRequestMsg(Collections.singletonList(relationRequestMsg));
            edgeRpcClient.sendUplinkMsg(builder.build());
        } catch (Exception e) {
            log.warn("Can't send relation request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
        }
    }

    private void processRuleChainMetadataRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processRuleChainMetadataRequest, cloudEvent [{}]", cloudEvent);
        EntityId ruleChainId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        log.debug("Sending rule chain metadata request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody());
        try {
            RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg = RuleChainMetadataRequestMsg.newBuilder()
                    .setRuleChainIdMSB(ruleChainId.getId().getMostSignificantBits())
                    .setRuleChainIdLSB(ruleChainId.getId().getLeastSignificantBits())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllRuleChainMetadataRequestMsg(Collections.singletonList(ruleChainMetadataRequestMsg));
            edgeRpcClient.sendUplinkMsg(builder.build());
        } catch (Exception e) {
            log.warn("Can't send rule chain metadata request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
        }
    }

    private void processCredentialsRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processCredentialsRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        log.debug("Sending credentials request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody());
        try {
            UplinkMsg msg = null;
            switch (entityId.getEntityType()) {
                case USER:
                    UserCredentialsRequestMsg userCredentialsRequestMsg = UserCredentialsRequestMsg.newBuilder()
                            .setUserIdMSB(entityId.getId().getMostSignificantBits())
                            .setUserIdLSB(entityId.getId().getLeastSignificantBits())
                            .build();
                    msg = UplinkMsg.newBuilder()
                            .addAllUserCredentialsRequestMsg(Collections.singletonList(userCredentialsRequestMsg))
                            .build();
                    break;
                case DEVICE:
                    DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                            .setDeviceIdMSB(entityId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(entityId.getId().getLeastSignificantBits())
                            .build();
                    msg = UplinkMsg.newBuilder()
                            .addAllDeviceCredentialsRequestMsg(Collections.singletonList(deviceCredentialsRequestMsg))
                            .build();
                    break;
            }
            if (msg != null) {
                edgeRpcClient.sendUplinkMsg(msg);
            }
        } catch (Exception e) {
            log.warn("Can't send credentials request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
        }
    }

    private void processGroupEntitiesRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processGroupEntitiesRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityGroupId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        String type = cloudEvent.getEntityBody().get("type").asText();
        log.debug("Sending group entities request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody());
        try {
            EntityGroupEntitiesRequestMsg entityGroupEntitiesRequestMsg = EntityGroupEntitiesRequestMsg.newBuilder()
                    .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                    .setType(type)
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllEntityGroupEntitiesRequestMsg(Collections.singletonList(entityGroupEntitiesRequestMsg));
            edgeRpcClient.sendUplinkMsg(builder.build());
        } catch (Exception e) {
            log.warn("Can't group entities credentials request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
        }
    }

    private UpdateMsgType getResponseMsgType(ActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        }
    }

    private UplinkMsg constructEntityDataProtoMsg(EntityId entityId, ActionType actionType, JsonElement entityData, long ts) {
        EntityDataProto entityDataProto = entityDataMsgConstructor.constructEntityDataMsg(entityId, actionType, entityData, ts);
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .addAllEntityData(Collections.singletonList(entityDataProto));
        return builder.build();
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        try {
            if (msg.getSuccess()) {
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
            initialized = true;
        } catch (Exception e) {
            log.error("Can't process edge configuration message [{}]", edgeConfiguration, e);
        }
    }

    private void onEntityUpdate(EntityUpdateMsg entityUpdateMsg) {
        try {
            if (entityUpdateMsg.hasDeviceUpdateMsg()) {
                log.debug("Device update message received [{}]", entityUpdateMsg.getDeviceUpdateMsg());
                deviceUpdateProcessor.onDeviceUpdate(tenantId, entityUpdateMsg.getDeviceUpdateMsg());
            } else if (entityUpdateMsg.hasDeviceCredentialsUpdateMsg()) {
                log.debug("Device credentials update message received [{}]", entityUpdateMsg.getDeviceCredentialsUpdateMsg());
                deviceUpdateProcessor.onDeviceCredentialsUpdate(tenantId, entityUpdateMsg.getDeviceCredentialsUpdateMsg());
            } else if (entityUpdateMsg.hasAssetUpdateMsg()) {
                log.debug("Asset update message received [{}]", entityUpdateMsg.getAssetUpdateMsg());
                assetUpdateProcessor.onAssetUpdate(tenantId, entityUpdateMsg.getAssetUpdateMsg());
            } else if (entityUpdateMsg.hasEntityViewUpdateMsg()) {
                log.debug("EntityView update message received [{}]", entityUpdateMsg.getEntityViewUpdateMsg());
                entityViewUpdateProcessor.onEntityViewUpdate(tenantId, entityUpdateMsg.getEntityViewUpdateMsg());
            } else if (entityUpdateMsg.hasRuleChainUpdateMsg()) {
                log.debug("Rule Chain udpate message received [{}]", entityUpdateMsg.getRuleChainUpdateMsg());
                ruleChainUpdateProcessor.onRuleChainUpdate(tenantId, entityUpdateMsg.getRuleChainUpdateMsg());
            } else if (entityUpdateMsg.hasRuleChainMetadataUpdateMsg()) {
                log.debug("Rule Chain Metadata udpate message received [{}]", entityUpdateMsg.getRuleChainMetadataUpdateMsg());
                ruleChainUpdateProcessor.onRuleChainMetadataUpdate(tenantId, entityUpdateMsg.getRuleChainMetadataUpdateMsg());
            } else if (entityUpdateMsg.hasDashboardUpdateMsg()) {
                log.debug("Dashboard message received [{}]", entityUpdateMsg.getDashboardUpdateMsg());
                dashboardUpdateProcessor.onDashboardUpdate(tenantId, entityUpdateMsg.getDashboardUpdateMsg());
            } else if (entityUpdateMsg.hasAlarmUpdateMsg()) {
                log.debug("Alarm message received [{}]", entityUpdateMsg.getAlarmUpdateMsg());
                alarmUpdateProcessor.onAlarmUpdate(tenantId, entityUpdateMsg.getAlarmUpdateMsg());
            } else if (entityUpdateMsg.hasCustomerUpdateMsg()) {
                log.debug("Customer message received [{}]", entityUpdateMsg.getCustomerUpdateMsg());
                customerUpdateProcessor.onCustomerUpdate(tenantId, entityUpdateMsg.getCustomerUpdateMsg());
            } else if (entityUpdateMsg.hasRelationUpdateMsg()) {
                log.debug("Relation update message received [{}]", entityUpdateMsg.getRelationUpdateMsg());
                relationUpdateProcessor.onRelationUpdate(tenantId, entityUpdateMsg.getRelationUpdateMsg());
            } else if (entityUpdateMsg.hasUserUpdateMsg()) {
                log.debug("User message received [{}]", entityUpdateMsg.getUserUpdateMsg());
                userUpdateProcessor.onUserUpdate(tenantId, entityUpdateMsg.getUserUpdateMsg());
            } else if (entityUpdateMsg.hasUserCredentialsUpdateMsg()) {
                log.debug("User credentials message received [{}]", entityUpdateMsg.getUserCredentialsUpdateMsg());
                userUpdateProcessor.onUserCredentialsUpdate(tenantId, entityUpdateMsg.getUserCredentialsUpdateMsg());
            } else if (entityUpdateMsg.hasEntityGroupUpdateMsg()) {
                log.debug("Entity group message received [{}]", entityUpdateMsg.getEntityGroupUpdateMsg());
                entityGroupUpdateProcessor.onEntityGroupUpdate(tenantId, entityUpdateMsg.getEntityGroupUpdateMsg());
            } else if (entityUpdateMsg.hasCustomTranslation()) {
                log.debug("Custom translation received [{}]", entityUpdateMsg.getCustomTranslation());
                whiteLabelingUpdateProcessor.onCustomTranslationUpdate(tenantId, entityUpdateMsg.getCustomTranslation());
            } else if (entityUpdateMsg.hasWhiteLabelingParams()) {
                log.debug("White labeling params received [{}]", entityUpdateMsg.getWhiteLabelingParams());
                whiteLabelingUpdateProcessor.onWhiteLabelingParamsUpdate(tenantId, entityUpdateMsg.getWhiteLabelingParams());
            } else if (entityUpdateMsg.hasLoginWhiteLabelingParams()) {
                log.debug("Login white labeling params received [{}]", entityUpdateMsg.getLoginWhiteLabelingParams());
                whiteLabelingUpdateProcessor.onLoginWhiteLabelingParamsUpdate(tenantId, entityUpdateMsg.getLoginWhiteLabelingParams());
            } else if (entityUpdateMsg.hasSchedulerEventUpdateMsg()) {
                log.debug("Schedule event received [{}]", entityUpdateMsg.getSchedulerEventUpdateMsg());
                schedulerEventUpdateProcessor.onScheduleEventUpdate(tenantId, entityUpdateMsg.getSchedulerEventUpdateMsg());
            }
        } catch (Exception e) {
            log.error("Can't process entity updated msg [{}]", entityUpdateMsg, e);
        }
    }

    private void onDownlink(DownlinkMsg downlinkMsg) {
        try {
            log.debug("onDownlink {}", downlinkMsg);
            if (downlinkMsg.getEntityDataList() != null && !downlinkMsg.getEntityDataList().isEmpty()) {
                for (EntityDataProto entityData : downlinkMsg.getEntityDataList()) {
                    EntityId entityId = constructEntityId(entityData);
                    if ((entityData.hasPostAttributesMsg() || entityData.hasPostTelemetryMsg()) && entityId != null) {
                        ListenableFuture<TbMsgMetaData> metaDataFuture = constructBaseMsgMetadata(entityId);
                        Futures.transform(metaDataFuture, metaData -> {
                            if (metaData != null) {
                                metaData.putValue(DataConstants.MSG_SOURCE_KEY, DataConstants.CLOUD_MSG_SOURCE);
                                if (entityData.hasPostAttributesMsg()) {
                                    processPostAttributes(entityId, entityData.getPostAttributesMsg(), metaData);
                                }
                                if (entityData.hasPostTelemetryMsg()) {
                                    processPostTelemetry(entityId, entityData.getPostTelemetryMsg(), metaData);
                                }
                            }
                            return null;
                        }, dbCallbackExecutor);
                    }
                }
            }
            if (downlinkMsg.getDeviceCredentialsRequestMsgList() != null && !downlinkMsg.getDeviceCredentialsRequestMsgList().isEmpty()) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                    processDeviceCredentialsRequestMsg(deviceCredentialsRequestMsg);
                }
            }
        } catch (Exception e) {
            log.error("Can't process downlink message [{}]", downlinkMsg, e);
        }
    }

    private void processDeviceCredentialsRequestMsg(DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            saveCloudEvent(tenantId, CloudEventType.DEVICE, ActionType.CREDENTIALS_UPDATED, deviceId, null);
        }
    }

    private void saveCloudEvent(TenantId tenantId,
                                CloudEventType cloudEventType,
                                ActionType cloudEventAction,
                                EntityId entityId,
                                JsonNode entityBody) {
        log.debug("Pushing cloud event to cloud queue. tenantId [{}], cloudEventType [{}], cloudEventAction[{}], entityId [{}], entityBody [{}]",
                tenantId, cloudEventType, cloudEventAction, entityId, entityBody);

        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setTenantId(tenantId);
        cloudEvent.setCloudEventType(cloudEventType);
        cloudEvent.setCloudEventAction(cloudEventAction.name());
        if (entityId != null) {
            cloudEvent.setEntityId(entityId.getId());
        }
        cloudEvent.setEntityBody(entityBody);
        cloudEventService.saveAsync(cloudEvent);
    }

    private ListenableFuture<TbMsgMetaData> constructBaseMsgMetadata(EntityId entityId) {
        switch (entityId.getEntityType()) {
            case DEVICE:
                ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(tenantId, new DeviceId(entityId.getId()));
                return Futures.transform(deviceFuture, device -> {
                    TbMsgMetaData metaData = new TbMsgMetaData();
                    if (device != null) {
                        metaData.putValue("deviceName", device.getName());
                        metaData.putValue("deviceType", device.getType());
                    }
                    return metaData;
                }, dbCallbackExecutor);
            case ASSET:
                ListenableFuture<Asset> assetFuture = assetService.findAssetByIdAsync(tenantId, new AssetId(entityId.getId()));
                return Futures.transform(assetFuture, asset -> {
                    TbMsgMetaData metaData = new TbMsgMetaData();
                    if (asset != null) {
                        metaData.putValue("assetName", asset.getName());
                        metaData.putValue("assetType", asset.getType());
                    }
                    return metaData;
                }, dbCallbackExecutor);
            case ENTITY_VIEW:
                ListenableFuture<EntityView> entityViewFuture = entityViewService.findEntityViewByIdAsync(tenantId, new EntityViewId(entityId.getId()));
                return Futures.transform(entityViewFuture, entityView -> {
                    TbMsgMetaData metaData = new TbMsgMetaData();
                    if (entityView != null) {
                        metaData.putValue("entityViewName", entityView.getName());
                        metaData.putValue("entityViewType", entityView.getType());
                    }
                    return metaData;
                }, dbCallbackExecutor);
            default:
                log.debug("Constructing empty metadata for entityId [{}]", entityId);
                return Futures.immediateFuture(new TbMsgMetaData());
        }
    }

    private EntityId constructEntityId(EntityDataProto entityData) {
        EntityType entityType = EntityType.valueOf(entityData.getEntityType());
        switch (entityType) {
            case DEVICE:
                return new DeviceId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ASSET:
                return new AssetId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ENTITY_VIEW:
                return new EntityViewId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case DASHBOARD:
                return new DashboardId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            default:
                log.warn("Unsupported entity type [{}] during construct of entity id. EntityDataProto [{}]", entityData.getEntityType(), entityData);
                return null;
        }
    }

    private void processPostTelemetry(EntityId entityId, TransportProtos.PostTelemetryMsg msg, TbMsgMetaData metaData) {
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
            metaData.putValue("ts", tsKv.getTs() + "");
            TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), entityId, metaData, gson.toJson(json));
            // TODO: voba - verify that null callback is OK
            tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, null);
        }
    }

    private void processPostAttributes(EntityId entityId, TransportProtos.PostAttributeMsg msg, TbMsgMetaData metaData) {
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_ATTRIBUTES_REQUEST.name(), entityId, metaData, gson.toJson(json));
        // TODO: voba - verify that null callback is OK
        tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, null);
    }

    private void scheduleReconnect(Exception e) {
        initialized = false;
        if (scheduledFuture == null) {
            scheduledFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
                log.info("Trying to reconnect due to the error: {}!", e.getMessage());
                edgeRpcClient.connect(routingKey, routingSecret,
                        this::onUplinkResponse,
                        this::onEdgeUpdate,
                        this::onEntityUpdate,
                        this::onDownlink,
                        this::scheduleReconnect);
            }, 0, reconnectTimeoutMs, TimeUnit.MILLISECONDS);
        }
    }
}
