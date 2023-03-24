package org.thingsboard.server.msa.ui.tests.solutionTemplates.smartOffice;

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

    @Test
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

    @Test
    public void installSmartOfficeFromGeneralPage() {
        String dashboardGroupName = "Smart office dashboards";
        String dashboardName = "Smart office";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartOffice().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphSmartOffice().getText().contains(dashboardName));
    }

    @Test
    public void installSmartOfficeFromDetailsPage() {
        String dashboardGroupName = "Smart office dashboards";
        String dashboardName = "Smart office";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartOffice().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphSmartOffice().getText().contains(dashboardName));
    }

    @Test
    public void closeInstallSmartOfficePopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void closeInstallSmartOfficePopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void installEntities() {
        String deviceProfileName = "smart-sensor";
        String deviceProfile1Name = "hvac";
        String deviceProfile2Name = "energy-meter";
        String deviceProfile3Name = "water-meter";
        String deviceGroupName = "Office sensors";
        String deviceName = "Smart sensor";
        String device1Name = "HVAC";
        String device2Name = "Energy meter";
        String device3Name = "Water meter";
        String assetGroupName = "Buildings";
        String assetName = "Office";
        String assetProfileName = "office";
        String dashboardName = "Smart office";

        testRestClient.postSmartOffice();

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(deviceProfileName).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile1Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile2Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile3Name).isDisplayed());

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entity(deviceGroupName).isDisplayed());

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entity(deviceName).isDisplayed());
        Assert.assertTrue(devicePage.entity(device1Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device2Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device3Name).isDisplayed());

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entity(assetGroupName).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(assetName).isDisplayed());

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entity(assetProfileName).isDisplayed());

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entity(dashboardName).isDisplayed());
    }

    @Test
    public void temperatureSmartOfficeDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartOfficeInstallBtn();
        testRestClient.postSmartOffice();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.smartOfficeDeleteBtn().isDisplayed());
    }

    @Test
    public void smartOfficeDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartOffice();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Test
    public void smartOfficeOpenInstruction() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void smartOfficeCloseInstruction() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartOfficeCloseInstructionByCloseBtn() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartOfficeInstructionGoToMainDashboard() {
        String dashboardGroupName = "Smart office dashboards";
        String dashboardName = "Smart office";
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void smartOfficeDetailsPageOpenInstruction() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void smartOfficeDetailsPageCloseInstruction() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartOfficeDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartOfficeDetailsPageInstructionGoToMainDashboard() {
        String dashboardGroupName = "Smart office dashboards";
        String dashboardName = "Smart office";
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void deleteEntities() {
        String deviceProfileName = "smart-sensor";
        String deviceProfile1Name = "hvac";
        String deviceProfile2Name = "energy-meter";
        String deviceProfile3Name = "water-meter";
        String deviceGroupName = "Office sensors";
        String deviceName = "Smart sensor";
        String device1Name = "HVAC";
        String device2Name = "Energy meter";
        String device3Name = "Water meter";
        String assetGroupName = "Buildings";
        String assetName = "Office";
        String assetProfileName = "office";
        String dashboardName = "Smart office";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartOffice();
        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfileName));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile1Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile2Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile3Name));

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceGroupName));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceName));
        Assert.assertTrue(devicePage.entityIsNotPresent(device1Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device2Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device3Name));

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entityIsNotPresent(assetGroupName));

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entityIsNotPresent(assetName));

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(assetProfileName));

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardName));
    }

    @Test(groups = "broken")
    public void linksBtn() {
        String dashboardGroupName = "Smart office dashboards";
        String dashboardName = "Smart office";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals("https://thingsboard.io/docs/user-guide/dashboards/", guide);
        Assert.assertEquals("https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api", linkHttpApi);
        Assert.assertEquals("https://thingsboard.io/docs/getting-started-guides/connectivity/", linkConnectionDevices);
        Assert.assertEquals("https://thingsboard.io/docs/user-guide/device-profiles/#alarm-rules", linkAlarmRule);
        Assert.assertEquals("http://localhost:8080/profiles/deviceProfiles", linkDeviceProfile);
    }
}
