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
package org.thingsboard.server.msa.ui.tests.solutionTemplates.airQualityMonitoring;

import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.AssetPageElements;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DashboardPageHelper;
import org.thingsboard.server.msa.ui.pages.DevicePageElements;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.RolesPageElements;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.pages.SolutionTemplateDetailsPageHelper;
import org.thingsboard.server.msa.ui.pages.SolutionTemplatesHomePageElements;
import org.thingsboard.server.msa.ui.pages.SolutionTemplatesInstalledViewHelper;
import org.thingsboard.server.msa.ui.pages.UsersPageElements;
import org.thingsboard.server.msa.ui.utils.Const;

import java.util.Set;

import static org.thingsboard.server.msa.TestProperties.getBaseUiUrl;
import static org.thingsboard.server.msa.ui.utils.Const.ALARM_RULES_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.CONNECTIVITY_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.HTTP_API_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LOS_ANGELES_CA_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_CITY_ASSET_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_5_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_CITY_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_SENSOR_RULE_CHAIN;

public class AirQualityMonitoringInstallTest extends AbstractDriverBaseTest {
    SideBarMenuViewHelper sideBarMenuView;
    SolutionTemplatesHomePageElements solutionTemplatesHomePage;
    SolutionTemplateDetailsPageHelper solutionTemplateDetailsPage;
    SolutionTemplatesInstalledViewHelper solutionTemplatesInstalledView;
    RuleChainsPageHelper ruleChainsPage;
    RolesPageElements rolesPage;
    CustomerPageHelper customerPage;
    ProfilesPageHelper profilesPage;
    DevicePageElements devicePage;
    AssetPageElements assetPage;
    DashboardPageHelper dashboardPage;
    UsersPageElements usersPageHelper;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        solutionTemplatesHomePage = new SolutionTemplatesHomePageElements(driver);
        solutionTemplateDetailsPage = new SolutionTemplateDetailsPageHelper(driver);
        solutionTemplatesInstalledView = new SolutionTemplatesInstalledViewHelper(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
        rolesPage = new RolesPageElements(driver);
        customerPage = new CustomerPageHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
        devicePage = new DevicePageElements(driver);
        assetPage = new AssetPageElements(driver);
        dashboardPage = new DashboardPageHelper(driver);
        usersPageHelper = new UsersPageElements(driver);
    }

    @AfterMethod
    public void delete() {
        testRestClient.deleteAirQualityMonitoring();
    }

    @Test
    public void airQualityMonitoringOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        Assert.assertEquals(getUrl(), Const.URL + "/solutionTemplates/air_quality_index");
        Assert.assertEquals(solutionTemplateDetailsPage.getHeadOfTitleCardName(), "Air Quality Monitoring");
        Assert.assertTrue(solutionTemplateDetailsPage.getTitleCardParagraphText().contains("AIR Quality Monitoring template"));
        Assert.assertTrue(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText().contains("Air Quality Monitoring template"));
        Assert.assertTrue(solutionTemplateDetailsPage.airQualityMonitoringScreenshotsAreCorrected());
    }

    @Test
    public void installAirQualityMonitoringFromGeneralPage() {
        String dashboardGroupName = "Air Quality Monitoring Public";
        String dashboardName = "Air Quality Monitoring";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.alarmFirstParagraphAirQualityMonitoring().getText().contains(dashboardName));
    }

    @Test
    public void installAirQualityMonitoringFromDetailsPage() {
        String dashboardGroupName = "Air Quality Monitoring Public";
        String dashboardName = "Air Quality Monitoring";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.alarmFirstParagraphAirQualityMonitoring().getText().contains(dashboardName));
    }

    @Test
    public void closeInstallAirQualityMonitoringPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void closeInstallAirQualityMonitoringPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void installEntities() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(AQI_SENSOR_RULE_CHAIN).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(AQI_CITY_RULE_CHAIN).isDisplayed());

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entity(AIR_QUALITY_MONITORING_DEVICE_GROUP).isDisplayed());

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entity(AIR_QUALITY_SENSOR_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(AIR_QUALITY_SENSOR_2_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(AIR_QUALITY_SENSOR_3_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(AIR_QUALITY_SENSOR_4_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(AIR_QUALITY_SENSOR_5_DEVICE).isDisplayed());

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entity(AIR_QUALITY_MONITORING_ASSET_GROUP).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(LOS_ANGELES_CA_ASSET).isDisplayed());

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entity(AQI_CITY_ASSET_PROFILE).isDisplayed());

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entity(AIR_QUALITY_MONITORING_DASHBOARD).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD).isDisplayed());
    }

    @Test
    public void airQualityMonitoringDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.airQualityMonitoringInstallBtn();
        testRestClient.postAirQualityMonitoring();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.airQualityMonitoringDeleteBtn().isDisplayed());
    }

    @Test
    public void airQualityMonitoringDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postAirQualityMonitoring();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Test
    public void airQualityMonitoringOpenInstruction() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void airQualityMonitoringCloseInstruction() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void airQualityMonitoringCloseInstructionByCloseBtn() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void airQualityMonitoringDetailsPageInstructionGoToMainDashboard() {
        String dashboardGroupName = "Air Quality Monitoring Public";
        String dashboardName = "Air Quality Monitoring";
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void airQualityMonitoringDetailsPageOpenInstruction() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void airQualityMonitoringDetailsPageDetailsPageCloseInstruction() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void airQualityMonitoringDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void airQualityMonitoringInstructionGoToMainDashboard() {
        String dashboardGroupName = "Air Quality Monitoring Public";
        String dashboardName = "Air Quality Monitoring";
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteAirQualityMonitoring();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(AQI_SENSOR_RULE_CHAIN));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(AQI_CITY_RULE_CHAIN));

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entityIsNotPresent(AIR_QUALITY_MONITORING_DEVICE_GROUP));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(AIR_QUALITY_SENSOR_1_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(AIR_QUALITY_SENSOR_2_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(AIR_QUALITY_SENSOR_3_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(AIR_QUALITY_SENSOR_4_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(AIR_QUALITY_SENSOR_5_DEVICE));

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entityIsNotPresent(AIR_QUALITY_MONITORING_ASSET_GROUP));

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entityIsNotPresent(LOS_ANGELES_CA_ASSET));

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(AQI_CITY_ASSET_PROFILE));

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(AIR_QUALITY_MONITORING_DASHBOARD));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD));
    }

    @Test(groups = "broken")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkAlarmRules = solutionTemplatesInstalledView.getAlarmRulesLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals(ALARM_RULES_DOCS_URL, linkAlarmRules);
        Assert.assertEquals(HTTP_API_DOCS_URL, linkHttpApi);
        Assert.assertEquals(CONNECTIVITY_DOCS_URL, linkConnectionDevices);
        Assert.assertEquals(getBaseUiUrl() + "/profiles/deviceProfiles", linkDeviceProfile);
    }
}
