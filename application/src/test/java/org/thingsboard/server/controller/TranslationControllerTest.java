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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.translation.CustomTranslationService;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.id.TenantId.SYS_TENANT_ID;

@DaoSqlTest
public class TranslationControllerTest extends AbstractControllerTest {

    private static final String ES_ES = "es_ES";
    private static final String EN_AU = "en_AU";
    private static final String AR_QA = "ar_QA";

    @Autowired
    CustomTranslationService customTranslationService;
    @Autowired
    AdminSettingsDao  adminSettingsDao;

    @Test
    public void shouldGetCorrectFullTranslation() throws Exception {
        loginSysAdmin();
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation, CustomTranslation.class);

        loginTenantAdmin();
        JsonNode itTenantCustomTranslation = JacksonUtil.toJsonNode("{\"update\" : \"tenant\" ," +
                " \"remove\" : \"tenant\", \"search\":\"tenant\"}");
        doPost("/api/translation/custom/" + ES_ES, itTenantCustomTranslation, CustomTranslation.class);

        loginCustomerAdminUser();
        JsonNode plCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"remove\" : \"customer\", \"search\":\"customer\"}");
        doPost("/api/translation/custom/" + ES_ES, plCustomerCustomTranslation, CustomTranslation.class);

        loginSubCustomerAdminUser();
        JsonNode plSubCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"search\":\"subCustomer\"}");
        doPost("/api/translation/custom/" + ES_ES, plSubCustomerCustomTranslation, CustomTranslation.class);

        // get full system translation
        loginSysAdmin();
        JsonNode fullSystemTranslation = doGet("/api/translation/full/" + ES_ES, JsonNode.class);
        assertThat(fullSystemTranslation.get("save").asText()).isEqualTo("system");
        assertThat(fullSystemTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(fullSystemTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Solution template");

        // get full tenant translation
        loginTenantAdmin();
        JsonNode fullTenantTranslation = doGet("/api/translation/full/" + ES_ES, JsonNode.class);
        assertThat(fullTenantTranslation.get("save").asText()).isEqualTo("system");
        assertThat(fullTenantTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(fullTenantTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(fullTenantTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Solution template");

        // get merged customer custom translation
        loginCustomerAdminUser();
        JsonNode fullCustomerTranslation = doGet("/api/translation/full/" + ES_ES, JsonNode.class);
        assertThat(fullCustomerTranslation.get("save").asText()).isEqualTo("system");
        assertThat(fullCustomerTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(fullCustomerTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(fullCustomerTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(fullCustomerTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Solution template");

        // get merged subcustomer custom translation
        loginSubCustomerAdminUser();
        JsonNode fullSubCustomerTranslation = doGet("/api/translation/full/" + ES_ES, JsonNode.class);
        assertThat(fullSubCustomerTranslation.get("save").asText()).isEqualTo("system");
        assertThat(fullSubCustomerTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(fullSubCustomerTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(fullSubCustomerTranslation.get("search").asText()).isEqualTo("subCustomer");
        assertThat(fullSubCustomerTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(fullSubCustomerTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Solution template");
    }

    @Test
    public void shouldGetCorrectTranslationInfo() throws Exception {
        loginSysAdmin();
        checkTranslationInfo();

        loginTenantAdmin();
        checkTranslationInfo();

        loginCustomerAdminUser();
        checkTranslationInfo();

        loginSubCustomerAdminUser();
        checkTranslationInfo();
    }

    @Test
    public void shouldCalculateTranslationProgress() throws Exception {
        loginSysAdmin();
        //check progress is 0
        JsonNode customTranslation = JacksonUtil.toJsonNode("{\"save\":\"arabic\"}");
        doPost("/api/translation/custom/" + AR_QA, customTranslation, CustomTranslation.class);

        TranslationInfo arabic = getTranslationInfo(AR_QA);
        assertThat(arabic.getProgress()).isEqualTo(0);

        JsonNode fullCustomerTranslation = doGet("/api/translation/full/" + AR_QA, JsonNode.class);

        //translate some keys and check progress > 0
        Iterator<String> fieldNames = fullCustomerTranslation.fieldNames();
        int count = 20;
        while (fieldNames.hasNext() && count > 0) {
            String fieldName = fieldNames.next();
            ((ObjectNode) customTranslation).set(fieldName, fullCustomerTranslation.get(fieldName));
            count--;
        }
        doPost("/api/translation/custom/" + AR_QA, customTranslation, CustomTranslation.class);

        Set<String> fullTranslationKeys = JacksonUtil.extractKeys(fullCustomerTranslation);
        Set<String> translated = JacksonUtil.extractKeys(customTranslation);

        TranslationInfo updatedSystemArabicInfo = getTranslationInfo(AR_QA);
        assertThat(updatedSystemArabicInfo.getProgress()).isGreaterThan(0)
                .isEqualTo((translated.size() * 100)/fullTranslationKeys.size());

        //login as tenant, translate all keys, check progress is 100
        loginTenantAdmin();
        JsonNode tenantCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"arabic\"}");
        doPost("/api/translation/custom/" + AR_QA, tenantCustomTranslation, CustomTranslation.class);
        TranslationInfo tenantArabicInfo = getTranslationInfo(AR_QA);
        assertThat(tenantArabicInfo.getProgress()).isEqualTo(updatedSystemArabicInfo.getProgress());

        doPost("/api/translation/custom/" + AR_QA, fullCustomerTranslation, CustomTranslation.class);
        TranslationInfo updatedTenantArabic = getTranslationInfo(AR_QA);
        assertThat(updatedTenantArabic.getProgress()).isEqualTo(100);
    }

    @Test
    public void shouldDownloadFullTranslation() throws Exception {
        loginSysAdmin();
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation, CustomTranslation.class);

        //download full translation
        byte[] contentAsByteArray = doGet("/api/translation/full/" + ES_ES).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        JsonNode downloadedCustomTranslation = JacksonUtil.fromBytes(contentAsByteArray);

        assertThat(downloadedCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(downloadedCustomTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(downloadedCustomTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Solution template");
    }

    private void checkTranslationInfo() throws Exception {
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"spanish\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation, CustomTranslation.class);

        JsonNode itCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"australian\"}");
        doPost("/api/translation/custom/" + EN_AU, itCustomTranslation, CustomTranslation.class);

        List<TranslationInfo> tenantTranslationInfos = doGetTyped("/api/translation/info", new TypeReference<>() {
        });
        assertThat(tenantTranslationInfos).hasSize(2);

        Optional<TranslationInfo> spanish = tenantTranslationInfos.stream().filter(info -> info.getLocaleCode().equals(ES_ES))
                .findFirst();
        assertThat(spanish).isPresent();
        TranslationInfo spanishInfo = spanish.get();
        Locale spanishLocale = new Locale("es", "ES");

        assertThat(spanishInfo.getLocaleCode()).isEqualTo(spanishLocale.toString());
        assertThat(spanishInfo.getCountry()).isEqualTo(spanishLocale.getDisplayCountry());
        assertThat(spanishInfo.getLanguage()).isEqualTo(spanishLocale.getDisplayLanguage());
        assertThat(spanishInfo.getProgress()).isGreaterThan(0);

        Optional<TranslationInfo> italian = tenantTranslationInfos.stream().filter(info -> info.getLocaleCode().equals(EN_AU))
                .findFirst();
        assertThat(italian).isPresent();
        TranslationInfo italianInfo = italian.get();
        Locale italianLocale = new Locale("en", "AU");

        assertThat(italianInfo.getLocaleCode()).isEqualTo(italianLocale.toString());
        assertThat(italianInfo.getCountry()).isEqualTo(italianLocale.getDisplayCountry());
        assertThat(italianInfo.getLanguage()).isEqualTo(italianLocale.getDisplayLanguage());
        assertThat(italianInfo.getProgress()).isEqualTo(0);
    }

    private TranslationInfo getTranslationInfo(String localeCode) throws Exception {
        List<TranslationInfo> translationInfos = doGetTyped("/api/translation/info", new TypeReference<>() {
        });
        Optional<TranslationInfo> arabic = translationInfos.stream().filter(info -> info.getLocaleCode().equals(localeCode))
                .findFirst();
        assertThat(arabic).isPresent();
        return arabic.get();
    }
}
