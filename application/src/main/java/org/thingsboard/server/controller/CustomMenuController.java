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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class CustomMenuController extends BaseController {

    @Autowired
    private CustomMenuService customMenuService;

    @ApiOperation(value = "Get end-user Custom Menu configuration (getCustomMenu)",
            notes = "Fetch the Custom Menu object for the end user. The custom menu is configured in the white labeling parameters. " +
                    "If custom menu configuration on the tenant level is present, it overrides the menu configuration of the system level. " +
                    "Similar, if the custom menu configuration on the customer level is present, it overrides the menu configuration of the tenant level."
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customMenu/customMenu", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CustomMenu getCustomMenu() throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        CustomMenu customMenu = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            customMenu = customMenuService.getSystemCustomMenu(TenantId.SYS_TENANT_ID);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            customMenu = customMenuService.getMergedTenantCustomMenu(getCurrentUser().getTenantId());
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            customMenu = customMenuService.getMergedCustomerCustomMenu(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId());
        }
        if (customMenu == null) {
            customMenu = new CustomMenu();
        }
        return customMenu;
    }

    @ApiOperation(value = "Get Custom Menu configuration (getCustomMenu)",
            notes = "Fetch the Custom Menu object that corresponds to the authority of the user. " +
                    "The API call is designed to load the custom menu items for edition. " +
                    "So, the result is NOT merged with the parent level configuration. " +
                    "Let's assume there is a custom menu configured on a system level. " +
                    "And there is no custom menu items configured on a tenant level. " +
                    "In such a case, the API call will return empty object for the tenant administrator. " +
                    ControllerConstants.WL_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customMenu/currentCustomMenu", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CustomMenu getCurrentCustomMenu() throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.READ);
        CustomMenu customMenu = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            customMenu = customMenuService.getSystemCustomMenu(TenantId.SYS_TENANT_ID);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            customMenu = customMenuService.getTenantCustomMenu(getTenantId());
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            customMenu = customMenuService.getCustomerCustomMenu(getTenantId(), getCurrentUser().getCustomerId());
        }
        return customMenu;
    }

    @ApiOperation(value = "Create Or Update Custom Menu (saveCustomMenu)",
            notes = "Creates or Updates the Custom Menu configuration." +
                     ControllerConstants.WL_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customMenu/customMenu", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public CustomMenu saveCustomMenu(
            @ApiParam(value = "A JSON value representing the custom menu")
            @RequestBody(required = false) CustomMenu customMenu) throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.WRITE);
        CustomMenu savedCustomMenu = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            savedCustomMenu = customMenuService.saveSystemCustomMenu(customMenu);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            savedCustomMenu = customMenuService.saveTenantCustomMenu(getCurrentUser().getTenantId(), customMenu);
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            savedCustomMenu = customMenuService.saveCustomerCustomMenu(getTenantId(), getCurrentUser().getCustomerId(), customMenu);
        }
        return savedCustomMenu;
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
