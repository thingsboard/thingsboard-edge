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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.mobile.LoginMobileInfo;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class MobileAppControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<MobileApp>> PAGE_DATA_MOBILE_APP_TYPE_REF = new TypeReference<>() {
    };

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();
    }

    @After
    public void tearDown() throws Exception {
        PageData<MobileApp> pageData = doGetTypedWithPageLink("/api/mobile/app?", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        for (MobileApp mobileApp : pageData.getData()) {
            doDelete("/api/mobile/app/" + mobileApp.getId().getId())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveMobileApp() throws Exception {
        PageData<MobileApp> pageData = doGetTypedWithPageLink("/api/mobile/app?", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        MobileApp mobileApp = validMobileApp("my.test.package", PlatformType.ANDROID);
        MobileApp savedMobileApp = doPost("/api/mobile/app", mobileApp, MobileApp.class);

        PageData<MobileApp> pageData2 = doGetTypedWithPageLink("/api/mobile/app?", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData2.getData()).hasSize(1);
        assertThat(pageData2.getData().get(0)).isEqualTo(savedMobileApp);

        MobileApp retrievedMobileAppInfo = doGet("/api/mobile/app/{id}", MobileApp.class, savedMobileApp.getId().getId());
        assertThat(retrievedMobileAppInfo).isEqualTo(savedMobileApp);

        // get mobile info
        LoginMobileInfo loginMobileInfo = doGet("/api/noauth/mobile?pkgName={pkgName}&platform={platform}", LoginMobileInfo.class, mobileApp.getPkgName(), mobileApp.getPlatformType());
        assertThat(loginMobileInfo.oAuth2ClientLoginInfos()).isEmpty();
        assertThat(loginMobileInfo.storeInfo()).isEqualTo(ModelConstants.MOBILE_APP_STORE_INFO_EMPTY_OBJECT);
        assertThat(loginMobileInfo.versionInfo()).isEqualTo(ModelConstants.MOBILE_APP_VERSION_INFO_EMPTY_OBJECT);
        assertThat(loginMobileInfo.selfRegistrationParams()).isNull();

        doDelete("/api/mobile/app/" + savedMobileApp.getId().getId());
        doGet("/api/mobile/app/{id}", savedMobileApp.getId().getId())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveMobileAppWithShortAppSecret() throws Exception {
        MobileApp mobileApp = validMobileApp("mobileApp.ce", PlatformType.ANDROID);
        mobileApp.setAppSecret("short");
        doPost("/api/mobile/app", mobileApp)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("appSecret must be at least 16 and max 2048 characters")));
    }

    @Test
    public void testGetTenantAppsByPlatformTypeSaveMobileApp() throws Exception {
        MobileApp androidApp = doPost("/api/mobile/app", validMobileApp("android.1", PlatformType.ANDROID), MobileApp.class);
        MobileApp androidApp2 = doPost("/api/mobile/app", validMobileApp("android.2", PlatformType.ANDROID), MobileApp.class);
        MobileApp iosApp = doPost("/api/mobile/app", validMobileApp("ios.1", PlatformType.IOS), MobileApp.class);

        PageData<MobileApp> pageData = doGetTypedWithPageLink("/api/mobile/app?", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).hasSize(3);
        assertThat(pageData.getData()).containsExactlyInAnyOrder(androidApp, androidApp2, iosApp);

        PageData<MobileApp> androidPageData = doGetTypedWithPageLink("/api/mobile/app?platformType=ANDROID&", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        assertThat(androidPageData.getData()).hasSize(2);
        assertThat(androidPageData.getData()).containsExactlyInAnyOrder(androidApp, androidApp2);
    }

    private MobileApp validMobileApp(String mobileAppName, PlatformType platformType) {
        MobileApp mobileApp = new MobileApp();
        mobileApp.setPkgName(mobileAppName);
        mobileApp.setAppSecret(StringUtils.randomAlphanumeric(24));
        mobileApp.setPlatformType(platformType);
        mobileApp.setStatus(MobileAppStatus.DRAFT);
        return mobileApp;
    }

}
