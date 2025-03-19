/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.TenantEntityWithDataDao;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.EntitiesLimitException;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class DataValidator<D extends BaseData<?>> {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private static final Pattern QUEUE_PATTERN = Pattern.compile("^[a-zA-Z0-9_.\\-]+$");
    private static final String DOMAIN_REGEX = "^(((?!-))(xn--|_)?[a-z0-9-]{0,61}[a-z0-9]{1,1}\\.)*(xn--)?([a-z0-9][a-z0-9\\-]{0,60}|[a-z0-9-]{1,30}\\.[a-z]{2,})$";
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(DOMAIN_REGEX);
    private static final String LOCALHOST_REGEX = "^localhost(:\\d{1,5})?$";
    private static final Pattern LOCALHOST_PATTERN = Pattern.compile(LOCALHOST_REGEX);
    private static final String NAME = "name";
    private static final String TOPIC = "topic";

    @Autowired @Lazy
    private ApiLimitService apiLimitService;

    // Returns old instance of the same object that is fetched during validation.
    public D validate(D data, Function<D, TenantId> tenantIdFunction) {
        try {
            if (data == null) {
                throw new DataValidationException("Data object can't be null!");
            }

            ConstraintValidator.validateFields(data);

            TenantId tenantId = tenantIdFunction.apply(data);
            validateDataImpl(tenantId, data);
            D old;
            if (data.getId() == null) {
                validateCreate(tenantId, data);
                old = null;
            } else {
                old = validateUpdate(tenantId, data);
            }
            return old;
        } catch (DataValidationException e) {
            log.error("{} object is invalid: [{}]", data == null ? "Data" : data.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    protected void validateDataImpl(TenantId tenantId, D data) {
    }

    protected void validateCreate(TenantId tenantId, D data) {
    }

    protected D validateUpdate(TenantId tenantId, D data) {
        return null;
    }

    public void validateDelete(TenantId tenantId, EntityId entityId) {
    }

    public void validateString(String exceptionPrefix, String name) {
        if (StringUtils.isEmpty(name) || name.trim().length() == 0) {
            throw new DataValidationException(exceptionPrefix + " should be specified!");
        }
        if (StringUtils.contains0x00(name)) {
            throw new DataValidationException(exceptionPrefix + " should not contain 0x00 symbol!");
        }
    }

    protected boolean isSameData(D existentData, D actualData) {
        return actualData.getId() != null && existentData.getId().equals(actualData.getId());
    }

    public static void validateEmail(String email) {
        if (!doValidateEmail(email)) {
            throw new DataValidationException("Invalid email address format '" + email + "'!");
        }
    }

    public static boolean doValidateEmail(String email) {
        if (email == null) {
            return false;
        }

        Matcher emailMatcher = EMAIL_PATTERN.matcher(email);
        return emailMatcher.matches();
    }

    public static void validateLocaleCode(String localeCode) {
        if (!doValidateLocaleCode(localeCode)) {
            throw new DataValidationException("Invalid locale format '" + localeCode + "'!");
        }
    }

    public static void validateCustomTranslationPatch(JsonNode customTranslation) {
        Streams.stream(customTranslation.fieldNames()).forEach(key -> {
            if (key.endsWith(".")) {
                throw new DataValidationException("The key can`t end with '.'");
            }
        });
    }

    public static void validateCustomTranslationKeys(Set<String> defaultLocaleKeys, JsonNode customTranslation) {
        Set<String> customTranslationKeys = JacksonUtil.extractKeys(customTranslation);
        customTranslationKeys.removeAll(defaultLocaleKeys);
        for (String addedKey : customTranslationKeys) {
            for (String defaultLocaleKey : defaultLocaleKeys) {
                if (addedKey.startsWith(defaultLocaleKey + ".")) {
                    throw new DataValidationException("The key [" + addedKey + "] overlaps default key [" + defaultLocaleKey + "]");
                }
            }
        }
    }

    public static boolean doValidateLocaleCode(String localeCode) {
        if (localeCode == null) {
            return false;
        }
        String[] parts = localeCode.split("_");
        try {
            switch (parts.length) {
                case 3: return isLocaleValid(new Locale(parts[0], parts[1], parts[2]));
                case 2: return isLocaleValid(new Locale(parts[0], parts[1]));
                case 1: return isLocaleValid(new Locale(parts[0]));
                default: return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isLocaleValid(Locale locale) {
        return locale.getISO3Language() != null && locale.getISO3Country() != null;
    }

    protected void validateNumberOfEntitiesPerTenant(TenantId tenantId,
                                                     EntityType entityType) {
        if (!apiLimitService.checkEntitiesLimit(tenantId, entityType)) {
            throw new EntitiesLimitException(tenantId, entityType);
        }
    }

    protected void validateMaxSumDataSizePerTenant(TenantId tenantId,
                                                   TenantEntityWithDataDao dataDao,
                                                   long maxSumDataSize,
                                                   long currentDataSize,
                                                   EntityType entityType) {
        if (maxSumDataSize > 0) {
            if (dataDao.sumDataSizeByTenantId(tenantId) + currentDataSize > maxSumDataSize) {
                throw new DataValidationException(String.format("%ss total size exceeds the maximum of " + FileUtils.byteCountToDisplaySize(maxSumDataSize), entityType.getNormalName()));
            }
        }
    }

    protected static void validateJsonStructure(JsonNode expectedNode, JsonNode actualNode) {
        Set<String> expectedFields = new HashSet<>();
        Iterator<String> fieldsIterator = expectedNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            expectedFields.add(fieldsIterator.next());
        }

        Set<String> actualFields = new HashSet<>();
        fieldsIterator = actualNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            actualFields.add(fieldsIterator.next());
        }

        if (!expectedFields.containsAll(actualFields) || !actualFields.containsAll(expectedFields)) {
            throw new DataValidationException("Provided json structure is different from stored one '" + actualNode + "'!");
        }
    }

    protected static void validateQueueName(String name) {
        validateQueueNameOrTopic(name, NAME);
        if (DataConstants.CF_QUEUE_NAME.equals(name) || DataConstants.CF_STATES_QUEUE_NAME.equals(name)) {
            throw new DataValidationException(String.format("The queue name '%s' is not allowed. This name is reserved for internal use. Please choose a different name.", name));
        }
    }

    protected static void validateQueueTopic(String topic) {
        validateQueueNameOrTopic(topic, TOPIC);
    }

    static void validateQueueNameOrTopic(String value, String fieldName) {
        if (StringUtils.isEmpty(value) || value.trim().length() == 0) {
            throw new DataValidationException(String.format("Queue %s should be specified!", fieldName));
        }
        if (!QUEUE_PATTERN.matcher(value).matches()) {
            throw new DataValidationException(
                    String.format("Queue %s contains a character other than ASCII alphanumerics, '.', '_' and '-'!", fieldName));
        }
    }

    public static boolean isValidDomain(String domainName) {
        if (domainName == null) {
            return false;
        }
        if (LOCALHOST_PATTERN.matcher(domainName).matches()) {
            return true;
        }
        return DOMAIN_PATTERN.matcher(domainName).matches();
    }

    public static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException e) {
            return false;
        } catch (URISyntaxException e) {
            return false;
        }
    }

}
