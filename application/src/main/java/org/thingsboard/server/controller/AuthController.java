/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRepository;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api")
@Slf4j
public class AuthController extends BaseController {



    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenFactory tokenFactory;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MailService mailService;

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/auth/user", method = RequestMethod.GET)
    public @ResponseBody User getUser() throws ThingsboardException {
        try {
            SecurityUser securityUser = getCurrentUser();
            return userService.findUserById(securityUser.getId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/auth/changePassword", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void changePassword (
            @RequestBody JsonNode changePasswordRequest) throws ThingsboardException {
        try {
            String currentPassword = changePasswordRequest.get("currentPassword").asText();
            String newPassword = changePasswordRequest.get("newPassword").asText();
            SecurityUser securityUser = getCurrentUser();
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(securityUser.getId());
            if (!passwordEncoder.matches(currentPassword, userCredentials.getPassword())) {
                throw new ThingsboardException("Current password doesn't match!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            userCredentials.setPassword(passwordEncoder.encode(newPassword));
            userService.saveUserCredentials(userCredentials);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    @RequestMapping(value = "/noauth/activate", params = { "activateToken" }, method = RequestMethod.GET)
    public ResponseEntity<String> checkActivateToken(
            @RequestParam(value = "activateToken") String activateToken) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(activateToken);
        if (userCredentials != null) {
            String createURI = "/login/createPassword";
            try {
                URI location = new URI(createURI + "?activateToken=" + activateToken);
                headers.setLocation(location);
                responseStatus = HttpStatus.SEE_OTHER;
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", createURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }
    
    @RequestMapping(value = "/noauth/resetPasswordByEmail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void requestResetPasswordByEmail (
            @RequestBody JsonNode resetPasswordByEmailRequest,
            HttpServletRequest request) throws ThingsboardException {
        try {
            String email = resetPasswordByEmailRequest.get("email").asText();
            UserCredentials userCredentials = userService.requestPasswordReset(email);
            String baseUrl = constructBaseUrl(request);
            String resetUrl = String.format("%s/api/noauth/resetPassword?resetToken=%s", baseUrl,
                    userCredentials.getResetToken());
            User user = userService.findUserById(userCredentials.getUserId());
            mailService.sendResetPasswordEmail(user.getTenantId(), resetUrl, email);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    @RequestMapping(value = "/noauth/resetPassword", params = { "resetToken" }, method = RequestMethod.GET)
    public ResponseEntity<String> checkResetToken(
            @RequestParam(value = "resetToken") String resetToken) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        String resetURI = "/login/resetPassword";
        UserCredentials userCredentials = userService.findUserCredentialsByResetToken(resetToken);
        if (userCredentials != null) {
            try {
                URI location = new URI(resetURI + "?resetToken=" + resetToken);
                headers.setLocation(location);
                responseStatus = HttpStatus.SEE_OTHER;
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", resetURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }
    
    @RequestMapping(value = "/noauth/activate", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode activateUser(
            @RequestBody JsonNode activateRequest,
            HttpServletRequest request) throws ThingsboardException {
        try {
            String activateToken = activateRequest.get("activateToken").asText();
            String password = activateRequest.get("password").asText();
            String encodedPassword = passwordEncoder.encode(password);
            UserCredentials credentials = userService.activateUserCredentials(activateToken, encodedPassword);
            User user = userService.findUserById(credentials.getUserId());
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal);
            String baseUrl = constructBaseUrl(request);
            String loginUrl = String.format("%s/login", baseUrl);
            String email = user.getEmail();

            try {
                mailService.sendAccountActivatedEmail(user.getTenantId(), loginUrl, email);
            } catch (Exception e) {
                log.info("Unable to send account activation email [{}]", e.getMessage());
            }

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
    
    @RequestMapping(value = "/noauth/resetPassword", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode resetPassword(
            @RequestBody JsonNode resetPasswordRequest,
            HttpServletRequest request) throws ThingsboardException {
        try {
            String resetToken = resetPasswordRequest.get("resetToken").asText();
            String password = resetPasswordRequest.get("password").asText();
            UserCredentials userCredentials = userService.findUserCredentialsByResetToken(resetToken);
            if (userCredentials != null) {
                String encodedPassword = passwordEncoder.encode(password);
                userCredentials.setPassword(encodedPassword);
                userCredentials.setResetToken(null);
                userCredentials = userService.saveUserCredentials(userCredentials);
                User user = userService.findUserById(userCredentials.getUserId());
                UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
                SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), principal);
                String baseUrl = constructBaseUrl(request);
                String loginUrl = String.format("%s/login", baseUrl);
                String email = user.getEmail();
                mailService.sendPasswordWasResetEmail(user.getTenantId(), loginUrl, email);

                JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
                JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);

                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode tokenObject = objectMapper.createObjectNode();
                tokenObject.put("token", accessToken.getToken());
                tokenObject.put("refreshToken", refreshToken.getToken());
                return tokenObject;
            } else {
                throw new ThingsboardException("Invalid reset token!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
