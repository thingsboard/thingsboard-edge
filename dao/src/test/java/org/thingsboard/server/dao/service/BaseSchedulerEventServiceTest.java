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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by igor on 3/13/18.
 */
public abstract class BaseSchedulerEventServiceTest extends AbstractServiceTest {

    private IdComparator<SchedulerEvent> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindEdgeSchedulerEventsByTenantIdAndName() {
        Edge edge = constructEdge(tenantId, "My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);

        String name1 = "Edge Scheduler Event name 1";
        List<SchedulerEvent> schedulerEventsName1 = new ArrayList<>();
        for (int i = 0; i < 123; i++) {
            SchedulerEvent schedulerEvent = createSchedulerEvent();
            schedulerEvent.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = name1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            schedulerEvent.setName(name);
            schedulerEventsName1.add(schedulerEventService.saveSchedulerEvent(schedulerEvent));
        }
        schedulerEventsName1.forEach(schedulerEvent ->
                schedulerEventService.assignSchedulerEventToEdge(tenantId, schedulerEvent.getId(), savedEdge.getId()));

        String name2 = "Edge Scheduler Event name 2";
        List<SchedulerEvent> schedulerEventsName2 = new ArrayList<>();
        for (int i = 0; i < 193; i++) {
            SchedulerEvent schedulerEvent = createSchedulerEvent();
            schedulerEvent.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = name2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            schedulerEvent.setName(name);
            schedulerEventsName2.add(schedulerEventService.saveSchedulerEvent(schedulerEvent));
        }
        schedulerEventsName2.forEach(schedulerEvent ->
                schedulerEventService.assignSchedulerEventToEdge(tenantId, schedulerEvent.getId(), savedEdge.getId()));

        List<SchedulerEvent> loadedSchedulerEventsName1 = new ArrayList<>();
        PageLink pageLink = new PageLink(19, 0, name1);
        PageData<SchedulerEvent> pageData = null;
        do {
            pageData = schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
            loadedSchedulerEventsName1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(schedulerEventsName1, idComparator);
        Collections.sort(loadedSchedulerEventsName1, idComparator);

        Assert.assertEquals(schedulerEventsName1, loadedSchedulerEventsName1);

        List<SchedulerEvent> loadedSchedulerEventsName2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, name2);
        do {
            pageData = schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
            loadedSchedulerEventsName2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(schedulerEventsName2, idComparator);
        Collections.sort(loadedSchedulerEventsName2, idComparator);

        Assert.assertEquals(schedulerEventsName2, loadedSchedulerEventsName2);

        for (SchedulerEvent schedulerEvent : loadedSchedulerEventsName1) {
            schedulerEventService.deleteSchedulerEvent(tenantId, schedulerEvent.getId());
        }

        pageLink = new PageLink(4, 0, name1);
        pageData = schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);;
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (SchedulerEvent schedulerEvent : loadedSchedulerEventsName2) {
            schedulerEventService.deleteSchedulerEvent(tenantId, schedulerEvent.getId());
        }

        pageLink = new PageLink(4, 0, name2);
        pageData = schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    private SchedulerEvent createSchedulerEvent() {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName("Scheduler Event");
        schedulerEvent.setType("Custom Type");
        ObjectNode schedule = mapper.createObjectNode();
        schedule.put("startTime", System.currentTimeMillis());
        schedule.put("timezone", "UTC");
        schedulerEvent.setSchedule(schedule);
        schedulerEvent.setConfiguration(mapper.createObjectNode());
        return schedulerEvent;
    }
}
