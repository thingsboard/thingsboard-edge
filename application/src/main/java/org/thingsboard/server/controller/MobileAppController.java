/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.mobile.TbMobileAppService;

import java.util.UUID;

import static org.thingsboard.server.common.data.permission.Resource.MOBILE_APP;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MobileAppController extends BaseController {

    private final TbMobileAppService tbMobileAppService;

    @ApiOperation(value = "Save Or update Mobile app (saveMobileApp)",
            notes = "Create or update the Mobile app. When creating mobile app, platform generates Mobile App Id as " + UUID_WIKI_LINK +
                    "The newly created Mobile App Id will be present in the response. " +
                    "Specify existing Mobile App Id to update the mobile app. " +
                    "Referencing non-existing Mobile App Id will cause 'Not Found' error." +
                    "\n\nThe pair of mobile app package name and platform type is unique for entire platform setup.\n\n" + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping(value = "/mobile/app")
    public MobileApp saveMobileApp(
            @Parameter(description = "A JSON value representing the Mobile Application.", required = true)
            @RequestBody @Valid MobileApp mobileApp) throws Exception {
        mobileApp.setTenantId(getTenantId());
        checkEntity(mobileApp.getId(), mobileApp, MOBILE_APP);
        return tbMobileAppService.save(mobileApp, getCurrentUser());
    }

    @ApiOperation(value = "Get mobile app infos (getTenantMobileAppInfos)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping(value = "/mobile/app")
    public PageData<MobileApp> getTenantMobileAppInfos(@Parameter(description = "Platform type: ANDROID or IOS")
                                                       @RequestParam(required = false) PlatformType platformType,
                                                       @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                       @RequestParam int pageSize,
                                                       @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                       @RequestParam int page,
                                                       @Parameter(description = "Case-insensitive 'substring' filter based on app's name")
                                                       @RequestParam(required = false) String textSearch,
                                                       @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                                       @RequestParam(required = false) String sortProperty,
                                                       @Parameter(description = SORT_ORDER_DESCRIPTION)
                                                       @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), MOBILE_APP, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return mobileAppService.findMobileAppsByTenantId(getTenantId(), platformType, pageLink);
    }

    @ApiOperation(value = "Get mobile info by id (getMobileAppInfoById)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping(value = "/mobile/app/{id}")
    public MobileApp getMobileAppById(@PathVariable UUID id) throws ThingsboardException {
        MobileAppId mobileAppId = new MobileAppId(id);
        return checkEntityId(mobileAppId, mobileAppService::findMobileAppById, Operation.READ);
    }

    @ApiOperation(value = "Delete Mobile App by ID (deleteMobileApp)",
            notes = "Deletes Mobile App by ID. Referencing non-existing mobile app Id will cause an error." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @DeleteMapping(value = "/mobile/app/{id}")
    public void deleteMobileApp(@PathVariable UUID id) throws Exception {
        MobileAppId mobileAppId = new MobileAppId(id);
        MobileApp mobileApp = checkMobileAppId(mobileAppId, Operation.DELETE);
        tbMobileAppService.delete(mobileApp, getCurrentUser());
    }

}
