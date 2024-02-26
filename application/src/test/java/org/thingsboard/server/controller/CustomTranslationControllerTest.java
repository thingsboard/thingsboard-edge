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
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.translation.CustomTranslationService;


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
        JsonNode mergedEsCustomTranslation = doGet("/api/customTranslation/customTranslation/" + ES_ES, JsonNode.class);
        assertThat(mergedEsCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(mergedEsCustomTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(mergedEsCustomTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(mergedEsCustomTranslation.get("search").asText()).isEqualTo("customer");

        // get merged subcustomer custom translation
        loginSubCustomerAdminUser();
        JsonNode mergedSubCustomerCustomTranslation = doGet("/api/customTranslation/customTranslation/" + ES_ES, JsonNode.class);
        assertThat(mergedSubCustomerCustomTranslation.get("save").asText()).isEqualTo("system");
        assertThat(mergedSubCustomerCustomTranslation.get("update").asText()).isEqualTo("tenant");
        assertThat(mergedSubCustomerCustomTranslation.get("remove").asText()).isEqualTo("customer");
        assertThat(mergedSubCustomerCustomTranslation.get("search").asText()).isEqualTo("subCustomer");
    }

    private void updateSpanishCustomTranslation(JsonNode newCustomTranslation) throws Exception {
        doPost("/api/customTranslation/customTranslation/" + ES_ES, newCustomTranslation, CustomTranslation.class);

        CustomTranslation updatedCustomTranslation = doGet("/api/customTranslation/currentCustomTranslation/" + ES_ES, CustomTranslation.class);
        assertThat(updatedCustomTranslation.getValue()).isEqualTo(newCustomTranslation);
    }

}
