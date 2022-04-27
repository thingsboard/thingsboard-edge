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
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.gcloud.pubsub.PubSubIntegration;
import org.thingsboard.integration.apache.pulsar.basic.BasicPulsarIntegration;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationStatistics;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.api.data.DefaultIntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.aws.kinesis.AwsKinesisIntegration;
import org.thingsboard.integration.aws.sqs.AwsSqsIntegration;
import org.thingsboard.integration.azure.AzureEventHubIntegration;
import org.thingsboard.integration.coap.CoapIntegration;
import org.thingsboard.integration.http.basic.BasicHttpIntegration;
import org.thingsboard.integration.http.chirpstack.ChirpStackIntegration;
import org.thingsboard.integration.http.loriot.LoriotIntegration;
import org.thingsboard.integration.http.oc.OceanConnectIntegration;
import org.thingsboard.integration.http.sigfox.SigFoxIntegration;
import org.thingsboard.integration.http.thingpark.ThingParkIntegration;
import org.thingsboard.integration.http.thingpark.ThingParkIntegrationEnterprise;
import org.thingsboard.integration.http.tmobile.TMobileIotCdpIntegration;
import org.thingsboard.integration.kafka.basic.BasicKafkaIntegration;
import org.thingsboard.integration.mqtt.aws.AwsIotIntegration;
import org.thingsboard.integration.mqtt.azure.AzureIotHubIntegration;
import org.thingsboard.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.integration.mqtt.ibm.IbmWatsonIotIntegration;
import org.thingsboard.integration.mqtt.ttn.TtnIntegration;
import org.thingsboard.integration.opcua.OpcUaIntegration;
import org.thingsboard.integration.rabbitmq.basic.BasicRabbitMQIntegration;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.integration.rpc.IntegrationRpcService;
import org.thingsboard.server.service.profile.DefaultTbDeviceProfileCache;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.utils.EventDeduplicationExecutor;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ashvayka on 02.12.17.
 */
@Slf4j
@TbCoreComponent
@Service
@Data
public class DefaultPlatformIntegrationService extends TbApplicationEventListener<PartitionChangeEvent> implements PlatformIntegrationService {

    private static final ReentrantLock entityCreationLock = new ReentrantLock();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private DataConverterService dataConverterService;

    @Autowired
    protected IntegrationContextComponent contextComponent;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    private EventService eventService;

    @Autowired
    @Lazy
    private TbQueueProducerProvider producerProvider;

    @Autowired
    @Lazy
    private DataDecodingEncodingService encodingService;

    @Autowired
    @Lazy
    private ActorSystemContext actorContext;

    @Autowired
    private TelemetrySubscriptionService telemetrySubscriptionService;

    @Autowired
    private IntegrationRpcService integrationRpcService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DeviceStateService deviceStateService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private DbCallbackExecutorService callbackExecutorService;

    @Autowired
    private TbApiUsageReportClient apiUsageReportClient;

    @Autowired
    private DefaultTbDeviceProfileCache deviceProfileCache;

    @Autowired(required = false)
    private CoapServerService coapServerService;

    @Value("${transport.rate_limits.enabled}")
    private boolean rateLimitEnabled;

    @Value("${transport.rate_limits.tenant}")
    private String perTenantLimitsConf;

    @Value("${transport.rate_limits.tenant}")
    private String perDevicesLimitsConf;

    @Value("${integrations.reinit.enabled:false}")
    private boolean reinitEnabled;

    @Value("${integrations.reinit.frequency:3600000}")
    private long reinitFrequency;

    @Value("${integrations.statistics.enabled}")
    private boolean statisticsEnabled;

    @Value("${integrations.statistics.persist_frequency}")
    private long statisticsPersistFrequency;

    @Value("${integrations.allow_Local_network_hosts:true}")
    private boolean allowLocalNetworkHosts;

    @Value("${integrations.allow_resource_intensive:true}")
    private boolean allowResourceIntensive;

    private ScheduledExecutorService statisticsExecutorService;
    private ScheduledExecutorService reinitExecutorService;
    private ListeningExecutorService refreshExecutorService;
    private ExecutorService callbackExecutor;

    private final Gson gson = new Gson();
    private final ConcurrentMap<IntegrationId, ComponentLifecycleEvent> integrationEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<IntegrationId, Pair<ThingsboardPlatformIntegration<?>, IntegrationContext>> integrationsByIdMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ThingsboardPlatformIntegration<?>> integrationsByRoutingKeyMap = new ConcurrentHashMap<>();
    private volatile EventDeduplicationExecutor<Set<TopicPartitionInfo>> deduplicationExecutor;
    private volatile Set<TopicPartitionInfo> myPartitions = ConcurrentHashMap.newKeySet();

    private ConcurrentMap<TenantId, TbRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private ConcurrentMap<DeviceId, TbRateLimits> perDeviceLimits = new ConcurrentHashMap<>();
    private boolean initialized;

    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> ruleEngineMsgProducer;
    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreMsgProducer;
    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> integrationRuleEngineMsgProducer;

