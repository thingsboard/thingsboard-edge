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
package org.thingsboard.monitoring.service.transport;

import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.config.transport.TransportInfo;
import org.thingsboard.monitoring.config.transport.TransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.BaseHealthChecker;
import org.thingsboard.monitoring.util.ResourceUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

@Slf4j
public abstract class TransportHealthChecker<C extends TransportMonitoringConfig> extends BaseHealthChecker<C, TransportMonitoringTarget> {

    private static final String DEFAULT_DEVICE_NAME = "[Monitoring] %s transport (%s)";
    private static final String DEFAULT_PROFILE_NAME = "[Monitoring] %s";

    public TransportHealthChecker(C config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initialize(TbClient tbClient) {
        String deviceName = String.format(DEFAULT_DEVICE_NAME, config.getTransportType(), target.getBaseUrl());
        Device device = tbClient.getTenantDevice(deviceName)
                .orElseGet(() -> {
                    log.info("Creating new device '{}'", deviceName);
                    return createDevice(config.getTransportType(), deviceName, tbClient);
                });
        DeviceCredentials credentials = tbClient.getDeviceCredentialsByDeviceId(device.getId())
                .orElseThrow(() -> new IllegalArgumentException("No credentials found for device " + device.getId()));

        DeviceConfig deviceConfig = new DeviceConfig();
        deviceConfig.setId(device.getId().toString());
        deviceConfig.setName(deviceName);
        deviceConfig.setCredentials(credentials);
        target.setDevice(deviceConfig);
    }

    @Override
    protected String createTestPayload(String testValue) {
        return JacksonUtil.newObjectNode().set(TEST_TELEMETRY_KEY, new TextNode(testValue)).toString();
    }

    @Override
    protected Object getInfo() {
        return new TransportInfo(getTransportType(), target.getBaseUrl());
    }

    @Override
    protected String getKey() {
        return getTransportType().name().toLowerCase() + "Transport";
    }

    protected abstract TransportType getTransportType();


    private Device createDevice(TransportType transportType, String name, TbClient tbClient) {
        Device device = new Device();
        device.setName(name);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId(RandomStringUtils.randomAlphabetic(20));

        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        if (transportType != TransportType.LWM2M) {
            device.setType("default");
            deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        } else {
            tbClient.getResources(new PageLink(1, 0, "lwm2m monitoring")).getData()
                    .stream().findFirst()
                    .orElseGet(() -> {
                        TbResource newResource = ResourceUtils.getResource("lwm2m/resource.json", TbResource.class);
                        log.info("Creating LwM2M resource");
                        return tbClient.saveResource(newResource);
                    });
            String profileName = String.format(DEFAULT_PROFILE_NAME, transportType);
            DeviceProfile profile = tbClient.getDeviceProfiles(new PageLink(1, 0, profileName)).getData()
                    .stream().findFirst()
                    .orElseGet(() -> {
                        DeviceProfile newProfile = ResourceUtils.getResource("lwm2m/device_profile.json", DeviceProfile.class);
                        newProfile.setName(profileName);
                        log.info("Creating LwM2M device profile");
                        return tbClient.saveDeviceProfile(newProfile);
                    });
            device.setType(profileName);
            device.setDeviceProfileId(profile.getId());
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
        return tbClient.saveDeviceWithCredentials(device, credentials).get();
    }

}
