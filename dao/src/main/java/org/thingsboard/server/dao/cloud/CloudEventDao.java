/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.cloud;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

/**
 * The Interface CloudEventDao.
 */
public interface CloudEventDao extends Dao<CloudEvent> {

    /**
     * Save or update cloud event object
     *
     * @param cloudEvent the event object
     * @return saved cloud event object future
     */
    ListenableFuture<Void> saveAsync(CloudEvent cloudEvent);


    /**
     * Find cloud events by tenantId and pageLink.
     *
     * @param tenantId the tenantId
     * @param pageLink the pageLink
     * @return the event list
     */
    PageData<CloudEvent> findCloudEvents(UUID tenantId, TimePageLink pageLink);

    long countEventsByTenantIdAndEntityIdAndActionAndTypeAndStartTimeAndEndTime(UUID tenantId,
                                                                                UUID entityId,
                                                                                CloudEventType cloudEventType,
                                                                                EdgeEventActionType cloudEventAction,
                                                                                Long startTime,
                                                                                Long endTime);

    /**
     * Executes stored procedure to cleanup old cloud events.
     * @param eventsTtl the ttl for cloud events in seconds
     */
    void cleanupEvents(long eventsTtl);

    void migrateCloudEvents();
}
