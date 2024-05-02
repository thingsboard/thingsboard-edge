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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.AndroidConfig;
import org.thingsboard.server.common.data.mobile.IosConfig;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.mobile.MobileAppSettingsService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.qr.MobileAppSecretService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.net.URI;
import java.net.URISyntaxException;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
public class MobileApplicationController extends BaseController {

    @Value("${cache.specs.mobileSecretKey.timeToLiveInMinutes:2}")
    private int mobileSecretKeyTtl;

    public static final String ASSET_LINKS_PATTERN = "[{\n" +
            "  \"relation\": [\"delegate_permission/common.handle_all_urls\"],\n" +
            "  \"target\": {\n" +
            "    \"namespace\": \"android_app\",\n" +
            "    \"package_name\": \"%s\",\n" +
            "    \"sha256_cert_fingerprints\":\n" +
            "    [\"%s\"]\n" +
            "  }\n" +
            "}]";

    public static final String APPLE_APP_SITE_ASSOCIATION_PATTERN = "{\n" +
            "    \"applinks\": {\n" +
            "        \"apps\": [],\n" +
            "        \"details\": [\n" +
            "            {\n" +
            "                \"appID\": \"%s\",\n" +
            "                \"paths\": [ \"/api/noauth/qr\" ]\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";

    public static final String ANDROID_APPLICATION_STORE_LINK = "https://play.google.com/store/apps/details?id=org.thingsboard.demo.app";
    public static final String APPLE_APPLICATION_STORE_LINK = "https://apps.apple.com/us/app/thingsboard-live/id1594355695";
    public static final String SECRET = "secret";
    public static final String SECRET_PARAM_DESCRIPTION = "A string value representing short-live secret key";
    public static final String DEFAULT_APP_DOMAIN = "thingsboard.cloud";
    public static final String DEEP_LINK_PATTERN = "https://%s/api/noauth/qr?secret=%s&ttl=%s";

    private final SystemSecurityService systemSecurityService;
    private final MobileAppSecretService mobileAppSecretService;
    private final MobileAppSettingsService mobileAppSettingsService;
    private final WhiteLabelingService whiteLabelingService;

