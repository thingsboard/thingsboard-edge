/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud.rpc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.edge.stats.CloudStatsCounterService;
import org.thingsboard.server.dao.edge.stats.CloudStatsKey;
import org.thingsboard.server.dao.eventsourcing.GrpcConnectionEstablishedEvent;
import org.thingsboard.server.dao.eventsourcing.InterruptSendUplinkEvent;
import org.thingsboard.server.dao.eventsourcing.StopCloudEventProcessingEvent;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.service.cloud.DownlinkMessageService;
import org.thingsboard.server.service.cloud.DownlinkMsgProcessedCallback;
import org.thingsboard.server.service.cloud.config.EdgeConfigurationHandler;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.cloud.info.PendingUplinkMsgPackHolder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.service.edge.rpc.EdgeGrpcSession.RATE_LIMIT_REACHED;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseGrpcClientManager extends TbApplicationEventListener<PartitionChangeEvent> implements GrpcClientManager {

    private final EdgeRpcClient edgeRpcClient;
    private final EdgeInfoHolder edgeInfo;
    private final PendingUplinkMsgPackHolder pendingMsgs;
    private final CloudStatsCounterService statsCounterService;
    private final PartitionService partitionService;
    private final ApplicationEventPublisher eventPublisher;
    private final ConnectionStatusManager connectionStatusManager;
    private final ConfigurableApplicationContext context;
    private final EdgeConfigurationHandler edgeConfigurationHandler;
    private final DownlinkMessageService downlinkMessageService;

    private ScheduledExecutorService shutdownExecutor;
    private ScheduledExecutorService connectExecutor;
    private ScheduledExecutorService reconnectExecutor;
    private ScheduledFuture<?> connectFuture;
    private ScheduledFuture<?> reconnectFuture;

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (ServiceType.TB_CORE.equals(event.getServiceType())) {
            establishRpcConnection();
        }
    }

    @PreDestroy
    private void destroy() {
        edgeInfo.setInitInProgress(false);
        edgeInfo.setInitialized(false);

        if (shutdownExecutor != null) {
            shutdownExecutor.shutdownNow();
        }

        connectionStatusManager.updateConnectivityStatus(false);

        EdgeSettings currentEdgeSettings = edgeInfo.getSettings();
        String edgeId = currentEdgeSettings != null ? currentEdgeSettings.getEdgeId() : "";
        log.info("[{}] Starting destroying process", edgeId);
        try {
            edgeRpcClient.disconnect(false);
        } catch (Exception e) {
            log.error("Exception during disconnect", e);
        }

        if (reconnectExecutor != null) {
            reconnectExecutor.shutdownNow();
            reconnectExecutor = null;
        }
        log.info("[{}] Destroy was successful", edgeId);
    }

    @Override
    public void sendUplinkMsg(UplinkMsg msg) {
        double serverMaxInboundMessageSize = edgeRpcClient.getServerMaxInboundMessageSize();

        if (serverMaxInboundMessageSize == 0 || msg.getSerializedSize() <= serverMaxInboundMessageSize) {
            edgeRpcClient.sendUplinkMsg(msg);
        } else {
            log.error("Uplink msg size [{}] exceeds server max inbound message size [{}]. Skipping this message. " +
                            "Please increase value of EDGES_RPC_MAX_INBOUND_MESSAGE_SIZE env variable on the server and restart it. Message {}",
                    msg.getSerializedSize(), serverMaxInboundMessageSize, msg);
            statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_PERMANENTLY_FAILED, edgeInfo.getTenantId(), 1);
            pendingMsgs.markAsProcessed(msg.getUplinkMsgId());
        }
    }

    @Override
    public void establishRpcConnection() {
        if (connectFuture != null) {
            connectFuture.cancel(true);
            connectFuture = null;
        }
        if (connectExecutor == null) {
            connectExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-connect"));
        }
        connectFuture = connectExecutor.schedule(() -> {
            try {
                synchronized (this) {
                    if (!isSystemTenantPartitionMine()) {
                        onDestroy();
                        return;
                    }
                    if (!edgeInfo.isInitialized() && !edgeInfo.isInitInProgress() && validateRoutingKeyAndSecret()) {
                        connectToServerAndLaunchEventsProcessing();
                    }
                }
            } catch (Exception e) {
                log.error("Failed to establish Cloud Edge service", e);
            }
        }, edgeInfo.getReconnectTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    private void connectToServerAndLaunchEventsProcessing() {
        edgeInfo.setInitInProgress(true);
        try {
            log.info("Starting Cloud Edge service");
            edgeRpcClient.connect(edgeInfo.getRoutingKey(), edgeInfo.getRoutingSecret(),
                    this::onUplinkResponse,
                    this::onEdgeUpdate,
                    this::onDownlink,
                    this::scheduleReconnect);
            launchCloudEventsProcessing();
        } catch (Exception e) {
            edgeInfo.setInitInProgress(false);
            log.error("Failed to establish connection to cloud", e);
            connectExecutor.schedule(this::establishRpcConnection, edgeInfo.getReconnectTimeoutMs(), TimeUnit.MILLISECONDS);
        }
    }

    private void scheduleReconnect(Exception e) {
        edgeInfo.setInitialized(false);

        connectionStatusManager.updateConnectivityStatus(false);

        if (reconnectFuture == null) {
            reconnectFuture = reconnectExecutor.scheduleAtFixedRate(() -> {
                log.info("Trying to reconnect due to the error: {}!", e.getMessage());
                try {
                    edgeRpcClient.disconnect(true);
                } catch (Exception ex) {
                    log.error("Exception during disconnect: {}", ex.getMessage());
                }
                try {
                    edgeRpcClient.connect(edgeInfo.getRoutingKey(), edgeInfo.getRoutingSecret(),
                            this::onUplinkResponse,
                            this::onEdgeUpdate,
                            this::onDownlink,
                            this::scheduleReconnect);
                } catch (Exception ex) {
                    log.error("Exception during connect: {}", ex.getMessage());
                }
            }, edgeInfo.getReconnectTimeoutMs(), edgeInfo.getReconnectTimeoutMs(), TimeUnit.MILLISECONDS);
        }
    }

    private void onDownlink(DownlinkMsg downlinkMsg) {
        boolean edgeCustomerIdUpdated = updateCustomerIdIfRequired(downlinkMsg);
        if (edgeInfo.isSyncInProgress() && downlinkMsg.hasSyncCompletedMsg()) {
            log.trace("[{}] downlinkMsg hasSyncCompletedMsg = true", downlinkMsg);
            edgeInfo.setSyncInProgress(false);
        }
        Futures.addCallback(
                downlinkMessageService.processDownlinkMsg(edgeInfo.getTenantId(), edgeInfo.getCustomerId(), downlinkMsg, edgeInfo.getSettings()),
                new DownlinkMsgProcessedCallback(edgeRpcClient, edgeInfo, downlinkMsg, edgeCustomerIdUpdated),
                MoreExecutors.directExecutor());
    }

    private boolean updateCustomerIdIfRequired(DownlinkMsg downlinkMsg) {
        if (downlinkMsg.hasEdgeConfiguration()) {
            return edgeConfigurationHandler.setOrUpdateCustomerId(downlinkMsg.getEdgeConfiguration());
        } else {
            return false;
        }
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        try {
            eventPublisher.publishEvent(InterruptSendUplinkEvent.INSTANCE);
            if (reconnectFuture != null) {
                reconnectFuture.cancel(true);
                reconnectFuture = null;
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
        edgeInfo.setInitInProgress(false);
    }

    private void initAndUpdateEdgeSettings(EdgeConfiguration edgeConfiguration) throws Exception {
        EdgeSettings edgeSettings = edgeConfigurationHandler.initAndUpdateEdgeSettings(edgeConfiguration);
        if (edgeSettings == null) {
            return;
        }
        if (edgeInfo.isPerformInitialSyncRequired()) {
            log.trace("Sending sync request, fullSyncRequired {}", edgeSettings.isFullSyncRequired());
            requestSyncToCloud(edgeSettings.isFullSyncRequired());
            edgeInfo.setPerformInitialSyncRequired(false);
        }
        edgeInfo.setInitialized(true);
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        try {
            if (edgeInfo.isSendingInProgress()) {
                if (msg.getSuccess()) {
                    statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_PUSHED, edgeInfo.getTenantId(), 1);
                    pendingMsgs.markAsProcessed(msg.getUplinkMsgId());
                    log.debug("uplink msg has been processed successfully! {}", msg);
                } else {
                    statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_TMP_FAILED, edgeInfo.getTenantId(), 1);
                    pendingMsgs.countDown();
                    if (msg.getErrorMsg().contains(RATE_LIMIT_REACHED)) {
                        log.warn("uplink msg processing failed! {}", RATE_LIMIT_REACHED);
                        edgeInfo.setRateLimitViolated(true);
                    } else {
                        log.error("uplink msg processing failed! Error msg: {}", msg.getErrorMsg());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Can't process uplink msg response [{}]", msg, e);
        }
    }

    private void launchCloudEventsProcessing() {
        eventPublisher.publishEvent(GrpcConnectionEstablishedEvent.INSTANCE);
        reconnectExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-reconnect"));
    }

    private void onDestroy() {
        eventPublisher.publishEvent(StopCloudEventProcessingEvent.INSTANCE);
        destroy();
    }

    private boolean validateRoutingKeyAndSecret() {
        if (StringUtils.isBlank(edgeInfo.getRoutingKey()) || StringUtils.isBlank(edgeInfo.getRoutingSecret())) {
            shutdownExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cloud-manager-shutdown")); // todo: move to separate init (& destroy?)
            shutdownExecutor.scheduleAtFixedRate(() -> log.error(
                    "Routing Key and Routing Secret must be provided! " +
                            "Please configure Routing Key and Routing Secret in the tb-edge.yml file " +
                            "or add CLOUD_ROUTING_KEY and CLOUD_ROUTING_SECRET variable to the tb-edge.conf file. " +
                            "ThingsBoard Edge is not going to connect to cloud!"), 0, 10, TimeUnit.SECONDS);
            return false;
        }
        return true;
    }

    private void requestSyncToCloud(boolean fullSyncRequired) {
        edgeRpcClient.sendSyncRequestMsg(fullSyncRequired);
        edgeInfo.setSyncInProgress(true);
    }

    private boolean isSystemTenantPartitionMine() {
        return partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
    }
}
