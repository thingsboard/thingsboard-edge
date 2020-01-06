/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuditLogController extends BaseController {

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/audit/logs/customer/{customerId}", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<AuditLog> getAuditLogsByCustomerId(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset,
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            checkParameter("CustomerId", strCustomerId);
            accessControlService.checkPermission(getCurrentUser(), Resource.AUDIT_LOG, Operation.READ);
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            return checkNotNull(auditLogService.findAuditLogsByTenantIdAndCustomerId(tenantId, new CustomerId(UUID.fromString(strCustomerId)), actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/audit/logs/user/{userId}", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<AuditLog> getAuditLogsByUserId(
            @PathVariable("userId") String strUserId,
            @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset,
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            checkParameter("UserId", strUserId);
            accessControlService.checkPermission(getCurrentUser(), Resource.AUDIT_LOG, Operation.READ);
            UserId userId = new UserId(toUUID(strUserId));
            checkUserId(userId, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            return checkNotNull(auditLogService.findAuditLogsByTenantIdAndUserId(tenantId, new UserId(UUID.fromString(strUserId)), actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/audit/logs/entity/{entityType}/{entityId}", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<AuditLog> getAuditLogsByEntityId(
            @PathVariable("entityType") String strEntityType,
            @PathVariable("entityId") String strEntityId,
            @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset,
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            checkParameter("EntityId", strEntityId);
            checkParameter("EntityType", strEntityType);
            accessControlService.checkPermission(getCurrentUser(), Resource.AUDIT_LOG, Operation.READ);
            EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
            checkEntityId(entityId, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            return checkNotNull(auditLogService.findAuditLogsByTenantIdAndEntityId(tenantId, EntityIdFactory.getByTypeAndId(strEntityType, strEntityId), actionTypes, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/audit/logs", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<AuditLog> getAuditLogs(
            @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset,
            @RequestParam(name = "actionTypes", required = false) String actionTypesStr) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.AUDIT_LOG, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            List<ActionType> actionTypes = parseActionTypesStr(actionTypesStr);
            Authority authority = getCurrentUser().getAuthority();
            if (authority == Authority.TENANT_ADMIN) {
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
