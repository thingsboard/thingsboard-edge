/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.update.UpdateService;
import org.thingsboard.server.service.update.model.UpdateMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MailService mailService;
    
    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private UpdateService updateService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/settings/{key}", method = RequestMethod.GET)
    @ResponseBody
    public AdminSettings getAdminSettings(@PathVariable("key") String key,
                                          @RequestParam(required = false,
                                                        defaultValue = "false") boolean systemByDefault) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            if (authority == Authority.SYS_ADMIN) {
                accessControlService.checkPermission(getCurrentUser(), TenantId.SYS_TENANT_ID, Resource.ADMIN_SETTINGS, Operation.READ);
                return checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key));
            } else {
                return getTenantAdminSettings(key, systemByDefault);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/settings", method = RequestMethod.POST)
    @ResponseBody 
    public AdminSettings saveAdminSettings(@RequestBody AdminSettings adminSettings) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            if (authority == Authority.SYS_ADMIN) {
                accessControlService.checkPermission(getCurrentUser(), TenantId.SYS_TENANT_ID, Resource.ADMIN_SETTINGS, Operation.WRITE);
                adminSettings = checkNotNull(adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings));
            } else {
                adminSettings = saveTenantAdminSettings(adminSettings);
            }
            return adminSettings;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/settings/testMail", method = RequestMethod.POST)
    public void sendTestMail(@RequestBody AdminSettings adminSettings) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            if (authority == Authority.SYS_ADMIN) {
                accessControlService.checkPermission(getCurrentUser(), TenantId.SYS_TENANT_ID, Resource.ADMIN_SETTINGS, Operation.READ);
            } else {
                accessControlService.checkPermission(getCurrentUser(), getTenantId(), Resource.TENANT, Operation.READ);
            }
            adminSettings = checkNotNull(adminSettings);
            if (adminSettings.getKey().equals("mail")) {
               String email = getCurrentUser().getEmail();
               mailService.sendTestMail(getTenantId(), adminSettings.getJsonValue(), email);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/updates", method = RequestMethod.GET)
    @ResponseBody
    public UpdateMessage checkUpdates() throws ThingsboardException {
        try {
            return updateService.checkUpdates();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private AdminSettings getTenantAdminSettings(String key, boolean systemByDefault) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), getTenantId(), Resource.TENANT, Operation.READ_ATTRIBUTES, getTenantId());
        String jsonString = getEntityAttributeValue(getTenantId(), key);
        JsonNode jsonValue = null;
        if (!StringUtils.isEmpty(jsonString)) {
            try {
                jsonValue = objectMapper.readTree(jsonString);
            } catch (Exception e) {}
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
        accessControlService.checkPermission(getCurrentUser(), getTenantId(), Resource.TENANT, Operation.WRITE_ATTRIBUTES, getTenantId());
        JsonNode jsonValue = adminSettings.getJsonValue();
        String jsonString = null;
        if (jsonValue != null) {
            try {
                jsonString = objectMapper.writeValueAsString(jsonValue);
            } catch (Exception e) {}
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
