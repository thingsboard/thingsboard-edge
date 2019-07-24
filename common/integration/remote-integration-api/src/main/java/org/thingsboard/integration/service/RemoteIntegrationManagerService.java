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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.converter.JSDownlinkDataConverter;
import org.thingsboard.integration.api.converter.JSUplinkDataConverter;
import org.thingsboard.integration.api.converter.TBDataConverter;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.remote.RemoteIntegrationContext;
import org.thingsboard.integration.rpc.IntegrationRpcClient;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.gen.integration.ConverterConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// TODO: 7/2/19 integration statistics?
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

    @Value("${integrations.allow_local_network_hosts:true}")
    private boolean allowLocalNetworkHosts;

    @Value("${executors.reconnect_timeout}")
    private long reconnectTimeoutMs;

    @Autowired
    private IntegrationRpcClient rpcClient;

    @Autowired
    private EventStorage eventStorage;

    @Autowired
    private JsInvokeService jsInvokeService;

    @Autowired
    private CacheManager cacheManager;

    private ThingsboardPlatformIntegration integration;

    private TBUplinkDataConverter uplinkDataConverter;
    private TBDownlinkDataConverter downlinkDataConverter;

    private ConverterId uplinkConverterId;
    private ConverterId downlinkConverterId;

    private ExecutorService executor;
    private ScheduledExecutorService reconnectScheduler;
    private ScheduledFuture<?> scheduledFuture;

    private boolean initialized;

    @PostConstruct
    public void init() {
        rpcClient.connect(routingKey, routingSecret, this::onConfigurationUpdate, this::onConverterConfigurationUpdate, this::scheduleReconnect);
        executor = Executors.newSingleThreadExecutor();
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
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
    }

    private void onConfigurationUpdate(IntegrationConfigurationProto integrationConfigurationProto) {
        if (integration != null) {
            integration.destroy();
        }
        //cancel reconnect scheduler
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        try {
            Integration configuration = createIntegrationConfiguration(integrationConfigurationProto);
            integration = createPlatformIntegration(integrationConfigurationProto.getType(), configuration.getConfiguration());
            integration.validateConfiguration(configuration, allowLocalNetworkHosts); // TODO: 7/3/19 allowLocalNetworkHosts?

            if (uplinkDataConverter == null) {
                uplinkDataConverter = createUplinkConverter(integrationConfigurationProto.getUplinkConverter());
            }
            if (downlinkDataConverter == null) {
                downlinkDataConverter = createDownlinkConverter(integrationConfigurationProto.getDownlinkConverter());
            }

            TbIntegrationInitParams params = new TbIntegrationInitParams(
                    new RemoteIntegrationContext(eventStorage, cacheManager, configuration, clientId, port),
                    configuration,
                    uplinkDataConverter,
                    downlinkDataConverter);
            integration.init(params);
            if (!initialized) {
                processHandleMessages();
                initialized = true;
            }
        } catch (Exception e) {
            log.error("Failed to initialize platform integration!", e);
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

        return integration;
    }

    private void scheduleReconnect(Exception e) {
        if (scheduledFuture == null) {
            scheduledFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
                log.info("Trying to reconnect due to the error: {}!", e.getMessage());
                rpcClient.connect(routingKey, routingSecret, this::onConfigurationUpdate, this::onConverterConfigurationUpdate, this::scheduleReconnect);
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
            while (true) {
                try {
                    rpcClient.handleMsgs();
                } catch (Exception e) {
                    log.warn("Failed to process messages handling!", e);
                }
            }
        });
    }
}
