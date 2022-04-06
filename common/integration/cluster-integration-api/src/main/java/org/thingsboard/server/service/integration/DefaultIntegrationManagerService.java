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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.gcloud.pubsub.PubSubIntegration;
import org.thingsboard.integration.apache.pulsar.basic.BasicPulsarIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationStatistics;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.aws.kinesis.AwsKinesisIntegration;
import org.thingsboard.integration.aws.sqs.AwsSqsIntegration;
import org.thingsboard.integration.azure.AzureEventHubIntegration;
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
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreOrIntegrationExecutorComponent;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.integration.state.IntegrationState;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@TbCoreOrIntegrationExecutorComponent
@Service
@RequiredArgsConstructor
public class DefaultIntegrationManagerService implements IntegrationManagerService {

    private final ConcurrentMap<IntegrationId, IntegrationState> integrations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, IntegrationState> integrationsByRoutingKeyMap = new ConcurrentHashMap<>();
    private final PartitionService partitionService;
    private final IntegrationContextProvider integrationContextProvider;
    private final IntegrationConfigurationService configurationService;
    private final DataConverterService dataConverterService;
    private final EventStorageService eventStorageService;
    private final TbServiceInfoProvider serviceInfoProvider;

    @Value("${integrations.rate_limits.enabled}")
    private boolean rateLimitEnabled;

    @Value("${integrations.rate_limits.tenant}")
    private String perTenantLimitsConf;

    @Value("${integrations.rate_limits.tenant}")
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

    private ScheduledExecutorService statisticsExecutorService;
    private ScheduledExecutorService reinitExecutorService;
    private ScheduledExecutorService refreshExecutorService;

