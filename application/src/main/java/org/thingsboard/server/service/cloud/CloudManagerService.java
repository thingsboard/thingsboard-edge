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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.rpc.api.RpcCallback;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
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
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
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
import org.thingsboard.server.gen.edge.EdgeEntityType;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.FaviconProto;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.PaletteProto;
import org.thingsboard.server.gen.edge.PaletteSettingsProto;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.RuleNodeProto;
import org.thingsboard.server.gen.edge.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
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

    private final Lock deviceCreationLock = new ReentrantLock();
    private final Lock assetCreationLock = new ReentrantLock();
    private final Lock entityViewCreationLock = new ReentrantLock();
    private final Lock dashboardCreationLock = new ReentrantLock();
    private final Lock userCreationLock = new ReentrantLock();

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
    private UserLoaderService userLoaderService;

    @Autowired
    private RuleChainService ruleChainService;

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
    private AlarmService alarmService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private RuleEngineTelemetryService telemetryService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private ActorService actorService;

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
        log.info("Starting edge mock service");
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
        Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceUpdateMsg.getName());
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                // TODO: add synchronized lock
                try {
                    deviceCreationLock.lock();
                    if (device == null) {
                        device = new Device();
                        device.setTenantId(tenantId);
                        device.setName(deviceUpdateMsg.getName());
                        device.setType(deviceUpdateMsg.getType());
                        device.setLabel(deviceUpdateMsg.getLabel());
                        device = deviceService.saveDevice(device);
                        deviceStateService.onDeviceAdded(device);
                    } else {
                        device.setType(deviceUpdateMsg.getType());
                        device.setLabel(deviceUpdateMsg.getLabel());
                        device = deviceService.saveDevice(device);
                    }
                    addEntityToGroup(deviceUpdateMsg.getGroupName(), device.getId(), EntityType.DEVICE);
                    updateDeviceCredentials(deviceUpdateMsg, device);
                } finally {
                    deviceCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                if (device != null) {
                    deviceService.deleteDevice(tenantId, device.getId());
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void updateDeviceCredentials(DeviceUpdateMsg deviceUpdateMsg, Device device) {
        log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                device.getName(), deviceUpdateMsg.getCredentialsId(), deviceUpdateMsg.getCredentialsValue());

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceUpdateMsg.getCredentialsType()));
        deviceCredentials.setCredentialsId(deviceUpdateMsg.getCredentialsId());
        deviceCredentials.setCredentialsValue(deviceUpdateMsg.getCredentialsValue());
        deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
        log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                device.getName(), deviceUpdateMsg.getCredentialsId(), deviceUpdateMsg.getCredentialsValue());

    }

    private void onAssetUpdate(AssetUpdateMsg assetUpdateMsg) {
        log.info("onAssetUpdate {}", assetUpdateMsg);
        Asset asset = assetService.findAssetByTenantIdAndName(tenantId, assetUpdateMsg.getName());
        switch (assetUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    assetCreationLock.lock();
                    if (asset == null) {
                        asset = new Asset();
                        asset.setTenantId(tenantId);
                        asset.setName(assetUpdateMsg.getName());
                        asset.setType(assetUpdateMsg.getType());
                        asset.setLabel(assetUpdateMsg.getLabel());
                        assetService.saveAsset(asset);
                    } else {
                        asset.setType(assetUpdateMsg.getType());
                        asset.setLabel(assetUpdateMsg.getLabel());
                        assetService.saveAsset(asset);
                    }
                    addEntityToGroup(assetUpdateMsg.getGroupName(), asset.getId(), EntityType.ASSET);
                } finally {
                    assetCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                if (asset != null) {
                    assetService.deleteAsset(tenantId, asset.getId());
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void onEntityViewUpdate(EntityViewUpdateMsg entityViewUpdateMsg) {
        log.info("onEntityViewUpdate {}", entityViewUpdateMsg);
        EntityView entityView = entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewUpdateMsg.getName());
        EntityId relatedEntityId = getRelatedEntityId(entityViewUpdateMsg);
        switch (entityViewUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    entityViewCreationLock.lock();
                    if (entityView == null) {
                        entityView = new EntityView();
                        entityView.setTenantId(tenantId);
                        entityView.setName(entityViewUpdateMsg.getName());
                        entityView.setType(entityViewUpdateMsg.getType());
                        entityView.setEntityId(relatedEntityId);
                        entityViewService.saveEntityView(entityView);
                    } else {
                        entityView.setEntityId(relatedEntityId);
                        entityView.setType(entityViewUpdateMsg.getType());
                        entityViewService.saveEntityView(entityView);
                    }
                    addEntityToGroup(entityViewUpdateMsg.getGroupName(), entityView.getId(), EntityType.ENTITY_VIEW);
                } finally {
                    entityViewCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                if (entityView != null) {
                    entityViewService.deleteEntityView(tenantId, entityView.getId());
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private EntityId getRelatedEntityId(EntityViewUpdateMsg entityViewUpdateMsg) {
        String entityName = entityViewUpdateMsg.getRelatedName();
        if (EdgeEntityType.DEVICE.equals(entityViewUpdateMsg.getRelatedEntityType())) {
            Device device = deviceService.findDeviceByTenantIdAndName(tenantId, entityName);
            if (device == null) {
                throw new RuntimeException("Related device [" + entityName + "] doesn't exist! Can't create entityView [" + entityViewUpdateMsg + "]");
            }
            return device.getId();
        } else if (EdgeEntityType.ASSET.equals(entityViewUpdateMsg.getRelatedEntityType())) {
            Asset asset = assetService.findAssetByTenantIdAndName(tenantId, entityName);
            if (asset == null) {
                throw new RuntimeException("Related asset [" + entityName + "] doesn't exist! Can't create entityView [" + entityViewUpdateMsg + "]");
            }
            return asset.getId();
        }
        throw new RuntimeException("Unsupported related EntityType [" + entityViewUpdateMsg.getRelatedEntityType() + "]");
    }

    private void onDashboardUpdate(DashboardUpdateMsg dashboardUpdateMsg) {
        log.info("DashboardUpdateMsg {}", dashboardUpdateMsg);
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        switch (dashboardUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    dashboardCreationLock.lock();
                    boolean created = false;
                    Dashboard dashboard = dashboardService.findDashboardById(tenantId, dashboardId);
                    if (dashboard == null) {
                        created = true;
                        dashboard = new Dashboard();
                        dashboard.setId(dashboardId);
                        dashboard.setTenantId(tenantId);

                    }
                    dashboard.setTitle(dashboardUpdateMsg.getTitle());
                    dashboard.setConfiguration(JacksonUtil.toJsonNode(dashboardUpdateMsg.getConfiguration()));
                    Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
                    if (created) {
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
                    actorService.onEntityStateChange(tenantId, schedulerEventId, ComponentLifecycleEvent.UPDATED);

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
                    actorService.onEntityStateChange(tenantId, ruleChainId, ComponentLifecycleEvent.UPDATED);

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

    private RpcCallback<Void> edgeEventSaveCallback = new RpcCallback<Void>() {
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
                    actorService.onEntityStateChange(tenantId, ruleChainId, ComponentLifecycleEvent.UPDATED);
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

    private void onCustomerUpdate(CustomerUpdateMsg customerUpdateMsg) {
        CustomerId customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
        switch (customerUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                boolean created = false;
                Customer customer = customerService.findCustomerById(tenantId, customerId);
                if (customer == null) {
                    created = true;
                    customer = new Customer();
                    customer.setId(customerId);
                    customer.setTenantId(tenantId);
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
                Customer savedCustomer = customerService.saveCustomer(customer);

                if (created) {
                    entityGroupService.addEntityToEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getOwnerId(), savedCustomer.getId());
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
        switch (userUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    userCreationLock.lock();
                    User user = userService.findUserByEmail(tenantId, userUpdateMsg.getEmail());
                    if (user == null) {
                        user = new User();
                        user.setTenantId(tenantId);
                    }
                    user.setEmail(userUpdateMsg.getEmail());
                    user.setAuthority(Authority.parse(userUpdateMsg.getAuthority()));
                    user.setFirstName(userUpdateMsg.getFirstName());
                    user.setLastName(userUpdateMsg.getLastName());
                    user.setAdditionalInfo(JacksonUtil.toJsonNode(userUpdateMsg.getAdditionalInfo()));
                    User savedUser = userService.saveUser(user);

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
            whiteLabelingService.saveTenantLoginWhiteLabelingParams(tenantId, loginWhiteLabelingParams);
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
                    TbMsg tbMsg = null;
                    TbMsg tmp = TbMsg.fromBytes(entityData.getTbMsg().toByteArray());
                    switch (tmp.getOriginator().getEntityType()) {
                        case DEVICE:
                            String deviceName = entityData.getEntityName();
                            Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                            if (device != null) {
                                tbMsg = new TbMsg(UUIDs.timeBased(), tmp.getType(), device.getId(), tmp.getMetaData().copy(),
                                        tmp.getDataType(), tmp.getData(), null, null);
                            }
                            break;
                        case ASSET:
                            String assetName = entityData.getEntityName();
                            Asset asset = assetService.findAssetByTenantIdAndName(tenantId, assetName);
                            if (asset != null) {
                                tbMsg = new TbMsg(UUIDs.timeBased(), tmp.getType(), asset.getId(), tmp.getMetaData().copy(),
                                        tmp.getDataType(), tmp.getData(), null, null);
                            }
                            break;
                        case ENTITY_VIEW:
                            String entityViewName = entityData.getEntityName();
                            EntityView entityView = entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName);
                            if (entityView != null) {
                                tbMsg = new TbMsg(UUIDs.timeBased(), tmp.getType(), entityView.getId(), tmp.getMetaData().copy(),
                                        tmp.getDataType(), tmp.getData(), null, null);
                            }
                            break;
                    }

                    if (tbMsg != null) {
                        if (DataConstants.ATTRIBUTES_UPDATED.equals(tbMsg.getType())) {
                            String scope = tbMsg.getMetaData().getValue("scope");
                            Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(tbMsg.getData()));
                            telemetryService.saveAndNotify(tenantId, tbMsg.getOriginator(), scope, new ArrayList<>(attributes), new FutureCallback<Void>() {
                                @Override
                                public void onSuccess(@Nullable Void result) {
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                }
                            });
                        }
                        actorService.onMsg(new SendToClusterMsg(tbMsg.getOriginator(), new ServiceToRuleEngineMsg(tenantId, tbMsg)));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Can't process downlink message [{}]", downlinkMsg, e);
        }
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
