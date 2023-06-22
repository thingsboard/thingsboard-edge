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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.LicenseUsageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.RepositorySettingsInfo;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.common.data.sync.vc.VcUtils;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.mail.MailTemplates;
import org.thingsboard.server.service.security.auth.oauth2.CookieUtils;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.sync.vc.autocommit.TbAutoCommitSettingsService;
import org.thingsboard.server.service.system.SystemInfoService;
import org.thingsboard.server.service.update.UpdateService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController extends BaseController {

    private final AttributesService attributesService;
    private final MailService mailService;
    private final SmsService smsService;
    private final AdminSettingsService adminSettingsService;
    private final SystemSecurityService systemSecurityService;
    @Lazy
    private final JwtSettingsService jwtSettingsService;
    @Lazy
    private final JwtTokenFactory tokenFactory;
    private final EntitiesVersionControlService versionControlService;
    private final TbAutoCommitSettingsService autoCommitSettingsService;
    private final UpdateService updateService;
    private final SystemInfoService systemInfoService;
    private final AuditLogService auditLogService;

    private static final String PREV_URI_PATH_PARAMETER = "prevUri";
    private static final String PREV_URI_COOKIE_NAME = "prev_uri";
    private static final String STATE_COOKIE_NAME = "state";
    private static final String MAIL_SETTINGS_KEY = "mail";
    private static final ConcurrentMap<String, TenantId> internalSessionMap = new ConcurrentHashMap<>();

    protected static final String RESOURCE_READ_CHECK = "\n\nSecurity check is performed to verify that " +
            "the user has 'READ' permission for the 'ADMIN_SETTINGS' (for 'SYS_ADMIN' authority) or 'WHITE_LABELING' (for 'TENANT_ADMIN' authority) resource.";
    protected static final String RESOURCE_WRITE_CHECK = "\n\nSecurity check is performed to verify that " +
            "the user has 'WRITE' permission for the 'ADMIN_SETTINGS' (for 'SYS_ADMIN' authority) or 'WHITE_LABELING' (for 'TENANT_ADMIN' authority) resource.";

    @ApiOperation(value = "Get the Administration Settings object using key (getAdminSettings)",
            notes = "Get the Administration Settings object using specified string key. " +
                    "Referencing non-existing key will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + RESOURCE_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/settings/{key}", method = RequestMethod.GET)
    @ResponseBody
    public AdminSettings getAdminSettings(
            @ApiParam(value = "A string value of the key (e.g. 'general' or 'mail').")
            @PathVariable("key") String key,
            @ApiParam(value = "Use system settings if settings are not defined on tenant level.")
            @RequestParam(required = false, defaultValue = "false") boolean systemByDefault) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        AdminSettings adminSettings;
        if (Authority.SYS_ADMIN.equals(authority)) {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key), "No Administration settings found for key: " + key);
        } else {
            accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
            adminSettings =  getTenantAdminSettings(getTenantId(), key, systemByDefault);
            if (adminSettings.getKey().equals("mailTemplates")) {
                ((ObjectNode) adminSettings.getJsonValue()).remove(MailTemplates.API_USAGE_STATE_ENABLED);
                ((ObjectNode) adminSettings.getJsonValue()).remove(MailTemplates.API_USAGE_STATE_WARNING);
                ((ObjectNode) adminSettings.getJsonValue()).remove(MailTemplates.API_USAGE_STATE_DISABLED);
            }
        }
        if (adminSettings.getKey().equals("mail")) {
            ((ObjectNode) adminSettings.getJsonValue()).remove("password");
            ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
        }
        return adminSettings;
    }

    @ApiOperation(value = "Get the Administration Settings object using key (getAdminSettings)",
            notes = "Creates or Updates the Administration Settings. Platform generates random Administration Settings Id during settings creation. " +
                    "The Administration Settings Id will be present in the response. Specify the Administration Settings Id when you would like to update the Administration Settings. " +
                    "Referencing non-existing Administration Settings Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + RESOURCE_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/settings", method = RequestMethod.POST)
    @ResponseBody
    public AdminSettings saveAdminSettings(
            @ApiParam(value = "A JSON value representing the Administration Settings.")
            @RequestBody AdminSettings adminSettings) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        adminSettings.setTenantId(getTenantId());
        if (Authority.SYS_ADMIN.equals(authority)) {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
            adminSettings = checkNotNull(adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings));
        } else {
            accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE);
            adminSettings = saveTenantAdminSettings(getTenantId(), adminSettings);
        }
        if (adminSettings.getKey().equals("mail")) {
            ((ObjectNode) adminSettings.getJsonValue()).remove("password");
            ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
        }
        return adminSettings;
    }

    @ApiOperation(value = "Get the Security Settings object",
            notes = "Get the Security Settings object that contains password policy, etc." + SYSTEM_AUTHORITY_PARAGRAPH + RESOURCE_READ_CHECK)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/securitySettings", method = RequestMethod.GET)
    @ResponseBody
    public SecuritySettings getSecuritySettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return checkNotNull(systemSecurityService.getSecuritySettings(TenantId.SYS_TENANT_ID));
    }

    @ApiOperation(value = "Update Security Settings (saveSecuritySettings)",
            notes = "Updates the Security Settings object that contains password policy, etc." + SYSTEM_AUTHORITY_PARAGRAPH + RESOURCE_WRITE_CHECK)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/securitySettings", method = RequestMethod.POST)
    @ResponseBody
    public SecuritySettings saveSecuritySettings(
            @ApiParam(value = "A JSON value representing the Security Settings.")
            @RequestBody SecuritySettings securitySettings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
        securitySettings = checkNotNull(systemSecurityService.saveSecuritySettings(TenantId.SYS_TENANT_ID, securitySettings));
        return securitySettings;
    }

    @ApiOperation(value = "Get the JWT Settings object (getJwtSettings)",
            notes = "Get the JWT Settings object that contains JWT token policy, etc. " + SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/jwtSettings", method = RequestMethod.GET)
    @ResponseBody
    public JwtSettings getJwtSettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return checkNotNull(jwtSettingsService.getJwtSettings());
    }

    @ApiOperation(value = "Update JWT Settings (saveJwtSettings)",
            notes = "Updates the JWT Settings object that contains JWT token policy, etc. The tokenSigningKey field is a Base64 encoded string." + SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/jwtSettings", method = RequestMethod.POST)
    @ResponseBody
    public JwtPair saveJwtSettings(
            @ApiParam(value = "A JSON value representing the JWT Settings.")
            @RequestBody JwtSettings jwtSettings) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        accessControlService.checkPermission(securityUser, Resource.ADMIN_SETTINGS, Operation.WRITE);
        checkNotNull(jwtSettingsService.saveJwtSettings(jwtSettings));
        return tokenFactory.createTokenPair(securityUser);
    }

    @ApiOperation(value = "Send test email (sendTestMail)",
            notes = "Attempts to send test email using Mail Settings provided as a parameter. " +
                    "Email is sent to the address specified in the profile of user who is performing the request" +
                    "You may change the 'To' email in the user profile of the System/Tenant Administrator. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + RESOURCE_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/settings/testMail", method = RequestMethod.POST)
    public void sendTestMail(
            @ApiParam(value = "A JSON value representing the Mail Settings.")
            @RequestBody AdminSettings adminSettings) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        if (Authority.SYS_ADMIN.equals(authority)) {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        } else {
            accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
        }
        adminSettings = checkNotNull(adminSettings);
        if (adminSettings.getKey().equals("mail")) {
            AdminSettings mailSettings;
            if (Authority.SYS_ADMIN.equals(authority)) {
                mailSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail"));
            } else {
                mailSettings = getTenantAdminSettings(getTenantId(), "mail", false);
            }
            if (adminSettings.getJsonValue().has("enableOauth2") && adminSettings.getJsonValue().get("enableOauth2").asBoolean()){
                JsonNode refreshToken = mailSettings.getJsonValue().get("refreshToken");
                if (refreshToken == null) {
                    throw new ThingsboardException("Refresh token was not generated. Please, generate refresh token.", ThingsboardErrorCode.GENERAL);
                }
                ((ObjectNode) adminSettings.getJsonValue()).put("refreshToken", refreshToken.asText());
            }
            else {
                if (!adminSettings.getJsonValue().has("password")) {
                    ((ObjectNode) adminSettings.getJsonValue()).put("password", mailSettings.getJsonValue().get("password").asText());
                }
            }
            String email = getCurrentUser().getEmail();
            mailService.sendTestMail(getTenantId(), adminSettings.getJsonValue(), email);
        }
    }

    @ApiOperation(value = "Send test sms (sendTestMail)",
            notes = "Attempts to send test sms to the System Administrator User using SMS Settings and phone number provided as a parameters of the request. "
                    + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + RESOURCE_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/settings/testSms", method = RequestMethod.POST)
    public void sendTestSms(
            @ApiParam(value = "A JSON value representing the Test SMS request.")
            @RequestBody TestSmsRequest testSmsRequest) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        Authority authority = currentUser.getAuthority();
        if (Authority.SYS_ADMIN.equals(authority)) {
            accessControlService.checkPermission(currentUser, Resource.ADMIN_SETTINGS, Operation.READ);
        } else {
            accessControlService.checkPermission(currentUser, Resource.WHITE_LABELING, Operation.READ);
        }
        try {
            smsService.sendTestSms(testSmsRequest);
            auditLogService.logEntityAction(currentUser.getTenantId(), currentUser.getCustomerId(), currentUser.getId(), currentUser.getName(), currentUser.getId(), currentUser, ActionType.SMS_SENT, null, testSmsRequest.getNumberTo());
        } catch (ThingsboardException e) {
            auditLogService.logEntityAction(currentUser.getTenantId(), currentUser.getCustomerId(), currentUser.getId(), currentUser.getName(), currentUser.getId(), currentUser, ActionType.SMS_SENT, e, testSmsRequest.getNumberTo());
            throw e;
        }
    }

    @ApiOperation(value = "Get repository settings (getRepositorySettings)",
            notes = "Get the repository settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings")
    public RepositorySettings getRepositorySettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        RepositorySettings versionControlSettings = checkNotNull(versionControlService.getVersionControlSettings(getTenantId()));
        versionControlSettings.setPassword(null);
        versionControlSettings.setPrivateKey(null);
        versionControlSettings.setPrivateKeyPassword(null);
        return versionControlSettings;
    }

    @ApiOperation(value = "Check repository settings exists (repositorySettingsExists)",
            notes = "Check whether the repository settings exists. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings/exists")
    public Boolean repositorySettingsExists() throws ThingsboardException {
        if (accessControlService.hasPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ)) {
            return versionControlService.getVersionControlSettings(getTenantId()) != null;
        } else {
            return false;
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings/info")
    public RepositorySettingsInfo getRepositorySettingsInfo() throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        RepositorySettings repositorySettings = versionControlService.getVersionControlSettings(getTenantId());
        if (repositorySettings != null) {
            return RepositorySettingsInfo.builder()
                    .configured(true)
                    .readOnly(repositorySettings.isReadOnly())
                    .build();
        } else {
            return RepositorySettingsInfo.builder()
                    .configured(false)
                    .build();
        }
    }

    @ApiOperation(value = "Creates or Updates the repository settings (saveRepositorySettings)",
            notes = "Creates or Updates the repository settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/repositorySettings")
    public DeferredResult<RepositorySettings> saveRepositorySettings(@RequestBody RepositorySettings settings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        ListenableFuture<RepositorySettings> future = versionControlService.saveVersionControlSettings(getTenantId(), settings);
        return wrapFuture(Futures.transform(future, savedSettings -> {
            savedSettings.setPassword(null);
            savedSettings.setPrivateKey(null);
            savedSettings.setPrivateKeyPassword(null);
            return savedSettings;
        }, MoreExecutors.directExecutor()));
    }

    @ApiOperation(value = "Delete repository settings (deleteRepositorySettings)",
            notes = "Deletes the repository settings."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/repositorySettings", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<Void> deleteRepositorySettings() throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
        return wrapFuture(versionControlService.deleteVersionControlSettings(getTenantId()));
    }

    @ApiOperation(value = "Check repository access (checkRepositoryAccess)",
            notes = "Attempts to check repository access. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/repositorySettings/checkAccess", method = RequestMethod.POST)
    public DeferredResult<Void> checkRepositoryAccess(
            @ApiParam(value = "A JSON value representing the Repository Settings.")
            @RequestBody RepositorySettings settings) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        settings = checkNotNull(settings);
        return wrapFuture(versionControlService.checkVersionControlAccess(getTenantId(), settings));
    }

    @ApiOperation(value = "Get auto commit settings (getAutoCommitSettings)",
            notes = "Get the auto commit settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings")
    public AutoCommitSettings getAutoCommitSettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return checkNotNull(autoCommitSettingsService.get(getTenantId()));
    }

    @ApiOperation(value = "Check auto commit settings exists (autoCommitSettingsExists)",
            notes = "Check whether the auto commit settings exists. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings/exists")
    public Boolean autoCommitSettingsExists() throws ThingsboardException {
        if (accessControlService.hasPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ)) {
            return autoCommitSettingsService.get(getTenantId()) != null;
        } else {
            return false;
        }
    }

    @ApiOperation(value = "Creates or Updates the auto commit settings (saveAutoCommitSettings)",
            notes = "Creates or Updates the auto commit settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/autoCommitSettings")
    public AutoCommitSettings saveAutoCommitSettings(@RequestBody AutoCommitSettings settings) throws ThingsboardException {
        settings.values().forEach(config -> VcUtils.checkBranchName(config.getBranch()));
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        return autoCommitSettingsService.save(getTenantId(), settings);
    }

    @ApiOperation(value = "Delete auto commit settings (deleteAutoCommitSettings)",
            notes = "Deletes the auto commit settings."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/autoCommitSettings", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAutoCommitSettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
        autoCommitSettingsService.delete(getTenantId());
    }

    @ApiOperation(value = "Check for new Platform Releases (checkUpdates)",
            notes = "Check notifications about new platform releases. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/updates", method = RequestMethod.GET)
    @ResponseBody
    public UpdateMessage checkUpdates() throws ThingsboardException {
        return updateService.checkUpdates();
    }

    @ApiOperation(value = "Get license usage info (getLicenseUsageInfo)",
            notes = "Get license usage info. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/licenseUsageInfo", method = RequestMethod.GET)
    @ResponseBody
    public LicenseUsageInfo getLicenseUsageInfo() throws ThingsboardException {
        // LicenseInfo licenseInfo = subscriptionService.getLicenseInfo();

        LicenseInfo licenseInfo = new LicenseInfo();
        licenseInfo.setMaxDevices(0L);
        licenseInfo.setMaxAssets(0L);
        licenseInfo.setWhiteLabelingEnabled(true);
        licenseInfo.setDevelopment(true);
        licenseInfo.setPlan("Development env");
        //
        LicenseUsageInfo licenseUsageInfo = new LicenseUsageInfo(licenseInfo);
        licenseUsageInfo.setDevicesCount(deviceService.countDevices());
        licenseUsageInfo.setAssetsCount(assetService.countAssets());
        licenseUsageInfo.setDashboardsCount(dashboardService.countDashboards());
        licenseUsageInfo.setIntegrationsCount(integrationService.countCoreIntegrations());
        return licenseUsageInfo;
    }

    @ApiOperation(value = "Get system info (getSystemInfo)",
            notes = "Get main information about system. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/systemInfo", method = RequestMethod.GET)
    @ResponseBody
    public SystemInfo getSystemInfo() throws ThingsboardException {
        return systemInfoService.getSystemInfo();
    }

    @ApiOperation(value = "Get features info (getFeaturesInfo)",
            notes = "Get information about enabled/disabled features. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/featuresInfo", method = RequestMethod.GET)
    @ResponseBody
    public FeaturesInfo getFeaturesInfo() {
        return systemInfoService.getFeaturesInfo();
    }

    @ApiOperation(value = "Get OAuth2 log in processing URL (getMailProcessingUrl)", notes = "Returns the URL enclosed in " +
            "double quotes. After successful authentication with OAuth2 provider and user consent for requested scope, it makes a redirect to this path so that the platform can do " +
            "further log in processing and generating access tokens. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/mail/oauth2/loginProcessingUrl", method = RequestMethod.GET)
    @ResponseBody
    public String getMailProcessingUrl() throws ThingsboardException {
         accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
         return "\"/api/admin/mail/oauth2/code\"";
    }

    @ApiOperation(value = "Redirect user to mail provider login page. ", notes = "After user logged in and provided access" +
            "provider sends authorization code to specified redirect uri.)" )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/mail/oauth2/authorize", method = RequestMethod.GET, produces = "application/text")
    public String getAuthorizationUrl(HttpServletRequest request, HttpServletResponse response) throws Exception {
        SecurityUser currentUser = getCurrentUser();
        String state = StringUtils.generateSafeToken();
        if (request.getParameter(PREV_URI_PATH_PARAMETER) != null) {
            CookieUtils.addCookie(response, PREV_URI_COOKIE_NAME, request.getParameter(PREV_URI_PATH_PARAMETER), 180);
        }
        CookieUtils.addCookie(response, STATE_COOKIE_NAME, state, 180);
        internalSessionMap.put(state, currentUser.getTenantId());

        AdminSettings adminSettings;
        if (Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
            accessControlService.checkPermission(currentUser, Resource.ADMIN_SETTINGS, Operation.READ);
            adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, MAIL_SETTINGS_KEY), "No Administration settings found for key: " + MAIL_SETTINGS_KEY);
        } else {
            accessControlService.checkPermission(currentUser, Resource.WHITE_LABELING, Operation.READ);
            adminSettings = getTenantAdminSettings(currentUser.getTenantId(), MAIL_SETTINGS_KEY, true);
        }
        JsonNode jsonValue = adminSettings.getJsonValue();
        String clientId = checkNotNull(jsonValue.get("clientId"), "No clientId was configured").asText();
        String authUri = checkNotNull(jsonValue.get("authUri"), "No authorization uri was configured").asText();
        String redirectUri = checkNotNull(jsonValue.get("redirectUri"), "No Redirect uri was configured").asText();
        List<String> scope =  JacksonUtil.convertValue(checkNotNull(jsonValue.get("scope"), "No scope was configured"), new TypeReference<>() {
        });

        return "\"" + new AuthorizationCodeRequestUrl(authUri, clientId)
                .setScopes(scope)
                .setState(state)
                .setRedirectUri(redirectUri)
                .build() + "\"";
    }

    @RequestMapping(value = "/mail/oauth2/code", params = {"code", "state"}, method = RequestMethod.GET)
    public void codeProcessingUrl( @RequestParam(value = "code") String code, @RequestParam(value = "state") String state,
                                   HttpServletRequest request, HttpServletResponse response) throws Exception {
        Optional<Cookie> prevUrlOpt = CookieUtils.getCookie(request, PREV_URI_COOKIE_NAME);
        Optional<Cookie> cookieState = CookieUtils.getCookie(request, STATE_COOKIE_NAME);

        String baseUrl = this.systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
        String prevUri = baseUrl + (prevUrlOpt.isPresent() ? prevUrlOpt.get().getValue(): "/settings/outgoing-mail");

        if (cookieState.isEmpty() || !cookieState.get().getValue().equals(state)) {
            CookieUtils.deleteCookie(request, response, STATE_COOKIE_NAME);
            throw new ThingsboardException("Refresh token was not generated, invalid state param", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        TenantId tenantId = internalSessionMap.remove(cookieState.get().getValue());

        CookieUtils.deleteCookie(request, response, STATE_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, PREV_URI_COOKIE_NAME);

        AdminSettings adminSettings;
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(tenantId, MAIL_SETTINGS_KEY), "No Administration mail settings found");
        } else {
            adminSettings = getTenantAdminSettings(tenantId, MAIL_SETTINGS_KEY, false);
        }
        JsonNode jsonValue = adminSettings.getJsonValue();

        String clientId = checkNotNull(jsonValue.get("clientId"), "No clientId was configured").asText();
        String clientSecret = checkNotNull(jsonValue.get("clientSecret"), "No client secret was configured").asText();
        String clientRedirectUri = checkNotNull(jsonValue.get("redirectUri"), "No Redirect uri was configured").asText();
        String tokenUri = checkNotNull(jsonValue.get("tokenUri"), "No authorization uri was configured").asText();

        TokenResponse tokenResponse;
        try {
            tokenResponse = new AuthorizationCodeTokenRequest(new NetHttpTransport(), new GsonFactory(), new GenericUrl(tokenUri), code)
                    .setRedirectUri(clientRedirectUri)
                    .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                    .execute();
        } catch (IOException e) {
            log.warn("Unable to retrieve refresh token: {}", e.getMessage());
            throw new ThingsboardException("Error while requesting access token: " + e.getMessage(), ThingsboardErrorCode.GENERAL);
        }
        ((ObjectNode)jsonValue).put("refreshToken", tokenResponse.getRefreshToken());
        ((ObjectNode)jsonValue).put("tokenGenerated", true);

        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            adminSettingsService.saveAdminSettings(tenantId, adminSettings);
        } else {
            saveTenantAdminSettings(tenantId, adminSettings);
        }
        response.sendRedirect(prevUri);
    }

    private AdminSettings getTenantAdminSettings(TenantId tenantId, String key, boolean systemByDefault) throws Exception {
        String jsonString = getTenantAttributeValue(tenantId, key);
        JsonNode jsonValue = null;
        if (!StringUtils.isEmpty(jsonString)) {
            try {
                jsonValue = JacksonUtil.toJsonNode(jsonString);
            } catch (Exception e) {
            }
        }
        if (jsonValue == null) {
            if (systemByDefault) {
                AdminSettings systemAdminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(tenantId, key));
                jsonValue = systemAdminSettings.getJsonValue();
            } else {
                jsonValue = JacksonUtil.newObjectNode();
            }
        }
        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setKey(key);
        adminSettings.setJsonValue(jsonValue);
        return adminSettings;
    }

    private AdminSettings saveTenantAdminSettings(TenantId tenantId, AdminSettings adminSettings) throws Exception {
        JsonNode jsonValue = adminSettings.getJsonValue();
        if (adminSettings.getKey().equals("mail")) {
            JsonNode oldJsonValue = JacksonUtil.toJsonNode(getTenantAttributeValue(tenantId, "mail"));
            if (oldJsonValue != null) {
                if (!jsonValue.has("password") && oldJsonValue.has("password")){
                    ((ObjectNode) jsonValue).put("password", oldJsonValue.get("password").asText());
                }
                if (!jsonValue.has("refreshToken") && oldJsonValue.has("refreshToken")){
                    ((ObjectNode) jsonValue).put("refreshToken", oldJsonValue.get("refreshToken").asText());
                }
                dropRefreshTokenIfProviderInfoChanged(jsonValue, oldJsonValue);
            }
        }
        String jsonString = null;
        if (jsonValue != null) {
            try {
                jsonString = JacksonUtil.toString(jsonValue);
            } catch (Exception e) {
            }
        }
        if (jsonString == null) {
            jsonString = "";
        }
        saveTenantAttribute(tenantId, adminSettings.getKey(), jsonString);
        return adminSettings;
    }

    private String getTenantAttributeValue(TenantId tenantId, String key) throws Exception {
        List<AttributeKvEntry> attributeKvEntries =
                attributesService.find(tenantId, tenantId, DataConstants.SERVER_SCOPE, Arrays.asList(key)).get();
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private void saveTenantAttribute(TenantId tenantId, String key, String value) throws Exception {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(key, value), ts));
        attributesService.save(tenantId, tenantId, DataConstants.SERVER_SCOPE, attributes).get();
    }

     private void dropRefreshTokenIfProviderInfoChanged(JsonNode newJsonValue, JsonNode oldJsonValue) {
        if (newJsonValue.has("enableOauth2") && newJsonValue.get("enableOauth2").asBoolean()){
            if (!newJsonValue.get("providerId").equals(oldJsonValue.get("providerId")) ||
                    !newJsonValue.get("clientId").equals(oldJsonValue.get("clientId")) ||
                    !newJsonValue.get("clientSecret").equals(oldJsonValue.get("clientSecret")) ||
                    !newJsonValue.get("redirectUri").equals(oldJsonValue.get("redirectUri")) ||
                    (newJsonValue.has("providerTenantId") && !newJsonValue.get("providerTenantId").equals(oldJsonValue.get("providerTenantId")))){
                ((ObjectNode) newJsonValue).put("refreshTokenGenerated", false);
                ((ObjectNode) newJsonValue).remove("refreshToken");
            }
        }
    }
}
