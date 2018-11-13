/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.*;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.cluster.discovery.ServerInstance;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.converter.TBDownlinkDataConverter;
import org.thingsboard.server.service.converter.TBUplinkDataConverter;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;
import org.thingsboard.server.service.integration.azure.AzureEventHubIntegration;
import org.thingsboard.server.service.integration.http.basic.BasicHttpIntegration;
import org.thingsboard.server.service.integration.http.ffb.FfbHttpIntegration;
import org.thingsboard.server.service.integration.http.oc.OceanConnectIntegration;
import org.thingsboard.server.service.integration.http.sigfox.SigFoxIntegration;
import org.thingsboard.server.service.integration.http.thingpark.ThingParkIntegration;
import org.thingsboard.server.service.integration.http.tmobile.TMobileIotCdpIntegration;
import org.thingsboard.server.service.integration.mqtt.aws.AwsIotIntegration;
import org.thingsboard.server.service.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.server.service.integration.mqtt.ibm.IbmWatsonIotIntegration;
import org.thingsboard.server.service.integration.mqtt.ttn.TtnIntegration;
import org.thingsboard.server.service.integration.msg.DefaultIntegrationDownlinkMsg;
import org.thingsboard.server.service.integration.msg.IntegrationDownlinkMsg;
import org.thingsboard.server.service.integration.opcua.OpcUaIntegration;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by ashvayka on 02.12.17.
 */
@Slf4j
@Service
public class DefaultPlatformIntegrationService implements PlatformIntegrationService {

    public static EventLoopGroup EVENT_LOOP_GROUP;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private DataConverterService dataConverterService;

    @Autowired
    protected IntegrationContext context;

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private ClusterRoutingService clusterRoutingService;

    @Autowired
    private EventService eventService;

    @Autowired
    @Lazy
    private ClusterRpcService clusterRpcService;

    @Autowired
    @Lazy
    private ClusterRoutingService routingService;

    @Autowired
    @Lazy
    private DataDecodingEncodingService encodingService;

    @Autowired
    @Lazy
    private ActorSystemContext actorContext;

    @Value("${transport.rate_limits.enabled}")
    private boolean rateLimitEnabled;

    @Value("${transport.rate_limits.tenant}")
    private String perTenantLimitsConf;

    @Value("${transport.rate_limits.tenant}")
    private String perDevicesLimitsConf;

    @Value("${integrations.statistics.enabled}")
    private boolean statisticsEnabled;

    @Value("${integrations.statistics.persist_frequency}")
    private long statisticsPersistFrequency;

    private ScheduledExecutorService statisticsExecutorService;
    private ListeningExecutorService refreshExecutorService;

    private ConcurrentMap<IntegrationId, ThingsboardPlatformIntegration> integrationsByIdMap;
    private ConcurrentMap<String, ThingsboardPlatformIntegration> integrationsByRoutingKeyMap;

