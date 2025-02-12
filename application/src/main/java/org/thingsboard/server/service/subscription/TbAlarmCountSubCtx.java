/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.subscription;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUpdate;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ToString(callSuper = true)
public class TbAlarmCountSubCtx extends TbAbstractEntityQuerySubCtx<AlarmCountQuery> {

    private final AlarmService alarmService;

    protected final Map<Integer, EntityId> subToEntityIdMap;

    @Getter
    private LinkedHashSet<EntityId> entitiesIds;

    private final int maxEntitiesPerAlarmSubscription;

    private final int maxAlarmQueriesPerRefreshInterval;

    @Getter
    @Setter
    private volatile int result;

    @Getter
    @Setter
    private boolean tooManyEntities;

    private int alarmCountInvocationAttempts;

    public TbAlarmCountSubCtx(String serviceId, WebSocketService wsService,
                              EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                              AttributesService attributesService, SubscriptionServiceStatistics stats, AlarmService alarmService,
                              WebSocketSessionRef sessionRef, int cmdId, int maxEntitiesPerAlarmSubscription, int maxAlarmQueriesPerRefreshInterval) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.alarmService = alarmService;
        this.subToEntityIdMap = new ConcurrentHashMap<>();
        this.maxEntitiesPerAlarmSubscription = maxEntitiesPerAlarmSubscription;
        this.maxAlarmQueriesPerRefreshInterval = maxAlarmQueriesPerRefreshInterval;
        this.entitiesIds = null;
    }

    @Override
    public void clearSubscriptions() {
        clearAlarmSubscriptions();
    }

    @Override
    public void fetchData() {
        resetInvocationCounter();
        if (query.getEntityFilter() != null) {
            entitiesIds = new LinkedHashSet<>();
            log.trace("[{}] Fetching data: {}", cmdId, alarmCountInvocationAttempts);
            PageData<EntityData> data = entityService.findEntityDataByQuery(getTenantId(), getCustomerId(), getMergedUserPermissions(), buildEntityDataQuery());
            entitiesIds.clear();
            tooManyEntities = data.hasNext();
            for (EntityData entityData : data.getData()) {
                entitiesIds.add(entityData.getEntityId());
            }
        }
    }

    @Override
    protected void update() {
        resetInvocationCounter();
        fetchAlarmCount();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    public void fetchAlarmCount() {
        alarmCountInvocationAttempts++;
        log.trace("[{}] Fetching alarms: {}", cmdId, alarmCountInvocationAttempts);
        if (alarmCountInvocationAttempts <= maxAlarmQueriesPerRefreshInterval) {
            int newCount = (int) alarmService.countAlarmsByQuery(getTenantId(), getCustomerId(), getMergedUserPermissions(), query, entitiesIds);
            if (newCount != result) {
                result = newCount;
                sendWsMsg(new AlarmCountUpdate(cmdId, result));
            }
        } else {
            log.trace("[{}] Ignore alarm count fetch due to rate limit: [{}] of maximum [{}]", cmdId, alarmCountInvocationAttempts, maxAlarmQueriesPerRefreshInterval);
        }
    }

    public void doFetchAlarmCount() {
        result = (int) alarmService.countAlarmsByQuery(getTenantId(), getCustomerId(), getMergedUserPermissions(), query, entitiesIds);
        sendWsMsg(new AlarmCountUpdate(cmdId, result));
    }

    private EntityDataQuery buildEntityDataQuery() {
        EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null,
                new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, ModelConstants.CREATED_TIME_PROPERTY)));
        return new EntityDataQuery(query.getEntityFilter(), edpl, null, null, query.getKeyFilters());
    }

    private void resetInvocationCounter() {
        alarmCountInvocationAttempts = 0;
    }

    public void createAlarmSubscriptions() {
        for (EntityId entityId : entitiesIds) {
            createAlarmSubscriptionForEntity(entityId);
        }
    }

    private void createAlarmSubscriptionForEntity(EntityId entityId) {
        int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
        subToEntityIdMap.put(subIdx, entityId);
        log.trace("[{}][{}][{}] Creating alarms subscription for [{}] ", serviceId, cmdId, subIdx, entityId);
        TbAlarmsSubscription subscription = TbAlarmsSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(subIdx)
                .tenantId(sessionRef.getSecurityCtx().getTenantId())
                .entityId(entityId)
                .updateProcessor((sub, update) -> fetchAlarmCount())
                .build();
        localSubscriptionService.addSubscription(subscription, sessionRef);
    }

    public void clearAlarmSubscriptions() {
        if (subToEntityIdMap != null) {
            for (Integer subId : subToEntityIdMap.keySet()) {
                localSubscriptionService.cancelSubscription(getTenantId(), getSessionId(), subId);
            }
            subToEntityIdMap.clear();
        }
    }

}
