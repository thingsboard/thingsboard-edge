/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.cloud;

import org.thingsboard.server.dao.model.sql.TsKvCloudEventEntity;

import java.util.UUID;

public interface TsKvCloudEventRepository extends BaseCloudEventRepository<TsKvCloudEventEntity, UUID> {

}

//public interface TsKvCloudEventRepository extends JpaRepository<TsKvCloudEventEntity, UUID>, JpaSpecificationExecutor<TsKvCloudEventEntity> {
//
//    @Query("SELECT e FROM TsKvCloudEventEntity e WHERE " +
//            "e.tenantId = :tenantId " +
//            "AND (:startTime IS NULL OR e.createdTime >= :startTime) " +
//            "AND (:endTime IS NULL OR e.createdTime <= :endTime) " +
//            "AND (:seqIdStart IS NULL OR e.seqId > :seqIdStart) " +
//            "AND (:seqIdEnd IS NULL OR e.seqId < :seqIdEnd)"
//    )
//    Page<TsKvCloudEventEntity> findEventsByTenantId(@Param("tenantId") UUID tenantId,
//                                                    @Param("startTime") Long startTime,
//                                                    @Param("endTime") Long endTime,
//                                                    @Param("seqIdStart") Long seqIdStart,
//                                                    @Param("seqIdEnd") Long seqIdEnd,
//                                                    Pageable pageable);
//
//    @Query("SELECT COUNT(e) FROM TsKvCloudEventEntity e WHERE " +
//            "e.tenantId = :tenantId " +
//            "AND e.entityId  = :entityId " +
//            "AND e.cloudEventType = :cloudEventType " +
//            "AND e.cloudEventAction = :cloudEventAction " +
//            "AND (:startTime IS NULL OR e.createdTime > :startTime) " +
//            "AND (:endTime IS NULL OR e.createdTime <= :endTime) "
//    )
//    long countEventsByTenantIdAndEntityIdAndActionAndTypeAndStartTimeAndEndTime(@Param("tenantId") UUID tenantId,
//                                                                                @Param("entityId") UUID entityId,
//                                                                                @Param("cloudEventType") CloudEventType cloudEventType,
//                                                                                @Param("cloudEventAction") EdgeEventActionType cloudEventAction,
//                                                                                @Param("startTime") Long startTime,
//                                                                                @Param("endTime") Long endTime);
//
//}
