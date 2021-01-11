/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.menu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class BaseCustomMenuService implements CustomMenuService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CUSTOM_MENU_ATTR_NAME = "customMenu";

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AttributesService attributesService;

    @Override
    public CustomMenu getSystemCustomMenu(TenantId tenantId) {
        AdminSettings customMenuSettings = adminSettingsService.findAdminSettingsByKey(tenantId, CUSTOM_MENU_ATTR_NAME);
        String json = null;
        if (customMenuSettings != null) {
            json = customMenuSettings.getJsonValue().get("value").asText();
        }
        return constructCustomMenu(json);
    }

    @Override
    public CustomMenu getTenantCustomMenu(TenantId tenantId) {
        return getEntityCustomMenu(tenantId, tenantId);
    }

    @Override
    public CustomMenu getCustomerCustomMenu(TenantId tenantId, CustomerId customerId) {
        return getEntityCustomMenu(tenantId, customerId);
    }

    @Override
    public CustomMenu getMergedTenantCustomMenu(TenantId tenantId) {
        CustomMenu result = getTenantCustomMenu(tenantId);
        if (result == null) {
            result = getSystemCustomMenu(tenantId);
        }
        return result;
    }

    @Override
    public CustomMenu getMergedCustomerCustomMenu(TenantId tenantId, CustomerId customerId) {
        CustomMenu result = getCustomerCustomMenu(tenantId, customerId);
        if (result == null) {
            result = getTenantCustomMenu(tenantId);
        }
        if (result == null) {
            result = getSystemCustomMenu(tenantId);
        }
        return result;
    }

    @Override
    public CustomMenu saveSystemCustomMenu(CustomMenu customMenu) {
        AdminSettings customMenuSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, CUSTOM_MENU_ATTR_NAME);
        if (customMenuSettings == null) {
            customMenuSettings = new AdminSettings();
            customMenuSettings.setKey(CUSTOM_MENU_ATTR_NAME);
            ObjectNode node = objectMapper.createObjectNode();
            customMenuSettings.setJsonValue(node);
        }
        String json;
        try {
            if (customMenu != null) {
                json = objectMapper.writeValueAsString(customMenu);
            } else {
                json = "";
            }
        } catch (JsonProcessingException e) {
            log.error("Unable to convert custom menu to JSON!", e);
            throw new IncorrectParameterException("Unable to convert custom menu to JSON!");
        }
        ((ObjectNode) customMenuSettings.getJsonValue()).put("value", json);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, customMenuSettings);
        return getSystemCustomMenu(TenantId.SYS_TENANT_ID);
    }

    @Override
    public CustomMenu saveTenantCustomMenu(TenantId tenantId, CustomMenu customMenu) {
        saveEntityCustomMenu(tenantId, tenantId, customMenu);
        return getTenantCustomMenu(tenantId);
    }

    @Override
    public CustomMenu saveCustomerCustomMenu(TenantId tenantId, CustomerId customerId, CustomMenu customMenu) {
        saveEntityCustomMenu(tenantId, customerId, customMenu);
        return getCustomerCustomMenu(tenantId, customerId);
    }

    private CustomMenu constructCustomMenu(String json) {
        CustomMenu result = null;
        if (!StringUtils.isEmpty(json)) {
            try {
                result = objectMapper.readValue(json, CustomMenu.class);
            } catch (IOException e) {
                log.error("Unable to read custom menu from JSON!", e);
                throw new IncorrectParameterException("Unable to read custom menu from JSON!");
            }
        }
        return result;
    }

    private CustomMenu getEntityCustomMenu(TenantId tenantId, EntityId entityId) {
        String json = getEntityAttributeValue(tenantId, entityId);
        return constructCustomMenu(json);
    }

    private String getEntityAttributeValue(TenantId tenantId, EntityId entityId) {
        List<AttributeKvEntry> attributeKvEntries;
        try {
            attributeKvEntries = attributesService.find(tenantId, entityId, DataConstants.SERVER_SCOPE, Arrays.asList(CUSTOM_MENU_ATTR_NAME)).get();
        } catch (Exception e) {
            log.error("Unable to read custom menu from attributes!", e);
            throw new IncorrectParameterException("Unable to read custom menu from attributes!");
        }
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private void saveEntityCustomMenu(TenantId tenantId, EntityId entityId, CustomMenu customMenu) {
        String json;
        try {
            if (customMenu != null) {
                json = objectMapper.writeValueAsString(customMenu);
            } else {
                json = "";
            }
        } catch (JsonProcessingException e) {
            log.error("Unable to convert custom menu to JSON!", e);
            throw new IncorrectParameterException("Unable to convert custom menu to JSON!");
        }
        saveEntityAttribute(tenantId, entityId, json);
    }

    private void saveEntityAttribute(TenantId tenantId, EntityId entityId, String value) {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(CUSTOM_MENU_ATTR_NAME, value), ts));
        try {
            attributesService.save(tenantId, entityId, DataConstants.SERVER_SCOPE, attributes).get();
        } catch (Exception e) {
            log.error("Unable to save custom menu to attributes!", e);
            throw new IncorrectParameterException("Unable to save custom menu to attributes!");
        }
    }

}
