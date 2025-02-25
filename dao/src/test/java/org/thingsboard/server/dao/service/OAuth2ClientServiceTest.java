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

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
public class OAuth2ClientServiceTest extends AbstractServiceTest {

    @Autowired
    protected OAuth2ClientService oAuth2ClientService;

    @After
    public void after() {
        oAuth2ClientService.deleteByTenantId(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testSaveOauth2Client() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client", List.of(PlatformType.ANDROID));
        OAuth2Client savedOauth2Client = oAuth2ClientService.saveOAuth2Client(SYSTEM_TENANT_ID, oAuth2Client);

        OAuth2Client retrievedOauth2Client = oAuth2ClientService.findOAuth2ClientById(savedOauth2Client.getTenantId(), savedOauth2Client.getId());
        assertThat(retrievedOauth2Client).isEqualTo(savedOauth2Client);

        savedOauth2Client.setTitle("New title");
        OAuth2Client updatedOauth2Client = oAuth2ClientService.saveOAuth2Client(SYSTEM_TENANT_ID, savedOauth2Client);

        OAuth2Client retrievedOauth2Client2 = oAuth2ClientService.findOAuth2ClientById(savedOauth2Client.getTenantId(), savedOauth2Client.getId());
        assertThat(retrievedOauth2Client2).isEqualTo(updatedOauth2Client);
    }

    @Test
    public void testSaveOauth2ClientWithoutMapper() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client", List.of(PlatformType.ANDROID));
        oAuth2Client.setMapperConfig(null);

        assertThatThrownBy(() -> {
            oAuth2ClientService.saveOAuth2Client(TenantId.SYS_TENANT_ID, oAuth2Client);
        }).hasMessageContaining("mapperConfig must not be null");
    }

    @Test
    public void testSaveOauth2ClientWithoutCustomConfig() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client", List.of(PlatformType.ANDROID));
        oAuth2Client.getMapperConfig().setCustom(null);

        assertThatThrownBy(() -> {
            oAuth2ClientService.saveOAuth2Client(TenantId.SYS_TENANT_ID, oAuth2Client);
        }).hasMessageContaining("Custom config should be specified!");
    }

    @Test
    public void testSaveOauth2ClientWithoutCustomUrl() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client", List.of(PlatformType.ANDROID));
        oAuth2Client.getMapperConfig().setCustom(OAuth2CustomMapperConfig.builder().build());
        assertThatThrownBy(() -> {
            oAuth2ClientService.saveOAuth2Client(TenantId.SYS_TENANT_ID, oAuth2Client);
        }).hasMessageContaining("Custom mapper URL should be specified!");
    }

    @Test
    public void testGetTenantOAuth2Clients() {
        List<OAuth2Client> oAuth2Clients = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, StringUtils.randomAlphabetic(5));
            OAuth2Client savedOauth2Client = oAuth2ClientService.saveOAuth2Client(SYSTEM_TENANT_ID, oAuth2Client);
            oAuth2Clients.add(savedOauth2Client);
        }
        List<OAuth2Client> retrieved = oAuth2ClientService.findOAuth2ClientsByTenantIdAndCustomerId(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID));
        assertThat(retrieved).containsOnlyOnceElementsOf(oAuth2Clients);

        PageData<OAuth2ClientInfo> retrievedInfos = oAuth2ClientService.findOAuth2ClientInfosByTenantIdAndCustomerId(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), new PageLink(10));
        List<OAuth2ClientInfo> oAuth2ClientInfos = oAuth2Clients.stream().map(OAuth2ClientInfo::new).collect(Collectors.toList());
        assertThat(retrievedInfos.getData()).containsOnlyOnceElementsOf(oAuth2ClientInfos);
    }

}
