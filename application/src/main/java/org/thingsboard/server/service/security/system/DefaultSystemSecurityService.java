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
package org.thingsboard.server.service.security.system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.user.UserServiceImpl;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.security.exception.UserPasswordExpiredException;
import org.thingsboard.server.utils.MiscUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.CacheConstants.SECURITY_SETTINGS_CACHE;

@Service
@Slf4j
public class DefaultSystemSecurityService implements SystemSecurityService {

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Resource
    private SystemSecurityService self;

    @Cacheable(cacheNames = SECURITY_SETTINGS_CACHE, key = "'securitySettings'")
    @Override
    public SecuritySettings getSecuritySettings(TenantId tenantId) {
        SecuritySettings securitySettings = null;
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(tenantId, "securitySettings");
        if (adminSettings != null) {
            try {
                securitySettings = JacksonUtil.convertValue(adminSettings.getJsonValue(), SecuritySettings.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load security settings!", e);
            }
        } else {
            securitySettings = new SecuritySettings();
            securitySettings.setPasswordPolicy(new UserPasswordPolicy());
            securitySettings.getPasswordPolicy().setMinimumLength(6);
        }
        return securitySettings;
    }

    @CacheEvict(cacheNames = SECURITY_SETTINGS_CACHE, key = "'securitySettings'")
    @Override
    public SecuritySettings saveSecuritySettings(TenantId tenantId, SecuritySettings securitySettings) {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(tenantId, "securitySettings");
        if (adminSettings == null) {
            adminSettings = new AdminSettings();
            adminSettings.setKey("securitySettings");
        }
        adminSettings.setJsonValue(JacksonUtil.valueToTree(securitySettings));
        AdminSettings savedAdminSettings = adminSettingsService.saveAdminSettings(tenantId, adminSettings);
        try {
            return JacksonUtil.convertValue(savedAdminSettings.getJsonValue(), SecuritySettings.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load security settings!", e);
        }
    }

    @Override
    public void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String username, String password) throws AuthenticationException {
        if (!encoder.matches(password, userCredentials.getPassword())) {
            int failedLoginAttempts = userService.onUserLoginIncorrectCredentials(tenantId, userCredentials.getUserId());
            SecuritySettings securitySettings = getSecuritySettings(tenantId);
            if (securitySettings.getMaxFailedLoginAttempts() != null && securitySettings.getMaxFailedLoginAttempts() > 0) {
                if (failedLoginAttempts > securitySettings.getMaxFailedLoginAttempts() && userCredentials.isEnabled()) {
                    userService.setUserCredentialsEnabled(TenantId.SYS_TENANT_ID, userCredentials.getUserId(), false);
                    if (StringUtils.isNoneBlank(securitySettings.getUserLockoutNotificationEmail())) {
                        try {
                            mailService.sendAccountLockoutEmail(tenantId, username, securitySettings.getUserLockoutNotificationEmail(), securitySettings.getMaxFailedLoginAttempts());
                        } catch (ThingsboardException e) {
                            log.warn("Can't send email regarding user account [{}] lockout to provided email [{}]", username, securitySettings.getUserLockoutNotificationEmail(), e);
                        }
                    }
                    throw new LockedException("Authentication Failed. Username was locked due to security policy.");
                }
            }
            throw new BadCredentialsException("Authentication Failed. Username or Password not valid.");
        }

        if (!userCredentials.isEnabled()) {
            throw new DisabledException("User is not active");
        }

        userService.onUserLoginSuccessful(tenantId, userCredentials.getUserId());

        SecuritySettings securitySettings = self.getSecuritySettings(tenantId);
        if (isPositiveInteger(securitySettings.getPasswordPolicy().getPasswordExpirationPeriodDays())) {
            if ((userCredentials.getCreatedTime()
                    + TimeUnit.DAYS.toMillis(securitySettings.getPasswordPolicy().getPasswordExpirationPeriodDays()))
                    < System.currentTimeMillis()) {
                userCredentials = userService.requestExpiredPasswordReset(tenantId, userCredentials.getId());
                throw new UserPasswordExpiredException("User password expired!", userCredentials.getResetToken());
            }
        }
    }

    @Override
    public void validatePassword(TenantId tenantId, String password, UserCredentials userCredentials) throws DataValidationException {
        SecuritySettings securitySettings = self.getSecuritySettings(tenantId);
        UserPasswordPolicy passwordPolicy = securitySettings.getPasswordPolicy();

        List<Rule> passwordRules = new ArrayList<>();
        passwordRules.add(new LengthRule(passwordPolicy.getMinimumLength(), Integer.MAX_VALUE));
        if (isPositiveInteger(passwordPolicy.getMinimumUppercaseLetters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.UpperCase, passwordPolicy.getMinimumUppercaseLetters()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumLowercaseLetters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.LowerCase, passwordPolicy.getMinimumLowercaseLetters()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumDigits())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.Digit, passwordPolicy.getMinimumDigits()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumSpecialCharacters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.Special, passwordPolicy.getMinimumSpecialCharacters()));
        }
        if (passwordPolicy.getAllowWhitespaces() != null && !passwordPolicy.getAllowWhitespaces()) {
            passwordRules.add(new WhitespaceRule());
        }
        PasswordValidator validator = new PasswordValidator(passwordRules);
        PasswordData passwordData = new PasswordData(password);
        RuleResult result = validator.validate(passwordData);
        if (!result.isValid()) {
            String message = String.join("\n", validator.getMessages(result));
            throw new DataValidationException(message);
        }

