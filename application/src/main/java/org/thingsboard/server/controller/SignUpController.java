/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
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
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.config.SignUpConfig;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.selfregistration.SelfRegistrationService;
import org.thingsboard.server.data.RecaptchaValidationResult;
import org.thingsboard.server.data.SignUpRequest;
import org.thingsboard.server.data.SignUpResult;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRepository;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class SignUpController extends BaseController {

    private static final String PRIVACY_POLICY_ACCEPTED = "privacyPolicyAccepted";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static EmailValidator emailValidator = EmailValidator.getInstance();

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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SelfRegistrationService selfRegistrationService;

    @Autowired
    private SystemSecurityService systemSecurityService;

    @PostConstruct
    public void init() throws Exception {
        restTemplate = new RestTemplate();
    }

    @RequestMapping(value = "/noauth/signup", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public SignUpResult signUp(@RequestBody SignUpRequest signUpRequest,
                               HttpServletRequest request) throws ThingsboardException {
        try {
            SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                    request.getServerName());
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
            if (!emailValidator.isValid(signUpRequest.getEmail())) {
                throw new DataValidationException("Invalid email address format '" + signUpRequest.getEmail() + "'!");
            }
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
            if (selfRegistrationParams.getDefaultDashboardId() != null || !StringUtils.isEmpty(selfRegistrationParams.getDefaultDashboardId())) {
                objectNode.put("defaultDashboardId", selfRegistrationParams.getDefaultDashboardId());
            }
            objectNode.put("defaultDashboardFullscreen", selfRegistrationParams.isDefaultDashboardFullscreen());
            user.setAdditionalInfo(objectNode);

            User savedUser = checkNotNull(userService.saveUser(user));

            List<GroupPermission> permissions = selfRegistrationParams.getPermissions();

            EntityGroup users = entityGroupService.findOrCreateUserGroup(tenantId, savedCustomer.getId(), "Self Registration Users",
                    "Autogenerated Self Registration group");
            entityGroupService.addEntityToEntityGroup(tenantId, users.getId(), savedUser.getId());

            for (GroupPermission permission : permissions) {
                permission.setTenantId(tenantId);
                permission.setUserGroupId(users.getId());
                if (permission.getEntityGroupId() != null) {
                    EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, permission.getEntityGroupId());
                    permission.setEntityGroupType(entityGroup.getType());
                }
                groupPermissionService.saveGroupPermission(tenantId, permission);
            }

            UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
            userCredentials.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

            userService.saveUserCredentials(tenantId, userCredentials);

            try {
                sendEmailVerification(tenantId, request, userCredentials, signUpRequest.getEmail());
            } catch (ThingsboardException e) {
                customerService.deleteCustomer(tenantId, savedCustomer.getId());
                throw e;
            }
            sendUserActivityNotification(tenantId, signUpRequest.getFirstName() + " " + signUpRequest.getLastName(),
                    signUpRequest.getEmail(), false, selfRegistrationParams.getNotificationEmail());
            return SignUpResult.SUCCESS;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void sendEmailVerification(TenantId tenantId, HttpServletRequest request, UserCredentials userCredentials, String targetEmail) throws ThingsboardException, IOException {
        String baseUrl = constructBaseUrl(request);
        String activationLink = String.format("%s/api/noauth/activateEmail?emailCode=%s", baseUrl, userCredentials.getActivateToken());
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

    @RequestMapping(value = "/noauth/resendEmailActivation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void resendEmailActivation(@RequestParam(value = "email") String email,
                                      HttpServletRequest request) throws ThingsboardException {
        try {
            SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                    request.getServerName());
            TenantId tenantId = selfRegistrationService.getTenantIdByDomainName(TenantId.SYS_TENANT_ID, request.getServerName());

            User existingUser = userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);
            if (existingUser != null) {
                UserCredentials credentials = userService.findUserCredentialsByUserId(existingUser.getTenantId(), existingUser.getId());
                if (credentials.isEnabled()) {
                    throw new DataValidationException("User with email '" + existingUser.getEmail() + "' "
                            + " is already active!");
                } else {
                    sendEmailVerification(tenantId, request, credentials, email);
                }
            } else {
                throw new DataValidationException("User with email '" + email + "' "
                        + " is not registered!");
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @RequestMapping(value = "/noauth/activateEmail", params = {"emailCode"}, method = RequestMethod.GET)
    public ResponseEntity<String> activateEmail(
            @RequestParam(value = "emailCode") String emailCode) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, emailCode);
        if (userCredentials != null) {
            String emailVerifiedURI = "/signup/emailVerified";
            try {
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

    @RequestMapping(value = "/noauth/activateByEmailCode", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode activateUserByEmailCode(
            @RequestParam(value = "emailCode") String emailCode,
            HttpServletRequest request) throws ThingsboardException {
        try {
            SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                    request.getServerName());
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
            user = userService.saveUser(user);
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal, getMergedUserPermissions(user, false));
            String baseUrl = constructBaseUrl(request);
            String loginUrl = String.format("%s/login", baseUrl);
            String email = user.getEmail();

            try {
                mailService.sendAccountActivatedEmail(tenantId, loginUrl, email);
            } catch (ThingsboardException e) {
                throw handleException(e);
            }

            sendUserActivityNotification(tenantId, user.getFirstName() + " " + user.getLastName(), email, true, selfRegistrationParams.getNotificationEmail());

            JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
            JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode tokenObject = objectMapper.createObjectNode();
            tokenObject.put("token", accessToken.getToken());
            tokenObject.put("refreshToken", refreshToken.getToken());
            return tokenObject;
        } catch (Exception e) {
            throw handleException(e);
        }
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

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/signup/privacyPolicyAccepted", method = RequestMethod.GET)
    public @ResponseBody
    Boolean privacyPolicyAccepted() throws ThingsboardException {
        try {
            SecurityUser securityUser = getCurrentUser();
            User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
            return isPrivacyPolicyAccepted(user);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/signup/acceptPrivacyPolicy", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode acceptPrivacyPolicy() throws ThingsboardException {
        try {
            SecurityUser securityUser = getCurrentUser();
            User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
            setPrivacyPolicyAccepted(user);
            user = userService.saveUser(user);
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user, false));
            JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
            JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode tokenObject = objectMapper.createObjectNode();
            tokenObject.put("token", accessToken.getToken());
            tokenObject.put("refreshToken", refreshToken.getToken());
            return tokenObject;
        } catch (Exception e) {
            throw handleException(e);
        }
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
