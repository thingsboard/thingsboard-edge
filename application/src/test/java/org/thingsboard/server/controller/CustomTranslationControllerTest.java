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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.translation.CustomTranslationService;


import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class CustomTranslationControllerTest extends AbstractControllerTest {

    private static final String ES_ES = "es_ES";
    private static final byte[] CUSTOM_TRANSLATION_BYTES = Base64.getDecoder().decode("eyJzYXZlIjoic2F2ZSBhbGFybSJ9");

    @Autowired
    CustomTranslationService customTranslationService;
    @Autowired
    AdminSettingsDao  adminSettingsDao;

    @Test
    public void shouldSaveCustomTranslation() throws Exception {
        loginSysAdmin();
        checkSaveCustomTranslation(ES_ES);
        String sysAdminEtag = getFullTranslation(ES_ES);

        loginTenantAdmin();
        checkSaveCustomTranslation(ES_ES);
        String tenantEtag = getFullTranslation(ES_ES);

        loginCustomerAdminUser();
        checkSaveCustomTranslation(ES_ES);
        String customerAdminEtag = getFullTranslation(ES_ES);

        loginSubCustomerAdminUser();
        checkSaveCustomTranslation(ES_ES);
        String subCustomerEtag = getFullTranslation(ES_ES);

        //check if full translation modified
        loginSysAdmin();
        assertThat(getFullTranslationResponseStatus(ES_ES, sysAdminEtag)).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginTenantAdmin();
        assertThat(getFullTranslationResponseStatus(ES_ES, tenantEtag)).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginCustomerAdminUser();
        assertThat(getFullTranslationResponseStatus(ES_ES, customerAdminEtag)).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        loginSubCustomerAdminUser();
        assertThat(getFullTranslationResponseStatus(ES_ES, subCustomerEtag)).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        //update system custom translation and check full translation is being modofied everywhere
        loginSysAdmin();
        JsonNode newCustomTranslation = JacksonUtil.toJsonNode("{\"update\" : \"system\"}");
        doPost("/api/translation/custom/" + ES_ES, newCustomTranslation, CustomTranslation.class);

        assertThat(getFullTranslationResponseStatus(ES_ES, sysAdminEtag)).isEqualTo(HttpStatus.OK.value());

        loginTenantAdmin();
        assertThat(getFullTranslationResponseStatus(ES_ES, tenantEtag)).isEqualTo(HttpStatus.OK.value());

        loginCustomerAdminUser();
        assertThat(getFullTranslationResponseStatus(ES_ES, customerAdminEtag)).isEqualTo(HttpStatus.OK.value());

        loginSubCustomerAdminUser();
        assertThat(getFullTranslationResponseStatus(ES_ES, subCustomerEtag)).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void shouldGetCorrectMergedCustomTranslation() throws Exception {
        loginSysAdmin();
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        updateSpanishCustomTranslation(esCustomTranslation);

        loginTenantAdmin();
        JsonNode esTenantCustomTranslation = JacksonUtil.toJsonNode("{\"update\" : \"tenant\" ," +
                " \"remove\" : \"tenant\", \"search\":\"tenant\"}");
        updateSpanishCustomTranslation(esTenantCustomTranslation);

        loginCustomerAdminUser();
        JsonNode esCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"remove\" : \"customer\", \"search\":\"customer\"}");
        updateSpanishCustomTranslation(esCustomerCustomTranslation);

        loginSubCustomerAdminUser();
        JsonNode esSubCustomerCustomTranslation = JacksonUtil.toJsonNode("{\"search\":\"subCustomer\"}");
        updateSpanishCustomTranslation(esSubCustomerCustomTranslation);

        // get merged customer custom translation
        loginCustomerAdminUser();
        JsonNode mergedEsCustomTranslation = doGet("/api/translation/custom/merged/" + ES_ES, JsonNode.class);
        assertThat(mergedEsCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(mergedEsCustomTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(mergedEsCustomTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(mergedEsCustomTranslation.get("search").asText()).isEqualTo("customer");

        // get merged subcustomer custom translation
        loginSubCustomerAdminUser();
        JsonNode mergedSubCustomerCustomTranslation = doGet("/api/translation/custom/merged/" + ES_ES, JsonNode.class);
        assertThat(mergedSubCustomerCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(mergedSubCustomerCustomTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(mergedSubCustomerCustomTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(mergedSubCustomerCustomTranslation.get("search").asText()).isEqualTo("subCustomer");
    }

    @Test
    public void shouldPatchCustomTranslation() throws Exception {
        loginSysAdmin();
        checkPatchCustomTranslation();

        loginTenantAdmin();
        checkPatchCustomTranslation();

        loginCustomerAdminUser();
        checkPatchCustomTranslation();

        loginSubCustomerAdminUser();
        checkPatchCustomTranslation();
    }

    @Test
    public void shouldDeleteCustomTranslationKey() throws Exception {
        loginSysAdmin();
        checkDeleteCustomTranslationKey("", "save alarm");

        loginTenantAdmin();
        checkDeleteCustomTranslationKey("save alarm", "tenant save alarm");

        loginCustomerAdminUser();
        checkDeleteCustomTranslationKey("tenant save alarm", "customer save alarm");

        loginSubCustomerAdminUser();
        checkDeleteCustomTranslationKey("customer save alarm", "subcustomer save alarm");
    }

    @Test
    public void shouldUploadCustomTranslation() throws Exception {
        loginSysAdmin();
        CustomTranslation uploaded = uploadCustomTranslation(HttpMethod.POST, "/api/translation/custom/" + ES_ES + "/upload",
                MediaType.APPLICATION_JSON_VALUE, CUSTOM_TRANSLATION_BYTES);
        assertThat(uploaded.getValue()).isEqualTo(JacksonUtil.toJsonNode("{\"save\":\"save alarm\"}"));

        CustomTranslation retrieved = doGet("/api/translation/custom/" + ES_ES, CustomTranslation.class);
        assertThat(retrieved.getValue().get("save").asText()).isEqualTo("save alarm");
    }

    @Test
    public void shouldDeleteCustomTranslation() throws Exception {
        loginSysAdmin();
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"" + StringUtils.randomAlphabetic(10) + "\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation, CustomTranslation.class);

        doDelete("/api/translation/custom/" + ES_ES);

        CustomTranslation retrieved = doGet("/api/translation/custom/" + ES_ES, CustomTranslation.class);
        assertThat(retrieved.getValue()).isEmpty();
    }

    private void updateSpanishCustomTranslation(JsonNode newCustomTranslation) throws Exception {
        doPost("/api/translation/custom/" + ES_ES, newCustomTranslation, CustomTranslation.class);

        CustomTranslation updatedCustomTranslation = doGet("/api/translation/custom/" + ES_ES, CustomTranslation.class);
        assertThat(updatedCustomTranslation.getValue()).isEqualTo(newCustomTranslation);
    }

    private void checkSaveCustomTranslation(String localeCode) {
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":\"" + StringUtils.randomAlphabetic(10) + "\"}");
        CustomTranslation savedCT = doPost("/api/translation/custom/" + localeCode, esCustomTranslation, CustomTranslation.class);
        assertThat(savedCT.getValue()).isEqualTo(esCustomTranslation);
    }

    private String getFullTranslation(String localeCode) throws Exception {
        return doGet("/api/translation/full/" + localeCode)
                .andReturn().getResponse().getHeader("ETag");
    }

    private int getFullTranslationResponseStatus(String localeCode, String etag) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setIfNoneMatch(etag);
        return doGet("/api/translation/full/" + localeCode, headers)
                .andReturn().getResponse().getStatus();
    }

    private void checkPatchCustomTranslation() throws Exception {
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":{\"alarm\":\"savealarm\"}, \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation, CustomTranslation.class);

        CustomTranslation savedCT = doGet("/api/translation/custom/" + ES_ES, CustomTranslation.class);
        assertThat(savedCT.getValue()).isEqualTo(esCustomTranslation);

        //patch with new value for "save.alarm" element
        JsonNode newValueForKeyWithDot = JacksonUtil.toJsonNode("{\"save.alarm\":\"new value\"}");
        doPatch("/api/translation/custom/" + ES_ES, newValueForKeyWithDot, CustomTranslation.class);

        JsonNode expectedResult = JacksonUtil.toJsonNode("{\"save\":{\"alarm\":\"new value\"}, \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        CustomTranslation updatedCT = doGet("/api/translation/custom/" + ES_ES, CustomTranslation.class);
        assertThat(updatedCT.getValue()).isEqualTo(expectedResult);

        JsonNode newValueForKeyWithoutDot = JacksonUtil.toJsonNode("{\"update\":\"update alarm\"}");
        doPatch("/api/translation/custom/" + ES_ES, newValueForKeyWithoutDot, CustomTranslation.class);

        JsonNode expectedResult2 = JacksonUtil.toJsonNode("{\"save\":{\"alarm\":\"new value\"}, \"update\" : \"update alarm\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        CustomTranslation updatedCT2 = doGet("/api/translation/custom/" + ES_ES, CustomTranslation.class);
        assertThat(updatedCT2.getValue()).isEqualTo(expectedResult2);
    }

    private void checkDeleteCustomTranslationKey(String parentValue, String currentValue) throws Exception {
        JsonNode esCustomTranslation = JacksonUtil.toJsonNode("{\"save\":{\"alarm\":\"" + currentValue + "\", \"device\":\"save device\"}, \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation, CustomTranslation.class);

        CustomTranslation savedCT = doGet("/api/translation/custom/" + ES_ES, CustomTranslation.class);
        assertThat(savedCT.getValue()).isEqualTo(esCustomTranslation);

        //delete key "save.alarm"
        String actualParentValue = doDelete("/api/translation/custom/" + ES_ES + "/" + "save.alarm", String.class);
        assertThat(actualParentValue).isEqualTo(parentValue);

        JsonNode expectedResult = JacksonUtil.toJsonNode("{\"save\":{\"device\":\"save device\"}, \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}");
        CustomTranslation updatedCT = doGet("/api/translation/custom/" + ES_ES, CustomTranslation.class);
        assertThat(updatedCT.getValue()).isEqualTo(expectedResult);

        //delete key "save"
        doDelete("/api/translation/custom/" + ES_ES + "/" + "save", String.class);

        JsonNode expectedResult2 = JacksonUtil.toJsonNode("{\"update\" : \"system\", \"remove\" : \"system\", \"search\":\"system\"}");
        CustomTranslation updatedCT2 = doGet("/api/translation/custom/" + ES_ES, CustomTranslation.class);
        assertThat(updatedCT2.getValue()).isEqualTo(expectedResult2);

        doPost("/api/translation/custom/" + ES_ES, esCustomTranslation, CustomTranslation.class);
    }

    private <R> CustomTranslation uploadCustomTranslation(HttpMethod httpMethod, String url, String mediaType, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.json", mediaType, content);
        var request = MockMvcRequestBuilders.multipart(httpMethod, url).file(file);
        setJwtToken(request);
        return readResponse(mockMvc.perform(request).andExpect(status().isOk()), CustomTranslation.class);
    }

}
