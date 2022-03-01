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
package org.thingsboard.server.dao.service.event;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public abstract class BaseEventServiceTest extends AbstractServiceTest {

    @Test
    public void saveEvent() throws Exception {
        DeviceId devId = new DeviceId(Uuids.timeBased());
        Event event = generateEvent(null, devId, "ALARM", Uuids.timeBased().toString());
        eventService.saveAsync(event).get();
        Optional<Event> loaded = eventService.findEvent(event.getTenantId(), event.getEntityId(), event.getType(), event.getUid());
        Assert.assertTrue(loaded.isPresent());
        Assert.assertNotNull(loaded.get());
        Assert.assertEquals(event.getEntityId(), loaded.get().getEntityId());
        Assert.assertEquals(event.getType(), loaded.get().getType());
        Assert.assertEquals(event.getBody(), loaded.get().getBody());
    }

    @Test
    public void findEventsByTypeAndTimeAscOrder() throws Exception {
        long timeBeforeStartTime = LocalDateTime.of(2016, Month.NOVEMBER, 1, 11, 30).toEpochSecond(ZoneOffset.UTC);
        long startTime = LocalDateTime.of(2016, Month.NOVEMBER, 1, 12, 0).toEpochSecond(ZoneOffset.UTC);
        long eventTime = LocalDateTime.of(2016, Month.NOVEMBER, 1, 12, 30).toEpochSecond(ZoneOffset.UTC);
        long endTime = LocalDateTime.of(2016, Month.NOVEMBER, 1, 13, 0).toEpochSecond(ZoneOffset.UTC);
        long timeAfterEndTime = LocalDateTime.of(2016, Month.NOVEMBER, 1, 13, 30).toEpochSecond(ZoneOffset.UTC);

        CustomerId customerId = new CustomerId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
        saveEventWithProvidedTime(timeBeforeStartTime, customerId, tenantId);
        Event savedEvent = saveEventWithProvidedTime(eventTime, customerId, tenantId);
        Event savedEvent2 = saveEventWithProvidedTime(eventTime+1, customerId, tenantId);
        Event savedEvent3 = saveEventWithProvidedTime(eventTime+2, customerId, tenantId);
        saveEventWithProvidedTime(timeAfterEndTime, customerId, tenantId);

        TimePageLink timePageLink = new TimePageLink(2, 0, "", new SortOrder("createdTime"), startTime, endTime);

        PageData<Event> events = eventService.findEvents(tenantId, customerId, DataConstants.STATS,
                timePageLink);

        Assert.assertNotNull(events.getData());
        Assert.assertTrue(events.getData().size() == 2);
        Assert.assertTrue(events.getData().get(0).getUuidId().equals(savedEvent.getUuidId()));
        Assert.assertTrue(events.getData().get(1).getUuidId().equals(savedEvent2.getUuidId()));
        Assert.assertTrue(events.hasNext());

        events = eventService.findEvents(tenantId, customerId, DataConstants.STATS, timePageLink.nextPageLink());

        Assert.assertNotNull(events.getData());
        Assert.assertTrue(events.getData().size() == 1);
        Assert.assertTrue(events.getData().get(0).getUuidId().equals(savedEvent3.getUuidId()));
        Assert.assertFalse(events.hasNext());
    }

    @Test
    public void findEventsByTypeAndTimeDescOrder() throws Exception {
        long timeBeforeStartTime = LocalDateTime.of(2017, Month.NOVEMBER, 1, 11, 30).toEpochSecond(ZoneOffset.UTC);
        long startTime = LocalDateTime.of(2017, Month.NOVEMBER, 1, 12, 0).toEpochSecond(ZoneOffset.UTC);
        long eventTime = LocalDateTime.of(2017, Month.NOVEMBER, 1, 12, 30).toEpochSecond(ZoneOffset.UTC);
        long endTime = LocalDateTime.of(2017, Month.NOVEMBER, 1, 13, 0).toEpochSecond(ZoneOffset.UTC);
        long timeAfterEndTime = LocalDateTime.of(2017, Month.NOVEMBER, 1, 13, 30).toEpochSecond(ZoneOffset.UTC);

        CustomerId customerId = new CustomerId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
        saveEventWithProvidedTime(timeBeforeStartTime, customerId, tenantId);
        Event savedEvent = saveEventWithProvidedTime(eventTime, customerId, tenantId);
        Event savedEvent2 = saveEventWithProvidedTime(eventTime+1, customerId, tenantId);
        Event savedEvent3 = saveEventWithProvidedTime(eventTime+2, customerId, tenantId);
        saveEventWithProvidedTime(timeAfterEndTime, customerId, tenantId);

        TimePageLink timePageLink = new TimePageLink(2, 0, "", new SortOrder("createdTime", SortOrder.Direction.DESC), startTime, endTime);

        PageData<Event> events = eventService.findEvents(tenantId, customerId, DataConstants.STATS,
                timePageLink);

        Assert.assertNotNull(events.getData());
        Assert.assertTrue(events.getData().size() == 2);
        Assert.assertTrue(events.getData().get(0).getUuidId().equals(savedEvent3.getUuidId()));
        Assert.assertTrue(events.getData().get(1).getUuidId().equals(savedEvent2.getUuidId()));
        Assert.assertTrue(events.hasNext());

        events = eventService.findEvents(tenantId, customerId, DataConstants.STATS, timePageLink.nextPageLink());

        Assert.assertNotNull(events.getData());
        Assert.assertTrue(events.getData().size() == 1);
        Assert.assertTrue(events.getData().get(0).getUuidId().equals(savedEvent.getUuidId()));
        Assert.assertFalse(events.hasNext());
    }

    private Event saveEventWithProvidedTime(long time, EntityId entityId, TenantId tenantId) throws Exception {
        Event event = generateEvent(tenantId, entityId, DataConstants.STATS, null);
        event.setId(new EventId(Uuids.startOf(time)));
        eventService.saveAsync(event).get();
        return event;
    }
}
