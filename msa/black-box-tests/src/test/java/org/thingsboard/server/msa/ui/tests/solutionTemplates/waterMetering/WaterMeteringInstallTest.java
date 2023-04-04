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
package org.thingsboard.server.msa.ui.tests.solutionTemplates.waterMetering;

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
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_CUSTOMER_A_CUSTOMER;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_CUSTOMER_B_CUSTOMER;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_CUSTOMER_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_READ_ONLY_ROLES;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_SHARED_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_SOLUTION_ALARM_ROUTING_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_SOLUTION_MAIN_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_SOLUTION_TENANT_ALARM_ROUTING_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_TENANT_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_USER_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_USER_ROLES;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERS_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METER_DEVICE_PROFILE_WM;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WM0000123_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WM0000124_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WM0000125_DEVICE;

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

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
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

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installWaterMeteringFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphWaterMetering().getText().contains(WATER_METERING_TENANT_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionSecondParagraphWaterMetering().getText().contains(WATER_METERING_TENANT_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.customersFirstParagraphWaterMetering().getText().contains(WATER_METERING_TENANT_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installWaterMeteringFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphWaterMetering().getText().contains(WATER_METERING_TENANT_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionSecondParagraphWaterMetering().getText().contains(WATER_METERING_TENANT_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.customersFirstParagraphWaterMetering().getText().contains(WATER_METERING_TENANT_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallWaterMeteringPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallWaterMeteringPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.setFirstUserName();
        solutionTemplatesInstalledView.setSecondUserName();
        String userCustomerA = solutionTemplatesInstalledView.getFirstUserName();
        String userCustomerB = solutionTemplatesInstalledView.getSecondUserName();
        solutionTemplatesInstalledView.closeBtn().click();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(WATER_METERING_SOLUTION_MAIN_RULE_CHAIN).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(WATER_METERING_SOLUTION_ALARM_ROUTING_RULE_CHAIN).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entity(WATER_METERING_SOLUTION_TENANT_ALARM_ROUTING_RULE_CHAIN).isDisplayed());

        sideBarMenuView.goToRoles();

        Assert.assertTrue(rolesPage.entity(WATER_METERING_READ_ONLY_ROLES).isDisplayed());
        Assert.assertTrue(rolesPage.entity(WATER_METERING_USER_ROLES).isDisplayed());

        sideBarMenuView.goToCustomerGroups();

        Assert.assertTrue(customerPage.entity(WATER_METERING_CUSTOMER_GROUP).isDisplayed());

        customerPage.entity("All").click();

        Assert.assertTrue(customerPage.entity(WATER_METERING_CUSTOMER_A_CUSTOMER).isDisplayed());
        Assert.assertTrue(customerPage.entity(WATER_METERING_CUSTOMER_B_CUSTOMER).isDisplayed());

        customerPage.manageCustomersUserBtn(WATER_METERING_CUSTOMER_A_CUSTOMER).click();

        Assert.assertTrue(usersPageHelper.entity(userCustomerA).isDisplayed());

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersUserBtn(WATER_METERING_CUSTOMER_B_CUSTOMER).click();

        Assert.assertTrue(usersPageHelper.entity(userCustomerB).isDisplayed());

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(WATER_METERING_CUSTOMER_A_CUSTOMER).click();
        devicePage.groupsBtn().click();

        Assert.assertTrue(devicePage.entity(WATER_METERS_DEVICE_GROUP).isDisplayed());

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(WATER_METERING_CUSTOMER_B_CUSTOMER).click();
        devicePage.groupsBtn().click();

        Assert.assertTrue(devicePage.entity(WATER_METERS_DEVICE_GROUP).isDisplayed());

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(WATER_METER_DEVICE_PROFILE_WM).isDisplayed());

        sideBarMenuView.goToAllDevices();

        Assert.assertTrue(devicePage.entity(WM0000123_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(WM0000124_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(WM0000125_DEVICE).isDisplayed());

        sideBarMenuView.goToDashboardGroups();

        Assert.assertTrue(dashboardPage.entity(WATER_METERING_DASHBOARD_GROUP).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(WATER_METERING_SHARED_DASHBOARD_GROUP).isDisplayed());

        dashboardPage.entity("All").click();

        Assert.assertTrue(dashboardPage.entity(WATER_METERING_USER_DASHBOARD).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(WATER_METERING_TENANT_DASHBOARD).isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void waterMeteringDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.waterMeteringInstallBtn();
        testRestClient.postWaterMetering();
        refreshPage();
        scrollToElement(solutionTemplatesHomePage.waterMetering());

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.waterMeteringDeleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void waterMeteringDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postWaterMetering();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void waterMeteringOpenInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void waterMeteringCloseInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void waterMeteringCloseInstructionByCloseBtn() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void waterMeteringInstructionGoToMainDashboard() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void waterMeteringDetailsPageOpenInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void waterMeteringDetailsPageCloseInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void waterMeteringDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void waterMeteringDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteWaterMetering();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(WATER_METERING_SOLUTION_MAIN_RULE_CHAIN));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(WATER_METERING_SOLUTION_ALARM_ROUTING_RULE_CHAIN));
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(WATER_METERING_SOLUTION_TENANT_ALARM_ROUTING_RULE_CHAIN));

        sideBarMenuView.goToRoles();

        Assert.assertTrue(rolesPage.entityIsNotPresent(WATER_METERING_READ_ONLY_ROLES));
        Assert.assertTrue(rolesPage.entityIsNotPresent(WATER_METERING_USER_ROLES));

        sideBarMenuView.goToCustomerGroups();

        Assert.assertTrue(customerPage.entityIsNotPresent(WATER_METERING_CUSTOMER_GROUP));

        customerPage.entity("All").click();

        Assert.assertTrue(customerPage.entityIsNotPresent(WATER_METERING_CUSTOMER_A_CUSTOMER));
        Assert.assertTrue(customerPage.entityIsNotPresent(WATER_METERING_CUSTOMER_B_CUSTOMER));

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(WATER_METER_DEVICE_PROFILE_WM));

        sideBarMenuView.goToDeviceGroups();

        Assert.assertTrue(devicePage.entityIsNotPresent(WATER_METERS_DEVICE_GROUP));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(WM0000123_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(WM0000124_DEVICE));
        Assert.assertTrue(devicePage.entityIsNotPresent(WM0000125_DEVICE));

        sideBarMenuView.goToDashboardGroups();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(WATER_METERING_DASHBOARD_GROUP));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(WATER_METERING_SHARED_DASHBOARD_GROUP));

        dashboardPage.entity("All").click();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(WATER_METERING_USER_DASHBOARD));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(WATER_METERING_TENANT_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Water metering")
    @Test(groups = {"broken"})
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals(DASHBOARD_GIDE_DOCS_URL, guide);
        Assert.assertEquals(HTTP_API_DOCS_URL, linkHttpApi);
        Assert.assertEquals(CONNECTIVITY_DOCS_URL, linkConnectionDevices);
        Assert.assertEquals(ALARM_RULES_DOCS_URL, linkAlarmRule);
        Assert.assertEquals(getBaseUiUrl() + "/profiles/deviceProfiles", linkDeviceProfile);
    }
}
