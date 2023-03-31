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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.exception.ThingsboardException;
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
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.mail.MailTemplates;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
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

@RestController
@TbCoreComponent
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController extends BaseController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            adminSettings = getTenantAdminSettings(key, systemByDefault);
            if (adminSettings.getKey().equals("mailTemplates")) {
                ((ObjectNode) adminSettings.getJsonValue()).remove(MailTemplates.API_USAGE_STATE_ENABLED);
                ((ObjectNode) adminSettings.getJsonValue()).remove(MailTemplates.API_USAGE_STATE_WARNING);
                ((ObjectNode) adminSettings.getJsonValue()).remove(MailTemplates.API_USAGE_STATE_DISABLED);
            }
        }
        if (adminSettings.getKey().equals("mail")) {
            ((ObjectNode) adminSettings.getJsonValue()).remove("password");
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
            adminSettings = saveTenantAdminSettings(adminSettings);
        }
        if (adminSettings.getKey().equals("mail")) {
            ((ObjectNode) adminSettings.getJsonValue()).remove("password");
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
            if (!adminSettings.getJsonValue().has("password")) {
                AdminSettings mailSettings;
                if (Authority.SYS_ADMIN.equals(authority)) {
                    mailSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail"));
                } else {
                    mailSettings = getTenantAdminSettings("mail", false);
                }
                ((ObjectNode) adminSettings.getJsonValue()).put("password", mailSettings.getJsonValue().get("password").asText());
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
        Authority authority = getCurrentUser().getAuthority();
        if (Authority.SYS_ADMIN.equals(authority)) {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        } else {
            accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
        }
        smsService.sendTestSms(testSmsRequest);
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

    private AdminSettings getTenantAdminSettings(String key, boolean systemByDefault) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
        String jsonString = getEntityAttributeValue(getTenantId(), key);
        JsonNode jsonValue = null;
        if (!StringUtils.isEmpty(jsonString)) {
            try {
                jsonValue = objectMapper.readTree(jsonString);
            } catch (Exception e) {
            }
        }
        if (jsonValue == null) {
            if (systemByDefault) {
                AdminSettings systemAdminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(getTenantId(), key));
                jsonValue = systemAdminSettings.getJsonValue();
            } else {
                jsonValue = objectMapper.createObjectNode();
            }
        }
        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setKey(key);
        adminSettings.setJsonValue(jsonValue);
        return adminSettings;
    }

    private AdminSettings saveTenantAdminSettings(AdminSettings adminSettings) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE);
        JsonNode jsonValue = adminSettings.getJsonValue();
        if (adminSettings.getKey().equals("mail") && !jsonValue.has("password")) {
            JsonNode oldMailSettings = objectMapper.readTree(getEntityAttributeValue(getTenantId(), "mail"));
            if (oldMailSettings != null) {
                if (oldMailSettings.has("password")) {
                    ((ObjectNode) jsonValue).put("password", oldMailSettings.get("password").asText());
                }
            }
        }
        String jsonString = null;
        if (jsonValue != null) {
            try {
                jsonString = objectMapper.writeValueAsString(jsonValue);
            } catch (Exception e) {
            }
        }
        if (jsonString == null) {
            jsonString = "";
        }
        saveEntityAttribute(getTenantId(), adminSettings.getKey(), jsonString);
        return adminSettings;
    }

    private String getEntityAttributeValue(EntityId entityId, String key) throws Exception {
        List<AttributeKvEntry> attributeKvEntries =
                attributesService.find(getTenantId(), entityId, DataConstants.SERVER_SCOPE, Arrays.asList(key)).get();
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private void saveEntityAttribute(EntityId entityId, String key, String value) throws Exception {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(key, value), ts));
        attributesService.save(getTenantId(), entityId, DataConstants.SERVER_SCOPE, attributes).get();
    }

}
