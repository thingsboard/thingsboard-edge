/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sqlts.timescale;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvEntity;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import java.util.List;

@TimescaleDBTsDao
public interface TsKvTimescaleRepository extends CrudRepository<TimescaleTsKvEntity, TimescaleTsKvCompositeKey> {

    @Query("SELECT tskv FROM TimescaleTsKvEntity tskv WHERE tskv.tenantId = :tenantId " +
            "AND tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey " +
            "AND tskv.ts > :startTs AND tskv.ts <= :endTs")
    List<TimescaleTsKvEntity> findAllWithLimit(
            @Param("tenantId") String tenantId,
            @Param("entityId") String entityId,
            @Param("entityKey") String key,
            @Param("startTs") long startTs,
            @Param("endTs") long endTs, Pageable pageable);

    @Query(value = "SELECT tskv.tenant_id as tenant_id, tskv.entity_id as entity_id, tskv.key as key, last(tskv.ts,tskv.ts) as ts," +
            " last(tskv.bool_v, tskv.ts) as bool_v, last(tskv.str_v, tskv.ts) as str_v," +
            " last(tskv.long_v, tskv.ts) as long_v, last(tskv.dbl_v, tskv.ts) as dbl_v" +
            " FROM tenant_ts_kv tskv WHERE tskv.tenant_id = cast(:tenantId AS varchar) " +
            "AND tskv.entity_id = cast(:entityId AS varchar) " +
            "GROUP BY tskv.tenant_id, tskv.entity_id, tskv.key", nativeQuery = true)
    List<TimescaleTsKvEntity> findAllLatestValues(
            @Param("tenantId") String tenantId,
            @Param("entityId") String entityId);

    @Transactional
    @Modifying
    @Query("DELETE FROM TimescaleTsKvEntity tskv WHERE tskv.tenantId = :tenantId " +
            "AND tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey " +
            "AND tskv.ts > :startTs AND tskv.ts <= :endTs")
    void delete(@Param("tenantId") String tenantId,
                @Param("entityId") String entityId,
                @Param("entityKey") String key,
                @Param("startTs") long startTs,
                @Param("endTs") long endTs);

}