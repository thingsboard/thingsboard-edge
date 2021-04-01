/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class TenantProfileController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public TenantProfile getTenantProfileById(@PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        try {
            TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
            return checkTenantProfileId(tenantProfileId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfo/{tenantProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityInfo getTenantProfileInfoById(@PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        try {
            TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
            return checkNotNull(tenantProfileService.findTenantProfileInfoById(getTenantId(), tenantProfileId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfo/default", method = RequestMethod.GET)
    @ResponseBody
    public EntityInfo getDefaultTenantProfileInfo() throws ThingsboardException {
        try {
            return checkNotNull(tenantProfileService.findDefaultTenantProfileInfo(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile", method = RequestMethod.POST)
    @ResponseBody
    public TenantProfile saveTenantProfile(@RequestBody TenantProfile tenantProfile) throws ThingsboardException {
        try {
            boolean newTenantProfile = tenantProfile.getId() == null;

            if (newTenantProfile) {
                accessControlService
                        .checkPermission(getCurrentUser(), Resource.TENANT_PROFILE, Operation.CREATE);
            } else {
                checkEntityId(tenantProfile.getId(), Operation.WRITE);
            }

            tenantProfile = checkNotNull(tenantProfileService.saveTenantProfile(getTenantId(), tenantProfile));
            tenantProfileCache.put(tenantProfile);
            tbClusterService.onTenantProfileChange(tenantProfile, null);
            tbClusterService.onEntityStateChange(TenantId.SYS_TENANT_ID, tenantProfile.getId(),
                    newTenantProfile ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return tenantProfile;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTenantProfile(@PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        try {
            TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
            TenantProfile profile = checkTenantProfileId(tenantProfileId, Operation.DELETE);
            tenantProfileService.deleteTenantProfile(getTenantId(), tenantProfileId);
            tbClusterService.onTenantProfileDelete(profile, null);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    public TenantProfile setDefaultTenantProfile(@PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        try {
            TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
            TenantProfile tenantProfile = checkTenantProfileId(tenantProfileId, Operation.WRITE);
            tenantProfileService.setDefaultTenantProfile(getTenantId(), tenantProfileId);
            return tenantProfile;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<TenantProfile> getTenantProfiles(@RequestParam int pageSize,
                                                     @RequestParam int page,
                                                     @RequestParam(required = false) String textSearch,
                                                     @RequestParam(required = false) String sortProperty,
                                                     @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(tenantProfileService.findTenantProfiles(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityInfo> getTenantProfileInfos(@RequestParam int pageSize,
                                                      @RequestParam int page,
                                                      @RequestParam(required = false) String textSearch,
                                                      @RequestParam(required = false) String sortProperty,
                                                      @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(tenantProfileService.findTenantProfileInfos(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
