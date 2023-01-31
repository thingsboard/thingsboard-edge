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
package org.thingsboard.server.dao.event;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.UUID;

/**
 * The Interface EventDao.
 */
public interface EventDao {

    /**
     * Save or update event object async
     *
     * @param event the event object
     * @return saved event object future
     */
    ListenableFuture<Void> saveAsync(Event event);

    /**
     * Find events by tenantId, entityId, eventType and pageLink.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param eventType the eventType
     * @param pageLink the pageLink
     * @return the event list
     */
    PageData<? extends Event> findEvents(UUID tenantId, UUID entityId, EventType eventType, TimePageLink pageLink);

    PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, EventFilter eventFilter, TimePageLink pageLink);

    /**
     * Find telemetry events by tenantId, entityId and eventType.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param eventType the eventType
     * @param limit the limit
     * @return the event list
     */
    List<? extends Event> findLatestEvents(UUID tenantId, UUID entityId, EventType eventType, int limit);

    /**
     * Executes stored procedure to cleanup old events. Uses separate ttl for debug and other events.
     * @param regularEventExpTs the expiration time of the regular events
     * @param debugEventExpTs the expiration time of the debug events
     * @param cleanupDb
     */
    void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb);

    /**
     * Removes all events for the specified entity and time interval
     *
     * @param tenantId
     * @param entityId
     * @param startTime
     * @param endTime
     */
    void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime);

    /**
     *
     * Removes all events for the specified entity, event filter and time interval
     *
     * @param tenantId
     * @param entityId
     * @param eventFilter
     * @param startTime
     * @param endTime
     */
    void removeEvents(UUID tenantId, UUID entityId, EventFilter eventFilter, Long startTime, Long endTime);

    void migrateEvents(long regularEventTs, long debugEventTs);
}
