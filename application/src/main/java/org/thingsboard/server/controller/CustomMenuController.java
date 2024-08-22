/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOM_MENU_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOM_MENU_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOM_MENU_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class CustomMenuController extends BaseController {

    @Autowired
    private CustomMenuService customMenuService;

    @ApiOperation(value = "Get All Custom menus for authorized user (getCustomMenuInfos)",
            notes = "Returns a page of custom menu info objects owned by the tenant or the customer of a current user. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customMenu/infos")
    public PageData<CustomMenuInfo> getCustomMenuInfos(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                       @RequestParam int pageSize,
                                                       @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                       @RequestParam int page,
                                                       @Parameter(description = CUSTOM_MENU_TEXT_SEARCH_DESCRIPTION)
                                                       @RequestParam(required = false) String textSearch,
                                                       @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title"}))
                                                       @RequestParam(required = false) String sortProperty,
                                                       @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                                       @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return customMenuService.getCustomMenuInfos(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), pageLink);
    }

    @ApiOperation(value = "Get end-user Custom Menu configuration (getCustomMenu)",
            notes = "Fetch the Custom Menu object for the authorized user. The custom menu is configured in the white labeling parameters. " +
                    "If custom menu configuration on the tenant level is present, it overrides the menu configuration of the system level. " +
                    "If the custom menu configuration on the customer level is present, it overrides the menu configuration of the tenant level." +
                    "If custom menu configuration is present on user level, it overrides the menu configuration of customer/tenant level. " +
                    "If no custom menu configured on user/customer/tenant level default customer/tenant hierarchy will be applied")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customMenu")
    public CustomMenuConfig getCustomMenu() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        Authority authority = currentUser.getAuthority();
        CustomMenuConfig customMenuConfig = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            customMenuConfig = customMenuService.getSystemAdminCustomMenu();
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            customMenuConfig = customMenuService.getTenantUserCustomMenu(currentUser.getTenantId(), currentUser.getId());
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            customMenuConfig = customMenuService.getCustomerUserCustomMenu(currentUser.getTenantId(), currentUser.getCustomerId(), currentUser.getId());
        }
        return checkNotNull(customMenuConfig);
    }

    @ApiOperation(value = "Get Custom Menu configuration by id (getCustomMenuById)",
            notes = "Fetch the Custom Menu object that corresponds to the authority of the user. " +
                    "The API call is designed to load the custom menu items for edition. " +
                    "So, the result is NOT merged with the parent level configuration. " +
                    "Let's assume there is a custom menu configured on a system level. " +
                    "And there is no custom menu items configured on a tenant level. " +
                    "In such a case, the API call will return empty object for the tenant administrator. " +
                    ControllerConstants.CUSTOM_MENU_READ_CHECK
    )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customMenu/{customMenuId}/config")
    public CustomMenuConfig getCustomMenuById(@Parameter(description = CUSTOM_MENU_ID_PARAM_DESCRIPTION)
                                        @PathVariable(CUSTOM_MENU_ID) UUID id) throws ThingsboardException {
        CustomMenuId customMenuId = new CustomMenuId(id);
        return checkNotNull(checkCustomMenuId(customMenuId, Operation.READ)).getConfig();
    }

    @ApiOperation(value = "Update Custom Menu configuration by id (updateCustomMenuConfig)",
            notes = ControllerConstants.CUSTOM_MENU_WRITE_CHECK
    )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = "/customMenu/{customMenuId}/config")
    public CustomMenu updateCustomMenuConfig(@Parameter(description = "A JSON value representing the custom menu configuration")
                                       @RequestBody CustomMenuConfig customMenuConfig,
                                       @Parameter(description = CUSTOM_MENU_ID_PARAM_DESCRIPTION)
                                       @PathVariable(CUSTOM_MENU_ID) UUID id) throws ThingsboardException {
        CustomMenuId customMenuId = new CustomMenuId(id);
        CustomMenu customMenu = checkNotNull(checkCustomMenuId(customMenuId, Operation.READ));
        customMenu.setConfig(customMenuConfig);
        return customMenuService.updateCustomMenu(customMenu);
    }

    @ApiOperation(value = "Update Custom Menu configuration by id (updateCustomMenuName)",
            notes = ControllerConstants.CUSTOM_MENU_WRITE_CHECK
    )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = "/customMenu/{customMenuId}/name")
    public void updateCustomMenuName(@Parameter(description = CUSTOM_MENU_ID_PARAM_DESCRIPTION)
                                  @PathVariable(CUSTOM_MENU_ID) UUID id,
                                  String name) throws ThingsboardException {
        CustomMenuId customMenuId = new CustomMenuId(id);
        checkNotNull(checkCustomMenuId(customMenuId, Operation.READ));
        customMenuService.updateCustomMenuName(customMenuId, name);
    }

    @ApiOperation(value = "Create Or Update Custom Menu (saveCustomMenu)",
            notes = "Creates or Updates the Custom Menu configuration." +
                    ControllerConstants.CUSTOM_MENU_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/customMenu")
    public CustomMenu saveCustomMenu(
            @Parameter(description = "A JSON value representing the custom menu basic info fields")
            @RequestBody CustomMenuInfo customMenuInfo,
            @Parameter(description = "A list of entity ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam(name = "assignToList", required = false) UUID[] ids) throws ThingsboardException {
        Operation operation = customMenuInfo.getId() == null ? Operation.CREATE : Operation.WRITE;
        SecurityUser currentUser = getCurrentUser();
        customMenuInfo.setTenantId(currentUser.getTenantId());
        customMenuInfo.setCustomerId(currentUser.getCustomerId());
        accessControlService.checkCustomMenuPermission(currentUser, operation, customMenuInfo);
        List<EntityId> assignToList = getAssignToList(customMenuInfo.getAssigneeType(), ids);
        return customMenuService.saveCustomMenuInfo(customMenuInfo, assignToList);
    }

    @ApiOperation(value = "Update custom menu assigners (updateCustomMenuAssigners)",
            notes = "Update oauth2 clients for the specified domain. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = "/customMenu/{id}/assignToList")
    public void updateCustomMenuAssigners(@PathVariable UUID id,
                                          @RequestBody UUID[] entityIds) throws ThingsboardException {
        CustomMenuId customMenuId = new CustomMenuId(id);
        CustomMenuInfo customMenuInfo = checkCustomMenuId(customMenuId, Operation.WRITE);
        List<EntityId> assigners = getAssignToList(customMenuInfo.getAssigneeType(), entityIds);
        customMenuService.updateCustomMenuAssignToList(customMenuInfo, assigners);
    }

    @ApiOperation(value = "Delete custom menu (deleteCustomMenu)",
            notes = "Deletes the custom menu. " +
                    "Referencing non-existing custom menu Id will cause an error. " +
                    "If the custom menu is associated with the user, customer or tenant, it will not be allowed for deletion.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/customMenu/{customMenuId}")
    public CustomMenuDeleteResult deleteCustomMenu(@Parameter(required = true, description = CUSTOM_MENU_ID_PARAM_DESCRIPTION)
                                                   @PathVariable(CUSTOM_MENU_ID) UUID id,
                                                   @RequestParam(name = "force", required = false) boolean force) throws ThingsboardException {
        CustomMenuId customMenuId = new CustomMenuId(id);
        checkCustomMenuId(customMenuId, Operation.DELETE);
        return customMenuService.deleteCustomMenu(getTenantId(), customMenuId, force);
    }

    private List<EntityId> getAssignToList(CMAssigneeType type, UUID[] ids) throws ThingsboardException {
        if (ids == null) {
            return List.of();
        }
        List<EntityId> assignToList = new ArrayList<>();
        switch (type) {
            case CUSTOMERS:
                for (UUID id : ids) {
                    CustomerId customerId = new CustomerId(id);
                    checkCustomerId(customerId, Operation.WRITE);
                    assignToList.add(customerId);
                }
                break;
            case USERS:
                for (UUID id : ids) {
                    UserId userId = new UserId(id);
                    checkUserId(userId, Operation.WRITE);
                    assignToList.add(userId);
                }
                break;
            default:
                throw new IllegalArgumentException("List of assigners can be applied only to CUSTOMERS or USERS assignee type!");
        }
        return assignToList;
    }
}
