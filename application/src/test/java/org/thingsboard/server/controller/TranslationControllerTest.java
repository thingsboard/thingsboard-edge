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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
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

@DaoSqlTest
public class TranslationControllerTest extends AbstractControllerTest {

    private static final String ES_ES = "es_ES";
    private static final String EN_AU = "en_AU";
    private static final String EN_US = "en_US";
    private static final String AR_QA = "ar_QA";

    @Autowired
    CustomTranslationService customTranslationService;
    @Autowired
    AdminSettingsDao  adminSettingsDao;

    @Test
    public void shouldGetCorrectFullTranslation() throws Exception {
        loginSysAdmin();
        JsonNode systemCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, systemCustomTranslation);

        loginTenantAdmin();
        JsonNode tenantCustomTranslation = JacksonUtil.toJsonNode("{\"update\" : \"tenant\" ," +
                " \"remove\" : \"tenant\", \"search\":\"tenant\"}");
        doPost("/api/translation/custom/" + ES_ES, tenantCustomTranslation);

        loginCustomerAdminUser();
        JsonNode customerCustomTranslation = JacksonUtil.toJsonNode("{\"remove\" : \"customer\", \"search\":\"customer\"}");
        doPost("/api/translation/custom/" + ES_ES, customerCustomTranslation);

