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
package org.thingsboard.server.service.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.alarm.AlarmOperationResult;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmCommentService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultAlarmSubscriptionService extends AbstractSubscriptionService implements AlarmSubscriptionService {

    private final AlarmService alarmService;
    private final TbAlarmCommentService alarmCommentService;
    private final TbApiUsageReportClient apiUsageClient;
    private final TbApiUsageStateService apiUsageStateService;

    @Override
    protected String getExecutorPrefix() {
        return "alarm";
    }

    @Override
    public Alarm createOrUpdateAlarm(Alarm alarm) {
        AlarmOperationResult result = alarmService.createOrUpdateAlarm(alarm, apiUsageStateService.getApiUsageState(alarm.getTenantId()).isAlarmCreationEnabled());
        if (result != null) {
            if (result.isSuccessful()) {
                onAlarmUpdated(result);
                AlarmSeverity oldSeverity = result.getOldSeverity();
                if (oldSeverity != null && !oldSeverity.equals(result.getAlarm().getSeverity())) {
                    AlarmComment alarmComment = AlarmComment.builder()
                            .alarmId(alarm.getId())
                            .type(AlarmCommentType.SYSTEM)
                            .comment(JacksonUtil.newObjectNode().put("text", String.format("Alarm severity was updated from %s to %s", oldSeverity, result.getAlarm().getSeverity())))
                            .build();
                    try {
                        alarmCommentService.saveAlarmComment(alarm, alarmComment, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to save alarm comment", e);
                    }
                }
            }
            if (result.isCreated()) {
                apiUsageClient.report(alarm.getTenantId(), null, ApiUsageRecordKey.CREATED_ALARMS_COUNT);
            }
            return result.getAlarm();
        } else {
            log.warn("Failed to create or update the Alarm: [{}].", alarm);
            return null;
        }
    }

    @Override
    public Boolean deleteAlarm(TenantId tenantId, AlarmId alarmId) {
        AlarmOperationResult result = alarmService.deleteAlarm(tenantId, alarmId);
        onAlarmDeleted(result);
        return result.isSuccessful();
    }

    @Override
    public ListenableFuture<Boolean> ackAlarm(TenantId tenantId, AlarmId alarmId, long ackTs) {
        ListenableFuture<AlarmOperationResult> result = alarmService.ackAlarm(tenantId, alarmId, ackTs);
        Futures.addCallback(result, new AlarmUpdateCallback(), wsCallBackExecutor);
        return Futures.transform(result, AlarmOperationResult::isSuccessful, wsCallBackExecutor);
    }

    @Override
    public ListenableFuture<Boolean> clearAlarm(TenantId tenantId, AlarmId alarmId, JsonNode details, long clearTs) {
        ListenableFuture<AlarmOperationResult> result = clearAlarmForResult(tenantId, alarmId, details, clearTs);
        return Futures.transform(result, AlarmOperationResult::isSuccessful, wsCallBackExecutor);
    }

    @Override
    public ListenableFuture<AlarmOperationResult> clearAlarmForResult(TenantId tenantId, AlarmId alarmId, JsonNode details, long clearTs) {
        ListenableFuture<AlarmOperationResult> result = alarmService.clearAlarm(tenantId, alarmId, details, clearTs);
        Futures.addCallback(result, new AlarmUpdateCallback(), wsCallBackExecutor);
        return result;
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId) {
        return alarmService.findAlarmByIdAsync(tenantId, alarmId);
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, AlarmId alarmId) {
        return alarmService.findAlarmById(tenantId, alarmId);
    }

    @Override
    public ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId) {
        return alarmService.findAlarmInfoByIdAsync(tenantId, alarmId);
    }

    @Override
    public ListenableFuture<PageData<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query) {
        return alarmService.findAlarms(tenantId, query);
    }

    @Override
    public ListenableFuture<PageData<AlarmInfo>> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query) {
        return alarmService.findCustomerAlarms(tenantId, customerId, query);
    }

    @Override
    public AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus, AlarmStatus alarmStatus) {
        return alarmService.findHighestAlarmSeverity(tenantId, entityId, alarmSearchStatus, alarmStatus);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, MergedUserPermissions mergedUserPermissions,
                                                               AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, query, orderedEntityIds);
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmService.findLatestByOriginatorAndType(tenantId, originator, type);
    }

    @Override
    public List<Long> findAlarmCounts(TenantId tenantId, AlarmQuery query, List<AlarmFilter> filters) {
        return alarmService.findAlarmCounts(tenantId, query, filters);
    }

    @Override
    public Set<EntityId> getPropagationEntityIds(Alarm alarm) {
        return alarmService.getPropagationEntityIds(alarm);
    }

    @Override
    public Set<EntityId> getPropagationEntityIds(Alarm alarm, List<EntityType> types) {
        return alarmService.getPropagationEntityIds(alarm, types);
    }

    private void onAlarmUpdated(AlarmOperationResult result) {
        wsCallBackExecutor.submit(() -> {
            Alarm alarm = result.getAlarm();
            TenantId tenantId = result.getAlarm().getTenantId();
            for (EntityId entityId : result.getPropagatedEntitiesList()) {
                forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
                    subscriptionManagerService.onAlarmUpdate(tenantId, entityId, alarm, TbCallback.EMPTY);
                }, () -> {
                    return TbSubscriptionUtils.toAlarmUpdateProto(tenantId, entityId, alarm);
                });
            }
        });
    }

    private void onAlarmDeleted(AlarmOperationResult result) {
        wsCallBackExecutor.submit(() -> {
            Alarm alarm = result.getAlarm();
            TenantId tenantId = result.getAlarm().getTenantId();
            for (EntityId entityId : result.getPropagatedEntitiesList()) {
                forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
                    subscriptionManagerService.onAlarmDeleted(tenantId, entityId, alarm, TbCallback.EMPTY);
                }, () -> {
                    return TbSubscriptionUtils.toAlarmDeletedProto(tenantId, entityId, alarm);
                });
            }
        });
    }

    private class AlarmUpdateCallback implements FutureCallback<AlarmOperationResult> {
        @Override
        public void onSuccess(@Nullable AlarmOperationResult result) {
            onAlarmUpdated(result);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to update alarm", t);
        }
    }

}
