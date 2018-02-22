/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.exception.ThingsboardException;

@RestController
@RequestMapping("/api")
public class DashboardController extends BaseController {

    public static final String DASHBOARD_ID = "dashboardId";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/serverTime", method = RequestMethod.GET)
    @ResponseBody
    public long getServerTime() throws ThingsboardException {
        return System.currentTimeMillis();
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/info/{dashboardId}", method = RequestMethod.GET)
    @ResponseBody
    public DashboardInfo getDashboardInfoById(@PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            return checkDashboardInfoId(dashboardId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.GET)
    @ResponseBody
    public Dashboard getDashboardById(@PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            return checkDashboardId(dashboardId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard", method = RequestMethod.POST)
    @ResponseBody 
    public Dashboard saveDashboard(@RequestBody Dashboard dashboard) throws ThingsboardException {
        try {
            dashboard.setTenantId(getCurrentUser().getTenantId());
            Dashboard savedDashboard = checkNotNull(dashboardService.saveDashboard(dashboard));

            logEntityAction(savedDashboard.getId(), savedDashboard,
                    savedDashboard.getCustomerId(),
                    dashboard.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedDashboard;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DASHBOARD), dashboard,
                    null, dashboard.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDashboard(@PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            Dashboard dashboard = checkDashboardId(dashboardId);
            dashboardService.deleteDashboard(dashboardId);

            logEntityAction(dashboardId, dashboard,
                    dashboard.getCustomerId(),
                    ActionType.DELETED, null, strDashboardId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.DASHBOARD),
                    null,
                    null,
                    ActionType.DELETED, e, strDashboardId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody 
    public Dashboard assignDashboardToCustomer(@PathVariable("customerId") String strCustomerId,
                                         @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId);

            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            checkDashboardId(dashboardId);
            
            Dashboard savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(dashboardId, customerId));

            logEntityAction(dashboardId, savedDashboard,
                    savedDashboard.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDashboardId, strCustomerId, customer.getName());


            return savedDashboard;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.DASHBOARD), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDashboardId, strCustomerId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseBody 
    public Dashboard unassignDashboardFromCustomer(@PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            Dashboard dashboard = checkDashboardId(dashboardId);
            if (dashboard.getCustomerId() == null || dashboard.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Dashboard isn't assigned to any customer!");
            }

            Customer customer = checkCustomerId(dashboard.getCustomerId());

            Dashboard savedDashboard = checkNotNull(dashboardService.unassignDashboardFromCustomer(dashboardId));

            logEntityAction(dashboardId, dashboard,
                    dashboard.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strDashboardId, customer.getId().toString(), customer.getName());

            return savedDashboard;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.DASHBOARD), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strDashboardId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard assignDashboardToPublicCustomer(@PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            Dashboard dashboard = checkDashboardId(dashboardId);
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(dashboard.getTenantId());
            Dashboard savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(dashboardId, publicCustomer.getId()));

            logEntityAction(dashboardId, savedDashboard,
                    savedDashboard.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDashboardId, publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDashboard;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.DASHBOARD), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDashboardId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/dashboards", params = { "limit" }, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<DashboardInfo> getTenantDashboards(
            @PathVariable("tenantId") String strTenantId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            checkTenantId(tenantId);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboards", params = { "limit" }, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<DashboardInfo> getTenantDashboards(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/dashboards", params = { "limit" }, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<DashboardInfo> getCustomerDashboards(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
