/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.rpc.api.RpcCallback;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityType;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.RuleNodeProto;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
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

@Service
@Slf4j
public class CloudManagerService {

    private static final ObjectMapper mapper = new ObjectMapper();

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
        processHandleMessages();
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
        if (msg.getSuccess()) {
            log.debug("[{}] Msg has been processed successfully! {}", routingKey, msg);
        } else {
            log.error("[{}] Msg processing failed! Error msg: {}", routingKey, msg.getErrorMsg());
        }
        latch.countDown();
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        initialized = true;
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
            }
        } catch (Exception e) {
            log.error("Can't process entity updated msg", e);
        }
    }

    private void onDeviceUpdate(DeviceUpdateMsg deviceUpdateMsg) {
        log.info("onDeviceUpdate {}", deviceUpdateMsg);
        Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceUpdateMsg.getName());
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                if (device == null) {
                    device = new Device();
                    device.setTenantId(tenantId);
                    device.setName(deviceUpdateMsg.getName());
                    device.setType(deviceUpdateMsg.getType());
                    device = deviceService.saveDevice(device);
                    deviceStateService.onDeviceAdded(device);
                } else {
                    device.setType(deviceUpdateMsg.getType());
                    deviceService.saveDevice(device);
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

    private void onAssetUpdate(AssetUpdateMsg assetUpdateMsg) {
        log.info("onAssetUpdate {}", assetUpdateMsg);
        Asset asset = assetService.findAssetByTenantIdAndName(tenantId, assetUpdateMsg.getName());
        switch (assetUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                if (asset == null) {
                    asset = new Asset();
                    asset.setTenantId(tenantId);
                    asset.setName(assetUpdateMsg.getName());
                    asset.setType(assetUpdateMsg.getType());
                    assetService.saveAsset(asset);
                } else {
                    asset.setType(assetUpdateMsg.getType());
                    assetService.saveAsset(asset);
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
        if (entityViewUpdateMsg.getRelatedEntityType().equals(EntityType.DEVICE)) {
            Device device = deviceService.findDeviceByTenantIdAndName(tenantId, entityName);
            if (device == null) {
                throw new RuntimeException("Related device [" + entityName + "] doesn't exist! Can't create entityView [" + entityViewUpdateMsg + "]");
            }
            return device.getId();
        } else if (entityViewUpdateMsg.getRelatedEntityType().equals(EntityType.ASSET)) {
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
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                dashboardService.deleteDashboard(tenantId, dashboardId);
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void onRuleChainUpdate(RuleChainUpdateMsg ruleChainUpdateMsg) {
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
        EntityId originatorId = getAlarmOriginator(alarmUpdateMsg.getOriginatorName(), org.thingsboard.server.common.data.EntityType.valueOf(alarmUpdateMsg.getOriginatorType()));
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
        UserId userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
        switch (userUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                boolean created = false;
                User user = userService.findUserById(tenantId, userId);
                if (user == null) {
                    created = true;
                    user = new User();
                    user.setId(userId);
                    user.setTenantId(tenantId);
                    CustomerId customerId = new CustomerId(new UUID(userUpdateMsg.getCustomerIdMSB(), userUpdateMsg.getCustomerIdLSB()));
                    user.setCustomerId(customerId);
                }
                user.setEmail(userUpdateMsg.getEmail());
                user.setAuthority(Authority.parse(userUpdateMsg.getAuthority()));
                user.setFirstName(userUpdateMsg.getFirstName());
                user.setLastName(userUpdateMsg.getLastName());
                user.setAdditionalInfo(JacksonUtil.toJsonNode(userUpdateMsg.getAdditionalInfo()));
                User savedUser = userService.saveUser(user);

                if (created) {
                    entityGroupService.addEntityToEntityGroupAll(savedUser.getTenantId(), savedUser.getOwnerId(), savedUser.getId());
                }

                UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
                if (created) {
                    userCredentials = new UserCredentials();
                    userCredentials.setUserId(savedUser.getId());
                }
                userCredentials.setEnabled(userUpdateMsg.getEnabled());
                userCredentials.setPassword(userUpdateMsg.getPassword());
                userService.saveUserCredentials(tenantId, userCredentials);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                userService.deleteUser(tenantId, userId);
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private EntityId getAlarmOriginator(String entityName, org.thingsboard.server.common.data.EntityType entityType) {
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
                                    tmp.getDataType(), tmp.getData(), null, null, 0L);
                        }
                        break;
                    case ASSET:
                        String assetName = entityData.getEntityName();
                        Asset asset = assetService.findAssetByTenantIdAndName(tenantId, assetName);
                        if (asset != null) {
                            tbMsg = new TbMsg(UUIDs.timeBased(), tmp.getType(), asset.getId(), tmp.getMetaData().copy(),
                                    tmp.getDataType(), tmp.getData(), null, null, 0L);
                        }
                        break;
                    case ENTITY_VIEW:
                        String entityViewName = entityData.getEntityName();
                        EntityView entityView = entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName);
                        if (entityView != null) {
                            tbMsg = new TbMsg(UUIDs.timeBased(), tmp.getType(), entityView.getId(), tmp.getMetaData().copy(),
                                    tmp.getDataType(), tmp.getData(), null, null, 0L);
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
