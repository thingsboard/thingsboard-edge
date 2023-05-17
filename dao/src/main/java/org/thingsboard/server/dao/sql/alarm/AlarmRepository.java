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
package org.thingsboard.server.dao.sql.alarm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.AlarmInfoEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
public interface AlarmRepository extends JpaRepository<AlarmEntity, UUID> {

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType ORDER BY a.startTs DESC")
    List<AlarmEntity> findLatestByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                    @Param("alarmType") String alarmType,
                                                    Pageable pageable);

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType AND a.cleared = FALSE ORDER BY a.createdTime DESC")
    List<AlarmEntity> findLatestActiveByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                          @Param("alarmType") String alarmType,
                                                          Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) "
            ,
            countQuery = "" +
                    "SELECT count(a) " + //alarms with relations only
                    "FROM AlarmInfoEntity a " +
                    "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND ea.tenantId = :tenantId " +
                    "AND ea.entityId = :affectedEntityId " +
                    "AND ea.entityType = :affectedEntityType " +
                    "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
                    "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findAlarms(@Param("tenantId") UUID tenantId,
                                     @Param("affectedEntityId") UUID affectedEntityId,
                                     @Param("affectedEntityType") String affectedEntityType,
                                     @Param("startTime") Long startTime,
                                     @Param("endTime") Long endTime,
                                     @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                     @Param("clearFilter") boolean clearFilter,
                                     @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                     @Param("ackFilter") boolean ackFilter,
                                     @Param("assigneeId") String assigneeId,
                                     @Param("searchText") String searchText,
                                     Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) "
            ,
            countQuery = "" +
                    "SELECT count(a) " + //alarms with relations only
                    "FROM AlarmInfoEntity a " +
                    "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND ea.tenantId = :tenantId " +
                    "AND ea.entityId = :affectedEntityId " +
                    "AND ea.entityType = :affectedEntityType " +
                    "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
                    "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
                    "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
                    "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findAlarmsV2(@Param("tenantId") UUID tenantId,
                                       @Param("affectedEntityId") UUID affectedEntityId,
                                       @Param("affectedEntityType") String affectedEntityType,
                                       @Param("startTime") Long startTime,
                                       @Param("endTime") Long endTime,
                                       @Param("alarmTypes") List<String> alarmTypes,
                                       @Param("alarmSeverities") List<AlarmSeverity> alarmSeverities,
                                       @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                       @Param("clearFilter") boolean clearFilter,
                                       @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                       @Param("ackFilter") boolean ackFilter,
                                       @Param("assigneeId") String assigneeId,
                                       @Param("searchText") String searchText,
                                       Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ",
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findAllAlarms(@Param("tenantId") UUID tenantId,
                                        @Param("startTime") Long startTime,
                                        @Param("endTime") Long endTime,
                                        @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                        @Param("clearFilter") boolean clearFilter,
                                        @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                        @Param("ackFilter") boolean ackFilter,
                                        @Param("assigneeId") String assigneeId,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ",
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
                    "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findAllAlarmsV2(@Param("tenantId") UUID tenantId,
                                          @Param("startTime") Long startTime,
                                          @Param("endTime") Long endTime,
                                          @Param("alarmTypes") List<String> alarmTypes,
                                          @Param("alarmSeverities") List<AlarmSeverity> alarmSeverities,
                                          @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                          @Param("clearFilter") boolean clearFilter,
                                          @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                          @Param("ackFilter") boolean ackFilter,
                                          @Param("assigneeId") String assigneeId,
                                          @Param("searchText") String searchText,
                                          Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) "
            ,
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findCustomerAlarms(@Param("tenantId") UUID tenantId,
                                             @Param("customerId") UUID customerId,
                                             @Param("startTime") Long startTime,
                                             @Param("endTime") Long endTime,
                                             @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                             @Param("clearFilter") boolean clearFilter,
                                             @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                             @Param("ackFilter") boolean ackFilter,
                                             @Param("assigneeId") String assigneeId,
                                             @Param("searchText") String searchText,
                                             Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) "
            ,
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
                    "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
                    "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findCustomerAlarmsV2(@Param("tenantId") UUID tenantId,
                                               @Param("customerId") UUID customerId,
                                               @Param("startTime") Long startTime,
                                               @Param("endTime") Long endTime,
                                               @Param("alarmTypes") List<String> alarmTypes,
                                               @Param("alarmSeverities") List<AlarmSeverity> alarmSeverities,
                                               @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                               @Param("clearFilter") boolean clearFilter,
                                               @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                               @Param("ackFilter") boolean ackFilter,
                                               @Param("assigneeId") String assigneeId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query(value = "SELECT a.severity FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = uuid(:assigneeId))")
    Set<AlarmSeverity> findAlarmSeverities(@Param("tenantId") UUID tenantId,
                                           @Param("affectedEntityId") UUID affectedEntityId,
                                           @Param("affectedEntityType") String affectedEntityType,
                                           @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                           @Param("clearFilter") boolean clearFilter,
                                           @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                           @Param("ackFilter") boolean ackFilter,
                                           @Param("assigneeId") String assigneeId);

    @Query("SELECT COUNT(a) " +
            "FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:typesList) IS NULL OR (a.type in (:typesList) AND ea.alarmType in (:typesList))) " +
            "AND ((:severityList) IS NULL OR a.severity in (:severityList)) " +
            "AND ((:clearFilterEnabled) IS FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) IS FALSE OR a.acknowledged = :ackFilter)")
    long findAlarmCount(@Param("tenantId") UUID tenantId,
                        @Param("affectedEntityId") UUID affectedEntityId,
                        @Param("affectedEntityType") String affectedEntityType,
                        @Param("startTime") Long startTime,
                        @Param("endTime") Long endTime,
                        @Param("typesList") List<String> typesList,
                        @Param("severityList") List<AlarmSeverity> severityList,
                        @Param("clearFilterEnabled") boolean clearFilterEnabled,
                        @Param("clearFilter") boolean clearFilter,
                        @Param("ackFilterEnabled") boolean ackFilterEnabled,
                        @Param("ackFilter") boolean ackFilter);

    @Query("SELECT a.id FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.createdTime < :time AND a.endTs < :time")
    Page<UUID> findAlarmsIdsByEndTsBeforeAndTenantId(@Param("time") Long time, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query(value = "SELECT a FROM AlarmInfoEntity a WHERE a.tenantId = :tenantId AND a.id = :alarmId")
    AlarmInfoEntity findAlarmInfoById(@Param("tenantId") UUID tenantId, @Param("alarmId") UUID alarmId);

    @Query(value = "SELECT create_or_update_active_alarm(:t_id, :c_id, :a_id, :a_created_ts, :a_o_id, :a_o_type, :a_type, :a_severity, " +
            ":a_start_ts, :a_end_ts, :a_details, :a_propagate, :a_propagate_to_owner, " +
            ":a_propagate_to_tenant, :a_propagation_types, :a_creation_enabled)", nativeQuery = true)
    String createOrUpdateActiveAlarm(@Param("t_id") UUID tenantId, @Param("c_id") UUID customerId,
                                     @Param("a_id") UUID alarmId, @Param("a_created_ts") long createdTime,
                                     @Param("a_o_id") UUID originatorId, @Param("a_o_type") int originatorType,
                                     @Param("a_type") String type, @Param("a_severity") String severity,
                                     @Param("a_start_ts") long startTs, @Param("a_end_ts") long endTs, @Param("a_details") String detailsAsString,
                                     @Param("a_propagate") boolean propagate, @Param("a_propagate_to_owner") boolean propagateToOwner,
                                     @Param("a_propagate_to_owner_hierarchy") boolean propagateToOwnerHierarchy,
                                     @Param("a_propagate_to_tenant") boolean propagateToTenant, @Param("a_propagation_types") String propagationTypes,
                                     @Param("a_creation_enabled") boolean alarmCreationEnabled);

    @Query(value = "SELECT update_alarm(:t_id, :a_id, :a_severity, :a_start_ts, :a_end_ts, :a_details, :a_propagate, :a_propagate_to_owner, " +
            ":a_propagate_to_tenant, :a_propagation_types)", nativeQuery = true)
    String updateAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_severity") String severity,
                       @Param("a_start_ts") long startTs, @Param("a_end_ts") long endTs, @Param("a_details") String detailsAsString,
                       @Param("a_propagate") boolean propagate, @Param("a_propagate_to_owner") boolean propagateToOwner,
                       @Param("a_propagate_to_owner_hierarchy") boolean propagateToOwnerHierarchy,
                       @Param("a_propagate_to_tenant") boolean propagateToTenant, @Param("a_propagation_types") String propagationTypes);

    @Query(value = "SELECT acknowledge_alarm(:t_id, :a_id, :a_ts)", nativeQuery = true)
    String acknowledgeAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_ts") long ts);

    @Query(value = "SELECT clear_alarm(:t_id, :a_id, :a_ts, :a_details)", nativeQuery = true)
    String clearAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_ts") long ts, @Param("a_details") String details);

    @Query(value = "SELECT assign_alarm(:t_id, :a_id, :u_id, :a_ts)", nativeQuery = true)
    String assignAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("u_id") UUID userId, @Param("a_ts") long assignTime);

    @Query(value = "SELECT unassign_alarm(:t_id, :a_id, :a_ts)", nativeQuery = true)
    String unassignAlarm(@Param("t_id") UUID tenantId, @Param("a_id") UUID alarmId, @Param("a_ts") long unassignTime);
}
