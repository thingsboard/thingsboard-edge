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
package org.thingsboard.server.dao.event;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BaseEventService implements EventService {

    @Value("${event.debug.max-symbols:4096}")
    private int maxDebugEventSymbols;

    @Autowired
    public EventDao eventDao;

    @Autowired
    private DataValidator<Event> eventValidator;

    @Override
    public ListenableFuture<Void> saveAsync(Event event) {
        eventValidator.validate(event, Event::getTenantId);
        checkAndTruncateDebugEvent(event);
        return eventDao.saveAsync(event);
    }

    private void checkAndTruncateDebugEvent(Event event) {
        if (event.getType().startsWith("DEBUG") && event.getBody() != null && event.getBody().has("data")) {
            String dataStr = event.getBody().get("data").asText();
            int length = dataStr.length();
            if (length > maxDebugEventSymbols) {
                ((ObjectNode) event.getBody()).put("data", dataStr.substring(0, maxDebugEventSymbols) + "...[truncated " + (length - maxDebugEventSymbols) + " symbols]");
                log.trace("[{}] Event was truncated: {}", event.getId(), dataStr);
            }
        }
    }

    @Override
    public Optional<Event> findEvent(TenantId tenantId, EntityId entityId, String eventType, String eventUid) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (entityId == null) {
            throw new DataValidationException("Entity id should be specified!.");
        }
        if (StringUtils.isEmpty(eventType)) {
            throw new DataValidationException("Event type should be specified!.");
        }
        if (StringUtils.isEmpty(eventUid)) {
            throw new DataValidationException("Event uid should be specified!.");
        }
        Event event = eventDao.findEvent(tenantId.getId(), entityId, eventType, eventUid);
        return event != null ? Optional.of(event) : Optional.empty();
    }

    @Override
    public PageData<Event> findEvents(TenantId tenantId, EntityId entityId, TimePageLink pageLink) {
        return eventDao.findEvents(tenantId.getId(), entityId, pageLink);
    }

    @Override
    public PageData<Event> findEvents(TenantId tenantId, EntityId entityId, String eventType, TimePageLink pageLink) {
        return eventDao.findEvents(tenantId.getId(), entityId, eventType, pageLink);
    }

    @Override
    public List<Event> findLatestEvents(TenantId tenantId, EntityId entityId, String eventType, int limit) {
        return eventDao.findLatestEvents(tenantId.getId(), entityId, eventType, limit);
    }

    @Override
    public PageData<Event> findEventsByFilter(TenantId tenantId, EntityId entityId, EventFilter eventFilter, TimePageLink pageLink) {
        return eventDao.findEventByFilter(tenantId.getId(), entityId, eventFilter, pageLink);
    }

    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId) {
        removeEvents(tenantId, entityId, null, null, null);
    }

    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId, EventFilter eventFilter, Long startTime, Long endTime) {
        TimePageLink eventsPageLink = new TimePageLink(1000, 0, null, null, startTime, endTime);
        PageData<Event> eventsPageData;
        do {
            if (eventFilter == null) {
                eventsPageData = findEvents(tenantId, entityId, eventsPageLink);
            } else {
                eventsPageData = findEventsByFilter(tenantId, entityId, eventFilter, eventsPageLink);
            }

            eventDao.removeAllByIds(eventsPageData.getData().stream()
                    .map(IdBased::getUuidId)
                    .collect(Collectors.toList()));
        } while (eventsPageData.hasNext());
    }

    @Override
    public void cleanupEvents(long regularEventStartTs, long regularEventEndTs, long debugEventStartTs, long debugEventEndTs) {
        eventDao.cleanupEvents(regularEventStartTs, regularEventEndTs, debugEventStartTs, debugEventEndTs);
    }
}