    private ConcurrentMap<TenantId, TbRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private ConcurrentMap<DeviceId, TbRateLimits> perDeviceLimits = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        integrationsByIdMap = new ConcurrentHashMap<>();
        integrationsByRoutingKeyMap = new ConcurrentHashMap<>();
        EVENT_LOOP_GROUP = new NioEventLoopGroup();
        refreshExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        refreshAllIntegrations();
        if (statisticsEnabled) {
            statisticsExecutorService = Executors.newSingleThreadScheduledExecutor();
            statisticsExecutorService.scheduleAtFixedRate(this::persistStatistics,
                    statisticsPersistFrequency, statisticsPersistFrequency, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    public void destroy() {
        if (statisticsEnabled) {
            statisticsExecutorService.shutdown();
        }
        integrationsByIdMap.values().forEach(ThingsboardPlatformIntegration::destroy);
        EVENT_LOOP_GROUP.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        integrationsByIdMap.clear();
        integrationsByRoutingKeyMap.clear();
        refreshExecutorService.shutdownNow();
    }

    @Override
    public ThingsboardPlatformIntegration createIntegration(Integration integration) throws Exception {
        return integrationsByIdMap.computeIfAbsent(integration.getId(), i -> {
            try {
                ThingsboardPlatformIntegration result = initIntegration(integration);
                integrationsByRoutingKeyMap.putIfAbsent(integration.getRoutingKey(), result);
                return result;
            } catch (Exception e) {
                throw handleException(e);
            }
        });
    }

    @Override
    public ThingsboardPlatformIntegration updateIntegration(Integration configuration) throws Exception {
        ThingsboardPlatformIntegration integration = integrationsByIdMap.get(configuration.getId());
        if (integration != null) {
            integration.update(new TbIntegrationInitParams(context, configuration, getUplinkDataConverter(configuration), getDownlinkDataConverter(configuration)));
            return integration;
        } else {
            return createIntegration(configuration);
        }
    }

    @Override
    public void deleteIntegration(IntegrationId integrationId) {
        ThingsboardPlatformIntegration integration = integrationsByIdMap.remove(integrationId);
        if (integration != null) {
            integrationsByRoutingKeyMap.remove(integration.getConfiguration().getRoutingKey());
            integration.destroy();
        }
    }

    @Override
    public Optional<ThingsboardPlatformIntegration> getIntegrationById(IntegrationId id) {
        ThingsboardPlatformIntegration result = integrationsByIdMap.get(id);
        if (result == null) {
            Integration configuration = integrationService.findIntegrationById(TenantId.SYS_TENANT_ID, id);
            if (configuration != null) {
                try {
                    result = createIntegration(configuration);
                } catch (Exception e) {
                    throw handleException(e);
                }
            }
        }
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String key) {
        ThingsboardPlatformIntegration result = integrationsByRoutingKeyMap.get(key);
        if (result == null) {
            Optional<Integration> configuration = integrationService.findIntegrationByRoutingKey(TenantId.SYS_TENANT_ID, key);
            if (configuration.isPresent()) {
                try {
                    result = createIntegration(configuration.get());
                } catch (Exception e) {
                    throw handleException(e);
                }
            }
        }
        return Optional.ofNullable(result);
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg msg, FutureCallback<Void> callback) {
        try {
            IntegrationId integrationId = msg.getIntegrationId();
            ThingsboardPlatformIntegration integration = integrationsByIdMap.get(integrationId);
            if (integration == null) {
                Optional<ServerAddress> server = clusterRoutingService.resolveById(integrationId);
                if (server.isPresent()) {
                    clusterRpcService.tell(server.get(), msg);
                } else {
                    Integration configuration = integrationService.findIntegrationById(TenantId.SYS_TENANT_ID, integrationId);
                    onMsg(createIntegration(configuration), msg);
                }
            } else {
                onMsg(integration, msg);
            }
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            }
            throw handleException(e);
        }
    }

    @Override
    public void onRemoteDownlinkMsg(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.IntegrationDownlinkProto proto;
        try {
            proto = ClusterAPIProtos.IntegrationDownlinkProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        TenantId tenantId = new TenantId(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
        IntegrationId integrationId = new IntegrationId(new UUID(proto.getIntegrationIdMSB(), proto.getIntegrationIdLSB()));
        IntegrationDownlinkMsg msg = new DefaultIntegrationDownlinkMsg(tenantId, integrationId, TbMsg.fromBytes(proto.getData().toByteArray()));
        onDownlinkMsg(msg, null);
    }

    private void onMsg(ThingsboardPlatformIntegration integration, IntegrationDownlinkMsg msg) {
        integration.onDownlinkMsg(context, msg);
    }

    private void persistStatistics() {
        ServerAddress serverAddress = discoveryService.getCurrentServer().getServerAddress();
        integrationsByIdMap.forEach((id, integration) -> {
            IntegrationStatistics statistics = integration.popStatistics();
            try {
                Event event = new Event();
                event.setEntityId(id);
                event.setTenantId(integration.getConfiguration().getTenantId());
                event.setType(DataConstants.STATS);
                event.setBody(toBodyJson(serverAddress, statistics.getMessagesProcessed(), statistics.getErrorsOccurred()));
                eventService.save(event);
            } catch (Exception e) {
                log.warn("[{}] Failed to persist statistics: {}", id, statistics, e);
            }
        });
    }

    private JsonNode toBodyJson(ServerAddress server, long messagesProcessed, long errorsOccurred) {
        return mapper.createObjectNode().put("server", server.toString()).put("messagesProcessed", messagesProcessed).put("errorsOccurred", errorsOccurred);
    }

    private ThingsboardPlatformIntegration initIntegration(Integration integration) throws Exception {
        ThingsboardPlatformIntegration result = createThingsboardPlatformIntegration(integration);
        result.init(new TbIntegrationInitParams(context, integration, getUplinkDataConverter(integration), getDownlinkDataConverter(integration)));
        return result;
    }

    private ThingsboardPlatformIntegration createThingsboardPlatformIntegration(Integration integration) {
        switch (integration.getType()) {
            case HTTP:
                return new BasicHttpIntegration();
            case SIGFOX:
                return new SigFoxIntegration();
            case OCEANCONNECT:
                return new OceanConnectIntegration();
            case THINGPARK:
                return new ThingParkIntegration();
            case TMOBILE_IOT_CDP:
                return new TMobileIotCdpIntegration();
            case MQTT:
                return new BasicMqttIntegration();
            case AWS_IOT:
                return new AwsIotIntegration();
            case IBM_WATSON_IOT:
                return new IbmWatsonIotIntegration();
            case TTN:
                return new TtnIntegration();
            case AZURE_EVENT_HUB:
                return new AzureEventHubIntegration();
            case OPC_UA:
                return new OpcUaIntegration(context);
            case FFB:
                return new FfbHttpIntegration();
            default:
                throw new RuntimeException("Not Implemented!");
        }
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
    public void onServerAdded(ServerInstance server) {
        refreshAllIntegrations();
    }

    @Override
    public void onServerUpdated(ServerInstance server) {
        refreshAllIntegrations();
    }

    @Override
    public void onServerRemoved(ServerInstance server) {
        refreshAllIntegrations();
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostTelemetryMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setPostTelemetry(msg).build(), callback);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostAttributeMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setPostAttributes(msg).build(), callback);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setGetAttributes(msg).build(), callback);
        }
    }

