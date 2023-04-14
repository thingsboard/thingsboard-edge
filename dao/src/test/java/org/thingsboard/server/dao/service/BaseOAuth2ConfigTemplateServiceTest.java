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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public abstract class BaseOAuth2ConfigTemplateServiceTest extends AbstractServiceTest {

    @Autowired
    protected OAuth2ConfigTemplateService oAuth2ConfigTemplateService;

    @Before
    public void beforeRun() throws Exception {
        Assert.assertTrue(oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().isEmpty());
    }

    @After
    public void after() throws Exception {
        oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().forEach(clientRegistrationTemplate -> {
            oAuth2ConfigTemplateService.deleteClientRegistrationTemplateById(clientRegistrationTemplate.getId());
        });

        Assert.assertTrue(oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().isEmpty());
    }


    @Test
    public void testSaveDuplicateProviderId() {
        OAuth2ClientRegistrationTemplate first = validClientRegistrationTemplate("providerId");
        OAuth2ClientRegistrationTemplate second = validClientRegistrationTemplate("providerId");
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(first);
        Assertions.assertThrows(DataValidationException.class, () -> {
            oAuth2ConfigTemplateService.saveClientRegistrationTemplate(second);
        });
    }

    @Test
    public void testCreateNewTemplate() {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = validClientRegistrationTemplate(UUID.randomUUID().toString());
        OAuth2ClientRegistrationTemplate savedClientRegistrationTemplate = oAuth2ConfigTemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);

        Assert.assertNotNull(savedClientRegistrationTemplate);
        Assert.assertNotNull(savedClientRegistrationTemplate.getId());
        clientRegistrationTemplate.setId(savedClientRegistrationTemplate.getId());
        clientRegistrationTemplate.setCreatedTime(savedClientRegistrationTemplate.getCreatedTime());
        Assert.assertEquals(clientRegistrationTemplate, savedClientRegistrationTemplate);
    }

    @Test
    public void testFindTemplate() {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = validClientRegistrationTemplate(UUID.randomUUID().toString());
        OAuth2ClientRegistrationTemplate savedClientRegistrationTemplate = oAuth2ConfigTemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);

        OAuth2ClientRegistrationTemplate foundClientRegistrationTemplate = oAuth2ConfigTemplateService.findClientRegistrationTemplateById(savedClientRegistrationTemplate.getId());
        Assert.assertEquals(savedClientRegistrationTemplate, foundClientRegistrationTemplate);
    }

    @Test
    public void testFindAll() {
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));

        Assert.assertEquals(2, oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().size());
    }

    @Test
    public void testDeleteTemplate() {
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));
        OAuth2ClientRegistrationTemplate saved = oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));

        Assert.assertEquals(3, oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().size());
        Assert.assertNotNull(oAuth2ConfigTemplateService.findClientRegistrationTemplateById(saved.getId()));

        oAuth2ConfigTemplateService.deleteClientRegistrationTemplateById(saved.getId());

        Assert.assertEquals(2, oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().size());
        Assert.assertNull(oAuth2ConfigTemplateService.findClientRegistrationTemplateById(saved.getId()));
    }

    private OAuth2ClientRegistrationTemplate validClientRegistrationTemplate(String providerId) {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = new OAuth2ClientRegistrationTemplate();
        clientRegistrationTemplate.setProviderId(providerId);
        clientRegistrationTemplate.setAdditionalInfo(JacksonUtil.newObjectNode().put(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        clientRegistrationTemplate.setMapperConfig(OAuth2MapperConfig.builder()
                .type(MapperType.BASIC)
                .basic(OAuth2BasicMapperConfig.builder()
                        .firstNameAttributeKey("firstName")
                        .lastNameAttributeKey("lastName")
                        .emailAttributeKey("email")
                        .tenantNamePattern("tenant")
                        .defaultDashboardName("Test")
                        .alwaysFullScreen(true)
                        .userGroupsNamePattern(Collections.singletonList("Tenant Administrators"))
                        .build()
                )
                .build());
        clientRegistrationTemplate.setAuthorizationUri("authorizationUri");
        clientRegistrationTemplate.setAccessTokenUri("tokenUri");
        clientRegistrationTemplate.setScope(Arrays.asList("scope1", "scope2"));
        clientRegistrationTemplate.setUserInfoUri("userInfoUri");
        clientRegistrationTemplate.setUserNameAttributeName("userNameAttributeName");
        clientRegistrationTemplate.setJwkSetUri("jwkSetUri");
        clientRegistrationTemplate.setClientAuthenticationMethod("clientAuthenticationMethod");
        clientRegistrationTemplate.setComment("comment");
        clientRegistrationTemplate.setLoginButtonIcon("icon");
        clientRegistrationTemplate.setLoginButtonLabel("label");
        clientRegistrationTemplate.setHelpLink("helpLink");
        return clientRegistrationTemplate;
    }
}
