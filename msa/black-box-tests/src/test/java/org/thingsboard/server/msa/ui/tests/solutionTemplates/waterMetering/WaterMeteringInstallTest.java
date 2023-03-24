package org.thingsboard.server.msa.ui.tests.solutionTemplates.waterMetering;

import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
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

public class WaterMeteringInstallTest extends AbstractDriverBaseTest {
    SideBarMenuViewHelper sideBarMenuView;
    SolutionTemplatesHomePageElements solutionTemplatesHomePage;
    SolutionTemplateDetailsPageHelper solutionTemplateDetailsPage;
    SolutionTemplatesInstalledViewHelper solutionTemplatesInstalledView;
    RuleChainsPageHelper ruleChainsPage;
    RolesPageElements rolesPage;
    CustomerPageHelper customerPage;
    ProfilesPageHelper profilesPage;
    DevicePageElements devicePage;
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
        dashboardPage = new DashboardPageHelper(driver);
        usersPageHelper = new UsersPageElements(driver);
    }

    @AfterMethod
    public void delete() {
        testRestClient.deleteWaterMetering();
    }

    @Test
    public void waterMeteringOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        Assert.assertEquals(getUrl(), Const.URL + "/solutionTemplates/water_metering");
        Assert.assertEquals(solutionTemplateDetailsPage.getHeadOfTitleCardName(), "Water metering");
        Assert.assertTrue(solutionTemplateDetailsPage.getTitleCardParagraphText().contains("Water Metering template"));
        Assert.assertTrue(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText().contains("Water Metering template"));
        Assert.assertTrue(solutionTemplateDetailsPage.waterMeteringScreenshotsAreCorrected());
    }

    @Test
    public void installWaterMeteringFromGeneralPage() {
        String dashboardGroupName = "Water Metering";
        String dashboardName = "Water Metering Tenant Dashboard";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphWaterMetering().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionSecondParagraphWaterMetering().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.customersFirstParagraphWaterMetering().getText().contains(dashboardName));
    }

    @Test
    public void installWaterMeteringFromDetailsPage() {
        String dashboardGroupName = "Water Metering";
        String dashboardName = "Water Metering Tenant Dashboard";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphWaterMetering().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionSecondParagraphWaterMetering().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.customersFirstParagraphWaterMetering().getText().contains(dashboardName));
    }

    @Test
    public void closeInstallWaterMeteringPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void closeInstallWaterMeteringPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void installEntities() {
        String ruleChainName = "Water Metering Solution Main";
        String ruleChain1Name = "Water Metering Solution Customer Alarm Routing";
        String ruleChain2Name = "Water Metering Solution Tenant Alarm Routing";
        String roleName = "Water Metering Read Only";
        String role1Name = "Water Metering User";
        String customerGroupName = "Water Metering";
        String deviceGroupName = "Water Meters";
        String deviceProfileName = "Water Meter";
        String deviceName = "WM0000123";
        String device1Name = "WM0000124";
        String device2Name = "WM0000125";
        String dashboardGroupName = "Water Metering";
        String dashboardGroup1Name = "Water Metering Shared";
        String dashboardName = "Water Metering User Dashboard";
        String dashboard1Name = "Water Metering Tenant Dashboard";

        testRestClient.postWaterMetering();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(ruleChain1Name).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(ruleChain2Name).isDisplayed());

        sideBarMenuView.rolesBtn().click();

        Assert.assertTrue(rolesPage.entity(roleName).isDisplayed());
        Assert.assertTrue(rolesPage.entity(role1Name).isDisplayed());

        sideBarMenuView.customerGroupsBtn().click();

        Assert.assertTrue(customerPage.entity(customerGroupName).isDisplayed());

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(deviceProfileName).isDisplayed());

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entity(deviceGroupName).isDisplayed());

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entity(deviceName).isDisplayed());
        Assert.assertTrue(devicePage.entity(device1Name).isDisplayed());
        Assert.assertTrue(devicePage.entity(device2Name).isDisplayed());

        sideBarMenuView.dashboardGroupsBtn().click();

        Assert.assertTrue(dashboardPage.entity(dashboardGroupName).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(dashboardGroup1Name).isDisplayed());

        dashboardPage.entity("All").click();

        Assert.assertTrue(dashboardPage.entity(dashboardName).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(dashboard1Name).isDisplayed());
    }

    @Test
    public void waterMeteringDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.waterMeteringInstallBtn();
        testRestClient.postWaterMetering();
        refreshPage();
        scrollToElement(solutionTemplatesHomePage.waterMetering());

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.waterMeteringDeleteBtn().isDisplayed());
    }

    @Test
    public void waterMeteringDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postWaterMetering();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Test
    public void waterMeteringOpenInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void waterMeteringCloseInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void waterMeteringCloseInstructionByCloseBtn() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void waterMeteringInstructionGoToMainDashboard() {
        String dashboardGroupName = "Water Metering";
        String dashboardName = "Water Metering Tenant Dashboard";

        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void waterMeteringDetailsPageOpenInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void waterMeteringDetailsPageCloseInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void waterMeteringDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void waterMeteringDetailsPageInstructionGoToMainDashboard() {
        String dashboardGroupName = "Water Metering";
        String dashboardName = "Water Metering Tenant Dashboard";

        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test()
    public void deleteEntities() {
        String ruleChainName = "Water Metering Solution Main";
        String ruleChain1Name = "Water Metering Solution Customer Alarm Routing";
        String ruleChain2Name = "Water Metering Solution Tenant Alarm Routing";
        String roleName = "Water Metering Read Only";
        String role1Name = "Water Metering User";
        String customerGroupName = "Water Metering";
        String deviceGroupName = "Water Meters";
        String deviceProfileName = "Water Meter";
        String deviceName = "WM0000123";
        String device1Name = "WM0000124";
        String device2Name = "WM0000125";
        String dashboardGroupName = "Water Metering";
        String dashboardGroup1Name = "Water Metering Shared";
        String dashboardName = "Water Metering User Dashboard";
        String dashboard1Name = "Water Metering Tenant Dashboard";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteWaterMetering();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChainName));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChain1Name));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChain2Name));

        sideBarMenuView.rolesBtn().click();

        Assert.assertTrue(rolesPage.entityIsNotPresent(roleName));
        Assert.assertTrue(rolesPage.entityIsNotPresent(role1Name));

        sideBarMenuView.customerGroupsBtn().click();

        Assert.assertTrue(customerPage.entityIsNotPresent(customerGroupName));

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfileName));

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceGroupName));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceName));
        Assert.assertTrue(devicePage.entityIsNotPresent(device1Name));
        Assert.assertTrue(devicePage.entityIsNotPresent(device2Name));

        sideBarMenuView.dashboardGroupsBtn().click();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardGroupName));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardGroup1Name));

        dashboardPage.entity("All").click();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardName));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboard1Name));
    }

    @Test(groups = {"broken"})
    public void linksBtn() {
        String dashboardGroupName = "Water Metering";
        String dashboardName = "Water Metering Tenant Dashboard";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
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
