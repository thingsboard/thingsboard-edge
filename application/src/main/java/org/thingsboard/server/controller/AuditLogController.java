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

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class AuditLogController extends BaseController {

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/audit/logs/customer/{customerId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByCustomerId(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            checkParameter("CustomerId", strCustomerId);
            accessControlService.checkPermission(getCurrentUser(), Resource.AUDIT_LOG, Operation.READ);
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            return checkNotNull(auditLogService.findAuditLogsByTenantIdAndCustomerId(tenantId, new CustomerId(UUID.fromString(strCustomerId)), actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/audit/logs/user/{userId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByUserId(
            @PathVariable("userId") String strUserId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            checkParameter("UserId", strUserId);
            accessControlService.checkPermission(getCurrentUser(), Resource.AUDIT_LOG, Operation.READ);
            UserId userId = new UserId(toUUID(strUserId));
            checkUserId(userId, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            return checkNotNull(auditLogService.findAuditLogsByTenantIdAndUserId(tenantId, new UserId(UUID.fromString(strUserId)), actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/audit/logs/entity/{entityType}/{entityId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogsByEntityId(
            @PathVariable("entityType") String strEntityType,
            @PathVariable("entityId") String strEntityId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            checkParameter("EntityId", strEntityId);
            checkParameter("EntityType", strEntityType);
            accessControlService.checkPermission(getCurrentUser(), Resource.AUDIT_LOG, Operation.READ);
            EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
            checkEntityId(entityId, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            return checkNotNull(auditLogService.findAuditLogsByTenantIdAndEntityId(tenantId, EntityIdFactory.getByTypeAndId(strEntityType, strEntityId), actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/audit/logs", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AuditLog> getAuditLogs(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.AUDIT_LOG, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            Authority authority = getCurrentUser().getAuthority();
            if (Authority.TENANT_ADMIN.equals(authority)) {
                return checkNotNull(auditLogService.findAuditLogsByTenantId(tenantId, actionTypes, pageLink));
            } else {
                return checkNotNull(auditLogService.findAuditLogsByTenantIdAndCustomerId(tenantId, getCurrentUser().getCustomerId(), actionTypes, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<ActionType> parseActionTypesStr(String actionTypesStr) {
        List<ActionType> result = null;
        if (StringUtils.isNoneBlank(actionTypesStr)) {
            String[] tmp = actionTypesStr.split(",");
            result = Arrays.stream(tmp).map(at -> ActionType.valueOf(at.toUpperCase())).collect(Collectors.toList());
        }
        return result;
    }
}
