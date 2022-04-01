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
package org.thingsboard.integration.service;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.gcloud.pubsub.PubSubIntegration;
import org.thingsboard.integration.apache.pulsar.basic.BasicPulsarIntegration;
import org.thingsboard.integration.api.IntegrationContext;
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
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.integration.EventStorageService;
import org.thingsboard.server.service.integration.IntegrationContextProvider;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class DefaultIntegrationManagerService implements IntegrationManagerService {

    private final ConcurrentMap<IntegrationId, ComponentLifecycleEvent> integrationEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<IntegrationId, Pair<ThingsboardPlatformIntegration<?>, IntegrationContext>> integrationsByIdMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ThingsboardPlatformIntegration<?>> integrationsByRoutingKeyMap = new ConcurrentHashMap<>();
    private final IntegrationContextProvider integrationContextProvider;
    private final EventStorageService eventStorageService;
    private final DataConverterService dataConverterService;
    private ListeningExecutorService refreshExecutorService;

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

    @PostConstruct
    public void init() {
        refreshExecutorService = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(4, "default-integration-refresh"));
    }

    @PreDestroy
    public void destroy() {
        refreshExecutorService.shutdownNow();
    }

    @Override
    public ThingsboardPlatformIntegration getOrCreateIntegration(Integration configuration, boolean forceReInit) {
        Pair<ThingsboardPlatformIntegration<?>, IntegrationContext> integrationPair;
        boolean newIntegration = false;
        synchronized (integrationsByIdMap) {
            integrationPair = integrationsByIdMap.get(configuration.getId());
            if (integrationPair == null) {
                IntegrationContext context = integrationContextProvider.buildIntegrationContext(configuration);
                ThingsboardPlatformIntegration<?> integration = newIntegration(context, configuration);
                integrationPair = Pair.of(integration, context);
                integrationsByIdMap.put(configuration.getId(), integrationPair);
                integrationsByRoutingKeyMap.putIfAbsent(configuration.getRoutingKey(), integration);
                newIntegration = true;
            }
        }

        if (newIntegration || forceReInit) {
            synchronized (integrationPair) {
                try {
                    integrationPair.getFirst().init(new TbIntegrationInitParams(integrationPair.getSecond(), configuration,
                            getUplinkDataConverter(configuration),
                            getDownlinkDataConverter(configuration)));
                    eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.STARTED, null);
                    integrationEvents.put(configuration.getId(), ComponentLifecycleEvent.STARTED);
                } catch (Exception e) {
                    integrationEvents.put(configuration.getId(), ComponentLifecycleEvent.FAILED);
                    eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), ComponentLifecycleEvent.FAILED, e);
                    throw handleException(e);
                }
            }
        }
        return integrationPair.getFirst();
    }

    private ThingsboardPlatformIntegration<?> newIntegration(IntegrationContext ctx, Integration configuration) {
        ThingsboardPlatformIntegration<?> platformIntegration = createPlatformIntegration(configuration);
        platformIntegration.validateConfiguration(configuration, allowLocalNetworkHosts);
        return platformIntegration;
    }

    private TBUplinkDataConverter getUplinkDataConverter(Integration integration) {
        return dataConverterService.getUplinkConverterById(integration.getTenantId(), integration.getDefaultConverterId())
                .orElseThrow(() -> new ThingsboardRuntimeException("Converter not found!", ThingsboardErrorCode.ITEM_NOT_FOUND));
    }

    private TBDownlinkDataConverter getDownlinkDataConverter(Integration integration) {
        return dataConverterService.getDownlinkConverterById(integration.getTenantId(), integration.getDownlinkConverterId())
                .orElse(null);
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
