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
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class CustomTranslationController extends BaseController {

    @Autowired
    private CustomTranslationService customTranslationService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customTranslation/customTranslation", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CustomTranslation getCustomTranslation() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            CustomTranslation customTranslation = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                customTranslation = customTranslationService.getSystemCustomTranslation(TenantId.SYS_TENANT_ID);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                customTranslation = customTranslationService.getMergedTenantCustomTranslation(getCurrentUser().getTenantId());
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                customTranslation = customTranslationService.getMergedCustomerCustomTranslation(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId());
            }
            return customTranslation;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customTranslation/currentCustomTranslation", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CustomTranslation getCurrentCustomTranslation() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.READ);
            CustomTranslation customTranslation = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                customTranslation = customTranslationService.getSystemCustomTranslation(TenantId.SYS_TENANT_ID);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                customTranslation = customTranslationService.getTenantCustomTranslation(getTenantId());
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                customTranslation = customTranslationService.getCustomerCustomTranslation(getTenantId(), getCurrentUser().getCustomerId());
            }
            return customTranslation;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customTranslation/customTranslation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public CustomTranslation saveCustomTranslation(@RequestBody CustomTranslation customTranslation) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.WRITE);
            CustomTranslation savedCustomTranslation = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                savedCustomTranslation = customTranslationService.saveSystemCustomTranslation(customTranslation);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                savedCustomTranslation = customTranslationService.saveTenantCustomTranslation(getCurrentUser().getTenantId(), customTranslation);
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                savedCustomTranslation = customTranslationService.saveCustomerCustomTranslation(getTenantId(), getCurrentUser().getCustomerId(), customTranslation);
            }
            return savedCustomTranslation;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
