/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.EventEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/3/2017.
 */
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    EventEntity findByTenantIdAndEntityTypeAndEntityIdAndEventTypeAndEventUid(UUID tenantId,
                                                                              EntityType entityType,
                                                                              UUID entityId,
                                                                              String eventType,
                                                                              String eventUid);

    EventEntity findByTenantIdAndEntityTypeAndEntityId(UUID tenantId,
                                                       EntityType entityType,
                                                       UUID entityId);

    @Query("SELECT e FROM EventEntity e WHERE e.tenantId = :tenantId AND e.entityType = :entityType " +
            "AND e.entityId = :entityId AND e.eventType = :eventType ORDER BY e.createdTime DESC")
    List<EventEntity> findLatestByTenantIdAndEntityTypeAndEntityIdAndEventType(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") EntityType entityType,
            @Param("entityId") UUID entityId,
            @Param("eventType") String eventType,
            Pageable pageable);

    @Query("SELECT e FROM EventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityType = :entityType AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime) " +
            "AND LOWER(e.eventType) LIKE LOWER(CONCAT('%', :textSearch, '%'))"
    )
    Page<EventEntity> findEventsByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                                      @Param("entityType") EntityType entityType,
                                                      @Param("entityId") UUID entityId,
                                                      @Param("textSearch") String textSearch,
                                                      @Param("startTime") Long startTime,
                                                      @Param("endTime") Long endTime,
                                                      Pageable pageable);

    @Query("SELECT e FROM EventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityType = :entityType AND e.entityId = :entityId " +
            "AND e.eventType = :eventType " +
            "AND (:startTime IS NULL OR e.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime)"
    )
    Page<EventEntity> findEventsByTenantIdAndEntityIdAndEventType(@Param("tenantId") UUID tenantId,
                                                                  @Param("entityType") EntityType entityType,
                                                                  @Param("entityId") UUID entityId,
                                                                  @Param("eventType") String eventType,
                                                                  @Param("startTime") Long startTime,
                                                                  @Param("endTime") Long endTime,
                                                                  Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = :eventType " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:type IS NULL OR lower(json_body->>'type') LIKE concat('%', lower(:type\\:\\:varchar), '%')) " +
                    "AND (:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:entityName IS NULL OR lower(json_body->>'entityName') LIKE concat('%', lower(:entityName\\:\\:varchar), '%')) " +
                    "AND (:relationType IS NULL OR lower(json_body->>'relationType') LIKE concat('%', lower(:relationType\\:\\:varchar), '%')) " +
                    "AND (:bodyEntityId IS NULL OR lower(json_body->>'entityId') LIKE concat('%', lower(:bodyEntityId\\:\\:varchar), '%')) " +
                    "AND (:msgType IS NULL OR lower(json_body->>'msgType') LIKE concat('%', lower(:msgType\\:\\:varchar), '%')) " +
                    "AND ((:isError = FALSE) OR (json_body->>'error') IS NOT NULL) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%')) " +
                    "AND (:data IS NULL OR lower(json_body->>'data') LIKE concat('%', lower(:data\\:\\:varchar), '%')) " +
                    "AND (:metadata IS NULL OR lower(json_body->>'metadata') LIKE concat('%', lower(:metadata\\:\\:varchar), '%')) ",
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = :eventType " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:type IS NULL OR lower(json_body->>'type') LIKE concat('%', lower(:type\\:\\:varchar), '%')) " +
                    "AND (:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:entityName IS NULL OR lower(json_body->>'entityName') LIKE concat('%', lower(:entityName\\:\\:varchar), '%')) " +
                    "AND (:relationType IS NULL OR lower(json_body->>'relationType') LIKE concat('%', lower(:relationType\\:\\:varchar), '%')) " +
                    "AND (:bodyEntityId IS NULL OR lower(json_body->>'entityId') LIKE concat('%', lower(:bodyEntityId\\:\\:varchar), '%')) " +
                    "AND (:msgType IS NULL OR lower(json_body->>'msgType') LIKE concat('%', lower(:msgType\\:\\:varchar), '%')) " +
                    "AND ((:isError = FALSE) OR (json_body->>'error') IS NOT NULL) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%')) " +
                    "AND (:data IS NULL OR lower(json_body->>'data') LIKE concat('%', lower(:data\\:\\:varchar), '%')) " +
                    "AND (:metadata IS NULL OR lower(json_body->>'metadata') LIKE concat('%', lower(:metadata\\:\\:varchar), '%'))"
    )
    Page<EventEntity> findDebugRuleNodeEvents(@Param("tenantId") UUID tenantId,
                                              @Param("entityId") UUID entityId,
                                              @Param("entityType") String entityType,
                                              @Param("eventType") String eventType,
                                              @Param("startTime") Long startTime,
                                              @Param("endTime") Long endTime,
                                              @Param("type") String type,
                                              @Param("server") String server,
                                              @Param("entityName") String entityName,
                                              @Param("relationType") String relationType,
                                              @Param("bodyEntityId") String bodyEntityId,
                                              @Param("msgType") String msgType,
                                              @Param("isError") boolean isError,
                                              @Param("error") String error,
                                              @Param("data") String data,
                                              @Param("metadata") String metadata,
                                              Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'DEBUG_INTEGRATION' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:type IS NULL OR lower(json_body->>'type') LIKE concat('%', lower(:type\\:\\:varchar), '%')) " +
                    "AND (:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:message IS NULL OR lower(json_body->>'message') LIKE concat('%', lower(:message\\:\\:varchar), '%')) " +
                    "AND (:status IS NULL OR lower(json_body->>'status') LIKE concat('%', lower(:status\\:\\:varchar), '%')) " +
                    "AND ((:isError = FALSE) OR (json_body->>'error') IS NOT NULL) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))",
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'DEBUG_INTEGRATION' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:type IS NULL OR lower(json_body->>'type') LIKE concat('%', lower(:type\\:\\:varchar), '%')) " +
                    "AND (:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:message IS NULL OR lower(json_body->>'message') LIKE concat('%', lower(:message\\:\\:varchar), '%')) " +
                    "AND (:status IS NULL OR lower(json_body->>'status') LIKE concat('%', lower(:status\\:\\:varchar), '%')) " +
                    "AND ((:isError = FALSE) OR (json_body->>'error') IS NOT NULL) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))"
    )
    Page<EventEntity> findDebugIntegrationEvents(@Param("tenantId") UUID tenantId,
                                                 @Param("entityId") UUID entityId,
                                                 @Param("entityType") String entityType,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime,
                                                 @Param("type") String type,
                                                 @Param("server") String server,
                                                 @Param("message") String message,
                                                 @Param("status") String status,
                                                 @Param("isError") boolean isError,
                                                 @Param("error") String error,
                                                 Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'DEBUG_CONVERTER' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:type IS NULL OR lower(json_body->>'type') LIKE concat('%', lower(:type\\:\\:varchar), '%')) " +
                    "AND (:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:inParam IS NULL OR lower(json_body->>'in') LIKE concat('%', lower(:inParam\\:\\:varchar), '%')) " +
                    "AND (:outParam IS NULL OR lower(json_body->>'out') LIKE concat('%', lower(:outParam\\:\\:varchar), '%')) " +
                    "AND (:metadata IS NULL OR lower(json_body->>'metadata') LIKE concat('%', lower(:metadata\\:\\:varchar), '%')) " +
                    "AND ((:isError = FALSE) OR (json_body->>'error') IS NOT NULL) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))",
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'DEBUG_CONVERTER' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:type IS NULL OR lower(json_body->>'type') LIKE concat('%', lower(:type\\:\\:varchar), '%')) " +
                    "AND (:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:inParam IS NULL OR lower(json_body->>'in') LIKE concat('%', lower(:inParam\\:\\:varchar), '%')) " +
                    "AND (:outParam IS NULL OR lower(json_body->>'out') LIKE concat('%', lower(:outParam\\:\\:varchar), '%')) " +
                    "AND (:metadata IS NULL OR lower(json_body->>'metadata') LIKE concat('%', lower(:metadata\\:\\:varchar), '%')) " +
                    "AND ((:isError = FALSE) OR (json_body->>'error') IS NOT NULL) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))"
    )
    Page<EventEntity> findDebugConverterEvents(@Param("tenantId") UUID tenantId,
                                                 @Param("entityId") UUID entityId,
                                                 @Param("entityType") String entityType,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime,
                                                 @Param("type") String type,
                                                 @Param("server") String server,
                                                 @Param("inParam") String in,
                                                 @Param("outParam") String out,
                                                 @Param("metadata") String metadata,
                                                 @Param("isError") boolean isError,
                                                 @Param("error") String error,
                                                 Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'ERROR' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:method IS NULL OR lower(json_body->>'method') LIKE concat('%', lower(:method\\:\\:varchar), '%')) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))",
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'ERROR' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:method IS NULL OR lower(json_body->>'method') LIKE concat('%', lower(:method\\:\\:varchar), '%')) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))")
    Page<EventEntity> findErrorEvents(@Param("tenantId") UUID tenantId,
                                      @Param("entityId") UUID entityId,
                                      @Param("entityType") String entityType,
                                      @Param("startTime") Long startTime,
                                      @Param("endTime") Long endTIme,
                                      @Param("server") String server,
                                      @Param("method") String method,
                                      @Param("error") String error,
                                      Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'LC_EVENT' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:event IS NULL OR lower(json_body->>'event') LIKE concat('%', lower(:event\\:\\:varchar), '%')) " +
                    "AND ((:statusFilterEnabled = FALSE) OR lower(json_body->>'success')\\:\\:boolean = :statusFilter) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))"
            ,
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'LC_EVENT' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:event IS NULL OR lower(json_body->>'event') LIKE concat('%', lower(:event\\:\\:varchar), '%')) " +
                    "AND ((:statusFilterEnabled = FALSE) OR lower(json_body->>'success')\\:\\:boolean = :statusFilter) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))"
    )
    Page<EventEntity> findLifeCycleEvents(@Param("tenantId") UUID tenantId,
                                          @Param("entityId") UUID entityId,
                                          @Param("entityType") String entityType,
                                          @Param("startTime") Long startTime,
                                          @Param("endTime") Long endTIme,
                                          @Param("server") String server,
                                          @Param("event") String event,
                                          @Param("statusFilterEnabled") boolean statusFilterEnabled,
                                          @Param("statusFilter") boolean statusFilter,
                                          @Param("error") String error,
                                          Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'STATS' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(e.body\\:\\:json->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:messagesProcessed = 0 OR (json_body->>'messagesProcessed')\\:\\:integer >= :messagesProcessed) " +
                    "AND (:errorsOccurred = 0 OR (json_body->>'errorsOccurred')\\:\\:integer >= :errorsOccurred) ",
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'LC_EVENT' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(e.body\\:\\:json->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:messagesProcessed = 0 OR (json_body->>'messagesProcessed')\\:\\:integer >= :messagesProcessed) " +
                    "AND (:errorsOccurred = 0 OR (json_body->>'errorsOccurred')\\:\\:integer >= :errorsOccurred) ")
    Page<EventEntity> findStatisticsEvents(@Param("tenantId") UUID tenantId,
                                           @Param("entityId") UUID entityId,
                                           @Param("entityType") String entityType,
                                           @Param("startTime") Long startTime,
                                           @Param("endTime") Long endTIme,
                                           @Param("server") String server,
                                           @Param("messagesProcessed") Integer messagesProcessed,
                                           @Param("errorsOccurred") Integer errorsOccurred,
                                           Pageable pageable);


}
