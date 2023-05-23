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
package org.thingsboard.server.dao.selfregistration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BaseSelfRegistrationService implements SelfRegistrationService {

    private static final String SELF_REGISTRATION_PARAMS = "selfRegistrationParams";

    private static final String PRIVACY_POLICY = "privacyPolicy";

    private static final String TERMS_OF_USE = "termsOfUse";

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Override
    public SelfRegistrationParams saveTenantSelfRegistrationParams(TenantId tenantId, SelfRegistrationParams selfRegistrationParams) {
        saveTenantSelfRegistrationParams(tenantId, tenantId, selfRegistrationParams);
        return getTenantSelfRegistrationParams(tenantId);
    }

    @Override
    public SelfRegistrationParams getTenantSelfRegistrationParams(TenantId tenantId) {
        return getTenantSelfRegistrationParams(tenantId, tenantId);
    }

    @Override
    public SelfRegistrationParams getSelfRegistrationParams(TenantId sysTenantId, String domainName, String pkgName) {
        SelfRegistrationParams result = null;
        EntityId entityId = getEntityIdByDomainName(sysTenantId, domainName);
        if (entityId != null) {
            result = getTenantSelfRegistrationParams(sysTenantId, entityId);
            if (!StringUtils.isEmpty(pkgName) && !pkgName.equals(result.getPkgName())) {
                result = null;
            }
        }
        if (result == null) {
            result = new SelfRegistrationParams();
        }
        return result;
    }

    @Override
    public String getPrivacyPolicy(TenantId sysTenantId, String domainName) {
        String result = null;
        EntityId entityId = getEntityIdByDomainName(sysTenantId, domainName);
        if (entityId != null) {
            result = getTenantPrivacyPolicy(sysTenantId, entityId);
        }
        if (result == null) {
            result = "";
        }
        return result;
    }

    @Override
    public String getTenantPrivacyPolicy(TenantId tenantId) {
        return getTenantPrivacyPolicy(tenantId, tenantId);
    }

    @Override
    public String getTermsOfUse(TenantId sysTenantId, String domainName) {
        String result = null;
        EntityId entityId = getEntityIdByDomainName(sysTenantId, domainName);
        if (entityId != null) {
            result = getTenantTermsOfUse(sysTenantId, entityId);
        }
        if (result == null) {
            result = "";
        }
        return result;
    }

    @Override
    public String getTenantTermsOfUse(TenantId tenantId) {
        return getTenantTermsOfUse(tenantId, tenantId);
    }

    @Override
    public TenantId getTenantIdByDomainName(TenantId sysTenantId, String domainName) {
        return new TenantId(getEntityIdByDomainName(sysTenantId, domainName).getId());
    }

    private void saveTenantSelfRegistrationParams(TenantId tenantId, EntityId entityId, SelfRegistrationParams selfRegistrationParams) {
        String selfRegistrationKey = constructSelfRegistrationKey(selfRegistrationParams.getDomainName());
        AdminSettings existentAdminSettingsByKey = adminSettingsService.findAdminSettingsByKey(tenantId, selfRegistrationKey);
        if (StringUtils.isEmpty(selfRegistrationParams.getAdminSettingsId())) {
            if (existentAdminSettingsByKey == null) {
                existentAdminSettingsByKey = saveSelfRegistrationSettings(tenantId, entityId, selfRegistrationKey);
                selfRegistrationParams.setAdminSettingsId(existentAdminSettingsByKey.getId().getId().toString());
            } else {
                log.error("Current domain name [{}] already registered in the system!", selfRegistrationParams.getDomainName());
                throw new IncorrectParameterException("Current domain name [" + selfRegistrationParams.getDomainName() + "] already registered in the system!");
            }
        } else {
            AdminSettings existentLoginWhiteLabelSettingsById = adminSettingsService.findAdminSettingsById(
                    tenantId,
                    new AdminSettingsId(UUID.fromString(selfRegistrationParams.getAdminSettingsId())));

            if (existentLoginWhiteLabelSettingsById == null) {
                log.error("Admin setting ID is already set in self registration object, but doesn't exist in the database");
                throw new IllegalStateException("Admin setting ID is already set in self registration object, but doesn't exist in the database");
            }

            if (!existentLoginWhiteLabelSettingsById.getKey().equals(selfRegistrationKey)) {
                if (existentAdminSettingsByKey == null) {
                    adminSettingsService.deleteAdminSettingsByKey(tenantId, existentLoginWhiteLabelSettingsById.getKey());
                    existentLoginWhiteLabelSettingsById = saveSelfRegistrationSettings(tenantId, entityId, selfRegistrationKey);
                    selfRegistrationParams.setAdminSettingsId(existentLoginWhiteLabelSettingsById.getId().getId().toString());
                } else {
                    log.error("Current domain name [{}] already registered in the system!", selfRegistrationParams.getDomainName());
                    throw new IncorrectParameterException("Current domain name [" + selfRegistrationParams.getDomainName() + "] already registered in the system!");
                }
            }
        }
        saveSelfRegistrationParams(tenantId, entityId, selfRegistrationParams);
    }

    private AdminSettings saveSelfRegistrationSettings(TenantId tenantId, EntityId currentEntityId, String selfRegistrationKey) {
        AdminSettings selfRegistrationSettings = new AdminSettings();
        selfRegistrationSettings.setKey(selfRegistrationKey);
        ObjectNode node = JacksonUtil.newObjectNode();
        selfRegistrationSettings.setJsonValue(node);
        ((ObjectNode) selfRegistrationSettings.getJsonValue()).put("entityType", currentEntityId.getEntityType().name());
        ((ObjectNode) selfRegistrationSettings.getJsonValue()).put("entityId", currentEntityId.getId().toString());
        return adminSettingsService.saveAdminSettings(tenantId, selfRegistrationSettings);
    }

    private void saveSelfRegistrationParams(TenantId tenantId, EntityId entityId, SelfRegistrationParams selfRegistrationParams) {
        String privacyPolicy = selfRegistrationParams.getPrivacyPolicy();
        selfRegistrationParams.setPrivacyPolicy(null);
        String selfRegistrationJson;
        String privacyPolicyJson;
        String termsOfUse = selfRegistrationParams.getTermsOfUse();
        selfRegistrationParams.setTermsOfUse(null);
        String termsOfUseJson;
        try {
            selfRegistrationJson = JacksonUtil.toString(selfRegistrationParams);
            privacyPolicyJson = JacksonUtil.toString(JacksonUtil.newObjectNode().put(PRIVACY_POLICY, privacyPolicy));
            termsOfUseJson = JacksonUtil.toString(JacksonUtil.newObjectNode().put(TERMS_OF_USE, termsOfUse));
        } catch (IllegalArgumentException e) {
            log.error("Unable to convert Self Registration Params to JSON!", e);
            throw new IncorrectParameterException("Unable to convert Self Registration Params to JSON!");
        }
        List<AttributeKvEntry> attributes = getAttributeKvEntries(privacyPolicyJson, termsOfUseJson, selfRegistrationJson);
        saveEntityAttributes(tenantId, entityId, attributes);
    }

    private List<AttributeKvEntry> getAttributeKvEntries(String privacyPolicy, String termsOfUse, String selfRegistrationValue) {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(SELF_REGISTRATION_PARAMS, selfRegistrationValue), ts));
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(PRIVACY_POLICY, privacyPolicy), ts));
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(TERMS_OF_USE, termsOfUse), ts));
        return attributes;
    }

    private void saveEntityAttributes(TenantId tenantId, EntityId entityId, List<AttributeKvEntry> attributes) {
        try {
            attributesService.save(tenantId, entityId, DataConstants.SERVER_SCOPE, attributes).get();
        } catch (Exception e) {
            log.error("Unable to save Self Registration Params to attributes!", e);
            throw new IncorrectParameterException("Unable to save Self Registration Params to attributes!");
        }
    }

    private EntityId getEntityIdByDomainName(TenantId sysTenantId, String domainName) {
        AdminSettings selfRegistrationSettings = adminSettingsService.findAdminSettingsByKey(sysTenantId, constructSelfRegistrationKey(domainName));
        EntityId entityId = null;
        if (selfRegistrationSettings != null) {
            String strEntityType = selfRegistrationSettings.getJsonValue().get("entityType").asText();
            String strEntityId = selfRegistrationSettings.getJsonValue().get("entityId").asText();
            entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        }
        return entityId;
    }

    private String getTenantPrivacyPolicy(TenantId tenantId, EntityId entityId) {
        return getEntityAttributeValue(tenantId, entityId, PRIVACY_POLICY);
    }

    private String getTenantTermsOfUse(TenantId tenantId, EntityId entityId) {
        return getEntityAttributeValue(tenantId, entityId, TERMS_OF_USE);
    }

    private SelfRegistrationParams getTenantSelfRegistrationParams(TenantId tenantId, EntityId entityId) {
        String json = getEntityAttributeValue(tenantId, entityId, SELF_REGISTRATION_PARAMS);
        return constructSelfRegistrationParams(json);
    }

    private String getEntityAttributeValue(TenantId tenantId, EntityId entityId, String key) {
        List<AttributeKvEntry> attributeKvEntries;
        try {
            attributeKvEntries = attributesService.find(tenantId, entityId, DataConstants.SERVER_SCOPE, Collections.singletonList(key)).get();
        } catch (Exception e) {
            log.error("Unable to read Self Registration Params from attributes!", e);
            throw new IncorrectParameterException("Unable to read Self Registration Params from attributes!");
        }
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private SelfRegistrationParams constructSelfRegistrationParams(String json) {
        SelfRegistrationParams result = null;
        if (!StringUtils.isEmpty(json)) {
            try {
                result = JacksonUtil.fromString(json, SelfRegistrationParams.class);
            } catch (IllegalArgumentException e) {
                log.error("Unable to read Self Registration Params from JSON!", e);
                throw new IncorrectParameterException("Unable to read Self Registration Params from JSON!");
            }
        }
        if (result == null) {
            result = new SelfRegistrationParams();
        }
        return result;
    }

    private String constructSelfRegistrationKey(String domainName) {
        return DataConstants.SELF_REGISTRATION_DOMAIN_NAME_PREFIX + domainName;
    }
}
