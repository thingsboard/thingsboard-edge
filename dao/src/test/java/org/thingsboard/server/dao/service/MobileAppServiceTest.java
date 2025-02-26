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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.mobile.MobileAppService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        MobileApp MobileApp = validMobileApp(SYSTEM_TENANT_ID, "mobileApp.ce", PlatformType.IOS);
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
        List<MobileApp> mobileApps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MobileApp oAuth2Client = validMobileApp(SYSTEM_TENANT_ID, StringUtils.randomAlphabetic(5), PlatformType.ANDROID);
            MobileApp savedOauth2Client = mobileAppService.saveMobileApp(SYSTEM_TENANT_ID, oAuth2Client);
            mobileApps.add(savedOauth2Client);
        }
        PageData<MobileApp> retrieved = mobileAppService.findMobileAppsByTenantId(TenantId.SYS_TENANT_ID, null, new PageLink(10, 0));
        assertThat(retrieved.getData()).containsOnlyOnceElementsOf(mobileApps);
    }

    private MobileApp validMobileApp(TenantId tenantId, String mobileAppName, PlatformType platformType) {
        MobileApp MobileApp = new MobileApp();
        MobileApp.setTenantId(tenantId);
        MobileApp.setPkgName(mobileAppName);
        MobileApp.setStatus(MobileAppStatus.DRAFT);
        MobileApp.setAppSecret(StringUtils.randomAlphanumeric(24));
        MobileApp.setPlatformType(platformType);
        return MobileApp;
    }
}
