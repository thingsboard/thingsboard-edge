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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.common.data.mobile.MobileAppInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.mobile.MobileAppService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.dao.oauth2.OAuth2Utils.OAUTH2_AUTHORIZATION_PATH_TEMPLATE;

@DaoSqlTest
public class MobileAppServiceTest extends AbstractServiceTest {

    @Autowired
    protected MobileAppService mobileAppService;

    @Autowired
    protected OAuth2ClientService oAuth2ClientService;

    @After
    public void after() {
        mobileAppService.deleteByTenantId(TenantId.SYS_TENANT_ID);
        oAuth2ClientService.deleteByTenantId(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testSaveMobileApp() {
        MobileApp MobileApp = validMobileApp(TenantId.SYS_TENANT_ID, "mobileApp.ce", true);
        MobileApp savedMobileApp = mobileAppService.saveMobileApp(SYSTEM_TENANT_ID, MobileApp);

        MobileApp retrievedMobileApp = mobileAppService.findMobileAppById(savedMobileApp.getTenantId(), savedMobileApp.getId());
        assertThat(retrievedMobileApp).isEqualTo(savedMobileApp);

        // update MobileApp name
        savedMobileApp.setPkgName("mobileApp.pe");
        MobileApp updatedMobileApp = mobileAppService.saveMobileApp(SYSTEM_TENANT_ID, savedMobileApp);

        MobileApp retrievedMobileApp2 = mobileAppService.findMobileAppById(savedMobileApp.getTenantId(), savedMobileApp.getId());
        assertThat(retrievedMobileApp2).isEqualTo(updatedMobileApp);

        //delete MobileApp
        mobileAppService.deleteMobileAppById(SYSTEM_TENANT_ID, savedMobileApp.getId());
        assertThat(mobileAppService.findMobileAppById(SYSTEM_TENANT_ID, savedMobileApp.getId())).isNull();
    }

    @Test
    public void testGetTenantMobileApps() {
        List<MobileApp> MobileApps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MobileApp oAuth2Client = validMobileApp(TenantId.SYS_TENANT_ID, StringUtils.randomAlphabetic(5), true);
            MobileApp savedOauth2Client = mobileAppService.saveMobileApp(SYSTEM_TENANT_ID, oAuth2Client);
            MobileApps.add(savedOauth2Client);
        }
        PageData<MobileAppInfo> retrieved = mobileAppService.findMobileAppInfosByTenantId(TenantId.SYS_TENANT_ID, new PageLink(10, 0));
        List<MobileAppInfo> MobileAppInfos = MobileApps.stream().map(MobileApp -> new MobileAppInfo(MobileApp, Collections.emptyList())).toList();
        assertThat(retrieved.getData()).containsOnlyOnceElementsOf(MobileAppInfos);
    }

    @Test
    public void tesGetMobileAppInfo() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client");
        OAuth2Client savedOauth2Client = oAuth2ClientService.saveOAuth2Client(SYSTEM_TENANT_ID, oAuth2Client);
        List<OAuth2ClientInfo> oAuth2ClientInfosByIds = oAuth2ClientService.findOAuth2ClientInfosByIds(TenantId.SYS_TENANT_ID, List.of(savedOauth2Client.getId()));

        MobileApp MobileApp = validMobileApp(TenantId.SYS_TENANT_ID, "my.app", true);
        MobileApp savedMobileApp = mobileAppService.saveMobileApp(SYSTEM_TENANT_ID, MobileApp);

        mobileAppService.updateOauth2Clients(TenantId.SYS_TENANT_ID, savedMobileApp.getId(), List.of(savedOauth2Client.getId()));

        // check MobileApp info
        MobileAppInfo retrievedInfo = mobileAppService.findMobileAppInfoById(SYSTEM_TENANT_ID, savedMobileApp.getId());
        assertThat(retrievedInfo).isEqualTo(new MobileAppInfo(savedMobileApp, oAuth2ClientInfosByIds));

        //find clients by MobileApp name
        List<OAuth2ClientLoginInfo> oauth2LoginInfo = oAuth2ClientService.findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType(savedMobileApp.getName(), null);
        assertThat(oauth2LoginInfo).containsOnly(new OAuth2ClientLoginInfo(savedOauth2Client.getLoginButtonLabel(), savedOauth2Client.getLoginButtonIcon(), String.format(OAUTH2_AUTHORIZATION_PATH_TEMPLATE, savedOauth2Client.getUuidId().toString())));
    }

    private MobileApp validMobileApp(TenantId tenantId, String mobileAppName, boolean oauth2Enabled) {
        MobileApp MobileApp = new MobileApp();
        MobileApp.setTenantId(tenantId);
        MobileApp.setPkgName(mobileAppName);
        MobileApp.setAppSecret(StringUtils.randomAlphanumeric(24));
        MobileApp.setOauth2Enabled(oauth2Enabled);
        return MobileApp;
    }
}
