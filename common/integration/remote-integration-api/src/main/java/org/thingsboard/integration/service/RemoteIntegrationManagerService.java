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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.converter.JSDownlinkDataConverter;
import org.thingsboard.integration.api.converter.JSUplinkDataConverter;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.remote.RemoteIntegrationContext;
import org.thingsboard.integration.remote.RemoteIntegrationService;
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
import java.util.concurrent.Future;

// TODO: 7/2/19 integration statistics?
@Service("RemoteIntegrationManagerService")
@Slf4j
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

    @Autowired
    private IntegrationRpcClient rpcClient;

    @Autowired
    private EventStorage eventStorage;

    @Autowired
    private JsInvokeService jsInvokeService;

    @Autowired
    private RemoteIntegrationService integrationService;

    private ThingsboardPlatformIntegration integration;
    private ExecutorService executor;
    private Future future;

    @PostConstruct
    public void init() {
        rpcClient.connect(routingKey, routingSecret, this::onConfigurationUpdate, this::scheduleReconnect);
        executor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        rpcClient.disconnect();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void onConfigurationUpdate(IntegrationConfigurationProto integrationConfigurationProto) {
        if (integration != null) {
            integration.destroy();
        }
        if (future != null) {
            future.cancel(true);
        }
        try {
            Integration configuration = createConfig(integrationConfigurationProto);
            integration = create(integrationConfigurationProto.getType());
            TbIntegrationInitParams params = new TbIntegrationInitParams(
                    new RemoteIntegrationContext(integrationService, eventStorage, configuration, clientId, port),
                    configuration,
                    createUplinkConverter(integrationConfigurationProto.getUplinkConverter()),
                    createDownlinkConverter(integrationConfigurationProto.getDownlinkConverter()));
            integration.init(params);
            future = processHandleMessages();
        } catch (Exception e) {
            log.warn("Failed to initialize platform integration!", e);
        }
    }

    private TBUplinkDataConverter createUplinkConverter(ConverterConfigurationProto uplinkConverter) throws IOException {
        TBUplinkDataConverter uplinkDataConverter = new JSUplinkDataConverter(jsInvokeService);
        uplinkDataConverter.init(constructConverter(uplinkConverter, ConverterType.UPLINK));
        return uplinkDataConverter;
    }

    private TBDownlinkDataConverter createDownlinkConverter(ConverterConfigurationProto downLinkConverter) throws IOException {
        if (!StringUtils.isEmpty(downLinkConverter.getConfiguration())) {
            TBDownlinkDataConverter downlinkDataConverter = new JSDownlinkDataConverter(jsInvokeService);
            downlinkDataConverter.init(constructConverter(downLinkConverter, ConverterType.DOWNLINK));
            return downlinkDataConverter;
        }
        return null;
    }

    private Converter constructConverter(ConverterConfigurationProto converterProto, ConverterType converterType) throws IOException {
        Converter converter = new Converter();
        converter.setTenantId(new TenantId(new UUID(converterProto.getTenantIdMSB(), converterProto.getTenantIdLSB())));
        converter.setName(converterProto.getName());
        converter.setType(converterType);
        converter.setDebugMode(converterProto.getDebugMode());
        converter.setConfiguration(mapper.readTree(converterProto.getConfiguration()));
        converter.setAdditionalInfo(mapper.readTree(converterProto.getAdditionalInfo()));
        return converter;
    }

    private Integration createConfig(IntegrationConfigurationProto integrationConfigurationProto) throws IOException {
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
        //TODO
    }

    private ThingsboardPlatformIntegration create(String type) throws Exception {
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
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private ThingsboardPlatformIntegration newInstance(String clazz) throws Exception {
        return (ThingsboardPlatformIntegration) Class.forName(clazz).newInstance();
    }

    private Future processHandleMessages() {
        return executor.submit(() -> {
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
