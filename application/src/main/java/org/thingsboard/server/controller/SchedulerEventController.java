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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SCHEDULER_EVENT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.EdgeController.EDGE_ID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class SchedulerEventController extends BaseController {

    private static final String SCHEDULER_EVENT_INFO_DESCRIPTION = "Scheduler Events allows you to schedule various types of events with flexible schedule configuration. " +
            "Scheduler fires configured scheduler events according to their schedule. See the 'Model' tab of the Response Class for more details. ";
    private static final String SCHEDULER_EVENT_WITH_CUSTOMER_INFO_DESCRIPTION = "Scheduler Event With Customer Info extends Scheduler Event Info object and adds " +
            "'customerTitle' - a String value representing the title of the customer which user created a Scheduler Event and " +
            "'customerIsPublic' - a boolean parameter that specifies if customer is public. See the 'Model' tab of the Response Class for more details. ";
    private static final String SCHEDULER_EVENT_DESCRIPTION = "Scheduler Event extends Scheduler Event Info object and adds " +
            "'configuration' - a JSON structure of scheduler event configuration. See the 'Model' tab of the Response Class for more details. ";
    private static final String INVALID_SCHEDULER_EVENT_ID = "Referencing non-existing Scheduler Event Id will cause 'Not Found' error.";

    private static final int DEFAULT_SCHEDULER_EVENT_LIMIT = 100;

    public static final String SCHEDULER_EVENT_ID = "schedulerEventId";

    @ApiOperation(value = "Get Scheduler Event With Customer Info (getSchedulerEventInfoById)",
            notes = "Fetch the SchedulerEventWithCustomerInfo object based on the provided scheduler event Id. " +
                    SCHEDULER_EVENT_WITH_CUSTOMER_INFO_DESCRIPTION + INVALID_SCHEDULER_EVENT_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvent/info/{schedulerEventId}", method = RequestMethod.GET)
    @ResponseBody
    public SchedulerEventWithCustomerInfo getSchedulerEventInfoById(
            @ApiParam(value = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
        return checkSchedulerEventInfoId(schedulerEventId, Operation.READ);
    }

    @ApiOperation(value = "Get Scheduler Event (getSchedulerEventById)",
            notes = "Fetch the SchedulerEvent object based on the provided scheduler event Id. " +
                    SCHEDULER_EVENT_DESCRIPTION + INVALID_SCHEDULER_EVENT_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvent/{schedulerEventId}", method = RequestMethod.GET)
    @ResponseBody
    public SchedulerEvent getSchedulerEventById(
            @ApiParam(value = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
        return checkSchedulerEventId(schedulerEventId, Operation.READ);
    }

    @ApiOperation(value = "Save Scheduler Event (saveSchedulerEvent)",
            notes = "Creates or Updates scheduler event. " + SCHEDULER_EVENT_DESCRIPTION +
                    "When creating scheduler event, platform generates scheduler event Id as " + UUID_WIKI_LINK +
                    "The newly created scheduler event id will be present in the response. Specify existing scheduler event id to update the scheduler event. " +
                    "Referencing non-existing scheduler event Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Scheduler Event entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvent", method = RequestMethod.POST)
    @ResponseBody
    public SchedulerEvent saveSchedulerEvent(
            @ApiParam(value = "A JSON value representing the Scheduler Event.")
            @RequestBody SchedulerEvent schedulerEvent) throws ThingsboardException {
        log.trace("saveSchedulerEvent {}", schedulerEvent);
        try {
            schedulerEvent.setTenantId(getCurrentUser().getTenantId());
            if (Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())) {
                schedulerEvent.setCustomerId(getCurrentUser().getCustomerId());
            }

            checkEntity(schedulerEvent.getId(), schedulerEvent, Resource.SCHEDULER_EVENT, null);

            SchedulerEvent savedSchedulerEvent = checkNotNull(schedulerEventService.saveSchedulerEvent(schedulerEvent));

            notificationEntityService.logEntityAction(getTenantId(), savedSchedulerEvent.getId(), savedSchedulerEvent,
                    savedSchedulerEvent.getCustomerId(),
                    schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED, getCurrentUser());

            if (schedulerEvent.getId() != null) {
                sendEntityNotificationMsg(getTenantId(), savedSchedulerEvent.getId(),
                        EdgeEventActionType.UPDATED);
            }

            if (schedulerEvent.getId() == null) {
                schedulerService.onSchedulerEventAdded(savedSchedulerEvent);
            } else {
                schedulerService.onSchedulerEventUpdated(savedSchedulerEvent);
            }

            return savedSchedulerEvent;
        } catch (Exception e) {
            log.warn("Failed to save or update schedulerEvent " + schedulerEvent, e);
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.SCHEDULER_EVENT), schedulerEvent,
                    schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED, getCurrentUser(), e);

            throw e;
        }
    }

    @ApiOperation(value = "Delete Scheduler Event (deleteSchedulerEvent)",
            notes = "Deletes the scheduler event. " + INVALID_SCHEDULER_EVENT_ID + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvent/{schedulerEventId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteSchedulerEvent(
            @ApiParam(value = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        try {
            SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
            SchedulerEvent schedulerEvent = checkSchedulerEventId(schedulerEventId, Operation.DELETE);
            schedulerEventService.deleteSchedulerEvent(getTenantId(), schedulerEventId);

            notificationEntityService.logEntityAction(getTenantId(), schedulerEventId, schedulerEvent,
                    schedulerEvent.getCustomerId(), ActionType.DELETED, getCurrentUser(), strSchedulerEventId);

            sendEntityNotificationMsg(getTenantId(), schedulerEventId, EdgeEventActionType.DELETED);

            schedulerService.onSchedulerEventDeleted(schedulerEvent);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.SCHEDULER_EVENT),
                    ActionType.DELETED, getCurrentUser(), e, strSchedulerEventId);

            throw e;
        }
    }


    @ApiOperation(value = "Get Scheduler Events By Type (getSchedulerEvents)",
            notes = "Requested scheduler events must be owned by tenant or assigned to customer which user is performing the request. "
                    + SCHEDULER_EVENT_WITH_CUSTOMER_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvents", method = RequestMethod.GET)
    @ResponseBody
    public List<SchedulerEventWithCustomerInfo> getSchedulerEvents(
            @ApiParam(value = "A string value representing the scheduler type. For example, 'generateReport'")
            @RequestParam(required = false) String type) throws ThingsboardException {
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
    }

    @ApiOperation(value = "Get Scheduler Events By Ids (getSchedulerEventsByIds)",
            notes = "Requested scheduler events must be owned by tenant or assigned to customer which user is performing the request. "
                    + SCHEDULER_EVENT_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvents", params = {"schedulerEventIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<SchedulerEventInfo> getSchedulerEventsByIds(
            @ApiParam(value = "A list of scheduler event ids, separated by comma ','", required = true)
            @RequestParam("schedulerEventIds") String[] strSchedulerEventIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("schedulerEventIds", strSchedulerEventIds);
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

    @ApiOperation(value = "Assign scheduler event to edge (assignSchedulerEventToEdge)",
            notes = "Creates assignment of an existing scheduler event to an instance of The Edge. " +
                    "Assignment works in async way - first, notification event pushed to edge service queue on platform. " +
                    "Second, remote edge service will receive a copy of assignment scheduler event " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once scheduler event will be delivered to edge service, it is going to be available for usage on remote edge instance. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_WRITE_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/schedulerEvent/{schedulerEventId}", method = RequestMethod.POST)
    @ResponseBody
    public SchedulerEventInfo assignSchedulerEventToEdge(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION)
                                                         @PathVariable(EDGE_ID) String strEdgeId,
                                                         @ApiParam(value = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION)
                                                         @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.WRITE);

            SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
            checkSchedulerEventId(schedulerEventId, Operation.READ);

            SchedulerEventInfo savedSchedulerEvent = checkNotNull(schedulerEventService.assignSchedulerEventToEdge(getCurrentUser().getTenantId(), schedulerEventId, edgeId));

            notificationEntityService.logEntityAction(getTenantId(), schedulerEventId, savedSchedulerEvent,
                    ActionType.ASSIGNED_TO_EDGE, getCurrentUser(), strSchedulerEventId, savedSchedulerEvent.getName(), strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, schedulerEventId, EdgeEventActionType.ASSIGNED_TO_EDGE);

            return savedSchedulerEvent;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.SCHEDULER_EVENT),
                    ActionType.ASSIGNED_TO_EDGE, getCurrentUser(), e, strSchedulerEventId, strEdgeId);

            throw e;
        }
    }

    @ApiOperation(value = "Unassign scheduler event from edge (unassignSchedulerEventFromEdge)",
            notes = "Clears assignment of the scheduler event to the edge. " +
                    "Unassignment works in async way - first, 'unassign' notification event pushed to edge queue on platform. " +
                    "Second, remote edge service will receive an 'unassign' command to remove entity group " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove entity group and entities inside this group locally." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_WRITE_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/schedulerEvent/{schedulerEventId}", method = RequestMethod.DELETE)
    @ResponseBody
    public SchedulerEventInfo unassignSchedulerEventFromEdge(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION)
                                                             @PathVariable(EDGE_ID) String strEdgeId,
                                                             @ApiParam(value = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION)
                                                             @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.WRITE);
            SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
            SchedulerEventInfo schedulerEvent = checkSchedulerEventId(schedulerEventId, Operation.READ);

            SchedulerEventInfo savedSchedulerEvent = checkNotNull(schedulerEventService.unassignSchedulerEventFromEdge(getCurrentUser().getTenantId(), schedulerEventId, edgeId));

            notificationEntityService.logEntityAction(getTenantId(), schedulerEventId, schedulerEvent, ActionType.UNASSIGNED_FROM_EDGE,
                    getCurrentUser(), strSchedulerEventId, savedSchedulerEvent.getName(), strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, schedulerEventId, EdgeEventActionType.UNASSIGNED_FROM_EDGE);

            return savedSchedulerEvent;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.SCHEDULER_EVENT),
                    ActionType.UNASSIGNED_FROM_EDGE, getCurrentUser(), e, strSchedulerEventId);

            throw e;
        }
    }

    @ApiOperation(value = "Get Edge Scheduler Events (getEdgeSchedulerEvents)",
            notes = "Returns a page of  Scheduler Events Info objects based on the provided Edge entity. " +
                    SCHEDULER_EVENT_DESCRIPTION + SCHEDULER_EVENT_INFO_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/schedulerEvents", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<SchedulerEventInfo> getEdgeSchedulerEvents(
            @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION)
            @PathVariable(EDGE_ID) String strEdgeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = "The case insensitive 'startsWith' filter based on the scheduler event name.")
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink));
    }

    @ApiOperation(value = "Get All Edge Scheduler Events (getAllSchedulerEvents)",
            notes = "Fetch the list of Scheduler Event Info objects based on the provided Edge entity. "
                    + SCHEDULER_EVENT_DESCRIPTION + SCHEDULER_EVENT_INFO_DESCRIPTION +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/allSchedulerEvents", method = RequestMethod.GET)
    @ResponseBody
    public List<SchedulerEventInfo> getAllSchedulerEvents(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION)
                                                          @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        List<SchedulerEventInfo> result = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_SCHEDULER_EVENT_LIMIT);
        PageData<SchedulerEventInfo> pageData;
        do {
            pageData = schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (pageData.getData().size() > 0) {
                result.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData.hasNext());
        return checkNotNull(result);
    }
}
