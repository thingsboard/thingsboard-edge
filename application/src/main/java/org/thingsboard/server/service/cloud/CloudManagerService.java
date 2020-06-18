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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.Palette;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.FaviconProto;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.PaletteProto;
import org.thingsboard.server.gen.edge.PaletteSettingsProto;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.RuleNodeProto;
import org.thingsboard.server.gen.edge.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.integration.DefaultPlatformIntegrationService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.user.UserLoaderService;
import org.thingsboard.storage.EventStorage;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class CloudManagerService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Gson gson = new Gson();

    private final Lock deviceCreationLock = new ReentrantLock();
    private final Lock assetCreationLock = new ReentrantLock();
    private final Lock entityViewCreationLock = new ReentrantLock();
    private final Lock dashboardCreationLock = new ReentrantLock();
    private final Lock userCreationLock = new ReentrantLock();
    private final Lock customerCreationLock = new ReentrantLock();

    @Value("${cloud.routingKey}")
    private String routingKey;

    @Value("${cloud.secret}")
    private String routingSecret;

    @Value("${cloud.reconnect_timeout}")
    private long reconnectTimeoutMs;

    @Autowired
    @Qualifier("edgeFileEventStorage")
    private EventStorage<UplinkMsg> eventStorage;

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
    private DeviceStateService deviceStateService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    private EdgeRpcClient edgeRpcClient;

    private CountDownLatch latch;

    private ExecutorService executor;
    private ScheduledExecutorService reconnectScheduler;
    private ScheduledFuture<?> scheduledFuture;
    private volatile boolean initialized;

    private TenantId tenantId;

    @PostConstruct
    public void init() {
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
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
        entityViewService.deleteEntityViewsByTenantId(tenantId);
        dashboardService.deleteDashboardsByTenantId(tenantId);
        whiteLabelingService.saveSystemLoginWhiteLabelingParams(new LoginWhiteLabelingParams());
        whiteLabelingService.saveTenantWhiteLabelingParams(tenantId, new WhiteLabelingParams());
        customTranslationService.saveTenantCustomTranslation(tenantId, new CustomTranslation());
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
        Tenant savedTenant = tenantService.saveTenant(tenant);
        userLoaderService.createUser(Authority.TENANT_ADMIN, savedTenant.getId(), null, "tenant@thingsboard.org", "tenant");
        return savedTenant;
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
                        List<UplinkMsg> uplinkMsgList = eventStorage.readCurrentBatch();
                        latch = new CountDownLatch(uplinkMsgList.size());
                        for (UplinkMsg msg : uplinkMsgList) {
                            edgeRpcClient.sendUplinkMsg(msg);
                        }
                        boolean success = latch.await(10, TimeUnit.SECONDS);
                        if (!success) {
                            log.warn("Failed to deliver the batch: {}", uplinkMsgList);
                        }
                        if (success && !uplinkMsgList.isEmpty()) {
                            eventStorage.discardCurrentBatch();
                        } else {
                            eventStorage.sleep();
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
                onDeviceUpdate(entityUpdateMsg.getDeviceUpdateMsg());
            } else if (entityUpdateMsg.hasAssetUpdateMsg()) {
                log.debug("Asset update message received [{}]", entityUpdateMsg.getAssetUpdateMsg());
                onAssetUpdate(entityUpdateMsg.getAssetUpdateMsg());
            } else if (entityUpdateMsg.hasEntityViewUpdateMsg()) {
                log.debug("EntityView update message received [{}]", entityUpdateMsg.getEntityViewUpdateMsg());
                onEntityViewUpdate(entityUpdateMsg.getEntityViewUpdateMsg());
            } else if (entityUpdateMsg.hasRuleChainUpdateMsg()) {
                log.debug("Rule Chain udpate message received [{}]", entityUpdateMsg.getRuleChainUpdateMsg());
                onRuleChainUpdate(entityUpdateMsg.getRuleChainUpdateMsg());
            } else if (entityUpdateMsg.hasRuleChainMetadataUpdateMsg()) {
                log.debug("Rule Chain Metadata udpate message received [{}]", entityUpdateMsg.getRuleChainMetadataUpdateMsg());
                onRuleChainMetadataUpdate(entityUpdateMsg.getRuleChainMetadataUpdateMsg());
            } else if (entityUpdateMsg.hasDashboardUpdateMsg()) {
                log.debug("Dashboard message received [{}]", entityUpdateMsg.getDashboardUpdateMsg());
                onDashboardUpdate(entityUpdateMsg.getDashboardUpdateMsg());
            } else if (entityUpdateMsg.hasAlarmUpdateMsg()) {
                log.debug("Alarm message received [{}]", entityUpdateMsg.getAlarmUpdateMsg());
                onAlarmUpdate(entityUpdateMsg.getAlarmUpdateMsg());
            } else if (entityUpdateMsg.hasCustomerUpdateMsg()) {
                log.debug("Customer message received [{}]", entityUpdateMsg.getCustomerUpdateMsg());
                onCustomerUpdate(entityUpdateMsg.getCustomerUpdateMsg());
            } else if (entityUpdateMsg.hasRelationUpdateMsg()) {
                log.debug("Relation update message received [{}]", entityUpdateMsg.getRelationUpdateMsg());
                onRelationUpdate(entityUpdateMsg.getRelationUpdateMsg());
            } else if (entityUpdateMsg.hasUserUpdateMsg()) {
                log.debug("User message received [{}]", entityUpdateMsg.getUserUpdateMsg());
                onUserUpdate(entityUpdateMsg.getUserUpdateMsg());
            } else if (entityUpdateMsg.hasCustomTranslation()) {
                log.debug("Custom translation received [{}]", entityUpdateMsg.getCustomTranslation());
                onCustomTranslationUpdate(entityUpdateMsg.getCustomTranslation());
            } else if (entityUpdateMsg.hasWhiteLabelingParams()) {
                log.debug("White labeling params received [{}]", entityUpdateMsg.getWhiteLabelingParams());
                onWhiteLabelingParamsUpdate(entityUpdateMsg.getWhiteLabelingParams());
            } else if (entityUpdateMsg.hasLoginWhiteLabelingParams()) {
                log.debug("Login white labeling params received [{}]", entityUpdateMsg.getLoginWhiteLabelingParams());
                onLoginWhiteLabelingParamsUpdate(entityUpdateMsg.getLoginWhiteLabelingParams());
            } else if (entityUpdateMsg.hasSchedulerEventUpdateMsg()) {
                log.debug("Schedule event received [{}]", entityUpdateMsg.getSchedulerEventUpdateMsg());
                onScheduleEventUpdate(entityUpdateMsg.getSchedulerEventUpdateMsg());
            }
        } catch (Exception e) {
            log.error("Can't process entity updated msg [{}]", entityUpdateMsg, e);
        }
    }

    private void addEntityToGroup(String groupName, EntityId entityId, EntityType entityType) {
        if (!StringUtils.isEmpty(groupName)) {
            EntityGroup orCreateEntityGroup = entityGroupService.findOrCreateEntityGroup(tenantId, tenantId, entityType, groupName, null, null);
            if (orCreateEntityGroup != null) {
                entityGroupService.addEntityToEntityGroup(tenantId, orCreateEntityGroup.getId(), entityId);
            }
        }
    }

    private void onDeviceUpdate(DeviceUpdateMsg deviceUpdateMsg) {
        log.info("onDeviceUpdate {}", deviceUpdateMsg);
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                saveOrUpdateDevice(deviceUpdateMsg);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                deviceService.deleteDevice(tenantId, deviceId);
                break;
            case DEVICE_CONFLICT_RPC_MESSAGE:
                try {
                    deviceCreationLock.lock();
                    String deviceName = deviceUpdateMsg.getName();
                    Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                    if (deviceByName != null) {
                        deviceByName.setName(RandomStringUtils.randomAlphabetic(15));
                        deviceService.saveDevice(deviceByName);
                        Device deviceCopy = saveOrUpdateDevice(deviceUpdateMsg);
                        ListenableFuture<List<Void>> future = updateOrCopyDeviceRelatedEntities(deviceByName, deviceCopy);
                        Futures.transform(future, list -> {
                            log.debug("Related entities copied, removing origin device [{}]", deviceByName.getId());
                            deviceService.deleteDevice(tenantId, deviceByName.getId());
                            return null;
                        }, MoreExecutors.directExecutor());
                    }
                } finally {
                    deviceCreationLock.unlock();
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private ListenableFuture<List<Void>> updateOrCopyDeviceRelatedEntities(Device origin, Device destination) {
        updateAuditLogs(origin, destination);
        updateEvents(origin, destination);
        ArrayList<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(updateEntityViews(origin, destination));
        futures.add(updateAlarms(origin, destination));
        futures.add(copyAttributes(origin, destination));
        futures.add(copyRelations(origin, destination, EntitySearchDirection.FROM));
        futures.add(copyRelations(origin, destination, EntitySearchDirection.TO));
        return Futures.allAsList(futures);
    }

    private ListenableFuture<Void> copyRelations(Device origin, Device destination, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(origin.getId(), direction, -1, false));
        ListenableFuture<List<EntityRelation>> relationsByQueryFuture = relationService.findByQuery(tenantId, query);
        return Futures.transform(relationsByQueryFuture, relationsByQuery -> {
            if (relationsByQuery != null && !relationsByQuery.isEmpty()) {
                for (EntityRelation relation : relationsByQuery) {
                    if (EntitySearchDirection.FROM.equals(direction)) {
                        relation.setFrom(destination.getId());
                    } else {
                        relation.setTo(destination.getId());
                    }
                    relationService.saveRelationAsync(tenantId, relation);
                }
            }
            log.debug("Related [{}] relations copied, origin [{}], destination [{}]", direction.name(), origin.getId(), destination.getId());
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> updateEntityViews(Device origin, Device destination) {
        ListenableFuture<List<EntityView>> entityViewsFuture = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, origin.getId());
        return Futures.transform(entityViewsFuture, entityViews -> {
            if (entityViews != null && !entityViews.isEmpty()) {
                for (EntityView entityView : entityViews) {
                    entityView.setEntityId(destination.getId());
                    entityViewService.saveEntityView(entityView);
               }
            }
            log.debug("Related entity views updated, origin [{}], destination [{}]", origin.getId(), destination.getId());
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> copyAttributes(Device origin, Device destination) {
        ListenableFuture<List<AttributeKvEntry>> allFuture = attributesService.findAll(tenantId, origin.getId(), DataConstants.SERVER_SCOPE);
        return Futures.transform(allFuture, attributes -> {
            if (attributes != null && !attributes.isEmpty()) {
                attributesService.save(tenantId, destination.getId(), DataConstants.SERVER_SCOPE, attributes);
            }
            log.debug("Related attributes copied, origin [{}], destination [{}]", origin.getId(), destination.getId());
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> updateAlarms(Device origin, Device destination) {
        ListenableFuture<TimePageData<AlarmInfo>> alarmsFuture = alarmService.findAlarms(tenantId, new AlarmQuery(origin.getId(), new TimePageLink(Integer.MAX_VALUE), null, null, false));
        return Futures.transform(alarmsFuture, alarms -> {
            if (alarms != null && alarms.getData() != null && !alarms.getData().isEmpty()) {
                for (AlarmInfo alarm : alarms.getData()) {
                    alarm.setOriginator(destination.getId());
                    alarmService.createOrUpdateAlarm(alarm);
                }
            }
            log.debug("Related alarms updated, origin [{}], destination [{}]", origin.getId(), destination.getId());
            return null;
        }, MoreExecutors.directExecutor());
    }

    private void updateAuditLogs(Device origin, Device destination) {
        TimePageData<AuditLog> auditLogs = auditLogService.findAuditLogsByTenantIdAndEntityId(tenantId, origin.getId(), null, new TimePageLink(Integer.MAX_VALUE));
        if (auditLogs != null && auditLogs.getData() != null && !auditLogs.getData().isEmpty()) {
            for (AuditLog auditLogEntry : auditLogs.getData()) {
                auditLogEntry.setEntityId(destination.getId());
                auditLogService.saveOrUpdateAuditLog(auditLogEntry);
            }
        }
        log.debug("Related audit logs updated, origin [{}], destination [{}]", origin.getId(), destination.getId());
    }

    private void updateEvents(Device origin, Device destination) {
        TimePageData<Event> events = eventService.findEvents(tenantId, origin.getId(), new TimePageLink(Integer.MAX_VALUE));
        if (events != null && events.getData() != null && !events.getData().isEmpty()) {
            for (Event event : events.getData()) {
                event.setEntityId(destination.getId());
                eventService.saveAsync(event);
            }
        }
        log.debug("Related events updated, origin [{}], destination [{}]", origin.getId(), destination.getId());
    }

    private Device saveOrUpdateDevice(DeviceUpdateMsg deviceUpdateMsg) {
        Device device;
        try {
            deviceCreationLock.lock();
            DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
            device = deviceService.findDeviceById(tenantId, deviceId);
            boolean create = false;
            if (device == null) {
                device = new Device();
                device.setTenantId(tenantId);
                device.setId(deviceId);
                create = true;
            }
            device.setName(deviceUpdateMsg.getName());
            device.setType(deviceUpdateMsg.getType());
            device.setLabel(deviceUpdateMsg.getLabel());
            device = deviceService.saveDevice(device, create);
            if (create) {
                deviceStateService.onDeviceAdded(device);
            }
            addEntityToGroup(deviceUpdateMsg.getGroupName(), device.getId(), EntityType.DEVICE);
            updateDeviceCredentials(deviceUpdateMsg, device);
        } finally {
            deviceCreationLock.unlock();
        }
        return device;
    }

    private void updateDeviceCredentials(DeviceUpdateMsg deviceUpdateMsg, Device device) {
        log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                device.getName(), deviceUpdateMsg.getCredentialsId(), deviceUpdateMsg.getCredentialsValue());

        try {
            DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
            deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceUpdateMsg.getCredentialsType()));
            deviceCredentials.setCredentialsId(deviceUpdateMsg.getCredentialsId());
            deviceCredentials.setCredentialsValue(deviceUpdateMsg.getCredentialsValue());
            deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
        } catch (Exception e) {
            log.error("Can't update device credentials for device [{}], deviceUpdateMsg [{}]", device.getName(), deviceUpdateMsg, e);
        }
        log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                device.getName(), deviceUpdateMsg.getCredentialsId(), deviceUpdateMsg.getCredentialsValue());

    }

    private void onAssetUpdate(AssetUpdateMsg assetUpdateMsg) {
        log.info("onAssetUpdate {}", assetUpdateMsg);
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        switch (assetUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    assetCreationLock.lock();
                    Asset asset = assetService.findAssetById(tenantId, assetId);
                    boolean create = false;
                    if (asset == null) {
                        asset = new Asset();
                        asset.setTenantId(tenantId);
                        asset.setId(assetId);
                        create = true;
                    }
                    asset.setName(assetUpdateMsg.getName());
                    asset.setType(assetUpdateMsg.getType());
                    asset.setLabel(assetUpdateMsg.getLabel());
                    assetService.saveAsset(asset, create);
                    addEntityToGroup(assetUpdateMsg.getGroupName(), asset.getId(), EntityType.ASSET);
                } finally {
                    assetCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                assetService.deleteAsset(tenantId, assetId);
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void onEntityViewUpdate(EntityViewUpdateMsg entityViewUpdateMsg) {
        log.info("onEntityViewUpdate {}", entityViewUpdateMsg);
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));
        switch (entityViewUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    entityViewCreationLock.lock();
                    EntityView entityView = entityViewService.findEntityViewById(tenantId, entityViewId);
                    boolean create = false;
                    if (entityView == null) {
                        entityView = new EntityView();
                        entityView.setTenantId(tenantId);
                        entityView.setId(entityViewId);
                        create = true;
                    }
                    EntityId entityId = null;
                    switch (entityViewUpdateMsg.getEntityType()) {
                        case DEVICE:
                            entityId = new DeviceId(new UUID(entityViewUpdateMsg.getEntityIdMSB(), entityViewUpdateMsg.getEntityIdLSB()));
                            break;
                        case ASSET:
                            entityId = new AssetId(new UUID(entityViewUpdateMsg.getEntityIdMSB(), entityViewUpdateMsg.getEntityIdLSB()));
                            break;
                    }
                    entityView.setName(entityViewUpdateMsg.getName());
                    entityView.setType(entityViewUpdateMsg.getType());
                    entityView.setEntityId(entityId);
                    entityViewService.saveEntityView(entityView, create);
                    addEntityToGroup(entityViewUpdateMsg.getGroupName(), entityView.getId(), EntityType.ENTITY_VIEW);
                } finally {
                    entityViewCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                entityViewService.deleteEntityView(tenantId, entityViewId);
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void onDashboardUpdate(DashboardUpdateMsg dashboardUpdateMsg) {
        log.info("DashboardUpdateMsg {}", dashboardUpdateMsg);
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        switch (dashboardUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    dashboardCreationLock.lock();
                    boolean create = false;
                    Dashboard dashboard = dashboardService.findDashboardById(tenantId, dashboardId);
                    if (dashboard == null) {
                        create = true;
                        dashboard = new Dashboard();
                        dashboard.setId(dashboardId);
                        dashboard.setTenantId(tenantId);
                    }
                    dashboard.setTitle(dashboardUpdateMsg.getTitle());
                    dashboard.setConfiguration(JacksonUtil.toJsonNode(dashboardUpdateMsg.getConfiguration()));
                    Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
                    if (create) {
                        entityGroupService.addEntityToEntityGroupAll(savedDashboard.getTenantId(), savedDashboard.getOwnerId(), savedDashboard.getId());
                    }
                    addEntityToGroup(dashboardUpdateMsg.getGroupName(), savedDashboard.getId(), EntityType.DASHBOARD);
                } finally {
                    dashboardCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                dashboardService.deleteDashboard(tenantId, dashboardId);
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void onScheduleEventUpdate(SchedulerEventUpdateMsg schedulerEventUpdateMsg) {
        try {
            SchedulerEventId schedulerEventId = new SchedulerEventId(new UUID(schedulerEventUpdateMsg.getIdMSB(), schedulerEventUpdateMsg.getIdLSB()));
            switch (schedulerEventUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    SchedulerEvent schedulerEvent = schedulerEventService.findSchedulerEventById(tenantId, schedulerEventId);
                    if (schedulerEvent == null) {
                        schedulerEvent = new SchedulerEvent();
                        schedulerEvent.setId(schedulerEventId);
                        schedulerEvent.setTenantId(tenantId);
                    }
                    schedulerEvent.setName(schedulerEventUpdateMsg.getName());
                    schedulerEvent.setType(schedulerEventUpdateMsg.getType());
                    schedulerEvent.setSchedule(JacksonUtil.toJsonNode(schedulerEventUpdateMsg.getSchedule()));
                    schedulerEvent.setConfiguration(JacksonUtil.toJsonNode(schedulerEventUpdateMsg.getConfiguration()));
                    schedulerEventService.saveSchedulerEvent(schedulerEvent);

                    // TODO: voba fix
                    // actorService.onEntityStateChange(tenantId, schedulerEventId, ComponentLifecycleEvent.UPDATED);

                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    schedulerEventService.deleteSchedulerEvent(tenantId, schedulerEventId);
                    break;
                case UNRECOGNIZED:
                    log.error("Unsupported msg type");
            }
        } catch (Exception e) {
            log.error("Can't process SchedulerEventUpdateMsg [{}]", schedulerEventUpdateMsg, e);
        }
    }

    private void onRuleChainUpdate(RuleChainUpdateMsg ruleChainUpdateMsg) {
        try {
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB()));
            switch (ruleChainUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, ruleChainId);
                    if (ruleChain == null) {
                        ruleChain = new RuleChain();
                        ruleChain.setId(ruleChainId);
                        ruleChain.setTenantId(tenantId);
                    }
                    ruleChain.setName(ruleChainUpdateMsg.getName());
                    if (ruleChainUpdateMsg.getFirstRuleNodeIdMSB() != 0 && ruleChainUpdateMsg.getFirstRuleNodeIdLSB() != 0) {
                        ruleChain.setFirstRuleNodeId(new RuleNodeId(new UUID(ruleChainUpdateMsg.getFirstRuleNodeIdMSB(), ruleChainUpdateMsg.getFirstRuleNodeIdLSB())));
                    }
                    ruleChain.setConfiguration(JacksonUtil.toJsonNode(ruleChainUpdateMsg.getConfiguration()));
                    ruleChain.setRoot(ruleChainUpdateMsg.getRoot());
                    ruleChain.setDebugMode(ruleChainUpdateMsg.getDebugMode());
                    ruleChainService.saveRuleChain(ruleChain);

                    eventStorage.write(constructRuleChainMetadataRequestMsg(ruleChain), edgeEventSaveCallback);

                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    ruleChainService.deleteRuleChainById(tenantId, ruleChainId);
                    break;
                case UNRECOGNIZED:
                    log.error("Unsupported msg type");
            }
        } catch (Exception e) {
            log.error("Can't process RuleChainUpdateMsg [{}]", ruleChainUpdateMsg, e);
        }
    }

    private UplinkMsg constructRuleChainMetadataRequestMsg(RuleChain ruleChain) {
        RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg = RuleChainMetadataRequestMsg.newBuilder()
                .setRuleChainIdMSB(ruleChain.getId().getId().getMostSignificantBits())
                .setRuleChainIdLSB(ruleChain.getId().getId().getLeastSignificantBits())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .addAllRuleChainMetadataRequestMsg(Collections.singletonList(ruleChainMetadataRequestMsg));
        return builder.build();
    }

    private IntegrationCallback<Void> edgeEventSaveCallback = new IntegrationCallback<Void>() {
        @Override
        public void onSuccess(@Nullable Void aVoid) {
            log.debug("Event saved successfully!");
        }

        @Override
        public void onError(Throwable t) {
            log.warn("Failure during event save", t);
        }
    };

    private void onRuleChainMetadataUpdate(RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        try {
            switch (ruleChainMetadataUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    RuleChainMetaData ruleChainMetadata = new RuleChainMetaData();
                    RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainMetadataUpdateMsg.getRuleChainIdMSB(), ruleChainMetadataUpdateMsg.getRuleChainIdLSB()));
                    ruleChainMetadata.setRuleChainId(ruleChainId);
                    ruleChainMetadata.setFirstNodeIndex(ruleChainMetadataUpdateMsg.getFirstNodeIndex());
                    ruleChainMetadata.setNodes(parseNodeProtos(ruleChainId, ruleChainMetadataUpdateMsg.getNodesList()));
                    ruleChainMetadata.setConnections(parseConnectionProtos(ruleChainMetadataUpdateMsg.getConnectionsList()));
                    ruleChainMetadata.setRuleChainConnections(parseRuleChainConnectionProtos(ruleChainMetadataUpdateMsg.getRuleChainConnectionsList()));
                    ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetadata);

                    tbClusterService.onEntityStateChange(tenantId, ruleChainId, ComponentLifecycleEvent.UPDATED);
                    break;
                case UNRECOGNIZED:
                    log.error("Unsupported msg type");
            }
        } catch (Exception e) {
            log.error("Can't process RuleChainMetadataUpdateMsg [{}]", ruleChainMetadataUpdateMsg, e);
        }
    }

    private List<RuleChainConnectionInfo> parseRuleChainConnectionProtos(List<org.thingsboard.server.gen.edge.RuleChainConnectionInfoProto> ruleChainConnectionsList) throws IOException {
        List<RuleChainConnectionInfo> result = new ArrayList<>();
        for (org.thingsboard.server.gen.edge.RuleChainConnectionInfoProto proto : ruleChainConnectionsList) {
            RuleChainConnectionInfo info = new RuleChainConnectionInfo();
            info.setFromIndex(proto.getFromIndex());
            info.setTargetRuleChainId(new RuleChainId(new UUID(proto.getTargetRuleChainIdMSB(), proto.getTargetRuleChainIdLSB())));
            info.setType(proto.getType());
            info.setAdditionalInfo(mapper.readTree(proto.getAdditionalInfo()));
            result.add(info);
        }
        return result;
    }

    private List<NodeConnectionInfo> parseConnectionProtos(List<org.thingsboard.server.gen.edge.NodeConnectionInfoProto> connectionsList) {
        List<NodeConnectionInfo> result = new ArrayList<>();
        for (org.thingsboard.server.gen.edge.NodeConnectionInfoProto proto : connectionsList) {
            NodeConnectionInfo info = new NodeConnectionInfo();
            info.setFromIndex(proto.getFromIndex());
            info.setToIndex(proto.getToIndex());
            info.setType(proto.getType());
            result.add(info);
        }
        return result;
    }

    private List<RuleNode> parseNodeProtos(RuleChainId ruleChainId, List<RuleNodeProto> nodesList) throws IOException {
        List<RuleNode> result = new ArrayList<>();
        for (RuleNodeProto proto : nodesList) {
            RuleNode ruleNode = new RuleNode();
            RuleNodeId ruleNodeId = new RuleNodeId(new UUID(proto.getIdMSB(), proto.getIdLSB()));
            ruleNode.setId(ruleNodeId);
            ruleNode.setRuleChainId(ruleChainId);
            ruleNode.setType(proto.getType());
            ruleNode.setName(proto.getName());
            ruleNode.setDebugMode(proto.getDebugMode());
            ruleNode.setConfiguration(mapper.readTree(proto.getConfiguration()));
            ruleNode.setAdditionalInfo(mapper.readTree(proto.getAdditionalInfo()));
            result.add(ruleNode);
        }
        return result;
    }

    private void onAlarmUpdate(AlarmUpdateMsg alarmUpdateMsg) {
        EntityId originatorId = getAlarmOriginator(alarmUpdateMsg.getOriginatorName(), EntityType.valueOf(alarmUpdateMsg.getOriginatorType()));
        if (originatorId != null) {
            try {
                Alarm existingAlarm = alarmService.findLatestByOriginatorAndType(tenantId, originatorId, alarmUpdateMsg.getType()).get();
                switch (alarmUpdateMsg.getMsgType()) {
                    case ENTITY_CREATED_RPC_MESSAGE:
                    case ENTITY_UPDATED_RPC_MESSAGE:
                        if (existingAlarm == null || existingAlarm.getStatus().isCleared()) {
                            existingAlarm = new Alarm();
                            existingAlarm.setTenantId(tenantId);
                            existingAlarm.setType(alarmUpdateMsg.getName());
                            existingAlarm.setOriginator(originatorId);
                            existingAlarm.setSeverity(AlarmSeverity.valueOf(alarmUpdateMsg.getSeverity()));
                            existingAlarm.setStatus(AlarmStatus.valueOf(alarmUpdateMsg.getStatus()));
                            existingAlarm.setStartTs(alarmUpdateMsg.getStartTs());
                            existingAlarm.setAckTs(alarmUpdateMsg.getAckTs());
                            existingAlarm.setClearTs(alarmUpdateMsg.getClearTs());
                            existingAlarm.setPropagate(alarmUpdateMsg.getPropagate());
                        }
                        existingAlarm.setEndTs(alarmUpdateMsg.getEndTs());
                        existingAlarm.setDetails(mapper.readTree(alarmUpdateMsg.getDetails()));
                        alarmService.createOrUpdateAlarm(existingAlarm);
                        break;
                    case ALARM_ACK_RPC_MESSAGE:
                        if (existingAlarm != null) {
                            alarmService.ackAlarm(tenantId, existingAlarm.getId(), alarmUpdateMsg.getAckTs());
                        }
                        break;
                    case ALARM_CLEAR_RPC_MESSAGE:
                        if (existingAlarm != null) {
                            alarmService.clearAlarm(tenantId, existingAlarm.getId(), mapper.readTree(alarmUpdateMsg.getDetails()), alarmUpdateMsg.getAckTs());
                        }
                        break;
                    case ENTITY_DELETED_RPC_MESSAGE:
                        if (existingAlarm != null) {
                            alarmService.deleteAlarm(tenantId, existingAlarm.getId());
                        }
                        break;
                }
            } catch (Exception e) {
                log.error("Error during on alarm update msg", e);
            }
        }
    }

    private void onRelationUpdate(RelationUpdateMsg relationUpdateMsg) {
        log.info("onRelationUpdate {}", relationUpdateMsg);
        try {
            EntityRelation entityRelation = new EntityRelation();

            UUID fromUUID = new UUID(relationUpdateMsg.getFromIdMSB(), relationUpdateMsg.getFromIdLSB());
            EntityId fromId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getFromEntityType()), fromUUID);
            entityRelation.setFrom(fromId);

            UUID toUUID = new UUID(relationUpdateMsg.getToIdMSB(), relationUpdateMsg.getToIdLSB());
            EntityId toId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getToEntityType()), toUUID);
            entityRelation.setTo(toId);

            entityRelation.setType(relationUpdateMsg.getType());
            entityRelation.setTypeGroup(RelationTypeGroup.valueOf(relationUpdateMsg.getTypeGroup()));
            entityRelation.setAdditionalInfo(mapper.readTree(relationUpdateMsg.getAdditionalInfo()));

            switch (relationUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    relationService.saveRelationAsync(tenantId, entityRelation);
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    relationService.deleteRelation(tenantId, entityRelation);
                    break;
                case UNRECOGNIZED:
                    log.error("Unsupported msg type");
            }
        } catch (Exception e) {
            log.error("Error during relation update msg", e);
        }
    }

    private void onCustomerUpdate(CustomerUpdateMsg customerUpdateMsg) {
        log.info("onCustomerUpdate {}", customerUpdateMsg);
        CustomerId customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
        switch (customerUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    customerCreationLock.lock();
                    Customer customer = customerService.findCustomerById(tenantId, customerId);
                    boolean create = false;
                    if (customer == null) {
                        customer = new Customer();
                        customer.setId(customerId);
                        customer.setTenantId(tenantId);
                        create = true;
                    }
                    customer.setTitle(customerUpdateMsg.getTitle());
                    customer.setCountry(customerUpdateMsg.getCountry());
                    customer.setState(customerUpdateMsg.getState());
                    customer.setCity(customerUpdateMsg.getCity());
                    customer.setAddress(customerUpdateMsg.getAddress());
                    customer.setAddress2(customerUpdateMsg.getAddress2());
                    customer.setZip(customerUpdateMsg.getZip());
                    customer.setPhone(customerUpdateMsg.getPhone());
                    customer.setEmail(customerUpdateMsg.getEmail());
                    customer.setAdditionalInfo(JacksonUtil.toJsonNode(customerUpdateMsg.getAdditionalInfo()));
                    customerService.saveCustomer(customer, create);
                } finally {
                    customerCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                customerService.deleteCustomer(tenantId, customerId);
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void onUserUpdate(UserUpdateMsg userUpdateMsg) {
        log.info("onUserUpdate {}", userUpdateMsg);
        UserId userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
        switch (userUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    userCreationLock.lock();
                    boolean create = false;
                    User user = userService.findUserById(tenantId, userId);
                    if (user == null) {
                        user = new User();
                        user.setTenantId(tenantId);
                        user.setId(userId);
                        create = true;
                    }
                    user.setEmail(userUpdateMsg.getEmail());
                    // TODO: voba - fix this hardcoded authority
                    user.setAuthority(Authority.TENANT_ADMIN);
                    // user.setAuthority(Authority.parse(userUpdateMsg.getAuthority()));
                    user.setFirstName(userUpdateMsg.getFirstName());
                    user.setLastName(userUpdateMsg.getLastName());
                    user.setAdditionalInfo(JacksonUtil.toJsonNode(userUpdateMsg.getAdditionalInfo()));
                    User savedUser = userService.saveUser(user, create);

                    UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
                    userCredentials.setEnabled(userUpdateMsg.getEnabled());
                    userCredentials.setPassword(userUpdateMsg.getPassword());
                    userCredentials.setActivateToken(null);
                    userCredentials.setResetToken(null);
                    userService.saveUserCredentials(tenantId, userCredentials);

                    addEntityToGroup(userUpdateMsg.getGroupName(), savedUser.getId(), EntityType.USER);

                    addEntityToGroup("Tenant Users", savedUser.getId(), EntityType.USER);
                } finally {
                    userCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                User userToDelete = userService.findUserByEmail(tenantId, userUpdateMsg.getEmail());
                if (userToDelete != null) {
                    userService.deleteUser(tenantId, userToDelete.getId());
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void onCustomTranslationUpdate(CustomTranslationProto customTranslationProto) {
        try {
            CustomTranslation customTranslation = new CustomTranslation();
            customTranslation.setTranslationMap(customTranslationProto.getTranslationMapMap());
            customTranslationService.saveTenantCustomTranslation(tenantId, customTranslation);
        } catch (Exception e) {
            log.error("Exception during updating custom translation", e);
        }
    }

    private void onLoginWhiteLabelingParamsUpdate(LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto) {
        try {
            LoginWhiteLabelingParams loginWhiteLabelingParams = constructLoginWhiteLabelingParams(loginWhiteLabelingParamsProto);
            whiteLabelingService.saveSystemLoginWhiteLabelingParams(loginWhiteLabelingParams);
        } catch (Exception e) {
            log.error("Exception during updating login white labeling params", e);
        }
    }

    public LoginWhiteLabelingParams constructLoginWhiteLabelingParams(LoginWhiteLabelingParamsProto loginWLPProto) {
        LoginWhiteLabelingParams loginWLP = new LoginWhiteLabelingParams();
        loginWLP.setLogoImageUrl(loginWLPProto.getLogoImageUrl());
        loginWLP.setLogoImageChecksum(loginWLPProto.getLogoImageChecksum());
        loginWLP.setLogoImageHeight((int) loginWLPProto.getLogoImageHeight());
        loginWLP.setAppTitle(loginWLPProto.getAppTitle());
        loginWLP.setFavicon(constructFavicon(loginWLPProto.getFavicon()));
        loginWLP.setFaviconChecksum(loginWLPProto.getFaviconChecksum());
        loginWLP.setPaletteSettings(constructPaletteSettings(loginWLPProto.getPaletteSettings()));
        loginWLP.setHelpLinkBaseUrl(loginWLPProto.getHelpLinkBaseUrl());
        loginWLP.setEnableHelpLinks(loginWLPProto.getEnableHelpLinks());
        loginWLP.setShowNameVersion(loginWLPProto.getShowNameVersion());
        loginWLP.setPlatformName(loginWLPProto.getPlatformName());
        loginWLP.setPlatformVersion(loginWLPProto.getPlatformVersion());

        loginWLP.setPageBackgroundColor(loginWLPProto.getPageBackgroundColor());
        loginWLP.setDarkForeground(loginWLPProto.getDarkForeground());
        loginWLP.setDomainName(loginWLPProto.getDomainName());
        loginWLP.setAdminSettingsId(loginWLPProto.getAdminSettingsId());
        loginWLP.setShowNameBottom(loginWLPProto.getShowNameBottom());

        return loginWLP;
    }

    private void onWhiteLabelingParamsUpdate(WhiteLabelingParamsProto wLPProto) {
        try {
            WhiteLabelingParams wLP = constructWhiteLabelingParams(wLPProto);
            whiteLabelingService.saveTenantWhiteLabelingParams(tenantId, wLP);
        } catch (Exception e) {
            log.error("Exception during updating white labeling params", e);
        }
    }

    private WhiteLabelingParams constructWhiteLabelingParams(WhiteLabelingParamsProto whiteLabelingParamsProto) {
        WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
        whiteLabelingParams.setLogoImageUrl(whiteLabelingParamsProto.getLogoImageUrl());
        whiteLabelingParams.setLogoImageChecksum(whiteLabelingParamsProto.getLogoImageChecksum());
        whiteLabelingParams.setLogoImageHeight((int) whiteLabelingParamsProto.getLogoImageHeight());
        whiteLabelingParams.setAppTitle(whiteLabelingParamsProto.getAppTitle());
        whiteLabelingParams.setFavicon(constructFavicon(whiteLabelingParamsProto.getFavicon()));
        whiteLabelingParams.setFaviconChecksum(whiteLabelingParamsProto.getFaviconChecksum());
        whiteLabelingParams.setPaletteSettings(constructPaletteSettings(whiteLabelingParamsProto.getPaletteSettings()));
        whiteLabelingParams.setHelpLinkBaseUrl(whiteLabelingParamsProto.getHelpLinkBaseUrl());
        whiteLabelingParams.setEnableHelpLinks(whiteLabelingParamsProto.getEnableHelpLinks());
        whiteLabelingParams.setShowNameVersion(whiteLabelingParamsProto.getShowNameVersion());
        whiteLabelingParams.setPlatformName(whiteLabelingParamsProto.getPlatformName());
        whiteLabelingParams.setPlatformVersion(whiteLabelingParamsProto.getPlatformVersion());
        return whiteLabelingParams;
    }

    private Favicon constructFavicon(FaviconProto faviconProto) {
        Favicon favicon = new Favicon();
        favicon.setUrl(faviconProto.getUrl());
        favicon.setType(faviconProto.getType());
        return favicon;
    }

    private PaletteSettings constructPaletteSettings(PaletteSettingsProto paletteSettingsProto) {
        PaletteSettings paletteSettings = new PaletteSettings();
        paletteSettings.setPrimaryPalette(constructPalette(paletteSettingsProto.getPrimaryPalette()));
        paletteSettings.setAccentPalette(constructPalette(paletteSettingsProto.getAccentPalette()));
        return paletteSettings;
    }

    private Palette constructPalette(PaletteProto paletteProto) {
        Palette palette = new Palette();
        palette.setType(paletteProto.getType());
        palette.setExtendsPalette(paletteProto.getExtendsPalette());
        palette.setColors(paletteProto.getColorsMap());
        return palette;
    }

    private EntityId getAlarmOriginator(String entityName, EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return deviceService.findDeviceByTenantIdAndName(tenantId, entityName).getId();
            case ASSET:
                return assetService.findAssetByTenantIdAndName(tenantId, entityName).getId();
            case ENTITY_VIEW:
                return entityViewService.findEntityViewByTenantIdAndName(tenantId, entityName).getId();
            default:
                return null;
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
        } catch (Exception e) {
            log.error("Can't process downlink message [{}]", downlinkMsg, e);
        }
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
