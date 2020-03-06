/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.AlarmInfoEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@SqlDao
public interface AlarmRepository extends CrudRepository<AlarmEntity, String> {

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType ORDER BY a.startTs DESC")
    List<AlarmEntity> findLatestByOriginatorAndType(@Param("originatorId") String originatorId,
                                                    @Param("alarmType") String alarmType,
                                                    Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AlarmInfoEntity(a) FROM AlarmEntity a, " +
            "RelationEntity re " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.id = re.toId AND re.toType = 'ALARM' " +
            "AND re.relationTypeGroup = 'ALARM' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :affectedEntityId " +
            "AND re.fromType = :affectedEntityType " +
            "AND (:startId IS NULL OR a.id >= :startId) " +
            "AND (:endId IS NULL OR a.id <= :endId) " +
            "AND (:idOffset IS NULL OR a.id < :idOffset) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT(:searchText, '%'))" +
            "OR LOWER(a.severity) LIKE LOWER(CONCAT(:searchText, '%'))" +
            "OR LOWER(a.status) LIKE LOWER(CONCAT(:searchText, '%')))")
    Page<AlarmInfoEntity> findAlarms(@Param("tenantId") String tenantId,
                                     @Param("affectedEntityId") String affectedEntityId,
                                     @Param("affectedEntityType") String affectedEntityType,
                                     @Param("relationType") String relationType,
                                     @Param("startId") String startId,
                                     @Param("endId") String endId,
                                     @Param("idOffset") String idOffset,
                                     @Param("searchText") String searchText,
                                     Pageable pageable);

    @Query("SELECT COUNT(a) FROM AlarmEntity a, " +
            "RelationEntity re " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.id = re.toId AND re.toType = 'ALARM' " +
            "AND re.relationTypeGroup = 'ALARM' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :affectedEntityId " +
            "AND re.fromType = :affectedEntityType " +
            "AND (:startId IS NULL OR a.id >= :startId) " +
            "AND (:endId IS NULL OR a.id <= :endId) " +
            "AND (:typesList IS NULL OR a.type in :typesList) " +
            "AND (:severityList IS NULL OR a.severity in :severityList) " +
            "AND (:statusList IS NULL OR a.status in :statusList)")
    long findAlarmCount(@Param("tenantId") String tenantId,
                         @Param("affectedEntityId") String affectedEntityId,
                         @Param("affectedEntityType") String affectedEntityType,
                         @Param("relationType") String relationType,
                         @Param("startId") String startId,
                         @Param("endId") String endId,
                         @Param("typesList") List<String> typesList,
                         @Param("severityList") List<AlarmSeverity> severityList,
                         @Param("statusList") List<AlarmStatus> statusList);
}