    private void forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg toDeviceActorMsg, TransportServiceCallback<Void> callback) {
        TransportToDeviceActorMsgWrapper wrapper = new TransportToDeviceActorMsgWrapper(toDeviceActorMsg);
        Optional<ServerAddress> address = routingService.resolveById(wrapper.getDeviceId());
        if (address.isPresent()) {
            clusterRpcService.tell(encodingService.convertToProtoDataMessage(address.get(), wrapper));
        } else {
            actorContext.getAppActor().tell(wrapper, ActorRef.noSender());
        }
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private boolean checkLimits(TransportProtos.SessionInfoProto sessionInfo, Object msg, TransportServiceCallback<Void> callback) {
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

    protected UUID toId(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    private void refreshAllIntegrations() {
        Set<IntegrationId> currentIntegrationIds = new HashSet<>(integrationsByIdMap.keySet());
        for (IntegrationId integrationId : currentIntegrationIds) {
            if (clusterRoutingService.resolveById(integrationId).isPresent()) {
                if (integrationsByIdMap.get(integrationId).getConfiguration().getType().isSingleton()) {
                    deleteIntegration(integrationId);
                }
            }
        }

        List<Integration> allIntegrations = integrationService.findAllIntegrations(TenantId.SYS_TENANT_ID);
        try {
            List<ListenableFuture<Void>> futures = Lists.newArrayList();
            for (Integration integration : allIntegrations) {
                futures.add(refreshExecutorService.submit(() -> {
                    try {
                        //Initialize the integration that belongs to current node only
                        if (!clusterRoutingService.resolveById(integration.getId()).isPresent()) {
                            createIntegration(integration);
                        }
                    } catch (Exception e) {
                        log.error("[{}] Unable to initialize integration {}", integration.getId(), integration.getName(), e);
                    }
                    return null;
                }));
            }
            Futures.allAsList(futures).get();
            log.info("{} Integrations refreshed", allIntegrations.size());
        } catch (Throwable th) {
            log.error("Could not init integrations", th);
        }

    }

}
