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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.data.AdminSettings;
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
import org.thingsboard.server.common.data.edge.CloudType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
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
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.WidgetsBundleUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.cloud.constructor.AlarmUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.RelationUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.processor.AdminSettingsUpdateProcessor;
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
import org.thingsboard.server.service.cloud.processor.WidgetTypeUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.WidgetsBundleUpdateProcessor;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.service.user.UserLoaderService;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
    protected TelemetrySubscriptionService tsSubService;

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
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

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
    private WidgetsBundleUpdateProcessor widgetsBundleUpdateProcessor;

    @Autowired
    private WidgetTypeUpdateProcessor widgetTypeUpdateProcessor;

    @Autowired
    private AdminSettingsUpdateProcessor adminSettingsUpdateProcessor;

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private DeviceUpdateMsgConstructor deviceUpdateMsgConstructor;

    @Autowired
    private AlarmUpdateMsgConstructor alarmUpdateMsgConstructor;

    @Autowired
    private RelationUpdateMsgConstructor relationUpdateMsgConstructor;

    @Autowired
    private EntityDataMsgConstructor entityDataMsgConstructor;

    @Autowired
    private EdgeRpcClient edgeRpcClient;

    @Autowired
    private InstallScripts installScripts;

    private CountDownLatch latch;

    private final Lock sequenceDependencyLock = new ReentrantLock();

    private EdgeSettings edgeSettings;

    private ExecutorService executor;
    private ScheduledExecutorService reconnectScheduler;
    private ScheduledFuture<?> scheduledFuture;
    private volatile boolean initialized;

    private TenantId tenantId;
    private CustomerId customerId;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Starting Cloud Edge service");
        edgeRpcClient.connect(routingKey, routingSecret,
                this::onUplinkResponse,
                this::onEdgeUpdate,
                this::onDownlink,
                this::scheduleReconnect);
        executor = Executors.newSingleThreadExecutor();
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        processHandleMessages();
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
                                log.trace("[{}] event(s) are going to be converted.", pageData.getData().size());
                                List<UplinkMsg> uplinkMsgsPack = convertToUplinkMsgsPack(pageData.getData());
                                log.trace("[{}] uplink msg(s) are going to be send.", uplinkMsgsPack.size());

                                latch = new CountDownLatch(uplinkMsgsPack.size());
                                for (UplinkMsg uplinkMsg : uplinkMsgsPack) {
                                    edgeRpcClient.sendUplinkMsg(uplinkMsg);
                                }

                                ifOffset = pageData.getData().get(pageData.getData().size() - 1).getUuidId();
                                success = latch.await(10, TimeUnit.SECONDS);
                                if (!success) {
                                    log.warn("Failed to deliver the batch: {}", uplinkMsgsPack);
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

    private List<UplinkMsg> convertToUplinkMsgsPack(List<CloudEvent> cloudEvents) {
        List<UplinkMsg> result = new ArrayList<>();
        for (CloudEvent cloudEvent : cloudEvents) {
            log.trace("Processing cloud event [{}]", cloudEvent);
            try {
                UplinkMsg uplinkMsg = null;
                ActionType edgeEventAction = ActionType.valueOf(cloudEvent.getCloudEventAction());
                switch (edgeEventAction) {
                    case UPDATED:
                    case ADDED:
                    case DELETED:
                    case ALARM_ACK:
                    case ALARM_CLEAR:
                    case CREDENTIALS_UPDATED:
                    case RELATION_ADD_OR_UPDATE:
                    case RELATION_DELETED:
                        uplinkMsg = processEntityMessage(cloudEvent, edgeEventAction);
                        break;
                    case ATTRIBUTES_UPDATED:
                    case ATTRIBUTES_DELETED:
                    case TIMESERIES_UPDATED:
                        uplinkMsg = processTelemetryMessage(cloudEvent);
                        break;
                    case ATTRIBUTES_REQUEST:
                        uplinkMsg = processAttributesRequest(cloudEvent);
                        break;
                    case RELATION_REQUEST:
                        uplinkMsg = processRelationRequest(cloudEvent);
                        break;
                    case RULE_CHAIN_METADATA_REQUEST:
                        uplinkMsg = processRuleChainMetadataRequest(cloudEvent);
                        break;
                    case CREDENTIALS_REQUEST:
                        uplinkMsg = processCredentialsRequest(cloudEvent);
                        break;
                    case GROUP_ENTITIES_REQUEST:
                        uplinkMsg = processGroupEntitiesRequest(cloudEvent);
                        break;
                }
                if (uplinkMsg != null) {
                    result.add(uplinkMsg);
                }
            } catch (Exception e) {
                log.error("Exception during processing events from queue, skipping event [{}]", cloudEvent, e);
            }
        }
        return result;
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

    private void updateQueueStartTs(Long newStartTs) {
        newStartTs = ++newStartTs; // increments ts by 1 - next cloud event search starts from current offset + 1
        List<AttributeKvEntry> attributes = Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()));
        attributesService.save(tenantId, tenantId, DataConstants.SERVER_SCOPE, attributes);
    }

    private UplinkMsg processTelemetryMessage(CloudEvent cloudEvent) {
        try {
            log.trace("Executing processTelemetryMessage, cloudEvent [{}]", cloudEvent);
            EntityId entityId;
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
                case ENTITY_GROUP:
                    entityId = new EntityGroupId(cloudEvent.getEntityId());
                    break;
                default:
                    throw new IllegalAccessException("Unsupported cloud event type [" + cloudEvent.getCloudEventType() + "]");
            }

            ActionType actionType = ActionType.valueOf(cloudEvent.getCloudEventAction());
            return constructEntityDataProtoMsg(entityId, actionType, JsonUtils.parse(mapper.writeValueAsString(cloudEvent.getEntityBody())));
        } catch (Exception e) {
            log.warn("Can't convert telemetry data msg, cloudEvent [{}]", cloudEvent, e);
            return null;
        }
    }

    private UplinkMsg processEntityMessage(CloudEvent cloudEvent, ActionType edgeEventAction) {
        UpdateMsgType msgType = getResponseMsgType(ActionType.valueOf(cloudEvent.getCloudEventAction()));
        log.trace("Executing processEntityMessage, cloudEvent [{}], edgeEventAction [{}], msgType [{}]", cloudEvent, edgeEventAction, msgType);
        switch (cloudEvent.getCloudEventType()) {
            case DEVICE:
                return processDevice(cloudEvent, msgType, edgeEventAction);
            case ALARM:
                return processAlarm(cloudEvent, msgType);
            case RELATION:
                return processRelation(cloudEvent, msgType);
            default:
                log.warn("Unsupported cloud event type [{}]", cloudEvent);
                return null;
        }
    }

    private UplinkMsg processDevice(CloudEvent cloudEvent, UpdateMsgType msgType, ActionType edgeActionType) {
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
                    } else {
                        log.info("Skipping event as device was not found [{}]", cloudEvent);
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
                    } else {
                        log.info("Skipping event as device credentials was not found [{}]", cloudEvent);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported edge action type [" + edgeActionType + "]");
            }
            return msg;
        } catch (Exception e) {
            log.error("Can't process device msg [{}] [{}]", cloudEvent, msgType, e);
            return null;
        }
    }

    private UplinkMsg processAlarm(CloudEvent cloudEvent, UpdateMsgType msgType) {
        try {
            AlarmId alarmId = new AlarmId(cloudEvent.getEntityId());
            Alarm alarm = alarmService.findAlarmByIdAsync(cloudEvent.getTenantId(), alarmId).get();
            UplinkMsg msg = null;
            if (alarm != null) {
                AlarmUpdateMsg alarmUpdateMsg = alarmUpdateMsgConstructor.constructAlarmUpdatedMsg(tenantId, msgType, alarm);
                msg = UplinkMsg.newBuilder()
                        .addAllAlarmUpdateMsg(Collections.singletonList(alarmUpdateMsg)).build();
            } else {
                log.info("Skipping event as alarm was not found [{}]", cloudEvent);
            }
            return msg;
        } catch (Exception e) {
            log.error("Can't process alarm msg [{}] [{}]", cloudEvent, msgType, e);
            return null;
        }
    }

    private UplinkMsg processAttributesRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processAttributesRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            AttributesRequestMsg attributesRequestMsg = AttributesRequestMsg.newBuilder()
                    .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                    .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                    .setEntityType(entityId.getEntityType().name())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllAttributesRequestMsg(Collections.singletonList(attributesRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't send attribute request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    private UplinkMsg processRelation(CloudEvent cloudEvent, UpdateMsgType msgType) {
        UplinkMsg msg = null;
        try {
            EntityRelation entityRelation = mapper.convertValue(cloudEvent.getEntityBody(), EntityRelation.class);
            if (entityRelation != null) {
                RelationUpdateMsg relationUpdateMsg = relationUpdateMsgConstructor.constructRelationUpdatedMsg(msgType, entityRelation);
                msg = UplinkMsg.newBuilder()
                        .addAllRelationUpdateMsg(Collections.singletonList(relationUpdateMsg)).build();
            }
        } catch (Exception e) {
            log.error("Can't process relation msg [{}] [{}]", cloudEvent, msgType, e);
        }
        return msg;
    }

    private UplinkMsg processRelationRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processRelationRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            RelationRequestMsg relationRequestMsg = RelationRequestMsg.newBuilder()
                    .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                    .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                    .setEntityType(entityId.getEntityType().name())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllRelationRequestMsg(Collections.singletonList(relationRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't send relation request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    private UplinkMsg processRuleChainMetadataRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processRuleChainMetadataRequest, cloudEvent [{}]", cloudEvent);
        EntityId ruleChainId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        try {
            RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg = RuleChainMetadataRequestMsg.newBuilder()
                    .setRuleChainIdMSB(ruleChainId.getId().getMostSignificantBits())
                    .setRuleChainIdLSB(ruleChainId.getId().getLeastSignificantBits())
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllRuleChainMetadataRequestMsg(Collections.singletonList(ruleChainMetadataRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't send rule chain metadata request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    private UplinkMsg processCredentialsRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processCredentialsRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
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
                default:
                    log.info("Skipping event as entity type doesn't supported [{}]", cloudEvent);
            }
            return msg;
        } catch (Exception e) {
            log.warn("Can't send credentials request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    private UplinkMsg processGroupEntitiesRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processGroupEntitiesRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityGroupId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        String type = cloudEvent.getEntityBody().get("type").asText();
        try {
            EntityGroupRequestMsg entityGroupEntitiesRequestMsg = EntityGroupRequestMsg.newBuilder()
                    .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                    .setType(type)
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllEntityGroupEntitiesRequestMsg(Collections.singletonList(entityGroupEntitiesRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't group entities credentials request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    private UpdateMsgType getResponseMsgType(ActionType actionType) {
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

    private UplinkMsg constructEntityDataProtoMsg(EntityId entityId, ActionType actionType, JsonElement entityData) {
        EntityDataProto entityDataProto = entityDataMsgConstructor.constructEntityDataMsg(entityId, actionType, entityData);
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

            UUID tenantUUID = new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB());
            this.tenantId = getOrCreateTenant(new TenantId(tenantUUID)).getTenantId();

            this.edgeSettings = cloudEventService.findEdgeSettings(tenantId);
            EdgeSettings newEdgeSetting = constructEdgeSettings(edgeConfiguration);
            if (this.edgeSettings == null || !this.edgeSettings.getEdgeId().equals(newEdgeSetting.getEdgeId())) {
                cleanUp();
                this.edgeSettings = newEdgeSetting;
            }

            cloudEventService.saveEdgeSettings(tenantId, newEdgeSetting);
            save(DefaultDeviceStateService.ACTIVITY_STATE, true);
            save(DefaultDeviceStateService.LAST_CONNECT_TIME, System.currentTimeMillis());

            AdminSettings existingMailTemplates = adminSettingsService.findAdminSettingsByKey(tenantId, "mailTemplates");
            if (newEdgeSetting.getCloudType().equals(CloudType.CE) && existingMailTemplates == null) {
                installScripts.loadMailTemplates();
            }

            initialized = true;
        } catch (Exception e) {
            log.error("Can't process edge configuration message [{}]", edgeConfiguration, e);
        }
    }

    private void cleanUp() {
        log.debug("Starting clean up procedure");
        userService.deleteTenantAdmins(tenantId);
        TextPageData<Customer> customers = customerService.findCustomersByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
        if (customers != null && customers.getData() != null && !customers.getData().isEmpty()) {
            for (Customer customer : customers.getData()) {
                userService.deleteCustomerUsers(tenantId, customer.getId());
            }
        }
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
        entityViewService.deleteEntityViewsByTenantId(tenantId);
        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
        dashboardService.deleteDashboardsByTenantId(tenantId);
        adminSettingsService.deleteAdminSettingsByKey(tenantId, "mailTemplates");
        adminSettingsService.deleteAdminSettingsByKey(tenantId, "mail");
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);
        widgetsBundleService.deleteWidgetsBundlesByTenantId(TenantId.SYS_TENANT_ID);
        whiteLabelingService.saveSystemLoginWhiteLabelingParams(new LoginWhiteLabelingParams());
        whiteLabelingService.saveTenantWhiteLabelingParams(tenantId, new WhiteLabelingParams());
        customTranslationService.saveTenantCustomTranslation(tenantId, new CustomTranslation());

        try {
            List<AttributeKvEntry> attributeKvEntries = attributesService.findAll(tenantId, tenantId, DataConstants.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            attributesService.removeAll(tenantId, tenantId, DataConstants.SERVER_SCOPE, attrKeys);
            ListenableFuture<List<EntityGroup>> entityGroupsFuture = entityGroupService.findAllEntityGroups(tenantId, tenantId);
            List<EntityGroup> entityGroups = entityGroupsFuture.get();
            entityGroups.stream()
                    .filter(e -> !e.getName().equals(EntityGroup.GROUP_ALL_NAME))
                    .filter(e -> !e.getName().equals(EntityGroup.GROUP_EDGE_CE_TENANT_ADMINS_NAME))
                    .filter(e -> !e.getName().equals(EntityGroup.GROUP_EDGE_CE_CUSTOMER_USERS_NAME))
                    .forEach(entityGroup -> entityGroupService.deleteEntityGroup(tenantId, entityGroup.getId()));
        } catch (InterruptedException | ExecutionException e) {
            log.error("Unable to delete entity groups", e);
        }
        log.debug("Clean up procedure successfully finished!");
    }

    private Tenant getOrCreateTenant(TenantId tenantId) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (tenant == null) {
            tenant = new Tenant();
            tenant.setTitle("Tenant");
            tenant.setId(tenantId);
            tenantService.saveTenant(tenant, true);
        }
        return tenant;
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
        edgeSettings.setCloudType(CloudType.valueOf(edgeConfiguration.getCloudType()));

        return edgeSettings;
    }

    private void onDownlink(DownlinkMsg downlinkMsg) {
        ListenableFuture<List<Void>> future = processDownlinkMsg(downlinkMsg);
        Futures.addCallback(future, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder().setSuccess(true).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }

            @Override
            public void onFailure(Throwable t) {
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder().setSuccess(false).setErrorMsg(t.getMessage()).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<List<Void>> processDownlinkMsg(DownlinkMsg downlinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            log.debug("onDownlink {}", downlinkMsg);
            if (downlinkMsg.getEntityDataList() != null && !downlinkMsg.getEntityDataList().isEmpty()) {
                for (EntityDataProto entityData : downlinkMsg.getEntityDataList()) {
                    EntityId entityId = constructEntityId(entityData);
                    if ((entityData.hasPostAttributesMsg() || entityData.hasPostTelemetryMsg()) && entityId != null) {
                        TbMsgMetaData metaData = constructBaseMsgMetadata(entityId);
                        metaData.putValue(DataConstants.MSG_SOURCE_KEY, DataConstants.CLOUD_MSG_SOURCE);
                        if (entityData.hasPostAttributesMsg()) {
                            metaData.putValue("scope", entityData.getPostAttributeScope());
                            result.add(processPostAttributes(entityId, entityData.getPostAttributesMsg(), metaData));
                        }
                        if (entityData.hasPostTelemetryMsg()) {
                            result.add(processPostTelemetry(entityId, entityData.getPostTelemetryMsg(), metaData));
                        }
                    }
                    if (entityData.hasAttributeDeleteMsg()) {
                        result.add(processAttributeDeleteMsg(entityId, entityData.getAttributeDeleteMsg(), entityData.getEntityType()));
                    }
                }
            }
            if (downlinkMsg.getDeviceCredentialsRequestMsgList() != null && !downlinkMsg.getDeviceCredentialsRequestMsgList().isEmpty()) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(processDeviceCredentialsRequestMsg(deviceCredentialsRequestMsg));
                }
            }
            if (downlinkMsg.getDeviceUpdateMsgList() != null && !downlinkMsg.getDeviceUpdateMsgList().isEmpty()) {
                for (DeviceUpdateMsg deviceUpdateMsg : downlinkMsg.getDeviceUpdateMsgList()) {
                    result.add(deviceUpdateProcessor.onDeviceUpdate(tenantId, customerId, deviceUpdateMsg, edgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getDeviceCredentialsUpdateMsgList() != null && !downlinkMsg.getDeviceCredentialsUpdateMsgList().isEmpty()) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(deviceUpdateProcessor.onDeviceCredentialsUpdate(tenantId, deviceCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getAssetUpdateMsgList() != null && !downlinkMsg.getAssetUpdateMsgList().isEmpty()) {
                for (AssetUpdateMsg assetUpdateMsg : downlinkMsg.getAssetUpdateMsgList()) {
                    result.add(assetUpdateProcessor.onAssetUpdate(tenantId, customerId, assetUpdateMsg, edgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getEntityViewUpdateMsgList() != null && !downlinkMsg.getEntityViewUpdateMsgList().isEmpty()) {
                for (EntityViewUpdateMsg entityViewUpdateMsg : downlinkMsg.getEntityViewUpdateMsgList()) {
                    result.add(entityViewUpdateProcessor.onEntityViewUpdate(tenantId, customerId, entityViewUpdateMsg, edgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getRuleChainUpdateMsgList() != null && !downlinkMsg.getRuleChainUpdateMsgList().isEmpty()) {
                for (RuleChainUpdateMsg ruleChainUpdateMsg : downlinkMsg.getRuleChainUpdateMsgList()) {
                    result.add(ruleChainUpdateProcessor.onRuleChainUpdate(tenantId, ruleChainUpdateMsg));
                }
            }
            if (downlinkMsg.getRuleChainMetadataUpdateMsgList() != null && !downlinkMsg.getRuleChainMetadataUpdateMsgList().isEmpty()) {
                for (RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg : downlinkMsg.getRuleChainMetadataUpdateMsgList()) {
                    result.add(ruleChainUpdateProcessor.onRuleChainMetadataUpdate(tenantId, ruleChainMetadataUpdateMsg));
                }
            }
            if (downlinkMsg.getDashboardUpdateMsgList() != null && !downlinkMsg.getDashboardUpdateMsgList().isEmpty()) {
                for (DashboardUpdateMsg dashboardUpdateMsg : downlinkMsg.getDashboardUpdateMsgList()) {
                    result.add(dashboardUpdateProcessor.onDashboardUpdate(tenantId, customerId, dashboardUpdateMsg, edgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getAlarmUpdateMsgList() != null && !downlinkMsg.getAlarmUpdateMsgList().isEmpty()) {
                for (AlarmUpdateMsg alarmUpdateMsg : downlinkMsg.getAlarmUpdateMsgList()) {
                    result.add(alarmUpdateProcessor.onAlarmUpdate(tenantId, alarmUpdateMsg));
                }
            }
            if (downlinkMsg.getCustomerUpdateMsgList() != null && !downlinkMsg.getCustomerUpdateMsgList().isEmpty()) {
                for (CustomerUpdateMsg customerUpdateMsg : downlinkMsg.getCustomerUpdateMsgList()) {
                    try {
                        sequenceDependencyLock.lock();
                        result.add(customerUpdateProcessor.onCustomerUpdate(tenantId, customerUpdateMsg, edgeSettings.getCloudType()));
                        updateCustomerId(customerUpdateMsg);
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getRelationUpdateMsgList() != null && !downlinkMsg.getRelationUpdateMsgList().isEmpty()) {
                for (RelationUpdateMsg relationUpdateMsg : downlinkMsg.getRelationUpdateMsgList()) {
                    result.add(relationUpdateProcessor.onRelationUpdate(tenantId, relationUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetsBundleUpdateMsgList() != null && !downlinkMsg.getWidgetsBundleUpdateMsgList().isEmpty()) {
                for (WidgetsBundleUpdateMsg widgetsBundleUpdateMsg : downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                    result.add(widgetsBundleUpdateProcessor.onWidgetsBundleUpdate(tenantId, widgetsBundleUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetTypeUpdateMsgList() != null && !downlinkMsg.getWidgetTypeUpdateMsgList().isEmpty()) {
                for (WidgetTypeUpdateMsg widgetTypeUpdateMsg : downlinkMsg.getWidgetTypeUpdateMsgList()) {
                    result.add(widgetTypeUpdateProcessor.onWidgetTypeUpdate(tenantId, widgetTypeUpdateMsg));
                }
            }
            if (downlinkMsg.getUserUpdateMsgList() != null && !downlinkMsg.getUserUpdateMsgList().isEmpty()) {
                for (UserUpdateMsg userUpdateMsg : downlinkMsg.getUserUpdateMsgList()) {
                    try {
                        sequenceDependencyLock.lock();
                        result.add(userUpdateProcessor.onUserUpdate(tenantId, userUpdateMsg, this.edgeSettings.getCloudType()));
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getUserCredentialsUpdateMsgList() != null && !downlinkMsg.getUserCredentialsUpdateMsgList().isEmpty()) {
                for (UserCredentialsUpdateMsg userCredentialsUpdateMsg : downlinkMsg.getUserCredentialsUpdateMsgList()) {
                    result.add(userUpdateProcessor.onUserCredentialsUpdate(tenantId, userCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getEntityGroupUpdateMsgList() != null && !downlinkMsg.getEntityGroupUpdateMsgList().isEmpty()) {
                for (EntityGroupUpdateMsg entityGroupUpdateMsg : downlinkMsg.getEntityGroupUpdateMsgList()) {
                    result.add(entityGroupUpdateProcessor.onEntityGroupUpdate(tenantId, entityGroupUpdateMsg));
                }
            }
            if (downlinkMsg.getCustomTranslationMsgList() != null && !downlinkMsg.getCustomTranslationMsgList().isEmpty()) {
                for (CustomTranslationProto customTranslationProto : downlinkMsg.getCustomTranslationMsgList()) {
                    result.add(whiteLabelingUpdateProcessor.onCustomTranslationUpdate(tenantId, customTranslationProto));
                }
            }
            if (downlinkMsg.getWhiteLabelingParamsList() != null && !downlinkMsg.getWhiteLabelingParamsList().isEmpty()) {
                for (WhiteLabelingParamsProto whiteLabelingParamsProto : downlinkMsg.getWhiteLabelingParamsList()) {
                    result.add(whiteLabelingUpdateProcessor.onWhiteLabelingParamsUpdate(tenantId, whiteLabelingParamsProto));
                }
            }
            if (downlinkMsg.getLoginWhiteLabelingParamsList() != null && !downlinkMsg.getLoginWhiteLabelingParamsList().isEmpty()) {
                for (LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto : downlinkMsg.getLoginWhiteLabelingParamsList()) {
                    result.add(whiteLabelingUpdateProcessor.onLoginWhiteLabelingParamsUpdate(tenantId, loginWhiteLabelingParamsProto));
                }
            }
            if (downlinkMsg.getSchedulerEventUpdateMsgList() != null && !downlinkMsg.getSchedulerEventUpdateMsgList().isEmpty()) {
                for (SchedulerEventUpdateMsg schedulerEventUpdateMsg : downlinkMsg.getSchedulerEventUpdateMsgList()) {
                    result.add(schedulerEventUpdateProcessor.onScheduleEventUpdate(tenantId, schedulerEventUpdateMsg));
                }
            }
            if (downlinkMsg.getAdminSettingsUpdateMsgList() != null && !downlinkMsg.getAdminSettingsUpdateMsgList().isEmpty()) {
                for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : downlinkMsg.getAdminSettingsUpdateMsgList()) {
                    result.add(adminSettingsUpdateProcessor.onAdminSettingsUpdate(tenantId, adminSettingsUpdateMsg));
                }
            }
        } catch (Exception e) {
            log.error("Can't process downlink message [{}]", downlinkMsg, e);
        }
        return Futures.allAsList(result);
    }

    private void updateCustomerId(CustomerUpdateMsg customerUpdateMsg) {
        switch (customerUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                customerId = null;
                break;
        }
    }

    private ListenableFuture<Void> processDeviceCredentialsRequestMsg(DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            ListenableFuture<CloudEvent> future = saveCloudEvent(tenantId, CloudEventType.DEVICE, ActionType.CREDENTIALS_UPDATED, deviceId, null);
            return Futures.transform(future, cloudEvent -> null, dbCallbackExecutorService);
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<CloudEvent> saveCloudEvent(TenantId tenantId,
                                                        CloudEventType cloudEventType,
                                                        ActionType cloudEventAction,
                                                        EntityId entityId,
                                                        JsonNode entityBody) {
        log.debug("Pushing event to cloud queue. tenantId [{}], cloudEventType [{}], cloudEventAction[{}], entityId [{}], entityBody [{}]",
                tenantId, cloudEventType, cloudEventAction, entityId, entityBody);

        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setTenantId(tenantId);
        cloudEvent.setCloudEventType(cloudEventType);
        cloudEvent.setCloudEventAction(cloudEventAction.name());
        if (entityId != null) {
            cloudEvent.setEntityId(entityId.getId());
        }
        cloudEvent.setEntityBody(entityBody);
        return cloudEventService.saveAsync(cloudEvent);
    }

    private TbMsgMetaData constructBaseMsgMetadata(EntityId entityId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        switch (entityId.getEntityType()) {
            case DEVICE:
                Device device = deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
                if (device != null) {
                    metaData.putValue("deviceName", device.getName());
                    metaData.putValue("deviceType", device.getType());
                }
                break;
            case ASSET:
                Asset asset = assetService.findAssetById(tenantId, new AssetId(entityId.getId()));
                if (asset != null) {
                    metaData.putValue("assetName", asset.getName());
                    metaData.putValue("assetType", asset.getType());
                }
                break;
            case ENTITY_VIEW:
                EntityView entityView = entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId()));
                if (entityView != null) {
                    metaData.putValue("entityViewName", entityView.getName());
                    metaData.putValue("entityViewType", entityView.getType());
                }
                break;
            case ENTITY_GROUP:
                EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, new EntityGroupId(entityId.getId()));
                if (entityGroup != null) {
                    metaData.putValue("entityGroupName", entityGroup.getName());
                    metaData.putValue("entityGroupType", entityGroup.getType().name());
                }
                break;
            default:
                log.debug("Using empty metadata for entityId [{}]", entityId);
                break;
        }
        return metaData;
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
            case TENANT:
                return new TenantId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case CUSTOMER:
                return new CustomerId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ENTITY_GROUP:
                return new EntityGroupId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            default:
                log.warn("Unsupported entity type [{}] during construct of entity id. EntityDataProto [{}]", entityData.getEntityType(), entityData);
                return null;
        }
    }

    private ListenableFuture<Void> processPostTelemetry(EntityId entityId, TransportProtos.PostTelemetryMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
            metaData.putValue("ts", tsKv.getTs() + "");
            TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), entityId, metaData, gson.toJson(json));
            tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't process post telemetry [{}]", msg, t);
                    futureToSet.setException(t);
                }
            });
        }
        return futureToSet;
    }

    private ListenableFuture<Void> processPostAttributes(EntityId entityId, TransportProtos.PostAttributeMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_ATTRIBUTES_REQUEST.name(), entityId, metaData, gson.toJson(json));
        tbClusterService.pushMsgToRuleEngine(tenantId, tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Can't process post attributes [{}]", msg, t);
                futureToSet.setException(t);
            }
        });
        return futureToSet;
    }

    private ListenableFuture<Void> processAttributeDeleteMsg(EntityId entityId, AttributeDeleteMsg attributeDeleteMsg, String entityType) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        String scope = attributeDeleteMsg.getScope();
        List<String> attributeNames = attributeDeleteMsg.getAttributeNamesList();
        attributesService.removeAll(tenantId, entityId, scope, attributeNames);
        if (EntityType.DEVICE.name().equals(entityType)) {
            Set<AttributeKey> attributeKeys = new HashSet<>();
            for (String attributeName : attributeNames) {
                attributeKeys.add(new AttributeKey(scope, attributeName));
            }
            tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(
                    tenantId, (DeviceId) entityId, attributeKeys), new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't process attribute delete msg [{}]", attributeDeleteMsg, t);
                    futureToSet.setException(t);
                }
            });
        }
        return futureToSet;
    }

    private void scheduleReconnect(Exception e) {
        initialized = false;
        if (tenantId != null) {
            save(DefaultDeviceStateService.ACTIVITY_STATE, false);
            save(DefaultDeviceStateService.LAST_DISCONNECT_TIME, System.currentTimeMillis());
        }
        if (scheduledFuture == null) {
            scheduledFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
                log.info("Trying to reconnect due to the error: {}!", e.getMessage());
                edgeRpcClient.connect(routingKey, routingSecret,
                        this::onUplinkResponse,
                        this::onEdgeUpdate,
                        this::onDownlink,
                        this::scheduleReconnect);
            }, 0, reconnectTimeoutMs, TimeUnit.MILLISECONDS);
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
