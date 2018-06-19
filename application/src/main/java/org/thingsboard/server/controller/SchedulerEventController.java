/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.security.Authority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class SchedulerEventController extends BaseController {

    public static final String SCHEDULER_EVENT_ID = "schedulerEventId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/schedulerEvent/{schedulerEventId}", method = RequestMethod.GET)
    @ResponseBody
    public SchedulerEvent getSchedulerEventById(@PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        try {
            SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
            return checkSchedulerEventId(schedulerEventId);
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
            if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                schedulerEvent.setCustomerId(getCurrentUser().getCustomerId());
            }
            SchedulerEvent savedSchedulerEvent = checkNotNull(schedulerEventService.saveSchedulerEvent(schedulerEvent));

            logEntityAction(savedSchedulerEvent.getId(), savedSchedulerEvent,
                    savedSchedulerEvent.getCustomerId(),
                    schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

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
            SchedulerEvent schedulerEvent = checkSchedulerEventId(schedulerEventId);
            schedulerEventService.deleteSchedulerEvent(schedulerEventId);

            logEntityAction(schedulerEventId, schedulerEvent,
                    schedulerEvent.getCustomerId(),
                    ActionType.DELETED, null, strSchedulerEventId);

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
    public List<SchedulerEventInfo> getSchedulerEvents(
            @RequestParam(required = false) String type) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
                if (type != null && type.trim().length()>0) {
                    return checkNotNull(schedulerEventService.findSchedulerEventsByTenantIdAndType(tenantId, type));
                } else {
                    return checkNotNull(schedulerEventService.findSchedulerEventsByTenantId(tenantId));
                }
            } else { //CUSTOMER_USER
                CustomerId customerId = getCurrentUser().getCustomerId();
                if (type != null && type.trim().length()>0) {
                    return checkNotNull(schedulerEventService.findSchedulerEventsByTenantIdAndCustomerIdAndType(tenantId, customerId, type));
                } else {
                    return checkNotNull(schedulerEventService.findSchedulerEventsByTenantIdAndCustomerId(tenantId, customerId));
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
