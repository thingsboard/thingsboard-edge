/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.RateLimitsTrigger;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.transport.DeviceDeletedEvent;
import org.thingsboard.server.common.transport.DeviceProfileUpdatedEvent;
import org.thingsboard.server.common.transport.DeviceUpdatedEvent;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportResourceCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.limits.EntityLimitKey;
import org.thingsboard.server.common.transport.limits.EntityLimitsCache;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.AsyncCallbackTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbTransportQueueFactory;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbTransportComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 17.10.18.
 */
@Slf4j
@Service
@TbTransportComponent
public class DefaultTransportService extends TransportActivityManager implements TransportService {

    public static final String OVERWRITE_ACTIVITY_TIME = "overwriteActivityTime";
    public static final TransportProtos.SessionEventMsg SESSION_EVENT_MSG_OPEN = TransportProtos.SessionEventMsg.newBuilder()
            .setSessionType(TransportProtos.SessionType.ASYNC)
            .setEvent(TransportProtos.SessionEvent.OPEN).build();
    public static final TransportProtos.SubscribeToAttributeUpdatesMsg SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG = TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder()
            .setSessionType(TransportProtos.SessionType.ASYNC).build();
    public static final TransportProtos.SubscribeToRPCMsg SUBSCRIBE_TO_RPC_ASYNC_MSG = TransportProtos.SubscribeToRPCMsg.newBuilder()
            .setSessionType(TransportProtos.SessionType.ASYNC).build();

    private final AtomicInteger atomicTs = new AtomicInteger(0);

    @Value("${transport.log.enabled:true}")
    private boolean logEnabled;
    @Value("${transport.log.max_length:1024}")
    private int logMaxLength;
    @Value("${transport.client_side_rpc.timeout:60000}")
    private long clientSideRpcTimeout;
    @Value("${queue.transport.poll_interval}")
    private int notificationsPollDuration;
    @Value("${transport.stats.enabled:false}")
    private boolean statsEnabled;


    @Autowired
    @Lazy
    private TbApiUsageReportClient apiUsageClient;
    private final Map<String, Number> statsMap = new LinkedHashMap<>();

    private final Gson gson = new Gson();
    private final PartitionService partitionService;
    private final TbTransportQueueFactory queueProvider;
    private final TbQueueProducerProvider producerProvider;

    private final TopicService topicService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final StatsFactory statsFactory;
    private final TransportDeviceProfileCache deviceProfileCache;
    private final TransportTenantProfileCache tenantProfileCache;

    private final TransportRateLimitService rateLimitService;
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransportResourceCache transportResourceCache;
    private final NotificationRuleProcessor notificationRuleProcessor;
    private final EntityLimitsCache entityLimitsCache;

    protected TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> transportApiRequestTemplate;
    protected TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> ruleEngineMsgProducer;
    protected TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> tbCoreMsgProducer;
    protected TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> transportNotificationsConsumer;

    protected MessagesStats ruleEngineProducerStats;
    protected MessagesStats tbCoreProducerStats;
    protected MessagesStats transportApiStats;

    protected ExecutorService transportCallbackExecutor;
    private ExecutorService mainConsumerExecutor;

    private final Map<String, RpcRequestMetadata> toServerRpcPendingMap = new ConcurrentHashMap<>();

    private volatile boolean stopped = false;

    public DefaultTransportService(PartitionService partitionService,
                                   TbServiceInfoProvider serviceInfoProvider,
                                   TbTransportQueueFactory queueProvider,
                                   TbQueueProducerProvider producerProvider,
                                   TopicService topicService,
                                   StatsFactory statsFactory,
                                   TransportDeviceProfileCache deviceProfileCache,
                                   TransportTenantProfileCache tenantProfileCache,
                                   TransportRateLimitService rateLimitService,
                                   DataDecodingEncodingService dataDecodingEncodingService, SchedulerComponent scheduler, TransportResourceCache transportResourceCache,
                                   ApplicationEventPublisher eventPublisher, NotificationRuleProcessor notificationRuleProcessor,
                                   EntityLimitsCache entityLimitsCache) {
        this.partitionService = partitionService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.queueProvider = queueProvider;
        this.producerProvider = producerProvider;
        this.topicService = topicService;
        this.statsFactory = statsFactory;
        this.deviceProfileCache = deviceProfileCache;
        this.tenantProfileCache = tenantProfileCache;
        this.rateLimitService = rateLimitService;
        this.dataDecodingEncodingService = dataDecodingEncodingService;
        this.scheduler = scheduler;
        this.transportResourceCache = transportResourceCache;
        this.eventPublisher = eventPublisher;
        this.notificationRuleProcessor = notificationRuleProcessor;
        this.entityLimitsCache = entityLimitsCache;
    }

