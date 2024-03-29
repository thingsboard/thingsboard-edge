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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.JacksonUtil.extractKeys;
import static org.thingsboard.common.util.JacksonUtil.merge;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseTranslationService implements TranslationService {

    private static final ReentrantLock progressCalculationLock = new ReentrantLock();
    public static final String LOCALE_FILES_DIRECTORY_PATH = "/public/assets/locale";
    public static final Pattern LOCALE_FILE_PATTERN = Pattern.compile("locale\\.constant-(.*?)\\.json");
    public static final String DEFAULT_LOCALE_CODE = "en_US";
    public static final JsonNode DEFAULT_LOCALE_TRANSLATION;
    public static final Set<String> DEFAULT_LOCALE_KEYS;
    public static final Set<String> SYSTEM_LOCALE_LIST;
    private final ConcurrentMap<String, Integer> translationProgressMap = new ConcurrentHashMap<>();
    private final CustomTranslationService customTranslationService;

    static {
        SYSTEM_LOCALE_LIST = readSystemLocaleCodes();
        DEFAULT_LOCALE_TRANSLATION = Objects.requireNonNull(getSystemLocaleTranslation(DEFAULT_LOCALE_CODE),
                "Failed to retrieve default locale translation!");
        DEFAULT_LOCALE_KEYS = extractKeys(DEFAULT_LOCALE_TRANSLATION);
    }

    @Override
    public List<TranslationInfo> getTranslationInfos(TenantId tenantId, CustomerId customerId) {
        List<TranslationInfo> translationInfos = new ArrayList<>();
        Set<String> customizedLocales = customTranslationService.getCustomizedLocales(tenantId, customerId);
        for (String locale : customizedLocales) {
            JsonNode customTranslation = getLocaleTranslation(tenantId, customerId, locale);
            translationInfos.add(createTranslationInfo(locale, customTranslation));
        }
        Set<String> notCustomizedLocales = new HashSet<>(SYSTEM_LOCALE_LIST);
        notCustomizedLocales.removeAll(customizedLocales);
        for (String locale : notCustomizedLocales) {
            translationInfos.add(createTranslationInfo(locale, getSystemLocaleTranslationProgress(locale)));
        }
        return translationInfos;
    }

    @Override
    public JsonNode getLocaleTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customTranslation;
        if (tenantId.isSysTenantId()) {
            customTranslation = customTranslationService.getCurrentCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode).getValue().deepCopy();
        } else if (customerId == null || customerId.isNullUid()) {
            customTranslation = customTranslationService.getMergedTenantCustomTranslation(tenantId, localeCode);
        } else {
            customTranslation = customTranslationService.getMergedCustomerCustomTranslation(tenantId, customerId, localeCode);
        }
        return mergeWithSystemLanguageTranslationIfExists(localeCode, customTranslation);
    }

    @Override
    public JsonNode getFullTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode translation = getLocaleTranslation(tenantId, customerId, localeCode);
        return merge(translation, DEFAULT_LOCALE_TRANSLATION);
    }

    private TranslationInfo createTranslationInfo(String locale, JsonNode translation) {
        return createTranslationInfo(locale, calculateTranslationProgress(translation));
    }

    private TranslationInfo createTranslationInfo(String locale, int progress) {
        return new TranslationInfo(locale, progress);
    }

    private static String getSystemLocaleFilePath(String localeCode) {
        return LOCALE_FILES_DIRECTORY_PATH + "/locale.constant-" + localeCode + ".json";
    }

    private JsonNode mergeWithSystemLanguageTranslationIfExists(String localeCode, JsonNode jsonNode) {
        JsonNode systemTranslation = getSystemLocaleTranslation(localeCode);
        if (systemTranslation != null) {
            return merge(jsonNode, systemTranslation);
        }
        return jsonNode;
    }

    private static JsonNode getSystemLocaleTranslation(String localeCode) {
        String filePath = getSystemLocaleFilePath(localeCode);
        try (InputStream in = BaseTranslationService.class.getResourceAsStream(filePath)) {
            if (in == null) {
                return null;
            }
            return JacksonUtil.OBJECT_MAPPER.readTree(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read locale translation!", e);
        }
    }

    private static Set<String> readSystemLocaleCodes() {
        List<String> filenames = new ArrayList<>();
        try (InputStream in = BaseTranslationService.class.getResourceAsStream(LOCALE_FILES_DIRECTORY_PATH);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get list of system locales!", e);
        }
        return filenames.stream().map(filename -> {
            Matcher matcher = LOCALE_FILE_PATTERN.matcher(filename);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public int getSystemLocaleTranslationProgress(String locale) {
        Integer progress = translationProgressMap.get(locale);
        if (progress == null) {
            progressCalculationLock.lock();
            try {
                progress = translationProgressMap.get(locale);
                if (progress == null) {
                    JsonNode systemLocaleTranslation = getSystemLocaleTranslation(locale);
                    progress = calculateTranslationProgress(systemLocaleTranslation);
                    translationProgressMap.put(locale, progress);
                    log.debug("Fetch locale {} translation progress into cache: {}", locale, progress);
                }
            } finally {
                progressCalculationLock.unlock();
            }
        }
        return progress;
    }

    private int calculateTranslationProgress(JsonNode translation) {
        Set<String> localeKeys = extractKeys(translation);
        long translated = DEFAULT_LOCALE_KEYS.stream()
                .filter(localeKeys::contains)
                .count();
        return (int) (((translated) * 100) / DEFAULT_LOCALE_KEYS.size());
    }
}