    @PostConstruct
    public void init() {
        refreshExecutorService = Executors.newScheduledThreadPool(4, ThingsBoardThreadFactory.forName("integration-refresh-service"));
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
    public void stop() {
        if (refreshExecutorService != null) {
            refreshExecutorService.shutdownNow();
        }
        if (statisticsEnabled) {
            statisticsExecutorService.shutdownNow();
        }
        if (reinitEnabled) {
            reinitExecutorService.shutdownNow();
        }
    }

    @Override
    public void refresh(IntegrationType integrationType, Set<TopicPartitionInfo> newPartitions) {
        Set<IntegrationId> currentIntegrationIds = new HashSet<>(integrations.keySet());
        for (IntegrationId integrationId : currentIntegrationIds) {
            Integration integration = integrations.get(integrationId).getIntegration().getConfiguration();
            if (!isMine(integration)) {
                scheduleIntegrationEvent(integration, ComponentLifecycleEvent.STOPPED);
            }
        }

        List<IntegrationInfo> allIntegrations = configurationService.getActiveIntegrationList(integrationType, false);
        try {
            for (IntegrationInfo integration : allIntegrations) {
                try {
                    //Initialize the integration that belongs to current node only
                    if (isMine(integration)) {
                        scheduleIntegrationEvent(integration, ComponentLifecycleEvent.CREATED);
                    }
                } catch (Exception e) {
                    log.info("[{}] Unable to initialize integration {}", integration.getId(), integration.getName(), e);
                }
            }
        } catch (Throwable th) {
            log.error("Could not init integrations", th);
        }
    }

    private void tryUpdate(IntegrationId id) {
        IntegrationState state = integrations.get(id);
        if (state == null) {
            log.debug("[{}] Skip processing of update due to missing state. Probably integration was already deleted.", id);
            return;
        }
        boolean locked = state.getUpdateLock().tryLock();
        if (locked) {
            try {
                update(state);
            } catch (Throwable e) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Failed to update state", id, e);
                } else {
                    log.warn("[{}] Failed to update state due to: {}. Enabled debug level to get more details.", id, e.getMessage());
                }
            } finally {
                state.getUpdateLock().unlock();
            }
        } else {
            refreshExecutorService.schedule(() -> tryUpdate(id), 1, TimeUnit.SECONDS);
        }
    }

    private void update(IntegrationState state) throws Exception {
        Queue<ComponentLifecycleEvent> stateQueue = state.getUpdateQueue();
        ComponentLifecycleEvent pendingEvent = null;
        while (!stateQueue.isEmpty()) {
            var update = stateQueue.poll();
            if (!ComponentLifecycleEvent.DELETED.equals(pendingEvent)) {
                pendingEvent = update;
            }
        }
        if (pendingEvent != null) {
            switch (pendingEvent) {
                case CREATED:
                case STARTED:
                case UPDATED:
                case ACTIVATED:
                    processUpdateEvent(state);
                    break;
                case STOPPED:
                case SUSPENDED:
                case DELETED:
                    processStop(state, pendingEvent);
                    break;
            }
            log.debug("[{}] Going to process new event: {}", state.getId(), pendingEvent);
        }
    }

    private void processUpdateEvent(IntegrationState state) throws Exception {
        Integration configuration = configurationService.getIntegration(state.getTenantId(), state.getId());
        if (state.getIntegration() == null) {
            if (!isMine(configuration)) {
                throw new ThingsboardException("Singleton integration already present on another node!", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            if (configuration.isRemote()) {
                throw new ThingsboardException("The integration is executed remotely!", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            if (!configuration.isEnabled()) {
                throw new ThingsboardException("The integration is disabled!", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            IntegrationContext context = integrationContextProvider.buildIntegrationContext(configuration);
            state.setContext(context);
            ThingsboardPlatformIntegration<?> integration = newIntegration(context, configuration);
            state.setIntegration(integration);
            integrationsByRoutingKeyMap.putIfAbsent(configuration.getRoutingKey(), state);
            try {
                integration.init(new TbIntegrationInitParams(context, configuration, getUplinkDataConverter(configuration), getDownlinkDataConverter(configuration)));
                eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.STARTED, null);
                state.setCurrentState(ComponentLifecycleEvent.STARTED);
            } catch (Exception e) {
                state.setCurrentState(ComponentLifecycleEvent.FAILED);
                eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.FAILED, e);
                throw handleException(e);
            }
        } else {
            IntegrationContext context = integrationContextProvider.buildIntegrationContext(configuration);
            try {
                if (configuration.isEnabled()) {
                    state.setContext(context);
                    state.getIntegration().update(new TbIntegrationInitParams(context, configuration, getUplinkDataConverter(configuration), getDownlinkDataConverter(configuration)));
                    eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.STARTED, null);
                    state.setCurrentState(ComponentLifecycleEvent.STARTED);
                } else {
                    processStop(state, ComponentLifecycleEvent.STOPPED);
                }
            } catch (Exception e) {
                state.setCurrentState(ComponentLifecycleEvent.FAILED);
                eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.FAILED, e);
                throw handleException(e);
            }
        }
    }

    private void processStop(IntegrationState state, ComponentLifecycleEvent event) {
        if (state != null) {
            integrations.remove(state.getId());
            ThingsboardPlatformIntegration<?> integration = state.getIntegration();
            if (integration != null) {
                Integration configuration = integration.getConfiguration();
                integrationsByRoutingKeyMap.remove(integration.getConfiguration().getRoutingKey());
                state.setCurrentState(event);
                try {
                    integration.destroy();
                    eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), event, null);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("[{}][{}] Failed to destroy the integration: {}", state.getTenantId(), state.getId(), state.getIntegration().getConfiguration(), e);
                    } else {
                        log.warn("[{}][{}] Failed to destroy the integration: ", state.getTenantId(), state.getId());
                    }
                    eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), event, e);
                    throw handleException(e);
                }
            }
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

    private boolean isMine(IntegrationInfo integration) {
        return !integration.getType().isSingleton()
                || partitionService.resolve(ServiceType.TB_INTEGRATION_EXECUTOR, integration.getType().name(), integration.getTenantId(), integration.getId()).isMyPartition();
    }

    private void scheduleIntegrationEvent(IntegrationInfo info, ComponentLifecycleEvent event) {
        IntegrationState state;
        switch (event) {
            case CREATED:
            case UPDATED:
                state = integrations.computeIfAbsent(info.getId(), tmp -> new IntegrationState(info.getTenantId(), info.getId()));
                break;
            default:
                state = integrations.get(info);
        }
        if (state != null) {
            state.getUpdateQueue().add(event);
            log.debug("[{}][{}] Scheduling new event: {}", info.getTenantId(), info.getId(), event);
            refreshExecutorService.submit(() -> tryUpdate(info.getId()));
        } else {
            log.info("[{}][{}] Ignoring new event: {}", info.getTenantId(), info.getId(), event);
        }
    }

    private ThingsboardPlatformIntegration<?> newIntegration(IntegrationContext ctx, Integration configuration) {
        ThingsboardPlatformIntegration<?> platformIntegration = createPlatformIntegration(configuration);
        platformIntegration.validateConfiguration(configuration, allowLocalNetworkHosts);
        return platformIntegration;
    }

    private void reInitIntegrations() {
        //TODO: ashvayka integration executor
//        if (initialized) {
//            integrationsByIdMap.forEach((integrationId, integration) -> {
//                if (integrationEvents.getOrDefault(integrationId, ComponentLifecycleEvent.STARTED).equals(ComponentLifecycleEvent.FAILED)) {
//                    DonAsynchron.withCallback(createIntegration(integration.getFirst().getConfiguration(), true),
//                            tmp -> log.debug("[{}] Re-initialized the integration {}", integration.getFirst().getConfiguration().getId(), integration.getFirst().getConfiguration().getName()),
//                            e -> log.info("[{}] Unable to initialize integration {}", integration.getFirst().getConfiguration().getId(), integration.getFirst().getConfiguration().getName(), e));
//                }
//            });
//        }
    }

    private void persistStatistics() {
        long ts = System.currentTimeMillis();
        integrations.forEach((id, integration) -> {
            IntegrationStatistics statistics = integration.getIntegration().popStatistics();
            IntegrationInfo integrationInfo = integration.getIntegration().getConfiguration();
            try {
                eventStorageService.persistStatistics(integrationInfo.getTenantId(), integrationInfo.getId(), ts, statistics, integration.getCurrentState());
            } catch (Exception e) {
                log.warn("[{}] Failed to persist statistics: {}", id, statistics, e);
            }
        });
    }

    public ThingsboardPlatformIntegration<?> createPlatformIntegration(Integration integration) {
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
                //TODO: ashvayka integration executor
//                return new CoapIntegration(coapServerService);
            case CUSTOM:
            case TCP:
            case UDP:
                throw new RuntimeException("Custom Integrations should be executed remotely!");
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private RuntimeException handleException(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }

}
