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
package org.thingsboard.server.service.apiusage;

import lombok.Data;
import lombok.Getter;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class BaseApiUsageState {
    private final Map<ApiUsageRecordKey, Long> currentCycleValues = new ConcurrentHashMap<>();
    private final Map<ApiUsageRecordKey, Long> currentHourValues = new ConcurrentHashMap<>();

    private final Map<ApiUsageRecordKey, Map<String, Long>> lastGaugesByServiceId = new HashMap<>();
    private final Map<ApiUsageRecordKey, Long> gaugesReportCycles = new HashMap<>();
    private static long gaugeReportInterval = TimeUnit.MINUTES.toMillis(3);

    @Getter
    private final ApiUsageState apiUsageState;
    @Getter
    private volatile long currentCycleTs;
    @Getter
    private volatile long nextCycleTs;
    @Getter
    private volatile long currentHourTs;

    public BaseApiUsageState(ApiUsageState apiUsageState) {
        this.apiUsageState = apiUsageState;
        this.currentCycleTs = SchedulerUtils.getStartOfCurrentMonth();
        this.nextCycleTs = SchedulerUtils.getStartOfNextMonth();
        this.currentHourTs = SchedulerUtils.getStartOfCurrentHour();
    }

    public StatsCalculationResult calculate(ApiUsageRecordKey key, long value, String serviceId) {
        long currentValue = get(key);
        long currentHourlyValue = getHourly(key);

        long newValue;
        long newHourlyValue;
        if (key.isCounter()) {
            newValue = currentValue + value;
            newHourlyValue = currentHourlyValue + value;
        } else {
            Long newGaugeValue = calculateGauge(key, value, serviceId);
            newValue = newGaugeValue != null ? newGaugeValue : currentValue;
            newHourlyValue = newGaugeValue != null ? Math.max(newGaugeValue, currentHourlyValue) : currentHourlyValue;
        }
        set(key, newValue);
        setHourly(key, newHourlyValue);

        return StatsCalculationResult.of(newValue, newHourlyValue);
    }

    private Long calculateGauge(ApiUsageRecordKey key, long value, String serviceId) {
        Map<String, Long> lastByServiceId = lastGaugesByServiceId.computeIfAbsent(key, k -> {
            gaugesReportCycles.put(key, System.currentTimeMillis());
            return new HashMap<>();
        });
        lastByServiceId.put(serviceId, value);

        Long gaugeReportCycle = gaugesReportCycles.get(key);
        if (gaugeReportCycle <= System.currentTimeMillis() - gaugeReportInterval) {
            long newValue = lastByServiceId.values().stream().mapToLong(Long::longValue).sum();
            lastGaugesByServiceId.remove(key);
            gaugesReportCycles.remove(key);
            return newValue;
        } else {
            return null;
        }
    }

    public void set(ApiUsageRecordKey key, Long value) {
        currentCycleValues.put(key, value);
    }

    public long get(ApiUsageRecordKey key) {
        return currentCycleValues.getOrDefault(key, 0L);
    }

    public void setHourly(ApiUsageRecordKey key, Long value) {
        currentHourValues.put(key, value);
    }

    public long getHourly(ApiUsageRecordKey key) {
        return currentHourValues.getOrDefault(key, 0L);
    }

    public void setHour(long currentHourTs) {
        this.currentHourTs = currentHourTs;
        currentHourValues.clear();
        lastGaugesByServiceId.clear();
        gaugesReportCycles.clear();
    }

    public void setCycles(long currentCycleTs, long nextCycleTs) {
        this.currentCycleTs = currentCycleTs;
        this.nextCycleTs = nextCycleTs;
        currentCycleValues.clear();
    }

    public void onRepartitionEvent() {
        lastGaugesByServiceId.clear();
        gaugesReportCycles.clear();
    }

    public ApiUsageStateValue getFeatureValue(ApiFeature feature) {
        switch (feature) {
            case TRANSPORT:
                return apiUsageState.getTransportState();
            case RE:
                return apiUsageState.getReExecState();
            case DB:
                return apiUsageState.getDbStorageState();
            case JS:
                return apiUsageState.getJsExecState();
            case EMAIL:
                return apiUsageState.getEmailExecState();
            case SMS:
                return apiUsageState.getSmsExecState();
            case ALARM:
                return apiUsageState.getAlarmExecState();
            default:
                return ApiUsageStateValue.ENABLED;
        }
    }

    public boolean setFeatureValue(ApiFeature feature, ApiUsageStateValue value) {
        ApiUsageStateValue currentValue = getFeatureValue(feature);
        switch (feature) {
            case TRANSPORT:
                apiUsageState.setTransportState(value);
                break;
            case RE:
                apiUsageState.setReExecState(value);
                break;
            case DB:
                apiUsageState.setDbStorageState(value);
                break;
            case JS:
                apiUsageState.setJsExecState(value);
                break;
            case EMAIL:
                apiUsageState.setEmailExecState(value);
                break;
            case SMS:
                apiUsageState.setSmsExecState(value);
                break;
            case ALARM:
                apiUsageState.setAlarmExecState(value);
                break;
        }
        return !currentValue.equals(value);
    }

    public abstract EntityType getEntityType();

    public TenantId getTenantId() {
        return getApiUsageState().getTenantId();
    }

    public EntityId getEntityId() {
        return getApiUsageState().getEntityId();
    }

    @Data(staticConstructor = "of")
    public static class StatsCalculationResult {
        private final long newValue;
        private final long newHourlyValue;
    }

}
