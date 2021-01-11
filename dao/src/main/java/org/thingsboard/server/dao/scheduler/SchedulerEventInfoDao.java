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
package org.thingsboard.server.dao.scheduler;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface SchedulerEventInfoDao.
 *
 */
public interface SchedulerEventInfoDao extends Dao<SchedulerEventInfo> {

    SchedulerEventWithCustomerInfo findSchedulerEventWithCustomerInfoById(UUID tenantId, UUID schedulerEventId);

    List<SchedulerEventInfo> findSchedulerEventsByTenantId(UUID tenantId);

    /**
     * Find scheduler events by tenantId.
     *
     * @param tenantId the tenantId
     * @return the list of scheduler event objects
     */
    List<SchedulerEventWithCustomerInfo> findSchedulerEventsWithCustomerInfoByTenantId(UUID tenantId);

    /**
     * Find scheduler events by tenantId and type.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @return the list of scheduler event objects
     */
    List<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndType(UUID tenantId, String type);

    /**
     * Find scheduler events by tenantId and customerId.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @return the list of scheduler event objects
     */
    List<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    /**
     * Find scheduler events by tenantId, customerId and type.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @return the list of scheduler event objects
     */
    List<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type);

    /**
     * Find scheduler events by tenantId and scheduler event Ids.
     *
     * @param tenantId the tenantId
     * @param schedulerEventIds the scheduler event Ids
     * @return the list of role objects
     */
    ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> schedulerEventIds);

}
