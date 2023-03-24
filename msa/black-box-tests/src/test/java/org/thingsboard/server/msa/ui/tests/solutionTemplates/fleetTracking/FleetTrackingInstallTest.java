package org.thingsboard.server.msa.ui.tests.solutionTemplates.fleetTracking;

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

    @Test
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

    @Test
    public void installFleetTrackingFromGeneralPage() {
        String name = "Fleet tracking";
        String nameFromView = "Fleet Tracking";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, name, name).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, name).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphFleetTracking().getText().contains(nameFromView));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphFleetTracking().getText().contains(nameFromView));
    }

    @Test
    public void installFleetTrackingFromDetailsPage() {
        String name = "Fleet tracking";
        String nameFromView = "Fleet Tracking";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, name, name).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, name).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphFleetTracking().getText().contains(nameFromView));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphFleetTracking().getText().contains(nameFromView));
    }

    @Test
    public void closeInstallFleetTrackingPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void closeInstallFleetTrackingPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void installEntities() {
        String deviceProfileName = "bus";
        String deviceGroupName = "Bus devices";
        String deviceName = "Bus A";
        String device1Name = "Bus B";
        String device2Name = "Bus C";
        String device3Name = "Bus D";
        String dashboardName = "Fleet tracking";

        testRestClient.postFleetTracking();

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(deviceProfileName).isDisplayed());

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entity(deviceGroupName).isDisplayed());

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entity(deviceName).isDisplayed());
        Assert.assertTrue(devicePage.entity(device1Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device2Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device3Name).isDisplayed());

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entity(dashboardName).isDisplayed());
    }

    @Test
    public void fleetTrackingDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.fleetTrackingInstallBtn();
        testRestClient.postFleetTracking();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.fleetTrackingDeleteBtn().isDisplayed());
    }

    @Test
    public void fleetTrackingDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postFleetTracking();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Test
    public void fleetTrackingOpenInstruction() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void fleetTrackingCloseInstruction() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void fleetTrackingCloseInstructionByCloseBtn() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void fleetTrackingDetailsPageInstructionGoToMainDashboard() {
        String name = "Fleet tracking";
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, name, name).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, name).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void fleetTrackingDetailsPageOpenInstruction() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void fleetTrackingDetailsPageCloseInstruction() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void fleetTrackingDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void fleetTrackingInstructionGoToMainDashboard() {
        String name = "Fleet tracking";
        testRestClient.postFleetTracking();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, name, name).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, name).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void deleteEntities() {
        String deviceProfileName = "bus";
        String deviceGroupName = "Bus devices";
        String deviceName = "Bus A";
        String device1Name = "Bus B";
        String device2Name = "Bus C";
        String device3Name = "Bus D";
        String dashboardName = "Fleet tracking";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteFleetTracking();
        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfileName));

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceGroupName));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceName));
        Assert.assertTrue(devicePage.entityIsNotPresent(device1Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device2Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device3Name));

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardName));
    }

    @Test(groups = "broken")
    public void linksBtn() {
        String name = "Fleet tracking";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, name, name).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, name).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals("https://thingsboard.io/docs/user-guide/dashboards/", guide);
        Assert.assertEquals("https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api", linkHttpApi);
        Assert.assertEquals("https://thingsboard.io/docs/getting-started-guides/connectivity/", linkConnectionDevices);
        Assert.assertEquals("https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules", linkAlarmRule);
        Assert.assertEquals("http://localhost:8080/profiles/deviceProfiles", linkDeviceProfile);
    }
}
