package org.thingsboard.server.msa.ui.tests.solutionTemplates.smartRetail;

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

public class SmartRetailInstallTest extends AbstractDriverBaseTest {
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
    UsersPageElements usersPage;

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
        usersPage = new UsersPageElements(driver);
    }

    @AfterMethod
    public void delete() {
        testRestClient.deleteSmartRetail();
    }

    @Test
    public void smartRetailOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        Assert.assertEquals(getUrl(), Const.URL + "/solutionTemplates/smart_retail");
        Assert.assertEquals(solutionTemplateDetailsPage.getHeadOfTitleCardName(), "Smart Retail");
        Assert.assertTrue(solutionTemplateDetailsPage.getTitleCardParagraphText().contains("Smart Retail template"));
        Assert.assertTrue(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText().contains("Smart Retail template"));
        Assert.assertTrue(solutionTemplateDetailsPage.smartRetailScreenshotsAreCorrected());
    }

    @Test
    public void installSmartRetailFromGeneralPage() {
        String dashboardGroupName = "Supermarket Users Shared";
        String dashboardName = "Smart Supermarket Administration";
        String dashboardName2 = "Smart Supermarket";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName2).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsSmartSupermarketAdministrationFirstParagraphSmartRetail().getText()
                .contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsSmartSupermarketFirstParagraphSmartRetail().getText().contains(dashboardName2));
        Assert.assertTrue(solutionTemplatesInstalledView.customersSmartSupermarketAdministrationThirdParagraphSmartRetail().getText().contains(dashboardName));
    }

    @Test
    public void installSmartRetailFromDetailsPage() {
        String dashboardGroupName = "Supermarket Users Shared";
        String dashboardName = "Smart Supermarket Administration";
        String dashboardName2 = "Smart Supermarket";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName2).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsSmartSupermarketAdministrationFirstParagraphSmartRetail().getText()
                .contains(dashboardName));
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsSmartSupermarketFirstParagraphSmartRetail().getText().contains(dashboardName2));
        Assert.assertTrue(solutionTemplatesInstalledView.customersSmartSupermarketAdministrationThirdParagraphSmartRetail().getText().contains(dashboardName));
    }

    @Test
    public void closeInstallSmartRetailPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void closeInstallSmartRetailPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void installEntities() {
        String ruleChainName = "Supermarket Devices";
        String rolesName = "Smart Retail Read Only";
        String roles1Name = "Smart Retail User";
        String roles2Name = "Smart Retail Administrator";
        String customerGroupName = "Smart Retail";
        String customerName1 = "Retail Company A";
        String customerName2 = "Retail Company B";
        String userGroupName = "Smart Retail Users";
        String userGroup1Name = "Smart Retail Administrators";
        String deviceGroupName = "Supermarket Devices";
        String deviceInCustomer1 = "Chiller 3";
        String device1InCustomer1 = "Chiller 65644";
        String device2InCustomer1 = "Door Sensor 1";
        String device3InCustomer1 = "Door Sensor 2";
        String device4InCustomer1 = "Door Sensor 3";
        String device5InCustomer1 = "Door Sensor 4534";
        String device6InCustomer1 = "Freezer 1";
        String device7InCustomer1 = "Freezer 43545";
        String device8InCustomer1 = "Liquid Level Sensor 1";
        String device9InCustomer1 = "Liquid Level Sensor 2";
        String device10InCustomer1 = "Liquid Level Sensor 3";
        String device11InCustomer1 = "Liquid Level Sensor 4";
        String device12InCustomer1 = "Motion Sensor 1";
        String device13InCustomer1 = "Occupancy Sensor";
        String device14InCustomer1 = "Smart Bin 1";
        String device15InCustomer1 = "Smart Bin 2";
        String device16InCustomer1 = "Smart Bin 3";
        String device17InCustomer1 = "Smart Bin 4";
        String device18InCustomer1 = "Smart Shelf 457321";
        String device19InCustomer1 = "Smart Shelf 557322";
        String device20InCustomer1 = "Smart Shelf 765765";
        String device21InCustomer1 = "Smoke Sensor 1";
        String device22InCustomer1 = "Smoke Sensor 2";
        String device23InCustomer1 = "Smoke Sensor 3";
        String device24InCustomer1 = "Smoke Sensor 4";
        String device25InCustomer1 = "Smoke Sensor 5";
        String device26InCustomer1 = "Smoke Sensor 6";
        String deviceInCustomer2 = "Smart Shelf 89546";
        String device1InCustomer2 = "Chiller 378876";
        String device2InCustomer2 = "Freezer 67478";
        String device3InCustomer2 = "Door Sensor 3456";
        String deviceProfileName = "Door Sensor";
        String deviceProfile1Name = "Smoke Sensor";
        String deviceProfile2Name = "Smart Shelf";
        String deviceProfile3Name = "Chiller";
        String deviceProfile4Name = "Motion Sensor";
        String deviceProfile5Name = "Freezer";
        String deviceProfile6Name = "Smart Bin";
        String deviceProfile7Name = "Occupancy Sensor";
        String deviceProfile8Name = "Liquid Level Sensor";
        String assetGroupNameInCustomer1 = "Supermarkets";
        String assetGroupNameInCustomer2 = "Supermarkets";
        String assetNameInCustomer1 = "Supermarket S1";
        String assetName1InCustomer1 = "Supermarket S2";
        String assetNameInCustomer2 = "Supermarket S3";
        String assetProfileName = "supermarket";
        String dashboardGroupName = "Supermarket Users Shared";
        String dashboardGroup1Name = "Supermarket Administrators Shared";
        String dashboardName = "Smart Supermarket User Management";
        String dashboard1Name = "Smart Supermarket Administration";
        String dashboard2Name = "Smart Supermarket";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String user1RetailCompanyA = solutionTemplatesInstalledView.users(customerName1).get(0).getText();
        String user2RetailCompanyA = solutionTemplatesInstalledView.users(customerName1).get(1).getText();
        String user1RetailCompanyB = solutionTemplatesInstalledView.users(customerName2).get(0).getText();
        String user2RetailCompanyB = solutionTemplatesInstalledView.users(customerName2).get(1).getText();
        solutionTemplatesInstalledView.closeBtn().click();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());

        sideBarMenuView.rolesBtn().click();

        Assert.assertTrue(rolesPage.entity(rolesName).isDisplayed());
        Assert.assertTrue(rolesPage.entity(roles1Name).isDisplayed());
        Assert.assertTrue(rolesPage.entity(roles2Name).isDisplayed());

        sideBarMenuView.customerGroupsBtn().click();

        Assert.assertTrue(customerPage.entity(customerGroupName).isDisplayed());

        customerPage.entity("All").click();
        customerPage.manageCustomersUserBtn(customerName1).click();

        Assert.assertTrue(usersPage.entity(userGroupName).isDisplayed());
        Assert.assertTrue(usersPage.entity(userGroup1Name).isDisplayed());


        usersPage.entity("All").click();

        Assert.assertTrue(usersPage.entity(user1RetailCompanyA).isDisplayed());
        Assert.assertTrue(usersPage.entity(user2RetailCompanyA).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersUserBtn(customerName2).click();

        Assert.assertTrue(usersPage.entity(userGroupName).isDisplayed());
        Assert.assertTrue(usersPage.entity(userGroup1Name).isDisplayed());

        usersPage.entity("All").click();

        Assert.assertTrue(usersPage.entity(user1RetailCompanyB).isDisplayed());
        Assert.assertTrue(usersPage.entity(user2RetailCompanyB).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersDeviceGroupsBtn(customerName1).click();

        Assert.assertTrue(devicePage.entity(deviceGroupName).isDisplayed());

        devicePage.entity("All").click();
        devicePage.changeItemsCountPerPage(30);

        Assert.assertTrue(devicePage.entity(deviceInCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device1InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device2InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device3InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device4InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device5InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device6InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device7InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device8InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device9InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device10InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device11InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device12InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device13InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device14InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device15InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device16InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device17InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device18InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device19InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device20InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device21InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device22InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device23InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device24InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device25InCustomer1).isDisplayed());
        Assert.assertTrue(devicePage.entity(device26InCustomer1).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersDeviceGroupsBtn(customerName2).click();

        Assert.assertTrue(devicePage.entity(deviceGroupName).isDisplayed());

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entity(deviceInCustomer2).isDisplayed());
        Assert.assertTrue(devicePage.entity(device1InCustomer2).isDisplayed());
        Assert.assertTrue(devicePage.entity(device2InCustomer2).isDisplayed());
        Assert.assertTrue(devicePage.entity(device3InCustomer2).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersAssetGroupsBtn(customerName1).click();

        Assert.assertTrue(assetPage.entity(assetGroupNameInCustomer1).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(assetNameInCustomer1).isDisplayed());
        Assert.assertTrue(assetPage.entity(assetName1InCustomer1).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersAssetGroupsBtn(customerName2).click();

        Assert.assertTrue(assetPage.entity(assetGroupNameInCustomer2).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(assetNameInCustomer2).isDisplayed());

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(deviceProfileName).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile1Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile2Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile3Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile4Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile5Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile6Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile7Name).isDisplayed());
        Assert.assertTrue(profilesPage.entity(deviceProfile8Name).isDisplayed());

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entity(assetProfileName).isDisplayed());

        sideBarMenuView.dashboardGroupsBtn().click();

        Assert.assertTrue(dashboardPage.entity(dashboardGroupName).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(dashboardGroup1Name).isDisplayed());

        dashboardPage.entity("All").click();

        Assert.assertTrue(dashboardPage.entity(dashboardName).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(dashboard1Name).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(dashboard2Name).isDisplayed());
    }

    @Test
    public void smartRetailDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartRetailInstallBtn();
        testRestClient.postSmartRetail();
        refreshPage();
        scrollToElement(solutionTemplatesHomePage.smartRetail());

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.smartRetailDeleteBtn().isDisplayed());
    }

    @Test
    public void smartRetailDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartRetail();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Test
    public void smartRetailOpenInstruction() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void smartRetailCloseInstruction() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartRetailCloseInstructionByCloseBtn() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartRetailInstructionGoToMainDashboard() {
        String dashboardGroupName = "Supermarket Users Shared";
        String dashboardName = "Smart Supermarket";
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void smartRetailDetailsPageOpenInstruction() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Test
    public void smartRetailDetailsPageCloseInstruction() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartRetailDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Test
    public void smartRetailDetailsPageInstructionGoToMainDashboard() {
        String dashboardGroupName = "Supermarket Users Shared";
        String dashboardName = "Smart Supermarket";
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Test
    public void deleteEntities() {
        String ruleChainName = "Supermarket Devices";
        String rolesName = "Smart Retail Read Only";
        String roles1Name = "Smart Retail User";
        String roles2Name = "Smart Retail Administrator";
        String customerGroupName = "Smart Retail";
        String customerName1 = "Retail Company A";
        String customerName2 = "Retail Company B";
        String deviceProfileName = "Door Sensor";
        String deviceProfile1Name = "Smoke Sensor";
        String deviceProfile2Name = "Smart Shelf";
        String deviceProfile3Name = "Chiller";
        String deviceProfile4Name = "Motion Sensor";
        String deviceProfile5Name = "Freezer";
        String deviceProfile6Name = "Smart Bin";
        String deviceProfile7Name = "Occupancy Sensor";
        String deviceProfile8Name = "Liquid Level Sensor";
        String assetProfileName = "supermarket";
        String dashboardGroupName = "Supermarket Users Shared";
        String dashboardGroup1Name = "Supermarket Administrators Shared";
        String dashboardName = "Smart Supermarket User Management";
        String dashboard1Name = "Smart Supermarket Administration";
        String dashboard2Name = "Smart Supermarket";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartRetail();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(ruleChainName));

        sideBarMenuView.rolesBtn().click();

        Assert.assertTrue(rolesPage.entityIsNotPresent(rolesName));
        Assert.assertTrue(rolesPage.entityIsNotPresent(roles1Name));
        Assert.assertTrue(rolesPage.entityIsNotPresent(roles2Name));

        sideBarMenuView.customerGroupsBtn().click();

        Assert.assertTrue(customerPage.entityIsNotPresent(customerGroupName));

        customerPage.entity("All").click();

        Assert.assertTrue(customerPage.entityIsNotPresent(customerName1));
        Assert.assertTrue(customerPage.entityIsNotPresent(customerName2));

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfileName));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile1Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile2Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile3Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile4Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile5Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile6Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile7Name));
        Assert.assertTrue(profilesPage.entityIsNotPresent(deviceProfile8Name));

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(assetProfileName));

        sideBarMenuView.dashboardGroupsBtn().click();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardGroupName));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardGroup1Name));

        dashboardPage.entity("All").click();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboardName));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboard1Name));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(dashboard2Name));
    }

    @Test
    public void linksBtn() {
        String dashboardGroupName = "Supermarket Users Shared";
        String dashboardGroup1Name = "Supermarket Administrators Shared";
        String dashboardName = "Smart Supermarket";
        String dashboard1Name = "Smart Supermarket Administration";

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkThingsBoardIoTGateway = solutionTemplatesInstalledView.getThingsBoardIoTGatewayLink();
        String linkThingsBoardMQTTGateway = solutionTemplatesInstalledView.getThingsBoardMQTTGatewayLink();
        String linkThingsBoardIntegration = solutionTemplatesInstalledView.getThingsBoardIntegration();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, dashboardGroupName, dashboardName).getId().toString();
        String dashboard1Id = getDashboardByName(EntityType.DASHBOARD, dashboardGroup1Name, dashboard1Name).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroupName).getId().toString();
        String entityGroup1Id = getEntityGroupByName(EntityType.DASHBOARD, dashboardGroup1Name).getId().toString();

        Assert.assertEquals(2, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboardGroups/" + entityGroupId + "/" + dashboardId));
        Assert.assertTrue(urls.contains(Const.URL + "/dashboardGroups/" + entityGroup1Id + "/" + dashboard1Id));
        Assert.assertEquals("https://thingsboard.io/docs/reference/http-api/#telemetry-upload-api", linkHttpApi);
        Assert.assertEquals("https://thingsboard.io/docs/getting-started-guides/connectivity/", linkConnectionDevices);
        Assert.assertEquals("https://thingsboard.io/docs/iot-gateway/what-is-iot-gateway/", linkThingsBoardIoTGateway);
        Assert.assertEquals("https://thingsboard.io/docs/paas/reference/gateway-mqtt-api/", linkThingsBoardMQTTGateway);
        Assert.assertEquals("https://thingsboard.io/docs/user-guide/integrations/", linkThingsBoardIntegration);
    }
}
