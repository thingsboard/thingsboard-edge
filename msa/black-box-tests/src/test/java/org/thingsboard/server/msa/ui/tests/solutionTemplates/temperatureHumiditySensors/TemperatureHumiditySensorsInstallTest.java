package org.thingsboard.server.msa.ui.tests.solutionTemplates.temperatureHumiditySensors;

import org.testng.Assert;
import org.openqa.selenium.WebElement;
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

public class TemperatureHumiditySensorsInstallTest extends AbstractDriverBaseTest {
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
        testRestClient.deleteTemperatureHumidity();
    }

    @Test
    public void temperatureHumiditySensorsOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        Assert.assertEquals(Const.URL + "/solutionTemplates/temperature_sensors", getUrl());
        Assert.assertEquals(solutionTemplateDetailsPage.getHeadOfTitleCardName(), "Temperature & Humidity Sensors");
        Assert.assertTrue(solutionTemplateDetailsPage.getTitleCardParagraphText().contains("Temperature & Humidity sensors"));
        Assert.assertTrue(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText().contains("Temperature & Humidity template"));
        Assert.assertTrue(solutionTemplateDetailsPage.temperatureHumiditySensorsScreenshotsAreCorrected());
    }

    @Test
    public void installTemperatureHumiditySensorsFromGeneralPage() {
        String dashboardGroupName = "Customer dashboards";
        String dashboardName = "Temperature & Humidity";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphTemperatureHumiditySensor().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphTemperatureHumiditySensor().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.alarmFirstParagraphTemperatureHumiditySensor().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.customersFirstParagraphTemperatureHumiditySensor().getText().contains(dashboardName));
    }

    @Test
    public void installTemperatureHumiditySensorsFromDetailsPage() {
        String dashboardGroupName = "Customer dashboards";
        String dashboardName = "Temperature & Humidity";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphTemperatureHumiditySensor().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphTemperatureHumiditySensor().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.alarmFirstParagraphTemperatureHumiditySensor().getText().contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.customersFirstParagraphTemperatureHumiditySensor().getText().contains(dashboardName));
    }

    @Test
    public void closeInstallTemperatureHumiditySensorsPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void closeInstallTemperatureHumiditySensorsPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void installEntities() {
        String ruleChainName = "Temperature & Humidity Sensors";
        String roleName = "Read Only";
        String customerName = "Customer D";
        String deviceGroupName = "Temperature & Humidity sensors";
        String deviceInCustomerName = "Sensor C1";
        String profileName = "Temperature Sensor";
        String deviceName = "Sensor T1";
        String dashboardName = "Temperature & Humidity";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.setFirstUserName();
        solutionTemplatesInstalledView.setSecondUserName();
        String user1 = solutionTemplatesInstalledView.getFirstUserName();
        String user2 = solutionTemplatesInstalledView.getSecondUserName();
        solutionTemplatesInstalledView.closeBtn().click();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());

        sideBarMenuView.rolesBtn().click();

        Assert.assertTrue(rolesPage.entity(roleName).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();

        Assert.assertTrue(customerPage.entity(customerName).isDisplayed());

        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();
        customerPage.entity(deviceGroupName).click();

        Assert.assertTrue(devicePage.entity(deviceInCustomerName).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersUserBtn(customerName).click();
        customerPage.entity("All").click();

        Assert.assertTrue(usersPageHelper.entity(user1).isDisplayed());
        Assert.assertTrue(usersPageHelper.entity(user2).isDisplayed());

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(profileName).isDisplayed());

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entity(deviceGroupName).isDisplayed());

        devicePage.entity(deviceGroupName).click();

        Assert.assertTrue(devicePage.entity(deviceName).isDisplayed());

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entity(dashboardName).isDisplayed());
    }

    @Test
    public void temperatureHumiditySensorsDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn();
        testRestClient.postTemperatureHumidity();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.temperatureHumiditySensorsDeleteBtn().isDisplayed());
    }

    @Test
    public void temperatureHumiditySensorsDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postTemperatureHumidity();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Test
    public void temperatureHumiditySensorsOpenInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void temperatureHumiditySensorsCloseInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void temperatureHumiditySensorsCloseInstructionByCloseBtn() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void temperatureHumiditySensorsInstructionGoToMainDashboard() {
        String dashboardGroupName = "Customer dashboards";
        String dashboardName = "Temperature & Humidity";

        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void temperatureHumiditySensorsDetailsPageOpenInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void temperatureHumiditySensorsDetailsPageCloseInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void temperatureHumiditySensorsDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void temperatureHumiditySensorsDetailsPageInstructionGoToMainDashboard() {
        String dashboardGroupName = "Customer dashboards";
        String dashboardName = "Temperature & Humidity";

        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test(groups = "broken")
    public void deleteEntities() {
        String ruleChainName = "Temperature & Humidity Sensors";
        String roleName = "Read Only";
        String customerName = "Customer D";
        String deviceGroupName = "Temperature & Humidity sensors";
        String deviceName = "Sensor T1";
        String profileName = "Temperature Sensor";
        String dashboardName = "Temperature & Humidity";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteTemperatureHumidity();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.elementIsNotPresent(ruleChainName));

        sideBarMenuView.rolesBtn().click();

        Assert.assertTrue(rolesPage.entityIsNotPresent(roleName));

        sideBarMenuView.goToAllCustomerGroupBtn();

        Assert.assertTrue(customerPage.entityIsNotPresent(customerName));

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(profileName));

        sideBarMenuView.deviceGroups().click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceGroupName));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(deviceName));

        dashboardPage.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardName));
    }

    @Test(groups = {"broken"})
    public void linksBtn() {
        String dashboardGroupName = "Customer dashboards";
        String dashboardName = "Temperature & Humidity";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
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
