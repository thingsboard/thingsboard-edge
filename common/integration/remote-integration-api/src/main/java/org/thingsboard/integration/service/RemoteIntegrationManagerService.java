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
package org.thingsboard.integration.service;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.integration.api.IntegrationStatistics;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.converter.JSDownlinkDataConverter;
import org.thingsboard.integration.api.converter.JSUplinkDataConverter;
import org.thingsboard.integration.api.converter.TBDataConverter;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.api.data.DefaultIntegrationDownlinkMsg;
import org.thingsboard.integration.remote.RemoteIntegrationContext;
import org.thingsboard.integration.rpc.IntegrationRpcClient;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.cluster.ServerType;
import org.thingsboard.server.gen.integration.ConverterConfigurationProto;
import org.thingsboard.server.gen.integration.DeviceDownlinkDataProto;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationStatisticsProto;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.gen.transport.KeyValueProto;
import org.thingsboard.server.gen.transport.KeyValueType;
import org.thingsboard.server.gen.transport.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TsKvListProto;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service("RemoteIntegrationManagerService")
@Slf4j
@Data
public class RemoteIntegrationManagerService {

    public static final ObjectMapper mapper = new ObjectMapper();

    @Value("${rpc.client_id}")
    private String clientId;

    @Value("${server.port}")
    private int port;

    @Value("${integration.routingKey}")
    private String routingKey;

    @Value("${integration.secret}")
    private String routingSecret;

    @Value("${integration.allow_local_network_hosts:true}")
    private boolean allowLocalNetworkHosts;

    @Value("${executors.reconnect_timeout}")
    private long reconnectTimeoutMs;

    @Value("${integration.statistics.enabled}")
    private boolean statisticsEnabled;

    @Value("${integration.statistics.persist_frequency}")
    private long statisticsPersistFrequency;

    @Autowired
    private IntegrationRpcClient rpcClient;

    @Autowired
    private EventStorage eventStorage;

    @Autowired
    private JsInvokeService jsInvokeService;

    private ThingsboardPlatformIntegration integration;
    private ComponentLifecycleEvent integrationEvent;

    private TBUplinkDataConverter uplinkDataConverter;
    private TBDownlinkDataConverter downlinkDataConverter;

    private ConverterId uplinkConverterId;
    private ConverterId downlinkConverterId;

    private ExecutorService executor;
    private ScheduledExecutorService reconnectScheduler;
    private ScheduledExecutorService statisticsExecutorService;
    private ScheduledExecutorService schedulerService;
    private ScheduledFuture<?> scheduledFuture;

    private volatile boolean initialized;
    private volatile boolean updatingIntegration;

