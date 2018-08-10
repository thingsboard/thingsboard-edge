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
package org.thingsboard.server.dao.localization;

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
import org.thingsboard.server.common.data.localization.CustomLocalization;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class BaseCustomLocalizationService implements CustomLocalizationService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CUSTOM_LOCALIZATION_ATTR_NAME = "customLocalization";

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AttributesService attributesService;

    @Override
    public CustomLocalization getSystemCustomLocalization() {
        AdminSettings customLocalizationSettings = adminSettingsService.findAdminSettingsByKey(CUSTOM_LOCALIZATION_ATTR_NAME);
        String json = null;
        if (customLocalizationSettings != null) {
            json = customLocalizationSettings.getJsonValue().get("value").asText();
        }
        return constructCustomLocalization(json);
    }

    @Override
    public CustomLocalization getTenantCustomLocalization(TenantId tenantId) {
        return getEntityCustomLocalization(tenantId);
    }

    @Override
    public CustomLocalization getCustomerCustomLocalization(CustomerId customerId) {
        return getEntityCustomLocalization(customerId);
    }

    @Override
    public CustomLocalization getMergedTenantCustomLocalization(TenantId tenantId) {
        CustomLocalization result = getTenantCustomLocalization(tenantId);
        result.merge(getSystemCustomLocalization());
        return result;
    }

    @Override
    public CustomLocalization getMergedCustomerCustomLocalization(TenantId tenantId, CustomerId customerId) {
        CustomLocalization result = getCustomerCustomLocalization(customerId);
        result.merge(getTenantCustomLocalization(tenantId)).merge(getSystemCustomLocalization());
        return result;
    }

    @Override
    public CustomLocalization saveSystemCustomLocalization(CustomLocalization customLocalization) {
        AdminSettings customLocalizationSettings = adminSettingsService.findAdminSettingsByKey(CUSTOM_LOCALIZATION_ATTR_NAME);
        if (customLocalizationSettings == null) {
            customLocalizationSettings = new AdminSettings();
            customLocalizationSettings.setKey(CUSTOM_LOCALIZATION_ATTR_NAME);
            ObjectNode node = objectMapper.createObjectNode();
            customLocalizationSettings.setJsonValue(node);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(customLocalization);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert custom localization to JSON!", e);
            throw new IncorrectParameterException("Unable to convert custom localization to JSON!");
        }
        ((ObjectNode) customLocalizationSettings.getJsonValue()).put("value", json);
        adminSettingsService.saveAdminSettings(customLocalizationSettings);
        return getSystemCustomLocalization();
    }

    @Override
    public CustomLocalization saveTenantCustomLocalization(TenantId tenantId, CustomLocalization customLocalization) {
        saveEntityCustomLocalization(tenantId, customLocalization);
        return getTenantCustomLocalization(tenantId);
    }

    @Override
    public CustomLocalization saveCustomerCustomLocalization(CustomerId customerId, CustomLocalization customLocalization) {
        saveEntityCustomLocalization(customerId, customLocalization);
        return getCustomerCustomLocalization(customerId);
    }

    private CustomLocalization constructCustomLocalization(String json) {
        CustomLocalization result = null;
        if (!StringUtils.isEmpty(json)) {
            try {
                result = objectMapper.readValue(json, CustomLocalization.class);
            } catch (IOException e) {
                log.error("Unable to read custom localization from JSON!", e);
                throw new IncorrectParameterException("Unable to read custom localization from JSON!");
            }
        }
        if (result == null) {
            result = new CustomLocalization();
        }
        return result;
    }

    private CustomLocalization getEntityCustomLocalization(EntityId entityId) {
        String json = getEntityAttributeValue(entityId);
        return constructCustomLocalization(json);
    }

    private String getEntityAttributeValue(EntityId entityId) {
        List<AttributeKvEntry> attributeKvEntries;
        try {
            attributeKvEntries = attributesService.find(entityId, DataConstants.SERVER_SCOPE, Arrays.asList(CUSTOM_LOCALIZATION_ATTR_NAME)).get();
        } catch (Exception e) {
            log.error("Unable to read custom localization from attributes!", e);
            throw new IncorrectParameterException("Unable to read custom localization from attributes!");
        }
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private void saveEntityCustomLocalization(EntityId entityId, CustomLocalization customLocalization) {
        String json;
        try {
            json = objectMapper.writeValueAsString(customLocalization);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert custom localization to JSON!", e);
            throw new IncorrectParameterException("Unable to convert custom localization to JSON!");
        }
        saveEntityAttribute(entityId, json);
    }

    private void saveEntityAttribute(EntityId entityId, String value) {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(CUSTOM_LOCALIZATION_ATTR_NAME, value), ts));
        try {
            attributesService.save(entityId, DataConstants.SERVER_SCOPE, attributes).get();
        } catch (Exception e) {
            log.error("Unable to save custom localization to attributes!", e);
            throw new IncorrectParameterException("Unable to save custom localization to attributes!");
        }
    }

}
