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
package org.thingsboard.server.msa.ui.tests.solutionTemplates.fleetTracking;

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
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FLEET_TRACKING_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_B_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_C_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_D_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_DEVICES_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_A_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FLEET_TRACKING_DASHBOARD_GROUP;

public class FleetTrackingInstallTest extends AbstractDriverBaseTest {
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
        testRestClient.deleteFleetTracking();
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void fleetTrackingOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        Assert.assertEquals(getUrl(), Const.URL + "/solutionTemplates/fleet_tracking");
        Assert.assertEquals(solutionTemplateDetailsPage.getHeadOfTitleCardName(), "Fleet tracking");
        Assert.assertTrue(solutionTemplateDetailsPage.getTitleCardParagraphText().contains("Fleet Tracking template"));
        Assert.assertTrue(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText().contains("Fleet Tracking template"));
        Assert.assertTrue(solutionTemplateDetailsPage.fleetTrackingScreenshotsAreCorrected());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installFleetTrackingFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphFleetTracking().getText().toLowerCase()
                .contains(FLEET_TRACKING_DASHBOARD.toLowerCase()));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphFleetTracking().getText().toLowerCase()
                .contains(FLEET_TRACKING_DASHBOARD.toLowerCase()));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installFleetTrackingFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphFleetTracking().getText().toLowerCase()
                .contains(FLEET_TRACKING_DASHBOARD.toLowerCase()));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphFleetTracking().getText().toLowerCase()
                .contains(FLEET_TRACKING_DASHBOARD.toLowerCase()));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallFleetTrackingPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallFleetTrackingPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        testRestClient.postFleetTracking();
        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(BUS_DEVICE_PROFILE).isDisplayed());

        sideBarMenuView.goToDeviceGroups();

        Assert.assertTrue(devicePage.entity(BUS_DEVICES_DEVICE_GROUP).isDisplayed());

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entity(BUS_A_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(BUS_B_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(BUS_C_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(BUS_D_DEVICE).isDisplayed());

        sideBarMenuView.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entity(FLEET_TRACKING_DASHBOARD).isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void fleetTrackingDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.fleetTrackingInstallBtn();
        testRestClient.postFleetTracking();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.fleetTrackingDeleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void fleetTrackingDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postFleetTracking();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }
    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void fleetTrackingOpenInstruction() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void fleetTrackingCloseInstruction() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void fleetTrackingCloseInstructionByCloseBtn() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void fleetTrackingDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void fleetTrackingDetailsPageOpenInstruction() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void fleetTrackingDetailsPageCloseInstruction() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void fleetTrackingDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void fleetTrackingInstructionGoToMainDashboard() {
        testRestClient.postFleetTracking();
        refreshPage();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteFleetTracking();
        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(BUS_DEVICE_PROFILE));

        sideBarMenuView.goToDeviceGroups();

        Assert.assertTrue(devicePage.entityIsNotPresent(BUS_DEVICES_DEVICE_GROUP));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(BUS_A_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(BUS_B_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(BUS_C_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(BUS_D_DEVICE));

        sideBarMenuView.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(FLEET_TRACKING_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Fleet tracking")
    @Test(groups = "broken")
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals(DASHBOARD_GIDE_DOCS_URL, guide);
        Assert.assertEquals(HTTP_API_DOCS_URL, linkHttpApi);
        Assert.assertEquals(CONNECTIVITY_DOCS_URL, linkConnectionDevices);
        Assert.assertEquals(ALARM_RULES_DOCS_URL, linkAlarmRule);
        Assert.assertEquals(getBaseUiUrl() + "/profiles/deviceProfiles", linkDeviceProfile);
    }
}