    @PostConstruct
    public void init() {
        rpcClient.connect(routingKey, routingSecret, this::onConfigurationUpdate, this::onConverterConfigurationUpdate, this::onDownlink, this::scheduleReconnect);
        executor = Executors.newSingleThreadExecutor();
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        schedulerService = Executors.newSingleThreadScheduledExecutor();
        processHandleMessages();
        if (statisticsEnabled) {
            statisticsExecutorService = Executors.newSingleThreadScheduledExecutor();
            statisticsExecutorService.scheduleAtFixedRate(this::persistStatistics, statisticsPersistFrequency, statisticsPersistFrequency, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        if (uplinkDataConverter != null) {
            uplinkDataConverter.destroy();
        }
        if (downlinkDataConverter != null) {
            downlinkDataConverter.destroy();
        }
        if (integration != null) {
            integration.destroy();
        }
        rpcClient.disconnect();
        if (executor != null) {
            executor.shutdownNow();
        }
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdownNow();
        }
        if (statisticsEnabled && statisticsExecutorService != null) {
            statisticsExecutorService.shutdownNow();
        }
        if (schedulerService != null) {
            schedulerService.shutdownNow();
        }
    }

    private void onConfigurationUpdate(IntegrationConfigurationProto integrationConfigurationProto) {
        if (integration != null) {
            updatingIntegration = true;
            integration.destroy();
        }
        //canceling reconnect scheduler
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        try {
            Integration configuration = createIntegrationConfiguration(integrationConfigurationProto);
            integration = createPlatformIntegration(integrationConfigurationProto.getType(), configuration.getConfiguration());
            integration.validateConfiguration(configuration, allowLocalNetworkHosts);

            if (uplinkDataConverter == null || !uplinkDataConverter.getName().equals(integrationConfigurationProto.getUplinkConverter().getName())) {
                uplinkDataConverter = createUplinkConverter(integrationConfigurationProto.getUplinkConverter());
            }

            if (downlinkDataConverter == null || !downlinkDataConverter.getName().equals(integrationConfigurationProto.getDownlinkConverter().getName())) {
                downlinkDataConverter = createDownlinkConverter(integrationConfigurationProto.getDownlinkConverter());
            }

            TbIntegrationInitParams params = new TbIntegrationInitParams(
                    new RemoteIntegrationContext(eventStorage, schedulerService, configuration, clientId, port),
                    configuration,
                    uplinkDataConverter,
                    downlinkDataConverter);
            integration.init(params);
            if (updatingIntegration) {
                integrationEvent = ComponentLifecycleEvent.UPDATED;
                persistLifecycleEvent(ComponentLifecycleEvent.UPDATED, null);
            } else {
                integrationEvent = ComponentLifecycleEvent.STARTED;
                persistLifecycleEvent(ComponentLifecycleEvent.STARTED, null);
            }
            initialized = true;
        } catch (Exception e) {
            log.error("Failed to initialize platform integration!", e);
            integrationEvent = ComponentLifecycleEvent.FAILED;
            persistLifecycleEvent(ComponentLifecycleEvent.FAILED, e);
        }
    }

    private void onConverterConfigurationUpdate(ConverterConfigurationProto converterProto) {
        ConverterId converterId = new ConverterId(new UUID(converterProto.getConverterIdMSB(), converterProto.getConverterIdLSB()));
        ConverterType converterType;
        TBDataConverter tbDataConverter;
        if (converterId.equals(uplinkConverterId)) {
            converterType = ConverterType.UPLINK;
            tbDataConverter = uplinkDataConverter;
        } else {
            converterType = ConverterType.DOWNLINK;
            tbDataConverter = downlinkDataConverter;
        }
        try {
            tbDataConverter.update(constructConverter(converterProto, converterType));
        } catch (IOException e) {
            log.error("[{}] Failed to update converter configuration", converterId, e);
        }
    }

    private void onDownlink(DeviceDownlinkDataProto deviceDownlinkDataProto) {
        DefaultIntegrationDownlinkMsg downlinkMsg = new DefaultIntegrationDownlinkMsg(
                integration.getConfiguration().getTenantId(),
                integration.getConfiguration().getId(), TbMsg.fromBytes(deviceDownlinkDataProto.getTbMsg().toByteArray()));

        integration.onDownlinkMsg(downlinkMsg);
    }

    private TBUplinkDataConverter createUplinkConverter(ConverterConfigurationProto uplinkConverter) throws IOException {
        JSUplinkDataConverter uplinkDataConverter = new JSUplinkDataConverter(jsInvokeService);
        Converter converter = constructConverter(uplinkConverter, ConverterType.UPLINK);
        uplinkConverterId = converter.getId();
        uplinkDataConverter.init(converter);
        return uplinkDataConverter;
    }

    private TBDownlinkDataConverter createDownlinkConverter(ConverterConfigurationProto downLinkConverter) throws IOException {
        if (!StringUtils.isEmpty(downLinkConverter.getConfiguration())) {
            JSDownlinkDataConverter downlinkDataConverter = new JSDownlinkDataConverter(jsInvokeService);
            Converter converter = constructConverter(downLinkConverter, ConverterType.DOWNLINK);
            downlinkConverterId = converter.getId();
            downlinkDataConverter.init(converter);
            return downlinkDataConverter;
        }
        return null;
    }

    private Converter constructConverter(ConverterConfigurationProto converterProto, ConverterType converterType) throws IOException {
        Converter converter = new Converter();
        converter.setId(new ConverterId(new UUID(converterProto.getConverterIdMSB(), converterProto.getConverterIdLSB())));
        converter.setTenantId(new TenantId(new UUID(converterProto.getTenantIdMSB(), converterProto.getTenantIdLSB())));
        converter.setName(converterProto.getName());
        converter.setType(converterType);
        converter.setDebugMode(converterProto.getDebugMode());
        converter.setConfiguration(mapper.readTree(converterProto.getConfiguration()));
        converter.setAdditionalInfo(mapper.readTree(converterProto.getAdditionalInfo()));
        return converter;
    }

    private Integration createIntegrationConfiguration(IntegrationConfigurationProto integrationConfigurationProto) throws IOException {
        Integration integration = new Integration();

        TenantId tenantId = new TenantId(new UUID(integrationConfigurationProto.getTenantIdMSB(), integrationConfigurationProto.getTenantIdLSB()));

        ConverterId defaultConverterId = new ConverterId(new UUID(integrationConfigurationProto.getUplinkConverter().getConverterIdMSB(), integrationConfigurationProto.getUplinkConverter().getConverterIdLSB()));
        ConverterId downlinkConverterId = new ConverterId(new UUID(integrationConfigurationProto.getDownlinkConverter().getConverterIdMSB(), integrationConfigurationProto.getDownlinkConverter().getConverterIdLSB()));

        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(defaultConverterId);
        integration.setDownlinkConverterId(downlinkConverterId);
        integration.setName(integrationConfigurationProto.getName());
        integration.setRoutingKey(integrationConfigurationProto.getRoutingKey());
        integration.setType(IntegrationType.valueOf(integrationConfigurationProto.getType()));
        integration.setDebugMode(integrationConfigurationProto.getDebugMode());
        integration.setRemote(true);
        integration.setSecret(routingSecret);
        integration.setConfiguration(mapper.readTree(integrationConfigurationProto.getConfiguration()));
        integration.setAdditionalInfo(mapper.readTree(integrationConfigurationProto.getAdditionalInfo()));

        if (!integrationConfigurationProto.hasField(integrationConfigurationProto.getDescriptorForType().findFieldByName("enabled"))) {
            integration.setEnabled(true);
        } else {
            integration.setEnabled(integrationConfigurationProto.getEnabled());
        }

        log.info("Received Integration update: {}!", integration);
        return integration;
    }

    private void scheduleReconnect(Exception e) {
        initialized = false;
        if (scheduledFuture == null) {
            scheduledFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
                log.info("Trying to reconnect due to the error: {}!", e.getMessage());
                rpcClient.connect(routingKey, routingSecret, this::onConfigurationUpdate, this::onConverterConfigurationUpdate, this::onDownlink, this::scheduleReconnect);
            }, 0, reconnectTimeoutMs, TimeUnit.MILLISECONDS);
        }
    }

    private ThingsboardPlatformIntegration createPlatformIntegration(String type, JsonNode configuration) throws Exception {
        switch (IntegrationType.valueOf(type)) {
            case HTTP:
                return newInstance("org.thingsboard.integration.http.basic.BasicHttpIntegration");
            case SIGFOX:
                return newInstance("org.thingsboard.integration.http.sigfox.SigFoxIntegration");
            case OCEANCONNECT:
                return newInstance("org.thingsboard.integration.http.oc.OceanConnectIntegration");
            case THINGPARK:
                return newInstance("org.thingsboard.integration.http.thingpark.ThingParkIntegration");
            case TPE:
                return newInstance("org.thingsboard.integration.http.thingpark.ThingParkIntegrationEnterprise");
            case TMOBILE_IOT_CDP:
                return newInstance("org.thingsboard.integration.http.tmobile.TMobileIotCdpIntegration");
            case MQTT:
                return newInstance("org.thingsboard.integration.mqtt.basic.BasicMqttIntegration");
            case AWS_IOT:
                return newInstance("org.thingsboard.integration.mqtt.aws.AwsIotIntegration");
            case IBM_WATSON_IOT:
                return newInstance("org.thingsboard.integration.mqtt.ibm.IbmWatsonIotIntegration");
            case TTN:
                return newInstance("org.thingsboard.integration.mqtt.ttn.TtnIntegration");
            case AZURE_EVENT_HUB:
                return newInstance("org.thingsboard.integration.azure.AzureEventHubIntegration");
            case OPC_UA:
                return newInstance("org.thingsboard.integration.opcua.OpcUaIntegration");
            case TCP:
                return newInstance("org.thingsboard.integration.tcpip.tcp.BasicTcpIntegration");
            case UDP:
                return newInstance("org.thingsboard.integration.tcpip.udp.BasicUdpIntegration");
            case AWS_SQS:
                return newInstance("org.thingsboard.integration.aws.sqs.AwsSqsIntegration");
            case AWS_KINESIS:
                return newInstance("org.thingsboard.integration.kinesis.AwsKinesisIntegration");
            case KAFKA:
                return newInstance("org.thingsboard.integration.kafka.basic.BasicKafkaIntegration");
            case CUSTOM:
                return newInstance(configuration.get("clazz").asText());
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private ThingsboardPlatformIntegration newInstance(String clazz) throws Exception {
        return (ThingsboardPlatformIntegration) Class.forName(clazz).newInstance();
    }

    private void processHandleMessages() {
        executor.submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    if (initialized) {
                        rpcClient.handleMsgs();
                    } else {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                    }
                } catch (Exception e) {
                    log.warn("Failed to process messages handling!", e);
                }
            }
        });
    }

    private void persistStatistics() {
        ServerAddress serverAddress = new ServerAddress(clientId, port, ServerType.CORE);
        long ts = System.currentTimeMillis();
        IntegrationStatistics statistics = integration.popStatistics();
        try {
            String eventData = mapper.writeValueAsString(toBodyJson(serverAddress, statistics.getMessagesProcessed(), statistics.getErrorsOccurred()));
            eventStorage.write(UplinkMsg.newBuilder()
                    .addEventsData(TbEventProto.newBuilder()
                            .setSource(TbEventSource.INTEGRATION)
                            .setType(DataConstants.STATS)
                            .setUid(UUIDs.timeBased().toString())
                            .setData(eventData)
                            .setDeviceName("")
                            .build())
                    .build(), null);

            PostTelemetryMsg.Builder telemetryBuilder = PostTelemetryMsg.newBuilder();
            List<KeyValueProto> telemetryResult = new ArrayList<>();
            telemetryResult.add(KeyValueProto.newBuilder().setKey("messagesCount").setType(KeyValueType.LONG_V)
                    .setLongV(statistics.getMessagesProcessed()).build());
            telemetryResult.add(KeyValueProto.newBuilder().setKey("errorsCount").setType(KeyValueType.LONG_V)
                    .setLongV(statistics.getErrorsOccurred()).build());
            telemetryResult.add(KeyValueProto.newBuilder().setKey("state").setType(KeyValueType.STRING_V)
                    .setStringV(integrationEvent != null ? integrationEvent.name() : "N/A").build());

            TsKvListProto.Builder tsKvListBuilder = TsKvListProto.newBuilder();
            tsKvListBuilder.setTs(ts);
            tsKvListBuilder.addAllKv(telemetryResult);
            telemetryBuilder.addTsKvList(tsKvListBuilder.build());

            eventStorage.write(UplinkMsg.newBuilder()
                    .addIntegrationStatistics(IntegrationStatisticsProto
                            .newBuilder()
                            .setPostTelemetryMsg(telemetryBuilder.build())
                            .build())
                    .build(), null);
        } catch (Exception e) {
            log.warn("[{}] Failed to persist statistics: {}", integration.getConfiguration().getId(), statistics, e);
        }
    }

    private void persistLifecycleEvent(ComponentLifecycleEvent event, Exception e) {
        try {
            String eventData = mapper.writeValueAsString(toBodyJson(new ServerAddress(clientId, port, ServerType.CORE), event, Optional.ofNullable(e)));
            eventStorage.write(UplinkMsg.newBuilder()
                    .addEventsData(TbEventProto.newBuilder()
                            .setSource(TbEventSource.INTEGRATION)
                            .setType(DataConstants.LC_EVENT)
                            .setUid(UUIDs.timeBased().toString())
                            .setData(eventData)
                            .setDeviceName("")
                            .build())
                    .build(), null);
        } catch (JsonProcessingException ex) {
            log.warn("[{}] Failed to persist lifecycle event!", integration.getConfiguration().getId(), e);
        }
    }

    private JsonNode toBodyJson(ServerAddress server, long messagesProcessed, long errorsOccurred) {
        return mapper.createObjectNode().put("server", server.toString()).put("messagesProcessed", messagesProcessed).put("errorsOccurred", errorsOccurred);
    }

    private JsonNode toBodyJson(ServerAddress server, ComponentLifecycleEvent event, Optional<Exception> e) {
        ObjectNode node = mapper.createObjectNode().put("server", server.toString()).put("event", event.name());
        if (e.isPresent()) {
            node = node.put("success", false);
            node = node.put("error", toString(e.get()));
        } else {
            node = node.put("success", true);
        }
        return node;
    }

    private String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
