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

@Service("RemoteIntegrationManagerService")
@Slf4j
public class RemoteIntegrationManagerService {

    public static final ObjectMapper mapper = new ObjectMapper();

    @Value("${integration.routingKey}")
    private String routingKey;

    @Value("${integration.secret}")
    private String routingSecret;

    @Autowired
    private IntegrationRpcClient rpcClient;

    @Autowired
    private JsInvokeService jsInvokeService;

    @Autowired
    private RemoteIntegrationService integrationService;

    private ThingsboardPlatformIntegration integration;

    @PostConstruct
    public void init() {
        rpcClient.connect(routingKey, routingSecret, this::onConfigurationUpdate, this::scheduleReconnect);
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        rpcClient.disconnect();
    }

    private void onConfigurationUpdate(IntegrationConfigurationProto integrationConfigurationProto) {
        if (integration != null) {
            integration.destroy();
        }
        try {
            Integration configuration = createConfig(integrationConfigurationProto);
            integration = create(integrationConfigurationProto.getType());
            TbIntegrationInitParams params = new TbIntegrationInitParams(
                    new RemoteIntegrationContext(integrationService, rpcClient, configuration),
                    configuration,
                    createUplinkConverter(integrationConfigurationProto.getUplinkConverter()),
                    createDownlinkConverter(integrationConfigurationProto.getDownlinkConverter()));
            integration.init(params);
        } catch (Exception e) {
            log.warn("Failed to initialize platform integration!", e);
        }
    }

    private TBUplinkDataConverter createUplinkConverter(ConverterConfigurationProto uplinkConverter) throws IOException {
        TBUplinkDataConverter uplinkDataConverter = new JSUplinkDataConverter(jsInvokeService);

        TenantId tenantId = new TenantId(new UUID(uplinkConverter.getTenantIdMSB(), uplinkConverter.getTenantIdLSB()));

        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName(uplinkConverter.getName());
        converter.setType(ConverterType.UPLINK);
        converter.setDebugMode(uplinkConverter.getDebugMode());
        converter.setConfiguration(mapper.readTree(uplinkConverter.getConfiguration()));
        converter.setAdditionalInfo(mapper.readTree(uplinkConverter.getAdditionalInfo()));

        uplinkDataConverter.init(converter);

        return uplinkDataConverter;
    }

    private TBDownlinkDataConverter createDownlinkConverter(ConverterConfigurationProto downLinkConverter) throws IOException {
        if (!StringUtils.isEmpty(downLinkConverter.getConfiguration())) {
            TBDownlinkDataConverter downlinkDataConverter = new JSDownlinkDataConverter(jsInvokeService);

            TenantId tenantId = new TenantId(new UUID(downLinkConverter.getTenantIdMSB(), downLinkConverter.getTenantIdLSB()));

            Converter converter = new Converter();
            converter.setTenantId(tenantId);
            converter.setName(downLinkConverter.getName());
            converter.setType(ConverterType.DOWNLINK);
            converter.setDebugMode(downLinkConverter.getDebugMode());
            converter.setConfiguration(mapper.readTree(downLinkConverter.getConfiguration()));
            converter.setAdditionalInfo(mapper.readTree(downLinkConverter.getAdditionalInfo()));

            downlinkDataConverter.init(converter);
            return downlinkDataConverter;
        }
        return null;
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
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private ThingsboardPlatformIntegration newInstance(String clazz) throws Exception {
        return (ThingsboardPlatformIntegration) Class.forName(clazz).newInstance();
    }
}