    @PostConstruct
    public void init() {
        super.init();
        this.ruleEngineProducerStats = statsFactory.createMessagesStats(StatsType.RULE_ENGINE.getName() + ".producer");
        this.tbCoreProducerStats = statsFactory.createMessagesStats(StatsType.CORE.getName() + ".producer");
        this.transportApiStats = statsFactory.createMessagesStats(StatsType.TRANSPORT.getName() + ".producer");
        this.transportCallbackExecutor = ThingsBoardExecutors.newWorkStealingPool(20, getClass());
        this.scheduler.scheduleAtFixedRate(this::invalidateRateLimits, new Random().nextInt((int) sessionReportTimeout), sessionReportTimeout, TimeUnit.MILLISECONDS);
        transportApiRequestTemplate = queueProvider.createTransportApiRequestTemplate();
        transportApiRequestTemplate.setMessagesStats(transportApiStats);
        ruleEngineMsgProducer = producerProvider.getRuleEngineMsgProducer();
        tbCoreMsgProducer = producerProvider.getTbCoreMsgProducer();
        transportNotificationsConsumer = queueProvider.createTransportNotificationsConsumer();
        TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, serviceInfoProvider.getServiceId());
        transportNotificationsConsumer.subscribe(Collections.singleton(tpi));
        transportApiRequestTemplate.init();
        mainConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("transport-consumer"));
    }

    @AfterStartUp(order = AfterStartUp.TRANSPORT_SERVICE)
    public void start() {
        mainConsumerExecutor.execute(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToTransportMsg>> records = transportNotificationsConsumer.poll(notificationsPollDuration);
                    if (records.size() == 0) {
                        continue;
                    }
                    records.forEach(record -> {
                        try {
                            processToTransportMsg(record.getValue());
                        } catch (Throwable e) {
                            log.warn("Failed to process the notification.", e);
                        }
                    });
                    transportNotificationsConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain messages from queue.", e);
                        try {
                            Thread.sleep(notificationsPollDuration);
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                        }
                    }
                }
            }
        });
    }

    private void invalidateRateLimits() {
        rateLimitService.invalidateRateLimitsIpTable(sessionInactivityTimeout);
    }

    @PreDestroy
    public void destroy() {
        stopped = true;

        if (transportNotificationsConsumer != null) {
            transportNotificationsConsumer.unsubscribe();
        }
        if (transportCallbackExecutor != null) {
            transportCallbackExecutor.shutdownNow();
        }
        if (mainConsumerExecutor != null) {
            mainConsumerExecutor.shutdownNow();
        }
        if (transportApiRequestTemplate != null) {
            transportApiRequestTemplate.stop();
        }
    }

    @Override
    public SessionMetaData registerAsyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener) {
        return sessions.computeIfAbsent(toSessionId(sessionInfo), (x) -> new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listener));
    }

    @Override
    public TransportProtos.GetEntityProfileResponseMsg getEntityProfile(TransportProtos.GetEntityProfileRequestMsg msg) {
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg =
                new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setEntityProfileRequestMsg(msg).build());
        try {
            TbProtoQueueMsg<TransportApiResponseMsg> response = transportApiRequestTemplate.send(protoMsg).get();
            return response.getValue().getEntityProfileResponseMsg();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<TransportProtos.GetQueueRoutingInfoResponseMsg> getQueueRoutingInfo(TransportProtos.GetAllQueueRoutingInfoRequestMsg msg) {
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg =
                new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setGetAllQueueRoutingInfoRequestMsg(msg).build());
        try {
            TbProtoQueueMsg<TransportApiResponseMsg> response = transportApiRequestTemplate.send(protoMsg).get();
            return response.getValue().getGetQueueRoutingInfoResponseMsgsList();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TransportProtos.GetResourceResponseMsg getResource(TransportProtos.GetResourceRequestMsg msg) {
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg =
                new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setResourceRequestMsg(msg).build());
        try {
            TbProtoQueueMsg<TransportApiResponseMsg> response = transportApiRequestTemplate.send(protoMsg).get();
            return response.getValue().getResourceResponseMsg();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TransportProtos.GetSnmpDevicesResponseMsg getSnmpDevicesIds(TransportProtos.GetSnmpDevicesRequestMsg requestMsg) {
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(
                UUID.randomUUID(), TransportApiRequestMsg.newBuilder()
                .setSnmpDevicesRequestMsg(requestMsg)
                .build()
        );

        try {
            TbProtoQueueMsg<TransportApiResponseMsg> response = transportApiRequestTemplate.send(protoMsg).get();
            return response.getValue().getSnmpDevicesResponseMsg();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TransportProtos.GetDeviceResponseMsg getDevice(TransportProtos.GetDeviceRequestMsg requestMsg) {
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(
                UUID.randomUUID(), TransportApiRequestMsg.newBuilder()
                .setDeviceRequestMsg(requestMsg)
                .build()
        );

        try {
            TransportApiResponseMsg response = transportApiRequestTemplate.send(protoMsg).get().getValue();
            if (response.hasDeviceResponseMsg()) {
                return response.getDeviceResponseMsg();
            } else {
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TransportProtos.GetDeviceCredentialsResponseMsg getDeviceCredentials(TransportProtos.GetDeviceCredentialsRequestMsg requestMsg) {
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(
                UUID.randomUUID(), TransportApiRequestMsg.newBuilder()
                .setDeviceCredentialsRequestMsg(requestMsg)
                .build()
        );

        try {
            TbProtoQueueMsg<TransportApiResponseMsg> response = transportApiRequestTemplate.send(protoMsg).get();
            return response.getValue().getDeviceCredentialsResponseMsg();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(DeviceTransportType transportType, TransportProtos.ValidateDeviceTokenRequestMsg msg,
                        TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        log.trace("Processing msg: {}", msg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(),
                TransportApiRequestMsg.newBuilder().setValidateTokenRequestMsg(msg).build());
        doProcess(transportType, protoMsg, callback);
    }

    @Override
    public void process(DeviceTransportType transportType, TransportProtos.ValidateBasicMqttCredRequestMsg msg,
                        TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        log.trace("Processing msg: {}", msg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(),
                TransportApiRequestMsg.newBuilder().setValidateBasicMqttCredRequestMsg(msg).build());
        doProcess(transportType, protoMsg, callback);
    }

    @Override
    public void process(TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg requestMsg, TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        log.trace("Processing msg: {}", requestMsg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setValidateDeviceLwM2MCredentialsRequestMsg(requestMsg).build());
        ListenableFuture<ValidateDeviceCredentialsResponse> response = Futures.transform(transportApiRequestTemplate.send(protoMsg), tmp -> {
            TransportProtos.ValidateDeviceCredentialsResponseMsg msg = tmp.getValue().getValidateCredResponseMsg();
            ValidateDeviceCredentialsResponse.ValidateDeviceCredentialsResponseBuilder result = ValidateDeviceCredentialsResponse.builder();
            if (msg.hasDeviceInfo()) {
                result.credentials(msg.getCredentialsBody());
                TransportDeviceInfo tdi = getTransportDeviceInfo(msg.getDeviceInfo());
                result.deviceInfo(tdi);
                ByteString profileBody = msg.getProfileBody();
                if (!profileBody.isEmpty()) {
                    DeviceProfile profile = deviceProfileCache.getOrCreate(tdi.getDeviceProfileId(), profileBody);
                    result.deviceProfile(profile);
                }
            }
            return result.build();
        }, MoreExecutors.directExecutor());
        AsyncCallbackTemplate.withCallback(response, callback::onSuccess, callback::onError, transportCallbackExecutor);
    }

    @Override
    public void process(DeviceTransportType transportType, TransportProtos.ValidateDeviceX509CertRequestMsg msg, TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        log.trace("Processing msg: {}", msg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setValidateX509CertRequestMsg(msg).build());
        doProcess(transportType, protoMsg, callback);
    }

    @Override
    public void process(DeviceTransportType transportType, TransportProtos.ValidateOrCreateDeviceX509CertRequestMsg msg, TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        log.trace("Processing msg: {}", msg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setValidateOrCreateX509CertRequestMsg(msg).build());
        doProcess(transportType, protoMsg, callback);
    }

    private void doProcess(DeviceTransportType transportType, TbProtoQueueMsg<TransportApiRequestMsg> protoMsg,
                           TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        ListenableFuture<ValidateDeviceCredentialsResponse> response = Futures.transform(transportApiRequestTemplate.send(protoMsg), tmp -> {
            TransportProtos.ValidateDeviceCredentialsResponseMsg msg = tmp.getValue().getValidateCredResponseMsg();
            ValidateDeviceCredentialsResponse.ValidateDeviceCredentialsResponseBuilder result = ValidateDeviceCredentialsResponse.builder();
            if (msg.hasDeviceInfo()) {
                result.credentials(msg.getCredentialsBody());
                TransportDeviceInfo tdi = getTransportDeviceInfo(msg.getDeviceInfo());
                result.deviceInfo(tdi);
                ByteString profileBody = msg.getProfileBody();
                if (!profileBody.isEmpty()) {
                    DeviceProfile profile = deviceProfileCache.getOrCreate(tdi.getDeviceProfileId(), profileBody);
                    if (transportType != DeviceTransportType.DEFAULT
                            && profile != null && profile.getTransportType() != DeviceTransportType.DEFAULT && profile.getTransportType() != transportType) {
                        log.debug("[{}] Device profile [{}] has different transport type: {}, expected: {}", tdi.getDeviceId(), tdi.getDeviceProfileId(), profile.getTransportType(), transportType);
                        throw new IllegalStateException("Device profile has different transport type: " + profile.getTransportType() + ". Expected: " + transportType);
                    }
                    result.deviceProfile(profile);
                }
            }
            return result.build();
        }, MoreExecutors.directExecutor());
        AsyncCallbackTemplate.withCallback(response, callback::onSuccess, callback::onError, transportCallbackExecutor);
    }


    @Override
    public void process(TenantId tenantId, TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg requestMsg, TransportServiceCallback<GetOrCreateDeviceFromGatewayResponse> callback) {
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setGetOrCreateDeviceRequestMsg(requestMsg).build());
        log.trace("Processing msg: {}", requestMsg);
        var key = new EntityLimitKey(tenantId, StringUtils.truncate(requestMsg.getDeviceName(), 256));
        if (entityLimitsCache.get(key)) {
            transportCallbackExecutor.submit(() -> callback.onError(new RuntimeException(DataConstants.MAXIMUM_NUMBER_OF_DEVICES_REACHED)));
        } else {
            ListenableFuture<GetOrCreateDeviceFromGatewayResponse> response = Futures.transform(transportApiRequestTemplate.send(protoMsg), tmp -> {
                TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg msg = tmp.getValue().getGetOrCreateDeviceResponseMsg();
                GetOrCreateDeviceFromGatewayResponse.GetOrCreateDeviceFromGatewayResponseBuilder result = GetOrCreateDeviceFromGatewayResponse.builder();
                if (msg.hasDeviceInfo()) {
                    TransportDeviceInfo tdi = getTransportDeviceInfo(msg.getDeviceInfo());
                    result.deviceInfo(tdi);
                    ByteString profileBody = msg.getProfileBody();
                    if (!profileBody.isEmpty()) {
                        result.deviceProfile(deviceProfileCache.getOrCreate(tdi.getDeviceProfileId(), profileBody));
                    }
                } else if (TransportProtos.TransportApiRequestErrorCode.ENTITY_LIMIT.equals(msg.getError())) {
                    entityLimitsCache.put(key, true);
                    throw new RuntimeException(DataConstants.MAXIMUM_NUMBER_OF_DEVICES_REACHED);
                }
                return result.build();
            }, MoreExecutors.directExecutor());
            AsyncCallbackTemplate.withCallback(response, callback::onSuccess, callback::onError, transportCallbackExecutor);
        }
    }

    @Override
    public void process(TransportProtos.LwM2MRequestMsg msg, TransportServiceCallback<TransportProtos.LwM2MResponseMsg> callback) {
        log.trace("Processing msg: {}", msg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(),
                TransportApiRequestMsg.newBuilder().setLwM2MRequestMsg(msg).build());
        AsyncCallbackTemplate.withCallback(transportApiRequestTemplate.send(protoMsg),
                response -> callback.onSuccess(response.getValue().getLwM2MResponseMsg()), callback::onError, transportCallbackExecutor);
    }

    private TransportDeviceInfo getTransportDeviceInfo(TransportProtos.DeviceInfoProto di) {
        TransportDeviceInfo tdi = new TransportDeviceInfo();
        tdi.setTenantId(TenantId.fromUUID(new UUID(di.getTenantIdMSB(), di.getTenantIdLSB())));
        tdi.setCustomerId(new CustomerId(new UUID(di.getCustomerIdMSB(), di.getCustomerIdLSB())));
        tdi.setDeviceId(new DeviceId(new UUID(di.getDeviceIdMSB(), di.getDeviceIdLSB())));
        tdi.setDeviceProfileId(new DeviceProfileId(new UUID(di.getDeviceProfileIdMSB(), di.getDeviceProfileIdLSB())));
        tdi.setAdditionalInfo(di.getAdditionalInfo());
        tdi.setDeviceName(di.getDeviceName());
        tdi.setDeviceType(di.getDeviceType());
        if (StringUtils.isNotEmpty(di.getPowerMode())) {
            tdi.setPowerMode(PowerMode.valueOf(di.getPowerMode()));
            tdi.setEdrxCycle(di.getEdrxCycle());
            tdi.setPsmActivityTimer(di.getPsmActivityTimer());
            tdi.setPagingTransmissionWindow(di.getPagingTransmissionWindow());
        }
        return tdi;
    }

    @Override
    public void process(ProvisionDeviceRequestMsg requestMsg, TransportServiceCallback<ProvisionDeviceResponseMsg> callback) {
        log.trace("Processing msg: {}", requestMsg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setProvisionDeviceRequestMsg(requestMsg).build());
        ListenableFuture<ProvisionDeviceResponseMsg> response = Futures.transform(transportApiRequestTemplate.send(protoMsg), tmp ->
                        tmp.getValue().getProvisionDeviceResponseMsg()
                , MoreExecutors.directExecutor());
        AsyncCallbackTemplate.withCallback(response, callback::onSuccess, callback::onError, transportCallbackExecutor);
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscriptionInfoProto msg, TransportServiceCallback<Void> callback) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", toSessionId(sessionInfo), msg);
        }
        sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                .setSubscriptionInfo(msg).build(), callback);
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SessionEventMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            recordActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                    .setSessionEvent(msg).build(), callback);
        }
    }

    @Override
    public void process(TransportToDeviceActorMsg msg, TransportServiceCallback<Void> callback) {
        TransportProtos.SessionInfoProto sessionInfo = msg.getSessionInfo();
        if (checkLimits(sessionInfo, msg, callback)) {
            SessionMetaData sessionMetaData = sessions.get(toSessionId(sessionInfo));
            if (sessionMetaData != null) {
                if (msg.hasSubscribeToAttributes()) {
                    sessionMetaData.setSubscribedToAttributes(true);
                }
                if (msg.hasSubscribeToRPC()) {
                    sessionMetaData.setSubscribedToRPC(true);
                }
            }

            recordActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, msg, callback);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostTelemetryMsg msg, TransportServiceCallback<Void> callback) {
        process(sessionInfo, msg, null, callback);
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostTelemetryMsg msg, TbMsgMetaData md, TransportServiceCallback<Void> callback) {
        int dataPoints = 0;
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            dataPoints += tsKv.getKvCount();
        }
        if (checkLimits(sessionInfo, msg, callback, dataPoints)) {
            recordActivityInternal(sessionInfo);
            TenantId tenantId = getTenantId(sessionInfo);
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            CustomerId customerId = getCustomerId(sessionInfo);
            MsgPackCallback packCallback = new MsgPackCallback(msg.getTsKvListCount(), new ApiStatsProxyCallback<>(tenantId, customerId, dataPoints, callback));
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                TbMsgMetaData metaData = md != null ? md.copy() : new TbMsgMetaData();
                metaData.putValue("deviceName", sessionInfo.getDeviceName());
                metaData.putValue("deviceType", sessionInfo.getDeviceType());
                metaData.putValue("ts", tsKv.getTs() + "");
                JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
                sendToRuleEngine(tenantId, deviceId, customerId, sessionInfo, json, metaData, TbMsgType.POST_TELEMETRY_REQUEST, packCallback);
            }
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostAttributeMsg msg, TransportServiceCallback<Void> callback) {
        process(sessionInfo, msg, null, callback);
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostAttributeMsg msg, TbMsgMetaData md, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback, msg.getKvCount())) {
            recordActivityInternal(sessionInfo);
            TenantId tenantId = getTenantId(sessionInfo);
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
            TbMsgMetaData metaData = md != null ? md.copy() : new TbMsgMetaData();
            metaData.putValue("deviceName", sessionInfo.getDeviceName());
            metaData.putValue("deviceType", sessionInfo.getDeviceType());
            if (msg.getShared()) {
                metaData.putValue(DataConstants.SCOPE, DataConstants.SHARED_SCOPE);
            }
            metaData.putValue(DataConstants.NOTIFY_DEVICE_METADATA_KEY, "false");
            CustomerId customerId = getCustomerId(sessionInfo);
            sendToRuleEngine(tenantId, deviceId, customerId, sessionInfo, json, metaData, TbMsgType.POST_ATTRIBUTES_REQUEST,
                    new TransportTbQueueCallback(new ApiStatsProxyCallback<>(tenantId, customerId, msg.getKvList().size(), callback)));
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            recordActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                    .setGetAttributes(msg).build(), new ApiStatsProxyCallback<>(getTenantId(sessionInfo), getCustomerId(sessionInfo), 1, callback));
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            SessionMetaData sessionMetaData = sessions.get(toSessionId(sessionInfo));
            if (sessionMetaData != null) {
                sessionMetaData.setSubscribedToAttributes(!msg.getUnsubscribe());
            }
            recordActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setSubscribeToAttributes(msg).build(),
                    new ApiStatsProxyCallback<>(getTenantId(sessionInfo), getCustomerId(sessionInfo), 1, callback));
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToRPCMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            SessionMetaData sessionMetaData = sessions.get(toSessionId(sessionInfo));
            if (sessionMetaData != null) {
                sessionMetaData.setSubscribedToRPC(!msg.getUnsubscribe());
            }
            recordActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setSubscribeToRPC(msg).build(),
                    new ApiStatsProxyCallback<>(getTenantId(sessionInfo), getCustomerId(sessionInfo), 1, callback));
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToDeviceRpcResponseMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            recordActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setToDeviceRPCCallResponse(msg).build(),
                    new ApiStatsProxyCallback<>(getTenantId(sessionInfo), getCustomerId(sessionInfo), 1, callback));
        }
    }

    @Override
    public void notifyAboutUplink(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.UplinkNotificationMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            recordActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setUplinkNotificationMsg(msg).build(), callback);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToDeviceRpcRequestMsg msg, RpcStatus rpcStatus, TransportServiceCallback<Void> callback) {
        process(sessionInfo, msg, rpcStatus, false, callback);
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToDeviceRpcRequestMsg msg, RpcStatus rpcStatus, boolean reportActivity, TransportServiceCallback<Void> callback) {
        TransportProtos.ToDeviceRpcResponseStatusMsg responseMsg = TransportProtos.ToDeviceRpcResponseStatusMsg.newBuilder()
                .setRequestId(msg.getRequestId())
                .setRequestIdLSB(msg.getRequestIdLSB())
                .setRequestIdMSB(msg.getRequestIdMSB())
                .setStatus(rpcStatus.name())
                .build();

        if (checkLimits(sessionInfo, responseMsg, callback)) {
            if (reportActivity) {
                recordActivityInternal(sessionInfo);
            }
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setRpcResponseStatusMsg(responseMsg).build(),
                    new ApiStatsProxyCallback<>(getTenantId(sessionInfo), getCustomerId(sessionInfo), 1, TransportServiceCallback.EMPTY));
        }
    }

    private void processTimeout(String requestId) {
        RpcRequestMetadata data = toServerRpcPendingMap.remove(requestId);
        if (data != null) {
            SessionMetaData md = sessions.get(data.getSessionId());
            if (md != null) {
                SessionMsgListener listener = md.getListener();
                transportCallbackExecutor.submit(() -> {
                    TransportProtos.ToServerRpcResponseMsg responseMsg =
                            TransportProtos.ToServerRpcResponseMsg.newBuilder()
                                    .setRequestId(data.getRequestId())
                                    .setError("timeout").build();
                    listener.onToServerRpcResponse(responseMsg);
                });
                if (md.getSessionType() == TransportProtos.SessionType.SYNC) {
                    deregisterSession(md.getSessionInfo());
                }
            } else {
                log.debug("[{}] Missing session.", data.getSessionId());
            }
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToServerRpcRequestMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            recordActivityInternal(sessionInfo);
            UUID sessionId = toSessionId(sessionInfo);
            TenantId tenantId = getTenantId(sessionInfo);
            DeviceId deviceId = getDeviceId(sessionInfo);
            JsonObject json = new JsonObject();
            json.addProperty("method", msg.getMethodName());
            json.add("params", JsonUtils.parse(msg.getParams()));

            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("deviceName", sessionInfo.getDeviceName());
            metaData.putValue("deviceType", sessionInfo.getDeviceType());
            metaData.putValue("requestId", Integer.toString(msg.getRequestId()));
            metaData.putValue("serviceId", serviceInfoProvider.getServiceId());
            metaData.putValue("sessionId", sessionId.toString());
            sendToRuleEngine(tenantId, deviceId, getCustomerId(sessionInfo), sessionInfo, json, metaData,
                    TbMsgType.TO_SERVER_RPC_REQUEST, new TransportTbQueueCallback(callback));
            String requestId = sessionId + "-" + msg.getRequestId();
            toServerRpcPendingMap.put(requestId, new RpcRequestMetadata(sessionId, msg.getRequestId()));
            scheduler.schedule(() -> processTimeout(requestId), clientSideRpcTimeout, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ClaimDeviceMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            recordActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                    .setClaimDevice(msg).build(), callback);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.GetOtaPackageRequestMsg msg, TransportServiceCallback<TransportProtos.GetOtaPackageResponseMsg> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            TbProtoQueueMsg<TransportApiRequestMsg> protoMsg =
                    new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setOtaPackageRequestMsg(msg).build());

            AsyncCallbackTemplate.withCallback(transportApiRequestTemplate.send(protoMsg), response -> {
                callback.onSuccess(response.getValue().getOtaPackageResponseMsg());
            }, callback::onError, transportCallbackExecutor);
        }
    }

    @Override
    public void recordActivity(TransportProtos.SessionInfoProto sessionInfo) {
        recordActivityInternal(sessionInfo);
    }

    private void recordActivityInternal(TransportProtos.SessionInfoProto sessionInfo) {
        onActivity(toSessionId(sessionInfo), sessionInfo, getCurrentTimeMillis());
    }

    @Override
    public void lifecycleEvent(TenantId tenantId, DeviceId deviceId, ComponentLifecycleEvent eventType, boolean success, Throwable error) {
        ToCoreMsg msg = ToCoreMsg.newBuilder()
                .setLifecycleEventMsg(TransportProtos.LifecycleEventProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setEntityIdMSB(deviceId.getId().getMostSignificantBits())
                        .setEntityIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setServiceId(serviceInfoProvider.getServiceId())
                        .setLcEventType(eventType.name())
                        .setSuccess(success)
                        .setError(error != null ? ExceptionUtils.getStackTrace(error) : ""))
                .build();
        try {
            sendToCore(tenantId, deviceId, msg, deviceId.getId(), TransportServiceCallback.EMPTY);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to send lifecycle event to core", tenantId, deviceId, e);
        }
    }

    @Override
    public void errorEvent(TenantId tenantId, DeviceId deviceId, String method, Throwable error) {
        ToCoreMsg msg = ToCoreMsg.newBuilder()
                .setErrorEventMsg(TransportProtos.ErrorEventProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setEntityIdMSB(deviceId.getId().getMostSignificantBits())
                        .setEntityIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setServiceId(serviceInfoProvider.getServiceId())
                        .setMethod(method)
                        .setError(ExceptionUtils.getRootCauseMessage(error)))
                .build();
        try {
            sendToCore(tenantId, deviceId, msg, deviceId.getId(), TransportServiceCallback.EMPTY);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to send error event to core", tenantId, deviceId, e);
        }
    }

    @Override
    public SessionMetaData registerSyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener, long timeout) {
        SessionMetaData currentSession = new SessionMetaData(sessionInfo, TransportProtos.SessionType.SYNC, listener);
        UUID sessionId = toSessionId(sessionInfo);
        sessions.putIfAbsent(sessionId, currentSession);

        TransportProtos.SessionCloseNotificationProto notification = TransportProtos.SessionCloseNotificationProto.newBuilder().setMessage("session timeout!").build();

        ScheduledFuture executorFuture = scheduler.schedule(() -> {
            listener.onRemoteSessionCloseCommand(sessionId, notification);
            deregisterSession(sessionInfo);
        }, timeout, TimeUnit.MILLISECONDS);

        currentSession.setScheduledFuture(executorFuture);
        return currentSession;
    }

    @Override
    public void deregisterSession(TransportProtos.SessionInfoProto sessionInfo) {
        SessionMetaData currentSession = sessions.get(toSessionId(sessionInfo));
        if (currentSession != null && currentSession.hasScheduledFuture()) {
            log.debug("Stopping scheduler to avoid resending response if request has been ack.");
            currentSession.getScheduledFuture().cancel(false);
        }
        sessions.remove(toSessionId(sessionInfo));
    }

    @Override
    public void log(TransportProtos.SessionInfoProto sessionInfo, String msg) {
        if (!logEnabled || sessionInfo == null || StringUtils.isEmpty(msg)) {
            return;
        }
        if (msg.length() > logMaxLength) {
            msg = msg.substring(0, logMaxLength);
        }
        TransportProtos.PostTelemetryMsg.Builder request = TransportProtos.PostTelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto.Builder builder = TransportProtos.TsKvListProto.newBuilder();
        builder.setTs(TimeUnit.MILLISECONDS.toSeconds(getCurrentTimeMillis()) * 1000L + (atomicTs.getAndIncrement() % 1000));
        builder.addKv(TransportProtos.KeyValueProto.newBuilder()
                .setKey("transportLog")
                .setType(TransportProtos.KeyValueType.STRING_V)
                .setStringV(msg).build());
        request.addTsKvList(builder.build());
        TransportProtos.PostTelemetryMsg postTelemetryMsg = request.build();
        process(sessionInfo, postTelemetryMsg, TransportServiceCallback.EMPTY);
    }

    private boolean checkLimits(TransportProtos.SessionInfoProto sessionInfo, Object msg, TransportServiceCallback<?> callback) {
        return checkLimits(sessionInfo, msg, callback, 0);
    }

    private boolean checkLimits(TransportProtos.SessionInfoProto sessionInfo, Object msg, TransportServiceCallback<?> callback, int dataPoints) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", toSessionId(sessionInfo), msg);
        }
        TenantId tenantId = TenantId.fromUUID(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));

        EntityType rateLimitedEntityType = rateLimitService.checkLimits(tenantId, deviceId, dataPoints);
        if (rateLimitedEntityType == null) {
            return true;
        } else {
            if (callback != null) {
                callback.onError(new TbRateLimitsException(rateLimitedEntityType));
            }
            if (rateLimitedEntityType == EntityType.DEVICE || rateLimitedEntityType == EntityType.TENANT) {
                notificationRuleProcessor.process(RateLimitsTrigger.builder()
                        .tenantId(tenantId)
                        .api(rateLimitedEntityType == EntityType.DEVICE ? LimitedApi.TRANSPORT_MESSAGES_PER_DEVICE : LimitedApi.TRANSPORT_MESSAGES_PER_TENANT)
                        .limitLevel(rateLimitedEntityType == EntityType.DEVICE ? deviceId : tenantId)
                        .limitLevelEntityName(rateLimitedEntityType == EntityType.DEVICE ? sessionInfo.getDeviceName() : null)
                        .build());
            }
            return false;
        }
    }

    protected void processToTransportMsg(ToTransportMsg toSessionMsg) {
        UUID sessionId = new UUID(toSessionMsg.getSessionIdMSB(), toSessionMsg.getSessionIdLSB());
        SessionMetaData md = sessions.get(sessionId);
        if (md != null) {
            log.trace("[{}] Processing notification: {}", sessionId, toSessionMsg);
            SessionMsgListener listener = md.getListener();
            transportCallbackExecutor.submit(() -> {
                if (toSessionMsg.hasGetAttributesResponse()) {
                    listener.onGetAttributesResponse(toSessionMsg.getGetAttributesResponse());
                }
                if (toSessionMsg.hasAttributeUpdateNotification()) {
                    listener.onAttributeUpdate(sessionId, toSessionMsg.getAttributeUpdateNotification());
                }
                if (toSessionMsg.hasSessionCloseNotification()) {
                    listener.onRemoteSessionCloseCommand(sessionId, toSessionMsg.getSessionCloseNotification());
                }
                if (toSessionMsg.hasToTransportUpdateCredentialsNotification()) {
                    listener.onToTransportUpdateCredentials(toSessionMsg.getToTransportUpdateCredentialsNotification());
                }
                if (toSessionMsg.hasToDeviceRequest()) {
                    listener.onToDeviceRpcRequest(sessionId, toSessionMsg.getToDeviceRequest());
                }
                if (toSessionMsg.hasToServerResponse()) {
                    String requestId = sessionId + "-" + toSessionMsg.getToServerResponse().getRequestId();
                    toServerRpcPendingMap.remove(requestId);
                    listener.onToServerRpcResponse(toSessionMsg.getToServerResponse());
                }
            });
            if (md.getSessionType() == TransportProtos.SessionType.SYNC) {
                deregisterSession(md.getSessionInfo());
            }
        } else {
            log.trace("Processing broadcast notification: {}", toSessionMsg);
            if (toSessionMsg.hasEntityUpdateMsg()) {
                TransportProtos.EntityUpdateMsg msg = toSessionMsg.getEntityUpdateMsg();
                EntityType entityType = EntityType.valueOf(msg.getEntityType());
                if (EntityType.DEVICE_PROFILE.equals(entityType)) {
                    DeviceProfile deviceProfile = deviceProfileCache.put(msg.getData());
                    if (deviceProfile != null) {
                        log.debug("On device profile update: {}", deviceProfile);
                        onProfileUpdate(deviceProfile);
                    }
                } else if (EntityType.TENANT_PROFILE.equals(entityType)) {
                    rateLimitService.update(tenantProfileCache.put(msg.getData()));
                } else if (EntityType.TENANT.equals(entityType)) {
                    Optional<Tenant> profileOpt = dataDecodingEncodingService.decode(msg.getData().toByteArray());
                    if (profileOpt.isPresent()) {
                        Tenant tenant = profileOpt.get();
                        boolean updated = tenantProfileCache.put(tenant.getId(), tenant.getTenantProfileId());
                        partitionService.evictTenantInfo(tenant.getId());
                        if (updated) {
                            rateLimitService.update(tenant.getId());
                        }
                    }
                } else if (EntityType.API_USAGE_STATE.equals(entityType)) {
                    Optional<ApiUsageState> stateOpt = dataDecodingEncodingService.decode(msg.getData().toByteArray());
                    if (stateOpt.isPresent()) {
                        ApiUsageState apiUsageState = stateOpt.get();
                        rateLimitService.update(apiUsageState.getTenantId(), apiUsageState.isTransportEnabled());
                        //TODO: if transport is disabled, we should close all sessions and not to check credentials.
                    }
                } else if (EntityType.DEVICE.equals(entityType)) {
                    Optional<Device> deviceOpt = dataDecodingEncodingService.decode(msg.getData().toByteArray());
                    deviceOpt.ifPresent(this::onDeviceUpdate);
                }
            } else if (toSessionMsg.hasEntityDeleteMsg()) {
                TransportProtos.EntityDeleteMsg msg = toSessionMsg.getEntityDeleteMsg();
                EntityType entityType = EntityType.valueOf(msg.getEntityType());
                UUID entityUuid = new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB());
                if (EntityType.DEVICE_PROFILE.equals(entityType)) {
                    deviceProfileCache.evict(new DeviceProfileId(new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB())));
                } else if (EntityType.TENANT_PROFILE.equals(entityType)) {
                    tenantProfileCache.remove(new TenantProfileId(entityUuid));
                } else if (EntityType.TENANT.equals(entityType)) {
                    TenantId tenantId = TenantId.fromUUID(entityUuid);
                    rateLimitService.remove(tenantId);
                    partitionService.removeTenant(tenantId);
                } else if (EntityType.DEVICE.equals(entityType)) {
                    rateLimitService.remove(new DeviceId(entityUuid));
                    onDeviceDeleted(new DeviceId(entityUuid));
                }
            } else if (toSessionMsg.hasResourceUpdateMsg()) {
                TransportProtos.ResourceUpdateMsg msg = toSessionMsg.getResourceUpdateMsg();
                TenantId tenantId = TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
                ResourceType resourceType = ResourceType.valueOf(msg.getResourceType());
                String resourceId = msg.getResourceKey();
                transportResourceCache.update(tenantId, resourceType, resourceId);
                sessions.forEach((id, mdRez) -> {
                    log.trace("ResourceUpdate - [{}] [{}]", id, mdRez);
                    transportCallbackExecutor.submit(() -> mdRez.getListener().onResourceUpdate(msg));
                });

            } else if (toSessionMsg.hasResourceDeleteMsg()) {
                TransportProtos.ResourceDeleteMsg msg = toSessionMsg.getResourceDeleteMsg();
                TenantId tenantId = TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
                ResourceType resourceType = ResourceType.valueOf(msg.getResourceType());
                String resourceId = msg.getResourceKey();
                transportResourceCache.evict(tenantId, resourceType, resourceId);
                sessions.forEach((id, mdRez) -> {
                    log.warn("ResourceDelete - [{}] [{}]", id, mdRez);
                    transportCallbackExecutor.submit(() -> mdRez.getListener().onResourceDelete(msg));
                });
            } else if (toSessionMsg.getQueueUpdateMsgsCount() > 0) {
                partitionService.updateQueues(toSessionMsg.getQueueUpdateMsgsList());
            } else if (toSessionMsg.getQueueDeleteMsgsCount() > 0) {
                partitionService.removeQueues(toSessionMsg.getQueueDeleteMsgsList());
            } else {
                //TODO: should we notify the device actor about missed session?
                log.debug("[{}] Missing session.", sessionId);
            }
        }
    }


    public void onProfileUpdate(DeviceProfile deviceProfile) {
        long deviceProfileIdMSB = deviceProfile.getId().getId().getMostSignificantBits();
        long deviceProfileIdLSB = deviceProfile.getId().getId().getLeastSignificantBits();
        sessions.forEach((id, md) -> {
            //TODO: if transport types are different - we should close the session.
            if (md.getSessionInfo().getDeviceProfileIdMSB() == deviceProfileIdMSB
                    && md.getSessionInfo().getDeviceProfileIdLSB() == deviceProfileIdLSB) {
                TransportProtos.SessionInfoProto newSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                        .mergeFrom(md.getSessionInfo())
                        .setDeviceProfileIdMSB(deviceProfileIdMSB)
                        .setDeviceProfileIdLSB(deviceProfileIdLSB)
                        .setDeviceType(deviceProfile.getName())
                        .build();
                md.setSessionInfo(newSessionInfo);
                transportCallbackExecutor.submit(() -> md.getListener().onDeviceProfileUpdate(newSessionInfo, deviceProfile));
            }
        });

        eventPublisher.publishEvent(new DeviceProfileUpdatedEvent(deviceProfile));
    }

    private void onDeviceUpdate(Device device) {
        long deviceIdMSB = device.getId().getId().getMostSignificantBits();
        long deviceIdLSB = device.getId().getId().getLeastSignificantBits();
        long deviceProfileIdMSB = device.getDeviceProfileId().getId().getMostSignificantBits();
        long deviceProfileIdLSB = device.getDeviceProfileId().getId().getLeastSignificantBits();
        sessions.forEach((id, md) -> {
            if ((md.getSessionInfo().getDeviceIdMSB() == deviceIdMSB && md.getSessionInfo().getDeviceIdLSB() == deviceIdLSB)) {
                DeviceProfile newDeviceProfile;
                if (md.getSessionInfo().getDeviceProfileIdMSB() != deviceProfileIdMSB
                        || md.getSessionInfo().getDeviceProfileIdLSB() != deviceProfileIdLSB) {
                    //TODO: if transport types are different - we should close the session.
                    newDeviceProfile = deviceProfileCache.get(new DeviceProfileId(new UUID(deviceProfileIdMSB, deviceProfileIdLSB)));
                } else {
                    newDeviceProfile = null;
                }
                TransportProtos.SessionInfoProto newSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                        .mergeFrom(md.getSessionInfo())
                        .setDeviceProfileIdMSB(deviceProfileIdMSB)
                        .setDeviceProfileIdLSB(deviceProfileIdLSB)
                        .setDeviceName(device.getName())
                        .setDeviceType(device.getType()).build();
                if (device.getAdditionalInfo().has("gateway")
                        && device.getAdditionalInfo().get("gateway").asBoolean()
                        && device.getAdditionalInfo().has(OVERWRITE_ACTIVITY_TIME)
                        && device.getAdditionalInfo().get(OVERWRITE_ACTIVITY_TIME).isBoolean()) {
                    md.setOverwriteActivityTime(device.getAdditionalInfo().get(OVERWRITE_ACTIVITY_TIME).asBoolean());
                }
                md.setSessionInfo(newSessionInfo);
                transportCallbackExecutor.submit(() -> md.getListener().onDeviceUpdate(newSessionInfo, device, Optional.ofNullable(newDeviceProfile)));
            }
        });

        eventPublisher.publishEvent(new DeviceUpdatedEvent(device));
    }

    private void onDeviceDeleted(DeviceId deviceId) {
        sessions.forEach((id, md) -> {
            DeviceId sessionDeviceId = new DeviceId(new UUID(md.getSessionInfo().getDeviceIdMSB(), md.getSessionInfo().getDeviceIdLSB()));
            if (sessionDeviceId.equals(deviceId)) {
                transportCallbackExecutor.submit(() -> {
                    md.getListener().onDeviceDeleted(deviceId);
                });
            }
        });

        eventPublisher.publishEvent(new DeviceDeletedEvent(deviceId));
    }

    protected UUID toSessionId(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    protected UUID getRoutingKey(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB());
    }

    protected TenantId getTenantId(TransportProtos.SessionInfoProto sessionInfo) {
        return TenantId.fromUUID(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
    }

    protected CustomerId getCustomerId(TransportProtos.SessionInfoProto sessionInfo) {
        long msb = sessionInfo.getCustomerIdMSB();
        long lsb = sessionInfo.getCustomerIdLSB();
        if (msb != 0 && lsb != 0) {
            return new CustomerId(new UUID(msb, lsb));
        } else {
            return new CustomerId(EntityId.NULL_UUID);
        }
    }

    protected DeviceId getDeviceId(TransportProtos.SessionInfoProto sessionInfo) {
        return new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
    }

    protected void sendToDeviceActor(TransportProtos.SessionInfoProto sessionInfo, TransportToDeviceActorMsg toDeviceActorMsg, TransportServiceCallback<Void> callback) {
        ToCoreMsg toCoreMsg = ToCoreMsg.newBuilder().setToDeviceActorMsg(toDeviceActorMsg).build();
        sendToCore(getTenantId(sessionInfo), getDeviceId(sessionInfo), toCoreMsg, getRoutingKey(sessionInfo), callback);
    }

    private void sendToCore(TenantId tenantId, EntityId entityId, ToCoreMsg msg, UUID routingKey, TransportServiceCallback<Void> callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Pushing to topic {} message {}", tenantId, entityId, tpi.getFullTopicName(), msg);
        }

        TransportTbQueueCallback transportTbQueueCallback = callback != null ?
                new TransportTbQueueCallback(callback) : null;
        tbCoreProducerStats.incrementTotal();
        StatsCallback wrappedCallback = new StatsCallback(transportTbQueueCallback, tbCoreProducerStats);
        tbCoreMsgProducer.send(tpi, new TbProtoQueueMsg<>(routingKey, msg), wrappedCallback);
    }

    private void sendToRuleEngine(TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, tbMsg.getOriginator());
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Pushing to topic {} message {}", tenantId, tbMsg.getOriginator(), tpi.getFullTopicName(), tbMsg);
        }
        ToRuleEngineMsg msg = ToRuleEngineMsg.newBuilder().setTbMsg(TbMsg.toByteString(tbMsg))
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
        ruleEngineProducerStats.incrementTotal();
        StatsCallback wrappedCallback = new StatsCallback(callback, ruleEngineProducerStats);
        ruleEngineMsgProducer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), wrappedCallback);
    }

    private void sendToRuleEngine(TenantId tenantId, DeviceId deviceId, CustomerId customerId, TransportProtos.SessionInfoProto sessionInfo, JsonObject json,
                                  TbMsgMetaData metaData, TbMsgType tbMsgType, TbQueueCallback callback) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
        DeviceProfile deviceProfile = deviceProfileCache.get(deviceProfileId);
        RuleChainId ruleChainId;
        String queueName;

        if (deviceProfile == null) {
            log.warn("[{}] Device profile is null!", deviceProfileId);
            ruleChainId = null;
            queueName = null;
        } else {
            ruleChainId = deviceProfile.getDefaultRuleChainId();
            queueName = deviceProfile.getDefaultQueueName();
        }

        TbMsg tbMsg = TbMsg.newMsg(queueName, tbMsgType, deviceId, customerId, metaData, gson.toJson(json), ruleChainId, null);
        sendToRuleEngine(tenantId, tbMsg, callback);
    }

    private class TransportTbQueueCallback implements TbQueueCallback {
        private final TransportServiceCallback<Void> callback;

        private TransportTbQueueCallback(TransportServiceCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            DefaultTransportService.this.transportCallbackExecutor.submit(() -> callback.onSuccess(null));
        }

        @Override
        public void onFailure(Throwable t) {
            DefaultTransportService.this.transportCallbackExecutor.submit(() -> callback.onError(t));
        }
    }

    private static class StatsCallback implements TbQueueCallback {
        private final TbQueueCallback callback;
        private final MessagesStats stats;

        private StatsCallback(TbQueueCallback callback, MessagesStats stats) {
            this.callback = callback;
            this.stats = stats;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            stats.incrementSuccessful();
            if (callback != null)
                callback.onSuccess(metadata);
        }

        @Override
        public void onFailure(Throwable t) {
            stats.incrementFailed();
            if (callback != null)
                callback.onFailure(t);
        }
    }

    private class MsgPackCallback implements TbQueueCallback {
        private final AtomicInteger msgCount;
        private final TransportServiceCallback<Void> callback;

        public MsgPackCallback(Integer msgCount, TransportServiceCallback<Void> callback) {
            this.msgCount = new AtomicInteger(msgCount);
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (msgCount.decrementAndGet() <= 0) {
                DefaultTransportService.this.transportCallbackExecutor.submit(() -> callback.onSuccess(null));
            }
        }

        @Override
        public void onFailure(Throwable t) {
            DefaultTransportService.this.transportCallbackExecutor.submit(() -> callback.onError(t));
        }
    }

    private class ApiStatsProxyCallback<T> implements TransportServiceCallback<T> {
        private final TenantId tenantId;
        private final CustomerId customerId;
        private final int dataPoints;
        private final TransportServiceCallback<T> callback;

        public ApiStatsProxyCallback(TenantId tenantId, CustomerId customerId, int dataPoints, TransportServiceCallback<T> callback) {
            this.tenantId = tenantId;
            this.customerId = customerId;
            this.dataPoints = dataPoints;
            this.callback = callback;
        }

        @Override
        public void onSuccess(T msg) {
            try {
                apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_MSG_COUNT, 1);
                apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_DP_COUNT, dataPoints);
            } finally {
                callback.onSuccess(msg);
            }
        }

        @Override
        public void onError(Throwable e) {
            callback.onError(e);
        }
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return transportCallbackExecutor;
    }

    @Override
    public boolean hasSession(TransportProtos.SessionInfoProto sessionInfo) {
        return sessions.containsKey(toSessionId(sessionInfo));
    }

    @Override
    public void createGaugeStats(String statsName, AtomicInteger number) {
        statsFactory.createGauge(StatsType.TRANSPORT + "." + statsName, number);
        statsMap.put(statsName, number);
    }

    @Scheduled(fixedDelayString = "${transport.stats.print-interval-ms:60000}")
    public void printStats() {
        if (statsEnabled && !statsMap.isEmpty()) {
            String values = statsMap.entrySet().stream()
                    .map(kv -> kv.getKey() + " [" + kv.getValue() + "]").collect(Collectors.joining(", "));
            log.info("Transport Stats: {}", values);
        }
    }
}
