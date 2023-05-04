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
package org.thingsboard.server.dao.sql.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.dao.model.sql.RuleNodeDebugEventEntity;

import java.util.List;
import java.util.UUID;


public interface RuleNodeDebugEventRepository extends EventRepository<RuleNodeDebugEventEntity, RuleNodeDebugEvent>, JpaRepository<RuleNodeDebugEventEntity, UUID> {

    @Override
    @Query(nativeQuery = true,  value = "SELECT * FROM rule_node_debug_event e WHERE e.tenant_id = :tenantId AND e.entity_id = :entityId ORDER BY e.ts DESC LIMIT :limit")
    List<RuleNodeDebugEventEntity> findLatestEvents(@Param("tenantId") UUID tenantId, @Param("entityId") UUID entityId, @Param("limit") int limit);

    @Override
    @Query("SELECT e FROM RuleNodeDebugEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.ts >= :startTime) " +
            "AND (:endTime IS NULL OR e.ts <= :endTime)"
    )
    Page<RuleNodeDebugEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                              @Param("entityId") UUID entityId,
                                              @Param("startTime") Long startTime,
                                              @Param("endTime") Long endTime,
                                              Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM rule_node_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                    "AND (:eventEntityId IS NULL OR e.e_entity_id = uuid(:eventEntityId)) " +
                    "AND (:eventEntityType IS NULL OR e.e_entity_type ILIKE concat('%', :eventEntityType, '%')) " +
                    "AND (:msgId IS NULL OR e.e_msg_id = uuid(:msgId)) " +
                    "AND (:msgType IS NULL OR e.e_msg_type ILIKE concat('%', :msgType, '%')) " +
                    "AND (:relationType IS NULL OR e.e_relation_type ILIKE concat('%', :relationType, '%')) " +
                    "AND (:data IS NULL OR e.e_data ILIKE concat('%', :data, '%')) " +
                    "AND (:metadata IS NULL OR e.e_metadata ILIKE concat('%', :metadata, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))"
            ,
            countQuery = "SELECT count(*) FROM rule_node_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                    "AND (:eventEntityId IS NULL OR e.e_entity_id = uuid(:eventEntityId)) " +
                    "AND (:eventEntityType IS NULL OR e.e_entity_type ILIKE concat('%', :eventEntityType, '%')) " +
                    "AND (:msgId IS NULL OR e.e_msg_id = uuid(:msgId)) " +
                    "AND (:msgType IS NULL OR e.e_msg_type ILIKE concat('%', :msgType, '%')) " +
                    "AND (:relationType IS NULL OR e.e_relation_type ILIKE concat('%', :relationType, '%')) " +
                    "AND (:data IS NULL OR e.e_data ILIKE concat('%', :data, '%')) " +
                    "AND (:metadata IS NULL OR e.e_metadata ILIKE concat('%', :metadata, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))"
    )
    Page<RuleNodeDebugEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                              @Param("entityId") UUID entityId,
                                              @Param("startTime") Long startTime,
                                              @Param("endTime") Long endTime,
                                              @Param("serviceId") String server,
                                              @Param("eventType") String type,
                                              @Param("eventEntityId") String eventEntityId,
                                              @Param("eventEntityType") String eventEntityType,
                                              @Param("msgId") String eventMsgId,
                                              @Param("msgType") String eventMsgType,
                                              @Param("relationType") String relationType,
                                              @Param("data") String data,
                                              @Param("metadata") String metadata,
                                              @Param("isError") boolean isError,
                                              @Param("error") String error,
                                              Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM RuleNodeDebugEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.ts >= :startTime) " +
            "AND (:endTime IS NULL OR e.ts <= :endTime)"
    )
    void removeEvents(@Param("tenantId") UUID tenantId,
                      @Param("entityId") UUID entityId,
                      @Param("startTime") Long startTime,
                      @Param("endTime") Long endTime);

    @Transactional
    @Modifying
    @Query(nativeQuery = true,
            value = "DELETE FROM rule_node_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                    "AND (:eventEntityId IS NULL OR e.e_entity_id = uuid(:eventEntityId)) " +
                    "AND (:eventEntityType IS NULL OR e.e_entity_type ILIKE concat('%', :eventEntityType, '%')) " +
                    "AND (:msgId IS NULL OR e.e_msg_id = uuid(:msgId)) " +
                    "AND (:msgType IS NULL OR e.e_msg_type ILIKE concat('%', :msgType, '%')) " +
                    "AND (:relationType IS NULL OR e.e_relation_type ILIKE concat('%', :relationType, '%')) " +
                    "AND (:data IS NULL OR e.e_data ILIKE concat('%', :data, '%')) " +
                    "AND (:metadata IS NULL OR e.e_metadata ILIKE concat('%', :metadata, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))")
    void removeEvents(@Param("tenantId") UUID tenantId,
                      @Param("entityId") UUID entityId,
                      @Param("startTime") Long startTime,
                      @Param("endTime") Long endTime,
                      @Param("serviceId") String server,
                      @Param("eventType") String type,
                      @Param("eventEntityId") String eventEntityId,
                      @Param("eventEntityType") String eventEntityType,
                      @Param("msgId") String eventMsgId,
                      @Param("msgType") String eventMsgType,
                      @Param("relationType") String relationType,
                      @Param("data") String data,
                      @Param("metadata") String metadata,
                      @Param("isError") boolean isError,
                      @Param("error") String error);
}
