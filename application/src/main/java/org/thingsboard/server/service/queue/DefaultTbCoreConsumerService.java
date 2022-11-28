/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.queue;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.CloudNotificationMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceStateServiceMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.FromDeviceRPCResponseProto;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationDownlinkMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationValidationResponseProto;
import org.thingsboard.server.gen.transport.TransportProtos.LocalSubscriptionServiceMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.RestApiCallResponseMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.SchedulerServiceMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.SubscriptionMgrMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAlarmDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAlarmUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAttributeDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAttributeUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbSubscriptionCloseProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbTimeSeriesDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbTimeSeriesUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.queue.util.TbPackCallback;
import org.thingsboard.server.queue.util.TbPackProcessingContext;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cloud.CloudNotificationService;
import org.thingsboard.server.service.edge.EdgeNotificationService;
import org.thingsboard.server.service.integration.IntegrationManagerService;
import org.thingsboard.server.service.integration.TbCoreIntegrationApiService;
import org.thingsboard.server.service.integration.TbIntegrationDownlinkService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.processing.IdMsgPair;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.service.ruleengine.RuleEngineCallService;
import org.thingsboard.server.service.scheduler.SchedulerService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.sync.vc.GitVersionControlQueueService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultTbCoreConsumerService extends AbstractConsumerService<ToCoreNotificationMsg> implements TbCoreConsumerService {

    @Value("${queue.core.poll-interval}")
    private long pollDuration;
    @Value("${queue.core.pack-processing-timeout}")
    private long packProcessingTimeout;
    @Value("${queue.core.stats.enabled:false}")
    private boolean statsEnabled;

    @Value("${queue.core.ota.pack-interval-ms:60000}")
    private long firmwarePackInterval;
    @Value("${queue.core.ota.pack-size:100}")
    private int firmwarePackSize;

    private final TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> mainConsumer;
    private final DeviceStateService stateService;
    private final SchedulerService schedulerService;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final SubscriptionManagerService subscriptionManagerService;
    private final TbCoreDeviceRpcService tbCoreDeviceRpcService;
    private final TbIntegrationDownlinkService downlinkService;
    private final IntegrationManagerService integrationManagerService;
    private final RuleEngineCallService ruleEngineCallService;
    private final EdgeNotificationService edgeNotificationService;
    private final CloudNotificationService cloudNotificationService;
    private final OtaPackageStateService firmwareStateService;
    private final GitVersionControlQueueService vcQueueService;
    private final TbCoreIntegrationApiService tbCoreIntegrationApiService;
    private final TbCoreConsumerStats stats;
    protected final TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> usageStatsConsumer;
    private final TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> firmwareStatesConsumer;
    private final TbQueueConsumer<TbProtoQueueMsg<ToCoreIntegrationMsg>> integrationApiConsumer;

    protected volatile ExecutorService usageStatsExecutor;
    private volatile ExecutorService firmwareStatesExecutor;
    private volatile ExecutorService integrationApiExecutor;

    public DefaultTbCoreConsumerService(TbCoreQueueFactory tbCoreQueueFactory, ActorSystemContext actorContext,
                                        DeviceStateService stateService, SchedulerService schedulerService, TbLocalSubscriptionService localSubscriptionService,
                                        SubscriptionManagerService subscriptionManagerService, DataDecodingEncodingService encodingService,
                                        TbCoreDeviceRpcService tbCoreDeviceRpcService,
                                        TbIntegrationDownlinkService downlinkService, IntegrationManagerService integrationManagerService,
                                        RuleEngineCallService ruleEngineCallService, StatsFactory statsFactory, TbDeviceProfileCache deviceProfileCache,
                                        TbAssetProfileCache assetProfileCache, TbTenantProfileCache tenantProfileCache, TbApiUsageStateService statsService,
                                        EdgeNotificationService edgeNotificationService,
                                        CloudNotificationService cloudNotificationService,
                                        OtaPackageStateService firmwareStateService,
                                        GitVersionControlQueueService vcQueueService,
                                        TbCoreIntegrationApiService tbCoreIntegrationApiService,
                                        PartitionService partitionService,
                                        Optional<JwtSettingsService> jwtSettingsService) {
        super(actorContext, encodingService, tenantProfileCache, deviceProfileCache, assetProfileCache, statsService, partitionService, tbCoreQueueFactory.createToCoreNotificationsMsgConsumer(), jwtSettingsService);
        this.mainConsumer = tbCoreQueueFactory.createToCoreMsgConsumer();
        this.usageStatsConsumer = tbCoreQueueFactory.createToUsageStatsServiceMsgConsumer();
        this.firmwareStatesConsumer = tbCoreQueueFactory.createToOtaPackageStateServiceMsgConsumer();
        this.integrationApiConsumer = tbCoreQueueFactory.createToCoreIntegrationMsgConsumer();
        this.stateService = stateService;
        this.schedulerService = schedulerService;
        this.localSubscriptionService = localSubscriptionService;
        this.subscriptionManagerService = subscriptionManagerService;
        this.tbCoreDeviceRpcService = tbCoreDeviceRpcService;
        this.downlinkService = downlinkService;
        this.integrationManagerService = integrationManagerService;
        this.ruleEngineCallService = ruleEngineCallService;
        this.edgeNotificationService = edgeNotificationService;
        this.cloudNotificationService = cloudNotificationService;
        this.stats = new TbCoreConsumerStats(statsFactory);
        this.firmwareStateService = firmwareStateService;
        this.vcQueueService = vcQueueService;
        this.tbCoreIntegrationApiService = tbCoreIntegrationApiService;
    }

    @PostConstruct
    public void init() {
        super.init("tb-core-consumer", "tb-core-notifications-consumer");
        this.usageStatsExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-core-usage-stats-consumer"));
        this.firmwareStatesExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-core-firmware-notifications-consumer"));
        this.integrationApiExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-core-integrations-consumer"));
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
        if (usageStatsExecutor != null) {
            usageStatsExecutor.shutdownNow();
        }
        if (firmwareStatesExecutor != null) {
            firmwareStatesExecutor.shutdownNow();
        }
        if (integrationApiExecutor != null) {
            integrationApiExecutor.shutdownNow();
        }
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        super.onApplicationEvent(event);
        launchUsageStatsConsumer();
        launchOtaPackageUpdateNotificationConsumer();
        launchIntegrationApiConsumer();
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (event.getServiceType().equals(getServiceType())) {
            log.info("Subscribing to partitions: {}", event.getPartitions());
            this.mainConsumer.subscribe(event.getPartitions());
            this.integrationApiConsumer.subscribe(
                    event
                            .getPartitions()
                            .stream()
                            .map(tpi -> tpi.newByTopic(integrationApiConsumer.getTopic()))
                            .collect(Collectors.toSet()));
            this.usageStatsConsumer.subscribe(
                    event
                            .getPartitions()
                            .stream()
                            .map(tpi -> tpi.newByTopic(usageStatsConsumer.getTopic()))
                            .collect(Collectors.toSet()));
        }
        this.firmwareStatesConsumer.subscribe();
    }

    @Override
    protected void launchMainConsumers() {
        consumersExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToCoreMsg>> msgs = mainConsumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    List<IdMsgPair<ToCoreMsg>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToCoreMsg>> pendingMap = orderedMsgList.stream().collect(
                            Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    TbPackProcessingContext<TbProtoQueueMsg<ToCoreMsg>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    PendingMsgHolder pendingMsgHolder = new PendingMsgHolder();
                    Future<?> packSubmitFuture = consumersExecutor.submit(() -> {
                        orderedMsgList.forEach((element) -> {
                            UUID id = element.getUuid();
                            TbProtoQueueMsg<ToCoreMsg> msg = element.getMsg();
                            log.trace("[{}] Creating main callback for message: {}", id, msg.getValue());
                            TbCallback callback = new TbPackCallback<>(id, ctx);
                            try {
                                ToCoreMsg toCoreMsg = msg.getValue();
                                pendingMsgHolder.setToCoreMsg(toCoreMsg);
                                if (toCoreMsg.hasToSubscriptionMgrMsg()) {
                                    log.trace("[{}] Forwarding message to subscription manager service {}", id, toCoreMsg.getToSubscriptionMgrMsg());
                                    forwardToSubMgrService(toCoreMsg.getToSubscriptionMgrMsg(), callback);
                                } else if (toCoreMsg.hasToDeviceActorMsg()) {
                                    log.trace("[{}] Forwarding message to device actor {}", id, toCoreMsg.getToDeviceActorMsg());
                                    forwardToDeviceActor(toCoreMsg.getToDeviceActorMsg(), callback);
                                } else if (toCoreMsg.hasDeviceStateServiceMsg()) {
                                    log.trace("[{}] Forwarding message to state service {}", id, toCoreMsg.getDeviceStateServiceMsg());
                                    forwardToStateService(toCoreMsg.getDeviceStateServiceMsg(), callback);
                                } else if (toCoreMsg.hasSchedulerServiceMsg()) {
                                    log.trace("[{}] Forwarding message to scheduler service {}", id, toCoreMsg.getSchedulerServiceMsg());
                                    forwardToSchedulerService(toCoreMsg.getSchedulerServiceMsg(), callback);
                                } else if (toCoreMsg.hasEdgeNotificationMsg()) {
                                    log.trace("[{}] Forwarding message to edge service {}", id, toCoreMsg.getEdgeNotificationMsg());
                                    forwardToEdgeNotificationService(toCoreMsg.getEdgeNotificationMsg(), callback);
                                } else if (toCoreMsg.hasCloudNotificationMsg()) {
                                    log.trace("[{}] Forwarding message to cloud service {}", id, toCoreMsg.getCloudNotificationMsg());
                                    forwardToCloudNotificationService(toCoreMsg.getCloudNotificationMsg(), callback);
                                } else if (toCoreMsg.hasDeviceActivityMsg()) {
                                    log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceActivityMsg());
                                    forwardToStateService(toCoreMsg.getDeviceActivityMsg(), callback);
                                } else if (!toCoreMsg.getToDeviceActorNotificationMsg().isEmpty()) {
                                    Optional<TbActorMsg> actorMsg = encodingService.decode(toCoreMsg.getToDeviceActorNotificationMsg().toByteArray());
                                    if (actorMsg.isPresent()) {
                                        TbActorMsg tbActorMsg = actorMsg.get();
                                        if (tbActorMsg.getMsgType().equals(MsgType.DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG)) {
                                            tbCoreDeviceRpcService.forwardRpcRequestToDeviceActor((ToDeviceRpcRequestActorMsg) tbActorMsg);
                                        } else {
                                            log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg.get());
                                            actorContext.tell(actorMsg.get());
                                        }
                                    }
                                    callback.onSuccess();
                                } else {
                                    log.warn("[{}] No rule how to forward message from main consumer for message: {}", id, toCoreMsg);
                                }
                            } catch (Throwable e) {
                                log.warn("[{}] Failed to process message: {}", id, msg, e);
                                callback.onFailure(e);
                            }
                        });
                    });
                    if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
                        if (!packSubmitFuture.isDone()) {
                            packSubmitFuture.cancel(true);
                            ToCoreMsg lastSubmitMsg = pendingMsgHolder.getToCoreMsg();
                            log.info("Timeout to process message: {}", lastSubmitMsg);
                        }
                        ctx.getAckMap().forEach((id, msg) -> log.debug("[{}] Timeout to process message: {}", id, msg.getValue()));
                        ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
                    }
                    mainConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain messages from queue.", e);
                        try {
                            Thread.sleep(pollDuration);
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                        }
                    }
                }
            }
            log.info("TB Core Consumer stopped.");
        });
    }

    private static class PendingMsgHolder {
        @Getter
        @Setter
        private volatile ToCoreMsg toCoreMsg;
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollDuration;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToCoreNotificationMsg> msg, TbCallback callback) {
        ToCoreNotificationMsg toCoreNotification = msg.getValue();
        if (toCoreNotification.hasToLocalSubscriptionServiceMsg()) {
            log.trace("[{}] Forwarding message to local subscription service {}", id, toCoreNotification.getToLocalSubscriptionServiceMsg());
            forwardToLocalSubMgrService(toCoreNotification.getToLocalSubscriptionServiceMsg(), callback);
        } else if (toCoreNotification.hasFromDeviceRpcResponse()) {
            log.trace("[{}] Forwarding message to RPC service {}", id, toCoreNotification.getFromDeviceRpcResponse());
            forwardToCoreRpcService(toCoreNotification.getFromDeviceRpcResponse(), callback);
        } else if (toCoreNotification.hasIntegrationDownlinkMsg()) {
            log.trace("[{}] Forwarding message to Integration service {}", id, toCoreNotification.getIntegrationDownlinkMsg());
            forwardToDownlinkService(toCoreNotification.getIntegrationDownlinkMsg(), callback);
        } else if (toCoreNotification.hasIntegrationValidationResponseMsg()) {
            log.trace("[{}] Forwarding message to Integration service {}", id, toCoreNotification.getIntegrationValidationResponseMsg());
            forwardToIntegrationManagerService(toCoreNotification.getIntegrationValidationResponseMsg(), callback);
        } else if (toCoreNotification.hasRestApiCallResponseMsg()) {
            log.trace("[{}] Forwarding message to RuleEngineCallService service {}", id, toCoreNotification.getRestApiCallResponseMsg());
            forwardToRuleEngineCallService(toCoreNotification.getRestApiCallResponseMsg(), callback);
        } else if (toCoreNotification.getComponentLifecycleMsg() != null && !toCoreNotification.getComponentLifecycleMsg().isEmpty()) {
            handleComponentLifecycleMsg(id, toCoreNotification.getComponentLifecycleMsg());
            callback.onSuccess();
        } else if (!toCoreNotification.getEdgeEventUpdateMsg().isEmpty()) {
            forwardToAppActor(id, encodingService.decode(toCoreNotification.getEdgeEventUpdateMsg().toByteArray()), callback);
        } else if (!toCoreNotification.getToEdgeSyncRequestMsg().isEmpty()) {
            forwardToAppActor(id, encodingService.decode(toCoreNotification.getToEdgeSyncRequestMsg().toByteArray()), callback);
        } else if (!toCoreNotification.getFromEdgeSyncResponseMsg().isEmpty()) {
            forwardToAppActor(id, encodingService.decode(toCoreNotification.getFromEdgeSyncResponseMsg().toByteArray()), callback);
        } else if (toCoreNotification.hasQueueUpdateMsg()) {
            TransportProtos.QueueUpdateMsg queue = toCoreNotification.getQueueUpdateMsg();
            partitionService.updateQueue(queue);
            callback.onSuccess();
        } else if (toCoreNotification.hasQueueDeleteMsg()) {
            TransportProtos.QueueDeleteMsg queue = toCoreNotification.getQueueDeleteMsg();
            partitionService.removeQueue(queue);
            callback.onSuccess();
        } else if (toCoreNotification.hasVcResponseMsg()) {
            vcQueueService.processResponse(toCoreNotification.getVcResponseMsg());
            callback.onSuccess();
        }
        if (statsEnabled) {
            stats.logToCoreNotification();
        }
    }

    private void launchUsageStatsConsumer() {
        usageStatsExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToUsageStatsServiceMsg>> msgs = usageStatsConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToUsageStatsServiceMsg>> pendingMap = msgs.stream().collect(
                            Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    TbPackProcessingContext<TbProtoQueueMsg<ToUsageStatsServiceMsg>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    pendingMap.forEach((id, msg) -> {
                        log.trace("[{}] Creating usage stats callback for message: {}", id, msg.getValue());
                        TbCallback callback = new TbPackCallback<>(id, ctx);
                        try {
                            handleUsageStats(msg, callback);
                        } catch (Throwable e) {
                            log.warn("[{}] Failed to process usage stats: {}", id, msg, e);
                            callback.onFailure(e);
                        }
                    });
                    if (!processingTimeoutLatch.await(getNotificationPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
                        ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process usage stats: {}", id, msg.getValue()));
                        ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process usage stats: {}", id, msg.getValue()));
                    }
                    usageStatsConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain usage stats from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new usage stats", e2);
                        }
                    }
                }
            }
            log.info("TB Usage Stats Consumer stopped.");
        });
    }

    private void launchOtaPackageUpdateNotificationConsumer() {
        long maxProcessingTimeoutPerRecord = firmwarePackInterval / firmwarePackSize;
        firmwareStatesExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> msgs = firmwareStatesConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    long timeToSleep = maxProcessingTimeoutPerRecord;
                    for (TbProtoQueueMsg<ToOtaPackageStateServiceMsg> msg : msgs) {
                        try {
                            long startTime = System.currentTimeMillis();
                            boolean isSuccessUpdate = handleOtaPackageUpdates(msg);
                            long endTime = System.currentTimeMillis();
                            long spentTime = endTime - startTime;
                            timeToSleep = timeToSleep - spentTime;
                            if (isSuccessUpdate) {
                                if (timeToSleep > 0) {
                                    log.debug("Spent time per record is: [{}]!", spentTime);
                                    Thread.sleep(timeToSleep);
                                    timeToSleep = 0;
                                }
                                timeToSleep += maxProcessingTimeoutPerRecord;
                            }
                        } catch (Throwable e) {
                            log.warn("Failed to process firmware update msg: {}", msg, e);
                        }
                    }
                    firmwareStatesConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain usage stats from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new firmware updates", e2);
                        }
                    }
                }
            }
            log.info("TB Ota Package States Consumer stopped.");
        });
    }

    private void launchIntegrationApiConsumer() {
        integrationApiExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToCoreIntegrationMsg>> msgs = integrationApiConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    for (TbProtoQueueMsg<ToCoreIntegrationMsg> msg : msgs) {
                        try {
                            // TODO: ashvayka: improve the retry strategy.
                            tbCoreIntegrationApiService.handle(msg, TbCallback.EMPTY);
                        } catch (Throwable e) {
                            log.warn("Failed to process integration msg: {}", msg, e);
                        }
                    }
                    integrationApiConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain usage stats from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new usage stats", e2);
                        }
                    }
                }
            }
            log.info("TB Usage Stats Consumer stopped.");
        });
    }

    private void handleUsageStats(TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, TbCallback callback) {
        apiUsageStateService.process(msg, callback);
    }

    private boolean handleOtaPackageUpdates(TbProtoQueueMsg<ToOtaPackageStateServiceMsg> msg) {
        return firmwareStateService.process(msg.getValue());
    }

    private void forwardToCoreRpcService(FromDeviceRPCResponseProto proto, TbCallback callback) {
        RpcError error = proto.getError() > 0 ? RpcError.values()[proto.getError()] : null;
        FromDeviceRpcResponse response = new FromDeviceRpcResponse(new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
                , proto.getResponse(), error);
        tbCoreDeviceRpcService.processRpcResponseFromRuleEngine(response);
        callback.onSuccess();
    }

    @Scheduled(fixedDelayString = "${queue.core.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
            stats.reset();
        }
    }

    private void forwardToLocalSubMgrService(LocalSubscriptionServiceMsgProto msg, TbCallback callback) {
        if (msg.hasSubUpdate()) {
            localSubscriptionService.onSubscriptionUpdate(msg.getSubUpdate().getSessionId(), TbSubscriptionUtils.fromProto(msg.getSubUpdate()), callback);
        } else if (msg.hasAlarmSubUpdate()) {
            localSubscriptionService.onSubscriptionUpdate(msg.getAlarmSubUpdate().getSessionId(), TbSubscriptionUtils.fromProto(msg.getAlarmSubUpdate()), callback);
        } else {
            throwNotHandled(msg, callback);
        }
    }

    private void forwardToSubMgrService(SubscriptionMgrMsgProto msg, TbCallback callback) {
        if (msg.hasAttributeSub()) {
            subscriptionManagerService.addSubscription(TbSubscriptionUtils.fromProto(msg.getAttributeSub()), callback);
        } else if (msg.hasTelemetrySub()) {
            subscriptionManagerService.addSubscription(TbSubscriptionUtils.fromProto(msg.getTelemetrySub()), callback);
        } else if (msg.hasAlarmSub()) {
            subscriptionManagerService.addSubscription(TbSubscriptionUtils.fromProto(msg.getAlarmSub()), callback);
        } else if (msg.hasSubClose()) {
            TbSubscriptionCloseProto closeProto = msg.getSubClose();
            subscriptionManagerService.cancelSubscription(closeProto.getSessionId(), closeProto.getSubscriptionId(), callback);
        } else if (msg.hasTsUpdate()) {
            TbTimeSeriesUpdateProto proto = msg.getTsUpdate();
            subscriptionManagerService.onTimeSeriesUpdate(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    KvProtoUtil.toTsKvEntityList(proto.getDataList()), callback);
        } else if (msg.hasAttrUpdate()) {
            TbAttributeUpdateProto proto = msg.getAttrUpdate();
            subscriptionManagerService.onAttributesUpdate(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getScope(), KvProtoUtil.toAttributeKvList(proto.getDataList()), callback);
        } else if (msg.hasAttrDelete()) {
            TbAttributeDeleteProto proto = msg.getAttrDelete();
            subscriptionManagerService.onAttributesDelete(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getScope(), proto.getKeysList(), proto.getNotifyDevice(), callback);
        } else if (msg.hasTsDelete()) {
            TbTimeSeriesDeleteProto proto = msg.getTsDelete();
            subscriptionManagerService.onTimeSeriesDelete(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getKeysList(), callback);
        } else if (msg.hasAlarmUpdate()) {
            TbAlarmUpdateProto proto = msg.getAlarmUpdate();
            subscriptionManagerService.onAlarmUpdate(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    JacksonUtil.fromString(proto.getAlarm(), Alarm.class), callback);
        } else if (msg.hasAlarmDelete()) {
            TbAlarmDeleteProto proto = msg.getAlarmDelete();
            subscriptionManagerService.onAlarmDeleted(
                    TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    JacksonUtil.fromString(proto.getAlarm(), Alarm.class), callback);
        } else {
            throwNotHandled(msg, callback);
        }
        if (statsEnabled) {
            stats.log(msg);
        }
    }

    private void forwardToStateService(DeviceStateServiceMsgProto deviceStateServiceMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceStateServiceMsg);
        }
        stateService.onQueueMsg(deviceStateServiceMsg, callback);
    }

    private void forwardToStateService(TransportProtos.DeviceActivityProto deviceActivityMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceActivityMsg);
        }
        TenantId tenantId = TenantId.fromUUID(new UUID(deviceActivityMsg.getTenantIdMSB(), deviceActivityMsg.getTenantIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(deviceActivityMsg.getDeviceIdMSB(), deviceActivityMsg.getDeviceIdLSB()));
        try {
            stateService.onDeviceActivity(tenantId, deviceId, deviceActivityMsg.getLastActivityTime());
            callback.onSuccess();
        } catch (Exception e) {
            callback.onFailure(new RuntimeException("Failed update device activity for device [" + deviceId.getId() + "]!", e));
        }
    }

    private void forwardToSchedulerService(SchedulerServiceMsgProto schedulerServiceMsg, TbCallback callback) {
       if (statsEnabled) {
            stats.log(schedulerServiceMsg);
        }
        schedulerService.onQueueMsg(schedulerServiceMsg, callback);
    }

    private void forwardToDownlinkService(IntegrationDownlinkMsgProto integrationDownlinkMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.logToCoreNotification();
        }
        downlinkService.onDownlinkToRemoteIntegrationMsg(integrationDownlinkMsg, callback);
    }

    private void forwardToIntegrationManagerService(IntegrationValidationResponseProto integrationDownlinkMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.logToCoreNotification();
        }
        integrationManagerService.handleValidationResponse(integrationDownlinkMsg, callback);
    }


    private void forwardToRuleEngineCallService(RestApiCallResponseMsgProto restApiCallResponseMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.logToCoreNotification();
        }
        ruleEngineCallService.onQueueMsg(restApiCallResponseMsg, callback);
    }

    private void forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg toDeviceActorMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(toDeviceActorMsg);
        }
        actorContext.tell(new TransportToDeviceActorMsgWrapper(toDeviceActorMsg, callback) );
    }

    private void forwardToEdgeNotificationService(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(edgeNotificationMsg);
        }
        edgeNotificationService.pushNotificationToEdge(edgeNotificationMsg, callback);
    }

    private void forwardToCloudNotificationService(CloudNotificationMsgProto cloudNotificationMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(cloudNotificationMsg);
        }
        cloudNotificationService.pushNotificationToCloud(cloudNotificationMsg, callback);
    }

    private void forwardToAppActor(UUID id, Optional<TbActorMsg> actorMsg, TbCallback callback) {
        if (actorMsg.isPresent()) {
            log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg.get());
            actorContext.tell(actorMsg.get());
        }
        callback.onSuccess();
    }

    private void throwNotHandled(Object msg, TbCallback callback) {
        log.warn("Message not handled: {}", msg);
        callback.onFailure(new RuntimeException("Message not handled!"));
    }

    @Override
    protected void stopMainConsumers() {
        if (mainConsumer != null) {
            mainConsumer.unsubscribe();
        }
        if (usageStatsConsumer != null) {
            usageStatsConsumer.unsubscribe();
        }
        if (firmwareStatesConsumer != null) {
            firmwareStatesConsumer.unsubscribe();
        }
        if (integrationApiConsumer != null) {
            integrationApiConsumer.unsubscribe();
        }
    }

}
