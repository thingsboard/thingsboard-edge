package org.thingsboard.server.msa.ui.tests.solutionTemplates.smartIrrigation;

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
        testRestClient.deleteSmartIrrigation();
    }

    @Test
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

    @Test
    public void installSmartIrrigationFromGeneralPage() {
        String dashboardGroupName = "Smart Irrigation";
        String dashboardName = "Irrigation Management";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartIrrigation().getText()
                .contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsFirstParagraphSmartIrrigation().getText().contains(dashboardName));
    }

    @Test
    public void installSmartIrrigationFromDetailsPage() {
        String dashboardGroupName = "Smart Irrigation";
        String dashboardName = "Irrigation Management";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartIrrigation().getText()
                .contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsFirstParagraphSmartIrrigation().getText().contains(dashboardName));
    }

    @Test
    public void closeInstallIrrigationOfficePopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void closeInstallSmartIrrigationPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void installEntities() {
        String ruleChainName = "SI Count Alarms";
        String ruleChain1Name = "SI Field";
        String ruleChain2Name = "SI Soil Moisture";
        String ruleChain3Name = "SI Water Meter";
        String ruleChain4Name = "SI Smart Valve";
        String deviceProfileName = "SI Smart Valve";
        String deviceProfile1Name = "SI Water Meter";
        String deviceProfile2Name = "SI Soil Moisture Sensor";
        String deviceGroupName = "Smart Irrigation";
        String deviceName = "SI Water Meter 1";
        String device1Name = "SI Water Meter 2";
        String device2Name = "SI Smart Valve 1";
        String device3Name = "SI Smart Valve 2";
        String device4Name = "SI Soil Moisture 1";
        String device5Name = "SI Soil Moisture 2";
        String device6Name = "SI Soil Moisture 3";
        String device7Name = "SI Soil Moisture 4";
        String device8Name = "SI Soil Moisture 5";
        String device9Name = "SI Soil Moisture 6";
        String device10Name = "SI Soil Moisture 7";
        String device11Name = "SI Soil Moisture 8";
        String assetGroupName = "Smart Irrigation";
        String assetName = "SI Field 1";
        String asset1Name = "SI Field 2";
        String assetProfileName = "SI Field";
        String dashboardName = "Irrigation Management";

        testRestClient.postSmartIrrigation();

        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(ruleChain1Name).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(ruleChain2Name).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(ruleChain3Name).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(ruleChain4Name).isDisplayed());

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(deviceProfileName).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile1Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile2Name).isDisplayed());;

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entity(deviceGroupName).isDisplayed());

        devicePage.entity("All").click();
        devicePage.changeItemsCountPerPage(30);

        Assert.assertTrue(devicePage.entity(deviceName).isDisplayed());
        Assert.assertTrue(devicePage.entity(device1Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device2Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device3Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device4Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device5Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device6Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device7Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device8Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device9Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device10Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device11Name).isDisplayed());

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entity(assetGroupName).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(assetName).isDisplayed());
        Assert.assertTrue(assetPage.entity(asset1Name).isDisplayed());

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entity(assetProfileName).isDisplayed());

        sideBarMenuView.dashboardGroupsBtn().click();
        dashboardPage.entity("All").click();

        Assert.assertTrue(dashboardPage.entity(dashboardName).isDisplayed());
    }

    @Test
    public void temperatureSmartIrrigationDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartIrrigationInstallBtn();
        testRestClient.postSmartIrrigation();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.smartIrrigationDeleteBtn().isDisplayed());
    }

    @Test
    public void smartIrrigationDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartIrrigation();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Test
    public void smartIrrigationOpenInstruction() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void smartIrrigationCloseInstruction() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartIrrigationCloseInstructionByCloseBtn() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartIrrigationInstructionGoToMainDashboard() {
        String dashboardGroupName = "Smart Irrigation";
        String dashboardName = "Irrigation Management";
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void smartIrrigationDetailsPageOpenInstruction() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void smartIrrigationDetailsPageCloseInstruction() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartIrrigationDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartIrrigationDetailsPageInstructionGoToMainDashboard() {
        String dashboardGroupName = "Smart Irrigation";
        String dashboardName = "Irrigation Management";
        testRestClient.postSmartIrrigation();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void deleteEntities() {
        String ruleChainName = "SI Count Alarms";
        String ruleChain1Name = "SI Field";
        String ruleChain2Name = "SI Soil Moisture";
        String ruleChain3Name = "SI Water Meter";
        String ruleChain4Name = "SI Smart Valve";
        String deviceProfileName = "SI Smart Valve";
        String deviceProfile1Name = "SI Water Meter";
        String deviceProfile2Name = "SI Soil Moisture Sensor";
        String deviceGroupName = "Smart Irrigation";
        String deviceName = "SI Water Meter 1";
        String device1Name = "SI Water Meter 2";
        String device2Name = "SI Smart Valve 1";
        String device3Name = "SI Smart Valve 2";
        String device4Name = "SI Soil Moisture 1";
        String device5Name = "SI Soil Moisture 2";
        String device6Name = "SI Soil Moisture 3";
        String device7Name = "SI Soil Moisture 4";
        String device8Name = "SI Soil Moisture 5";
        String device9Name = "SI Soil Moisture 6";
        String device10Name = "SI Soil Moisture 7";
        String device11Name = "SI Soil Moisture 8";
        String assetGroupName = "Smart Irrigation";
        String assetName = "SI Field 1";
        String asset1Name = "SI Field 2";
        String assetProfileName = "SI Field";
        String dashboardName = "Irrigation Management";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartIrrigation();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChainName));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChain1Name));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChain2Name));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChain3Name));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChain4Name));

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfileName));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile1Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile2Name));

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceGroupName));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceName));
        Assert.assertTrue(devicePage.entityIsNotPresent(device1Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device2Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device3Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device4Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device5Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device6Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device7Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device8Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device9Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device10Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device11Name));

        sideBarMenuView.assetGroups().click();

        Assert.assertTrue(assetPage.entityIsNotPresent(assetGroupName));

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entityIsNotPresent(assetName));
        Assert.assertTrue(assetPage.entityIsNotPresent(asset1Name));

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(assetProfileName));

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardName));
    }

    @Test()
    public void linksBtn() {
        String dashboardGroupName = "Smart Irrigation";
        String dashboardName = "Irrigation Management";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals("https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api", linkHttpApi);
        Assert.assertEquals("https://thingsboard.io/docs/getting-started-guides/connectivity/", linkConnectionDevices);
    }
}