    @PostConstruct
    public void init() {
        ruleEngineMsgProducer = producerProvider.getRuleEngineMsgProducer();
        tbCoreMsgProducer = producerProvider.getTbCoreMsgProducer();
        integrationRuleEngineMsgProducer = producerProvider.getIntegrationRuleEngineMsgProducer();
        this.callbackExecutor = ThingsBoardExecutors.newWorkStealingPool(20, "default-integration-callback");
        refreshExecutorService = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(4, "default-integration-refresh"));
        deduplicationExecutor = new EventDeduplicationExecutor<>(DefaultPlatformIntegrationService.class.getSimpleName(), refreshExecutorService, this::refreshAllIntegrations);
        if (reinitEnabled) {
            reinitExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("default-integration-reinit"));
            reinitExecutorService.scheduleAtFixedRate(this::reInitIntegrations, reinitFrequency, reinitFrequency, TimeUnit.MILLISECONDS);
        }
        if (statisticsEnabled) {
            statisticsExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("default-integration-stats"));
            statisticsExecutorService.scheduleAtFixedRate(this::persistStatistics, statisticsPersistFrequency, statisticsPersistFrequency, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    public void destroy() {
        if (statisticsEnabled) {
            statisticsExecutorService.shutdown();
        }
        if (reinitEnabled) {
            reinitExecutorService.shutdownNow();
        }
        integrationsByIdMap.values().forEach(v -> v.getFirst().destroy());
        integrationsByIdMap.clear();
        integrationsByRoutingKeyMap.clear();
        refreshExecutorService.shutdownNow();
    }

    @Override
    public void validateIntegrationConfiguration(Integration integration) {
        if (StringUtils.isEmpty(integration.getName())) {
            throw new DataValidationException("Integration name should be specified!");
        }
        if (integration.getType() == null) {
            throw new DataValidationException("Integration type should be specified!");
        }
        if (StringUtils.isEmpty(integration.getRoutingKey())) {
            throw new DataValidationException("Integration routing key should be specified!");
        }
        if (!integration.getType().isRemoteOnly()) {
            ThingsboardPlatformIntegration platformIntegration = createThingsboardPlatformIntegration(integration);
            platformIntegration.validateConfiguration(integration, allowLocalNetworkHosts);
        }
    }

    @Override
    public void checkIntegrationConnection(Integration integration) throws Exception {
        ThingsboardPlatformIntegration platformIntegration = createThingsboardPlatformIntegration(integration);
        platformIntegration.checkConnection(integration, new LocalIntegrationContext(contextComponent, integration));
    }

    @Override
    public ListenableFuture<ThingsboardPlatformIntegration> createIntegration(Integration configuration) {
        return createIntegration(configuration, false);
    }

    private ListenableFuture<ThingsboardPlatformIntegration> createIntegration(Integration configuration, boolean forceReinit) {
        if (configuration.getType().isSingleton()) {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, configuration.getTenantId(), configuration.getId());
            if (!myPartitions.contains(tpi)) {
                return Futures.immediateFailedFuture(new ThingsboardException("Singleton integration already present on another node!", ThingsboardErrorCode.INVALID_ARGUMENTS));
            }
        }
        if (configuration.isRemote()) {
            return Futures.immediateFailedFuture(new ThingsboardException("The integration is executed remotely!", ThingsboardErrorCode.INVALID_ARGUMENTS));
        }
        if (!configuration.isEnabled()) {
            return Futures.immediateFailedFuture(new ThingsboardException("The integration is disabled!", ThingsboardErrorCode.INVALID_ARGUMENTS));
        }
        return refreshExecutorService.submit(() -> getOrCreateThingsBoardPlatformIntegration(configuration, forceReinit));
    }


    @Override
    public void updateIntegration(Integration configuration) {
        if (configuration.isRemote()) {
            integrationsByIdMap.remove(configuration.getId());
            integrationsByRoutingKeyMap.remove(configuration.getRoutingKey());
            integrationRpcService.updateIntegration(configuration);
        } else {
            if (configuration.getType().isSingleton()) {
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, configuration.getTenantId(), configuration.getId());
                if (!myPartitions.contains(tpi)) {
                    return;
                }
            }
            if (configuration.isEnabled()) {
                refreshExecutorService.submit(() -> {
                    Pair<ThingsboardPlatformIntegration<?>, IntegrationContext> integration = integrationsByIdMap.get(configuration.getId());
                    if (integration != null) {
                        synchronized (integration) {
                            try {
                                IntegrationContext newCtx = new LocalIntegrationContext(contextComponent, configuration);
                                integrationsByIdMap.put(configuration.getId(), Pair.of(integration.getFirst(), newCtx));
                                integration.getFirst().update(new TbIntegrationInitParams(newCtx,
                                        configuration, getUplinkDataConverter(configuration), getDownlinkDataConverter(configuration)));
                                actorContext.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.UPDATED, null);
                                integrationEvents.put(configuration.getId(), ComponentLifecycleEvent.UPDATED);
                            } catch (Exception e) {
                                integration.getFirst().destroy();
                                integrationEvents.put(configuration.getId(), ComponentLifecycleEvent.FAILED);
                                actorContext.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.FAILED, e);
                            }
                        }
                    } else {
                        getOrCreateThingsBoardPlatformIntegration(configuration, false);
                    }
                });
            } else {
                stopIntegration(configuration.getId(), ComponentLifecycleEvent.STOPPED);
            }
        }
    }

    @Override
    public ListenableFuture<Void> deleteIntegration(IntegrationId integrationId) {
        return stopIntegration(integrationId, ComponentLifecycleEvent.DELETED);
    }

    private ListenableFuture<Void> stopIntegration(IntegrationId integrationId, ComponentLifecycleEvent event) {
        return refreshExecutorService.submit(() -> {
            Pair<ThingsboardPlatformIntegration<?>, IntegrationContext> integration = integrationsByIdMap.remove(integrationId);
            integrationEvents.remove(integrationId);
            if (integration != null) {
                Integration configuration = integration.getFirst().getConfiguration();
                try {
                    integrationsByRoutingKeyMap.remove(configuration.getRoutingKey());
                    integration.getFirst().destroy();
                    actorContext.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), event, null);
                } catch (Exception e) {
                    actorContext.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), event, e);
                    throw e;
                }
            }
            return null;
        });
    }

    @Override
    public ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String key) {
        ThingsboardPlatformIntegration<?> result = integrationsByRoutingKeyMap.get(key);
        if (result == null) {
            Optional<Integration> configurationOpt = integrationService.findIntegrationByRoutingKey(TenantId.SYS_TENANT_ID, key);
            if (configurationOpt.isPresent()) {
                Integration integration = configurationOpt.get();
                if (integration.isRemote()) {
                    return Futures.immediateFailedFuture(new ThingsboardException("The integration is executed remotely!", ThingsboardErrorCode.INVALID_ARGUMENTS));
                }
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, integration.getTenantId(), integration.getId());
                if (integration.getType().isSingleton() && !myPartitions.contains(tpi)) {
                    return Futures.immediateFailedFuture(new ThingsboardException("Singleton integration already present on another node!", ThingsboardErrorCode.INVALID_ARGUMENTS));
                }
                if (!integration.isEnabled()) {
                    return Futures.immediateFailedFuture(new ThingsboardException("The integration is disabled!", ThingsboardErrorCode.INVALID_ARGUMENTS));
                }
                return Futures.immediateFailedFuture(new ThingsboardException("Integration is not present in routing key map!", ThingsboardErrorCode.GENERAL));
            } else {
                return Futures.immediateFailedFuture(new ThingsboardException("Failed to find integration by routing key!", ThingsboardErrorCode.ITEM_NOT_FOUND));
            }
        } else {
            return Futures.immediateFuture(result);
        }
    }

    @Override
    public void onQueueMsg(TransportProtos.IntegrationDownlinkMsgProto msgProto, TbCallback callback) {
        try {
            TenantId tenantId = new TenantId(new UUID(msgProto.getTenantIdMSB(), msgProto.getTenantIdLSB()));
            IntegrationId integrationId = new IntegrationId(new UUID(msgProto.getIntegrationIdMSB(), msgProto.getIntegrationIdLSB()));
            IntegrationDownlinkMsg msg = new DefaultIntegrationDownlinkMsg(tenantId, integrationId, TbMsg.fromBytes(ServiceQueue.MAIN, msgProto.getData().toByteArray(), TbMsgCallback.EMPTY), null);
            Pair<ThingsboardPlatformIntegration<?>, IntegrationContext> integration = integrationsByIdMap.get(integrationId);
            if (integration == null) {
                boolean remoteIntegrationDownlink = integrationRpcService.handleRemoteDownlink(msg);
                if (!remoteIntegrationDownlink) {
                    Integration configuration = integrationService.findIntegrationById(TenantId.SYS_TENANT_ID, integrationId);
                    DonAsynchron.withCallback(createIntegration(configuration), i -> {
                        onMsg(i, msg);
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }, e -> {
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }, refreshExecutorService);
                    return;
                }
            } else {
                onMsg(integration.getFirst(), msg);
            }
            if (callback != null) {
                callback.onSuccess();
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            }
            throw handleException(e);
        }
    }

    private void onMsg(ThingsboardPlatformIntegration<?> integration, IntegrationDownlinkMsg msg) {
        if (!integrationEvents.getOrDefault(msg.getIntegrationId(), ComponentLifecycleEvent.FAILED).equals(ComponentLifecycleEvent.FAILED)) {
            integration.onDownlinkMsg(msg);
        }
    }

    private void persistStatistics() {
        String serviceId = serviceInfoProvider.getServiceId();
        integrationsByIdMap.forEach((id, integration) -> {
            long ts = System.currentTimeMillis();
            IntegrationStatistics statistics = integration.getFirst().popStatistics();
            try {
                Event event = new Event();
                event.setEntityId(id);
                event.setTenantId(integration.getFirst().getConfiguration().getTenantId());
                event.setType(DataConstants.STATS);
                event.setBody(toBodyJson(serviceId, statistics.getMessagesProcessed(), statistics.getErrorsOccurred()));
                eventService.saveAsync(event);

                ComponentLifecycleEvent latestEvent = integrationEvents.get(id);

                List<TsKvEntry> statsTs = new ArrayList<>();
                statsTs.add(new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_messagesCount", statistics.getMessagesProcessed())));
                statsTs.add(new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_errorsCount", statistics.getErrorsOccurred())));
                statsTs.add(new BasicTsKvEntry(ts, new StringDataEntry(serviceId + "_state", latestEvent != null ? latestEvent.name() : "N/A")));
                telemetrySubscriptionService.saveAndNotifyInternal(integration.getFirst().getConfiguration().getTenantId(), id, statsTs, new FutureCallback<Integer>() {
                    @Override
                    public void onSuccess(@Nullable Integer result) {
                        log.trace("[{}] Persisted statistics telemetry: {}", id, statistics);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}] Failed to persist statistics telemetry: {}", id, statistics, t);
                    }
                });
            } catch (Exception e) {
                log.warn("[{}] Failed to persist statistics: {}", id, statistics, e);
            }
        });
    }

    private JsonNode toBodyJson(String serviceId, long messagesProcessed, long errorsOccurred) {
        return mapper.createObjectNode().put("server", serviceId).put("messagesProcessed", messagesProcessed).put("errorsOccurred", errorsOccurred);
    }

    private TBUplinkDataConverter getUplinkDataConverter(Integration integration) {
        return dataConverterService.getUplinkConverterById(integration.getTenantId(), integration.getDefaultConverterId())
                .orElseThrow(() -> new ThingsboardRuntimeException("Converter not found!", ThingsboardErrorCode.ITEM_NOT_FOUND));
    }

    private TBDownlinkDataConverter getDownlinkDataConverter(Integration integration) {
        return dataConverterService.getDownlinkConverterById(integration.getTenantId(), integration.getDownlinkConverterId())
                .orElse(null);
    }

    private RuntimeException handleException(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            deduplicationExecutor.submit(partitionChangeEvent.getPartitions());
        }
    }

    @Override
    public void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, IntegrationCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            reportActivity(sessionInfo);
            TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            int dataPoints = 0;
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                dataPoints += tsKv.getKvCount();
            }
            MsgPackCallback packCallback = new MsgPackCallback(msg.getTsKvListCount(), new ApiStatsProxyCallback<>(tenantId, getCustomerId(sessionInfo), dataPoints, callback));
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                TbMsgMetaData metaData = new TbMsgMetaData();
                metaData.putValue("deviceName", sessionInfo.getDeviceName());
                metaData.putValue("deviceType", sessionInfo.getDeviceType());
                metaData.putValue("ts", tsKv.getTs() + "");
                JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
                sendToRuleEngine(tenantId, deviceId, sessionInfo, json, metaData, SessionMsgType.POST_TELEMETRY_REQUEST, packCallback);
            }
        }
    }

    @Override
    public void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, IntegrationCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            reportActivity(sessionInfo);
            TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("deviceName", sessionInfo.getDeviceName());
            metaData.putValue("deviceType", sessionInfo.getDeviceType());
            sendToRuleEngine(tenantId, deviceId, sessionInfo, json, metaData, SessionMsgType.POST_ATTRIBUTES_REQUEST,
                    new IntegrationTbQueueCallback(new ApiStatsProxyCallback<>(tenantId, getCustomerId(sessionInfo), msg.getKvList().size(), callback)));
        }
    }

    @Override
    public void process(TenantId tenantId, TbMsg tbMsg, IntegrationCallback<Void> callback) {
        sendToRuleEngine(tenantId, tbMsg, new IntegrationTbQueueCallback(new ApiStatsProxyCallback<>(tenantId, tbMsg.getCustomerId(), 1, callback)));
    }


    @Override
    public Device getOrCreateDevice(Integration integration, String deviceName, String deviceType, String deviceLabel, String customerName, String groupName) {
        Device device = deviceService.findDeviceByTenantIdAndName(integration.getTenantId(), deviceName);
        if (device == null) {
            entityCreationLock.lock();
            try {
                return processGetOrCreateDevice(integration, deviceName, deviceType, deviceLabel, customerName, groupName);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return device;
    }

    @Override
    public Asset getOrCreateAsset(Integration integration, String assetName, String assetType, String assetLabel, String customerName, String groupName) {
        Asset asset = assetService.findAssetByTenantIdAndName(integration.getTenantId(), assetName);
        if (asset == null) {
            entityCreationLock.lock();
            try {
                return processGetOrCreateAsset(integration, assetName, assetType, assetLabel, customerName, groupName);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return asset;
    }

    @Override
    public EntityView getOrCreateEntityView(Integration configuration, Device device, EntityViewDataProto proto) {
        String entityViewName = proto.getViewName();
        EntityView entityView = entityViewService.findEntityViewByTenantIdAndName(configuration.getTenantId(), entityViewName);
        if (entityView == null) {
            entityCreationLock.lock();
            try {
                entityView = entityViewService.findEntityViewByTenantIdAndName(configuration.getTenantId(), entityViewName);
                if (entityView == null) {
                    entityView = new EntityView();
                    entityView.setName(entityViewName);
                    entityView.setType(proto.getViewType());
                    entityView.setTenantId(configuration.getTenantId());
                    entityView.setEntityId(device.getId());

                    TelemetryEntityView telemetryEntityView = new TelemetryEntityView();
                    telemetryEntityView.setTimeseries(proto.getTelemetryKeysList());
                    entityView.setKeys(telemetryEntityView);

                    entityView = entityViewService.saveEntityView(entityView);
                    createRelationFromIntegration(configuration, entityView.getId());
                }
            } finally {
                entityCreationLock.unlock();
            }
        }
        return entityView;
    }

    private Device processGetOrCreateDevice(Integration integration, String deviceName, String deviceType, String deviceLabel, String customerName, String groupName) {
        Device device = deviceService.findDeviceByTenantIdAndName(integration.getTenantId(), deviceName);
        if (device == null && integration.isAllowCreateDevicesOrAssets()) {
            device = new Device();
            device.setName(deviceName);
            device.setType(deviceType);
            device.setTenantId(integration.getTenantId());
            if (!StringUtils.isEmpty(deviceLabel)) {
                device.setLabel(deviceLabel);
            }
            if (!StringUtils.isEmpty(customerName)) {
                Customer customer = getOrCreateCustomer(integration, customerName);
                device.setCustomerId(customer.getId());
            }

            device = deviceService.saveDevice(device);

            if (!StringUtils.isEmpty(groupName)) {
                addEntityToEntityGroup(groupName, integration, device.getId(), device.getOwnerId(), device.getEntityType());
            }

            createRelationFromIntegration(integration, device.getId());
            clusterService.onDeviceUpdated(device, null);
            pushDeviceCreatedEventToRuleEngine(integration, device);
        } else {
            throw new ThingsboardRuntimeException("Creating devices is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return device;
    }

    private Asset processGetOrCreateAsset(Integration integration, String assetName, String assetType, String assetLabel, String customerName, String groupName) {
        Asset asset = assetService.findAssetByTenantIdAndName(integration.getTenantId(), assetName);
        if (asset == null && integration.isAllowCreateDevicesOrAssets()) {
            asset = new Asset();
            asset.setName(assetName);
            asset.setType(assetType);
            asset.setTenantId(integration.getTenantId());
            if (!StringUtils.isEmpty(assetLabel)) {
                asset.setLabel(assetLabel);
            }
            if (!StringUtils.isEmpty(customerName)) {
                Customer customer = getOrCreateCustomer(integration, customerName);
                asset.setCustomerId(customer.getId());
            }
            asset = assetService.saveAsset(asset);

            if (!StringUtils.isEmpty(groupName)) {
                addEntityToEntityGroup(groupName, integration, asset.getId(), asset.getOwnerId(), asset.getEntityType());
            }

            createRelationFromIntegration(integration, asset.getId());
            pushAssetCreatedEventToRuleEngine(integration, asset);
        } else {
            throw new ThingsboardRuntimeException("Creating assets is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return asset;
    }

    private Customer getOrCreateCustomer(Integration integration, String customerName) {
        Customer customer;
        Optional<Customer> customerOptional = customerService.findCustomerByTenantIdAndTitle(integration.getTenantId(), customerName);
        if (customerOptional.isPresent()) {
            customer = customerOptional.get();
        } else {
            customer = new Customer();
            customer.setTitle(customerName);
            customer.setTenantId(integration.getTenantId());
            customer = customerService.saveCustomer(customer);
            pushCustomerCreatedEventToRuleEngine(integration, customer);
        }
        return customer;
    }

    private void addEntityToEntityGroup(String groupName, Integration integration, EntityId entityId, EntityId parentId, EntityType entityType) {
        TenantId tenantId = integration.getTenantId();
        ListenableFuture<Optional<EntityGroup>> futureEntityGroup = entityGroupService
                .findEntityGroupByTypeAndNameAsync(tenantId, parentId, entityType, groupName);

        DonAsynchron.withCallback(futureEntityGroup, optionalEntityGroup -> {
            EntityGroup entityGroup =
                    optionalEntityGroup.orElseGet(() -> createEntityGroup(groupName, parentId, entityType, tenantId));
            pushEntityGroupCreatedEventToRuleEngine(integration, entityGroup);
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroup.getId(), entityId);
        }, throwable -> log.warn("[{}][{}] Failed to find entity group: {}:{}", tenantId, parentId, entityType, groupName, throwable), callbackExecutorService);
    }

    private EntityGroup createEntityGroup(String entityGroupName, EntityId parentEntityId, EntityType entityType, TenantId tenantId) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(entityGroupName);
        entityGroup.setType(entityType);
        return entityGroupService.saveEntityGroup(tenantId, parentEntityId, entityGroup);
    }

    private void createRelationFromIntegration(Integration integration, EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(integration.getId());
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.INTEGRATION_TYPE);
        relationService.saveRelation(integration.getTenantId(), relation);
    }

    private void pushDeviceCreatedEventToRuleEngine(Integration integration, Device device) {
        try {
            DeviceProfile deviceProfile = deviceProfileCache.find(device.getDeviceProfileId());
            RuleChainId ruleChainId;
            String queueName;

            if (deviceProfile == null) {
                ruleChainId = null;
                queueName = ServiceQueue.MAIN;
            } else {
                ruleChainId = deviceProfile.getDefaultRuleChainId();
                String defaultQueueName = deviceProfile.getDefaultQueueName();
                queueName = defaultQueueName != null ? defaultQueueName : ServiceQueue.MAIN;
            }

            ObjectNode entityNode = mapper.valueToTree(device);
            TbMsg tbMsg = TbMsg.newMsg(queueName, DataConstants.ENTITY_CREATED, device.getId(), deviceActionTbMsgMetaData(integration, device),
                    mapper.writeValueAsString(entityNode), ruleChainId, null);

            process(device.getTenantId(), tbMsg, null);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private void pushAssetCreatedEventToRuleEngine(Integration integration, Asset asset) {
        try {
            ObjectNode entityNode = mapper.valueToTree(asset);
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, asset.getId(), asset.getCustomerId(), assetActionTbMsgMetaData(integration, asset), mapper.writeValueAsString(entityNode));
            process(integration.getTenantId(), tbMsg, null);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push asset action to rule engine: {}", asset.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }


    private void pushEntityGroupCreatedEventToRuleEngine(Integration integration, EntityGroup entityGroup) {
        try {
            ObjectNode entityNode = mapper.valueToTree(entityGroup);
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, entityGroup.getId(), getTbMsgMetaData(integration), mapper.writeValueAsString(entityNode));
            process(integration.getTenantId(), tbMsg, null);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push entityGroup action to rule engine: {}", entityGroup.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private void pushCustomerCreatedEventToRuleEngine(Integration integration, Customer customer) {
        try {
            ObjectNode entityNode = mapper.valueToTree(customer);
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, customer.getId(), customer.getParentCustomerId(), getTbMsgMetaData(integration), mapper.writeValueAsString(entityNode));
            process(customer.getTenantId(), tbMsg, null);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push customer action to rule engine: {}", customer.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private TbMsgMetaData deviceActionTbMsgMetaData(Integration integration, Device device) {
        return getActionTbMsgMetaData(integration, device.getCustomerId());
    }

    private TbMsgMetaData assetActionTbMsgMetaData(Integration integration, Asset asset) {
        return getActionTbMsgMetaData(integration, asset.getCustomerId());
    }

    private TbMsgMetaData getActionTbMsgMetaData(Integration integration, CustomerId customerId) {
        TbMsgMetaData metaData = getTbMsgMetaData(integration);
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    private TbMsgMetaData getTbMsgMetaData(Integration integration) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("integrationId", integration.getId().toString());
        metaData.putValue("integrationName", integration.getName());
        return metaData;
    }

    private void reportActivity(SessionInfoProto sessionInfo) {
        TransportProtos.SubscriptionInfoProto subscriptionInfoProto = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(false).setRpcSubscription(false)
                .setLastActivityTime(System.currentTimeMillis()).build();
        TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                .setSubscriptionInfo(subscriptionInfoProto).build();
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, getTenantId(sessionInfo), getDeviceId(sessionInfo));
        tbCoreMsgProducer.send(tpi, new TbProtoQueueMsg<>(getRoutingKey(sessionInfo),
                TransportProtos.ToCoreMsg.newBuilder().setToDeviceActorMsg(msg).build()), null);
    }

    private void sendToRuleEngine(TenantId tenantId, DeviceId deviceId, TransportProtos.SessionInfoProto sessionInfo, JsonObject json,
                                  TbMsgMetaData metaData, SessionMsgType sessionMsgType, TbQueueCallback callback) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));

        DeviceProfile deviceProfile = deviceProfileCache.get(tenantId, deviceProfileId);
        RuleChainId ruleChainId;
        String queueName;

        if (deviceProfile == null) {
            log.warn("[{}] Device profile is null!", deviceProfileId);
            ruleChainId = null;
            queueName = ServiceQueue.MAIN;
        } else {
            ruleChainId = deviceProfile.getDefaultRuleChainId();
            String defaultQueueName = deviceProfile.getDefaultQueueName();
            queueName = defaultQueueName != null ? defaultQueueName : ServiceQueue.MAIN;
        }

        TbMsg tbMsg = TbMsg.newMsg(queueName, sessionMsgType.name(), deviceId, getCustomerId(sessionInfo), metaData, gson.toJson(json), ruleChainId, null);

        sendToRuleEngine(tenantId, tbMsg, callback);
    }

    private void sendToRuleEngine(TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, tbMsg.getOriginator());
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder().setTbMsg(TbMsg.toByteString(tbMsg))
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
        integrationRuleEngineMsgProducer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
    }

    protected UUID getRoutingKey(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB());
    }

    protected TenantId getTenantId(TransportProtos.SessionInfoProto sessionInfo) {
        return new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
    }

    protected DeviceId getDeviceId(TransportProtos.SessionInfoProto sessionInfo) {
        return new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
    }

    private boolean checkLimits(SessionInfoProto sessionInfo, Object msg, IntegrationCallback<Void> callback) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", toId(sessionInfo), msg);
        }
        if (!rateLimitEnabled) {
            return true;
        }
        TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
        TbRateLimits rateLimits = perTenantLimits.computeIfAbsent(tenantId, id -> new TbRateLimits(perTenantLimitsConf));
        if (!rateLimits.tryConsume()) {
            if (callback != null) {
                callback.onError(new TbRateLimitsException(EntityType.TENANT));
            }
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Tenant level rate limit detected: {}", toId(sessionInfo), tenantId, msg);
            }
            return false;
        }
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        rateLimits = perDeviceLimits.computeIfAbsent(deviceId, id -> new TbRateLimits(perDevicesLimitsConf));
        if (!rateLimits.tryConsume()) {
            if (callback != null) {
                callback.onError(new TbRateLimitsException(EntityType.DEVICE));
            }
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Device level rate limit detected: {}", toId(sessionInfo), deviceId, msg);
            }
            return false;
        }
        return true;
    }

    private UUID toId(SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    private synchronized void refreshAllIntegrations(Set<TopicPartitionInfo> partitions) {
        Set<TopicPartitionInfo> newPartitions = ConcurrentHashMap.newKeySet();
        newPartitions.addAll(partitions);
        myPartitions = newPartitions;
        Set<IntegrationId> currentIntegrationIds = new HashSet<>(integrationsByIdMap.keySet());
        for (IntegrationId integrationId : currentIntegrationIds) {
            Integration integration = integrationsByIdMap.get(integrationId).getFirst().getConfiguration();
            if (integration.getType().isSingleton()) {
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, integration.getTenantId(), integration.getId());
                if (!myPartitions.contains(tpi)) {
                    stopIntegration(integrationId, ComponentLifecycleEvent.STOPPED);
                }
            }
        }

        List<Integration> allIntegrations = integrationService.findAllIntegrations(TenantId.SYS_TENANT_ID);
        try {
            List<ListenableFuture<?>> futures = Lists.newArrayList();
            for (Integration integration : allIntegrations) {
                try {
                    //Initialize the integration that belongs to current node only
                    if (!integration.isRemote()) {
                        if (!integration.getType().isSingleton()) {
                            futures.add(createIntegration(integration));
                        } else {
                            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, integration.getTenantId(), integration.getId());
                            if (myPartitions.contains(tpi)) {
                                futures.add(createIntegration(integration));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.info("[{}] Unable to initialize integration {}", integration.getId(), integration.getName(), e);
                }
            }
            Futures.successfulAsList(futures).get();
            log.info("{} Integrations refreshed", futures.size());
        } catch (Throwable th) {
            log.error("Could not init integrations", th);
        }
        initialized = true;
    }

    private void reInitIntegrations() {
        if (initialized) {
            integrationsByIdMap.forEach((integrationId, integration) -> {
                if (integrationEvents.getOrDefault(integrationId, ComponentLifecycleEvent.STARTED).equals(ComponentLifecycleEvent.FAILED)) {
                    DonAsynchron.withCallback(createIntegration(integration.getFirst().getConfiguration(), true),
                            tmp -> log.debug("[{}] Re-initialized the integration {}", integration.getFirst().getConfiguration().getId(), integration.getFirst().getConfiguration().getName()),
                            e -> log.info("[{}] Unable to initialize integration {}", integration.getFirst().getConfiguration().getId(), integration.getFirst().getConfiguration().getName(), e));
                }
            });
        }
    }

    private ThingsboardPlatformIntegration<?> getOrCreateThingsBoardPlatformIntegration(Integration configuration, boolean forceReinit) {
        Pair<ThingsboardPlatformIntegration<?>, IntegrationContext> integrationPair;
        boolean newIntegration = false;
        synchronized (integrationsByIdMap) {
            integrationPair = integrationsByIdMap.get(configuration.getId());
            if (integrationPair == null) {
                IntegrationContext context = new LocalIntegrationContext(contextComponent, configuration);
                ThingsboardPlatformIntegration<?> integration = newIntegration(context, configuration);
                integrationPair = Pair.of(integration, context);
                integrationsByIdMap.put(configuration.getId(), integrationPair);
                integrationsByRoutingKeyMap.putIfAbsent(configuration.getRoutingKey(), integration);
                newIntegration = true;
            }
        }

        if (newIntegration || forceReinit) {
            synchronized (integrationPair) {
                try {
                    integrationPair.getFirst().init(new TbIntegrationInitParams(integrationPair.getSecond(), configuration, getUplinkDataConverter(configuration), getDownlinkDataConverter(configuration)));
                    actorContext.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.STARTED, null);
                    integrationEvents.put(configuration.getId(), ComponentLifecycleEvent.STARTED);
                } catch (Exception e) {
                    integrationPair.getFirst().destroy();
                    integrationEvents.put(configuration.getId(), ComponentLifecycleEvent.FAILED);
                    actorContext.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.FAILED, e);
                    throw handleException(e);
                }
            }
        }
        return integrationPair.getFirst();
    }

    private ThingsboardPlatformIntegration<?> newIntegration(IntegrationContext ctx, Integration configuration) {
        ThingsboardPlatformIntegration<?> platformIntegration = createThingsboardPlatformIntegration(configuration);
        platformIntegration.validateConfiguration(configuration, allowLocalNetworkHosts);
        return platformIntegration;
    }

    private ThingsboardPlatformIntegration<?> createThingsboardPlatformIntegration(Integration integration) {
        switch (integration.getType()) {
            case HTTP:
                return new BasicHttpIntegration();
            case LORIOT:
                return new LoriotIntegration();
            case SIGFOX:
                return new SigFoxIntegration();
            case OCEANCONNECT:
                return new OceanConnectIntegration();
            case THINGPARK:
                return new ThingParkIntegration();
            case TPE:
                return new ThingParkIntegrationEnterprise();
            case TMOBILE_IOT_CDP:
                return new TMobileIotCdpIntegration();
            case MQTT:
                return new BasicMqttIntegration();
            case PUB_SUB:
                return new PubSubIntegration();
            case AWS_IOT:
                return new AwsIotIntegration();
            case AWS_SQS:
                return new AwsSqsIntegration();
            case IBM_WATSON_IOT:
                return new IbmWatsonIotIntegration();
            case TTN:
            case TTI:
                return new TtnIntegration();
            case CHIRPSTACK:
                return new ChirpStackIntegration();
            case AZURE_EVENT_HUB:
                return new AzureEventHubIntegration();
            case AZURE_IOT_HUB:
                return new AzureIotHubIntegration();
            case OPC_UA:
                return new OpcUaIntegration();
            case AWS_KINESIS:
                return new AwsKinesisIntegration();
            case KAFKA:
                return new BasicKafkaIntegration();
            case RABBITMQ:
                return new BasicRabbitMQIntegration();
            case APACHE_PULSAR:
                return new BasicPulsarIntegration();
            case COAP:
                return new CoapIntegration(coapServerService);
            case CUSTOM:
            case TCP:
            case UDP:
                throw new RuntimeException("Custom Integrations should be executed remotely!");
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private class IntegrationTbQueueCallback implements TbQueueCallback {
        private final IntegrationCallback<Void> callback;

        private IntegrationTbQueueCallback(IntegrationCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            });
        }

        @Override
        public void onFailure(Throwable t) {
            DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> {
                if (callback != null) {
                    callback.onError(t);
                }
            });
        }
    }

    private class MsgPackCallback implements TbQueueCallback {
        private final AtomicInteger msgCount;
        private final IntegrationCallback<Void> callback;

        public MsgPackCallback(Integer msgCount, IntegrationCallback<Void> callback) {
            this.msgCount = new AtomicInteger(msgCount);
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (msgCount.decrementAndGet() <= 0 && callback != null) {
                DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> callback.onSuccess(null));
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (callback != null) {
                callback.onError(t);
            }
        }
    }

    private class ApiStatsProxyCallback<T> implements IntegrationCallback<T> {
        private final TenantId tenantId;
        private final CustomerId customerId;
        private final int dataPoints;
        private final IntegrationCallback<T> callback;

        public ApiStatsProxyCallback(TenantId tenantId, CustomerId customerId, int dataPoints, IntegrationCallback<T> callback) {
            this.tenantId = tenantId;
            this.customerId = customerId;
            this.dataPoints = dataPoints;
            this.callback = callback;
        }

        @Override
        public void onSuccess(T msg) {
            try {
                apiUsageReportClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_MSG_COUNT, 1);
                apiUsageReportClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_DP_COUNT, dataPoints);
            } finally {
                if (callback != null) {
                    callback.onSuccess(msg);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }


    private static CustomerId getCustomerId(SessionInfoProto sessionInfo) {
        CustomerId customerId;
        if (sessionInfo.getCustomerIdMSB() > 0 && sessionInfo.getCustomerIdLSB() > 0) {
            customerId = new CustomerId(new UUID(sessionInfo.getCustomerIdMSB(), sessionInfo.getCustomerIdLSB()));
        } else {
            customerId = null;
        }
        return customerId;
    }
}
