/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.monitoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringConfig;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringTarget;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.util.ResourceUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.ScriptCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.monitoring.service.BaseHealthChecker.TEST_CF_TELEMETRY_KEY;
import static org.thingsboard.monitoring.service.BaseHealthChecker.TEST_TELEMETRY_KEY;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitoringEntityService {

    private final TbClient tbClient;

    @Value("${monitoring.calculated_fields.enabled:true}")
    private boolean calculatedFieldsMonitoringEnabled;

    public void checkEntities() {
        RuleChain ruleChain = tbClient.getRuleChains(RuleChainType.CORE, new PageLink(10)).getData().stream()
                .filter(RuleChain::isRoot)
                .findFirst().orElseThrow();
        RuleChainId ruleChainId = ruleChain.getId();

        JsonNode ruleChainDescriptor = ResourceUtils.getResource("rule_chain.json");
        List<String> attributeKeys = tbClient.getAttributeKeys(ruleChainId);
        Map<String, String> attributes = tbClient.getAttributeKvEntries(ruleChainId, attributeKeys).stream()
                .collect(Collectors.toMap(KvEntry::getKey, KvEntry::getValueAsString));

        int currentVersion = Integer.parseInt(attributes.getOrDefault("version", "0"));
        int newVersion = ruleChainDescriptor.get("version").asInt();
        if (currentVersion == newVersion) {
            log.info("Not updating rule chain, version is the same ({})", currentVersion);
            return;
        } else {
            log.info("Updating rule chain '{}' from version {} to {}", ruleChain.getName(), currentVersion, newVersion);
        }

        String metadataJson = RegexUtils.replace(ruleChainDescriptor.get("metadata").toString(),
                "\\$\\{MONITORING:(.+?)}", matchResult -> {
                    String key = matchResult.group(1);
                    String value = attributes.get(key);
                    if (value == null) {
                        throw new IllegalArgumentException("No attribute found for key " + key);
                    }
                    log.info("Using {}: {}", key, value);
                    return value;
                });
        RuleChainMetaData metaData = JacksonUtil.fromString(metadataJson, RuleChainMetaData.class);
        metaData.setRuleChainId(ruleChainId);
        tbClient.saveRuleChainMetaData(metaData);
        tbClient.saveEntityAttributesV2(ruleChainId, DataConstants.SERVER_SCOPE, JacksonUtil.newObjectNode()
                .put("version", newVersion));
    }

    public Asset getOrCreateMonitoringAsset() {
        String assetName = "[Monitoring] Latencies";
        return tbClient.findAsset(assetName).orElseGet(() -> {
            Asset asset = new Asset();
            asset.setType("Monitoring");
            asset.setName(assetName);
            asset = tbClient.saveAsset(asset);
            log.info("Created monitoring asset {}", asset.getId());
            return asset;
        });
    }

    public void checkEntities(TransportMonitoringConfig config, TransportMonitoringTarget target) {
        Device device = getOrCreateDevice(config, target);
        DeviceCredentials credentials = tbClient.getDeviceCredentialsByDeviceId(device.getId())
                .orElseThrow(() -> new IllegalArgumentException("No credentials found for device " + device.getId()));

        DeviceConfig deviceConfig = new DeviceConfig();
        deviceConfig.setId(device.getId().toString());
        deviceConfig.setName(device.getName());
        deviceConfig.setCredentials(credentials);
        target.setDevice(deviceConfig);
    }

    private Device getOrCreateDevice(TransportMonitoringConfig config, TransportMonitoringTarget target) {
        TransportType transportType = config.getTransportType();
        String deviceName = String.format("%s %s (%s) - %s", target.getNamePrefix(), transportType.getName(), target.getQueue(), target.getBaseUrl()).trim();
        Device device = tbClient.getTenantDevice(deviceName).orElse(null);
        if (device != null) {
            if (calculatedFieldsMonitoringEnabled) {
                CalculatedField calculatedField = tbClient.getCalculatedFieldsByEntityId(device.getId(), new PageLink(1, 0, TEST_CF_TELEMETRY_KEY))
                        .getData().stream().findFirst().orElse(null);
                if (calculatedField == null) {
                    createCalculatedField(device);
                }
            }
            return device;
        }

        log.info("Creating new device '{}'", deviceName);
        device = new Device();
        device.setName(deviceName);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId(RandomStringUtils.randomAlphabetic(20));
        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());

        DeviceProfile deviceProfile = getOrCreateDeviceProfile(config, target);
        device.setType(deviceProfile.getName());
        device.setDeviceProfileId(deviceProfile.getId());

        if (transportType != TransportType.LWM2M) {
            deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        } else {
            deviceData.setTransportConfiguration(new Lwm2mDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
            LwM2MDeviceCredentials lwm2mCreds = new LwM2MDeviceCredentials();
            NoSecClientCredential client = new NoSecClientCredential();
            client.setEndpoint(credentials.getCredentialsId());
            lwm2mCreds.setClient(client);
            LwM2MBootstrapClientCredentials bootstrap = new LwM2MBootstrapClientCredentials();
            bootstrap.setBootstrapServer(new NoSecBootstrapClientCredential());
            bootstrap.setLwm2mServer(new NoSecBootstrapClientCredential());
            lwm2mCreds.setBootstrap(bootstrap);
            credentials.setCredentialsValue(JacksonUtil.toString(lwm2mCreds));
        }

        device = tbClient.saveDeviceWithCredentials(device, credentials).get();
        if (calculatedFieldsMonitoringEnabled) {
            createCalculatedField(device);
        }
        return device;
    }

    private DeviceProfile getOrCreateDeviceProfile(TransportMonitoringConfig config, TransportMonitoringTarget target) {
        TransportType transportType = config.getTransportType();
        String profileName = String.format("%s %s (%s)", target.getNamePrefix(), transportType.getName(), target.getQueue()).trim();
        DeviceProfile deviceProfile = tbClient.getDeviceProfiles(new PageLink(1, 0, profileName)).getData()
                .stream().findFirst().orElse(null);
        if (deviceProfile != null) {
            return deviceProfile;
        }

        log.info("Creating new device profile '{}'", profileName);
        if (transportType != TransportType.LWM2M) {
            deviceProfile = new DeviceProfile();
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
            DeviceProfileData profileData = new DeviceProfileData();
            profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
            profileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
            deviceProfile.setProfileData(profileData);
        } else {
            tbClient.getResources(new PageLink(1, 0, "LwM2M Monitoring")).getData()
                    .stream().findFirst()
                    .orElseGet(() -> {
                        TbResource newResource = ResourceUtils.getResource("lwm2m/resource.json", TbResource.class);
                        log.info("Creating LwM2M resource");
                        return tbClient.saveResource(newResource);
                    });
            deviceProfile = ResourceUtils.getResource("lwm2m/device_profile.json", DeviceProfile.class);
        }

        deviceProfile.setName(profileName);
        deviceProfile.setDefaultQueueName(target.getQueue());
        return tbClient.saveDeviceProfile(deviceProfile);
    }

    private void createCalculatedField(Device device) {
        log.info("Creating calculated field for device '{}'", device.getName());
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setName(TEST_CF_TELEMETRY_KEY);
        calculatedField.setEntityId(device.getId());
        calculatedField.setType(CalculatedFieldType.SCRIPT);
        ScriptCalculatedFieldConfiguration configuration = new ScriptCalculatedFieldConfiguration();
        Argument testDataArgument = new Argument();
        testDataArgument.setRefEntityKey(new ReferencedEntityKey(TEST_TELEMETRY_KEY, ArgumentType.TS_LATEST, null));
        configuration.setArguments(Map.of(
                TEST_TELEMETRY_KEY, testDataArgument
        ));
        configuration.setExpression("return { \"" + TEST_CF_TELEMETRY_KEY + "\": " + TEST_TELEMETRY_KEY + " + \"-cf\" };");
        Output output = new Output();
        output.setType(OutputType.TIME_SERIES);
        configuration.setOutput(output);
        calculatedField.setConfiguration(configuration);
        calculatedField.setDebugMode(true);
        tbClient.saveCalculatedField(calculatedField);
    }

    public void checkEntities(IntegrationMonitoringConfig config, IntegrationMonitoringTarget target) {
        Device device = getOrCreateDevice(config, target);
        DeviceConfig deviceConfig = new DeviceConfig();
        deviceConfig.setId(device.getId().toString());
        deviceConfig.setName(device.getName());
        target.setDevice(deviceConfig);

        Converter converter = getOrCreateConverter();
        Integration integration = getOrCreateIntegration(config, target, converter.getId());
        target.setIntegration(integration);
    }

    private Device getOrCreateDevice(IntegrationMonitoringConfig config, IntegrationMonitoringTarget target) {
        String deviceName = String.format("%s %s integration - %s", target.getNamePrefix(), config.getIntegrationType().getName(), target.getBaseUrl()).trim();
        return tbClient.getTenantDevice(deviceName)
                .orElseGet(() -> {
                    Device defaultDevice = ResourceUtils.getResource("integration/device.json", Device.class);
                    defaultDevice.setName(deviceName);
                    log.info("Creating new device '{}'", deviceName);
                    return tbClient.saveDevice(defaultDevice);
                });
    }

    private Integration getOrCreateIntegration(IntegrationMonitoringConfig config, IntegrationMonitoringTarget target, ConverterId converterId) {
        String integrationName = String.format("%s %s integration", target.getNamePrefix(), config.getIntegrationType().getName()).trim();
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

    private Converter getOrCreateConverter() {
        String converterName = "Default converter";
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
