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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
public class TenantController extends BaseController {

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private TenantService tenantService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.GET)
    @ResponseBody
    public Tenant getTenantById(@PathVariable("tenantId") String strTenantId) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            checkTenantId(tenantId, Operation.READ);
            return checkNotNull(tenantService.findTenantById(tenantId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant", method = RequestMethod.POST)
    @ResponseBody
    public Tenant saveTenant(@RequestBody Tenant tenant) throws ThingsboardException {
        try {
            boolean newTenant = tenant.getId() == null;

            Operation operation = newTenant ? Operation.CREATE : Operation.WRITE;

            accessControlService.checkPermission(getCurrentUser(), Resource.TENANT, operation,
                    tenant.getId(), tenant);

            tenant = checkNotNull(tenantService.saveTenant(tenant));
            if (newTenant) {
                installScripts.createDefaultRuleChains(tenant.getId());
            }
            return tenant;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTenant(@PathVariable("tenantId") String strTenantId) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            checkTenantId(tenantId, Operation.DELETE);
            tenantService.deleteTenant(tenantId);

            actorService.onEntityStateChange(tenantId, tenantId, ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenants", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Tenant> getTenants(@RequestParam int limit,
                                           @RequestParam(required = false) String textSearch,
                                           @RequestParam(required = false) String idOffset,
                                           @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(tenantService.findTenants(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenants", params = {"tenantIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Tenant> getTenantsByIds(
            @RequestParam("tenantIds") String[] strTenantIds) throws ThingsboardException {
        checkArrayParameter("tenantIds", strTenantIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<TenantId> tenantIds = new ArrayList<>();
            for (String strTenantId : strTenantIds) {
                tenantIds.add(new TenantId(toUUID(strTenantId)));
            }
            List<Tenant> tenants = checkNotNull(tenantService.findTenantsByIdsAsync(tenantId, tenantIds).get());
            return filterTenantsByReadPermission(tenants);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<Tenant> filterTenantsByReadPermission(List<Tenant> tenants) {
        return tenants.stream().filter(tenant -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.TENANT, Operation.READ, tenant.getId(), tenant);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

}
