/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
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

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class UserPermissionsController extends BaseController {

    @PreAuthorize("isAuthenticated()")
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
