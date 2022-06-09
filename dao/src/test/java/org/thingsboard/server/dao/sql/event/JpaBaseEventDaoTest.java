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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.event.EventDao;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.DataConstants.ALARM;
import static org.thingsboard.server.common.data.DataConstants.STATS;

/**
 * Created by Valerii Sosliuk on 5/5/2017.
 */
@Slf4j
public class JpaBaseEventDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private EventDao eventDao;
    UUID tenantId = Uuids.timeBased();

    @After
    public void deleteEvents() {
        List<Event> events = eventDao.find(TenantId.fromUUID(tenantId));
        for (Event event : events) {
            eventDao.removeById(TenantId.fromUUID(tenantId), event.getUuidId());
        }
    }

    @Test
    public void findEvent() {
        UUID entityId = Uuids.timeBased();
        Event savedEvent = eventDao.save(TenantId.fromUUID(tenantId), getEvent(entityId, tenantId, entityId));
        Event foundEvent = eventDao.findEvent(tenantId, new DeviceId(entityId), DataConstants.STATS, savedEvent.getUid());
        assertNotNull("Event expected to be not null", foundEvent);
        assertEquals(savedEvent.getId(), foundEvent.getId());
    }

    @Test
    public void findEventsByEntityIdAndPageLink() throws Exception {
        UUID entityId1 = Uuids.timeBased();
        UUID entityId2 = Uuids.timeBased();
        long startTime = System.currentTimeMillis();
        long endTime = createEventsTwoEntities(tenantId, entityId1, entityId2, 20);

        TimePageLink pageLink1 = new TimePageLink(30);
        PageData<Event> events1 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink1);
        assertEquals(10, events1.getData().size());

        TimePageLink pageLink2 = new TimePageLink(30, 0, "", null, startTime, null);
        PageData<Event> events2 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink2);
        assertEquals(10, events2.getData().size());

        TimePageLink pageLink3 = new TimePageLink(30, 0, "", null, startTime, endTime);
        PageData<Event> events3 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink3);
        assertEquals(10, events3.getData().size());

        TimePageLink pageLink4 = new TimePageLink(5, 0, "", null, startTime, endTime);
        PageData<Event> events4 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink4);
        assertEquals(5, events4.getData().size());

        pageLink4 = pageLink4.nextPageLink();
        PageData<Event> events5 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink4);
        assertEquals(5, events5.getData().size());

        pageLink4 = pageLink4.nextPageLink();
        PageData<Event> events6 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink4);
        assertEquals(0, events6.getData().size());

    }

    @Test
    public void findEventsByEntityIdAndEventTypeAndPageLink() throws Exception {
        UUID entityId1 = Uuids.timeBased();
        UUID entityId2 = Uuids.timeBased();
        long startTime = System.currentTimeMillis();
        long endTime = createEventsTwoEntitiesTwoTypes(tenantId, entityId1, entityId2, 20);

        TimePageLink pageLink1 = new TimePageLink(30);
        PageData<Event> events1 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink1);
        assertEquals(5, events1.getData().size());

        TimePageLink pageLink2 = new TimePageLink(30, 0, "", null, startTime, null);
        PageData<Event> events2 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink2);
        assertEquals(5, events2.getData().size());

        TimePageLink pageLink3 = new TimePageLink(30, 0, "", null, startTime, endTime);
        PageData<Event> events3 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink3);
        assertEquals(5, events3.getData().size());

        TimePageLink pageLink4 = new TimePageLink(4, 0, "", null, startTime, endTime);
        PageData<Event> events4 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink4);
        assertEquals(4, events4.getData().size());

        pageLink4 = pageLink4.nextPageLink();
        PageData<Event> events5 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink4);
        assertEquals(1, events5.getData().size());
    }

    private long createEventsTwoEntitiesTwoTypes(UUID tenantId, UUID entityId1, UUID entityId2, int count) throws Exception {
        for (int i = 0; i < count / 2; i++) {
            String type = i % 2 == 0 ? STATS : ALARM;
            UUID eventId1 = Uuids.timeBased();
            Event event1 = getEvent(eventId1, tenantId, entityId1, type);
            eventDao.saveAsync(event1).get();
            UUID eventId2 = Uuids.timeBased();
            Event event2 = getEvent(eventId2, tenantId, entityId2, type);
            eventDao.saveAsync(event2).get();
        }
        return System.currentTimeMillis();
    }

    private long createEventsTwoEntities(UUID tenantId, UUID entityId1, UUID entityId2, int count) throws Exception {
        for (int i = 0; i < count / 2; i++) {
            UUID eventId1 = Uuids.timeBased();
            Event event1 = getEvent(eventId1, tenantId, entityId1);
            eventDao.saveAsync(event1).get();
            UUID eventId2 = Uuids.timeBased();
            Event event2 = getEvent(eventId2, tenantId, entityId2);
            eventDao.saveAsync(event2).get();
        }
        return System.currentTimeMillis();
    }

    private Event getEvent(UUID eventId, UUID tenantId, UUID entityId, String type) {
        Event event = getEvent(eventId, tenantId, entityId);
        event.setType(type);
        return event;
    }

    private Event getEvent(UUID eventId, UUID tenantId, UUID entityId) {
        Event event = new Event();
        event.setId(new EventId(eventId));
        event.setTenantId(TenantId.fromUUID(tenantId));
        EntityId deviceId = new DeviceId(entityId);
        event.setEntityId(deviceId);
        event.setUid(event.getId().getId().toString());
        event.setType(STATS);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree("{\"key\":\"value\"}");
            event.setBody(jsonNode);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return event;
    }
}
