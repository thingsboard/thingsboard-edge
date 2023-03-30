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
package org.thingsboard.server.msa.ui.tests.solutionTemplates.smartIrrigation;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
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
import org.thingsboard.server.msa.ui.pages.SchedulerPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.pages.SolutionTemplateDetailsPageHelper;
import org.thingsboard.server.msa.ui.pages.SolutionTemplatesHomePageElements;
import org.thingsboard.server.msa.ui.pages.SolutionTemplatesInstalledViewHelper;
import org.thingsboard.server.msa.ui.pages.UsersPageElements;
import org.thingsboard.server.msa.ui.utils.Const;

import java.util.Set;

import static org.thingsboard.server.msa.ui.utils.Const.CONNECTIVITY_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.HTTP_API_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.EVENING_SCHEDULER_EVENT;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.IRRIGATION_MANAGEMENT_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.MORNING_SCHEDULER_EVENT;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_COUNT_ALARMS_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_FIELD_1_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_FIELD_2_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_FIELD_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_FIELD_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SMART_VALVE_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SMART_VALVE_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SMART_VALVE_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SMART_VALVE_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_5_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_6_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_7_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_8_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_WATER_METER_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_WATER_METER_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_WATER_WATER_METER_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_WATER_WATER_METER_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_IRRIGATION_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_IRRIGATION_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_IRRIGATION_DEVICE_GROUP;

public class smartIrrigationInstallTest extends AbstractDriverBaseTest {
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
    SchedulerPageHelper schedulerPage;

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
        schedulerPage = new SchedulerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        testRestClient.deleteSmartIrrigation();
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void smartIrrigationOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        Assert.assertEquals(getUrl(), Const.URL + "/solutionTemplates/smart_irrigation");
        Assert.assertEquals(solutionTemplateDetailsPage.getHeadOfTitleCardName(), "Smart Irrigation");
        Assert.assertTrue(solutionTemplateDetailsPage.getTitleCardParagraphText().contains("Smart Irrigation template"));
        Assert.assertTrue(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText().contains("Smart Irrigation template"));
        Assert.assertTrue(solutionTemplateDetailsPage.smartIrrigationScreenshotsAreCorrected());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installSmartIrrigationFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartIrrigation().getText()
                .contains(IRRIGATION_MANAGEMENT_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsFirstParagraphSmartIrrigation().getText().contains(IRRIGATION_MANAGEMENT_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installSmartIrrigationFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartIrrigation().getText()
                .contains(IRRIGATION_MANAGEMENT_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsFirstParagraphSmartIrrigation().getText().contains(IRRIGATION_MANAGEMENT_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallIrrigationOfficePopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallSmartIrrigationPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(SI_COUNT_ALARMS_RULE_CHAIN).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(SI_FIELD_RULE_CHAIN).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(SI_SOIL_MOISTURE_RULE_CHAIN).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(SI_WATER_METER_RULE_CHAIN).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(SI_SMART_VALVE_RULE_CHAIN).isDisplayed());

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(SI_SMART_VALVE_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(SI_WATER_METER_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(SI_SOIL_MOISTURE_SENSOR_DEVICE_PROFILE).isDisplayed());;

        sideBarMenuView.goToDeviceGroups();

        Assert.assertTrue(devicePage.entity(SMART_IRRIGATION_DEVICE_GROUP).isDisplayed());

        devicePage.entity("All").click();
        devicePage.changeItemsCountPerPage(30);

        Assert.assertTrue(devicePage.entity(SI_WATER_WATER_METER_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_WATER_WATER_METER_2_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SMART_VALVE_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SMART_VALVE_2_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SOIL_MOISTURE_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SOIL_MOISTURE_2_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SOIL_MOISTURE_3_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SOIL_MOISTURE_4_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SOIL_MOISTURE_5_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SOIL_MOISTURE_6_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SOIL_MOISTURE_7_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SI_SOIL_MOISTURE_8_DEVICE).isDisplayed());

        sideBarMenuView.goToAssetGroups();

        Assert.assertTrue(assetPage.entity(SMART_IRRIGATION_ASSET_GROUP).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(SI_FIELD_1_ASSET).isDisplayed());
        Assert.assertTrue(assetPage.entity(SI_FIELD_2_ASSET).isDisplayed());

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entity(SI_FIELD_ASSET).isDisplayed());

        sideBarMenuView.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entity(IRRIGATION_MANAGEMENT_DASHBOARD).isDisplayed());

        sideBarMenuView.goToScheduler();

        Assert.assertEquals(schedulerPage.schedulers(MORNING_SCHEDULER_EVENT).size(), 2);
        Assert.assertEquals(schedulerPage.schedulers(EVENING_SCHEDULER_EVENT).size(), 2);
        schedulerPage.schedulers(MORNING_SCHEDULER_EVENT).forEach(element -> Assert.assertTrue(element.isDisplayed()));
        schedulerPage.schedulers(EVENING_SCHEDULER_EVENT).forEach(element -> Assert.assertTrue(element.isDisplayed()));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void temperatureSmartIrrigationDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartIrrigationInstallBtn();
        testRestClient.postSmartIrrigation();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.smartIrrigationDeleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void smartIrrigationDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartIrrigation();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void smartIrrigationOpenInstruction() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void smartIrrigationCloseInstruction() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void smartIrrigationCloseInstructionByCloseBtn() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void smartIrrigationInstructionGoToMainDashboard() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void smartIrrigationDetailsPageOpenInstruction() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void smartIrrigationDetailsPageCloseInstruction() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void smartIrrigationDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void smartIrrigationDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartIrrigation();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(SI_COUNT_ALARMS_RULE_CHAIN));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(SI_FIELD_RULE_CHAIN));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(SI_SOIL_MOISTURE_RULE_CHAIN));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(SI_WATER_METER_RULE_CHAIN));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(SI_SMART_VALVE_RULE_CHAIN));

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(SI_SMART_VALVE_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(SI_WATER_METER_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(SI_SOIL_MOISTURE_SENSOR_DEVICE_PROFILE));

        sideBarMenuView.goToDeviceGroups();

        Assert.assertTrue(devicePage.entityIsNotPresent(SMART_IRRIGATION_DEVICE_GROUP));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(SI_WATER_WATER_METER_1_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_WATER_WATER_METER_2_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SMART_VALVE_1_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SMART_VALVE_2_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SOIL_MOISTURE_1_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SOIL_MOISTURE_2_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SOIL_MOISTURE_3_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SOIL_MOISTURE_4_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SOIL_MOISTURE_5_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SOIL_MOISTURE_6_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SOIL_MOISTURE_7_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(SI_SOIL_MOISTURE_8_DEVICE));

        sideBarMenuView.goToAssetGroups();

        Assert.assertTrue(assetPage.entityIsNotPresent(SMART_IRRIGATION_ASSET_GROUP));

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entityIsNotPresent(SI_FIELD_1_ASSET));
        Assert.assertTrue(assetPage.entityIsNotPresent(SI_FIELD_2_ASSET));

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(SI_FIELD_ASSET));

        sideBarMenuView.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(IRRIGATION_MANAGEMENT_DASHBOARD));

        sideBarMenuView.goToScheduler();

        Assert.assertTrue(schedulerPage.schedulerIsNotPresent(MORNING_SCHEDULER_EVENT));
        Assert.assertTrue(schedulerPage.schedulerIsNotPresent(EVENING_SCHEDULER_EVENT));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Irrigation")
    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals(HTTP_API_DOCS_URL, linkHttpApi);
        Assert.assertEquals(CONNECTIVITY_DOCS_URL, linkConnectionDevices);
    }
}
