// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.cloud;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;

import java.util.UUID;

@NoRepositoryBean
public interface BaseCloudEventRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    @Query("SELECT e FROM #{#entityName} e WHERE " +
            "e.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR e.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime) " +
            "AND (:seqIdStart IS NULL OR e.seqId > :seqIdStart) " +
            "AND (:seqIdEnd IS NULL OR e.seqId < :seqIdEnd)"
    )
    Page<T> findEventsByTenantId(@Param("tenantId") UUID tenantId,
                                 @Param("startTime") Long startTime,
                                 @Param("endTime") Long endTime,
                                 @Param("seqIdStart") Long seqIdStart,
                                 @Param("seqIdEnd") Long seqIdEnd,
                                 Pageable pageable);

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId  = :entityId " +
            "AND e.cloudEventType = :cloudEventType " +
            "AND e.cloudEventAction = :cloudEventAction " +
            "AND (:startTime IS NULL OR e.createdTime > :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime) "
    )
    long countEventsByTenantIdAndEntityIdAndActionAndTypeAndStartTimeAndEndTime(@Param("tenantId") UUID tenantId,
                                                                                @Param("entityId") UUID entityId,
                                                                                @Param("cloudEventType") CloudEventType cloudEventType,
                                                                                @Param("cloudEventAction") EdgeEventActionType cloudEventAction,
                                                                                @Param("startTime") Long startTime,
                                                                                @Param("endTime") Long endTime);
}
