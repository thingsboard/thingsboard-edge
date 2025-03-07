/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.queue;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.edqs.fields.QueueStatsFields;
import org.thingsboard.server.dao.model.sql.QueueStatsEntity;

import java.util.List;
import java.util.UUID;

public interface QueueStatsRepository extends JpaRepository<QueueStatsEntity, UUID> {

    QueueStatsEntity findByTenantIdAndQueueNameAndServiceId(UUID tenantId, String queueName, String serviceId);

    @Query("SELECT q FROM QueueStatsEntity q WHERE q.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(q.queueName, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(q.serviceId, CONCAT('%', :textSearch, '%')) = true)")
    Page<QueueStatsEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM QueueStatsEntity t WHERE t.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    List<QueueStatsEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> queueStatsIds);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.QueueStatsFields(q.id, q.createdTime," +
            "q.tenantId, q.queueName, q.serviceId) FROM QueueStatsEntity q WHERE q.id > :id ORDER BY q.id")
    List<QueueStatsFields> findNextBatch(@Param("id") UUID id, Limit limit);

}