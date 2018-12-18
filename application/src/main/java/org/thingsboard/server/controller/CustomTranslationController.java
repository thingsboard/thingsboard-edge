/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.translation.CustomTranslationService;

@RestController
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
            if (authority == Authority.SYS_ADMIN) {
                customTranslation = customTranslationService.getSystemCustomTranslation(TenantId.SYS_TENANT_ID);
            } else if (authority == Authority.TENANT_ADMIN) {
                customTranslation = customTranslationService.getMergedTenantCustomTranslation(getCurrentUser().getTenantId());
            } else if (authority == Authority.CUSTOMER_USER) {
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
            if (authority == Authority.SYS_ADMIN) {
                customTranslation = customTranslationService.getSystemCustomTranslation(TenantId.SYS_TENANT_ID);
            } else if (authority == Authority.TENANT_ADMIN) {
                customTranslation = customTranslationService.getTenantCustomTranslation(getTenantId());
            } else if (authority == Authority.CUSTOMER_USER) {
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
            if (authority == Authority.SYS_ADMIN) {
                savedCustomTranslation = customTranslationService.saveSystemCustomTranslation(customTranslation);
            } else if (authority == Authority.TENANT_ADMIN) {
                savedCustomTranslation = customTranslationService.saveTenantCustomTranslation(getCurrentUser().getTenantId(), customTranslation);
            } else if (authority == Authority.CUSTOMER_USER) {
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
