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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HomeDashboard;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.EntityGroupController.ENTITY_GROUP_ID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class DashboardController extends BaseController {

    public static final String DASHBOARD_ID = "dashboardId";

    @Autowired
    private WhiteLabelingService whiteLabelingService;

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
        return saveGroupEntity(dashboard, strEntityGroupId, dashboardService::saveDashboard);
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDashboard(@PathVariable(DASHBOARD_ID) String strDashboardId) throws ThingsboardException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            Dashboard dashboard = checkDashboardId(dashboardId, Operation.DELETE);

            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), dashboardId);

            dashboardService.deleteDashboard(getCurrentUser().getTenantId(), dashboardId);
            logEntityAction(dashboardId, dashboard,
                    null,
                    ActionType.DELETED, null, strDashboardId);

            sendDeleteNotificationMsg(getTenantId(), dashboardId, relatedEdgeIds);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DASHBOARD),
                    null,
                    null,
                    ActionType.DELETED, e, strDashboardId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getTenantDashboards(
            @PathVariable("tenantId") String strTenantId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            checkTenantId(tenantId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getTenantDashboards(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/user/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getUserDashboards(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
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
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            MergedUserPermissions mergedUserPermissions = securityUser.getUserPermissions();
            return entityService.findUserEntities(securityUser.getTenantId(), securityUser.getCustomerId(), mergedUserPermissions, EntityType.DASHBOARD,
                    operationType, null, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getGroupDashboards(
            @PathVariable("entityGroupId") String strEntityGroupId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            checkParameter("entityGroupId", strEntityGroupId);
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
            if (entityGroup.getType() != EntityType.DASHBOARD) {
                throw new ThingsboardException("Invalid entity group type '" + entityGroup.getType() + "'! Should be 'DASHBOARD'.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(dashboardService.findDashboardsByEntityGroupId(entityGroupId, pageLink));
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

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/dashboards", method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getDashboardsByEntityGroupId(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = "Page size", required = true, allowableValues = "range[1, infinity]") @RequestParam int pageSize,
            @ApiParam(value = "Page", required = true, allowableValues = "range[0, infinity]") @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        EntityType entityType = entityGroup.getType();
        checkEntityGroupType(entityType);
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(dashboardService.findDashboardsByEntityGroupId(entityGroupId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/dashboards/import", method = RequestMethod.POST)
    @ResponseBody
    public void importGroupDashboards(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @RequestBody List<Dashboard> dashboardList,
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

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/dashboards/export", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Dashboard> exportGroupDashboards(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
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

    @PreAuthorize("isAuthenticated()")
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

    @PreAuthorize("isAuthenticated()")
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

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboard/home/info", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void setTenantHomeDashboardInfo(@RequestBody HomeDashboardInfo homeDashboardInfo) throws ThingsboardException {
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

    @PreAuthorize("hasAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/customer/dashboard/home/info", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void setCustomerHomeDashboardInfo(@RequestBody HomeDashboardInfo homeDashboardInfo) throws ThingsboardException {
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
        } catch (Exception e) {}
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
        } catch (Exception e) {}
        return null;
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }
}
