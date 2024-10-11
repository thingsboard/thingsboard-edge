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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.mobile.LoginMobileInfo;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.common.data.mobile.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.MobileAppVersionInfo;
import org.thingsboard.server.common.data.mobile.UserMobileInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
public class MobileV2Controller extends BaseController {

    @ApiOperation(value = "Get mobile app login info (getLoginMobileInfo)")
    @GetMapping(value = "/api/noauth/mobile")
    public LoginMobileInfo getLoginMobileInfo(@Parameter(description = "Mobile application package name")
                                              @RequestParam String pkgName,
                                              @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                              @RequestParam PlatformType platform) {
        List<OAuth2ClientLoginInfo> oauth2Clients = oAuth2ClientService.findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType(pkgName, platform);
        MobileApp mobileApp = mobileAppService.findMobileAppByPkgNameAndPlatformType(pkgName, platform);
        return new LoginMobileInfo(oauth2Clients, mobileApp != null ? mobileApp.getVersionInfo() : null);
    }

    @ApiOperation(value = "Get user mobile app basic info (getUserMobileInfo)", notes = AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/api/mobile")
    public UserMobileInfo getUserMobileInfo(@Parameter(description = "Mobile application package name")
                                            @RequestParam String pkgName,
                                            @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                            @RequestParam PlatformType platform) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        HomeDashboardInfo homeDashboardInfo = securityUser.isSystemAdmin() ? null : getHomeDashboardInfo(securityUser, user.getAdditionalInfo());
        MobileAppBundle mobileAppBundle = mobileAppBundleService.findMobileAppBundleByPkgNameAndPlatform(securityUser.getTenantId(), pkgName, platform);
        return new UserMobileInfo(user, homeDashboardInfo, mobileAppBundle != null ? mobileAppBundle.getLayoutConfig() : null);
    }

    @ApiOperation(value = "Get mobile app version info (getMobileVersionInfo)")
    @GetMapping(value = "/api/mobile/versionInfo")
    public MobileAppVersionInfo getMobileVersionInfo(@Parameter(description = "Mobile application package name")
                                                     @RequestParam String pkgName,
                                                     @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                                     @RequestParam PlatformType platform) {
        MobileApp mobileApp = mobileAppService.findMobileAppByPkgNameAndPlatformType(pkgName, platform);
        return mobileApp != null ? mobileApp.getVersionInfo() : null;
    }

}
