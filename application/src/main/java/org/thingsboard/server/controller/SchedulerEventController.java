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
package org.thingsboard.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class SchedulerEventController extends BaseController {

    public static final String SCHEDULER_EVENT_ID = "schedulerEventId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvent/info/{schedulerEventId}", method = RequestMethod.GET)
    @ResponseBody
    public SchedulerEventWithCustomerInfo getSchedulerEventInfoById(@PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        try {
            SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
            return checkSchedulerEventInfoId(schedulerEventId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvent/{schedulerEventId}", method = RequestMethod.GET)
    @ResponseBody
    public SchedulerEvent getSchedulerEventById(@PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        try {
            SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
            return checkSchedulerEventId(schedulerEventId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvent", method = RequestMethod.POST)
    @ResponseBody
    public SchedulerEvent saveSchedulerEvent(@RequestBody SchedulerEvent schedulerEvent) throws ThingsboardException {
        try {
            schedulerEvent.setTenantId(getCurrentUser().getTenantId());
            if (Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())) {
                schedulerEvent.setCustomerId(getCurrentUser().getCustomerId());
            }

            checkEntity(schedulerEvent.getId(), schedulerEvent, Resource.SCHEDULER_EVENT, null);

            SchedulerEvent savedSchedulerEvent = checkNotNull(schedulerEventService.saveSchedulerEvent(schedulerEvent));

            logEntityAction(savedSchedulerEvent.getId(), savedSchedulerEvent,
                    savedSchedulerEvent.getCustomerId(),
                    schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            if (schedulerEvent.getId() == null) {
                schedulerService.onSchedulerEventAdded(savedSchedulerEvent);
            } else {
                schedulerService.onSchedulerEventUpdated(savedSchedulerEvent);
            }

            return savedSchedulerEvent;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.SCHEDULER_EVENT), schedulerEvent,
                    null, schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvent/{schedulerEventId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteSchedulerEvent(@PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        try {
            SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
            SchedulerEvent schedulerEvent = checkSchedulerEventId(schedulerEventId, Operation.DELETE);
            schedulerEventService.deleteSchedulerEvent(getTenantId(), schedulerEventId);

            logEntityAction(schedulerEventId, schedulerEvent,
                    schedulerEvent.getCustomerId(),
                    ActionType.DELETED, null, strSchedulerEventId);

            schedulerService.onSchedulerEventDeleted(schedulerEvent);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.SCHEDULER_EVENT),
                    null,
                    null,
                    ActionType.DELETED, e, strSchedulerEventId);

            throw handleException(e);
        }
    }


    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvents", method = RequestMethod.GET)
    @ResponseBody
    public List<SchedulerEventWithCustomerInfo> getSchedulerEvents(
            @RequestParam(required = false) String type) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                if (type != null && type.trim().length() > 0) {
                    return checkNotNull(schedulerEventService.findSchedulerEventsByTenantIdAndType(tenantId, type));
                } else {
                    return checkNotNull(schedulerEventService.findSchedulerEventsWithCustomerInfoByTenantId(tenantId));
                }
            } else { //CUSTOMER_USER
                CustomerId customerId = getCurrentUser().getCustomerId();
                if (type != null && type.trim().length() > 0) {
                    return checkNotNull(schedulerEventService.findSchedulerEventsByTenantIdAndCustomerIdAndType(tenantId, customerId, type));
                } else {
                    return checkNotNull(schedulerEventService.findSchedulerEventsByTenantIdAndCustomerId(tenantId, customerId));
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvents", params = {"schedulerEventIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<SchedulerEventInfo> getSchedulerEventsByIds(
            @RequestParam("schedulerEventIds") String[] strSchedulerEventIds) throws ThingsboardException {
        checkArrayParameter("schedulerEventIds", strSchedulerEventIds);
        try {
            if (!accessControlService.hasPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.READ)) {
                return Collections.emptyList();
            }
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<SchedulerEventId> schedulerEventIds = new ArrayList<>();
            for (String strSchedulerEventId : strSchedulerEventIds) {
                schedulerEventIds.add(new SchedulerEventId(toUUID(strSchedulerEventId)));
            }
            List<SchedulerEventInfo> schedulerEvents = checkNotNull(schedulerEventService.findSchedulerEventInfoByIdsAsync(tenantId, schedulerEventIds).get());
            return filterSchedulerEventsByReadPermission(schedulerEvents);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<SchedulerEventInfo> filterSchedulerEventsByReadPermission(List<SchedulerEventInfo> schedulerEvents) {
        return schedulerEvents.stream().filter(schedulerEvent -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.READ, schedulerEvent.getId(), schedulerEvent);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }
}
