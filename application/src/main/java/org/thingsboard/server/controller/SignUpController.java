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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.selfregistration.MobileRedirectParams;
import org.thingsboard.server.common.data.selfregistration.MobileSelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SignUpField;
import org.thingsboard.server.common.data.selfregistration.SignUpFieldId;
import org.thingsboard.server.common.data.selfregistration.WebSelfRegistrationParams;
import org.thingsboard.server.common.data.signup.SignUpRequest;
import org.thingsboard.server.common.data.signup.SignUpResult;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.config.SignUpConfig;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.data.RecaptchaValidationResult;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.utils.MiscUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.ADDRESS;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.ADDRESS2;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.CITY;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.COUNTRY;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.EMAIL;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.FIRST_NAME;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.LAST_NAME;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.PASSWORD;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.PHONE;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.STATE;
import static org.thingsboard.server.common.data.selfregistration.SignUpFieldId.ZIP;
import static org.thingsboard.server.common.data.wl.WhiteLabelingType.SELF_REGISTRATION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class SignUpController extends BaseController {

    private static final String MOBILE_PLATFORM_IS_REQUIRED = "Mobile platform type is required for mobile signup";

    private static final String SELF_REGISTRATION_SETTINGS_WAS_NOT_FOUND = "Self registration settings was not found";

    private static final String WL_SETTINGS_WAS_NOT_FOUND = "White labeling settings was not found";

    private static final String INVALID_APP_SECRET = "Invalid Application Secret!";

    private static final String MOBILE_APP_BUNDLE_WAS_NOT_FOUND = "Mobile app bundle was not found";

    private static final String PRIVACY_POLICY_ACCEPTED = "privacyPolicyAccepted";

    private static final String TERMS_OF_USE_ACCEPTED = "termsOfUseAccepted";

    private RestTemplate restTemplate;

    @Autowired
    private SignUpConfig signUpConfig;

    @Autowired
    private MailService mailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenFactory tokenFactory;

    @Autowired
    private SystemSecurityService systemSecurityService;

    @PostConstruct
    public void init() throws Exception {
        restTemplate = new RestTemplate();
    }

    @RequestMapping(value = "/noauth/signup/recaptchaPublicKey", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public String getRecaptchaPublicKey() {
        return "\"\"";
    }

    @ApiOperation(value = "User Sign Up (signUp)",
            notes = "Process user sign up request. Creates the Customer and corresponding User based on self Registration parameters for the domain. " +
                    "See [Self Registration Controller](/swagger-ui.html#/self-registration-controller) for more details.  " +
                    "The result is either 'SUCCESS' or 'INACTIVE_USER_EXISTS'. " +
                    "If Success, the user will receive an email with instruction to activate the account. " +
                    "The content of the email is customizable via the mail templates.")
    @RequestMapping(value = "/noauth/signup", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public SignUpResult signUp(
            @Parameter(description = "A JSON value representing the signup request.", required = true)
            @RequestBody SignUpRequest signUpRequest, HttpServletRequest request) throws ThingsboardException, IOException {
        SelfRegistrationParams selfRegistrationParams;
        TenantId tenantId;
        if (!StringUtils.isEmpty(signUpRequest.getPkgName())) {
            validateAppSecret(signUpRequest);
            MobileAppBundle mobileAppBundle = findMobileAppBundle(signUpRequest.getPkgName(), signUpRequest.getPlatform());
            selfRegistrationParams = mobileAppBundle.getSelfRegistrationParams();
            tenantId = mobileAppBundle.getTenantId();
        } else {
            WhiteLabeling whiteLabeling = findSelfRegistrationWL(request.getServerName());
            selfRegistrationParams = JacksonUtil.treeToValue(whiteLabeling.getSettings(), WebSelfRegistrationParams.class);
            tenantId = whiteLabeling.getTenantId();
        }

        //validate required fields
        validateRequiredFields(signUpRequest, selfRegistrationParams);

        //Verify recaptcha response
        validateReCaptcha(signUpRequest.getRecaptchaResponse(), request.getRemoteAddr(),
                selfRegistrationParams.getCaptcha().getSecretKey());

        //Verify email
        DataValidator.validateEmail(signUpRequest.getFields().get(EMAIL));
        User existingUser = userService.findUserByEmail(tenantId, signUpRequest.getFields().get(EMAIL));
        if (existingUser != null) {
            UserCredentials credentials = userService.findUserCredentialsByUserId(tenantId, existingUser.getId());
            if (credentials.isEnabled()) {
                throw new DataValidationException("User with email '" + existingUser.getEmail() + "' "
                        + " is already registered!");
            } else {
                return SignUpResult.INACTIVE_USER_EXISTS;
            }
        }

        systemSecurityService.validatePassword(signUpRequest.getFields().get(PASSWORD), null);

        Customer customer = new Customer();

        customer.setTenantId(tenantId);
        String customerTitlePrefix = Optional.ofNullable(selfRegistrationParams.getCustomerTitlePrefix()).orElse("");
        customer.setTitle(customerTitlePrefix + signUpRequest.getFields().get(EMAIL));
        customer.setOwnerId(tenantId);
        customer.setEmail(signUpRequest.getFields().get(EMAIL));
        customer.setCountry(signUpRequest.getFields().get(COUNTRY));
        customer.setState(signUpRequest.getFields().get(STATE));
        customer.setCity(signUpRequest.getFields().get(CITY));
        customer.setAddress(signUpRequest.getFields().get(ADDRESS));
        customer.setAddress2(signUpRequest.getFields().get(ADDRESS2));
        customer.setZip(signUpRequest.getFields().get(ZIP));
        customer.setPhone(signUpRequest.getFields().get(PHONE));
        customer.setCustomMenuId(selfRegistrationParams.getCustomMenuId());

        Customer savedCustomer = checkNotNull(customerService.saveCustomer(customer));
        if (selfRegistrationParams.getCustomerGroupId() != null) {
            entityGroupService.addEntityToEntityGroup(tenantId, selfRegistrationParams.getCustomerGroupId(), savedCustomer.getId());
        }

        User user = new User();
        user.setFirstName(signUpRequest.getFields().get(FIRST_NAME));
        user.setLastName(signUpRequest.getFields().get(LAST_NAME));
        user.setEmail(signUpRequest.getFields().get(EMAIL));
        user.setPhone(signUpRequest.getFields().get(PHONE));
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(tenantId);
        user.setCustomerId(savedCustomer.getId());
        ObjectNode objectNode = JacksonUtil.newObjectNode();
        objectNode.put("lang", "en_US");

        Optional.ofNullable(selfRegistrationParams.getDefaultDashboard())
                .ifPresent(dashboard -> {
                    objectNode.put("defaultDashboardId", dashboard.getId());
                    objectNode.put("defaultDashboardFullscreen", dashboard.isFullscreen());
                });
        user.setAdditionalInfo(objectNode);

        User savedUser = checkNotNull(userService.saveUser(tenantId, user));

        List<GroupPermission> permissions = selfRegistrationParams.getPermissions();

        EntityGroup usersEntityGroup = entityGroupService.findOrCreateUserGroup(tenantId, savedCustomer.getId(), "Self Registration Users",
                "Autogenerated Self Registration group");
        entityGroupService.addEntityToEntityGroup(tenantId, usersEntityGroup.getId(), savedUser.getId());

        for (GroupPermission permission : permissions) {
            permission.setTenantId(tenantId);
            permission.setUserGroupId(usersEntityGroup.getId());
            if (permission.getEntityGroupId() != null) {
                EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, permission.getEntityGroupId());
                if (entityGroup != null) {
                    permission.setEntityGroupType(entityGroup.getType());
                    groupPermissionService.saveGroupPermission(tenantId, permission);
                }
            } else {
                groupPermissionService.saveGroupPermission(tenantId, permission);
            }
        }

        UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
        userCredentials.setPassword(passwordEncoder.encode(signUpRequest.getFields().get(PASSWORD)));

        userService.saveUserCredentials(tenantId, userCredentials);

        try {
            sendEmailVerification(tenantId, request, userCredentials, signUpRequest.getFields().get(EMAIL), null, signUpRequest.getPkgName());
        } catch (ThingsboardException e) {
            customerService.deleteCustomer(tenantId, savedCustomer.getId());
            throw e;
        }
        sendUserActivityNotification(tenantId, signUpRequest.getFields().get(FIRST_NAME) + " " + signUpRequest.getFields().get(LAST_NAME),
                signUpRequest.getFields().get(EMAIL), false, selfRegistrationParams.getNotificationEmail());

        logEntityActionService.logEntityAction(tenantId, savedCustomer.getId(), savedCustomer, savedCustomer.getId(),
                ActionType.ADDED, null);
        logEntityActionService.logEntityAction(tenantId, savedUser.getId(), savedUser, savedUser.getCustomerId(),
                ActionType.ADDED, null);
        logEntityActionService.logEntityAction(tenantId, savedUser.getId(), savedUser, savedCustomer.getId(),
                ActionType.ADDED_TO_ENTITY_GROUP, null, usersEntityGroup.toString(), usersEntityGroup.getName());

        return SignUpResult.SUCCESS;
    }

    private void sendEmailVerification(TenantId tenantId, HttpServletRequest request, UserCredentials userCredentials, String targetEmail, String baseUrl, String pkgName) throws ThingsboardException {
        if (baseUrl == null) {
            baseUrl = MiscUtils.constructBaseUrl(request);
        }
        String activationLink = String.format("%s/api/noauth/activateEmail?emailCode=%s", baseUrl, userCredentials.getActivateToken());
        if (!StringUtils.isEmpty(pkgName)) {
            activationLink = String.format("%s&pkgName=%s", activationLink, pkgName);
        }
        try {
            mailService.sendActivationEmail(tenantId, activationLink, userCredentials.getActivationTokenTtl(), targetEmail);
        } catch (Exception e) {
            throw new ThingsboardException("Temporarily unable to send activation email", ThingsboardErrorCode.GENERAL);
        }
    }

    private void sendUserActivityNotification(TenantId tenantId, String userFullName, String userEmail, boolean activated, String infoMail) {
        try {
            if (activated) {
                mailService.sendUserActivatedEmail(tenantId, userFullName, userEmail, infoMail);
            } else {
                mailService.sendUserRegisteredEmail(tenantId, userFullName, userEmail, infoMail);
            }
        } catch (ThingsboardException e) {
            String action = activated ? "activation" : "registration";
            log.error("Failed to send notification email about user {}", action, e);
        }
    }

    @ApiOperation(value = "Resend Activation Email (resendEmailActivation)",
            notes = "Request to resend the activation email for the user. Checks that user was not activated yet.")
    @RequestMapping(value = "/noauth/resendEmailActivation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void resendEmailActivation(
            @Parameter(description = "Email of the user.", required = true, example = "john.doe@company.com")
            @RequestParam(value = "email") String email,
            @Parameter(description = "Optional package name of the mobile application.")
            @RequestParam(required = false) String pkgName,
            @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
            @RequestParam(required = false) PlatformType platform,
            HttpServletRequest request) throws ThingsboardException, IOException {
        TenantId tenantId;
        if (!StringUtils.isEmpty(pkgName)) {
            MobileAppBundle mobileAppBundle = findMobileAppBundle(pkgName, platform);
            tenantId = mobileAppBundle.getTenantId();
        } else {
            WhiteLabeling whiteLabeling = findSelfRegistrationWL(request.getServerName());
            tenantId = whiteLabeling.getTenantId();
        }

        User existingUser = userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);
        if (existingUser != null) {
            UserCredentials credentials = userService.findUserCredentialsByUserId(existingUser.getTenantId(), existingUser.getId());
            if (credentials.isEnabled()) {
                throw new DataValidationException("User with email '" + existingUser.getEmail() + "' "
                        + " is already active!");
            } else {
                sendEmailVerification(tenantId, request, credentials, email, null, pkgName);
            }
        } else {
            throw new DataValidationException("User with email '" + email + "' "
                    + " is not registered!");
        }
    }

    @ApiOperation(value = "Activate User using code from Email (activateEmail)",
            notes = "Activate the user using code(link) from the activation email. " +
                    "Validates the code an redirects according to the signup flow. " +
                    "Checks that user was not activated yet.")
    @RequestMapping(value = "/noauth/activateEmail", params = {"emailCode"}, method = RequestMethod.GET)
    public ResponseEntity<String> activateEmail(
            @Parameter(description = "Activation token.", required = true)
            @RequestParam(value = "emailCode") String emailCode,
            @Parameter(description = "Optional package name of the mobile application.")
            @RequestParam(required = false) String pkgName,
            @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
            @RequestParam(required = false) PlatformType platform,
            HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, emailCode);
        if (userCredentials != null) {
            String emailVerifiedURI = null;
            try {
                if (!StringUtils.isEmpty(pkgName)) {
                    MobileAppBundle mobileAppBundle = findMobileAppBundle(pkgName, platform);
                    MobileRedirectParams redirect = mobileAppBundle.getSelfRegistrationParams().getRedirect();
                    emailVerifiedURI = redirect.getScheme() + "://" + redirect.getHost() + "/signup/emailVerified";
                } else {
                    emailVerifiedURI = "/signup/emailVerified";
                }
                URI location = new URI(emailVerifiedURI + "?emailCode=" + emailCode);
                headers.setLocation(location);
                responseStatus = HttpStatus.PERMANENT_REDIRECT;
            } catch (URISyntaxException | ThingsboardException e) {
                log.error("Unable to create URI with address [{}]", emailVerifiedURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }

    @ApiOperation(value = "Mobile Login redirect (mobileLogin)",
            notes = "This method generates redirect to the special link that is handled by mobile application. Useful for email verification flow on mobile app.")
    @RequestMapping(value = "/noauth/login", params = {"pkgName, platform"}, method = RequestMethod.GET)
    public ResponseEntity<String> mobileLogin(
            @Parameter(description = "Mobile app package name. Used to identify the application and build the redirect link.", required = true)
            @RequestParam(value = "pkgName") String pkgName,
            @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}), required = true)
            @RequestParam PlatformType platform,
            HttpServletRequest request) throws ThingsboardException {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        MobileAppBundle mobileAppBundle = findMobileAppBundle(pkgName, platform);
        MobileSelfRegistrationParams selfRegistrationParams = mobileAppBundle.getSelfRegistrationParams();
        String redirectURI = selfRegistrationParams.getRedirect().getScheme() + "://" + selfRegistrationParams.getRedirect().getHost() + "/login";
        try {
            URI location = new URI(redirectURI);
            headers.setLocation(location);
            responseStatus = HttpStatus.PERMANENT_REDIRECT;
        } catch (URISyntaxException e) {
            log.error("Unable to create URI with address [{}]", redirectURI);
            responseStatus = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }

    @ApiOperation(value = "Activate and login using code from Email (activateUserByEmailCode)",
            notes = "Activate the user using code(link) from the activation email and return the JWT Token. " +
                    "Sends the notification and email about user activation. " +
                    "Checks that user was not activated yet.")
    @RequestMapping(value = "/noauth/activateByEmailCode", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JwtPair activateUserByEmailCode(
            @Parameter(description = "Activation token.", required = true)
            @RequestParam(value = "emailCode") String emailCode,
            @Parameter(description = "Optional package name of the mobile application.")
            @RequestParam(required = false) String pkgName,
            @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
            @RequestParam(required = false) PlatformType platform,
            HttpServletRequest request) throws ThingsboardException {
        SelfRegistrationParams selfRegistrationParams;
        TenantId tenantId;
        if (!StringUtils.isEmpty(pkgName)) {
            MobileAppBundle mobileAppBundle = findMobileAppBundle(pkgName, platform);
            selfRegistrationParams = mobileAppBundle.getSelfRegistrationParams();
            tenantId = mobileAppBundle.getTenantId();
        } else {
            WhiteLabeling whiteLabeling = findSelfRegistrationWL(request.getServerName());
            selfRegistrationParams = JacksonUtil.treeToValue(whiteLabeling.getSettings(), WebSelfRegistrationParams.class);
            tenantId = whiteLabeling.getTenantId();
        }

        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, emailCode);
        if (userCredentials == null) {
            throw new ThingsboardException("Invalid email code!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (userCredentials.getPassword() == null) {
            throw new ThingsboardException("Unable to activate user!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        String encodedPassword = userCredentials.getPassword();
        UserCredentials credentials = userService.activateUserCredentials(TenantId.SYS_TENANT_ID, emailCode, encodedPassword);
        User user = userService.findUserById(TenantId.SYS_TENANT_ID, credentials.getUserId());
        setPrivacyPolicyAccepted(user);
        setTermsOfUseAccepted(user);
        user = userService.saveUser(tenantId, user);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal, getMergedUserPermissions(user, false));
        String baseUrl = MiscUtils.constructBaseUrl(request);
        String loginUrl;
        if (!StringUtils.isEmpty(pkgName)) {
            loginUrl = String.format("%s/api/noauth/login?pkgName=%s", baseUrl, pkgName);
        } else {
            loginUrl = String.format("%s/login", baseUrl);
        }
        String email = user.getEmail();

        try {
            mailService.sendAccountActivatedEmail(tenantId, loginUrl, email);
        } catch (Exception e) {
            log.warn("Unable to send account activated email for {}: {}", email, e.getMessage());
        }

        sendUserActivityNotification(tenantId, user.getFirstName() + " " + user.getLastName(), email, true, selfRegistrationParams.getNotificationEmail());

        systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(request), ActionType.LOGIN, null);
        return tokenFactory.createTokenPair(securityUser);
    }

    private void setPrivacyPolicyAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(PRIVACY_POLICY_ACCEPTED, true);
        user.setAdditionalInfo(additionalInfo);
    }

    private boolean isPrivacyPolicyAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (additionalInfo != null && additionalInfo.has(PRIVACY_POLICY_ACCEPTED)) {
            return additionalInfo.get(PRIVACY_POLICY_ACCEPTED).asBoolean();
        }
        return false;
    }

    @ApiOperation(value = "Check privacy policy (privacyPolicyAccepted)",
            notes = "Checks that current user accepted the privacy policy.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/signup/privacyPolicyAccepted", method = RequestMethod.GET)
    public @ResponseBody
    Boolean privacyPolicyAccepted() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        return isPrivacyPolicyAccepted(user);
    }

    @ApiOperation(value = "Accept privacy policy (acceptPrivacyPolicy)",
            notes = "Accept privacy policy by the current user.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/signup/acceptPrivacyPolicy", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode acceptPrivacyPolicy() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        setPrivacyPolicyAccepted(user);
        user = userService.saveUser(securityUser.getTenantId(), user);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user, false));
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);

        ObjectNode tokenObject = JacksonUtil.newObjectNode();
        tokenObject.put("token", tokenPair.getToken());
        tokenObject.put("refreshToken", tokenPair.getRefreshToken());
        return tokenObject;
    }

    private void setTermsOfUseAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (additionalInfo == null || !(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(TERMS_OF_USE_ACCEPTED, true);
        user.setAdditionalInfo(additionalInfo);
    }

    private boolean isTermsOfUseAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (additionalInfo != null && additionalInfo.has(TERMS_OF_USE_ACCEPTED)) {
            return additionalInfo.get(TERMS_OF_USE_ACCEPTED).asBoolean();
        }
        return false;
    }

    @ApiOperation(value = "Check Terms Of User (termsOfUseAccepted)",
            notes = "Checks that current user accepted the privacy policy.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/signup/termsOfUseAccepted", method = RequestMethod.GET)
    public @ResponseBody
    Boolean termsOfUseAccepted() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        return isTermsOfUseAccepted(user);
    }

    @ApiOperation(value = "Accept Terms of Use (acceptTermsOfUse)",
            notes = "Accept Terms of Use by the current user.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/signup/acceptTermsOfUse", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode acceptTermsOfUse() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        setTermsOfUseAccepted(user);
        user = userService.saveUser(securityUser.getTenantId(), user);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user, false));
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);

        ObjectNode tokenObject = JacksonUtil.newObjectNode();
        tokenObject.put("token", tokenPair.getToken());
        tokenObject.put("refreshToken", tokenPair.getRefreshToken());
        return tokenObject;
    }

    private void validateReCaptcha(String userResponse, String ipAddress, String recaptchaSecretKey) throws ThingsboardException {
        checkParameter("Recaptcha response", userResponse);
        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("secret", recaptchaSecretKey);
        parameters.add("response", userResponse);
        parameters.add("remoteip", ipAddress);

        log.debug("Validating reCAPTCHA:\n    verification url: {}\n    verification parameters: {}", signUpConfig.getRecaptchaVerificationUrl(), parameters);

        try {
            RecaptchaValidationResult result = restTemplate.postForEntity(signUpConfig.getRecaptchaVerificationUrl(), parameters, RecaptchaValidationResult.class).getBody();
            log.debug("reCAPTCHA validation finished: {}", result);
            if (result.isFailure()) {
                log.error("reCAPTCHA validation failed: {}", result);
                throw new DataValidationException("Invalid reCaptcha response!");
            }
        } catch (RestClientException ex) {
            log.error("Error validating reCAPTCHA. User response: [{}], verification URL: [{}]", userResponse, signUpConfig.getRecaptchaVerificationUrl());
            throw new ThingsboardException("Error validating reCAPTCHA", ex, ThingsboardErrorCode.GENERAL);
        }
    }

    private void validateRequiredFields(SignUpRequest signUpRequest, SelfRegistrationParams selfRegistrationParams) {
        Map<SignUpFieldId, String> signUpRequestFields = signUpRequest.getFields();
        for (SignUpField field : selfRegistrationParams.getSignUpFields()) {
            if (field.isRequired() && StringUtils.isEmpty(signUpRequestFields.get(field.getId()))) {
                    throw new DataValidationException(field.getLabel() + " is required");
            }
        }
    }

    private void validateAppSecret(SignUpRequest signUpRequest) throws ThingsboardException {
        checkNotNull(signUpRequest.getPlatform(), MOBILE_PLATFORM_IS_REQUIRED);
        MobileApp mobileApp = mobileAppService.findMobileAppByPkgNameAndPlatformType(signUpRequest.getPkgName(), signUpRequest.getPlatform());
        if (StringUtils.isEmpty(signUpRequest.getAppSecret()) || !signUpRequest.getAppSecret().equals(mobileApp.getAppSecret())) {
            throw new DataValidationException(INVALID_APP_SECRET);
        }
    }

    private MobileAppBundle findMobileAppBundle(String pkgName, PlatformType platform) throws ThingsboardException {
        checkNotNull(platform, MOBILE_PLATFORM_IS_REQUIRED);
        MobileAppBundle mobileAppBundle = mobileAppBundleService.findMobileAppBundleByPkgNameAndPlatform(TenantId.SYS_TENANT_ID, pkgName, platform);
        checkNotNull(mobileAppBundle, MOBILE_APP_BUNDLE_WAS_NOT_FOUND);
        checkNotNull(mobileAppBundle.getSelfRegistrationParams(), SELF_REGISTRATION_SETTINGS_WAS_NOT_FOUND);
        return mobileAppBundle;
    }

    private WhiteLabeling findSelfRegistrationWL(String domain) throws ThingsboardException {
        WhiteLabeling whiteLabeling = whiteLabelingService.findWhiteLabelingByDomainAndType(domain, SELF_REGISTRATION);
        checkNotNull(whiteLabeling, WL_SETTINGS_WAS_NOT_FOUND);
        checkNotNull(whiteLabeling.getSettings(), SELF_REGISTRATION_SETTINGS_WAS_NOT_FOUND);
        return whiteLabeling;
    }

}
