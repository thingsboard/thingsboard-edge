/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.translation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.translation.TranslationInfo;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.thingsboard.common.util.JacksonUtil.extractKeys;
import static org.thingsboard.common.util.JacksonUtil.merge;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseTranslationService implements TranslationService {

    public static final String DEFAULT_LOCALE_FILE_PATH = "/locale/locale.constant-en_US.json";
    public static JsonNode DEFAULT_LOCALE_TRANSLATION;
    public static final Set<String> DEFAULT_LOCALE_KEYS;
    public static final Map<String, TranslationInfo> LOCALES_INFO = new HashMap<>();

    static {
        Arrays.stream(DateFormat.getAvailableLocales()).forEach(locale -> LOCALES_INFO.put(locale.toString(),
                        new TranslationInfo(locale.toString(), locale.getDisplayLanguage(), locale.getDisplayCountry())));
        try {
            DEFAULT_LOCALE_TRANSLATION = JacksonUtil.toJsonNode(new ClassPathResource(DEFAULT_LOCALE_FILE_PATH).getFile());
        } catch (IOException e) {
            DEFAULT_LOCALE_TRANSLATION = JacksonUtil.newObjectNode();
        }
        DEFAULT_LOCALE_KEYS = extractKeys(DEFAULT_LOCALE_TRANSLATION);
    }

    private final CustomTranslationService customTranslationService;

    @Override
    public List<TranslationInfo> getSystemTranslationInfo() throws IOException {
        List<TranslationInfo> translationInfos = new ArrayList<>();
        List<String> locales = customTranslationService.getCustomizedLocales(TenantId.SYS_TENANT_ID, null);
        for (String locale : locales) {
            translationInfos.add(buildLocaleInfo(locale, getSystemTranslation(locale)));
        }
        return translationInfos;
    }

    @Override
    public List<TranslationInfo> getTenantTranslationInfo(TenantId tenantId) throws IOException {
        List<TranslationInfo> translationInfos = new ArrayList<>();
        List<String> locales = customTranslationService.getCustomizedLocales(tenantId, null);
        for (String locale : locales) {
            translationInfos.add(buildLocaleInfo(locale, getTenantTranslation(tenantId, locale)));
        }
        return translationInfos;
    }

    @Override
    public List<TranslationInfo> getCustomerTranslationInfo(TenantId tenantId, CustomerId customerId) throws IOException {
        List<TranslationInfo> translationInfos = new ArrayList<>();
        List<String> locales = customTranslationService.getCustomizedLocales(tenantId, customerId);
        for (String locale : locales) {
            translationInfos.add(buildLocaleInfo(locale, getCustomerTranslation(tenantId, customerId, locale)));
        }
        return translationInfos;
    }

    @Override
    public JsonNode getFullSystemTranslation(String localeCode) throws IOException {
        return merge(getSystemTranslation(localeCode), DEFAULT_LOCALE_TRANSLATION);
    }

    @Override
    public JsonNode getFullTenantTranslation(TenantId tenantId, String localeCode) throws IOException {
        return merge(getTenantTranslation(tenantId, localeCode), DEFAULT_LOCALE_TRANSLATION);
    }

    @Override
    public JsonNode getFullCustomerTranslation(TenantId tenantId, CustomerId customerId, String localeCode) throws IOException {
        return merge(getCustomerTranslation(tenantId, customerId, localeCode), DEFAULT_LOCALE_TRANSLATION);
    }

    @Override
    public JsonNode getSystemTranslation(String localeCode) throws IOException {
        CustomTranslation customTranslation = customTranslationService.getCurrentCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode);
        return mergeWithSystemLanguageTranslationIfExists(localeCode, customTranslation.getValue());
    }

    @Override
    public JsonNode getTenantTranslation(TenantId tenantId, String localeCode) throws IOException {
        JsonNode customTranslation = customTranslationService.getMergedTenantCustomTranslation(tenantId, localeCode);
        return mergeWithSystemLanguageTranslationIfExists(localeCode, customTranslation);
    }

    @Override
    public JsonNode getCustomerTranslation(TenantId tenantId, CustomerId customerId, String localeCode) throws IOException {
        JsonNode customTranslation = customTranslationService.getMergedCustomerCustomTranslation(tenantId, customerId, localeCode);
        return mergeWithSystemLanguageTranslationIfExists(localeCode, customTranslation);
    }

    private JsonNode mergeWithSystemLanguageTranslationIfExists(String localeCode, JsonNode jsonNode) throws IOException {
        String systemLangTranslationFile = "/locale/locale.constant-" + localeCode + ".json";
        if (ResourceUtils.resourceExists(this, new ClassPathResource(systemLangTranslationFile).getPath())) {
            return merge(jsonNode, Objects.requireNonNull(JacksonUtil.toJsonNode(new ClassPathResource(systemLangTranslationFile).getFile())));
        }
        return jsonNode;
    }

    private TranslationInfo buildLocaleInfo(String localeCode, JsonNode translation) {
        TranslationInfo translationInfo = LOCALES_INFO.getOrDefault(localeCode, new TranslationInfo());
        return TranslationInfo.builder()
                .localeCode(localeCode)
                .country(translationInfo.getCountry())
                .language(translationInfo.getLanguage())
                .progress(calculateTranslationProgress(translation))
                .build();
    }

    private int calculateTranslationProgress(JsonNode translation) {
        Set<String> localeKeys = extractKeys(translation);
        Set<String> translatedKeys = new HashSet<>(DEFAULT_LOCALE_KEYS);
        translatedKeys.retainAll(localeKeys);
        return ((translatedKeys.size()) * 100)/ DEFAULT_LOCALE_KEYS.size();
    }
}
