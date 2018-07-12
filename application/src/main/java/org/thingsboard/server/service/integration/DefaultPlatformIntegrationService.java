/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.cluster.discovery.DiscoveryServiceListener;
import org.thingsboard.server.service.cluster.discovery.ServerInstance;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.converter.TBDownlinkDataConverter;
import org.thingsboard.server.service.converter.TBUplinkDataConverter;
import org.thingsboard.server.service.integration.azure.AzureEventHubIntegration;
import org.thingsboard.server.service.integration.http.basic.BasicHttpIntegration;
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Created by ashvayka on 02.12.17.
 */
@Slf4j
@Service
public class DefaultPlatformIntegrationService implements PlatformIntegrationService, DiscoveryServiceListener {

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


    @Value("${integrations.statistics.enabled}")
    private boolean statisticsEnabled;

    @Value("${integrations.statistics.persist_frequency}")
    private long statisticsPersistFrequency;

    private ScheduledExecutorService statisticsExecutorService;

    private ConcurrentMap<IntegrationId, ThingsboardPlatformIntegration> integrationsByIdMap;
    private ConcurrentMap<String, ThingsboardPlatformIntegration> integrationsByRoutingKeyMap;

    @PostConstruct
    public void init() {
        integrationsByIdMap = new ConcurrentHashMap<>();
        integrationsByRoutingKeyMap = new ConcurrentHashMap<>();
        EVENT_LOOP_GROUP = new NioEventLoopGroup();
        discoveryService.addListener(this);
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
            Integration configuration = integrationService.findIntegrationById(id);
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
            Optional<Integration> configuration = integrationService.findIntegrationByRoutingKey(key);
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
                    Integration configuration = integrationService.findIntegrationById(integrationId);
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
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private TBUplinkDataConverter getUplinkDataConverter(Integration integration) {
        return dataConverterService.getUplinkConverterById(integration.getDefaultConverterId())
                .orElseThrow(() -> new ThingsboardRuntimeException("Converter not found!", ThingsboardErrorCode.ITEM_NOT_FOUND));
    }

    private TBDownlinkDataConverter getDownlinkDataConverter(Integration integration) {
        return dataConverterService.getDownlinkConverterById(integration.getDownlinkConverterId())
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

    private void refreshAllIntegrations() {
        Set<IntegrationId> currentIntegrationIds = new HashSet<>(integrationsByIdMap.keySet());
        for (IntegrationId integrationId : currentIntegrationIds) {
            if (clusterRoutingService.resolveById(integrationId).isPresent()) {
                if (integrationsByIdMap.get(integrationId).getConfiguration().getType().isSingleton()) {
                    deleteIntegration(integrationId);
                }
            }
        }
        integrationService.findAllIntegrations().forEach(configuration -> {
            try {
                //Initialize the integration that belongs to current node only
                if (!clusterRoutingService.resolveById(configuration.getId()).isPresent()) {
                    createIntegration(configuration);
                }
            } catch (Exception e) {
                log.error("[{}] Unable to initialize integration {}", configuration.getId(), configuration.getName(), e);
            }
        });
    }

}
