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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.AllowedPermissionsInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class UserPermissionsController extends BaseController {

    @ApiOperation(value = "Get Permissions (getAllowedPermissions)",
            notes = "Returns a complex object that describes:\n\n" +
                    " * all possible (both granted and not granted) permissions for the authority of the user (Tenant or Customer);\n" +
                    " * all granted permissions for the user;\n\n " +
                    "The result impacts UI behavior and hides certain UI elements if user has no permissions to invoke the related operations. " +
                    "Nevertheless, all API calls check the permissions each time they are executed on the server side." +
                    PAGE_DATA_PARAMETERS + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/permissions/allowedPermissions", method = RequestMethod.GET)
    @ResponseBody
    public AllowedPermissionsInfo getAllowedPermissions() throws ThingsboardException {
        try {
            Set<Resource> allowedResources = Resource.resourcesByAuthority.get(getCurrentUser().getAuthority());
            Map<Resource, Set<Operation>> operationsByResource = new HashMap<>();
            allowedResources.forEach(resource -> operationsByResource.put(resource, Resource.operationsByResource.get(resource)));
            return new AllowedPermissionsInfo(operationsByResource,
                    Operation.allowedForGroupRoleOperations,
                    Operation.allowedForGroupOwnerOnlyOperations,
                    Operation.allowedForGroupOwnerOnlyGroupOperations,
                    allowedResources, getCurrentUser().getUserPermissions(), getCurrentUser().getOwnerId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
