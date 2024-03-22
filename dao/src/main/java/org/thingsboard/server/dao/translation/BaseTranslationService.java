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
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.TranslationInfo;

import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.thingsboard.common.util.JacksonUtil.extractKeys;
import static org.thingsboard.common.util.JacksonUtil.merge;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseTranslationService implements TranslationService {

    public static final String LOCALE_FILES_DIRECTORY_PATH = "/public/assets/locale";
    public static final String DEFAULT_LOCALE_CODE = "en_US";
    public static JsonNode DEFAULT_LOCALE_TRANSLATION;
    public static final Set<String> DEFAULT_LOCALE_KEYS;
    public static final Map<String, TranslationInfo> LOCALES_INFO = new HashMap<>();

    static {
        for (Locale availableLocale : DateFormat.getAvailableLocales()) {
            LOCALES_INFO.put(availableLocale.toString(),
                    new TranslationInfo(availableLocale.toString(), availableLocale.getDisplayLanguage(), availableLocale.getDisplayCountry()));
        }
        DEFAULT_LOCALE_TRANSLATION = Objects.requireNonNull(readLocaleFile(getLocaleFilePath(DEFAULT_LOCALE_CODE)),
                "Failed to retrieve default locale translation!");
        DEFAULT_LOCALE_KEYS = extractKeys(DEFAULT_LOCALE_TRANSLATION);
    }

    private final CustomTranslationService customTranslationService;

    @Override
    public List<TranslationInfo> getSystemTranslationInfos() {
        List<TranslationInfo> translationInfos = new ArrayList<>();
        List<String> locales = customTranslationService.getCustomizedLocales(TenantId.SYS_TENANT_ID, null);
        for (String locale : locales) {
            translationInfos.add(buildLocaleInfo(locale, getSystemTranslation(locale)));
        }
        return translationInfos;
    }

    @Override
    public List<TranslationInfo> getTenantTranslationInfos(TenantId tenantId) {
        List<TranslationInfo> translationInfos = new ArrayList<>();
        List<String> locales = customTranslationService.getCustomizedLocales(tenantId, null);
        for (String locale : locales) {
            translationInfos.add(buildLocaleInfo(locale, getTenantTranslation(tenantId, locale)));
        }
        return translationInfos;
    }

    @Override
    public List<TranslationInfo> getCustomerTranslationInfos(TenantId tenantId, CustomerId customerId) {
        List<TranslationInfo> translationInfos = new ArrayList<>();
        List<String> locales = customTranslationService.getCustomizedLocales(tenantId, customerId);
        for (String locale : locales) {
            translationInfos.add(buildLocaleInfo(locale, getCustomerTranslation(tenantId, customerId, locale)));
        }
        return translationInfos;
    }

    @Override
    public JsonNode getFullSystemTranslation(String localeCode) {
        return merge(getSystemTranslation(localeCode), DEFAULT_LOCALE_TRANSLATION);
    }

    @Override
    public JsonNode getFullTenantTranslation(TenantId tenantId, String localeCode) {
        return merge(getTenantTranslation(tenantId, localeCode), DEFAULT_LOCALE_TRANSLATION);
    }

    @Override
    public JsonNode getFullCustomerTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        return merge(getCustomerTranslation(tenantId, customerId, localeCode), DEFAULT_LOCALE_TRANSLATION);
    }

    @Override
    public JsonNode getSystemTranslation(String localeCode) {
        JsonNode customTranslation = customTranslationService.getCurrentCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode)
                .getValue().deepCopy();
        return mergeWithSystemLanguageTranslationIfExists(localeCode, customTranslation);
    }

    @Override
    public JsonNode getTenantTranslation(TenantId tenantId, String localeCode) {
        JsonNode customTranslation = customTranslationService.getMergedTenantCustomTranslation(tenantId, localeCode);
        return mergeWithSystemLanguageTranslationIfExists(localeCode, customTranslation);
    }

    @Override
    public JsonNode getCustomerTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customTranslation = customTranslationService.getMergedCustomerCustomTranslation(tenantId, customerId, localeCode);
        return mergeWithSystemLanguageTranslationIfExists(localeCode, customTranslation);
    }

    private static String getLocaleFilePath(String localeCode) {
        return LOCALE_FILES_DIRECTORY_PATH + "/locale.constant-" + localeCode + ".json";
    }

    private JsonNode mergeWithSystemLanguageTranslationIfExists(String localeCode, JsonNode jsonNode) {
        JsonNode systemTranslation = readLocaleFile(getLocaleFilePath(localeCode));
        if (systemTranslation != null) {
            return merge(jsonNode, systemTranslation);
        }
        return jsonNode;
    }

    private static JsonNode readLocaleFile(String localeFilePath) {
        try (InputStream in = BaseTranslationService.class.getResourceAsStream(localeFilePath)) {
            if (in == null){
                return null;
            }
            return JacksonUtil.OBJECT_MAPPER.readTree(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read locale translation!", e);
        }
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
        long translated = DEFAULT_LOCALE_KEYS.stream()
                .filter(localeKeys::contains)
                .count();
        return (int) (((translated) * 100) / DEFAULT_LOCALE_KEYS.size());
    }
}