        if (userCredentials != null && isPositiveInteger(passwordPolicy.getPasswordReuseFrequencyDays())) {
            long passwordReuseFrequencyTs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(passwordPolicy.getPasswordReuseFrequencyDays());
            User user = userService.findUserById(tenantId, userCredentials.getUserId());
            JsonNode additionalInfo = user.getAdditionalInfo();
            if (additionalInfo instanceof ObjectNode && additionalInfo.has(UserServiceImpl.USER_PASSWORD_HISTORY)) {
                JsonNode userPasswordHistoryJson = additionalInfo.get(UserServiceImpl.USER_PASSWORD_HISTORY);
                Map<String, String> userPasswordHistoryMap = JacksonUtil.convertValue(userPasswordHistoryJson, new TypeReference<>() {});
                for (Map.Entry<String, String> entry : userPasswordHistoryMap.entrySet()) {
                    if (encoder.matches(password, entry.getValue()) && Long.parseLong(entry.getKey()) > passwordReuseFrequencyTs) {
                        throw new DataValidationException("Password was already used for the last " + passwordPolicy.getPasswordReuseFrequencyDays() + " days");
                    }
                }
            }
        }
    }

    @Override
    public String getBaseUrl(Authority authority, TenantId tenantId, CustomerId customerId, HttpServletRequest httpServletRequest) {
        String baseUrl;
        LoginWhiteLabelingParams loginWhiteLabelingParams = null;
        if (Authority.CUSTOMER_USER.equals(authority)) {
            try {
                loginWhiteLabelingParams = whiteLabelingService.getCustomerLoginWhiteLabelingParams(tenantId, customerId);
            } catch (Exception e) {
                log.warn("Failed to fetch CustomerLoginWhiteLabelingParams.");
            }
        }

        if ((!isBaseUrlSet(loginWhiteLabelingParams) && Authority.CUSTOMER_USER.equals(authority)) || Authority.TENANT_ADMIN.equals(authority)) {
            try {
                loginWhiteLabelingParams = whiteLabelingService.getTenantLoginWhiteLabelingParams(tenantId);
            } catch (Exception e) {
                log.warn("Failed to fetch TenantLoginWhiteLabelingParams.");
            }
        }

        if (!isBaseUrlSet(loginWhiteLabelingParams)) {
            try {
                loginWhiteLabelingParams = whiteLabelingService.getSystemLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID);
            } catch (Exception e) {
                log.warn("Failed to fetch TenantLoginWhiteLabelingParams.");
            }
        }

        if (isBaseUrlSet(loginWhiteLabelingParams) && loginWhiteLabelingParams.isProhibitDifferentUrl()) {
            baseUrl = loginWhiteLabelingParams.getBaseUrl();
        } else {
            return getBaseUrl(tenantId, customerId, httpServletRequest);
        }
        return baseUrl;
    }

    private boolean isBaseUrlSet(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        return loginWhiteLabelingParams != null && StringUtils.isNoneEmpty(loginWhiteLabelingParams.getBaseUrl());
    }

    @Override
    public String getBaseUrl(TenantId tenantId, CustomerId customerId, HttpServletRequest httpServletRequest) {
        String baseUrl = null;
        AdminSettings generalSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "general");

        JsonNode prohibitDifferentUrl = generalSettings.getJsonValue().get("prohibitDifferentUrl");

        if (prohibitDifferentUrl != null && prohibitDifferentUrl.asBoolean()) {
            baseUrl = generalSettings.getJsonValue().get("baseUrl").asText();
        }

        if (StringUtils.isEmpty(baseUrl)) {
            baseUrl = MiscUtils.constructBaseUrl(httpServletRequest);
        }

        return baseUrl;
    }

    private static boolean isPositiveInteger(Integer val) {
        return val != null && val > 0;
    }
}
