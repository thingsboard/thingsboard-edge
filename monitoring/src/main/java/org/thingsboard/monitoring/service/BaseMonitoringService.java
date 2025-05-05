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

import com.google.common.collect.Sets;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.client.WsClientFactory;
import org.thingsboard.monitoring.config.MonitoringConfig;
import org.thingsboard.monitoring.config.MonitoringTarget;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.monitoring.util.TbStopWatch;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.TsValue;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class BaseMonitoringService<C extends MonitoringConfig<T>, T extends MonitoringTarget> {

    @Autowired(required = false)
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

    @Value("${monitoring.edqs.enabled:false}")
    private boolean edqsMonitoringEnabled;

    @PostConstruct
    private void init() {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        tbClient.logIn();
        configs.forEach(config -> {
            config.getTargets().forEach(target -> {
                BaseHealthChecker<C, T> healthChecker = initHealthChecker(target, config);
                healthCheckers.add(healthChecker);

                if (target.isCheckDomainIps()) {
                    getAssociatedUrls(target.getBaseUrl()).forEach(url -> {
                        healthChecker.getAssociates().put(url, initHealthChecker(createTarget(url), config));
                    });
                }
            });
        });
    }

    private BaseHealthChecker<C, T> initHealthChecker(T target, C config) {
        BaseHealthChecker<C, T> healthChecker = (BaseHealthChecker<C, T>) createHealthChecker(config, target);
        log.info("Initializing {} for {}", healthChecker.getClass().getSimpleName(), target.getBaseUrl());
        healthChecker.initialize(tbClient);
        devices.add(target.getDeviceId());
        return healthChecker;
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
                stopWatch.start();
                wsClient.subscribeForTelemetry(devices, TransportHealthChecker.TEST_TELEMETRY_KEY).waitForReply();
                reporter.reportLatency(Latencies.WS_SUBSCRIBE, stopWatch.getTime());

                for (BaseHealthChecker<C, T> healthChecker : healthCheckers) {
                    check(healthChecker, wsClient);
                }
            }

            if (edqsMonitoringEnabled) {
                try {
                    stopWatch.start();
                    checkEdqs();
                    reporter.reportLatency(Latencies.EDQS_QUERY, stopWatch.getTime());

                    reporter.serviceIsOk(MonitoredServiceKey.EDQS);
                } catch (ServiceFailureException e) {
                    reporter.serviceFailure(MonitoredServiceKey.EDQS, e);
                } catch (Exception e) {
                    reporter.serviceFailure(MonitoredServiceKey.GENERAL, e);
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

    private void check(BaseHealthChecker<C, T> healthChecker, WsClient wsClient) throws Exception {
        healthChecker.check(wsClient);

        T target = healthChecker.getTarget();
        if (target.isCheckDomainIps()) {
            Set<String> associatedUrls = getAssociatedUrls(target.getBaseUrl());
            Map<String, BaseHealthChecker<C, T>> associates = healthChecker.getAssociates();
            Set<String> prevAssociatedUrls = new HashSet<>(associates.keySet());

            boolean changed = false;
            for (String url : associatedUrls) {
                if (!prevAssociatedUrls.contains(url)) {
                    BaseHealthChecker<C, T> associate = initHealthChecker(createTarget(url), healthChecker.getConfig());
                    associates.put(url, associate);
                    changed = true;
                }
            }
            for (String url : prevAssociatedUrls) {
                if (!associatedUrls.contains(url)) {
                    stopHealthChecker(healthChecker);
                    associates.remove(url);
                    changed = true;
                }
            }
            if (changed) {
                log.info("Updated IPs for {}: {} (old list: {})", target.getBaseUrl(), associatedUrls, prevAssociatedUrls);
            }
        }
    }

    private void checkEdqs() {
        EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
        entityTypeFilter.setEntityType(EntityType.DEVICE);
        EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "name")));
        EntityDataQuery entityDataQuery = new EntityDataQuery(entityTypeFilter, pageLink,
                List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "type")),
                List.of(new EntityKey(EntityKeyType.TIME_SERIES, "testData")),
                Collections.emptyList());

        PageData<EntityData> result = tbClient.findEntityDataByQuery(entityDataQuery);
        Set<UUID> devices = result.getData().stream()
                .map(entityData -> entityData.getEntityId().getId())
                .collect(Collectors.toSet());
        Set<UUID> missing = Sets.difference(new HashSet<>(this.devices), devices);
        if (!missing.isEmpty()) {
            throw new ServiceFailureException("Missing devices in the response: " + missing);
        }

        result.getData().stream()
                .filter(entityData -> this.devices.contains(entityData.getEntityId().getId()))
                .forEach(entityData -> {
                    Map<String, TsValue> values = new HashMap<>(entityData.getLatest().get(EntityKeyType.ENTITY_FIELD));
                    values.putAll(entityData.getLatest().get(EntityKeyType.TIME_SERIES));

                    Stream.of("name", "type", "testData").forEach(key -> {
                        TsValue value = values.get(key);
                        if (value == null || StringUtils.isBlank(value.getValue())) {
                            throw new ServiceFailureException("Missing " + key + " for device " + entityData.getEntityId());
                        }
                    });
                });
    }

    @SneakyThrows
    private Set<String> getAssociatedUrls(String baseUrl) {
        URI url = new URI(baseUrl);
        return Arrays.stream(InetAddress.getAllByName(url.getHost()))
                .map(InetAddress::getHostAddress)
                .map(ip -> {
                    try {
                        return new URI(url.getScheme(), null, ip, url.getPort(), "", null, null).toString();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }

    private void stopHealthChecker(BaseHealthChecker<C, T> healthChecker) throws Exception {
        healthChecker.destroyClient();
        devices.remove(healthChecker.getTarget().getDeviceId());
        log.info("Stopped {} for {}", healthChecker.getClass().getSimpleName(), healthChecker.getTarget().getBaseUrl());
    }

    protected abstract BaseHealthChecker<?, ?> createHealthChecker(C config, T target);

    protected abstract T createTarget(String baseUrl);

    protected abstract String getName();

}
