/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.service.mail.DefaultMailService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class BaseAdminControllerTest extends AbstractControllerTest {
    final JwtSettings defaultJwtSettings = new JwtSettings(9000, 604800, "thingsboard.io", "thingsboardDefaultSigningKey");

    @Autowired
    MailService mailService;

    @Autowired
    DefaultMailService defaultMailService;

    @Test
    public void testFindAdminSettingsByKey() throws Exception {
        loginSysAdmin();
        doGet("/api/admin/settings/general")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.key", is("general")))
                .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://localhost:8080")));

        doGet("/api/admin/settings/mail")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.key", is("mail")))
                .andExpect(jsonPath("$.jsonValue.smtpProtocol", is("smtp")))
                .andExpect(jsonPath("$.jsonValue.smtpHost", is("localhost")))
                .andExpect(jsonPath("$.jsonValue.smtpPort", is("25")));

        doGet("/api/admin/settings/unknown")
                .andExpect(status().isNotFound());

    }

    @Test
    public void testSaveAdminSettings() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/general", AdminSettings.class);

        JsonNode jsonValue = adminSettings.getJsonValue();
        ((ObjectNode) jsonValue).put("baseUrl", "http://myhost.org");
        adminSettings.setJsonValue(jsonValue);

        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());

        doGet("/api/admin/settings/general")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://myhost.org")));

        ((ObjectNode) jsonValue).put("baseUrl", "http://localhost:8080");
        adminSettings.setJsonValue(jsonValue);

        doPost("/api/admin/settings", adminSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveAdminSettingsWithEmptyKey() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        adminSettings.setKey(null);
        doPost("/api/admin/settings", adminSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Key should be specified")));
    }

    @Test
    public void testChangeAdminSettingsKey() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        adminSettings.setKey("newKey");
        doPost("/api/admin/settings", adminSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("is prohibited")));
    }

    @Test
    public void testSendTestMail() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        doPost("/api/admin/settings/testMail", adminSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testSendTestMailTimeout() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        ObjectNode objectNode = JacksonUtil.fromString(adminSettings.getJsonValue().toString(), ObjectNode.class);

        objectNode.put("smtpHost", "mail.gandi.net");
        objectNode.put("timeout", 1_000);
        objectNode.put("username", "username");
        objectNode.put("password", "password");

        adminSettings.setJsonValue(objectNode);

        Mockito.doAnswer((invocations) -> {
            var tenantId = (TenantId) invocations.getArgument(1);
            var jsonConfig = (JsonNode) invocations.getArgument(2);
            var email = (String) invocations.getArgument(3);

            defaultMailService.sendTestMail(tenantId, jsonConfig, email);
            return null;
        }).when(mailService).sendTestMail(Mockito.any(), Mockito.any(), Mockito.anyString());
        doPost("/api/admin/settings/testMail", adminSettings).andExpect(status().is5xxServerError());
        Mockito.doNothing().when(mailService).sendTestMail(Mockito.any(), Mockito.any(), Mockito.any());
    }

    void resetJwtSettingsToDefault() throws Exception {
        loginSysAdmin();
        doPost("/api/admin/jwtSettings", defaultJwtSettings).andExpect(status().isOk()); // jwt test scenarios are always started from
        loginTenantAdmin();
    }

    @Test
    public void testGetAndSaveDefaultJwtSettings() throws Exception {
        JwtSettings jwtSettings;
        loginSysAdmin();

        jwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(defaultJwtSettings);

        doPost("/api/admin/jwtSettings", jwtSettings).andExpect(status().isOk());

        jwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(defaultJwtSettings);

        resetJwtSettingsToDefault();
    }

    @Test
    public void testCreateJwtSettings() throws Exception {
        loginSysAdmin();

        JwtSettings jwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(defaultJwtSettings);

        jwtSettings.setTokenSigningKey(Base64.getEncoder().encodeToString(
                RandomStringUtils.randomAlphanumeric(256 / Byte.SIZE).getBytes(StandardCharsets.UTF_8)));

        doPost("/api/admin/jwtSettings", jwtSettings).andExpect(status().isOk());

        doGet("/api/admin/jwtSettings").andExpect(status().isUnauthorized()); //the old JWT token does not work after signing key was changed!

        loginSysAdmin();
        JwtSettings newJwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(newJwtSettings);

        resetJwtSettingsToDefault();
    }

}
