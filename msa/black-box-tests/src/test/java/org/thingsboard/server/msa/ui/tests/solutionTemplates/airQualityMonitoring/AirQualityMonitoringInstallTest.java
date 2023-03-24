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
        String ruleChainName = "AQI Sensor";
        String ruleChain1Name = "AQI City";
        String deviceGroupName = "Air Quality Monitoring";
        String deviceName = "Air Quality Sensor 1";
        String device1Name = "Air Quality Sensor 2";
        String device2Name = "Air Quality Sensor 3";
        String device3Name = "Air Quality Sensor 4";
        String device4Name = "Air Quality Sensor 5";
        String assetGroupName = "Air Quality Monitoring";
        String assetProfileName = "AQI City";
        String assetName = "Los Angeles, CA";
        String dashboardName = "Air Quality Monitoring";
        String dashboard1Name = "Air Quality Monitoring Administration";

        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(ruleChain1Name).isDisplayed());

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entity(deviceGroupName).isDisplayed());

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entity(deviceName).isDisplayed());
        Assert.assertTrue(devicePage.entity(device1Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device2Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device3Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device4Name).isDisplayed());

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entity(assetGroupName).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(assetName).isDisplayed());

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entity(assetProfileName).isDisplayed());

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entity(dashboardName).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(dashboard1Name).isDisplayed());
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
        String ruleChainName = "AQI Sensor";
        String ruleChain1Name = "AQI City";
        String deviceGroupName = "Air Quality Monitoring";
        String deviceName = "Air Quality Sensor 1";
        String device1Name = "Air Quality Sensor 2";
        String device2Name = "Air Quality Sensor 3";
        String device3Name = "Air Quality Sensor 4";
        String device4Name = "Air Quality Sensor 5";
        String assetGroupName = "Air Quality Monitoring";
        String assetProfileName = "AQI City";
        String assetName = "Los Angeles, CA";
        String dashboardName = "Air Quality Monitoring";
        String dashboard1Name = "Air Quality Monitoring Administration";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteAirQualityMonitoring();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChainName));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChain1Name));

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceGroupName));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceName));
        Assert.assertTrue(devicePage.entityIsNotPresent(device1Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device2Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device3Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device4Name));

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entityIsNotPresent(assetGroupName));

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entityIsNotPresent(assetName));

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(assetProfileName));

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardName));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboard1Name));
    }

    @Test(groups = "broken")
    public void linksBtn() {
        String dashboardGroupName = "Air Quality Monitoring Public";
        String dashboardName = "Air Quality Monitoring";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkAlarmRules = solutionTemplatesInstalledView.getAlarmRulesLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals("https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules", linkAlarmRules);
        Assert.assertEquals("https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api", linkHttpApi);
        Assert.assertEquals("https://thingsboard.io/docs/getting-started-guides/connectivity/", linkConnectionDevices);
        Assert.assertEquals("https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules", linkAlarmRule);
        Assert.assertEquals("http://localhost:8080/profiles/deviceProfiles", linkDeviceProfile);
    }
}