        loginSubCustomerAdminUser();
        JsonNode subCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"search\":\"subCustomer\"}");
        doPost("/api/translation/custom/" + ES_ES, subCustomerCustomTranslation);

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

        // get full customer custom translation
        loginCustomerAdminUser();
        JsonNode fullCustomerTranslation = doGet("/api/translation/full/" + ES_ES, JsonNode.class);
        assertThat(fullCustomerTranslation.get("save").asText()).isEqualTo("system");
        assertThat(fullCustomerTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(fullCustomerTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(fullCustomerTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(fullCustomerTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Solution template");

        // get full subcustomer custom translation
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
    public void shouldGetCorrectTranslationForBasicEdit() throws Exception {
        loginSysAdmin();
        JsonNode systemCustomTranslation = JacksonUtil.toJsonNode("{\"account\": {\"account\" : \"systemAccount\"}, \"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, systemCustomTranslation);

        loginTenantAdmin();
        JsonNode tenantCustomTranslation = JacksonUtil.toJsonNode("{\"account\": {\"account\" : \"tenantAccount\"}, \"update\" : \"tenant\" ," +
                " \"remove\" : \"tenant\", \"search\":\"tenant\"}");
        doPost("/api/translation/custom/" + ES_ES, tenantCustomTranslation);

        loginCustomerAdminUser();
        JsonNode customerCustomTranslation = JacksonUtil.toJsonNode("{\"account\": {\"account\" : \"customerAccount\"}, \"remove\" : \"customer\", \"search\":\"customer\"}");
        doPost("/api/translation/custom/" + ES_ES, customerCustomTranslation);

        loginSubCustomerAdminUser();
        JsonNode subCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"account\": {\"account\" : \"subCustomerAccount\"}, \"search\":\"subCustomer\"}");
        doPost("/api/translation/custom/" + ES_ES, subCustomerCustomTranslation);

        // get system translation for edit
        loginSysAdmin();
        JsonNode systemTranslationForEdit = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(systemTranslationForEdit.get("account").get("account"), "systemAccount", "Account", "Cuenta", "C");
        verifyInfo(systemTranslationForEdit.get("save"), "system", null, null, "A");
        verifyInfo(systemTranslationForEdit.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(systemTranslationForEdit.get("solution-template").get("solution-template"), null, "Solution template", null, "U");

        // get tenant translation for edit
        loginTenantAdmin();
        JsonNode tenantTranslationForEdit = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(tenantTranslationForEdit.get("account").get("account"), "tenantAccount", "Account", "systemAccount", "C");
        verifyInfo(tenantTranslationForEdit.get("save"), "system", null, null, "T");
        verifyInfo(tenantTranslationForEdit.get("update"), "tenant", null, "system", "C");
        verifyInfo(tenantTranslationForEdit.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(tenantTranslationForEdit.get("solution-template").get("solution-template"), null, "Solution template", null, "U");

        // get customer for edit
        loginCustomerAdminUser();
        JsonNode customerTranslationForEdit = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(customerTranslationForEdit.get("account").get("account"), "customerAccount", "Account", "tenantAccount", "C");
        verifyInfo(customerTranslationForEdit.get("save"), "system", null, null, "T");
        verifyInfo(customerTranslationForEdit.get("update"), "tenant", null, null, "T");
        verifyInfo(customerTranslationForEdit.get("remove"), "customer", null, "tenant", "C");
        verifyInfo(customerTranslationForEdit.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(customerTranslationForEdit.get("solution-template").get("solution-template"), null, "Solution template", null, "U");

        // get subcustomer translation  for edit
        loginSubCustomerAdminUser();
        JsonNode subCustomerTranslation = doGet("/api/translation/edit/basic/" + ES_ES, JsonNode.class);
        verifyInfo(subCustomerTranslation.get("account").get("account"), "subCustomerAccount", "Account", "customerAccount", "C");
        verifyInfo(subCustomerTranslation.get("save"), "system", null, null, "T");
        verifyInfo(subCustomerTranslation.get("update"), "tenant", null, null, "T");
        verifyInfo(subCustomerTranslation.get("remove"), "customer", null, null, "T");
        verifyInfo(subCustomerTranslation.get("search"), "subCustomer", null, "customer", "C");
        verifyInfo(subCustomerTranslation.get("access").get("unauthorized"), "No autorizado", "Unauthorized", null, "T");
        verifyInfo(subCustomerTranslation.get("solution-template").get("solution-template"), null, "Solution template", null, "U");
    }

    private static void verifyInfo(JsonNode keyInfo, String translated, String origin, String parent, String state) {
        if (translated != null) {
            assertThat(keyInfo.get("t").asText()).isEqualTo(translated);
        }
        if (origin != null) {
            assertThat(keyInfo.get("o").asText()).isEqualTo(origin);
        }
        if (parent != null) {
            assertThat(keyInfo.get("p").asText()).isEqualTo(parent);
        }
        assertThat(keyInfo.get("s").asText()).isEqualTo(state);
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
        doPost("/api/translation/custom/" + AR_QA, customTranslation);

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
        doPost("/api/translation/custom/" + AR_QA, customTranslation);

        Set<String> fullTranslationKeys = JacksonUtil.extractKeys(fullCustomerTranslation);
        Set<String> translated = JacksonUtil.extractKeys(customTranslation);

        TranslationInfo updatedSystemArabicInfo = getTranslationInfo(AR_QA);
        assertThat(updatedSystemArabicInfo.getProgress()).isGreaterThan(0)
                .isEqualTo((translated.size() * 100)/fullTranslationKeys.size());

        //login as tenant, translate all keys, check progress is 100
        loginTenantAdmin();
        JsonNode tenantCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"arabic\"}");
        doPost("/api/translation/custom/" + AR_QA, tenantCustomTranslation);
        TranslationInfo tenantArabicInfo = getTranslationInfo(AR_QA);
        assertThat(tenantArabicInfo.getProgress()).isEqualTo(updatedSystemArabicInfo.getProgress());

        doPost("/api/translation/custom/" + AR_QA, fullCustomerTranslation);
        TranslationInfo updatedTenantArabic = getTranslationInfo(AR_QA);
        assertThat(updatedTenantArabic.getProgress()).isEqualTo(100);
    }

    @Test
    public void shouldDownloadFullTranslation() throws Exception {
        loginSysAdmin();
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation);

        //download full translation
        byte[] contentAsByteArray = doGet("/api/translation/full/" + ES_ES).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        JsonNode downloadedCustomTranslation = JacksonUtil.fromBytes(contentAsByteArray);

        assertThat(downloadedCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(downloadedCustomTranslation.get("access").get("unauthorized").asText()).isEqualTo("No autorizado");
        assertThat(downloadedCustomTranslation.get("solution-template").get("solution-template").asText()).isEqualTo("Solution template");
    }

    @Test
    public void shouldGetCorrectLoginTranslation() throws Exception {
        // get login system translation
        loginSysAdmin();
        JsonNode loginSystemTranslation = doGet("/api/noauth/translation/login/" + ES_ES, JsonNode.class);
        assertThat(loginSystemTranslation.get("login").get("login").asText()).isEqualTo("Entrar");
        JsonNode enloginSystemTranslation = doGet("/api/noauth/translation/login/" + EN_US, JsonNode.class);
        assertThat(enloginSystemTranslation.get("login").get("login").asText()).isEqualTo("Login");

        // get login tenant translation
        loginTenantAdmin();
        JsonNode loginTenantTranslation = doGet("/api/noauth/translation/login/" + ES_ES, JsonNode.class);
        assertThat(loginTenantTranslation.get("login").get("login").asText()).isEqualTo("Entrar");
        JsonNode enloginTenantTranslation = doGet("/api/noauth/translation/login/" + EN_US, JsonNode.class);
        assertThat(enloginTenantTranslation.get("login").get("login").asText()).isEqualTo("Login");

        // get login customer custom translation
        loginCustomerAdminUser();
        JsonNode loginCustomerTranslation = doGet("/api/noauth/translation/login/" + ES_ES, JsonNode.class);
        assertThat(loginCustomerTranslation.get("login").get("login").asText()).isEqualTo("Entrar");
        JsonNode enLoginCustomerTranslation = doGet("/api/noauth/translation/login/" + EN_US, JsonNode.class);
        assertThat(enLoginCustomerTranslation.get("login").get("login").asText()).isEqualTo("Login");

        // get login subcustomer custom translation
        loginSubCustomerAdminUser();
        JsonNode loginSubCustomerTranslation = doGet("/api/noauth/translation/login/" + ES_ES, JsonNode.class);
        assertThat(loginSubCustomerTranslation.get("login").get("login").asText()).isEqualTo("Entrar");
        JsonNode enLoginSubCustomerTranslation = doGet("/api/noauth/translation/login/" + EN_US, JsonNode.class);
        assertThat(enLoginSubCustomerTranslation.get("login").get("login").asText()).isEqualTo("Login");
    }

    private void checkTranslationInfo() throws Exception {
        JsonNode auCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"australian\"}");
        doPost("/api/translation/custom/" + EN_AU, auCustomTranslation);

        List<TranslationInfo> translationInfos = doGetTyped("/api/translation/info", new TypeReference<>() {
        });

        Optional<TranslationInfo> defaultLocaleInfo = translationInfos.stream().filter(info -> EN_US.equalsIgnoreCase(info.getLocaleCode()))
                .findFirst();
        assertThat(defaultLocaleInfo).isPresent();
        TranslationInfo englishInfo = defaultLocaleInfo.get();

        assertThat(englishInfo.getLocaleCode()).isEqualToIgnoringCase(EN_US);
        assertThat(englishInfo.getLanguage()).isEqualToIgnoringCase("English (English)");
        assertThat(englishInfo.getCountry()).isEqualToIgnoringCase("United States");
        assertThat(englishInfo.getProgress()).isEqualTo(100);

        Optional<TranslationInfo> australian = translationInfos.stream().filter(info -> EN_AU.equalsIgnoreCase(info.getLocaleCode()))
                .findFirst();
        assertThat(australian).isPresent();
        TranslationInfo australianInfo = australian.get();

        assertThat(australianInfo.getLocaleCode()).isEqualToIgnoringCase(EN_AU);
        assertThat(australianInfo.getLanguage()).isEqualToIgnoringCase("English (English)");
        assertThat(australianInfo.getCountry()).isEqualToIgnoringCase("Australia");
        assertThat(australianInfo.getProgress()).isEqualTo(0);
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
