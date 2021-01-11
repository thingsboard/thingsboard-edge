/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.scheduler;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.SchedulerEventInfoEntity;
import org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity;

import java.util.List;
import java.util.UUID;

public interface SchedulerEventInfoRepository extends CrudRepository<SchedulerEventInfoEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "WHERE s.id = :schedulerEventId")
    SchedulerEventWithCustomerInfoEntity findSchedulerEventWithCustomerInfoById(@Param("schedulerEventId") UUID schedulerEventId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "WHERE s.tenantId = :tenantId")
    List<SchedulerEventWithCustomerInfoEntity> findSchedulerEventsWithCustomerInfoByTenantId(@Param("tenantId") UUID tenantId);

    List<SchedulerEventInfoEntity> findSchedulerEventInfoEntitiesByTenantId(UUID tenantId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "WHERE s.tenantId = :tenantId " +
            "AND s.type = :type")
    List<SchedulerEventWithCustomerInfoEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                                     @Param("type") String type);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "WHERE s.tenantId = :tenantId " +
            "AND s.customerId = :customerId")
    List<SchedulerEventWithCustomerInfoEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                           @Param("customerId") UUID customerId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "WHERE s.tenantId = :tenantId " +
            "AND s.customerId = :customerId " +
            "AND s.type = :type")
    List<SchedulerEventWithCustomerInfoEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                                                  @Param("customerId") UUID customerId,
                                                                                  @Param("type") String type);

    List<SchedulerEventInfoEntity> findSchedulerEventsByTenantIdAndIdIn(UUID tenantId, List<UUID> schedulerEventIds);

}
