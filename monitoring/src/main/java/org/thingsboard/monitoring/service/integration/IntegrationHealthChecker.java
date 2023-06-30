/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.monitoring.service.integration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.integration.IntegrationInfo;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringConfig;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringTarget;
import org.thingsboard.monitoring.config.integration.IntegrationType;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.service.BaseHealthChecker;
import org.thingsboard.monitoring.util.ResourceUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

@Slf4j
public abstract class IntegrationHealthChecker<C extends IntegrationMonitoringConfig> extends BaseHealthChecker<C, IntegrationMonitoringTarget> {

    private static final String DEFAULT_DEVICE_NAME = "[Monitoring] %s integration (%s)";
    private static final String DEFAULT_CONVERTER_NAME = "[Monitoring] Default converter";
    private static final String DEFAULT_INTEGRATION_NAME = "[Monitoring] %s integration";

    public IntegrationHealthChecker(C config, IntegrationMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected final void initialize(TbClient tbClient) {
        String deviceName = String.format(DEFAULT_DEVICE_NAME, config.getIntegrationType(), target.getBaseUrl());
        Device device = findOrCreateDevice(deviceName, tbClient);
        DeviceConfig deviceConfig = new DeviceConfig();
        deviceConfig.setId(device.getId().toString());
        deviceConfig.setName(deviceName);
        target.setDevice(deviceConfig);

        String converterName = DEFAULT_CONVERTER_NAME;
        Converter converter = findOrCreateConverter(converterName, tbClient);

        String integrationName = String.format(DEFAULT_INTEGRATION_NAME, config.getIntegrationType());
        Integration integration = findOrCreateIntegration(integrationName, converter.getId(), tbClient);
        target.setIntegration(integration);
    }

    @Override
    protected final String createTestPayload(String testValue) {
        ObjectNode payload = JacksonUtil.newObjectNode();
        payload.set("telemetry", JacksonUtil.newObjectNode()
                .set(TEST_TELEMETRY_KEY, new TextNode(testValue)));
        payload.set("device", new TextNode(target.getDevice().getName()));
        return payload.toString();
    }

    @Override
    protected final Object getInfo() {
        return new IntegrationInfo(getIntegrationType(), target.getBaseUrl());
    }

    @Override
    protected final String getKey() {
        return getIntegrationType().name().toLowerCase() + "Integration";
    }

    protected abstract IntegrationType getIntegrationType();


    private Device findOrCreateDevice(String deviceName, TbClient tbClient) {
        return tbClient.getTenantDevice(deviceName)
                .orElseGet(() -> {
                    Device defaultDevice = ResourceUtils.getResource("integration/device.json", Device.class);
                    defaultDevice.setName(deviceName);
                    log.info("Creating new device '{}'", deviceName);
                    return tbClient.saveDevice(defaultDevice);
                });
    }

    private Integration findOrCreateIntegration(String integrationName, ConverterId converterId, TbClient tbClient) {
        return tbClient.getIntegrations(new PageLink(1, 0, integrationName)).getData()
                .stream().findFirst()
                .orElseGet(() -> {
                    Integration defaultIntegration = ResourceUtils.getResource("integration/" + config.getIntegrationType().name().toLowerCase() + "/integration.json", Integration.class);
                    defaultIntegration.setName(integrationName);
                    defaultIntegration.setDefaultConverterId(converterId);
                    defaultIntegration.setRoutingKey(UUID.randomUUID().toString());
                    defaultIntegration.setConfiguration(JacksonUtil.toJsonNode(
                            String.format(defaultIntegration.getConfiguration().toString(),
                                    target.getBaseUrl() /* %1$s */, defaultIntegration.getRoutingKey() /* %2$s */)));
                    log.info("Creating new integration '{}'", integrationName);
                    return tbClient.saveIntegration(defaultIntegration);
                });
    }

    private Converter findOrCreateConverter(String converterName, TbClient tbClient) {
        return tbClient.getConverters(new PageLink(1, 0, converterName)).getData()
                .stream().findFirst()
                .orElseGet(() -> {
                    Converter defaultConverter = ResourceUtils.getResource("integration/converter.json", Converter.class);
                    defaultConverter.setName(converterName);
                    log.info("Creating new converter '{}'", converterName);
                    return tbClient.saveConverter(defaultConverter);
                });
    }

}
