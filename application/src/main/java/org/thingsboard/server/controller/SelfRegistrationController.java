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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.selfregistration.SelfRegistrationService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.ControllerConstants.WL_READ_CHECK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class SelfRegistrationController extends BaseController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PRIVACY_POLICY = "privacyPolicy";
    private static final String TERMS_OF_USE = "termsOfUse";
    private static final String DOMAIN_NAME = "domainName";
    private static final String SELF_REGISTRATION_DESC = "Self Registration allows users to signup for using the platform and automatically create a Customer account for them. " +
            "You may configure default dashboard and user roles that will be assigned for this Customer. " +
            "This allows you to build out-of-the-box solutions for customers. " +
            "Ability to white-label the login and main pages helps to brand the platform.";

    @Autowired
    private SelfRegistrationService selfRegistrationService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AttributesService attributesService;

    @ApiOperation(value = "Create Or Update Self Registration parameters (saveSelfRegistrationParams)",
            notes = "Creates or Updates the Self Registration parameters. When creating, platform generates Admin Settings Id as " + UUID_WIKI_LINK +
                    "The newly created Admin Settings Id will be present in the response. " +
                    "Specify existing Admin Settings Id to update the Self Registration parameters. " +
                    "Referencing non-existing Admin Settings Id will cause 'Not Found' error." +
                    "\n\n" + SELF_REGISTRATION_DESC +
                    TENANT_AUTHORITY_PARAGRAPH + ControllerConstants.WL_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/selfRegistration/selfRegistrationParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public SelfRegistrationParams saveSelfRegistrationParams(
            @ApiParam(value = "A JSON value representing the Self Registration Parameters.", required = true)
            @RequestBody SelfRegistrationParams selfRegistrationParams) throws ThingsboardException, JsonProcessingException {
        SecurityUser securityUser = getCurrentUser();
        Authority authority = securityUser.getAuthority();
        checkSelfRegistrationPermissions(Operation.WRITE);
        SelfRegistrationParams savedSelfRegistrationParams = null;
        if (Authority.TENANT_ADMIN.equals(authority)) {
            savedSelfRegistrationParams = selfRegistrationService.saveTenantSelfRegistrationParams(getTenantId(), selfRegistrationParams);
            JsonNode privacyPolicyNode = MAPPER.readTree(selfRegistrationService.getTenantPrivacyPolicy(securityUser.getTenantId()));
            if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
                savedSelfRegistrationParams.setPrivacyPolicy(privacyPolicyNode.get(PRIVACY_POLICY).asText());
            }
            JsonNode termsOfUseNode = MAPPER.readTree(selfRegistrationService.getTenantTermsOfUse(securityUser.getTenantId()));
            if (termsOfUseNode != null && termsOfUseNode.has(TERMS_OF_USE)) {
                savedSelfRegistrationParams.setTermsOfUse(termsOfUseNode.get(TERMS_OF_USE).asText());
            }
        }
        return savedSelfRegistrationParams;
    }

    @ApiOperation(value = "Get Self Registration parameters (getSelfRegistrationParams)",
            notes = "Fetch the Self Registration parameters object for the tenant of the current user. "
                    + TENANT_AUTHORITY_PARAGRAPH + WL_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/selfRegistration/selfRegistrationParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SelfRegistrationParams getSelfRegistrationParams() throws ThingsboardException, JsonProcessingException {
        SecurityUser securityUser = getCurrentUser();
        checkSelfRegistrationPermissions(Operation.READ);
        SelfRegistrationParams selfRegistrationParams = null;
        if (Authority.TENANT_ADMIN.equals(securityUser.getAuthority())) {
            selfRegistrationParams = selfRegistrationService.getTenantSelfRegistrationParams(securityUser.getTenantId());
            JsonNode privacyPolicyNode = MAPPER.readTree(selfRegistrationService.getTenantPrivacyPolicy(securityUser.getTenantId()));
            if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
                selfRegistrationParams.setPrivacyPolicy(privacyPolicyNode.get(PRIVACY_POLICY).asText());
            }
            JsonNode termsOfUseNode = MAPPER.readTree(selfRegistrationService.getTenantTermsOfUse(securityUser.getTenantId()));
            if (termsOfUseNode != null && termsOfUseNode.has(TERMS_OF_USE)) {
                selfRegistrationParams.setTermsOfUse(termsOfUseNode.get(TERMS_OF_USE).asText());
            }
        }
        return selfRegistrationParams;
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/selfRegistration/selfRegistrationParams/{domainName}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> deleteSelfRegistrationParams(@PathVariable(DOMAIN_NAME) String domainName) throws ThingsboardException {
        checkParameter(DOMAIN_NAME, domainName);
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        SecurityUser securityUser = getCurrentUser();
        Authority authority = securityUser.getAuthority();
        accessControlService.checkPermission(securityUser, Resource.TENANT, Operation.WRITE_ATTRIBUTES);
        if (Authority.TENANT_ADMIN.equals(authority)) {
            ListenableFuture<List<String>> future = attributesService.removeAll(
                    securityUser.getTenantId(),
                    securityUser.getTenantId(),
                    DataConstants.SERVER_SCOPE,
                    Arrays.asList("selfRegistrationParams", "termsOfUse", "privacyPolicy"));
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable List<String> keys) {
                    adminSettingsService.deleteAdminSettingsByKey(
                            securityUser.getTenantId(),
                            DataConstants.SELF_REGISTRATION_DOMAIN_NAME_PREFIX + domainName);
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
                }

                @Override
                public void onFailure(Throwable throwable) {
                    responseWriter.setErrorResult(throwable);
                }
            }, MoreExecutors.directExecutor());
        }
        return responseWriter;
    }

    @ApiOperation(value = "Get Self Registration form parameters without authentication (getSignUpSelfRegistrationParams)",
            notes = "Fetch the Self Registration parameters based on the domain name from the request. Available for non-authorized users. " +
                    "Contains the information to customize the sign-up form."
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/noauth/selfRegistration/signUpSelfRegistrationParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SignUpSelfRegistrationParams getSignUpSelfRegistrationParams(
            @RequestParam(required = false) String pkgName,
            HttpServletRequest request) throws ThingsboardException {
        SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                request.getServerName(), pkgName);

        SignUpSelfRegistrationParams result = new SignUpSelfRegistrationParams();
        result.setSignUpTextMessage(selfRegistrationParams.getSignUpTextMessage());
        result.setCaptchaSiteKey(selfRegistrationParams.getCaptchaSiteKey());
        result.setCaptchaVersion(selfRegistrationParams.getCaptchaVersion());
        result.setCaptchaAction(selfRegistrationParams.getCaptchaAction());
        result.setShowPrivacyPolicy(selfRegistrationParams.getShowPrivacyPolicy());
        result.setShowTermsOfUse(selfRegistrationParams.getShowTermsOfUse());

        return result;
    }

    @ApiOperation(value = "Get Privacy Policy for Self Registration form (getPrivacyPolicy)",
            notes = "Fetch the Privacy Policy based on the domain name from the request. Available for non-authorized users. ")
    @RequestMapping(value = "/noauth/selfRegistration/privacyPolicy", method = RequestMethod.GET)
    @ResponseBody
    public String getPrivacyPolicy(HttpServletRequest request) throws ThingsboardException, JsonProcessingException {
        JsonNode privacyPolicyNode = MAPPER.readTree(selfRegistrationService.getPrivacyPolicy(TenantId.SYS_TENANT_ID,
                request.getServerName()));
        if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
            return privacyPolicyNode.get(PRIVACY_POLICY).toString();
        }
        return "";
    }

    @ApiOperation(value = "Get Terms of Use for Self Registration form (getTermsOfUse)",
            notes = "Fetch the Terms of Use based on the domain name from the request. Available for non-authorized users. ")
    @RequestMapping(value = "/noauth/selfRegistration/termsOfUse", method = RequestMethod.GET)
    @ResponseBody
    public String getTermsOfUse(HttpServletRequest request) throws ThingsboardException, JsonProcessingException {
        JsonNode termsOfUse = MAPPER.readTree(selfRegistrationService.getTermsOfUse(TenantId.SYS_TENANT_ID,
                request.getServerName()));
        if (termsOfUse != null && termsOfUse.has(TERMS_OF_USE)) {
            return termsOfUse.get(TERMS_OF_USE).toString();
        }
        return "";
    }

    private void checkSelfRegistrationPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
