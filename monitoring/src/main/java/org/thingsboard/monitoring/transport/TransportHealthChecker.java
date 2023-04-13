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

import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.config.service.TransportMonitoringConfig;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.TransportFailureException;
import org.thingsboard.monitoring.data.TransportInfo;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;

@Slf4j
public abstract class TransportHealthChecker<C extends TransportMonitoringConfig> {

    protected final C config;
    protected final MonitoringTargetConfig target;
    private TransportInfo transportInfo;

    @Autowired
    private MonitoringReporter reporter;
    @Autowired
    private TbStopWatch stopWatch;
    @Value("${monitoring.check_timeout_ms}")
    private int resultCheckTimeoutMs;

    public static final String TEST_TELEMETRY_KEY = "testData";

    protected TransportHealthChecker(C config, MonitoringTargetConfig target) {
        this.config = config;
        this.target = target;
    }

    @PostConstruct
    private void init() {
        transportInfo = new TransportInfo(getTransportType(), target.getBaseUrl());
    }

    public final void check(WsClient wsClient) {
        log.debug("[{}] Checking", transportInfo);
        try {
            wsClient.registerWaitForUpdate();

            String testValue = UUID.randomUUID().toString();
            String testPayload = JacksonUtil.newObjectNode().set(TEST_TELEMETRY_KEY, new TextNode(testValue)).toString();
            try {
                initClientAndSendPayload(testPayload);
                log.trace("[{}] Sent test payload ({})", transportInfo, testPayload);
            } catch (Throwable e) {
                throw new TransportFailureException(e);
            }

            log.trace("[{}] Waiting for WS update", transportInfo);
            checkWsUpdate(wsClient, testValue);

            reporter.serviceIsOk(transportInfo);
            reporter.serviceIsOk(MonitoredServiceKey.GENERAL);
        } catch (TransportFailureException transportFailureException) {
            reporter.serviceFailure(transportInfo, transportFailureException);
        } catch (Exception e) {
            reporter.serviceFailure(MonitoredServiceKey.GENERAL, e);
        }
    }

    private void initClientAndSendPayload(String payload) throws Throwable {
        initClient();
        stopWatch.start();
        sendTestPayload(payload);
        reporter.reportLatency(Latencies.transportRequest(getTransportType()), stopWatch.getTime());
    }

    private void checkWsUpdate(WsClient wsClient, String testValue) {
        stopWatch.start();
        wsClient.waitForUpdate(resultCheckTimeoutMs);
        log.trace("[{}] Waited for WS update. Last WS msg: {}", transportInfo, wsClient.lastMsg);
        Object update = wsClient.getTelemetryUpdate(target.getDevice().getId(), TEST_TELEMETRY_KEY);
        if (update == null) {
            throw new TransportFailureException("No WS update arrived within " + resultCheckTimeoutMs + " ms");
        } else if (!update.toString().equals(testValue)) {
            throw new TransportFailureException("Was expecting value " + testValue + " but got " + update);
        }
        reporter.reportLatency(Latencies.WS_UPDATE, stopWatch.getTime());
    }


    protected abstract void initClient() throws Exception;

    protected abstract void sendTestPayload(String payload) throws Exception;

    @PreDestroy
    protected abstract void destroyClient() throws Exception;

    protected abstract TransportType getTransportType();

}
