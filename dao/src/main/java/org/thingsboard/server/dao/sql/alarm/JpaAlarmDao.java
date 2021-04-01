/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.query.AlarmQueryRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Slf4j
@Component
public class JpaAlarmDao extends JpaAbstractDao<AlarmEntity, Alarm> implements AlarmDao {

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private AlarmQueryRepository alarmQueryRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<AlarmEntity> getEntityClass() {
        return AlarmEntity.class;
    }

    @Override
    protected CrudRepository<AlarmEntity, UUID> getCrudRepository() {
        return alarmRepository;
    }

    @Override
    public Boolean deleteAlarm(TenantId tenantId, Alarm alarm) {
        return removeById(tenantId, alarm.getUuidId());
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return service.submit(() -> {
            List<AlarmEntity> latest = alarmRepository.findLatestByOriginatorAndType(
                    originator.getId(),
                    type,
                    PageRequest.of(0, 1));
            return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
        });
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key) {
        return findByIdAsync(tenantId, key);
    }

    @Override
    public PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query) {
        log.trace("Try to find alarms by entity [{}], status [{}] and pageLink [{}]", query.getAffectedEntityId(), query.getStatus(), query.getPageLink());
        EntityId affectedEntity = query.getAffectedEntityId();
        Set<AlarmStatus> statusSet = null;
        if (query.getSearchStatus() != null) {
            statusSet = query.getSearchStatus().getStatuses();
        } else if (query.getStatus() != null) {
            statusSet = Collections.singleton(query.getStatus());
        }
        return DaoUtil.toPageData(
                alarmRepository.findAlarms(
                        tenantId.getId(),
                        affectedEntity.getId(),
                        affectedEntity.getEntityType().name(),
                        query.getPageLink().getStartTime(),
                        query.getPageLink().getEndTime(),
                        statusSet,
                        Objects.toString(query.getPageLink().getTextSearch(), ""),
                        DaoUtil.toPageable(query.getPageLink())
                )
        );
    }

    @Override
    public long findAlarmCount(TenantId tenantId, AlarmQuery query, AlarmFilter filter) {
        log.trace("Try to find alarm count by entity [{}], status [{}], pageLink [{}] and filter", query.getAffectedEntityId(), query.getStatus(), query.getPageLink(), filter);
        EntityId affectedEntity = query.getAffectedEntityId();
        Long startTime;
        if (query.getPageLink().getStartTime() != null && filter.getStartTime() != null) {
            startTime = Math.max(query.getPageLink().getStartTime(), filter.getStartTime());
        } else {
            startTime = query.getPageLink().getStartTime() != null ? query.getPageLink().getStartTime() : filter.getStartTime();
        }
        return alarmRepository.findAlarmCount(
                tenantId.getId(),
                affectedEntity.getId(),
                affectedEntity.getEntityType().name(),
                startTime,
                query.getPageLink().getEndTime(),
                filter.getTypesList(),
                filter.getSeverityList(),
                filter.getStatusList()
        );
    }


    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, CustomerId customerId, MergedUserPermissions mergedUserPermissions, AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return alarmQueryRepository.findAlarmDataByQueryForEntities(tenantId, customerId, mergedUserPermissions, query, orderedEntityIds);
    }

    @Override
    public Set<AlarmSeverity> findAlarmSeverities(TenantId tenantId, EntityId entityId, Set<AlarmStatus> statuses) {
        return alarmRepository.findAlarmSeverities(tenantId.getId(), entityId.getId(), entityId.getEntityType().name(), statuses);
    }
}
