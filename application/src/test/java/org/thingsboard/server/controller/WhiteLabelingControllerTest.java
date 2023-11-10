/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.wl.WhiteLabelingDao;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.id.TenantId.SYS_TENANT_ID;

@DaoSqlTest
public class WhiteLabelingControllerTest extends AbstractControllerTest {

    @Autowired
    WhiteLabelingDao whiteLabelingDao;

    @After
    public void afterTest(){
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey();
        key.setType(WhiteLabelingType.MAIL_TEMPLATES);
        key.setEntityType(EntityType.TENANT.name());
        key.setEntityId(tenantId.getId());

        if (whiteLabelingDao.findById(SYS_TENANT_ID, key) != null) {
            whiteLabelingDao.removeById(SYS_TENANT_ID, key);
        }

        key.setEntityId(SYS_TENANT_ID.getId());

        if (whiteLabelingDao.findById(SYS_TENANT_ID, key) != null) {
            whiteLabelingDao.removeById(SYS_TENANT_ID, key);
        }
    }

    @Test
    public void shouldUpdateWhiteLabelParams() throws Exception {
        loginSysAdmin();
        updateAppTitleAndVerify("New title");

        loginTenantAdmin();
        updateAppTitleAndVerify("New title 2");

        loginCustomerAdminUser();
        updateAppTitleAndVerify("New title 3");
    }

    @Test
    public void shouldUpdateLoginWhiteLabelParams() throws Exception {
        loginSysAdmin();
        updateBaseUrlAndVerify("domain.com");

        loginTenantAdmin();
        updateDomainNameAndVerify("domain2.com");

        loginCustomerAdminUser();
        LoginWhiteLabelingParams loginWhiteLabelingParams = updateDomainNameAndVerify("domain3.com");

        //check update settings for registered domain should be prohibited
        loginWhiteLabelingParams.setDomainName("domain2.com");
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams)
                .andExpect(status().isBadRequest());

        loginDifferentTenant();
        LoginWhiteLabelingParams differentTenantWLParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
        differentTenantWLParams.setDomainName("domain.com");
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldGetCorrectMergedWhiteLabelParams() throws Exception {
        loginSysAdmin();
        WhiteLabelingParams systemWhiteLabelParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);

        String appName = "SystemName";
        String faviconUrl = "http://testUrl";

        systemWhiteLabelParams.setAppTitle(appName);
        systemWhiteLabelParams.setFavicon(new Favicon(faviconUrl));
        doPost("/api/whiteLabel/whiteLabelParams", systemWhiteLabelParams, WhiteLabelingParams.class);

        WhiteLabelingParams systemMergedParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(systemMergedParams.getAppTitle()).isEqualTo(appName);
        assertThat(systemMergedParams.getFavicon().getUrl()).isEqualTo(faviconUrl);

