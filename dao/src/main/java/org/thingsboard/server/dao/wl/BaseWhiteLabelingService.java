/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.dao.wl;

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
import org.thingsboard.server.common.data.wl.*;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class BaseWhiteLabelingService implements WhiteLabelingService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String LOGIN_WHITE_LABEL_PARAMS = "loginWhiteLabelParams";

    private static final String WHITE_LABEL_PARAMS = "whiteLabelParams";

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AttributesService attributesService;

    @Override
    public LoginWhiteLabelingParams getLoginWhiteLabelingParams() {
        AdminSettings loginWhiteLabelParamsSettings = adminSettingsService.findAdminSettingsByKey(LOGIN_WHITE_LABEL_PARAMS);
        String json = null;
        if (loginWhiteLabelParamsSettings != null) {
            json = loginWhiteLabelParamsSettings.getJsonValue().get("value").asText();
        }
        return constructLoginWlParams(json);
    }

    @Override
    public WhiteLabelingParams getSystemWhiteLabelingParams() {
        AdminSettings whiteLabelParamsSettings = adminSettingsService.findAdminSettingsByKey(WHITE_LABEL_PARAMS);
        String json = null;
        if (whiteLabelParamsSettings != null) {
            json = whiteLabelParamsSettings.getJsonValue().get("value").asText();
        }
        return constructWlParams(json);
    }

    @Override
    public WhiteLabelingParams getTenantWhiteLabelingParams(TenantId tenantId) {
        return getEntityWhiteLabelParams(tenantId);
    }

    @Override
    public WhiteLabelingParams getCustomerWhiteLabelingParams(CustomerId customerId) {
        return getEntityWhiteLabelParams(customerId);
    }

    @Override
    public WhiteLabelingParams getMergedSystemWhiteLabelingParams(String logoImageChecksum, String faviconChecksum) {
        WhiteLabelingParams result = getSystemWhiteLabelingParams();
        result.prepareImages(logoImageChecksum, faviconChecksum);
        return result;
    }

    @Override
    public LoginWhiteLabelingParams getMergedLoginWhiteLabelingParams(String logoImageChecksum, String faviconChecksum) {
        LoginWhiteLabelingParams result = getLoginWhiteLabelingParams();
        result.merge(getSystemWhiteLabelingParams());
        result.prepareImages(logoImageChecksum, faviconChecksum);
        return result;
    }

    @Override
    public WhiteLabelingParams getMergedTenantWhiteLabelingParams(TenantId tenantId, String logoImageChecksum, String faviconChecksum) {
        WhiteLabelingParams result = getTenantWhiteLabelingParams(tenantId);
        result.merge(getSystemWhiteLabelingParams());
        result.prepareImages(logoImageChecksum, faviconChecksum);
        return result;
    }

    @Override
    public WhiteLabelingParams getMergedCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, String logoImageChecksum, String faviconChecksum) {
        WhiteLabelingParams result = getCustomerWhiteLabelingParams(customerId);
        result.merge(getTenantWhiteLabelingParams(tenantId)).merge(getSystemWhiteLabelingParams());
        result.prepareImages(logoImageChecksum, faviconChecksum);
        return result;
    }

    @Override
    public WhiteLabelingParams saveSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        whiteLabelingParams = prepareChecksums(whiteLabelingParams);
        AdminSettings whiteLabelParamsSettings = adminSettingsService.findAdminSettingsByKey(WHITE_LABEL_PARAMS);
        if (whiteLabelParamsSettings == null) {
            whiteLabelParamsSettings = new AdminSettings();
            whiteLabelParamsSettings.setKey(WHITE_LABEL_PARAMS);
            ObjectNode node = objectMapper.createObjectNode();
            whiteLabelParamsSettings.setJsonValue(node);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(whiteLabelingParams);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert White Labeling Params to JSON!", e);
            throw new IncorrectParameterException("Unable to convert White Labeling Params to JSON!");
        }
        ((ObjectNode)whiteLabelParamsSettings.getJsonValue()).put("value", json);
        adminSettingsService.saveAdminSettings(whiteLabelParamsSettings);
        return getSystemWhiteLabelingParams();
    }

    @Override
    public LoginWhiteLabelingParams saveLoginWhiteLabelingParams(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        loginWhiteLabelingParams = prepareChecksums(loginWhiteLabelingParams);
        AdminSettings loginWhiteLabelParamsSettings = adminSettingsService.findAdminSettingsByKey(LOGIN_WHITE_LABEL_PARAMS);
        if (loginWhiteLabelParamsSettings == null) {
            loginWhiteLabelParamsSettings = new AdminSettings();
            loginWhiteLabelParamsSettings.setKey(LOGIN_WHITE_LABEL_PARAMS);
            ObjectNode node = objectMapper.createObjectNode();
            loginWhiteLabelParamsSettings.setJsonValue(node);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(loginWhiteLabelingParams);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert Login White Labeling Params to JSON!", e);
            throw new IncorrectParameterException("Unable to convert Login White Labeling Params to JSON!");
        }
        ((ObjectNode)loginWhiteLabelParamsSettings.getJsonValue()).put("value", json);
        adminSettingsService.saveAdminSettings(loginWhiteLabelParamsSettings);
        return getLoginWhiteLabelingParams();
    }

    @Override
    public WhiteLabelingParams saveTenantWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams) {
        whiteLabelingParams = prepareChecksums(whiteLabelingParams);
        saveEntityWhiteLabelParams(tenantId, whiteLabelingParams);
        return getTenantWhiteLabelingParams(tenantId);
    }

    @Override
    public WhiteLabelingParams saveCustomerWhiteLabelingParams(CustomerId customerId, WhiteLabelingParams whiteLabelingParams) {
        whiteLabelingParams = prepareChecksums(whiteLabelingParams);
        saveEntityWhiteLabelParams(customerId, whiteLabelingParams);
        return getCustomerWhiteLabelingParams(customerId);
    }

    @Override
    public WhiteLabelingParams mergeSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        return whiteLabelingParams;
    }

    @Override
    public WhiteLabelingParams mergeTenantWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        return whiteLabelingParams.merge(getSystemWhiteLabelingParams());
    }

    @Override
    public WhiteLabelingParams mergeCustomerWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams) {
        return whiteLabelingParams.merge(getTenantWhiteLabelingParams(tenantId)).
                merge(getSystemWhiteLabelingParams());
    }

    private LoginWhiteLabelingParams constructLoginWlParams(String json) {
        LoginWhiteLabelingParams result = null;
        if (!StringUtils.isEmpty(json)) {
            try {
                result = objectMapper.readValue(json, LoginWhiteLabelingParams.class);
            } catch (IOException e) {
                log.error("Unable to read Login White Labeling Params from JSON!", e);
                throw new IncorrectParameterException("Unable to read Login White Labeling Params from JSON!");
            }
        }
        if (result == null) {
            result = new LoginWhiteLabelingParams();
        }
        return result;
    }

    private WhiteLabelingParams constructWlParams(String json) {
        WhiteLabelingParams result = null;
        if (!StringUtils.isEmpty(json)) {
            try {
                result = objectMapper.readValue(json, WhiteLabelingParams.class);
            } catch (IOException e) {
                log.error("Unable to read White Labeling Params from JSON!", e);
                throw new IncorrectParameterException("Unable to read White Labeling Params from JSON!");
            }
        }
        if (result == null) {
            result = new WhiteLabelingParams();
        }
        return result;
    }

    private WhiteLabelingParams getEntityWhiteLabelParams(EntityId entityId) {
        String json = getEntityAttributeValue(entityId, WHITE_LABEL_PARAMS);
        return constructWlParams(json);
    }

    private String getEntityAttributeValue(EntityId entityId, String key) {
        List<AttributeKvEntry> attributeKvEntries = null;
        try {
            attributeKvEntries = attributesService.find(entityId, DataConstants.SERVER_SCOPE, Arrays.asList(key)).get();
        } catch (Exception e) {
            log.error("Unable to read White Labeling Params from attributes!", e);
            throw new IncorrectParameterException("Unable to read White Labeling Params from attributes!");
        }
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private void saveEntityWhiteLabelParams(EntityId entityId, WhiteLabelingParams whiteLabelingParams) {
        String json;
        try {
            json = objectMapper.writeValueAsString(whiteLabelingParams);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert White Labeling Params to JSON!", e);
            throw new IncorrectParameterException("Unable to convert White Labeling Params to JSON!");
        }
        saveEntityAttribute(entityId, WHITE_LABEL_PARAMS, json);
    }

    private void saveEntityAttribute(EntityId entityId, String key, String value) {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(key, value), ts));
        try {
            attributesService.save(entityId, DataConstants.SERVER_SCOPE, attributes).get();
        } catch (Exception e) {
            log.error("Unable to save White Labeling Params to attributes!", e);
            throw new IncorrectParameterException("Unable to save White Labeling Params to attributes!");
        }
    }

    private <T extends WhiteLabelingParams> T prepareChecksums(T whiteLabelingParams) {
        String logoImageChecksum = "";
        String logoImageUrl = whiteLabelingParams.getLogoImageUrl();
        if (!StringUtils.isEmpty(logoImageUrl)) {
            logoImageChecksum = calculateSha1Checksum(logoImageUrl);
        }
        whiteLabelingParams.setLogoImageChecksum(logoImageChecksum);
        String faviconChecksum = "";
        if (whiteLabelingParams.getFavicon() != null && !StringUtils.isEmpty(whiteLabelingParams.getFavicon().getUrl())) {
            faviconChecksum = calculateSha1Checksum(whiteLabelingParams.getFavicon().getUrl());
        }
        whiteLabelingParams.setFaviconChecksum(faviconChecksum);
        return whiteLabelingParams;
    }

    private static String calculateSha1Checksum(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        byte[] inputBytes = data.getBytes();
        byte[] hashBytes = digest.digest(inputBytes);
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < hashBytes.length; i++) {
            sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }


}
