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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.ConverterDebugEvent;
import org.thingsboard.server.common.data.event.ErrorEvent;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.RawDataEvent;
import org.thingsboard.server.common.data.event.RuleChainDebugEvent;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BaseEventService implements EventService {

    @Value("${sql.ttl.events.events_ttl:0}")
    private long ttlInSec;
    @Value("${sql.ttl.events.debug_events_ttl:604800}")
    private long debugTtlInSec;

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
        switch (event.getType()) {
            case DEBUG_RULE_NODE:
                RuleNodeDebugEvent rnEvent = (RuleNodeDebugEvent) event;
                truncateField(rnEvent, RuleNodeDebugEvent::getData, RuleNodeDebugEvent::setData);
                truncateField(rnEvent, RuleNodeDebugEvent::getMetadata, RuleNodeDebugEvent::setMetadata);
                truncateField(rnEvent, RuleNodeDebugEvent::getError, RuleNodeDebugEvent::setError);
                break;
            case DEBUG_RULE_CHAIN:
                RuleChainDebugEvent rcEvent = (RuleChainDebugEvent) event;
                truncateField(rcEvent, RuleChainDebugEvent::getMessage, RuleChainDebugEvent::setMessage);
                truncateField(rcEvent, RuleChainDebugEvent::getError, RuleChainDebugEvent::setError);
                break;
            case LC_EVENT:
                LifecycleEvent lcEvent = (LifecycleEvent) event;
                truncateField(lcEvent, LifecycleEvent::getError, LifecycleEvent::setError);
                break;
            case ERROR:
                ErrorEvent eEvent = (ErrorEvent) event;
                truncateField(eEvent, ErrorEvent::getError, ErrorEvent::setError);
                break;
            case RAW_DATA:
                RawDataEvent rawDataEvent = (RawDataEvent) event;
                truncateField(rawDataEvent, RawDataEvent::getUuid, RawDataEvent::setUuid);
                truncateField(rawDataEvent, RawDataEvent::getMessage, RawDataEvent::setMessage);
                truncateField(rawDataEvent, RawDataEvent::getMessageType, RawDataEvent::setMessageType);
                break;
            case DEBUG_CONVERTER:
                ConverterDebugEvent cEvent = (ConverterDebugEvent) event;
                truncateField(cEvent, ConverterDebugEvent::getInMsg, ConverterDebugEvent::setInMsg);
                truncateField(cEvent, ConverterDebugEvent::getOutMsg, ConverterDebugEvent::setOutMsg);
                truncateField(cEvent, ConverterDebugEvent::getMetadata, ConverterDebugEvent::setMetadata);
                truncateField(cEvent, ConverterDebugEvent::getError, ConverterDebugEvent::setError);
                break;
            case DEBUG_INTEGRATION:
                IntegrationDebugEvent iEvent = (IntegrationDebugEvent) event;
                truncateField(iEvent, IntegrationDebugEvent::getMessage, IntegrationDebugEvent::setMessage);
                truncateField(iEvent, IntegrationDebugEvent::getStatus, IntegrationDebugEvent::setStatus);
                truncateField(iEvent, IntegrationDebugEvent::getError, IntegrationDebugEvent::setError);
                break;
        }
    }

    private <T extends Event> void truncateField(T event, Function<T, String> getter, BiConsumer<T, String> setter) {
        var str = getter.apply(event);
        if (StringUtils.isNotEmpty(str)) {
            var length = str.length();
            if (length > maxDebugEventSymbols) {
                setter.accept(event, str.substring(0, maxDebugEventSymbols) + "...[truncated " + (length - maxDebugEventSymbols) + " symbols]");
            }
        }
    }

    @Override
    public PageData<EventInfo> findEvents(TenantId tenantId, EntityId entityId, EventType eventType, TimePageLink pageLink) {
        return convert(entityId.getEntityType(), eventDao.findEvents(tenantId.getId(), entityId.getId(), eventType, pageLink));
    }

    @Override
    public List<EventInfo> findLatestEvents(TenantId tenantId, EntityId entityId, EventType eventType, int limit) {
        return convert(entityId.getEntityType(), eventDao.findLatestEvents(tenantId.getId(), entityId.getId(), eventType, limit));
    }

    @Override
    public PageData<EventInfo> findEventsByFilter(TenantId tenantId, EntityId entityId, EventFilter eventFilter, TimePageLink pageLink) {
        return convert(entityId.getEntityType(), eventDao.findEventByFilter(tenantId.getId(), entityId.getId(), eventFilter, pageLink));
    }

    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId) {
        removeEvents(tenantId, entityId, null, null, null);
    }

    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId, EventFilter eventFilter, Long startTime, Long endTime) {
        if (eventFilter == null) {
            eventDao.removeEvents(tenantId.getId(), entityId.getId(), startTime, endTime);
        } else {
            eventDao.removeEvents(tenantId.getId(), entityId.getId(), eventFilter, startTime, endTime);
        }
    }

    @Override
    public void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb) {
        eventDao.cleanupEvents(regularEventExpTs, debugEventExpTs, cleanupDb);
    }

    @Override
    public void migrateEvents() {
        eventDao.migrateEvents(ttlInSec > 0 ? (System.currentTimeMillis() - ttlInSec * 1000) : 0, debugTtlInSec > 0 ? (System.currentTimeMillis() - debugTtlInSec * 1000) : 0);
    }

    private PageData<EventInfo> convert(EntityType entityType, PageData<? extends Event> pd) {
        return new PageData<>(pd.getData() == null ? null :
                pd.getData().stream().map(e -> e.toInfo(entityType)).collect(Collectors.toList())
                , pd.getTotalPages(), pd.getTotalElements(), pd.hasNext());
    }

    private List<EventInfo> convert(EntityType entityType, List<? extends Event> list) {
        return list == null ? null : list.stream().map(e -> e.toInfo(entityType)).collect(Collectors.toList());
    }

}
