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
package org.thingsboard.monitoring.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.client.WsClientFactory;
import org.thingsboard.monitoring.config.DeviceConfig;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.service.TransportMonitoringConfig;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public final class TransportMonitoringService {

    private final List<TransportMonitoringConfig> configs;
    private final List<TransportHealthChecker<?>> transportHealthCheckers = new LinkedList<>();
    private final List<UUID> devices = new LinkedList<>();

    private final TbClient tbClient;
    private final WsClientFactory wsClientFactory;
    private final TbStopWatch stopWatch;
    private final MonitoringReporter reporter;
    private final ApplicationContext applicationContext;
    private ScheduledExecutorService scheduler;
    @Value("${monitoring.transports.monitoring_rate_ms}")
    private int monitoringRateMs;

    @PostConstruct
    private void init() {
        configs.forEach(config -> {
            config.getTargets().stream()
                    .filter(target -> StringUtils.isNotBlank(target.getBaseUrl()))
                    .peek(target -> checkMonitoringTarget(config, target, tbClient))
                    .forEach(target -> {
                        TransportHealthChecker<?> transportHealthChecker = applicationContext.getBean(config.getTransportType().getServiceClass(), config, target);
                        transportHealthCheckers.add(transportHealthChecker);
                        devices.add(target.getDevice().getId());
                    });
        });
        scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("monitoring-executor"));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startMonitoring() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                log.debug("Starting transports check");
                stopWatch.start();
                String accessToken = tbClient.logIn();
                reporter.reportLatency(Latencies.LOG_IN, stopWatch.getTime());

                try (WsClient wsClient = wsClientFactory.createClient(accessToken)) {
                    wsClient.subscribeForTelemetry(devices, TransportHealthChecker.TEST_TELEMETRY_KEY).waitForReply();

                    for (TransportHealthChecker<?> transportHealthChecker : transportHealthCheckers) {
                        transportHealthChecker.check(wsClient);
                    }
                }
                reporter.reportLatencies(tbClient);
                log.debug("Finished transports check");
            } catch (Throwable error) {
                try {
                    reporter.serviceFailure(MonitoredServiceKey.GENERAL, error);
                } catch (Throwable reportError) {
                    log.error("Error occurred during service failure reporting", reportError);
                }
            }
        }, 0, monitoringRateMs, TimeUnit.MILLISECONDS);
    }

    private void checkMonitoringTarget(TransportMonitoringConfig config, MonitoringTargetConfig target, TbClient tbClient) {
        DeviceConfig deviceConfig = target.getDevice();
        tbClient.logIn();

        DeviceId deviceId;
        if (deviceConfig == null || deviceConfig.getId() == null) {
            String deviceName = String.format("[%s] Monitoring device (%s)", config.getTransportType(), target.getBaseUrl());
            Device device = tbClient.getTenantDevice(deviceName)
                    .orElseGet(() -> {
                        log.info("Creating new device '{}'", deviceName);
                        Device monitoringDevice = new Device();
                        monitoringDevice.setName(deviceName);
                        monitoringDevice.setType("default");
                        DeviceData deviceData = new DeviceData();
                        deviceData.setConfiguration(new DefaultDeviceConfiguration());
                        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
                        return tbClient.saveDevice(monitoringDevice);
                    });
            deviceId = device.getId();
            target.getDevice().setId(deviceId.toString());
        } else {
            deviceId = new DeviceId(deviceConfig.getId());
        }

        log.info("Loading credentials for device {}", deviceId);
        DeviceCredentials credentials = tbClient.getDeviceCredentialsByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("No credentials found for device " + deviceId));
        target.getDevice().setCredentials(credentials);
    }

}
