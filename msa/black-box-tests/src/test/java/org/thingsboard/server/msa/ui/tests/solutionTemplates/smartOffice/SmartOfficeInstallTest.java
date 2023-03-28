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
package org.thingsboard.server.msa.ui.tests.solutionTemplates.smartOffice;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.openqa.selenium.WebElement;
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
import static org.thingsboard.server.msa.ui.utils.Const.DASHBOARD_GIDE_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.HTTP_API_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUILDINGS_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OFFICE_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OFFICE_ASSET_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_OFFICE_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.HVAC_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.ENERGY_METER_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_OFFICE_DASHBOARDS_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METER_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OFFICE_SENSORS_DEVICE_GROUPS;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SENSOR_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.HVAC_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.ENERGY_METER_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_DEVICE_PROFILE_SO;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SENSOR_DEVICE_PROFILE;

public class SmartOfficeInstallTest extends AbstractDriverBaseTest {
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
        testRestClient.deleteSmartOffice();
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void smartOfficeOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        Assert.assertEquals(Const.URL + "/solutionTemplates/smart_office", getUrl());
        Assert.assertEquals(solutionTemplateDetailsPage.getHeadOfTitleCardName(), "Smart office");
        Assert.assertTrue(solutionTemplateDetailsPage.getTitleCardParagraphText().contains("Smart Office template"));
        Assert.assertTrue(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText().contains("Smart Office template"));
        Assert.assertTrue(solutionTemplateDetailsPage.smartOfficeScreenshotsAreCorrected());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installSmartOfficeFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartOffice().getText().contains(SMART_OFFICE_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphSmartOffice().getText().contains(SMART_OFFICE_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installSmartOfficeFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartOffice().getText().contains(SMART_OFFICE_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphSmartOffice().getText().contains(SMART_OFFICE_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallSmartOfficePopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallSmartOfficePopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        testRestClient.postSmartOffice();
        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(SMART_SENSOR_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(HVAC_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(ENERGY_METER_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(WATER_METERING_DEVICE_PROFILE_SO).isDisplayed());

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entity(OFFICE_SENSORS_DEVICE_GROUPS).isDisplayed());

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entity(SMART_SENSOR_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(HVAC_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(ENERGY_METER_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(WATER_METER_DEVICE).isDisplayed());

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entity(BUILDINGS_ASSET_GROUP).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(OFFICE_ASSET).isDisplayed());

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entity(OFFICE_ASSET_PROFILE).isDisplayed());

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entity(SMART_OFFICE_DASHBOARD).isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void temperatureSmartOfficeDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartOfficeInstallBtn();
        testRestClient.postSmartOffice();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.smartOfficeDeleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void smartOfficeDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartOffice();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void smartOfficeOpenInstruction() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void smartOfficeCloseInstruction() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void smartOfficeCloseInstructionByCloseBtn() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void smartOfficeInstructionGoToMainDashboard() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void smartOfficeDetailsPageOpenInstruction() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void smartOfficeDetailsPageCloseInstruction() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void smartOfficeDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void smartOfficeDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartOffice();
        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(SMART_SENSOR_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(HVAC_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(ENERGY_METER_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(WATER_METERING_DEVICE_PROFILE_SO));

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entityIsNotPresent(OFFICE_SENSORS_DEVICE_GROUPS));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(SMART_SENSOR_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(HVAC_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(ENERGY_METER_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(WATER_METER_DEVICE));

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entityIsNotPresent(BUILDINGS_ASSET_GROUP));

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entityIsNotPresent(OFFICE_ASSET));

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(OFFICE_ASSET_PROFILE));

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(SMART_OFFICE_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart office")
    @Test(groups = "broken")
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals(DASHBOARD_GIDE_DOCS_URL, guide);
        Assert.assertEquals(HTTP_API_DOCS_URL, linkHttpApi);
        Assert.assertEquals(CONNECTIVITY_DOCS_URL, linkConnectionDevices);
        Assert.assertEquals(ALARM_RULES_DOCS_URL, linkAlarmRule);
        Assert.assertEquals(getBaseUiUrl() + "/profiles/deviceProfiles", linkDeviceProfile);
    }
}
