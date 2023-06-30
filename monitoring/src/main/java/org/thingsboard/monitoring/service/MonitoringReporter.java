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

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.data.Latency;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.notification.HighLatencyNotification;
import org.thingsboard.monitoring.data.notification.ServiceFailureNotification;
import org.thingsboard.monitoring.data.notification.ServiceRecoveryNotification;
import org.thingsboard.monitoring.notification.NotificationService;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringReporter {

    private final NotificationService notificationService;

    private final Map<String, Latency> latencies = new ConcurrentHashMap<>();
    private final Map<Object, AtomicInteger> failuresCounters = new ConcurrentHashMap<>();

    @Value("${monitoring.failures_threshold}")
    private int failuresThreshold;
    @Value("${monitoring.repeated_failure_notification}")
    private int repeatedFailureNotification;

    @Value("${monitoring.latency.enabled}")
    private boolean latencyReportingEnabled;
    @Value("${monitoring.latency.threshold_ms}")
    private int latencyThresholdMs;
    @Value("${monitoring.latency.reporting_asset_id}")
    private String reportingAssetId;

    public void reportLatencies(TbClient tbClient) {
        List<Latency> latencies = this.latencies.values().stream()
                .filter(Latency::isNotEmpty)
                .map(latency -> {
                    Latency snapshot = latency.snapshot();
                    latency.reset();
                    return snapshot;
                })
                .collect(Collectors.toList());
        if (latencies.isEmpty()) {
            return;
        }
        log.info("Latencies:\n{}", latencies.stream().map(latency -> latency.getKey() + ": " + latency.getAvg() + " ms")
                .collect(Collectors.joining("\n")) + "\n");

        if (!latencyReportingEnabled) return;

        if (latencies.stream().anyMatch(latency -> latency.getAvg() >= (double) latencyThresholdMs)) {
            HighLatencyNotification highLatencyNotification = new HighLatencyNotification(latencies, latencyThresholdMs);
            notificationService.sendNotification(highLatencyNotification);
        }

        try {
            if (StringUtils.isBlank(reportingAssetId)) {
                String assetName = "[Monitoring] Latencies";
                Asset monitoringAsset = tbClient.findAsset(assetName).orElseGet(() -> {
                    Asset asset = new Asset();
                    asset.setType("Monitoring");
                    asset.setName(assetName);
                    asset = tbClient.saveAsset(asset);
                    log.info("Created monitoring asset {}", asset.getId());
                    return asset;
                });
                reportingAssetId = monitoringAsset.getId().toString();
            }

            ObjectNode msg = JacksonUtil.newObjectNode();
            latencies.forEach(latency -> {
                msg.set(latency.getKey(), new DoubleNode(latency.getAvg()));
            });
            tbClient.saveEntityTelemetry(new AssetId(UUID.fromString(reportingAssetId)), "time", msg);
        } catch (Exception e) {
            log.error("Failed to report latencies: {}", e.getMessage());
        }
    }

    public void reportLatency(String key, long latencyInNanos) {
        String latencyKey = key + "Latency";
        double latencyInMs = (double) latencyInNanos / 1000_000;
        log.trace("Reporting latency [{}]: {} ms", key, latencyInMs);
        latencies.computeIfAbsent(latencyKey, k -> new Latency(latencyKey)).report(latencyInMs);
    }

    public void serviceFailure(Object serviceKey, Throwable error) {
        if (log.isDebugEnabled()) {
            log.error("Error occurred", error);
        }
        int failuresCount = failuresCounters.computeIfAbsent(serviceKey, k -> new AtomicInteger()).incrementAndGet();
        ServiceFailureNotification notification = new ServiceFailureNotification(serviceKey, error, failuresCount);
        log.error(notification.getText());
        if (failuresCount == failuresThreshold || (repeatedFailureNotification != 0 && failuresCount % repeatedFailureNotification == 0)) {
            notificationService.sendNotification(notification);
        }
    }

    public void serviceIsOk(Object serviceKey) {
        ServiceRecoveryNotification notification = new ServiceRecoveryNotification(serviceKey);
        if (!serviceKey.equals(MonitoredServiceKey.GENERAL)) {
            log.info(notification.getText());
        }
        AtomicInteger failuresCounter = failuresCounters.get(serviceKey);
        if (failuresCounter != null) {
            if (failuresCounter.get() >= failuresThreshold) {
                notificationService.sendNotification(notification);
            }
            failuresCounter.set(0);
        }
    }

}