        loginTenantAdmin();
        WhiteLabelingParams tenantWhiteLabelParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);

        String tenantAppName = "TenantAppName";
        String logoImageUrl = "http://logoImageUrl";
        tenantWhiteLabelParams.setAppTitle(tenantAppName);
        tenantWhiteLabelParams.setLogoImageUrl(logoImageUrl);
        doPost("/api/whiteLabel/whiteLabelParams", tenantWhiteLabelParams, WhiteLabelingParams.class);

        WhiteLabelingParams tenantMergedParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(tenantMergedParams.getAppTitle()).isEqualTo(tenantAppName);
        assertThat(tenantMergedParams.getFavicon().getUrl()).isEqualTo(faviconUrl);
        assertThat(tenantMergedParams.getLogoImageUrl()).isEqualTo(logoImageUrl);

        loginCustomerAdminUser();
        WhiteLabelingParams customerWhiteLabelParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);

        String customerAppName = "CustomerAppName";
        customerWhiteLabelParams.setAppTitle(customerAppName);
        doPost("/api/whiteLabel/whiteLabelParams", customerWhiteLabelParams, WhiteLabelingParams.class);

        WhiteLabelingParams customerMergedParams = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(customerMergedParams.getAppTitle()).isEqualTo(customerAppName);
        assertThat(customerMergedParams.getFavicon().getUrl()).isEqualTo(faviconUrl);
        assertThat(customerMergedParams.getLogoImageUrl()).isEqualTo(logoImageUrl);
    }

    @Test
    public void shouldGetCorrectMergedLoginWhiteLabelParams() throws Exception {
        loginSysAdmin();
        LoginWhiteLabelingParams systemloginWhiteLabelParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);

        systemloginWhiteLabelParams.setDarkForeground(true);
        doPost("/api/whiteLabel/loginWhiteLabelParams", systemloginWhiteLabelParams, WhiteLabelingParams.class);

        LoginWhiteLabelingParams mergedLoginWhiteLabelParams = doGet("/api/noauth/whiteLabel/loginWhiteLabelParams", LoginWhiteLabelingParams.class);
        assertThat(mergedLoginWhiteLabelParams.isDarkForeground()).isTrue();
    }

    @Test
    public void shouldUpdateSystemMailTemplates() throws Exception {
        loginSysAdmin();
        JsonNode mailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);

        String newSubject = "new subject" + StringUtils.randomAlphabetic(5);
        ((ObjectNode) mailTemplates).put("subject", newSubject);
        doPost("/api/whiteLabel/mailTemplates", mailTemplates, JsonNode.class);

        JsonNode updatedMailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);
        assertThat(updatedMailTemplates.get("subject").asText()).isEqualTo(newSubject);
    }

    @Test
    public void shouldUpdateTenantMailTemplates() throws Exception {
        loginTenantAdmin();
        JsonNode mailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);

        String newSubject = "new subject" + StringUtils.randomAlphabetic(5);
        ((ObjectNode) mailTemplates).put("subject", newSubject);
        doPost("/api/whiteLabel/mailTemplates", mailTemplates, JsonNode.class);

        JsonNode updatedMailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);
        assertThat(updatedMailTemplates.get("subject").asText()).isEqualTo(newSubject);
    }

    @Test
    public void shouldGetSystemMailTemplatesIfTenantOneDoesNotExistsAndSystemDefaultSetToTrue() throws Exception {
        loginSysAdmin();
        JsonNode systemMailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);
        String newSubject = "new subject" + StringUtils.randomAlphabetic(5);
        ((ObjectNode) systemMailTemplates).put("subject", newSubject);
        doPost("/api/whiteLabel/mailTemplates", systemMailTemplates, JsonNode.class);

        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(EntityType.TENANT.name(),
                tenantId.getId(), WhiteLabelingType.MAIL_TEMPLATES);
        if (whiteLabelingDao.findById(tenantId, key) != null) {
            whiteLabelingDao.removeById(tenantId, key);
        }

        loginTenantAdmin();
        JsonNode tenantMailTemplates = doGet("/api/whiteLabel/mailTemplates?systemByDefault=true", JsonNode.class);
        assertThat(tenantMailTemplates.get("subject")).isEqualTo(systemMailTemplates.get("subject"));
    }

    @Test
    public void shouldNotSendMailIfMailTemplateContainsVulnerableCodeExecution() throws Exception {
        loginTenantAdmin();
        JsonNode mailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);

        JsonNode badTemplate = JacksonUtil.toJsonNode("{\"subject\":\"Test message from ThingsBoard tenant\",\"body\":\"<#assign ex=\\\"freemarker.template.utility.Execute\\\"?new()> ${ex(\\\"id\\\")}\"}");
        ((ObjectNode) mailTemplates).set("test", badTemplate);
        ((ObjectNode) mailTemplates).put("useSystemMailSettings", false);
        doPost("/api/whiteLabel/mailTemplates", mailTemplates, JsonNode.class);

        mailTemplates = doGet("/api/whiteLabel/mailTemplates", JsonNode.class);
        assertThat(mailTemplates.get("test")).isEqualTo(badTemplate);

        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        ObjectNode objectNode = JacksonUtil.fromString(adminSettings.getJsonValue().toString(), ObjectNode.class);
        objectNode.put("smtpHost", "smtp.gmail.com");
        objectNode.put("smtpPort", "465");
        objectNode.put("smtpProtocol", "smtps");
        objectNode.put("mailFrom", "testMail@gmail.com");
        objectNode.put("timeout", 1_000);
        objectNode.put("username", "username");
        objectNode.put("password", "password");
        adminSettings.setJsonValue(objectNode);

        //send test mail
        MvcResult mvcResult = doPost("/api/admin/settings/testMail", adminSettings).andExpect(status().is5xxServerError())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString()).contains("Unable to send mail: Instantiating freemarker.template.utility.Execute is not allowed in the template for security reasons");

        //update templates
        badTemplate = JacksonUtil.toJsonNode("{\"subject\":\"Test message from ThingsBoard tenant\",\"body\":\"<#assign uri=object?api.class.getResource(\\\"/\\\").toURI()>\"}");
        ((ObjectNode) mailTemplates).set("test", badTemplate);
        doPost("/api/whiteLabel/mailTemplates", mailTemplates, JsonNode.class);

        //send test mail
        mvcResult = doPost("/api/admin/settings/testMail", adminSettings).andExpect(status().is5xxServerError())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString()).contains("Unable to send mail: Can't use ?api, because the \\\"api_builtin_enabled\\\" configuration setting is false.");
    }

    @Test
    public void updateLoginWhiteLabelParamsForCustomerByTenant() throws Exception {
        loginTenantAdmin();
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams?customerId=" + customerId, LoginWhiteLabelingParams.class);

        String domainName = "customer-domain.com";
        loginWhiteLabelingParams.setDomainName(domainName);

        doPost("/api/whiteLabel/loginWhiteLabelParams?customerId=" + customerId, loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        LoginWhiteLabelingParams updated = doGet("/api/whiteLabel/currentLoginWhiteLabelParams?customerId=" + customerId, LoginWhiteLabelingParams.class);
        assertThat(updated).isEqualTo(loginWhiteLabelingParams);

        loginCustomerAdminUser();
        LoginWhiteLabelingParams updated2 = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
        assertThat(updated2).isEqualTo(loginWhiteLabelingParams);
    }

    @Test
    public void shouldNotUpdateLoginWhiteLabelParamsForAnotherCustomerByTenant() throws Exception {
        loginTenantAdmin();
        doGet("/api/whiteLabel/currentLoginWhiteLabelParams?customerId=" + differentTenantCustomerId);

        LoginWhiteLabelingParams loginWhiteLabelingParams = new LoginWhiteLabelingParams();
        doPost("/api/whiteLabel/loginWhiteLabelParams?customerId=" + differentTenantCustomerId, loginWhiteLabelingParams);
    }

    @Test
    public void updateWhiteLabelParamsForCustomerByTenant() throws Exception {
        loginTenantAdmin();
        WhiteLabelingParams whiteLabelingParams = doGet("/api/whiteLabel/currentWhiteLabelParams?customerId=" + customerId, WhiteLabelingParams.class);

        String appTile = "customer title";
        whiteLabelingParams.setAppTitle(appTile);

        doPost("/api/whiteLabel/whiteLabelParams?customerId=" + customerId, whiteLabelingParams, WhiteLabelingParams.class);

        WhiteLabelingParams updated = doGet("/api/whiteLabel/currentWhiteLabelParams?customerId=" + customerId, WhiteLabelingParams.class);
        assertThat(updated).isEqualTo(whiteLabelingParams);

        loginCustomerAdminUser();
        WhiteLabelingParams updated2 = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);
        assertThat(updated2).isEqualTo(whiteLabelingParams);
    }

    @Test
    public void shouldNotUpdateWhiteLabelParamsForAnotherCustomerByTenant() throws Exception {
        loginTenantAdmin();
        doGet("/api/whiteLabel/currentWhiteLabelParams?customerId=" + differentTenantCustomerId);

        LoginWhiteLabelingParams loginWhiteLabelingParams = new LoginWhiteLabelingParams();
        doPost("/api/whiteLabel/currentWhiteLabelParams?customerId=" + differentTenantCustomerId, loginWhiteLabelingParams);
    }

    private void updateAppTitleAndVerify(String appTile) throws Exception {

        WhiteLabelingParams whiteLabelingParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);

        whiteLabelingParams.setAppTitle(appTile);
        doPost("/api/whiteLabel/whiteLabelParams", whiteLabelingParams, WhiteLabelingParams.class);

        Awaitility.await("Waiting while whitelabel params is updated")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> appTile.equals(doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class).getAppTitle()));
    }

    private LoginWhiteLabelingParams updateBaseUrlAndVerify(String baseUrl) throws Exception {
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);

        loginWhiteLabelingParams.setBaseUrl(baseUrl);
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        Awaitility.await("Waiting while login whitelabel params is updated")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> baseUrl.equals(doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class).getBaseUrl()));

        return doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
    }

    private LoginWhiteLabelingParams updateDomainNameAndVerify(String domainName) throws Exception {
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);

        loginWhiteLabelingParams.setDomainName(domainName);
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        Awaitility.await("Waiting while login whitelabel params is updated")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> domainName.equals(doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class).getDomainName()));

        return doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
    }

}
