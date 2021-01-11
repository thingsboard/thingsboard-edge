/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customMenu/customMenu", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CustomMenu getCustomMenu() throws ThingsboardException {
        try {
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
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customMenu/currentCustomMenu", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CustomMenu getCurrentCustomMenu() throws ThingsboardException {
        try {
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
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customMenu/customMenu", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public CustomMenu saveCustomMenu(@RequestBody(required = false) CustomMenu customMenu) throws ThingsboardException {
        try {
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
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
