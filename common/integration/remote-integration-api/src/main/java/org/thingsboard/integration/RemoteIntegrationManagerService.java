/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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
package org.thingsboard.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.remote.RemoteIntegrationContext;
import org.thingsboard.integration.remote.RemoteIntegrationService;
import org.thingsboard.integration.rpc.IntegrationRpcClient;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.gen.integration.ConverterConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;

import javax.annotation.PostConstruct;

@Service("RemoteIntegrationManagerService")
@Slf4j
public class RemoteIntegrationManagerService {

    @Value("${integration.routingKey}")
    private String routingKey;

    @Value("${integration.routingSecret}")
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

    private void onConfigurationUpdate(IntegrationConfigurationProto integrationConfigurationProto) {
        if (integration != null) {
            integration.destroy();
        }
        try {
            Integration configuration = createConfig(integrationConfigurationProto);
            integration = create(integrationConfigurationProto.getType());
            TbIntegrationInitParams params = new TbIntegrationInitParams(
                    new RemoteIntegrationContext(integrationService, configuration),
                    configuration,
                    createUplinkConverter(integrationConfigurationProto.getUplinkConverter()),
                    createDownlinkConverter(integrationConfigurationProto.getUplinkConverter()));
            //TODO: initialize converters
            integration.init(params);
        } catch (Exception e) {
            log.warn("Failed to initialize configuration");
        }
    }

    private TBDownlinkDataConverter createDownlinkConverter(ConverterConfigurationProto uplinkConverter) {
    }

    private TBUplinkDataConverter createUplinkConverter(ConverterConfigurationProto uplinkConverter) {
    }

    private Integration createConfig(IntegrationConfigurationProto integrationConfigurationProto) {
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
