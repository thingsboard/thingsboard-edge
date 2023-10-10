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
package org.thingsboard.server.dao.wl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.thingsboard.server.dao.entity.AbstractEntityService.checkConstraintViolation;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseWhiteLabelingService extends AbstractCachedService<WhiteLabelingCompositeKey, WhiteLabeling, WhiteLabelingEvictEvent>  implements WhiteLabelingService {

    private static final String ALLOW_WHITE_LABELING = "allowWhiteLabeling";
    private static final String ALLOW_CUSTOMER_WHITE_LABELING = "allowCustomerWhiteLabeling";

    private final AdminSettingsService adminSettingsService;
    private final WhiteLabelingDao whiteLabelingDao;
    private final TenantService tenantService;
    private final CustomerService customerService;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    public LoginWhiteLabelingParams getSystemLoginWhiteLabelingParams(TenantId tenantId) {
        WhiteLabeling whiteLabeling = findById(tenantId, TenantId.SYS_TENANT_ID, WhiteLabelingType.LOGIN);
        return constructLoginWlParams(whiteLabeling != null ? whiteLabeling.getSettings() : null);
    }

    @Override
    public WhiteLabelingParams getSystemWhiteLabelingParams(TenantId tenantId) {
        WhiteLabeling whiteLabeling = findById(tenantId, TenantId.SYS_TENANT_ID, WhiteLabelingType.GENERAL);
        return constructWlParams(whiteLabeling != null ? whiteLabeling.getSettings() : null, true);
    }

    @Override
    public WhiteLabelingParams getTenantWhiteLabelingParams(TenantId tenantId) {
        return getEntityWhiteLabelParams(tenantId, tenantId);
    }

    @Override
    public WhiteLabelingParams getCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId) {
        return getEntityWhiteLabelParams(tenantId, customerId);
    }

    @Override
    public WhiteLabelingParams getMergedSystemWhiteLabelingParams(TenantId tenantId, String logoImageChecksum, String faviconChecksum) {
        WhiteLabelingParams result = getSystemWhiteLabelingParams(tenantId);
        result.prepareImages(logoImageChecksum, faviconChecksum);
        return result;
    }

    @Override
    public LoginWhiteLabelingParams getTenantLoginWhiteLabelingParams(TenantId tenantId) {
        return getEntityLoginWhiteLabelParams(tenantId, tenantId);
    }

    @Override
    public LoginWhiteLabelingParams getCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId) {
        return getEntityLoginWhiteLabelParams(tenantId, customerId);
    }

    @Override
    public LoginWhiteLabelingParams getMergedLoginWhiteLabelingParams(TenantId tenantId, String domainName, String logoImageChecksum, String faviconChecksum) throws Exception {
        LoginWhiteLabelingParams result;
        WhiteLabeling existingLoginWLSettings;
        if (validateDomain(domainName) && ((existingLoginWLSettings =  whiteLabelingDao.findByDomain(tenantId, domainName)) != null)) {
            EntityId entityId = existingLoginWLSettings.getEntityId();
            result = getEntityLoginWhiteLabelParams(tenantId, entityId);
            if (entityId.getEntityType().equals(EntityType.CUSTOMER)) {
                Customer customer = customerService.findCustomerById(tenantId, (CustomerId) entityId);
                if (customer.isSubCustomer()) {
                    result.merge(getCustomerHierarchyLoginWhileLabelingParams(tenantId, customer.getParentCustomerId(), result));
                }
            }
            result.merge(getSystemLoginWhiteLabelingParams(tenantId));
        } else {
            result = getSystemLoginWhiteLabelingParams(tenantId);
        }
        result.merge(getSystemWhiteLabelingParams(tenantId));
        result.prepareImages(logoImageChecksum, faviconChecksum);
        return result;
    }

    private LoginWhiteLabelingParams getCustomerHierarchyLoginWhileLabelingParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams childCustomerWLLParams) throws Exception {
        LoginWhiteLabelingParams entityLoginWhiteLabelParams = getEntityLoginWhiteLabelParams(tenantId, customerId);
        childCustomerWLLParams.merge(entityLoginWhiteLabelParams);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            return getCustomerHierarchyLoginWhileLabelingParams(tenantId, customer.getParentCustomerId(), childCustomerWLLParams);
        } else {
            return childCustomerWLLParams;
        }
    }

    @Override
    public WhiteLabelingParams getMergedTenantWhiteLabelingParams(TenantId tenantId, String logoImageChecksum, String faviconChecksum) throws Exception {
        WhiteLabelingParams result = getTenantWhiteLabelingParams(tenantId);
        result.merge(getSystemWhiteLabelingParams(tenantId));
        result.prepareImages(logoImageChecksum, faviconChecksum);
        return result;
    }

    @Override
    public WhiteLabelingParams getMergedCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, String logoImageChecksum, String faviconChecksum) throws Exception {
        WhiteLabelingParams result = getCustomerWhiteLabelingParams(tenantId, customerId);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            result.merge(getMergedCustomerHierarchyWhileLabelingParams(tenantId, customer.getParentCustomerId(), result));
        }
        result.merge(getTenantWhiteLabelingParams(tenantId)).merge(getSystemWhiteLabelingParams(tenantId));
        result.prepareImages(logoImageChecksum, faviconChecksum);
        return result;
    }

    private WhiteLabelingParams getMergedCustomerHierarchyWhileLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams childCustomerWLParams) throws Exception {
        WhiteLabelingParams entityWhiteLabelParams = getEntityWhiteLabelParams(tenantId, customerId);
        childCustomerWLParams.merge(entityWhiteLabelParams);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            return getMergedCustomerHierarchyWhileLabelingParams(tenantId, customer.getParentCustomerId(), childCustomerWLParams);
        } else {
            return childCustomerWLParams;
        }
    }

    @Override
    public WhiteLabelingParams saveSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        prepareChecksums(whiteLabelingParams);
        saveWhiteLabelParams(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, whiteLabelingParams);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(TenantId.SYS_TENANT_ID).entityId(TenantId.SYS_TENANT_ID)
                .edgeEventType(EdgeEventType.WHITE_LABELING).actionType(ActionType.UPDATED).build());
        return getSystemWhiteLabelingParams(TenantId.SYS_TENANT_ID);
    }

    @Override
    public LoginWhiteLabelingParams saveSystemLoginWhiteLabelingParams(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        if (!StringUtils.isBlank(loginWhiteLabelingParams.getDomainName())) {
            throw new DataValidationException("Domain name is prohibited for system level");
        }
        prepareChecksums(loginWhiteLabelingParams);
        saveLoginWhiteLabelParams(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, loginWhiteLabelingParams);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(TenantId.SYS_TENANT_ID).entityId(TenantId.SYS_TENANT_ID)
                .edgeEventType(EdgeEventType.LOGIN_WHITE_LABELING).actionType(ActionType.UPDATED).build());
        return getSystemLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID);
    }

    @Override
    public LoginWhiteLabelingParams saveTenantLoginWhiteLabelingParams(TenantId tenantId, LoginWhiteLabelingParams loginWhiteLabelingParams) throws Exception {
        saveEntityLoginWhiteLabelingParams(tenantId, tenantId, loginWhiteLabelingParams);
        return getTenantLoginWhiteLabelingParams(tenantId);
    }

    @Override
    public LoginWhiteLabelingParams saveCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams loginWhiteLabelingParams) throws Exception {
        saveEntityLoginWhiteLabelingParams(tenantId, customerId, loginWhiteLabelingParams);
        return getCustomerLoginWhiteLabelingParams(tenantId, customerId);
    }

    private boolean validateDomain(String domainName) {
        try {
            LoginWhiteLabelingParams systemParams = getSystemLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID);
            if (systemParams != null) {
                if (isBaseUrlMatchesDomain(systemParams.getBaseUrl(), domainName)) {
                    return false;
                }
            }
            AdminSettings generalSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "general");
            String baseUrl = generalSettings.getJsonValue().get("baseUrl").asText();
            return !isBaseUrlMatchesDomain(baseUrl, domainName);
        } catch (Exception e) {
            log.warn("Failed to validate domain.", e);
            return false;
        }
    }

    private boolean isBaseUrlMatchesDomain(String baseUrl, String domainName) {
        String baseUrlDomainName = this.domainNameFromBaseUrl(baseUrl);
        return baseUrlDomainName != null && baseUrlDomainName.equalsIgnoreCase(domainName);
    }

    private String domainNameFromBaseUrl(String baseUrl) {
        if (StringUtils.isNotBlank(baseUrl)) {
            try {
                return URI.create(baseUrl).getHost();
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private void saveEntityLoginWhiteLabelingParams(TenantId tenantId, EntityId entityId, LoginWhiteLabelingParams loginWhiteLabelParams) {
        if (loginWhiteLabelParams.getDomainName() == null) {
            throw new IncorrectParameterException("Domain name could not be empty!");
        }
        if (!validateDomain(loginWhiteLabelParams.getDomainName())) {
            throw new IncorrectParameterException("Current domain name [" + loginWhiteLabelParams.getDomainName() + "] already used in the system level!");
        }

        prepareChecksums(loginWhiteLabelParams);
        saveLoginWhiteLabelParams(tenantId, entityId, loginWhiteLabelParams);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(entityId)
                .edgeEventType(EdgeEventType.LOGIN_WHITE_LABELING).actionType(ActionType.UPDATED).build());
    }

    @Override
    public WhiteLabelingParams saveTenantWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams) {
        prepareChecksums(whiteLabelingParams);
        saveWhiteLabelParams(tenantId, tenantId, whiteLabelingParams);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(tenantId)
                .edgeEventType(EdgeEventType.WHITE_LABELING).actionType(ActionType.UPDATED).build());
        return getTenantWhiteLabelingParams(tenantId);
    }

    @Override
    public WhiteLabelingParams saveCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams) {
        prepareChecksums(whiteLabelingParams);
        saveWhiteLabelParams(tenantId, customerId, whiteLabelingParams);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(customerId)
                .edgeEventType(EdgeEventType.WHITE_LABELING).actionType(ActionType.UPDATED).build());
        return getCustomerWhiteLabelingParams(tenantId, customerId);
    }

    @Override
    public WhiteLabelingParams mergeSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        return whiteLabelingParams;
    }

    @Override
    public WhiteLabelingParams mergeTenantWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams) {
        return whiteLabelingParams.merge(getSystemWhiteLabelingParams(tenantId));
    }

    @Override
    public WhiteLabelingParams mergeCustomerWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams) {
        WhiteLabelingParams otherWlParams;
        try {
            otherWlParams = getTenantWhiteLabelingParams(tenantId);
            return whiteLabelingParams.merge(otherWlParams).
                    merge(getSystemWhiteLabelingParams(tenantId));
        } catch (Exception e) {
            log.error("Unable to merge Customer White Labeling Params!", e);
            throw new RuntimeException("Unable to merge Customer White Labeling Params!", e);
        }
    }

    @Override
    public void deleteDomainWhiteLabelingByEntityId(TenantId tenantId, EntityId entityId) {
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(entityId.getEntityType().name(), entityId.getId(), WhiteLabelingType.LOGIN);
        if (whiteLabelingDao.findById(tenantId, key) != null) {
            whiteLabelingDao.removeById(tenantId, key);
        }
        publishEvictEvent(new WhiteLabelingEvictEvent(key));
    }

    private WhiteLabelingParams constructWlParams(JsonNode json, boolean isSystem) {
        WhiteLabelingParams result = null;
        if (json != null) {
            try {
                result = JacksonUtil.treeToValue(json, WhiteLabelingParams.class);
                if (isSystem) {
                    if (!json.has("helpLinkBaseUrl")) {
                        result.setHelpLinkBaseUrl("https://thingsboard.io");
                    }
                    if (!json.has("uiHelpBaseUrl")) {
                        result.setUiHelpBaseUrl(null);
                    }
                    if (!json.has("enableHelpLinks")) {
                        result.setEnableHelpLinks(true);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Unable to read White Labeling Params from JSON!", e);
                throw new IncorrectParameterException("Unable to read White Labeling Params from JSON!");
            }
        }
        if (result == null) {
            result = new WhiteLabelingParams();
            if (isSystem) {
                result.setHelpLinkBaseUrl("https://thingsboard.io");
                result.setUiHelpBaseUrl(null);
                result.setEnableHelpLinks(true);
            }
        }
        return result;
    }

    private WhiteLabelingParams getEntityWhiteLabelParams(TenantId tenantId, EntityId entityId) {
        JsonNode jsonNode = null;
        if (isWhiteLabelingAllowed(tenantId, entityId)) {
            WhiteLabeling whiteLabeling = findById(tenantId, entityId, WhiteLabelingType.GENERAL);
            if (whiteLabeling != null) {
                jsonNode = whiteLabeling.getSettings();
            }
        }
        return constructWlParams(jsonNode, false);
    }

    private LoginWhiteLabelingParams constructLoginWlParams(JsonNode json) {
        LoginWhiteLabelingParams result = null;
        if (json != null) {
            try {
                result = JacksonUtil.treeToValue(json, LoginWhiteLabelingParams.class);
            } catch (IllegalArgumentException e) {
                log.error("Unable to read Login White Labeling Params from JSON!", e);
                throw new IncorrectParameterException("Unable to read Login White Labeling Params from JSON!");
            }
        }
        if (result == null) {
            result = new LoginWhiteLabelingParams();
        }
        return result;
    }


    private LoginWhiteLabelingParams getEntityLoginWhiteLabelParams(TenantId tenantId, EntityId entityId) {
        JsonNode jsonNode = null;
        if (isWhiteLabelingAllowed(tenantId, entityId)) {
            WhiteLabeling whiteLabeling = findById(tenantId, entityId, WhiteLabelingType.LOGIN);
            if (whiteLabeling != null) {
                jsonNode = whiteLabeling.getSettings();
            }
        }
        return constructLoginWlParams(jsonNode);
    }

    public boolean isWhiteLabelingAllowed(TenantId tenantId, EntityId entityId) {
        if (entityId.getEntityType().equals(EntityType.CUSTOMER)) {
            Customer customer = customerService.findCustomerById(tenantId, (CustomerId) entityId);
            if (customer == null) {
                return false;
            }
            if (customer.isSubCustomer()) {
                if (isWhiteLabelingAllowed(tenantId, customer.getParentCustomerId())) {
                    return isWhiteLabelingAllowed(tenantId, customer.getCustomerId());
                } else {
                    return false;
                }
            } else {
                if (isCustomerWhiteLabelingAllowed(customer.getTenantId())) {
                    JsonNode allowWhiteLabelJsonNode = customer.getAdditionalInfo().get(ALLOW_WHITE_LABELING);
                    if (allowWhiteLabelJsonNode == null) {
                        return true;
                    } else {
                        return allowWhiteLabelJsonNode.asBoolean();
                    }
                } else {
                    return false;
                }
            }
        } else if (entityId.getEntityType().equals(EntityType.TENANT)) {
            Tenant tenant = tenantService.findTenantById((TenantId) entityId);
            JsonNode allowWhiteLabelJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_WHITE_LABELING) : null;
            if (allowWhiteLabelJsonNode == null) {
                return true;
            } else {
                return allowWhiteLabelJsonNode.asBoolean();
            }
        }
        log.error("Unsupported entity type [{}]!", entityId.getEntityType().name());
        throw new IncorrectParameterException("Unsupported entity type [" + entityId.getEntityType().name() + "]!");
    }

    @Override
    public boolean isCustomerWhiteLabelingAllowed(TenantId tenantId) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        JsonNode allowWhiteLabelJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_WHITE_LABELING) : null;
        if (allowWhiteLabelJsonNode == null) {
            return true;
        } else {
            if (allowWhiteLabelJsonNode.asBoolean()) {
                JsonNode allowCustomerWhiteLabelJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_CUSTOMER_WHITE_LABELING) : null;
                if (allowCustomerWhiteLabelJsonNode == null) {
                    return true;
                } else {
                    return allowCustomerWhiteLabelJsonNode.asBoolean();
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean isWhiteLabelingConfigured(TenantId tenantId) {
        return findById(tenantId, TenantId.SYS_TENANT_ID, WhiteLabelingType.GENERAL) != null;
    }

    @Override
    public JsonNode saveMailTemplates(TenantId tenantId, JsonNode mailTemplates) {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setEntityId(tenantId);
        whiteLabeling.setType(WhiteLabelingType.MAIL_TEMPLATES);
        whiteLabeling.setSettings(mailTemplates);

        return whiteLabelingDao.save(tenantId, whiteLabeling).getSettings();
    }

    @Override
    public JsonNode getCurrentTenantMailTemplates(TenantId tenantId, boolean systemByDefault) {
        JsonNode mailTemplatesSettings = findMailTemplatesByTenantId(tenantId, tenantId);
        if (mailTemplatesSettings.isEmpty()) {
            if (systemByDefault) {
                mailTemplatesSettings = findMailTemplatesByTenantId(tenantId, TenantId.SYS_TENANT_ID);
            }
        }
        return mailTemplatesSettings;
    }

    @Override
    public JsonNode findMailTemplatesByTenantId(TenantId tenantId, TenantId settingsTenantId) {
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey();
        key.setEntityId(settingsTenantId.getId());
        key.setEntityType(settingsTenantId.getEntityType().name());
        key.setType(WhiteLabelingType.MAIL_TEMPLATES);

        WhiteLabeling mailTemplates = whiteLabelingDao.findById(tenantId, key);
        return mailTemplates == null ? JacksonUtil.newObjectNode() : mailTemplates.getSettings();
    }

    @Override
    public JsonNode getMergedTenantMailTemplates(TenantId tenantId) throws ThingsboardException {
        JsonNode mailTemplatesSettings = getCurrentTenantMailTemplates(tenantId, true);
        if (mailTemplatesSettings.isEmpty()) {
            throw new ThingsboardException("Failed to get tenant mail templates configuration. Settings not found!", ThingsboardErrorCode.GENERAL);
        }
        JsonNode useSystemMailSettingsNode = mailTemplatesSettings.get("useSystemMailSettings");
        if (useSystemMailSettingsNode == null || useSystemMailSettingsNode.asBoolean()) {
            JsonNode systemMailTemplates = findMailTemplatesByTenantId(tenantId, TenantId.SYS_TENANT_ID);
            if (systemMailTemplates.isEmpty()) {
                throw new ThingsboardException("Failed to get system mail templates configuration. Settings not found!", ThingsboardErrorCode.GENERAL);
            }
            mailTemplatesSettings = systemMailTemplates;
        }
        return mailTemplatesSettings;
    }

    private void saveLoginWhiteLabelParams(TenantId tenantId, EntityId entityId, LoginWhiteLabelingParams whiteLabelingParams) {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setEntityId(entityId);
        whiteLabeling.setType(WhiteLabelingType.LOGIN);
        whiteLabeling.setSettings(JacksonUtil.valueToTree(whiteLabelingParams));
        whiteLabeling.setDomain(whiteLabelingParams.getDomainName());

        doSaveWhiteLabelingSettings(tenantId, whiteLabeling);
    }

    private void saveWhiteLabelParams(TenantId tenantId, EntityId entityId, WhiteLabelingParams whiteLabelingParams) {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setEntityId(entityId);
        whiteLabeling.setType(WhiteLabelingType.GENERAL);
        whiteLabeling.setSettings(JacksonUtil.valueToTree(whiteLabelingParams));

        doSaveWhiteLabelingSettings(tenantId, whiteLabeling);
    }

    private void doSaveWhiteLabelingSettings(TenantId tenantId, WhiteLabeling whiteLabeling) {
        try {
            WhiteLabeling saved = whiteLabelingDao.save(tenantId, whiteLabeling);
            WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(saved.getEntityId().getEntityType().name(), saved.getEntityId().getId(), saved.getType());
            publishEvictEvent(new WhiteLabelingEvictEvent(key));
        } catch (Exception t) {
            checkConstraintViolation(t,
                    "white_labeling_domain_name_key", "Such domain name already registered in the system!");
            throw t;
        }
    }

    private WhiteLabeling findById(TenantId tenantId, EntityId entityId, WhiteLabelingType type) {
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(entityId.getEntityType().name(), entityId.getId(), type);
        log.trace("Executing findById for key [{}] ", key);
        return cache.getAndPutInTransaction(key,
                () -> whiteLabelingDao.findById(tenantId,
                        key), true);
    }

    private <T extends WhiteLabelingParams> T prepareChecksums(T whiteLabelingParams) {
        String logoImageChecksum = null;
        String logoImageUrl = whiteLabelingParams.getLogoImageUrl();
        if (!StringUtils.isEmpty(logoImageUrl)) {
            logoImageChecksum = calculateSha1Checksum(logoImageUrl);
        }
        whiteLabelingParams.setLogoImageChecksum(logoImageChecksum);
        String faviconChecksum = null;
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

    @TransactionalEventListener(classes = WhiteLabelingEvictEvent.class)
    @Override
    public void handleEvictEvent(WhiteLabelingEvictEvent event) {
        cache.evict(event.getKey());
    }
}
