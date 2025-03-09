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
package org.thingsboard.server.transport.mqtt.gateway;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.transport.mqtt.TbMqttTransportComponent;
import org.thingsboard.server.common.msg.gateway.metrics.GatewayMetadata;
import org.thingsboard.server.transport.mqtt.gateway.metrics.GatewayMetricsState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbMqttTransportComponent
public class GatewayMetricsService {

    public static final String GATEWAY_METRICS = "gatewayMetrics";

    @Value("${transport.mqtt.gateway_metrics_report_interval_sec:60}")
    private int metricsReportIntervalSec;

    @Autowired
    private SchedulerComponent scheduler;

    @Autowired
    private TransportService transportService;

    private Map<DeviceId, GatewayMetricsState> states = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        scheduler.scheduleAtFixedRate(this::reportMetrics, metricsReportIntervalSec, metricsReportIntervalSec, TimeUnit.SECONDS);
    }

    public void process(TransportProtos.SessionInfoProto sessionInfo, DeviceId gatewayId, List<GatewayMetadata> data, long serverReceiveTs) {
        states.computeIfAbsent(gatewayId, k -> new GatewayMetricsState(sessionInfo)).update(data, serverReceiveTs);
    }

    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceId gatewayId) {
        var state = states.get(gatewayId);
        if (state != null) {
            state.updateSessionInfo(sessionInfo);
        }
    }

    public void onDeviceDelete(DeviceId deviceId) {
        states.remove(deviceId);
    }

    public void reportMetrics() {
        if (states.isEmpty()) {
            return;
        }
        Map<DeviceId, GatewayMetricsState> statesToReport = states;
        states = new ConcurrentHashMap<>();

        long ts = System.currentTimeMillis();

        statesToReport.forEach((gatewayId, state) -> {
            reportMetrics(state, ts);
        });
    }

    private void reportMetrics(GatewayMetricsState state, long ts) {
        if (state.isEmpty()) {
            return;
        }
        var result = state.getStateResult();
        var kvProto = TransportProtos.KeyValueProto.newBuilder()
                .setKey(GATEWAY_METRICS)
                .setType(TransportProtos.KeyValueType.JSON_V)
                .setJsonV(JacksonUtil.toString(result))
                .build();

        TransportProtos.TsKvListProto tsKvList = TransportProtos.TsKvListProto.newBuilder()
                .setTs(ts)
                .addKv(kvProto)
                .build();

        TransportProtos.PostTelemetryMsg telemetryMsg = TransportProtos.PostTelemetryMsg.newBuilder()
                .addTsKvList(tsKvList)
                .build();

        transportService.process(state.getSessionInfo(), telemetryMsg, TransportServiceCallback.EMPTY);
    }

}
