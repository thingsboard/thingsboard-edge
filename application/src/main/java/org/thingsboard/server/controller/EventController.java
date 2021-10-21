/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EventController extends BaseController {

    private static final String NEW_LINE = "\n\n";

    @Autowired
    private EventService eventService;

    @ApiOperation(value = "Get Events by type (getEvents)",
            notes = "Returns a page of events for specified entity by specifying event type. " +
                    PAGE_DATA_PARAMETERS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}/{eventType}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<Event> getEvents(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = "A string value representing event type", example = "STATS", required = true)
            @PathVariable("eventType") String eventType,
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @RequestParam(TENANT_ID) String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EVENT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EVENT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = EVENT_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = EVENT_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));

            EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
            checkEntityId(entityId, Operation.READ);
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            return checkNotNull(eventService.findEvents(tenantId, entityId, eventType, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Events (getEvents)",
            notes = "Returns a page of events for specified entity. " +
                    PAGE_DATA_PARAMETERS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<Event> getEvents(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @RequestParam("tenantId") String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = EVENT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EVENT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = EVENT_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = EVENT_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));

            EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
            checkEntityId(entityId, Operation.READ);

            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

            return checkNotNull(eventService.findEvents(tenantId, entityId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Events by event filter (getEvents)",
            notes = "Returns a page of events for the chosen entity by specifying the event filter. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE + "5 different eventFilter objects could be set for different event types. " +
                    "The eventType field is required. Others are optional. If some of them are set, the filtering will be applied according to them. " +
                    "See the examples below for all the fields used for each event type filtering. " + NEW_LINE +
                    EVENT_ERROR_FILTER_OBJ + NEW_LINE +
                    EVENT_LC_EVENT_FILTER_OBJ + NEW_LINE +
                    EVENT_STATS_FILTER_OBJ + NEW_LINE +
                    EVENT_DEBUG_RULE_NODE_FILTER_OBJ + NEW_LINE +
                    EVENT_DEBUG_RULE_CHAIN_FILTER_OBJ + NEW_LINE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/events/{entityType}/{entityId}", method = RequestMethod.POST)
    @ResponseBody
    public PageData<Event> getEvents(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_TYPE) String strEntityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @RequestParam(TENANT_ID) String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = "A JSON value representing the event filter.", required = true)
            @RequestBody EventFilter eventFilter,
            @ApiParam(value = EVENT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EVENT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = EVENT_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = EVENT_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));

            EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
            checkEntityId(entityId, Operation.READ);

            if (sortProperty != null && sortProperty.equals("createdTime") && eventFilter.hasFilterForJsonBody()) {
                sortProperty = ModelConstants.CREATED_TIME_PROPERTY;
            }

            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            return checkNotNull(eventService.findEventsByFilter(tenantId, entityId, eventFilter, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
