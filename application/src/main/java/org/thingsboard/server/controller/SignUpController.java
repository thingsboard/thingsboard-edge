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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.signup.SignUpRequest;
import org.thingsboard.server.common.data.signup.SignUpResult;
import org.thingsboard.server.config.SignUpConfig;
import org.thingsboard.server.dao.selfregistration.SelfRegistrationService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.data.RecaptchaValidationResult;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.utils.MiscUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class SignUpController extends BaseController {

    private static final String PRIVACY_POLICY_ACCEPTED = "privacyPolicyAccepted";

    private static final String TERMS_OF_USE_ACCEPTED = "termsOfUseAccepted";

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
    private SelfRegistrationService selfRegistrationService;

    @Autowired
    private SystemSecurityService systemSecurityService;

    @PostConstruct
    public void init() throws Exception {
        restTemplate = new RestTemplate();
    }

    @ApiOperation(value = "User Sign Up (signUp)",
            notes = "Process user sign up request. Creates the Customer and corresponding User based on self Registration parameters for the domain. " +
                    "See [Self Registration Controller](/swagger-ui.html#/self-registration-controller) for more details.  " +
                    "The result is either 'SUCCESS' or 'INACTIVE_USER_EXISTS'. " +
                    "If Success, the user will receive an email with instruction to activate the account. " +
                    "The content of the email is customizable via the mail templates.", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/noauth/signup", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public SignUpResult signUp(
            @ApiParam(value = "A JSON value representing the signup request.", required = true)
            @RequestBody SignUpRequest signUpRequest, HttpServletRequest request) throws ThingsboardException, IOException {
        SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                request.getServerName(), null);
        if (!StringUtils.isEmpty(signUpRequest.getPkgName())) {
            if (!signUpRequest.getPkgName().equals(selfRegistrationParams.getPkgName())) {
                throw new DataValidationException("Invalid Application Id!");
            }
            if (StringUtils.isEmpty(signUpRequest.getAppSecret())) {
                throw new DataValidationException("Invalid Application Secret!");
            }
            if (!signUpRequest.getAppSecret().equals(selfRegistrationParams.getAppSecret())) {
                throw new DataValidationException("Invalid Application Secret!");
            }
        }
        TenantId tenantId = selfRegistrationService.getTenantIdByDomainName(TenantId.SYS_TENANT_ID, request.getServerName());

        checkNotNull(signUpRequest);
        checkParameter("First name", signUpRequest.getFirstName());
        checkParameter("Last name", signUpRequest.getLastName());
        checkParameter("Email", signUpRequest.getEmail());
        checkParameter("Password", signUpRequest.getPassword());
        checkParameter("Recaptcha response", signUpRequest.getRecaptchaResponse());

        //Verify recaptcha response
        RecaptchaValidationResult result = validateReCaptcha(signUpRequest.getRecaptchaResponse(), request.getRemoteAddr(),
                selfRegistrationParams.getCaptchaSecretKey());
        if (result.isFailure()) {
            log.error("reCAPTCHA validation failed: {}", result);
            throw new DataValidationException("Invalid reCaptcha response!");
        }

        //Verify email
        DataValidator.validateEmail(signUpRequest.getEmail());
        User existingUser = userService.findUserByEmail(tenantId, signUpRequest.getEmail());
        if (existingUser != null) {
            UserCredentials credentials = userService.findUserCredentialsByUserId(tenantId, existingUser.getId());
            if (credentials.isEnabled()) {
                throw new DataValidationException("User with email '" + existingUser.getEmail() + "' "
                        + " is already registered!");
            } else {
                return SignUpResult.INACTIVE_USER_EXISTS;
            }
        }

        systemSecurityService.validatePassword(tenantId, signUpRequest.getPassword(), null);

        Customer customer = new Customer();

        customer.setTenantId(tenantId);
        customer.setTitle(signUpRequest.getEmail());
        customer.setOwnerId(tenantId);
        customer.setEmail(signUpRequest.getEmail());

        Customer savedCustomer = checkNotNull(customerService.saveCustomer(customer));

        User user = new User();
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setEmail(signUpRequest.getEmail());
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(tenantId);
        user.setCustomerId(savedCustomer.getId());
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("lang", "en_US");
        if (!StringUtils.isEmpty(selfRegistrationParams.getDefaultDashboardId())) {
            objectNode.put("defaultDashboardId", selfRegistrationParams.getDefaultDashboardId());
        }
        objectNode.put("defaultDashboardFullscreen", selfRegistrationParams.isDefaultDashboardFullscreen());
        user.setAdditionalInfo(objectNode);

        User savedUser = checkNotNull(userService.saveUser(user));

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
        userCredentials.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

        userService.saveUserCredentials(tenantId, userCredentials);

        try {
            sendEmailVerification(tenantId, request, userCredentials, signUpRequest.getEmail(), null, signUpRequest.getPkgName());
        } catch (ThingsboardException e) {
            customerService.deleteCustomer(tenantId, savedCustomer.getId());
            throw e;
        }
        sendUserActivityNotification(tenantId, signUpRequest.getFirstName() + " " + signUpRequest.getLastName(),
                signUpRequest.getEmail(), false, selfRegistrationParams.getNotificationEmail());

        notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedCustomer.getId(), savedCustomer,
                savedCustomer.getId(), ActionType.ADDED, null);
        notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, savedUser.getCustomerId(), savedUser.getId(),
                savedUser, null, ActionType.ADDED, true, null);
        notificationEntityService.notifyAddToEntityGroup(tenantId, savedUser.getId(), savedUser, savedCustomer.getId(),
                usersEntityGroup.getId(), null, usersEntityGroup.toString(), usersEntityGroup.getName());

        return SignUpResult.SUCCESS;
    }

    private void sendEmailVerification(TenantId tenantId, HttpServletRequest request, UserCredentials userCredentials, String targetEmail, String baseUrl, String pkgName) throws ThingsboardException, IOException {
        if (baseUrl == null) {
            baseUrl = MiscUtils.constructBaseUrl(request);
        }
        String activationLink = String.format("%s/api/noauth/activateEmail?emailCode=%s", baseUrl, userCredentials.getActivateToken());
        if (!StringUtils.isEmpty(pkgName)) {
            activationLink = String.format("%s&pkgName=%s", activationLink, pkgName);
        }
        mailService.sendActivationEmail(tenantId, activationLink, targetEmail);
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
            log.error("Failed to send notification email about user {}", action);
            log.error("Cause:", e);
        }
    }

    @ApiOperation(value = "Resend Activation Email (resendEmailActivation)",
            notes = "Request to resend the activation email for the user. Checks that user was not activated yet.", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/noauth/resendEmailActivation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void resendEmailActivation(
            @ApiParam(value = "Email of the user.", required = true, example = "john.doe@company.com")
            @RequestParam(value = "email") String email,
            @ApiParam(value = "Optional package name of the mobile application.")
            @RequestParam(required = false) String pkgName, HttpServletRequest request) throws ThingsboardException, IOException {
        SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                request.getServerName(), pkgName);
        if (!StringUtils.isEmpty(pkgName)) {
            if (!pkgName.equals(selfRegistrationParams.getPkgName())) {
                throw new DataValidationException("Invalid Application Id!");
            }
        }
        TenantId tenantId = selfRegistrationService.getTenantIdByDomainName(TenantId.SYS_TENANT_ID, request.getServerName());

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
                    "Checks that user was not activated yet.", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/noauth/activateEmail", params = {"emailCode"}, method = RequestMethod.GET)
    public ResponseEntity<String> activateEmail(
            @ApiParam(value = "Activation token.", required = true)
            @RequestParam(value = "emailCode") String emailCode,
            @ApiParam(value = "Optional package name of the mobile application.")
            @RequestParam(required = false) String pkgName,
            HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, emailCode);
        if (userCredentials != null) {
            String emailVerifiedURI = null;
            try {
                if (!StringUtils.isEmpty(pkgName)) {
                    SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                            request.getServerName(), pkgName);
                    if (!pkgName.equals(selfRegistrationParams.getPkgName())) {
                        throw new DataValidationException("Invalid Application Id!");
                    }
                    emailVerifiedURI = selfRegistrationParams.getAppScheme() + "://" + selfRegistrationParams.getAppHost() + "/signup/emailVerified";
                } else {
                    emailVerifiedURI = "/signup/emailVerified";
                }
                URI location = new URI(emailVerifiedURI + "?emailCode=" + emailCode);
                headers.setLocation(location);
                responseStatus = HttpStatus.PERMANENT_REDIRECT;
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", emailVerifiedURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }

    @ApiOperation(value = "Mobile Login redirect (mobileLogin)",
            notes = "This method generates redirect to the special link that is handled by mobile application. Useful for email verification flow on mobile app.", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/noauth/login", params = {"pkgName"}, method = RequestMethod.GET)
    public ResponseEntity<String> mobileLogin(
            @ApiParam(value = "Mobile app package name. Used to identify the application and build the redirect link.", required = true)
            @RequestParam(value = "pkgName") String pkgName,
            HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                request.getServerName(), pkgName);
        if (!pkgName.equals(selfRegistrationParams.getPkgName())) {
            throw new DataValidationException("Invalid Application Id!");
        }
        String redirectURI = selfRegistrationParams.getAppScheme() + "://" + selfRegistrationParams.getAppHost() + "/login";
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
                    "Checks that user was not activated yet.", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/noauth/activateByEmailCode", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JwtPair activateUserByEmailCode(
            @ApiParam(value = "Activation token.", required = true)
            @RequestParam(value = "emailCode") String emailCode,
            @ApiParam(value = "Optional package name of the mobile application.")
            @RequestParam(required = false) String pkgName,
            HttpServletRequest request) throws ThingsboardException {
        SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                request.getServerName(), pkgName);
        if (!StringUtils.isEmpty(pkgName)) {
            if (!pkgName.equals(selfRegistrationParams.getPkgName())) {
                throw new DataValidationException("Invalid Application Id!");
            }
        }
        TenantId tenantId = selfRegistrationService.getTenantIdByDomainName(TenantId.SYS_TENANT_ID, request.getServerName());

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
        user = userService.saveUser(user);
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

        mailService.sendAccountActivatedEmail(tenantId, loginUrl, email);

        sendUserActivityNotification(tenantId, user.getFirstName() + " " + user.getLastName(), email, true, selfRegistrationParams.getNotificationEmail());

        return tokenFactory.createTokenPair(securityUser);
    }

    private void setPrivacyPolicyAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (additionalInfo == null || !(additionalInfo instanceof ObjectNode)) {
            additionalInfo = objectMapper.createObjectNode();
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
            notes = "Checks that current user accepted the privacy policy.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/signup/privacyPolicyAccepted", method = RequestMethod.GET)
    public @ResponseBody Boolean privacyPolicyAccepted() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        return isPrivacyPolicyAccepted(user);
    }

    @ApiOperation(value = "Accept privacy policy (acceptPrivacyPolicy)",
            notes = "Accept privacy policy by the current user.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/signup/acceptPrivacyPolicy", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode acceptPrivacyPolicy() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        setPrivacyPolicyAccepted(user);
        user = userService.saveUser(user);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user, false));
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode tokenObject = objectMapper.createObjectNode();
        tokenObject.put("token", tokenPair.getToken());
        tokenObject.put("refreshToken", tokenPair.getRefreshToken());
        return tokenObject;
    }

    private void setTermsOfUseAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (additionalInfo == null || !(additionalInfo instanceof ObjectNode)) {
            additionalInfo = objectMapper.createObjectNode();
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
            notes = "Checks that current user accepted the privacy policy.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/signup/termsOfUseAccepted", method = RequestMethod.GET)
    public @ResponseBody
    Boolean termsOfUseAccepted() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        return isTermsOfUseAccepted(user);
    }

    @ApiOperation(value = "Accept Terms of Use (acceptTermsOfUse)",
            notes = "Accept Terms of Use by the current user.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/signup/acceptTermsOfUse", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode acceptTermsOfUse() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        setTermsOfUseAccepted(user);
        user = userService.saveUser(user);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user, false));
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode tokenObject = objectMapper.createObjectNode();
        tokenObject.put("token", tokenPair.getToken());
        tokenObject.put("refreshToken", tokenPair.getRefreshToken());
        return tokenObject;
    }

    private RecaptchaValidationResult validateReCaptcha(String userResponse, String ipAddress, String recaptchaSecretKey) throws ThingsboardException {
        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("secret", recaptchaSecretKey);
        parameters.add("response", userResponse);
        parameters.add("remoteip", ipAddress);

        log.debug("Validating reCAPTCHA:\n    verification url: {}\n    verification parameters: {}", signUpConfig.getRecaptchaVerificationUrl(), parameters);

        try {
            RecaptchaValidationResult result = restTemplate.postForEntity(signUpConfig.getRecaptchaVerificationUrl(), parameters, RecaptchaValidationResult.class).getBody();
            log.debug("reCAPTCHA validation finished: {}", result);
            return result;
        } catch (RestClientException ex) {
            log.error("Error validating reCAPTCHA. User response: [{}], verification URL: [{}]", userResponse, signUpConfig.getRecaptchaVerificationUrl());
            throw new ThingsboardException("Error validating reCAPTCHA", ex, ThingsboardErrorCode.GENERAL);
        }
    }

}
