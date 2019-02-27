/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DashboardController extends BaseController {

    public static final String DASHBOARD_ID = "dashboardId";

    @Value("${dashboard.max_datapoints_limit}")
    private long maxDatapointsLimit;


    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/serverTime", method = RequestMethod.GET)
    @ResponseBody
    public long getServerTime() throws ThingsboardException {
        return System.currentTimeMillis();
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/maxDatapointsLimit", method = RequestMethod.GET)
    @ResponseBody
    public long getMaxDatapointsLimit() throws ThingsboardException {
        return maxDatapointsLimit;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/info/{dashboardId}", method = RequestMethod.GET)
    @ResponseBody
    public DashboardInfo getDashboardInfoById(@PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            return checkDashboardInfoId(dashboardId, Operation.READ);
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
            return checkDashboardId(dashboardId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard", method = RequestMethod.POST)
    @ResponseBody 
    public Dashboard saveDashboard(@RequestBody Dashboard dashboard,
                                   @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId) throws ThingsboardException {
        try {
            dashboard.setTenantId(getCurrentUser().getTenantId());

            Operation operation = dashboard.getId() == null ? Operation.CREATE : Operation.WRITE;

            if (operation == Operation.CREATE
                    && getCurrentUser().getAuthority() == Authority.CUSTOMER_USER &&
                    (dashboard.getCustomerId() == null || dashboard.getCustomerId().isNullUid())) {
                dashboard.setCustomerId(getCurrentUser().getCustomerId());
            }

            EntityGroupId entityGroupId = null;
            if (!StringUtils.isEmpty(strEntityGroupId)) {
                entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            }

            accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, operation,
                    dashboard.getId(), dashboard, entityGroupId);

            Dashboard savedDashboard = checkNotNull(dashboardService.saveDashboard(dashboard));

            if (entityGroupId != null && operation == Operation.CREATE) {
                entityGroupService.addEntityToEntityGroup(getTenantId(), entityGroupId, savedDashboard.getId());
            }

            logEntityAction(savedDashboard.getId(), savedDashboard,
                    null,
                    dashboard.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedDashboard;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DASHBOARD), dashboard,
                    null, dashboard.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDashboard(@PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            Dashboard dashboard = checkDashboardId(dashboardId, Operation.DELETE);
            dashboardService.deleteDashboard(getCurrentUser().getTenantId(), dashboardId);

            logEntityAction(dashboardId, dashboard,
                    null,
                    ActionType.DELETED, null, strDashboardId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.DASHBOARD),
                    null,
                    null,
                    ActionType.DELETED, e, strDashboardId);

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
            checkTenantId(tenantId, Operation.READ);
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
            accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/user/dashboards", params = { "limit" }, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<DashboardInfo> getUserDashboards(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset,
            @RequestParam(required = false) String operation,
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
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return getGroupEntitiesByPageLink(securityUser, EntityType.DASHBOARD, operationType, entityId -> new DashboardId(entityId.getId()),
                    (entityIds) -> {
                        try {
                            return dashboardService.findDashboardInfoByIdsAsync(getTenantId(), entityIds).get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/dashboards", params = { "limit" }, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<DashboardInfo> getGroupDashboards(
            @PathVariable("entityGroupId") String strEntityGroupId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            checkParameter("entityGroupId", strEntityGroupId);
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
            if (entityGroup.getType() != EntityType.DASHBOARD) {
                throw new ThingsboardException("Invalid entity group type '" + entityGroup.getType() + "'! Should be 'DASHBOARD'.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            List<EntityId> ids = entityGroupService.findAllEntityIds(getTenantId(), entityGroupId, new TimePageLink(Integer.MAX_VALUE)).get();
            List<DashboardId> dashboardIdsList = new ArrayList<>();
            ids.forEach((dashboardId) -> dashboardIdsList.add(new DashboardId(dashboardId.getId())));
            return loadAndFilterEntities(dashboardIdsList, (entityIds) -> {
                try {
                    return dashboardService.findDashboardInfoByIdsAsync(getTenantId(), entityIds).get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboards", params = {"dashboardIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<DashboardInfo> getDashboardsByIds(
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

    private List<DashboardInfo> filterDashboardsByReadPermission(List<DashboardInfo> dashboards) {
        return dashboards.stream().filter(dashboard -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ, dashboard.getId(), dashboard);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

}
