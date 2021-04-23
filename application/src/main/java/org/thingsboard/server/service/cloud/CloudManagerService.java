/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
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
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.GroupPermissionProto;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RoleProto;
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
import org.thingsboard.server.service.cloud.constructor.AlarmUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.cloud.constructor.RelationUpdateMsgConstructor;
import org.thingsboard.server.service.cloud.processor.AdminSettingsProcessor;
import org.thingsboard.server.service.cloud.processor.AlarmProcessor;
import org.thingsboard.server.service.cloud.processor.AssetProcessor;
import org.thingsboard.server.service.cloud.processor.CustomerProcessor;
import org.thingsboard.server.service.cloud.processor.DashboardProcessor;
import org.thingsboard.server.service.cloud.processor.DeviceProcessor;
import org.thingsboard.server.service.cloud.processor.DeviceProfileProcessor;
import org.thingsboard.server.service.cloud.processor.EntityGroupProcessor;
import org.thingsboard.server.service.cloud.processor.EntityViewProcessor;
import org.thingsboard.server.service.cloud.processor.GroupPermissionProcessor;
import org.thingsboard.server.service.cloud.processor.RelationProcessor;
import org.thingsboard.server.service.cloud.processor.RoleProcessor;
import org.thingsboard.server.service.cloud.processor.RuleChainProcessor;
import org.thingsboard.server.service.cloud.processor.SchedulerEventProcessor;
import org.thingsboard.server.service.cloud.processor.TelemetryProcessor;
import org.thingsboard.server.service.cloud.processor.UserProcessor;
import org.thingsboard.server.service.cloud.processor.WhiteLabelingProcessor;
import org.thingsboard.server.service.cloud.processor.WidgetTypeProcessor;
import org.thingsboard.server.service.cloud.processor.WidgetsBundleProcessor;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CloudManagerService {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";

    @Value("${cloud.routingKey}")
    private String routingKey;

    @Value("${cloud.secret}")
    private String routingSecret;

    @Value("${cloud.reconnect_timeout}")
    private long reconnectTimeoutMs;

    @Autowired
    private CloudEventService cloudEventService;

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
    private RoleService roleService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    private RuleChainProcessor ruleChainProcessor;

    @Autowired
    private TelemetryProcessor telemetryProcessor;

    @Autowired
    private DeviceProcessor deviceProcessor;

    @Autowired
    private DeviceProfileProcessor deviceProfileProcessor;

    @Autowired
    private AssetProcessor assetProcessor;

    @Autowired
    private EntityViewProcessor entityViewProcessor;

    @Autowired
    private RelationProcessor relationProcessor;

    @Autowired
    private DashboardProcessor dashboardProcessor;

    @Autowired
    private CustomerProcessor customerProcessor;

    @Autowired
    private AlarmProcessor alarmProcessor;

    @Autowired
    private UserProcessor userProcessor;

    @Autowired
    private EntityGroupProcessor entityGroupProcessor;

    @Autowired
    private SchedulerEventProcessor schedulerEventProcessor;

    @Autowired
    private RoleProcessor roleProcessor;

    @Autowired
    private GroupPermissionProcessor groupPermissionProcessor;

    @Autowired
    private WhiteLabelingProcessor whiteLabelingProcessor;

    @Autowired
    private WidgetsBundleProcessor widgetsBundleProcessor;

    @Autowired
    private WidgetTypeProcessor widgetTypeProcessor;

    @Autowired
    private AdminSettingsProcessor adminSettingsProcessor;

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
        validateRoutingKeyAndSecret();

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

    private void validateRoutingKeyAndSecret() {
        if (StringUtils.isBlank(routingKey) || StringUtils.isBlank(routingSecret)) {
            new Thread(() -> {
                log.error("Routing Key and Routing Secret must be provided and can't be blank. Please configure Routing Key and Routing Secret in the tb-edge.yml file or add CLOUD_ROUTING_KEY and CLOUD_ROUTING_SECRET export to the tb-edge.conf file. Stopping ThingsBoard Edge application...");
                System.exit(-1);
            }, "Shutdown Thread").start();
        }
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        String edgeId = edgeSettings != null ? edgeSettings.getEdgeId() : "";
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
                        Long queueStartTs = getQueueStartTs().get();
                        TimePageLink pageLink =
                                new TimePageLink(new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount()),
                                        queueStartTs,
                                        System.currentTimeMillis());
                        PageData<CloudEvent> pageData;
                        UUID ifOffset = null;
                        boolean success = true;
                        do {
                            pageData = cloudEventService.findCloudEvents(tenantId, pageLink);
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
                                    pageLink = pageLink.nextPageLink();
                                }
                            }
                        } while (initialized && (!success || pageData.hasNext()));

                        if (ifOffset != null) {
                            Long newStartTs = Uuids.unixTimestamp(ifOffset);
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
                    case GROUP_PERMISSIONS_REQUEST:
                        uplinkMsg = processEntityGroupPermissionsRequest(cloudEvent);
                        break;
                    case RPC_CALL:
                        uplinkMsg = processRpcCallResponse(cloudEvent);
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
            ArrayList<AttributesRequestMsg> allAttributesRequestMsg = new ArrayList<>();
            AttributesRequestMsg serverAttributesRequestMsg = AttributesRequestMsg.newBuilder()
                    .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                    .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                    .setEntityType(entityId.getEntityType().name())
                    .setScope(DataConstants.SERVER_SCOPE)
                    .build();
            allAttributesRequestMsg.add(serverAttributesRequestMsg);
            if (EntityType.DEVICE.equals(entityId.getEntityType())) {
                AttributesRequestMsg sharedAttributesRequestMsg = AttributesRequestMsg.newBuilder()
                        .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                        .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                        .setEntityType(entityId.getEntityType().name())
                        .setScope(DataConstants.SHARED_SCOPE)
                        .build();
                allAttributesRequestMsg.add(sharedAttributesRequestMsg);
            }
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllAttributesRequestMsg(allAttributesRequestMsg);
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't send attribute request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

    private UplinkMsg processRelation(CloudEvent cloudEvent, UpdateMsgType msgType) {
        log.trace("Executing processRelation, cloudEvent [{}]", cloudEvent);
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

    private UplinkMsg processRpcCallResponse(CloudEvent cloudEvent) {
        log.trace("Executing processRpcCallResponse, cloudEvent [{}]", cloudEvent);
        UplinkMsg msg = null;
        try {
            DeviceId deviceId = new DeviceId(cloudEvent.getEntityId());
            DeviceRpcCallMsg rpcResponseMsg = deviceUpdateMsgConstructor.constructDeviceRpcResponseMsg(deviceId, cloudEvent.getEntityBody());
            msg = UplinkMsg.newBuilder()
                    .addAllDeviceRpcCallMsg(Collections.singletonList(rpcResponseMsg)).build();
        } catch (Exception e) {
            log.error("Can't process RPC response msg [{}]", cloudEvent, e);
        }
        return msg;
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

    private UplinkMsg processEntityGroupPermissionsRequest(CloudEvent cloudEvent) throws IOException {
        log.trace("Executing processEntityGroupPermissionsRequest, cloudEvent [{}]", cloudEvent);
        EntityId entityGroupId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        String type = cloudEvent.getEntityBody().get("type").asText();
        try {
            EntityGroupRequestMsg entityGroupPermissionsRequestMsg = EntityGroupRequestMsg.newBuilder()
                    .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                    .setType(type)
                    .build();
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .addAllEntityGroupPermissionsRequestMsg(Collections.singletonList(entityGroupPermissionsRequestMsg));
            return builder.build();
        } catch (Exception e) {
            log.warn("Failed process group permissions request, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
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
            this.tenantId = getOrCreateTenant(new TenantId(tenantUUID), CloudType.valueOf(edgeConfiguration.getCloudType())).getTenantId();

            this.edgeSettings = cloudEventService.findEdgeSettings(tenantId);
            EdgeSettings newEdgeSetting = constructEdgeSettings(edgeConfiguration);
            if (this.edgeSettings == null || !this.edgeSettings.getEdgeId().equals(newEdgeSetting.getEdgeId())) {
                cleanUp();
                this.edgeSettings = newEdgeSetting;
                // TODO: voba - should sync be executed in some other cases ???
                edgeRpcClient.sendSyncRequestMsg();
            }

            cloudEventService.saveEdgeSettings(tenantId, newEdgeSetting);

            // TODO: voba - verify storage of edge entity
            saveEdge(edgeConfiguration);

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
        edge.setEdgeLicenseKey(edgeConfiguration.getEdgeLicenseKey());
        edge.setCloudEndpoint(edgeConfiguration.getCloudEndpoint());
        edge.setAdditionalInfo(JacksonUtil.toJsonNode(edgeConfiguration.getAdditionalInfo()));
        edgeService.saveEdge(edge);
        saveCloudEvent(tenantId, CloudEventType.EDGE, ActionType.ATTRIBUTES_REQUEST, edgeId, null);
        saveCloudEvent(tenantId, CloudEventType.EDGE, ActionType.RELATION_REQUEST, edgeId, null);
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
        adminSettingsService.deleteAdminSettingsByKey(tenant.getId(), "mailTemplates");
        adminSettingsService.deleteAdminSettingsByKey(tenant.getId(), "mail");
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenant.getId());
        whiteLabelingService.saveSystemLoginWhiteLabelingParams(new LoginWhiteLabelingParams());
        whiteLabelingService.saveTenantWhiteLabelingParams(tenant.getId(), new WhiteLabelingParams());
        customTranslationService.saveTenantCustomTranslation(tenant.getId(), new CustomTranslation());
        roleService.deleteRolesByTenantId(tenant.getId());
        groupPermissionService.deleteGroupPermissionsByTenantId(tenant.getId());
        cloudEventService.deleteCloudEventsByTenantId(tenant.getId());
        try {
            List<AttributeKvEntry> attributeKvEntries = attributesService.findAll(tenant.getId(), tenant.getId(), DataConstants.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            attributesService.removeAll(tenant.getId(), tenant.getId(), DataConstants.SERVER_SCOPE, attrKeys);
            ListenableFuture<List<EntityGroup>> entityGroupsFuture = entityGroupService.findAllEntityGroups(tenant.getId(), tenant.getId());
            List<EntityGroup> entityGroups = entityGroupsFuture.get();
            entityGroups.stream()
                    .filter(e -> !e.getName().equals(EntityGroup.GROUP_ALL_NAME))
                    .filter(e -> !e.getName().equals(EntityGroup.GROUP_EDGE_CE_TENANT_ADMINS_NAME))
                    .filter(e -> !e.getName().equals(EntityGroup.GROUP_EDGE_CE_CUSTOMER_USERS_NAME))
                    .forEach(entityGroup -> entityGroupService.deleteEntityGroup(tenant.getId(), entityGroup.getId()));
        } catch (InterruptedException | ExecutionException e) {
            log.error("Unable to delete entity groups", e);
        }
    }

    private Tenant getOrCreateTenant(TenantId tenantId, CloudType cloudType) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (tenant == null) {
            tenant = new Tenant();
            tenant.setTitle("Tenant");
            tenant.setId(tenantId);
            Tenant savedTenant = tenantService.saveTenant(tenant, true);
            if (CloudType.CE.equals(cloudType)) {
                entityGroupService.findOrCreateTenantUsersGroup(savedTenant.getId());
                entityGroupService.findOrCreateTenantAdminsGroup(savedTenant.getId());
            }
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
            if (downlinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : downlinkMsg.getEntityDataList()) {
                    result.addAll(telemetryProcessor.onTelemetryUpdate(tenantId, entityData));
                }
            }
            if (downlinkMsg.getDeviceRpcCallMsgCount() > 0) {
                for (DeviceRpcCallMsg deviceRpcRequestMsg : downlinkMsg.getDeviceRpcCallMsgList()) {
                    result.add(deviceProcessor.onDeviceRpcRequest(tenantId, deviceRpcRequestMsg));
                }
            }
            if (downlinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(processDeviceCredentialsRequestMsg(deviceCredentialsRequestMsg));
                }
            }
            if (downlinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : downlinkMsg.getDeviceUpdateMsgList()) {
                    result.add(deviceProcessor.onDeviceUpdate(tenantId, customerId, deviceUpdateMsg, edgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
                for (DeviceProfileUpdateMsg deviceProfileUpdateMsg : downlinkMsg.getDeviceProfileUpdateMsgList()) {
                    result.add(deviceProfileProcessor.onDeviceProfileUpdate(tenantId, deviceProfileUpdateMsg));
                }
            }
            if (downlinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(deviceProcessor.onDeviceCredentialsUpdate(tenantId, deviceCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getAssetUpdateMsgCount() > 0) {
                for (AssetUpdateMsg assetUpdateMsg : downlinkMsg.getAssetUpdateMsgList()) {
                    result.add(assetProcessor.onAssetUpdate(tenantId, customerId, assetUpdateMsg, edgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getEntityViewUpdateMsgCount() > 0) {
                for (EntityViewUpdateMsg entityViewUpdateMsg : downlinkMsg.getEntityViewUpdateMsgList()) {
                    result.add(entityViewProcessor.onEntityViewUpdate(tenantId, customerId, entityViewUpdateMsg, edgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getRuleChainUpdateMsgCount() > 0) {
                for (RuleChainUpdateMsg ruleChainUpdateMsg : downlinkMsg.getRuleChainUpdateMsgList()) {
                    result.add(ruleChainProcessor.onRuleChainUpdate(tenantId, ruleChainUpdateMsg));
                }
            }
            if (downlinkMsg.getRuleChainMetadataUpdateMsgCount() > 0) {
                for (RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg : downlinkMsg.getRuleChainMetadataUpdateMsgList()) {
                    result.add(ruleChainProcessor.onRuleChainMetadataUpdate(tenantId, ruleChainMetadataUpdateMsg));
                }
            }
            if (downlinkMsg.getDashboardUpdateMsgCount() > 0) {
                for (DashboardUpdateMsg dashboardUpdateMsg : downlinkMsg.getDashboardUpdateMsgList()) {
                    result.add(dashboardProcessor.onDashboardUpdate(tenantId, customerId, dashboardUpdateMsg, edgeSettings.getCloudType()));
                }
            }
            if (downlinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : downlinkMsg.getAlarmUpdateMsgList()) {
                    result.add(alarmProcessor.onAlarmUpdate(tenantId, alarmUpdateMsg));
                }
            }
            if (downlinkMsg.getCustomerUpdateMsgCount() > 0) {
                for (CustomerUpdateMsg customerUpdateMsg : downlinkMsg.getCustomerUpdateMsgList()) {
                    try {
                        sequenceDependencyLock.lock();
                        result.add(customerProcessor.onCustomerUpdate(tenantId, customerUpdateMsg, edgeSettings.getCloudType()));
                        updateCustomerId(customerUpdateMsg);
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : downlinkMsg.getRelationUpdateMsgList()) {
                    result.add(relationProcessor.onRelationUpdate(tenantId, relationUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetsBundleUpdateMsgCount() > 0) {
                for (WidgetsBundleUpdateMsg widgetsBundleUpdateMsg : downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                    result.add(widgetsBundleProcessor.onWidgetsBundleUpdate(tenantId, widgetsBundleUpdateMsg));
                }
            }
            if (downlinkMsg.getWidgetTypeUpdateMsgCount() > 0) {
                for (WidgetTypeUpdateMsg widgetTypeUpdateMsg : downlinkMsg.getWidgetTypeUpdateMsgList()) {
                    result.add(widgetTypeProcessor.onWidgetTypeUpdate(tenantId, widgetTypeUpdateMsg));
                }
            }
            if (downlinkMsg.getUserUpdateMsgCount() > 0) {
                for (UserUpdateMsg userUpdateMsg : downlinkMsg.getUserUpdateMsgList()) {
                    try {
                        sequenceDependencyLock.lock();
                        result.add(userProcessor.onUserUpdate(tenantId, userUpdateMsg, this.edgeSettings.getCloudType()));
                    } finally {
                        sequenceDependencyLock.unlock();
                    }
                }
            }
            if (downlinkMsg.getUserCredentialsUpdateMsgCount() > 0) {
                for (UserCredentialsUpdateMsg userCredentialsUpdateMsg : downlinkMsg.getUserCredentialsUpdateMsgList()) {
                    result.add(userProcessor.onUserCredentialsUpdate(tenantId, userCredentialsUpdateMsg));
                }
            }
            if (downlinkMsg.getEntityGroupUpdateMsgCount() > 0) {
                for (EntityGroupUpdateMsg entityGroupUpdateMsg : downlinkMsg.getEntityGroupUpdateMsgList()) {
                    result.add(entityGroupProcessor.onEntityGroupUpdate(tenantId, entityGroupUpdateMsg));
                }
            }
            if (downlinkMsg.getCustomTranslationMsgCount() > 0) {
                for (CustomTranslationProto customTranslationProto : downlinkMsg.getCustomTranslationMsgList()) {
                    result.add(whiteLabelingProcessor.onCustomTranslationUpdate(tenantId, customTranslationProto));
                }
            }
            if (downlinkMsg.getWhiteLabelingParamsCount() > 0) {
                for (WhiteLabelingParamsProto whiteLabelingParamsProto : downlinkMsg.getWhiteLabelingParamsList()) {
                    result.add(whiteLabelingProcessor.onWhiteLabelingParamsUpdate(tenantId, whiteLabelingParamsProto));
                }
            }
            if (downlinkMsg.getLoginWhiteLabelingParamsCount() > 0) {
                for (LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto : downlinkMsg.getLoginWhiteLabelingParamsList()) {
                    result.add(whiteLabelingProcessor.onLoginWhiteLabelingParamsUpdate(tenantId, loginWhiteLabelingParamsProto));
                }
            }
            if (downlinkMsg.getSchedulerEventUpdateMsgCount() > 0) {
                for (SchedulerEventUpdateMsg schedulerEventUpdateMsg : downlinkMsg.getSchedulerEventUpdateMsgList()) {
                    result.add(schedulerEventProcessor.onScheduleEventUpdate(tenantId, schedulerEventUpdateMsg));
                }
            }
            if (downlinkMsg.getAdminSettingsUpdateMsgCount() > 0) {
                for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : downlinkMsg.getAdminSettingsUpdateMsgList()) {
                    result.add(adminSettingsProcessor.onAdminSettingsUpdate(tenantId, adminSettingsUpdateMsg));
                }
            }
            if (downlinkMsg.getRoleMsgCount() > 0) {
                for (RoleProto roleProto : downlinkMsg.getRoleMsgList()) {
                    result.add(roleProcessor.onRoleUpdate(tenantId, roleProto));
                }
            }
            if (downlinkMsg.getGroupPermissionMsgCount() > 0) {
                for (GroupPermissionProto groupPermissionProto : downlinkMsg.getGroupPermissionMsgList()) {
                    result.add(groupPermissionProcessor.onGroupPermissionUpdate(tenantId, groupPermissionProto));
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



    private void scheduleReconnect(Exception e) {
        initialized = false;
        if (tenantId != null) {
            save(DefaultDeviceStateService.ACTIVITY_STATE, false);
            save(DefaultDeviceStateService.LAST_DISCONNECT_TIME, System.currentTimeMillis());
        }
        if (scheduledFuture == null) {
            scheduledFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
                log.info("Trying to reconnect due to the error: {}!", e.getMessage());
                try {
                    edgeRpcClient.disconnect(true);
                } catch (Exception ex) {
                    log.error("Exception during disconnect: {}", ex.getMessage());
                }
                edgeRpcClient.connect(routingKey, routingSecret,
                        this::onUplinkResponse,
                        this::onEdgeUpdate,
                        this::onDownlink,
                        this::scheduleReconnect);
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
