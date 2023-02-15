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
import org.thingsboard.server.common.data.alarm.AlarmStatus;
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

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AlarmInfoEntity(a) FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) "
            ,
            countQuery = "" +
                    "SELECT count(a) " + //alarms with relations only
                    "FROM AlarmEntity a " +
                    "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND ea.tenantId = :tenantId " +
                    "AND ea.entityId = :affectedEntityId " +
                    "AND ea.entityType = :affectedEntityType " +
                    "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
                    "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
                    "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findAlarms(@Param("tenantId") UUID tenantId,
                                     @Param("affectedEntityId") UUID affectedEntityId,
                                     @Param("affectedEntityType") String affectedEntityType,
                                     @Param("startTime") Long startTime,
                                     @Param("endTime") Long endTime,
                                     @Param("alarmStatuses") Set<AlarmStatus> alarmStatuses,
                                     @Param("searchText") String searchText,
                                     Pageable pageable);

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AlarmInfoEntity(a) FROM AlarmEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ",
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmEntity a " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findAllAlarms(@Param("tenantId") UUID tenantId,
                                        @Param("startTime") Long startTime,
                                        @Param("endTime") Long endTime,
                                        @Param("alarmStatuses") Set<AlarmStatus> alarmStatuses,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AlarmInfoEntity(a) FROM AlarmEntity a " +
            "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) "
            ,
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmEntity a " +
                    "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findCustomerAlarms(@Param("tenantId") UUID tenantId,
                                             @Param("customerId") UUID customerId,
                                             @Param("startTime") Long startTime,
                                             @Param("endTime") Long endTime,
                                             @Param("alarmStatuses") Set<AlarmStatus> alarmStatuses,
                                             @Param("searchText") String searchText,
                                             Pageable pageable);

    @Query(value = "SELECT a.severity FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses))")
    Set<AlarmSeverity> findAlarmSeverities(@Param("tenantId") UUID tenantId,
                                           @Param("affectedEntityId") UUID affectedEntityId,
                                           @Param("affectedEntityType") String affectedEntityType,
                                           @Param("alarmStatuses") Set<AlarmStatus> alarmStatuses);

    @Query("SELECT COUNT(a) " +
            "FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:typesList) IS NULL OR a.type in (:typesList)) " +
            "AND ((:severityList) IS NULL OR a.severity in (:severityList)) " +
            "AND ((:statusList) IS NULL OR a.status in (:statusList))")
    long findAlarmCount(@Param("tenantId") UUID tenantId,
                         @Param("affectedEntityId") UUID affectedEntityId,
                         @Param("affectedEntityType") String affectedEntityType,
                         @Param("startTime") Long startTime,
                         @Param("endTime") Long endTime,
                         @Param("typesList") List<String> typesList,
                         @Param("severityList") List<AlarmSeverity> severityList,
                         @Param("statusList") List<AlarmStatus> statusList);

    @Query("SELECT a.id FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.createdTime < :time AND a.endTs < :time")
    Page<UUID> findAlarmsIdsByEndTsBeforeAndTenantId(@Param("time") Long time, @Param("tenantId") UUID tenantId, Pageable pageable);

}
