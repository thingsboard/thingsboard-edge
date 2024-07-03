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
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.thingsboard.server.common.data.mobile.AndroidConfig;
import org.thingsboard.server.common.data.mobile.IosConfig;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class MobileApplicationControllerTest extends AbstractControllerTest {

    @Value("${cache.specs.mobileSecretKey.timeToLiveInMinutes:2}")
    private int mobileSecretKeyTtl;
    private static final String ANDROID_PACKAGE_NAME = "testAppPackage";
    private static final String ANDROID_APP_SHA256 = "DF:28:32:66:8B:A7:D3:EC:7D:73:CF:CC";
    private static final String APPLE_APP_ID = "testId";
    private static final String TEST_LABEL = "Test label";

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();

        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        QRCodeConfig qrCodeConfig = new QRCodeConfig();
        qrCodeConfig.setQrCodeLabel(TEST_LABEL);

        mobileAppSettings.setUseDefaultApp(true);
        AndroidConfig androidConfig = AndroidConfig.builder()
                .appPackage(ANDROID_PACKAGE_NAME)
                .sha256CertFingerprints(ANDROID_APP_SHA256)
                .enabled(true)
                .build();

        IosConfig iosConfig = IosConfig.builder()
                .appId(APPLE_APP_ID)
                .enabled(true)
                .build();
        mobileAppSettings.setAndroidConfig(androidConfig);
        mobileAppSettings.setIosConfig(iosConfig);
        mobileAppSettings.setQrCodeConfig(qrCodeConfig);

        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveMobileAppSettings() throws Exception {
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        assertThat(mobileAppSettings.getQrCodeConfig().getQrCodeLabel()).isEqualTo(TEST_LABEL);
        assertThat(mobileAppSettings.isUseDefaultApp()).isTrue();

        mobileAppSettings.setUseDefaultApp(false);

        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());

        MobileAppSettings updatedMobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        assertThat(updatedMobileAppSettings.isUseDefaultApp()).isFalse();

        loginTenantAdmin();
        MobileAppSettings retrievedMobileAppSettingsByTenant = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        assertThat(retrievedMobileAppSettingsByTenant.isUseSystemSettings()).isTrue();

        //check qr code config has system settings
        QRCodeConfig qrCodeConfig = doGet("/api/mobile/app/settings/merged", MobileAppSettings.class).getQrCodeConfig();
        assertThat(qrCodeConfig.getQrCodeLabel()).isEqualTo(TEST_LABEL);

        //check deep link configured based on system settings
        String deepLink = doGet("/api/mobile/deepLink", String.class);

        Pattern deepLinkPattern = Pattern.compile("\"https://([^/]+)/api/noauth/qr\\?secret=([^&]+)&ttl=([^&]+)\"");
        Matcher parsedDeepLink = deepLinkPattern.matcher(deepLink);
        assertThat(parsedDeepLink.matches()).isTrue();
        assertThat(parsedDeepLink.group(1)).isEqualTo("localhost");

        loginCustomerUser();
        //check qr code config has system settings
        QRCodeConfig customerUSerQrCodeConfig = doGet("/api/mobile/app/settings/merged", MobileAppSettings.class).getQrCodeConfig();
        assertThat(customerUSerQrCodeConfig.getQrCodeLabel()).isEqualTo(TEST_LABEL);

        //check deep link configured based on system settings
        String customerDeepLink = doGet("/api/mobile/deepLink", String.class);
        Matcher customerParsedDeepLink = deepLinkPattern.matcher(customerDeepLink);
        assertThat(customerParsedDeepLink.matches()).isTrue();
        assertThat(customerParsedDeepLink.group(1)).isEqualTo("localhost");

        loginSysAdmin();
        mobileAppSettings.setUseDefaultApp(true);
        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());

        loginTenantAdmin();
        MobileAppSettings tenantMobileAppSettingsAfterUpdate = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        assertThat(tenantMobileAppSettingsAfterUpdate.isUseDefaultApp()).isTrue();

        //check qr code config has system settings
        QRCodeConfig tenantQrCodeConfig = doGet("/api/mobile/app/settings/merged", MobileAppSettings.class).getQrCodeConfig();
        assertThat(tenantQrCodeConfig.getQrCodeLabel()).isEqualTo(TEST_LABEL);

        //check deep link configured based on system settings
        String tenantDeepLink = doGet("/api/mobile/deepLink", String.class);

        Pattern deepLinkPatternWithHost = Pattern.compile("\"https://([^/]+)/api/noauth/qr\\?secret=([^&]+)&ttl=([^&]+)&host=([^&]+)\"");
        Matcher tenantParsedDeepLink = deepLinkPatternWithHost.matcher(tenantDeepLink);
        assertThat(tenantParsedDeepLink.matches()).isTrue();
        assertThat(tenantParsedDeepLink.group(1)).isEqualTo("thingsboard.cloud");

        loginCustomerUser();
        //check qr code config has system settings
        QRCodeConfig customerQrCodeConfigDefaultApp = doGet("/api/mobile/app/settings/merged", MobileAppSettings.class).getQrCodeConfig();
        assertThat(customerQrCodeConfigDefaultApp.getQrCodeLabel()).isEqualTo(TEST_LABEL);

        //check deep link configured based on system settings
        String customerDeepLinkAfterUpdate = doGet("/api/mobile/deepLink", String.class);
        Matcher customerParsedDeepLinkDefaultApp = deepLinkPatternWithHost.matcher(customerDeepLinkAfterUpdate);
        assertThat(customerParsedDeepLinkDefaultApp.matches()).isTrue();
        assertThat(customerParsedDeepLinkDefaultApp.group(1)).isEqualTo("thingsboard.cloud");

    }

    @Test
    public void testShouldNotSaveMobileAppSettingsWithoutRequiredConfig() throws Exception {
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);

        mobileAppSettings.setUseDefaultApp(false);
        mobileAppSettings.setAndroidConfig(null);
        mobileAppSettings.setIosConfig(null);
        mobileAppSettings.setQrCodeConfig(null);

        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Android/ios settings are required to use custom application!")));

        mobileAppSettings.setAndroidConfig(AndroidConfig.builder().enabled(false).build());
        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Android/ios settings are required to use custom application!")));

        mobileAppSettings.setIosConfig(IosConfig.builder().enabled(false).build());
        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Qr code configuration is required!")));

        mobileAppSettings.setQrCodeConfig(QRCodeConfig.builder().showOnHomePage(false).build());
        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testShouldNotSaveMobileAppSettingsWithoutRequiredAndroidConf() throws Exception {
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        mobileAppSettings.setUseDefaultApp(false);
        AndroidConfig androidConfig = AndroidConfig.builder()
                .enabled(true)
                .appPackage(null)
                .sha256CertFingerprints(null)
                .build();
        mobileAppSettings.setAndroidConfig(androidConfig);

        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Application package and sha256 cert fingerprints are required for custom android application!")));

        androidConfig.setAppPackage("test_app_package");
        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Application package and sha256 cert fingerprints are required for custom android application!")));

        androidConfig.setSha256CertFingerprints("test_sha_256");
        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testShouldNotSaveMobileAppSettingsWithoutRequiredIosConf() throws Exception {
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        mobileAppSettings.setUseDefaultApp(false);
        IosConfig iosConfig = IosConfig.builder()
                .enabled(true)
                .appId(null)
                .build();
        mobileAppSettings.setIosConfig(iosConfig);

        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Application id is required for custom ios application!")));

        iosConfig.setAppId("test_app_id");
        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testShouldSaveMobileAppSettingsForDefaultApp() throws Exception {
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        mobileAppSettings.setUseDefaultApp(true);
        mobileAppSettings.setIosConfig(null);
        mobileAppSettings.setAndroidConfig(null);

        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testGetApplicationAssociations() throws Exception {
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        mobileAppSettings.setUseDefaultApp(false);
        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());

        JsonNode assetLinks = doGet("/.well-known/assetlinks.json", JsonNode.class);
        assertThat(assetLinks.get(0).get("target").get("package_name").asText()).isEqualTo(ANDROID_PACKAGE_NAME);
        assertThat(assetLinks.get(0).get("target").get("sha256_cert_fingerprints").get(0).asText()).isEqualTo(ANDROID_APP_SHA256);

        JsonNode appleAssociation = doGet("/.well-known/apple-app-site-association", JsonNode.class);
        assertThat(appleAssociation.get("applinks").get("details").get(0).get("appID").asText()).isEqualTo(APPLE_APP_ID);
    }

    @Test
    public void testGetMobileDeepLink() throws Exception {
        loginSysAdmin();
        String deepLink = doGet("/api/mobile/deepLink", String.class);

        Pattern expectedPattern = Pattern.compile("\"https://([^/]+)/api/noauth/qr\\?secret=([^&]+)&ttl=([^&]+)&host=([^&]+)\"");
        Matcher parsedDeepLink = expectedPattern.matcher(deepLink);
        assertThat(parsedDeepLink.matches()).isTrue();
        String appHost = parsedDeepLink.group(1);
        String secret = parsedDeepLink.group(2);
        String ttl = parsedDeepLink.group(3);
        assertThat(appHost).isEqualTo("thingsboard.cloud");
        assertThat(ttl).isEqualTo(String.valueOf(mobileSecretKeyTtl));

        JwtPair jwtPair = doGet("/api/noauth/qr/" + secret, JwtPair.class);
        assertThat(jwtPair).isNotNull();

        loginTenantAdmin();
        String tenantDeepLink = doGet("/api/mobile/deepLink", String.class);
        Matcher tenantParsedDeepLink = expectedPattern.matcher(tenantDeepLink);
        assertThat(tenantParsedDeepLink.matches()).isTrue();
        String tenantSecret = tenantParsedDeepLink.group(2);

        JwtPair tenantJwtPair =  doGet("/api/noauth/qr/" + tenantSecret, JwtPair.class);
        assertThat(tenantJwtPair).isNotNull();

        loginCustomerUser();
        String customerDeepLink = doGet("/api/mobile/deepLink", String.class);
        Matcher customerParsedDeepLink = expectedPattern.matcher(customerDeepLink);
        assertThat(customerParsedDeepLink.matches()).isTrue();
        String customerSecret = customerParsedDeepLink.group(2);

        JwtPair customerJwtPair = doGet("/api/noauth/qr/" + customerSecret, JwtPair.class);
        assertThat(customerJwtPair).isNotNull();

        // update mobile setting to use custom one
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        mobileAppSettings.setUseDefaultApp(false);
        doPost("/api/mobile/app/settings", mobileAppSettings);

        String customAppDeepLink = doGet("/api/mobile/deepLink", String.class);
        Pattern customAppExpectedPattern = Pattern.compile("\"https://([^/]+)/api/noauth/qr\\?secret=([^&]+)&ttl=([^&]+)\"");
        Matcher customAppParsedDeepLink = customAppExpectedPattern.matcher(customAppDeepLink);
        assertThat(customAppParsedDeepLink.matches()).isTrue();
        assertThat(customAppParsedDeepLink.group(1)).isEqualTo("localhost");

        loginTenantAdmin();
        String tenantCustomAppDeepLink = doGet("/api/mobile/deepLink", String.class);
        Matcher tenantCustomAppParsedDeepLink = customAppExpectedPattern.matcher(tenantCustomAppDeepLink);
        assertThat(tenantCustomAppParsedDeepLink.matches()).isTrue();
        assertThat(tenantCustomAppParsedDeepLink.group(1)).isEqualTo("localhost");

        loginCustomerUser();
        String customerCustomAppDeepLink = doGet("/api/mobile/deepLink", String.class);
        Matcher customerCustomAppParsedDeepLink = customAppExpectedPattern.matcher(customerCustomAppDeepLink);
        assertThat(customerCustomAppParsedDeepLink.matches()).isTrue();
        assertThat(customerCustomAppParsedDeepLink.group(1)).isEqualTo("localhost");
    }

    @Test
    public void shouldGetCorrectDeepLinkForTenantsWithWhiteLabeling() throws Exception {
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        assertThat(mobileAppSettings.getQrCodeConfig().getQrCodeLabel()).isEqualTo(TEST_LABEL);
        assertThat(mobileAppSettings.isUseDefaultApp()).isTrue();

        mobileAppSettings.setUseDefaultApp(false);

        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());

        loginTenantAdmin();
        String domainName = "my.domain.name";
        updateDomainNameAndBaseUrl(domainName, domainName);

        //check deep link
        String deepLinkForWrongBaseUrl = doGet("/api/mobile/deepLink", String.class);

        Pattern expectedPattern = Pattern.compile("\"https://([^/]+)/api/noauth/qr\\?secret=([^&]+)&ttl=([^&]+)\"");
        Matcher parsedDeepLink = expectedPattern.matcher(deepLinkForWrongBaseUrl);
        assertThat(parsedDeepLink.matches()).isTrue();
        String appHost = parsedDeepLink.group(1);
        assertThat(appHost).isEqualTo(domainName);

        // update base url to correct one
        updateDomainNameAndBaseUrl("my.domain.name","https://my.domain.name");

        //check deep link
        String deepLinkForCorrectBaseUrl = doGet("/api/mobile/deepLink", String.class);

        Matcher parsedDeepLinkAfterUpdate = expectedPattern.matcher(deepLinkForCorrectBaseUrl);
        assertThat(parsedDeepLinkAfterUpdate.matches()).isTrue();
        String appHostAfterUpdate = parsedDeepLink.group(1);
        assertThat(appHostAfterUpdate).isEqualTo(domainName);

    }

    private LoginWhiteLabelingParams updateDomainNameAndBaseUrl(String domainName, String baseUrl) throws Exception {
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);

        loginWhiteLabelingParams.setDomainName(domainName);
        loginWhiteLabelingParams.setBaseUrl(baseUrl);
        loginWhiteLabelingParams.setProhibitDifferentUrl(true);
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        Awaitility.await("Waiting while login whitelabel params is updated")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> baseUrl.equals(doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class).getBaseUrl()));

        return doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
    }

}
