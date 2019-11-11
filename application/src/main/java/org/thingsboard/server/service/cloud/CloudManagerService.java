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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityType;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.RuleNodeProto;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.storage.EventStorage;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
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
    private RuleChainService ruleChainService;

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
                this::onDeviceUpdate,
                this::onAssetUpdate,
                this::onEntityViewUpdate,
                this::onRuleChainUpdate,
                this::onRuleChainMetadataUpdate,
                this::onDashboardUpdate,
                this::onDownlink,
                this::scheduleReconnect);
        executor = Executors.newSingleThreadExecutor();
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        setTenantId();
        processHandleMessages();
    }

    private void setTenantId() {
        TextPageData<Tenant> tenants = tenantService.findTenants(new TextPageLink(1));
        if (tenants.getData() != null && !tenants.getData().isEmpty()) {
            tenantId = tenants.getData().get(0).getId();
        }
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
//                if (ruleChainUpdateMsg.getFirstRuleNodeIdMSB() != 0 && ruleChainUpdateMsg.getFirstRuleNodeIdLSB() != 0) {
//                    originalRuleChain.setFirstRuleNodeId(new RuleNodeId(new UUID(ruleChainUpdateMsg.getFirstRuleNodeIdMSB(), ruleChainUpdateMsg.getFirstRuleNodeIdLSB())));
//                }
                ruleChain.setConfiguration(JacksonUtil.toJsonNode(ruleChainUpdateMsg.getConfiguration()));
                ruleChain.setRoot(ruleChainUpdateMsg.getRoot());
                ruleChain.setDebugMode(ruleChainUpdateMsg.getDebugMode());
                ruleChainService.saveRuleChain(ruleChain);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                ruleChainService.deleteRuleChainById(tenantId, ruleChainId);
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void onRuleChainMetadataUpdate(RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        try {
            switch (ruleChainMetadataUpdateMsg.getMsgType()) {
                case ENTITY_UPDATED_RPC_MESSAGE:
                    RuleChainMetaData ruleChainMetadata = new RuleChainMetaData();
                    RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainMetadataUpdateMsg.getRuleChainIdMSB(), ruleChainMetadataUpdateMsg.getRuleChainIdLSB()));
                    ruleChainMetadata.setRuleChainId(ruleChainId);
                    ruleChainMetadata.setFirstNodeIndex(ruleChainMetadataUpdateMsg.getFirstNodeIndex());
                    ruleChainMetadata.setNodes(parseNodeProtos(ruleChainId, ruleChainMetadataUpdateMsg.getNodesList()));
                    ruleChainMetadata.setConnections(parseConnectionProtos(ruleChainMetadataUpdateMsg.getConnectionsList()));
                    ruleChainMetadata.setRuleChainConnections(parseRuleChainConnectionProtos(ruleChainMetadataUpdateMsg.getRuleChainConnectionsList()));
                    ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetadata);
                    break;
                case UNRECOGNIZED:
                    log.error("Unsupported msg type");
            }
        } catch (IOException e) {
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

    private void onDownlink(DownlinkMsg downlinkMsg) {
        log.info("onDownlink {}", downlinkMsg);
    }

    private void scheduleReconnect(Exception e) {
        initialized = false;
        if (scheduledFuture == null) {
            scheduledFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
                log.info("Trying to reconnect due to the error: {}!", e.getMessage());
                edgeRpcClient.connect(routingKey, routingSecret,
                        this::onUplinkResponse,
                        this::onEdgeUpdate,
                        this::onDeviceUpdate,
                        this::onAssetUpdate,
                        this::onEntityViewUpdate,
                        this::onRuleChainUpdate,
                        this::onRuleChainMetadataUpdate,
                        this::onDashboardUpdate,
                        this::onDownlink,
                        this::scheduleReconnect);
            }, 0, reconnectTimeoutMs, TimeUnit.MILLISECONDS);
        }
    }
}
