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

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.translation.CustomTranslationService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.id.TenantId.SYS_TENANT_ID;

@DaoSqlTest
public class CustomTranslationControllerTest extends AbstractControllerTest {

    private static final String ES_ES = "es_ES";

    @Autowired
    CustomTranslationService customTranslationService;
    @Autowired
    AdminSettingsDao  adminSettingsDao;

    @After
    public void afterTest() {
        if (adminSettingsDao.findByTenantIdAndKey(SYS_TENANT_ID.getId(), "customTranslation") != null) {
            adminSettingsDao.removeByTenantIdAndKey(SYS_TENANT_ID.getId(), "customTranslation");
        }
    }

    @Test
    public void shouldGetCorrectMergedCustomTranslation() throws Exception {
        loginSysAdmin();
        String esCustomTranslation = "{\"save\":\"system\", \"update\" : \"system\" ," +
                " \"remove\" : \"system\", \"search\":\"system\"}";
        updateSpanishCustomTranslation(esCustomTranslation);

        loginTenantAdmin();
        String esTenantCustomTranslation = "{\"update\" : \"tenant\" ," +
                " \"remove\" : \"tenant\", \"search\":\"tenant\"}";
        updateSpanishCustomTranslation(esTenantCustomTranslation);

        loginCustomerAdminUser();
        String esCustomerCustomTranslation = "{\"remove\" : \"customer\", \"search\":\"customer\"}";
        updateSpanishCustomTranslation(esCustomerCustomTranslation);

        loginSubCustomerAdminUser();
        String esSubCustomerCustomTranslation = "{\"search\":\"subCustomer\"}";
        updateSpanishCustomTranslation(esSubCustomerCustomTranslation);

        // get merged customer custom translation
        loginCustomerAdminUser();
        CustomTranslation mergedCustomTranslation = doGet("/api/customTranslation/customTranslation", CustomTranslation.class);
        String mergedEsCustomTranslation = mergedCustomTranslation.getTranslationMap().get(ES_ES);
        assertThat(JacksonUtil.toJsonNode(mergedEsCustomTranslation).get("save").asText()).isEqualTo("system");
        assertThat(JacksonUtil.toJsonNode(mergedEsCustomTranslation).get("update").asText()).isEqualTo("tenant");
        assertThat(JacksonUtil.toJsonNode(mergedEsCustomTranslation).get("remove").asText()).isEqualTo("customer");
        assertThat(JacksonUtil.toJsonNode(mergedEsCustomTranslation).get("search").asText()).isEqualTo("customer");

        // get merged subcustomer custom translation
        loginSubCustomerAdminUser();
        CustomTranslation mergedSubCustomerCustomTranslation = doGet("/api/customTranslation/customTranslation", CustomTranslation.class);
        String mergedEsSubCustomerCustomTranslation = mergedSubCustomerCustomTranslation.getTranslationMap().get(ES_ES);
        assertThat(JacksonUtil.toJsonNode(mergedEsSubCustomerCustomTranslation).get("save").asText()).isEqualTo("system");
        assertThat(JacksonUtil.toJsonNode(mergedEsSubCustomerCustomTranslation).get("update").asText()).isEqualTo("tenant");
        assertThat(JacksonUtil.toJsonNode(mergedEsSubCustomerCustomTranslation).get("remove").asText()).isEqualTo("customer");
        assertThat(JacksonUtil.toJsonNode(mergedEsSubCustomerCustomTranslation).get("search").asText()).isEqualTo("subCustomer");
    }

    private void updateSpanishCustomTranslation(String newCustomTranslation) throws Exception {
        CustomTranslation customTranslation = doGet("/api/customTranslation/currentCustomTranslation", CustomTranslation.class);
        Map<String, String> translationMap = new HashMap<>();

        translationMap.put(ES_ES, newCustomTranslation);
        customTranslation.setTranslationMap(translationMap);
        doPost("/api/customTranslation/customTranslation", customTranslation, CustomTranslation.class);

        CustomTranslation updatedCustomTranslation = doGet("/api/customTranslation/currentCustomTranslation", CustomTranslation.class);
        assertThat(updatedCustomTranslation.getTranslationMap().get(ES_ES)).isEqualTo(newCustomTranslation);
    }

}
