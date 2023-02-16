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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.thingsboard.server.common.data.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HomeDashboard;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.dashboard.TbDashboardService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.USER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.ControllerConstants.WL_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.WL_WRITE_CHECK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class DashboardController extends BaseController {

    private final TbDashboardService tbDashboardService;
    public static final String DASHBOARD_ID = "dashboardId";
    private static final String HOME_DASHBOARD_ID = "homeDashboardId";
    private static final String HOME_DASHBOARD_HIDE_TOOLBAR = "homeDashboardHideToolbar";
    public static final String DASHBOARD_INFO_DEFINITION = "The Dashboard Info object contains lightweight information about the dashboard (e.g. title, image, assigned customers) but does not contain the heavyweight configuration JSON.";
    public static final String DASHBOARD_DEFINITION = "The Dashboard object is a heavyweight object that contains information about the dashboard (e.g. title, image, assigned customers) and also configuration JSON (e.g. layouts, widgets, entity aliases).";
    public static final String HIDDEN_FOR_MOBILE = "Exclude dashboards that are hidden for mobile";

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Value("${ui.dashboard.max_datapoints_limit}")
    private long maxDatapointsLimit;

    @ApiOperation(value = "Get server time (getServerTime)",
            notes = "Get the server time (milliseconds since January 1, 1970 UTC). " +
                    "Used to adjust view of the dashboards according to the difference between browser and server time.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/serverTime", method = RequestMethod.GET)
    @ResponseBody
    @ApiResponse(code = 200, message = "OK", examples = @Example(value = @ExampleProperty(value = "1636023857137", mediaType = "application/json")))
    public long getServerTime() throws ThingsboardException {
        return System.currentTimeMillis();
    }

    @ApiOperation(value = "Get max data points limit (getMaxDatapointsLimit)",
            notes = "Get the maximum number of data points that dashboard may request from the server per in a single subscription command. " +
                    "This value impacts the time window behavior. It impacts 'Max values' parameter in case user selects 'None' as 'Data aggregation function'. " +
                    "It also impacts the 'Grouping interval' in case of any other 'Data aggregation function' is selected. " +
                    "The actual value of the limit is configurable in the system configuration file.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/maxDatapointsLimit", method = RequestMethod.GET)
    @ResponseBody
    @ApiResponse(code = 200, message = "OK", examples = @Example(value = @ExampleProperty(value = "5000", mediaType = "application/json")))
    public long getMaxDatapointsLimit() throws ThingsboardException {
        return maxDatapointsLimit;
    }

    @ApiOperation(value = "Get Dashboard Info (getDashboardInfoById)",
            notes = "Get the information about the dashboard based on 'dashboardId' parameter. " + DASHBOARD_INFO_DEFINITION,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/info/{dashboardId}", method = RequestMethod.GET)
    @ResponseBody
    public DashboardInfo getDashboardInfoById(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            return checkDashboardInfoId(dashboardId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Dashboard (getDashboardById)",
            notes = "Get the dashboard based on 'dashboardId' parameter. " + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.GET)
    @ResponseBody
    public Dashboard getDashboardById(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            return checkDashboardId(dashboardId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Dashboard (saveDashboard)",
            notes = "Create or update the Dashboard. When creating dashboard, platform generates Dashboard Id as " + UUID_WIKI_LINK +
                    "The newly created Dashboard id will be present in the response. " +
                    "Specify existing Dashboard id to update the dashboard. " +
                    "Referencing non-existing dashboard Id will cause 'Not Found' error. " +
                    "Only users with 'TENANT_ADMIN') authority may create the dashboards." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Dashboard entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard saveDashboard(
            @ApiParam(value = "A JSON value representing the dashboard.")
            @RequestBody Dashboard dashboard,
            @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return saveGroupEntity(dashboard, strEntityGroupId, (dashboard1, entityGroup) -> {
            try {
                return tbDashboardService.save(dashboard1, entityGroup, user);
            } catch (Exception e) {
                throw handleException(e);
            }
        });
    }

    @ApiOperation(value = "Delete the Dashboard (deleteDashboard)",
            notes = "Delete the Dashboard. Only users with 'TENANT_ADMIN') authority may delete the dashboards." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDashboard(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.DELETE);
        tbDashboardService.delete(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Dashboards by System Administrator (getTenantDashboards)",
            notes = "Returns a page of dashboard info objects owned by tenant. " + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS +
                    SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getTenantDashboards(
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(TENANT_ID) String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
            checkTenantId(tenantId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Dashboards (getTenantDashboards)",
            notes = "Returns a page of dashboard info objects owned by the tenant of a current user. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getTenantDashboards(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = HIDDEN_FOR_MOBILE)
            @RequestParam(required = false) Boolean mobile,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (mobile != null && mobile) {
                return checkNotNull(dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink));
            } else {
                return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Dashboards (getUserDashboards)",
            notes = "Returns a page of Dashboard Info objects available for specified or current user. " +
                    PAGE_DATA_PARAMETERS + DASHBOARD_INFO_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getUserDashboards(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = HIDDEN_FOR_MOBILE)
            @RequestParam(required = false) Boolean mobile,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "Filter by allowed operations for the current user")
            @RequestParam(required = false) String operation,
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @RequestParam(name = "userId", required = false) String strUserId) throws ThingsboardException {
        try {
            SecurityUser securityUser;
            if (!StringUtils.isEmpty(strUserId)) {
                UserId userId = new UserId(toUUID(strUserId));
                User user = checkUserId(userId, Operation.READ);
                UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
                securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user, false));
            } else {
                securityUser = getCurrentUser();
            }
            Operation operationType = Operation.READ;
            if (!StringUtils.isEmpty(operation)) {
                try {
                    operationType = Operation.valueOf(operation);
                } catch (IllegalArgumentException e) {
                    throw new ThingsboardException("Unsupported operation type '" + operation + "'!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            MergedUserPermissions mergedUserPermissions = securityUser.getUserPermissions();
            return entityService.findUserEntities(securityUser.getTenantId(), securityUser.getCustomerId(), mergedUserPermissions, EntityType.DASHBOARD,
                    operationType, null, pageLink, mobile != null ? mobile : false);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get All Dashboards for current user (getAllDashboards)",
            notes = "Returns a page of dashboard info objects owned by the tenant or the customer of a current user. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboards/all", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getAllDashboards(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                if (includeCustomers != null && includeCustomers) {
                    return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
                } else {
                    return checkNotNull(dashboardService.findTenantDashboardsByTenantId(tenantId, pageLink));
                }
            } else {
                CustomerId customerId = getCurrentUser().getCustomerId();
                if (includeCustomers != null && includeCustomers) {
                    return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerIdIncludingSubsCustomers(tenantId, customerId, pageLink));
                } else {
                    return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Dashboards (getCustomerDashboards)",
            notes = "Returns a page of dashboard info objects owned by the specified customer. "
                    + DASHBOARD_INFO_DEFINITION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getCustomerDashboards(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerIdIncludingSubsCustomers(tenantId, customerId, pageLink));
            } else {
                return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get dashboards by Dashboard Ids (getDashboardsByIds)",
            notes = "Returns a list of DashboardInfo objects based on the provided ids. Filters the list based on the user permissions. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboards", params = {"dashboardIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<DashboardInfo> getDashboardsByIds(
            @ApiParam(value = "A list of dashboard ids, separated by comma ','", required = true)
            @RequestParam("dashboardIds") String[] strDashboardIds) throws ThingsboardException {
        checkArrayParameter("dashboardIds", strDashboardIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<DashboardId> dashboardIds = new ArrayList<>();
            for (String strDashboardId : strDashboardIds) {
                dashboardIds.add(new DashboardId(toUUID(strDashboardId)));
            }
            List<DashboardInfo> dashboards = checkNotNull(dashboardService.findDashboardInfoByIdsAsync(tenantId, dashboardIds).get());
            return filterDashboardsByReadPermission(dashboards);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get dashboards by Entity Group Id (getDashboardsByEntityGroupId)",
            notes = "Returns a page of Dashboard objects that belongs to specified Entity Group Id. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getDashboardsByEntityGroupId(
            @ApiParam(value = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        checkEntityGroupType(EntityType.DASHBOARD, entityGroup.getType());
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(dashboardService.findDashboardsByEntityGroupId(entityGroupId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Import Dashboards (importGroupDashboards)",
            notes = "Import the dashboards to specified group."
                    + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_WRITE_CHECK,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/dashboards/import", method = RequestMethod.POST)
    @ResponseBody
    public void importGroupDashboards(
            @ApiParam(value = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = "JSON array with the dashboard objects", required = true)
            @RequestBody List<Dashboard> dashboardList,
            @ApiParam(value = "Overwrite dashboards with the same name")
            @RequestParam(required = false, defaultValue = "false", name = "overwrite") boolean overwrite) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            checkEntityGroupId(entityGroupId, Operation.WRITE);
            dashboardService.importDashboards(tenantId, entityGroupId, dashboardList, overwrite);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Export Dashboards (exportGroupDashboards)",
            notes = "Export the dashboards that belong to specified group id."
                    + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/dashboards/export", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Dashboard> exportGroupDashboards(
            @ApiParam(value = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = "Limit of the entities to export", required = true)
            @RequestParam int limit) throws ThingsboardException {
        try {

            TenantId tenantId = getCurrentUser().getTenantId();
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            checkEntityGroupId(entityGroupId, Operation.READ);
            TimePageLink pageLink = new TimePageLink(limit);
            return dashboardService.exportDashboards(tenantId, entityGroupId, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<DashboardInfo> filterDashboardsByReadPermission(List<DashboardInfo> dashboards) {
        return dashboards.stream().filter(dashboard -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ, dashboard.getId(), dashboard);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    @ApiOperation(value = "Get Home Dashboard (getHomeDashboard)",
            notes = "Returns the home dashboard object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the User. " +
                    "If 'homeDashboardId' parameter is not set on the User level and the User has authority 'CUSTOMER_USER', check the same parameter for the corresponding Customer. " +
                    "If 'homeDashboardId' parameter is not set on the User and Customer levels then checks the same parameter for the Tenant that owns the user. "
                    + DASHBOARD_DEFINITION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/home", method = RequestMethod.GET)
    @ResponseBody
    public HomeDashboard getHomeDashboard() throws ThingsboardException {
        try {
            SecurityUser securityUser = getCurrentUser();
            if (securityUser.isSystemAdmin()) {
                return null;
            }
            User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
            JsonNode additionalInfo;
            HomeDashboard homeDashboard = null;

            boolean ownerWhiteLabelingAllowed = whiteLabelingService.isWhiteLabelingAllowed(getTenantId(), user.getOwnerId());

            if (ownerWhiteLabelingAllowed) {
                additionalInfo = user.getAdditionalInfo();
                homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
            }
            if (homeDashboard == null) {
                if (securityUser.isCustomerUser() && ownerWhiteLabelingAllowed) {
                    Customer customer = customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId());
                    additionalInfo = customer.getAdditionalInfo();
                    homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
                }
                if (homeDashboard == null && ((securityUser.isTenantAdmin() && ownerWhiteLabelingAllowed) ||
                        (securityUser.isCustomerUser() && whiteLabelingService.isWhiteLabelingAllowed(getTenantId(), getTenantId())))) {
                    Tenant tenant = tenantService.findTenantById(securityUser.getTenantId());
                    additionalInfo = tenant.getAdditionalInfo();
                    homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
                }
            }
            return homeDashboard;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Home Dashboard Info (getHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the User. " +
                    "If 'homeDashboardId' parameter is not set on the User level and the User has authority 'CUSTOMER_USER', check the same parameter for the corresponding Customer. " +
                    "If 'homeDashboardId' parameter is not set on the User and Customer levels then checks the same parameter for the Tenant that owns the user. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/home/info", method = RequestMethod.GET)
    @ResponseBody
    public HomeDashboardInfo getHomeDashboardInfo() throws ThingsboardException {
        try {
            SecurityUser securityUser = getCurrentUser();
            if (securityUser.isSystemAdmin()) {
                return null;
            }
            User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
            JsonNode additionalInfo = user.getAdditionalInfo();
            HomeDashboardInfo homeDashboardInfo;
            homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
            if (homeDashboardInfo == null) {
                if (securityUser.isCustomerUser()) {
                    Customer customer = customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId());
                    additionalInfo = customer.getAdditionalInfo();
                    homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
                }
                if (homeDashboardInfo == null) {
                    Tenant tenant = tenantService.findTenantById(securityUser.getTenantId());
                    additionalInfo = tenant.getAdditionalInfo();
                    homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
                }
            }
            return homeDashboardInfo;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Home Dashboard Info (getTenantHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the corresponding tenant. " +
                    TENANT_AUTHORITY_PARAGRAPH + WL_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboard/home/info", method = RequestMethod.GET)
    @ResponseBody
    public HomeDashboardInfo getTenantHomeDashboardInfo() throws ThingsboardException {
        try {
            checkWhiteLabelingPermissions(Operation.READ);
            Tenant tenant = tenantService.findTenantById(getTenantId());
            JsonNode additionalInfo = tenant.getAdditionalInfo();
            DashboardId dashboardId = null;
            boolean hideDashboardToolbar = true;
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                dashboardId = new DashboardId(toUUID(strDashboardId));
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
            }
            return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Home Dashboard Info (getCustomerHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the corresponding customer. " +
                    CUSTOMER_AUTHORITY_PARAGRAPH + WL_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/customer/dashboard/home/info", method = RequestMethod.GET)
    @ResponseBody
    public HomeDashboardInfo getCustomerHomeDashboardInfo() throws ThingsboardException {
        try {
            checkWhiteLabelingPermissions(Operation.READ);
            Customer customer = customerService.findCustomerById(getTenantId(), getCurrentUser().getCustomerId());
            JsonNode additionalInfo = customer.getAdditionalInfo();
            DashboardId dashboardId = null;
            boolean hideDashboardToolbar = true;
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                dashboardId = new DashboardId(toUUID(strDashboardId));
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
            }
            return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Update Tenant Home Dashboard Info (getTenantHomeDashboardInfo)",
            notes = "Update the home dashboard assignment for the current tenant. " +
                    TENANT_AUTHORITY_PARAGRAPH + WL_WRITE_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboard/home/info", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void setTenantHomeDashboardInfo(
            @ApiParam(value = "A JSON object that represents home dashboard id and other parameters", required = true)
            @RequestBody HomeDashboardInfo homeDashboardInfo) throws ThingsboardException {
        try {
            checkWhiteLabelingPermissions(Operation.WRITE);
            if (homeDashboardInfo.getDashboardId() != null) {
                checkDashboardId(homeDashboardInfo.getDashboardId(), Operation.READ);
            }
            Tenant tenant = tenantService.findTenantById(getTenantId());
            JsonNode additionalInfo = tenant.getAdditionalInfo();
            if (additionalInfo == null || !(additionalInfo instanceof ObjectNode)) {
                additionalInfo = JacksonUtil.OBJECT_MAPPER.createObjectNode();
            }
            if (homeDashboardInfo.getDashboardId() != null) {
                ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_ID, homeDashboardInfo.getDashboardId().getId().toString());
                ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_HIDE_TOOLBAR, homeDashboardInfo.isHideDashboardToolbar());
            } else {
                ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_ID);
                ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_HIDE_TOOLBAR);
            }
            tenant.setAdditionalInfo(additionalInfo);
            tenantService.saveTenant(tenant);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Update Customer Home Dashboard Info (setCustomerHomeDashboardInfo)",
            notes = "Update the home dashboard assignment for the current customer. " +
                    CUSTOMER_AUTHORITY_PARAGRAPH + WL_WRITE_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/customer/dashboard/home/info", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void setCustomerHomeDashboardInfo(
            @ApiParam(value = "A JSON object that represents home dashboard id and other parameters", required = true)
            @RequestBody HomeDashboardInfo homeDashboardInfo) throws ThingsboardException {
        try {
            checkWhiteLabelingPermissions(Operation.WRITE);
            if (homeDashboardInfo.getDashboardId() != null) {
                checkDashboardId(homeDashboardInfo.getDashboardId(), Operation.READ);
            }
            Customer customer = customerService.findCustomerById(getTenantId(), getCurrentUser().getCustomerId());
            JsonNode additionalInfo = customer.getAdditionalInfo();
            if (additionalInfo == null || !(additionalInfo instanceof ObjectNode)) {
                additionalInfo = JacksonUtil.OBJECT_MAPPER.createObjectNode();
            }
            if (homeDashboardInfo.getDashboardId() != null) {
                ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_ID, homeDashboardInfo.getDashboardId().getId().toString());
                ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_HIDE_TOOLBAR, homeDashboardInfo.isHideDashboardToolbar());
            } else {
                ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_ID);
                ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_HIDE_TOOLBAR);
            }
            customer.setAdditionalInfo(additionalInfo);
            customerService.saveCustomer(customer);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private HomeDashboardInfo extractHomeDashboardInfoFromAdditionalInfo(JsonNode additionalInfo) {
        try {
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
                checkDashboardId(dashboardId, Operation.READ);
                boolean hideDashboardToolbar = true;
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
                return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private HomeDashboard extractHomeDashboardFromAdditionalInfo(JsonNode additionalInfo) {
        try {
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
                Dashboard dashboard = checkDashboardId(dashboardId, Operation.READ);
                boolean hideDashboardToolbar = true;
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
                return new HomeDashboard(dashboard, hideDashboardToolbar);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }
}
