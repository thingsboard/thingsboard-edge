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
package org.thingsboard.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.client.WsClientFactory;
import org.thingsboard.monitoring.config.MonitoringConfig;
import org.thingsboard.monitoring.config.MonitoringTarget;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.monitoring.util.TbStopWatch;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class BaseMonitoringService<C extends MonitoringConfig<T>, T extends MonitoringTarget> {

    @Autowired
    private List<C> configs;
    private final List<BaseHealthChecker<C, T>> healthCheckers = new LinkedList<>();
    private final List<UUID> devices = new LinkedList<>();

    @Autowired
    private TbClient tbClient;
    @Autowired
    private WsClientFactory wsClientFactory;
    @Autowired
    private TbStopWatch stopWatch;
    @Autowired
    private MonitoringReporter reporter;
    @Autowired
    protected ApplicationContext applicationContext;

    @PostConstruct
    private void init() {
        tbClient.logIn();
        configs.forEach(config -> {
            config.getTargets().forEach(target -> {
                BaseHealthChecker<C, T> healthChecker = (BaseHealthChecker<C, T>) createHealthChecker(config, target);
                log.info("Initializing {}", healthChecker.getClass().getSimpleName());
                healthChecker.initialize(tbClient);
                devices.add(target.getDeviceId());
                healthCheckers.add(healthChecker);
            });
        });
    }

    public final void runChecks() {
        if (healthCheckers.isEmpty()) {
            return;
        }
        try {
            log.info("Starting {}", getName());
            stopWatch.start();
            String accessToken = tbClient.logIn();
            reporter.reportLatency(Latencies.LOG_IN, stopWatch.getTime());

            try (WsClient wsClient = wsClientFactory.createClient(accessToken)) {
                wsClient.subscribeForTelemetry(devices, TransportHealthChecker.TEST_TELEMETRY_KEY).waitForReply();

                for (BaseHealthChecker<C, T> healthChecker : healthCheckers) {
                    healthChecker.check(wsClient);
                }
            }
            reporter.reportLatencies(tbClient);
            log.debug("Finished {}", getName());
        } catch (Throwable error) {
            try {
                reporter.serviceFailure(MonitoredServiceKey.GENERAL, error);
            } catch (Throwable reportError) {
                log.error("Error occurred during service failure reporting", reportError);
            }
        }
    }

    protected abstract BaseHealthChecker<?, ?> createHealthChecker(C config, T target);

    protected abstract String getName();

}
