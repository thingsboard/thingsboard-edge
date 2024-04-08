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
package org.thingsboard.server.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.translation.TranslationCacheKey;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.JacksonUtil.extractKeys;
import static org.thingsboard.common.util.JacksonUtil.newObjectNode;
import static org.thingsboard.common.util.JacksonUtil.update;

@Service
@Slf4j
@TbCoreComponent
public class DefaultTbTranslationService extends AbstractTbEntityService implements TbTranslationService {

    public static final String LOCALE_FILES_DIRECTORY_PATH = "public/assets/locale";
    public static final Pattern LOCALE_FILE_PATTERN = Pattern.compile("locale\\.constant-(.*?)\\.json");
    public static final String DEFAULT_LOCALE_CODE = "en_US";
    private static final Set<String> DEFAULT_LOCALE_KEYS;
    private static final Map<String, JsonNode> TRANSLATION_VALUE_MAP = new HashMap<>();
    private static final Map<String, TranslationInfo> TRANSLATION_INFO_MAP = new HashMap<>();
    private final Cache<TranslationCacheKey, String> etagCache;
    private final CustomTranslationService customTranslationService;
    private final TbClusterService clusterService;

    static {
        JsonNode defaultTranslation = readSystemLocaleTranslation(DEFAULT_LOCALE_CODE);
        DEFAULT_LOCALE_KEYS = extractKeys(defaultTranslation);

        Set<String> systemLocaleCodes = getSystemLocaleCodes();
        for (String localeCode : systemLocaleCodes) {
            JsonNode systemLocaleTranslation = readSystemLocaleTranslation(localeCode);
            TRANSLATION_INFO_MAP.put(localeCode, createTranslationInfo(localeCode, systemLocaleTranslation));
            TRANSLATION_VALUE_MAP.put(localeCode, update(defaultTranslation.deepCopy(), systemLocaleTranslation));
        }
    }

    public DefaultTbTranslationService(TbClusterService clusterService, CustomTranslationService customTranslationService,
                                       @Value("${cache.translation.etag.timeToLiveInMinutes:44640}") int cacheTtl,
                                       @Value("${cache.translation.etag.maxSize:1000000}") int cacheMaxSize) {
        this.clusterService = clusterService;
        this.customTranslationService = customTranslationService;
        this.etagCache = Caffeine.newBuilder()
                .expireAfterAccess(cacheTtl, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
    }

    @Override
    public List<TranslationInfo> getTranslationInfos(TenantId tenantId, CustomerId customerId) {
        Map<String, TranslationInfo> translationInfos = JacksonUtil.clone(TRANSLATION_INFO_MAP);

        Set<String> customizedLocales = customTranslationService.getCustomizedLocales(tenantId, customerId);
        for (String customizedLocale : customizedLocales) {
            JsonNode customTranslation = getMergedCustomTranslation(tenantId, customerId, customizedLocale);
            if (translationInfos.containsKey(customizedLocale)) {
                JsonNode systemTranslation = readSystemLocaleTranslation(customizedLocale);
                customTranslation = update(systemTranslation, customTranslation);
            }
            translationInfos.put(customizedLocale, createTranslationInfo(customizedLocale, customTranslation));
        }
        return new ArrayList<>(translationInfos.values());
    }

    @Override
    public JsonNode getLoginTranslation(String localeCode) {
        ObjectNode loginPageTranslation = newObjectNode();
        JsonNode defaultTranslation = TRANSLATION_VALUE_MAP.getOrDefault(localeCode, TRANSLATION_VALUE_MAP.get(DEFAULT_LOCALE_CODE));
        loginPageTranslation.set("login", defaultTranslation.get("login"));
        loginPageTranslation.set("signup", defaultTranslation.get("signup"));
        return loginPageTranslation;
    }

    @Override
    public JsonNode getFullTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customTranslation = getMergedCustomTranslation(tenantId, customerId, localeCode);
        JsonNode defaultTranslation = TRANSLATION_VALUE_MAP.getOrDefault(localeCode, TRANSLATION_VALUE_MAP.get(DEFAULT_LOCALE_CODE)).deepCopy();
        return update(defaultTranslation, customTranslation);
    }

