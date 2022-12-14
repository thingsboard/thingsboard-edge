/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.EmailTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;

@RestController
@RequestMapping("/api/auth/2fa")
@TbCoreComponent
@RequiredArgsConstructor
public class TwoFactorAuthController extends BaseController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final TwoFaConfigManager twoFaConfigManager;
    private final JwtTokenFactory tokenFactory;
    private final SystemSecurityService systemSecurityService;
    private final UserService userService;


    @ApiOperation(value = "Request 2FA verification code (requestTwoFaVerificationCode)",
            notes = "Request 2FA verification code." + NEW_LINE +
                    "To make a request to this endpoint, you need an access token with the scope of PRE_VERIFICATION_TOKEN, " +
                    "which is issued on username/password auth if 2FA is enabled." + NEW_LINE +
                    "The API method is rate limited (using rate limit config from TwoFactorAuthSettings). " +
                    "Will return a Bad Request error if provider is not configured for usage, " +
                    "and Too Many Requests error if rate limits are exceeded.")
    @PostMapping("/verification/send")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public void requestTwoFaVerificationCode(@RequestParam TwoFaProviderType providerType) throws Exception {
        SecurityUser user = getCurrentUser();
        twoFactorAuthService.prepareVerificationCode(user, providerType, true);
    }

    @ApiOperation(value = "Check 2FA verification code (checkTwoFaVerificationCode)",
            notes = "Checks 2FA verification code, and if it is correct the method returns a regular access and refresh token pair." + NEW_LINE +
                    "The API method is rate limited (using rate limit config from TwoFactorAuthSettings), and also will block a user " +
                    "after X unsuccessful verification attempts if such behavior is configured (in TwoFactorAuthSettings)." + NEW_LINE +
                    "Will return a Bad Request error if provider is not configured for usage, " +
                    "and Too Many Requests error if rate limits are exceeded.")
    @PostMapping("/verification/check")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public JwtPair checkTwoFaVerificationCode(@RequestParam TwoFaProviderType providerType,
                                              @RequestParam String verificationCode, HttpServletRequest servletRequest) throws Exception {
        SecurityUser user = getCurrentUser();
        boolean verificationSuccess = twoFactorAuthService.checkVerificationCode(user, providerType, verificationCode, true);
        if (verificationSuccess) {
            systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(servletRequest), ActionType.LOGIN, null);
            user = new SecurityUser(userService.findUserById(user.getTenantId(), user.getId()), true, user.getUserPrincipal(), getMergedUserPermissions(user, false));
            return tokenFactory.createTokenPair(user);
        } else {
            ThingsboardException error = new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(servletRequest), ActionType.LOGIN, error);
            throw error;
        }
    }


    @ApiOperation(value = "Get available 2FA providers (getAvailableTwoFaProviders)", notes =
            "Get the list of 2FA provider infos available for user to use. Example:\n" +
                    "```\n[\n" +
                    "  {\n    \"type\": \"EMAIL\",\n    \"default\": true,\n    \"contact\": \"ab*****ko@gmail.com\"\n  },\n" +
                    "  {\n    \"type\": \"TOTP\",\n    \"default\": false,\n    \"contact\": null\n  },\n" +
                    "  {\n    \"type\": \"SMS\",\n    \"default\": false,\n    \"contact\": \"+38********12\"\n  }\n" +
                    "]\n```")
    @GetMapping("/providers")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public List<TwoFaProviderInfo> getAvailableTwoFaProviders() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        Optional<PlatformTwoFaSettings> platformTwoFaSettings = twoFaConfigManager.getPlatformTwoFaSettings(user.getTenantId(), true);
        return twoFaConfigManager.getAccountTwoFaSettings(user.getTenantId(), user.getId())
                .map(settings -> settings.getConfigs().values()).orElse(Collections.emptyList())
                .stream().map(config -> {
                    String contact = null;
                    switch (config.getProviderType()) {
                        case SMS:
                            String phoneNumber = ((SmsTwoFaAccountConfig) config).getPhoneNumber();
                            contact = StringUtils.obfuscate(phoneNumber, 2, '*', phoneNumber.indexOf('+') + 1, phoneNumber.length());
                            break;
                        case EMAIL:
                            String email = ((EmailTwoFaAccountConfig) config).getEmail();
                            contact = StringUtils.obfuscate(email, 2, '*', 0, email.indexOf('@'));
                            break;
                    }
                    return TwoFaProviderInfo.builder()
                            .type(config.getProviderType())
                            .isDefault(config.isUseByDefault())
                            .contact(contact)
                            .minVerificationCodeSendPeriod(platformTwoFaSettings.get().getMinVerificationCodeSendPeriod())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class TwoFaProviderInfo {
        private TwoFaProviderType type;
        private boolean isDefault;
        private String contact;
        private Integer minVerificationCodeSendPeriod;
    }

}
