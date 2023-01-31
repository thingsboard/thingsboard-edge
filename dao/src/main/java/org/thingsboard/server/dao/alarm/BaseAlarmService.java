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
package org.thingsboard.server.dao.alarm;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.exception.ApiUsageLimitsExceededException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.dao.service.DataValidator;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.dao.service.Validator.validateEntityDataPageLink;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmService extends AbstractEntityService implements AlarmService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";

    @Autowired
    private AlarmDao alarmDao;

    @Autowired
    private EntityService entityService;

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private DataValidator<Alarm> alarmDataValidator;

    protected ExecutorService readResultsProcessingExecutor;

    @PostConstruct
    public void startExecutor() {
        readResultsProcessingExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("alarm-service"));
    }

    @PreDestroy
    public void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    @Override
    public AlarmOperationResult createOrUpdateAlarm(Alarm alarm) {
        return createOrUpdateAlarm(alarm, true);
    }

    @Override
    public AlarmOperationResult createOrUpdateAlarm(Alarm alarm, boolean alarmCreationEnabled) {
        alarmDataValidator.validate(alarm, Alarm::getTenantId);
        try {
            if (alarm.getStartTs() == 0L) {
                alarm.setStartTs(System.currentTimeMillis());
            }
            if (alarm.getEndTs() == 0L) {
                alarm.setEndTs(alarm.getStartTs());
            }
            alarm.setCustomerId(entityService.fetchEntityCustomerId(alarm.getTenantId(), alarm.getOriginator()));
            if (alarm.getId() == null) {
                Alarm existing = alarmDao.findLatestByOriginatorAndType(alarm.getTenantId(), alarm.getOriginator(), alarm.getType());
                if (existing == null || existing.getStatus().isCleared()) {
                    if (!alarmCreationEnabled) {
                        throw new ApiUsageLimitsExceededException("Alarms creation is disabled");
                    }
                    return createAlarm(alarm);
                } else {
                    return updateAlarm(existing, alarm);
                }
            } else {
                return updateAlarm(alarm);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmDao.findLatestByOriginatorAndTypeAsync(tenantId, originator, type);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, MergedUserPermissions mergedUserPermissions,
                                                               AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateEntityDataPageLink(query.getPageLink());
        return alarmDao.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, query, orderedEntityIds);
    }

    @Override
    @Transactional
    public AlarmOperationResult deleteAlarm(TenantId tenantId, AlarmId alarmId) {
        log.debug("Deleting Alarm Id: {}", alarmId);
        Alarm alarm = alarmDao.findAlarmById(tenantId, alarmId.getId());
        if (alarm == null) {
            return new AlarmOperationResult(alarm, false);
        }
        AlarmOperationResult result = new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
        deleteEntityRelations(tenantId, alarm.getId());
        alarmDao.removeById(tenantId, alarm.getUuidId());
        return result;
    }

    private AlarmOperationResult createAlarm(Alarm alarm) throws InterruptedException, ExecutionException {
        log.debug("New Alarm : {}", alarm);
        Alarm saved = alarmDao.save(alarm.getTenantId(), alarm);
        List<EntityId> propagatedEntitiesList = createEntityAlarmRecords(saved);
        return new AlarmOperationResult(saved, true, true, propagatedEntitiesList);
    }

    private List<EntityId> createEntityAlarmRecords(Alarm alarm) throws InterruptedException, ExecutionException {
        Set<EntityId> propagatedEntitiesSet = new LinkedHashSet<>();
        propagatedEntitiesSet.add(alarm.getOriginator());
        if (alarm.isPropagate()) {
            propagatedEntitiesSet.addAll(getRelatedEntities(alarm));
        }
        if (alarm.isPropagateToOwnerHierarchy()) {
            propagatedEntitiesSet.addAll(ownerService.getOwners(alarm.getTenantId(), alarm.getOriginator()));
        } else if (alarm.isPropagateToOwner()) {
            propagatedEntitiesSet.add(ownerService.getOwner(alarm.getTenantId(), alarm.getOriginator()));
        }
        if (alarm.isPropagateToTenant()) {
            propagatedEntitiesSet.add(alarm.getTenantId());
        }
        for (EntityId entityId : propagatedEntitiesSet) {
            createEntityAlarmRecord(alarm.getTenantId(), entityId, alarm);
        }
        return new ArrayList<>(propagatedEntitiesSet);
    }

    private Set<EntityId> getRelatedEntities(Alarm alarm) throws InterruptedException, ExecutionException {
        EntityRelationsQuery commonQuery = new EntityRelationsQuery();
        commonQuery.setParameters(new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE, RelationTypeGroup.COMMON, false));
        EntityRelationsQuery groupQuery = new EntityRelationsQuery();
        groupQuery.setParameters(new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE, RelationTypeGroup.FROM_ENTITY_GROUP, false));
        List<String> propagateRelationTypes = alarm.getPropagateRelationTypes();
        Stream<EntityRelation> commonRelations = relationService.findByQuery(alarm.getTenantId(), commonQuery).get().stream();
        Stream<EntityRelation> groupRelations = relationService.findByQuery(alarm.getTenantId(), groupQuery).get().stream();
        if (!CollectionUtils.isEmpty(propagateRelationTypes)) {
            commonRelations = commonRelations.filter(entityRelation -> propagateRelationTypes.contains(entityRelation.getType()));
        }
        Set<EntityId> parentEntities = new LinkedHashSet<>();
        parentEntities.addAll(commonRelations.map(EntityRelation::getFrom).collect(Collectors.toList()));
        parentEntities.addAll(groupRelations.map(EntityRelation::getFrom).collect(Collectors.toList()));
        return parentEntities;
    }

    private AlarmOperationResult updateAlarm(Alarm update) {
        alarmDataValidator.validate(update, Alarm::getTenantId);
        return getAndUpdate(update.getTenantId(), update.getId(),
                (alarm) -> alarm == null ? null : updateAlarm(alarm, update));
    }

    private AlarmOperationResult updateAlarm(Alarm oldAlarm, Alarm newAlarm) {
        boolean propagationEnabled = !oldAlarm.isPropagate() && newAlarm.isPropagate();
        boolean propagationToOwnerEnabled = !oldAlarm.isPropagateToOwner() && newAlarm.isPropagateToOwner();
        boolean propagationToOwnerHierarchyEnabled = !oldAlarm.isPropagateToOwnerHierarchy() && newAlarm.isPropagateToOwnerHierarchy();
        boolean propagationToTenantEnabled = !oldAlarm.isPropagateToTenant() && newAlarm.isPropagateToTenant();
        Alarm result = alarmDao.save(newAlarm.getTenantId(), merge(oldAlarm, newAlarm));
        List<EntityId> propagatedEntitiesList;
        if (propagationEnabled || propagationToOwnerEnabled || propagationToTenantEnabled || propagationToOwnerHierarchyEnabled) {
            try {
                propagatedEntitiesList = createEntityAlarmRecords(result);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to update alarm relations [{}]", result, e);
                throw new RuntimeException(e);
            }
        } else {
            propagatedEntitiesList = new ArrayList<>(getPropagationEntityIds(result));
        }
        return new AlarmOperationResult(result, true, propagatedEntitiesList);
    }

    @Override
    public ListenableFuture<AlarmOperationResult> ackAlarm(TenantId tenantId, AlarmId alarmId, long ackTime) {
        return getAndUpdateAsync(tenantId, alarmId, new Function<Alarm, AlarmOperationResult>() {
            @Nullable
            @Override
            public AlarmOperationResult apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isAck()) {
                    return new AlarmOperationResult(alarm, false);
                } else {
                    AlarmStatus oldStatus = alarm.getStatus();
                    AlarmStatus newStatus = oldStatus.isCleared() ? AlarmStatus.CLEARED_ACK : AlarmStatus.ACTIVE_ACK;
                    alarm.setStatus(newStatus);
                    alarm.setAckTs(ackTime);
                    alarm = alarmDao.save(alarm.getTenantId(), alarm);
                    return new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
                }
            }
        });
    }

    @Override
    public ListenableFuture<AlarmOperationResult> clearAlarm(TenantId tenantId, AlarmId alarmId, JsonNode details, long clearTime) {
        return getAndUpdateAsync(tenantId, alarmId, new Function<Alarm, AlarmOperationResult>() {
            @Nullable
            @Override
            public AlarmOperationResult apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isCleared()) {
                    return new AlarmOperationResult(alarm, false);
                } else {
                    AlarmStatus oldStatus = alarm.getStatus();
                    AlarmStatus newStatus = oldStatus.isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK;
                    alarm.setStatus(newStatus);
                    alarm.setClearTs(clearTime);
                    if (details != null) {
                        alarm.setDetails(details);
                    }
                    alarm = alarmDao.save(alarm.getTenantId(), alarm);
                    return new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
                }
            }
        });
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmById [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmById(tenantId, alarmId.getId());
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
    }

    @Override
    public ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmInfoByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return Futures.transformAsync(alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId()),
                a -> {
                    AlarmInfo alarmInfo = new AlarmInfo(a);
                    return Futures.transform(
                            entityService.fetchEntityNameAsync(tenantId, alarmInfo.getOriginator()), originatorName -> {
                                alarmInfo.setOriginatorName(originatorName);
                                return alarmInfo;
                            }, MoreExecutors.directExecutor());
                }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<PageData<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query) {
        PageData<AlarmInfo> alarms = alarmDao.findAlarms(tenantId, query);
        if (query.getFetchOriginator() != null && query.getFetchOriginator().booleanValue()) {
            return fetchAlarmsOriginators(tenantId, alarms);
        }
        return Futures.immediateFuture(alarms);
    }

    @Override
    public ListenableFuture<PageData<AlarmInfo>> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query) {
        PageData<AlarmInfo> alarms = alarmDao.findCustomerAlarms(tenantId, customerId, query);
        if (query.getFetchOriginator() != null && query.getFetchOriginator().booleanValue()) {
            return fetchAlarmsOriginators(tenantId, alarms);
        }
        return Futures.immediateFuture(alarms);
    }

    private ListenableFuture<PageData<AlarmInfo>> fetchAlarmsOriginators(TenantId tenantId, PageData<AlarmInfo> alarms) {
        List<ListenableFuture<AlarmInfo>> alarmFutures = new ArrayList<>(alarms.getData().size());
        for (AlarmInfo alarmInfo : alarms.getData()) {
            alarmFutures.add(Futures.transform(
                    entityService.fetchEntityNameAsync(tenantId, alarmInfo.getOriginator()), originatorName -> {
                        if (originatorName == null) {
                            originatorName = "Deleted";
                        }
                        alarmInfo.setOriginatorName(originatorName);
                        return alarmInfo;
                    }, MoreExecutors.directExecutor()
            ));
        }
        return Futures.transform(Futures.successfulAsList(alarmFutures),
                alarmInfos -> new PageData<>(alarmInfos, alarms.getTotalPages(), alarms.getTotalElements(),
                        alarms.hasNext()), MoreExecutors.directExecutor());
    }

    @Override
    public List<Long> findAlarmCounts(TenantId tenantId, AlarmQuery query, List<AlarmFilter> filters) {
        List<Long> alarmCounts = new ArrayList<>();
        for (AlarmFilter filter : filters) {
            long count = alarmDao.findAlarmCount(tenantId, query, filter);
            alarmCounts.add(count);
        }
        return alarmCounts;
    }

    @Override
    public AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                                  AlarmStatus alarmStatus) {
        Set<AlarmStatus> statusList = null;
        if (alarmSearchStatus != null) {
            statusList = alarmSearchStatus.getStatuses();
        } else if (alarmStatus != null) {
            statusList = Collections.singleton(alarmStatus);
        }

        Set<AlarmSeverity> alarmSeverities = alarmDao.findAlarmSeverities(tenantId, entityId, statusList);

        return alarmSeverities.stream().min(AlarmSeverity::compareTo).orElse(null);
    }

    @Override
    public void deleteEntityAlarmRelations(TenantId tenantId, EntityId entityId) {
        alarmDao.deleteEntityAlarmRecords(tenantId, entityId);
    }

    private Alarm merge(Alarm existing, Alarm alarm) {
        if (alarm.getStartTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getStartTs());
        }
        if (alarm.getEndTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getEndTs());
        }
        if (alarm.getClearTs() > existing.getClearTs()) {
            existing.setClearTs(alarm.getClearTs());
        }
        if (alarm.getAckTs() > existing.getAckTs()) {
            existing.setAckTs(alarm.getAckTs());
        }
        existing.setStatus(alarm.getStatus());
        existing.setSeverity(alarm.getSeverity());
        existing.setDetails(alarm.getDetails());
        existing.setCustomerId(alarm.getCustomerId());
        existing.setPropagate(existing.isPropagate() || alarm.isPropagate());
        existing.setPropagateToOwner(existing.isPropagateToOwner() || alarm.isPropagateToOwner());
        existing.setPropagateToOwnerHierarchy(existing.isPropagateToOwnerHierarchy() || alarm.isPropagateToOwnerHierarchy());
        existing.setPropagateToTenant(existing.isPropagateToTenant() || alarm.isPropagateToTenant());
        List<String> existingPropagateRelationTypes = existing.getPropagateRelationTypes();
        List<String> newRelationTypes = alarm.getPropagateRelationTypes();
        if (!CollectionUtils.isEmpty(newRelationTypes)) {
            if (!CollectionUtils.isEmpty(existingPropagateRelationTypes)) {
                existing.setPropagateRelationTypes(Stream.concat(existingPropagateRelationTypes.stream(), newRelationTypes.stream())
                        .distinct()
                        .collect(Collectors.toList()));
            } else {
                existing.setPropagateRelationTypes(newRelationTypes);
            }
        }
        return existing;
    }

    @Override
    public Set<EntityId> getPropagationEntityIds(Alarm alarm) {
        return processGetPropagationEntityIds(alarm, null);
    }

    @Override
    public Set<EntityId> getPropagationEntityIds(Alarm alarm, List<EntityType> types) {
        return processGetPropagationEntityIds(alarm, types);
    }

    private Set<EntityId> processGetPropagationEntityIds(Alarm alarm, List<EntityType> types) {
        validateId(alarm.getId(), "Alarm id should be specified!");
        if (alarm.isPropagate() || alarm.isPropagateToOwner() || alarm.isPropagateToTenant() || alarm.isPropagateToOwnerHierarchy()) {
            List<EntityAlarm> entityAlarms = CollectionUtils.isEmpty(types) ?
                    alarmDao.findEntityAlarmRecords(alarm.getTenantId(), alarm.getId()) :
                    alarmDao.findEntityAlarmRecordsByEntityTypes(alarm.getTenantId(), alarm.getId(), types);
            return entityAlarms.stream().map(EntityAlarm::getEntityId).collect(Collectors.toSet());
        } else {
            return Collections.singleton(alarm.getOriginator());
        }
    }

    private void createEntityAlarmRecord(TenantId tenantId, EntityId entityId, Alarm alarm) {
        EntityAlarm entityAlarm = new EntityAlarm(tenantId, entityId, alarm.getCreatedTime(), alarm.getType(), alarm.getCustomerId(), alarm.getId());
        try {
            alarmDao.createEntityAlarmRecord(entityAlarm);
        } catch (Exception e) {
            log.warn("[{}] Failed to create entity alarm record: {}", tenantId, entityAlarm, e);
        }
    }

    private <T> ListenableFuture<T> getAndUpdateAsync(TenantId tenantId, AlarmId alarmId, Function<Alarm, T> function) {
        validateId(alarmId, "Alarm id should be specified!");
        ListenableFuture<Alarm> entity = alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
        return Futures.transform(entity, function, readResultsProcessingExecutor);
    }

    private <T> T getAndUpdate(TenantId tenantId, AlarmId alarmId, Function<Alarm, T> function) {
        validateId(alarmId, "Alarm id should be specified!");
        Alarm entity = alarmDao.findAlarmById(tenantId, alarmId.getId());
        return function.apply(entity);
    }
}