    @Override
    public JsonNode getTranslationForBasicEdit(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode fullTranslation = getFullTranslation(tenantId, customerId, localeCode).deepCopy();
        JsonNode currentCustomTranslation = customTranslationService.getCurrentCustomTranslation(tenantId, customerId, localeCode).getValue();
        JsonNode originalTranslation = TRANSLATION_VALUE_MAP.get(DEFAULT_LOCALE_CODE);
        JsonNode parentTranslation = getParentTranslation(tenantId, customerId, localeCode);
        JsonNode translatedOnlyTranslation = getTranslatedOnlyTranslation(tenantId, customerId, localeCode);

        getTranslationForEdit(fullTranslation, currentCustomTranslation, parentTranslation, originalTranslation, translatedOnlyTranslation);
        return fullTranslation;
    }

    private void getTranslationForEdit(JsonNode fullTranslation, JsonNode customTranslation, JsonNode parentTranslation, JsonNode originTranslation, JsonNode translatedTranslation) {
        Iterator<String> fieldNamesIterator = fullTranslation.fieldNames();
        while (fieldNamesIterator.hasNext()) {
            String fieldName = fieldNamesIterator.next();
            JsonNode fullNode = fullTranslation.get(fieldName);
            JsonNode customNode = customTranslation == null ? null : customTranslation.get(fieldName);
            JsonNode parentNode = parentTranslation == null ? null : parentTranslation.get(fieldName);
            JsonNode originNode = originTranslation == null ? null : originTranslation.get(fieldName);
            JsonNode translatedNode = translatedTranslation == null ? null : translatedTranslation.get(fieldName);
            if (fullNode.isObject()) {
                getTranslationForEdit(fullNode, customNode, parentNode, originNode, translatedNode);
            } else {
                ObjectNode info = newObjectNode();
                info.put("t", fullNode.asText()); // translated value
                if (originNode != null) {
                    info.put("o", originNode.asText()); // original translation
                }
                String state;
                if (customNode != null) {
                    if (parentNode == null) {
                        state = "A";
                    } else {
                        state = "C";
                        info.put("p", parentNode.asText()); // parent translation
                    }
                } else if (translatedNode != null) {
                    state = "T";
                } else {
                    state = "U";
                }
                info.put("s", state); // state
                ((ObjectNode) fullTranslation).set(fieldName, info);
            }
        }
    }

