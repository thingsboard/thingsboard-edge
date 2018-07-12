/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.timeseries;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.TsKvCompositeKey;
import org.thingsboard.server.dao.model.sql.TsKvEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SqlDao
public interface TsKvRepository extends CrudRepository<TsKvEntity, TsKvCompositeKey> {

    @Query("SELECT tskv FROM TsKvEntity tskv WHERE tskv.entityId = :entityId " +
            "AND tskv.entityType = :entityType AND tskv.key = :entityKey " +
            "AND tskv.ts > :startTs AND tskv.ts < :endTs ORDER BY tskv.ts DESC")
    List<TsKvEntity> findAllWithLimit(@Param("entityId") String entityId,
                                      @Param("entityType") EntityType entityType,
                                      @Param("entityKey") String key,
                                      @Param("startTs") long startTs,
                                      @Param("endTs") long endTs,
                                      Pageable pageable);

    @Async
    @Query("SELECT new TsKvEntity(MAX(tskv.strValue), MAX(tskv.longValue), MAX(tskv.doubleValue)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.entityType = :entityType " +
            "AND tskv.key = :entityKey AND tskv.ts > :startTs AND tskv.ts < :endTs")
    CompletableFuture<TsKvEntity> findMax(@Param("entityId") String entityId,
                                          @Param("entityType") EntityType entityType,
                                          @Param("entityKey") String entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);

    @Async
    @Query("SELECT new TsKvEntity(MIN(tskv.strValue), MIN(tskv.longValue), MIN(tskv.doubleValue)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.entityType = :entityType " +
            "AND tskv.key = :entityKey AND tskv.ts > :startTs AND tskv.ts < :endTs")
    CompletableFuture<TsKvEntity> findMin(@Param("entityId") String entityId,
                       @Param("entityType") EntityType entityType,
                       @Param("entityKey") String entityKey,
                       @Param("startTs") long startTs,
                       @Param("endTs") long endTs);

    @Async
    @Query("SELECT new TsKvEntity(COUNT(tskv.booleanValue), COUNT(tskv.strValue), COUNT(tskv.longValue), COUNT(tskv.doubleValue)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.entityType = :entityType " +
            "AND tskv.key = :entityKey AND tskv.ts > :startTs AND tskv.ts < :endTs")
    CompletableFuture<TsKvEntity> findCount(@Param("entityId") String entityId,
                                 @Param("entityType") EntityType entityType,
                                 @Param("entityKey") String entityKey,
                                 @Param("startTs") long startTs,
                                 @Param("endTs") long endTs);

    @Async
    @Query("SELECT new TsKvEntity(AVG(tskv.longValue), AVG(tskv.doubleValue)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.entityType = :entityType " +
            "AND tskv.key = :entityKey AND tskv.ts > :startTs AND tskv.ts < :endTs")
    CompletableFuture<TsKvEntity> findAvg(@Param("entityId") String entityId,
                       @Param("entityType") EntityType entityType,
                       @Param("entityKey") String entityKey,
                       @Param("startTs") long startTs,
                       @Param("endTs") long endTs);


    @Async
    @Query("SELECT new TsKvEntity(SUM(tskv.longValue), SUM(tskv.doubleValue)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.entityType = :entityType " +
            "AND tskv.key = :entityKey AND tskv.ts > :startTs AND tskv.ts < :endTs")
    CompletableFuture<TsKvEntity> findSum(@Param("entityId") String entityId,
                       @Param("entityType") EntityType entityType,
                       @Param("entityKey") String entityKey,
                       @Param("startTs") long startTs,
                       @Param("endTs") long endTs);
}