    @ApiOperation(value = "Get associated android applications (getAssetLinks)")
    @GetMapping(value = "/.well-known/assetlinks.json")
    public ResponseEntity<JsonNode> getAssetLinks(HttpServletRequest request) {
        String domainName = request.getServerName();
        WhiteLabeling loginWL = whiteLabelingService.findByDomainName(domainName);
        MobileAppSettings mobileAppSettings;
        if (loginWL != null) {
            mobileAppSettings = mobileAppSettingsService.getCurrentMobileAppSettings(loginWL.getTenantId());
        } else {
            mobileAppSettings = mobileAppSettingsService.getCurrentMobileAppSettings(TenantId.SYS_TENANT_ID);
        }
        AndroidConfig androidConfig = mobileAppSettings.getAndroidConfig();
        if (androidConfig != null && androidConfig.isEnabled() && !androidConfig.getAppPackage().isBlank() && !androidConfig.getSha256CertFingerprints().isBlank()) {
            return ResponseEntity.ok(JacksonUtil.toJsonNode(String.format(ASSET_LINKS_PATTERN, androidConfig.getAppPackage(), androidConfig.getSha256CertFingerprints())));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "Get associated ios applications (getAppleAppSiteAssociation)")
    @GetMapping(value = "/.well-known/apple-app-site-association")
    public ResponseEntity<JsonNode> getAppleAppSiteAssociation(HttpServletRequest request) {
        String domainName = request.getServerName();
        WhiteLabeling loginWL = whiteLabelingService.findByDomainName(domainName);
        MobileAppSettings mobileAppSettings;
        if (loginWL != null) {
            mobileAppSettings = mobileAppSettingsService.getCurrentMobileAppSettings(loginWL.getTenantId());
        } else {
            mobileAppSettings = mobileAppSettingsService.getCurrentMobileAppSettings(TenantId.SYS_TENANT_ID);
        }
        IosConfig iosConfig = mobileAppSettings.getIosConfig();
        if (iosConfig != null && iosConfig.isEnabled() && !iosConfig.getAppId().isBlank()) {
            return ResponseEntity.ok(JacksonUtil.toJsonNode(String.format(APPLE_APP_SITE_ASSOCIATION_PATTERN, iosConfig.getAppId())));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "Create Or Update the Mobile application settings (saveMobileAppSettings)",
            notes = "The request payload contains configuration for android/iOS applications and platform qr code widget settings." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/api/mobile/app/settings")
    public MobileAppSettings saveMobileAppSettings(@Parameter(description = "A JSON value representing the mobile apps configuration")
                                                   @RequestBody MobileAppSettings mobileAppSettings) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(getCurrentUser(), Resource.MOBILE_APP_SETTINGS, Operation.WRITE);
        mobileAppSettings.setTenantId(getTenantId());
        return mobileAppSettingsService.saveMobileAppSettings(currentUser.getTenantId(), mobileAppSettings);
    }

    @ApiOperation(value = "Get Mobile application settings (getMobileAppSettings)",
            notes = "The response payload contains configuration for android/iOS applications and platform qr code widget settings." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/api/mobile/app/settings")
    public MobileAppSettings getMobileAppSettings() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(getCurrentUser(), Resource.MOBILE_APP_SETTINGS, Operation.READ);
        return mobileAppSettingsService.getCurrentMobileAppSettings(currentUser.getTenantId());
    }

    @ApiOperation(value = "Get QR code configuration for home page (getMobileAppQrCodeConfig)",
            notes = "The response payload contains ui configuration of qr code" + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/api/mobile/qr/config")
    public QRCodeConfig getMobileAppQrCodeConfig() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(getCurrentUser(), Resource.MOBILE_APP_SETTINGS, Operation.READ);
        return mobileAppSettingsService.getMobileAppSettings(currentUser.getTenantId()).getQrCodeConfig();
    }

    @ApiOperation(value = "Get the deep link to the associated mobile application (getMobileAppDeepLink)",
            notes = "Fetch the url that takes user to linked mobile application " + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/api/mobile/deepLink", produces = "text/plain")
    public String getMobileAppDeepLink(HttpServletRequest request) throws ThingsboardException, URISyntaxException {
        SecurityUser currentUser = getCurrentUser();
        String secret = mobileAppSecretService.generateMobileAppSecret(getCurrentUser());
        String baseUrl = systemSecurityService.getBaseUrl(currentUser.getAuthority(), currentUser.getTenantId(), currentUser.getCustomerId(), request);
        String platformDomain = new URI(baseUrl).getHost();
        MobileAppSettings mobileAppSettings = mobileAppSettingsService.getMobileAppSettings(currentUser.getTenantId());
        String appDomain;
        if (!mobileAppSettings.isUseDefaultApp()) {
            appDomain = platformDomain;
        } else {
            appDomain = DEFAULT_APP_DOMAIN;
        }
        String deepLink = String.format(DEEP_LINK_PATTERN, appDomain, secret, mobileSecretKeyTtl);
        if (!appDomain.equals(platformDomain)) {
            deepLink = deepLink + "&host=" + baseUrl;
        }
        return "\"" + deepLink + "\"";
    }

    @ApiOperation(value = "Get User Token (getUserTokenByMobileSecret)",
            notes = "Returns the token of the User based on the provided secret key.")
    @GetMapping(value = "/api/noauth/qr/{secret}")
    public JwtPair getUserTokenByMobileSecret(@Parameter(description = SECRET_PARAM_DESCRIPTION)
                                @PathVariable(SECRET) String secret) throws ThingsboardException {
        checkParameter(SECRET, secret);
        return mobileAppSecretService.getJwtPair(secret);
    }

    @GetMapping(value = "/api/noauth/qr")
    public ResponseEntity<?> getApplicationRedirect(@RequestHeader(value = "User-Agent") String userAgent) {
        if (userAgent.contains("Android")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", ANDROID_APPLICATION_STORE_LINK)
                    .build();
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", APPLE_APPLICATION_STORE_LINK)
                    .build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .build();
        }
    }
}