    private JsonNode getTranslatedOnlyTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customTranslation = getMergedCustomTranslation(tenantId, customerId, localeCode);
        if (TRANSLATION_VALUE_MAP.containsKey(localeCode)) {
            JsonNode systemTranslation = readSystemLocaleTranslation(localeCode);
            customTranslation = update(systemTranslation, customTranslation);
        }
        return customTranslation;
    }

    @Override
    public CustomTranslation saveCustomTranslation(CustomTranslation customTranslation) {
        CustomTranslation saved = customTranslationService.saveCustomTranslation(customTranslation);
        evictFromCache(customTranslation.getTenantId());
        return saved;
    }

    @Override
    public CustomTranslation patchCustomTranslation(CustomTranslation customTranslation) {
        CustomTranslation saved = customTranslationService.patchCustomTranslation(customTranslation);
        evictFromCache(customTranslation.getTenantId());
        return saved;
    }

    @Override
    public CustomTranslation deleteCustomTranslationKey(TenantId tenantId, CustomerId customerId, String localeCode, String keyPath) {
        CustomTranslation customTranslation = customTranslationService.deleteCustomTranslationKeyByPath(tenantId, customerId, localeCode, keyPath);
        evictFromCache(tenantId);
        return customTranslation;
    }

    @Override
    public void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        customTranslationService.deleteCustomTranslation(tenantId, customerId, localeCode);
        evictFromCache(tenantId);
    }

    @Override
    public String getETag(TranslationCacheKey translationCacheKey) {
        return etagCache.getIfPresent(translationCacheKey);
    }

    @Override
    public void putETag(TranslationCacheKey translationCacheKey, String etag) {
        etagCache.put(translationCacheKey, etag);
    }

    @Override
    public void evictETags(TenantId tenantId) {
        if (tenantId.isSysTenantId()) {
            etagCache.invalidateAll();
        } else {
            Set<TranslationCacheKey> keysToInvalidate = etagCache
                    .asMap().keySet().stream()
                    .filter(translationCacheKey -> translationCacheKey.getTenantId().equals(tenantId))
                    .collect(Collectors.toSet());
            etagCache.invalidateAll(keysToInvalidate);
        }
    }

    private static TranslationInfo createTranslationInfo(String localeCode, JsonNode translation) {
        int progress = calculateTranslationProgress(translation);
        Locale locale = Locale.forLanguageTag(localeCode.replace("_", "-"));
        return TranslationInfo.builder()
                .localeCode(localeCode)
                .country(locale.getDisplayCountry())
                .language(locale.getDisplayLanguage() + " (" + locale.getDisplayLanguage(locale) + ")")
                .progress(progress)
                .build();
    }

    private JsonNode getMergedCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customTranslation;
        if (tenantId.isSysTenantId()) {
            customTranslation = customTranslationService.getCurrentCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode).getValue().deepCopy();
        } else if (customerId == null || customerId.isNullUid()) {
            customTranslation = customTranslationService.getMergedTenantCustomTranslation(tenantId, localeCode);
        } else {
            customTranslation = customTranslationService.getMergedCustomerCustomTranslation(tenantId, customerId, localeCode);
        }
        return customTranslation;
    }

    private static JsonNode readSystemLocaleTranslation(String localeCode) {
        String filePath = LOCALE_FILES_DIRECTORY_PATH + "/locale.constant-" + localeCode + ".json";
        try (InputStream in = DefaultTbTranslationService.class.getClassLoader().getResourceAsStream(filePath)) {
            return JacksonUtil.OBJECT_MAPPER.readTree(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read locale translation for " + localeCode + "!", e);
        }
    }

    private static Set<String> getSystemLocaleCodes() {
        List<String> filenames = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:public/assets/locale/*.json");
            Arrays.stream(resources).forEach(resource -> filenames.add(resource.getFilename()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to get list of system locales!", e);
        }
        return filenames.stream().map(DefaultTbTranslationService::getLocaleFromFileName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static String getLocaleFromFileName(String filename) {
        Matcher matcher = LOCALE_FILE_PATTERN.matcher(filename);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private static int calculateTranslationProgress(JsonNode translation) {
        Set<String> localeKeys = extractKeys(translation);
        long translated = DEFAULT_LOCALE_KEYS.stream()
                .filter(localeKeys::contains)
                .count();
        return (int) (((translated) * 100) / DEFAULT_LOCALE_KEYS.size());
    }

    private JsonNode getParentTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode parentTranslation;
        if (customerId != null && !customerId.isNullUid()) {
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            if (customer.isSubCustomer()) {
                parentTranslation = getFullTranslation(tenantId, customer.getParentCustomerId(), localeCode);
            } else {
                parentTranslation = getFullTranslation(tenantId, null, localeCode);
            }
        } else {
            if (tenantId.isSysTenantId()) {
                parentTranslation = TRANSLATION_VALUE_MAP.getOrDefault(localeCode, TRANSLATION_VALUE_MAP.get(DEFAULT_LOCALE_CODE));
            } else {
                parentTranslation = getFullTranslation(TenantId.SYS_TENANT_ID, null, localeCode);
            }
        }
        return parentTranslation;
    }

    private void evictFromCache(TenantId tenantId) {
        evictETags(tenantId);
        clusterService.broadcastToCore(TransportProtos.ToCoreNotificationMsg.newBuilder()
                .setTranslationCacheInvalidateMsg(TransportProtos.TranslationCacheInvalidateMsg.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .build())
                .build());
    }
}
