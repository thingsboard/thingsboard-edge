/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.translation;

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
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class BaseCustomTranslationService implements CustomTranslationService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CUSTOM_TRANSLATION_ATTR_NAME = "customTranslation";

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AttributesService attributesService;

    @Override
    public CustomTranslation getSystemCustomTranslation(TenantId tenantId) {
        AdminSettings customTranslationSettings = adminSettingsService.findAdminSettingsByKey(tenantId, CUSTOM_TRANSLATION_ATTR_NAME);
        String json = null;
        if (customTranslationSettings != null) {
            json = customTranslationSettings.getJsonValue().get("value").asText();
        }
        return constructCustomTranslation(json);
    }

    @Override
    public CustomTranslation getTenantCustomTranslation(TenantId tenantId) {
        return getEntityCustomTranslation(tenantId, tenantId);
    }

    @Override
    public CustomTranslation getCustomerCustomTranslation(TenantId tenantId, CustomerId customerId) {
        return getEntityCustomTranslation(tenantId, customerId);
    }

    @Override
    public CustomTranslation getMergedTenantCustomTranslation(TenantId tenantId) {
        CustomTranslation result = getTenantCustomTranslation(tenantId);
        result.merge(getSystemCustomTranslation(tenantId));
        return result;
    }

    @Override
    public CustomTranslation getMergedCustomerCustomTranslation(TenantId tenantId, CustomerId customerId) {
        CustomTranslation result = getCustomerCustomTranslation(tenantId, customerId);
        result.merge(getTenantCustomTranslation(tenantId)).merge(getSystemCustomTranslation(tenantId));
        return result;
    }

    @Override
    public CustomTranslation saveSystemCustomTranslation(CustomTranslation customTranslation) {
        AdminSettings customTranslationSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, CUSTOM_TRANSLATION_ATTR_NAME);
        if (customTranslationSettings == null) {
            customTranslationSettings = new AdminSettings();
            customTranslationSettings.setKey(CUSTOM_TRANSLATION_ATTR_NAME);
            ObjectNode node = objectMapper.createObjectNode();
            customTranslationSettings.setJsonValue(node);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(customTranslation);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert custom translation to JSON!", e);
            throw new IncorrectParameterException("Unable to convert custom translation to JSON!");
        }
        ((ObjectNode) customTranslationSettings.getJsonValue()).put("value", json);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, customTranslationSettings);
        return getSystemCustomTranslation(TenantId.SYS_TENANT_ID);
    }

    @Override
    public CustomTranslation saveTenantCustomTranslation(TenantId tenantId, CustomTranslation customTranslation) {
        saveEntityCustomTranslation(tenantId, tenantId, customTranslation);
        return getTenantCustomTranslation(tenantId);
    }

    @Override
    public CustomTranslation saveCustomerCustomTranslation(TenantId tenantId, CustomerId customerId, CustomTranslation customTranslation) {
        saveEntityCustomTranslation(tenantId, customerId, customTranslation);
        return getCustomerCustomTranslation(tenantId, customerId);
    }

    private CustomTranslation constructCustomTranslation(String json) {
        CustomTranslation result = null;
        if (!StringUtils.isEmpty(json)) {
            try {
                result = objectMapper.readValue(json, CustomTranslation.class);
            } catch (IOException e) {
                log.error("Unable to read custom translation from JSON!", e);
                throw new IncorrectParameterException("Unable to read custom translation from JSON!");
            }
        }
        if (result == null) {
            result = new CustomTranslation();
        }
        return result;
    }

    private CustomTranslation getEntityCustomTranslation(TenantId tenantId, EntityId entityId) {
        String json = getEntityAttributeValue(tenantId, entityId);
        return constructCustomTranslation(json);
    }

    private String getEntityAttributeValue(TenantId tenantId, EntityId entityId) {
        List<AttributeKvEntry> attributeKvEntries;
        try {
            attributeKvEntries = attributesService.find(tenantId, entityId, DataConstants.SERVER_SCOPE, Arrays.asList(CUSTOM_TRANSLATION_ATTR_NAME)).get();
        } catch (Exception e) {
            log.error("Unable to read custom translation from attributes!", e);
            throw new IncorrectParameterException("Unable to read custom translation from attributes!");
        }
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private void saveEntityCustomTranslation(TenantId tenantId, EntityId entityId, CustomTranslation customTranslation) {
        String json;
        try {
            json = objectMapper.writeValueAsString(customTranslation);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert custom translation to JSON!", e);
            throw new IncorrectParameterException("Unable to convert custom translation to JSON!");
        }
        saveEntityAttribute(tenantId, entityId, json);
    }

    private void saveEntityAttribute(TenantId tenantId, EntityId entityId, String value) {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(CUSTOM_TRANSLATION_ATTR_NAME, value), ts));
        try {
            attributesService.save(tenantId, entityId, DataConstants.SERVER_SCOPE, attributes).get();
        } catch (Exception e) {
            log.error("Unable to save custom translation to attributes!", e);
            throw new IncorrectParameterException("Unable to save custom translation to attributes!");
        }
    }

}
