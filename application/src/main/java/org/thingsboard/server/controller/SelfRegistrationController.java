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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundlePolicyInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.WebSelfRegistrationParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.ControllerConstants.WL_READ_CHECK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class SelfRegistrationController extends BaseController {

    private static final String PRIVACY_POLICY = "privacyPolicy";
    private static final String TERMS_OF_USE = "termsOfUse";
    private static final String SELF_REGISTRATION_DESC = "Self Registration allows users to signup for using the platform and automatically create a Customer account for them. " +
            "You may configure default dashboard and user roles that will be assigned for this Customer. " +
            "This allows you to build out-of-the-box solutions for customers. " +
            "Ability to white-label the login and main pages helps to brand the platform.";

    @ApiOperation(value = "Create Or Update Self Registration parameters (saveSelfRegistrationParams)",
            notes = "Creates or Updates the Self Registration parameters. When creating, platform generates Admin Settings Id as " + UUID_WIKI_LINK +
                    "The newly created Admin Settings Id will be present in the response. " +
                    "Specify existing Admin Settings Id to update the Self Registration parameters. " +
                    "Referencing non-existing Admin Settings Id will cause 'Not Found' error." +
                    "\n\n" + SELF_REGISTRATION_DESC +
                    TENANT_AUTHORITY_PARAGRAPH + ControllerConstants.WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/selfRegistration/selfRegistrationParams")
    public WebSelfRegistrationParams saveWebSelfRegistrationParams(
            @Parameter(description = "A JSON value representing the Self Registration Parameters.", required = true)
            @RequestBody WebSelfRegistrationParams selfRegistrationParams) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        Authority authority = securityUser.getAuthority();
        checkSelfRegistrationPermissions(Operation.WRITE);
        WebSelfRegistrationParams savedSelfRegistrationParams = null;
        if (Authority.TENANT_ADMIN.equals(authority)) {
            savedSelfRegistrationParams = whiteLabelingService.saveTenantSelfRegistrationParams(getTenantId(), selfRegistrationParams);
            JsonNode privacyPolicyNode = whiteLabelingService.getTenantPrivacyPolicy(securityUser.getTenantId());
            if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
                savedSelfRegistrationParams.setPrivacyPolicy(privacyPolicyNode.get(PRIVACY_POLICY).asText());
            }
            JsonNode termsOfUseNode = whiteLabelingService.getTenantTermsOfUse(securityUser.getTenantId());
            if (termsOfUseNode != null && termsOfUseNode.has(TERMS_OF_USE)) {
                savedSelfRegistrationParams.setTermsOfUse(termsOfUseNode.get(TERMS_OF_USE).asText());
            }
        }
        return savedSelfRegistrationParams;
    }

    @ApiOperation(value = "Get Self Registration parameters (getSelfRegistrationParams)",
            notes = "Fetch the Self Registration parameters object for the tenant of the current user. "
                    + TENANT_AUTHORITY_PARAGRAPH + WL_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/selfRegistration/selfRegistrationParams")
    public SelfRegistrationParams getWebSelfRegistrationParams() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        checkSelfRegistrationPermissions(Operation.READ);
        WebSelfRegistrationParams selfRegistrationParams = null;
        if (Authority.TENANT_ADMIN.equals(securityUser.getAuthority())) {
            selfRegistrationParams = whiteLabelingService.getTenantSelfRegistrationParams(securityUser.getTenantId());
            JsonNode privacyPolicyNode = whiteLabelingService.getTenantPrivacyPolicy(securityUser.getTenantId());
            if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
                selfRegistrationParams.setPrivacyPolicy(privacyPolicyNode.get(PRIVACY_POLICY).asText());
            }
            JsonNode termsOfUseNode = whiteLabelingService.getTenantTermsOfUse(securityUser.getTenantId());
            if (termsOfUseNode != null && termsOfUseNode.has(TERMS_OF_USE)) {
                selfRegistrationParams.setTermsOfUse(termsOfUseNode.get(TERMS_OF_USE).asText());
            }
        }
        return selfRegistrationParams;
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/selfRegistration/selfRegistrationParams")
    public void deleteWebSelfRegistrationParams() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        checkSelfRegistrationPermissions(Operation.WRITE);
        whiteLabelingService.deleteWhiteLabeling(currentUser.getTenantId(), currentUser.getCustomerId(), WhiteLabelingType.SELF_REGISTRATION);
    }

    @ApiOperation(value = "Get Self Registration form parameters without authentication (getSignUpSelfRegistrationParams)",
            notes = "Fetch the Self Registration parameters based on the domain name from the request. Available for non-authorized users. " +
                    "Contains the information to customize the sign-up form.")
    @GetMapping(value = "/noauth/selfRegistration/signUpSelfRegistrationParams")
    public SignUpSelfRegistrationParams getSignUpSelfRegistrationParams(
            @RequestParam(required = false) String pkgName,
            @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
            @RequestParam(required = false) PlatformType platform,
            HttpServletRequest request) throws ThingsboardException {
        SelfRegistrationParams selfRegistrationParams;
        if (!StringUtils.isEmpty(pkgName)) {
            checkNotNull(platform, "Platform type is required if package name is specified");
            MobileAppBundle appBundle = mobileAppBundleService.findMobileAppBundleByPkgNameAndPlatform(TenantId.SYS_TENANT_ID, pkgName, platform);
            selfRegistrationParams = appBundle != null ? appBundle.getSelfRegistrationParams() : null;
        } else {
            selfRegistrationParams = whiteLabelingService.getSelfRegistrationParamsByDomain(request.getServerName());
        }
        return selfRegistrationParams != null ? new SignUpSelfRegistrationParams(selfRegistrationParams.getTitle(), selfRegistrationParams.getCaptcha(), selfRegistrationParams.getSignUpFields(),
                selfRegistrationParams.getShowPrivacyPolicy(), selfRegistrationParams.getShowTermsOfUse()) : null;
    }

    @ApiOperation(value = "Get Privacy Policy for Self Registration form (getPrivacyPolicy)",
            notes = "Fetch the Privacy Policy based on the domain name from the request. Available for non-authorized users. ")
    @RequestMapping(value = "/noauth/selfRegistration/privacyPolicy", method = RequestMethod.GET)
    @ResponseBody
    public String getPrivacyPolicy(@RequestParam(required = false) String pkgName,
                                   @RequestParam(required = false) PlatformType platform,
                                   HttpServletRequest request) throws ThingsboardException {
        JsonNode privacyPolicyNode;
        if (!StringUtils.isEmpty(pkgName)) {
            checkNotNull(platform, "Platform type is required if package name is specified");
            MobileAppBundlePolicyInfo policyInfo = mobileAppBundleService.findMobileAppBundlePolicyInfoByPkgNameAndPlatform(TenantId.SYS_TENANT_ID, pkgName, platform);
            privacyPolicyNode = JacksonUtil.toJsonNode(policyInfo != null ? policyInfo.getPrivacyPolicy() : null);
        } else {
            privacyPolicyNode = whiteLabelingService.getPrivacyPolicyByDomainName(request.getServerName());
        }
        if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
            return privacyPolicyNode.get(PRIVACY_POLICY).toString();
        }
        return "";
    }

    @ApiOperation(value = "Get Terms of Use for Self Registration form (getTermsOfUse)",
            notes = "Fetch the Terms of Use based on the domain name from the request. Available for non-authorized users. ")
    @RequestMapping(value = "/noauth/selfRegistration/termsOfUse", method = RequestMethod.GET)
    @ResponseBody
    public String getTermsOfUse(@RequestParam(required = false) String pkgName,
                                @RequestParam(required = false) PlatformType platform,
                                HttpServletRequest request) throws ThingsboardException {
        JsonNode termsOfUseNode;
        if (!StringUtils.isEmpty(pkgName)) {
            checkNotNull(platform, "Platform type is required if package name is specified");
            MobileAppBundlePolicyInfo termsOfUseInfo = mobileAppBundleService.findMobileAppBundlePolicyInfoByPkgNameAndPlatform(TenantId.SYS_TENANT_ID, pkgName, platform);
            termsOfUseNode = JacksonUtil.toJsonNode(termsOfUseInfo != null ? termsOfUseInfo.getTermsOfUse() : null);
        } else {
            termsOfUseNode = whiteLabelingService.getTermsOfUseByDomainName(request.getServerName());
        }
        if (termsOfUseNode != null && termsOfUseNode.has(TERMS_OF_USE)) {
            return termsOfUseNode.get(TERMS_OF_USE).toString();
        }
        return "";
    }

    private void checkSelfRegistrationPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
