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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.cloud.processor.AlarmUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.AssetUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.CustomerUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.DashboardUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.DeviceUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.EntityViewUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.RelationUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.RuleChainUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.SchedulerEventUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.UserUpdateProcessor;
import org.thingsboard.server.service.cloud.processor.WhiteLabelingUpdateProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.user.UserLoaderService;
import org.thingsboard.storage.EventStorage;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CloudManagerService {

    private final Gson gson = new Gson();

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
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

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
    private SchedulerEventUpdateProcessor schedulerEventUpdateProcessor;

    @Autowired
    private WhiteLabelingUpdateProcessor whiteLabelingUpdateProcessor;

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
                deviceUpdateProcessor.onDeviceUpdate(tenantId, entityUpdateMsg.getDeviceUpdateMsg());
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
