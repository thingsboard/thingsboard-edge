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
package org.thingsboard.server.msa.ui.tests.solutionTemplates.smartRetail;

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

import static org.thingsboard.server.msa.ui.utils.Const.CONNECTIVITY_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.HTTP_API_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.THINGSBOARD_INTEGRATION_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.THINGSBOARD_IOT_GATEWAY_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.THINGSBOARD_MQTT_GATEWAY_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CHILLER3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CHILLER65644_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CHILLER_378876_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CHILLER_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_3456_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_4534_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FREEZER_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FREEZER_43545_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FREEZER_67478;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FREEZER_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.MOTION_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.MOTION_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OCCUPANCY_SENSOR_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OCCUPANCY_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.RETAIL_COMPANY_A_CUSTOMER;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.RETAIL_COMPANY_B_CUSTOMER;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_ADMINISTRATORS_USER_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_ADMINISTRATOR_ROLE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_CUSTOMER_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_READ_ONLY_ROLE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_USERS_USER_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_USER_ROLE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_457321_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_557322_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_765765_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_89546_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_5_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_6_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_DEVICES_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_DEVICES_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_S2_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_S1_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_S3_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_ASSET_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SUPERMARKET_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_USER_SHARED_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD;

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

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
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

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installSmartRetailFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsSmartSupermarketAdministrationFirstParagraphSmartRetail().getText()
                .contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsSmartSupermarketFirstParagraphSmartRetail().getText().contains(SMART_SUPERMARKET_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.customersSmartSupermarketAdministrationThirdParagraphSmartRetail().getText().contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installSmartRetailFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(getUrl(), Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsSmartSupermarketAdministrationFirstParagraphSmartRetail().getText()
                .contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.dashboardsSmartSupermarketFirstParagraphSmartRetail().getText().contains(SMART_SUPERMARKET_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.customersSmartSupermarketAdministrationThirdParagraphSmartRetail().getText().contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallSmartRetailPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallSmartRetailPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String user1RetailCompanyA = solutionTemplatesInstalledView.users(RETAIL_COMPANY_A_CUSTOMER).get(0).getText();
        String user2RetailCompanyA = solutionTemplatesInstalledView.users(RETAIL_COMPANY_A_CUSTOMER).get(1).getText();
        String user1RetailCompanyB = solutionTemplatesInstalledView.users(RETAIL_COMPANY_B_CUSTOMER).get(0).getText();
        String user2RetailCompanyB = solutionTemplatesInstalledView.users(RETAIL_COMPANY_B_CUSTOMER).get(1).getText();
        solutionTemplatesInstalledView.closeBtn().click();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(SUPERMARKET_DEVICES_RULE_CHAIN).isDisplayed());

        sideBarMenuView.rolesBtn().click();

        Assert.assertTrue(rolesPage.entity(SMART_RETAIL_READ_ONLY_ROLE).isDisplayed());
        Assert.assertTrue(rolesPage.entity(SMART_RETAIL_USER_ROLE).isDisplayed());
        Assert.assertTrue(rolesPage.entity(SMART_RETAIL_ADMINISTRATOR_ROLE).isDisplayed());

        sideBarMenuView.customerGroupsBtn().click();

        Assert.assertTrue(customerPage.entity(SMART_RETAIL_CUSTOMER_GROUP).isDisplayed());

        customerPage.entity("All").click();
        customerPage.manageCustomersUserBtn(RETAIL_COMPANY_A_CUSTOMER).click();

        Assert.assertTrue(usersPage.entity(SMART_RETAIL_USERS_USER_GROUP).isDisplayed());
        Assert.assertTrue(usersPage.entity(SMART_RETAIL_ADMINISTRATORS_USER_GROUP).isDisplayed());


        usersPage.entity("All").click();

        Assert.assertTrue(usersPage.entity(user1RetailCompanyA).isDisplayed());
        Assert.assertTrue(usersPage.entity(user2RetailCompanyA).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersUserBtn(RETAIL_COMPANY_B_CUSTOMER).click();

        Assert.assertTrue(usersPage.entity(SMART_RETAIL_USERS_USER_GROUP).isDisplayed());
        Assert.assertTrue(usersPage.entity(SMART_RETAIL_ADMINISTRATORS_USER_GROUP).isDisplayed());

        usersPage.entity("All").click();

        Assert.assertTrue(usersPage.entity(user1RetailCompanyB).isDisplayed());
        Assert.assertTrue(usersPage.entity(user2RetailCompanyB).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersDeviceGroupsBtn(RETAIL_COMPANY_A_CUSTOMER).click();

        Assert.assertTrue(devicePage.entity(SUPERMARKET_DEVICES_DEVICE_GROUP).isDisplayed());

        devicePage.entity("All").click();
        devicePage.changeItemsCountPerPage(30);

        Assert.assertTrue(devicePage.entity(CHILLER3_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(CHILLER65644_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(DOOR_SENSOR_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(DOOR_SENSOR_2_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(DOOR_SENSOR_3_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(DOOR_SENSOR_4534_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(FREEZER_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(FREEZER_43545_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(LIQUID_LEVEL_SENSOR_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(LIQUID_LEVEL_SENSOR_2_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(LIQUID_LEVEL_SENSOR_3_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(LIQUID_LEVEL_SENSOR_4_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(MOTION_SENSOR_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(OCCUPANCY_SENSOR_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMART_BIN_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMART_BIN_2_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMART_BIN_3_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMART_BIN_4_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMART_SHELF_457321_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMART_SHELF_557322_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMART_SHELF_765765_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMOKE_SENSOR_1_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMOKE_SENSOR_2_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMOKE_SENSOR_3_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMOKE_SENSOR_4_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMOKE_SENSOR_5_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(SMOKE_SENSOR_6_DEVICE).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersDeviceGroupsBtn(RETAIL_COMPANY_B_CUSTOMER).click();

        Assert.assertTrue(devicePage.entity(SUPERMARKET_DEVICES_DEVICE_GROUP).isDisplayed());

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entity(SMART_SHELF_89546_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(CHILLER_378876_DEVICE).isDisplayed());
        Assert.assertTrue(devicePage.entity(FREEZER_67478).isDisplayed());
        Assert.assertTrue(devicePage.entity(DOOR_SENSOR_3456_DEVICE).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersAssetGroupsBtn(RETAIL_COMPANY_A_CUSTOMER).click();

        Assert.assertTrue(assetPage.entity(SUPERMARKETS_ASSET_GROUP).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(SUPERMARKETS_S1_ASSET).isDisplayed());
        Assert.assertTrue(assetPage.entity(SUPERMARKETS_S2_ASSET).isDisplayed());

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.manageCustomersAssetGroupsBtn(RETAIL_COMPANY_B_CUSTOMER).click();

        Assert.assertTrue(assetPage.entity(SUPERMARKETS_ASSET_GROUP).isDisplayed());

        assetPage.entity("All").click();

        Assert.assertTrue(assetPage.entity(SUPERMARKETS_S3_ASSET).isDisplayed());

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(DOOR_SENSOR_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(SMOKE_SENSOR_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(SMART_SHELF_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(CHILLER_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(MOTION_SENSOR_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(FREEZER_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(SMART_BIN_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(OCCUPANCY_SENSOR_DEVICE_PROFILE).isDisplayed());
        Assert.assertTrue(profilesPage.entity(LIQUID_LEVEL_SENSOR_DEVICE_PROFILE).isDisplayed());

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entity(SUPERMARKET_ASSET_PROFILE).isDisplayed());

        sideBarMenuView.dashboardGroupsBtn().click();

        Assert.assertTrue(dashboardPage.entity(SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP).isDisplayed());

        dashboardPage.entity("All").click();

        Assert.assertTrue(dashboardPage.entity(SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD).isDisplayed());
        Assert.assertTrue(dashboardPage.entity(SMART_SUPERMARKET_DASHBOARD).isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void smartRetailDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartRetailInstallBtn();
        testRestClient.postSmartRetail();
        refreshPage();
        scrollToElement(solutionTemplatesHomePage.smartRetail());

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.smartRetailDeleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void smartRetailDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartRetail();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void smartRetailOpenInstruction() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void smartRetailCloseInstruction() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void smartRetailCloseInstructionByCloseBtn() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void smartRetailInstructionGoToMainDashboard() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void smartRetailDetailsPageOpenInstruction() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void smartRetailDetailsPageCloseInstruction() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void smartRetailDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void smartRetailDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postSmartRetail();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartRetail();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(SUPERMARKET_DEVICES_RULE_CHAIN));

        sideBarMenuView.rolesBtn().click();

        Assert.assertTrue(rolesPage.entityIsNotPresent(SMART_RETAIL_READ_ONLY_ROLE));
        Assert.assertTrue(rolesPage.entityIsNotPresent(SMART_RETAIL_USER_ROLE));
        Assert.assertTrue(rolesPage.entityIsNotPresent(SMART_RETAIL_ADMINISTRATOR_ROLE));

        sideBarMenuView.customerGroupsBtn().click();

        Assert.assertTrue(customerPage.entityIsNotPresent(SMART_RETAIL_CUSTOMER_GROUP));

        customerPage.entity("All").click();

        Assert.assertTrue(customerPage.entityIsNotPresent(RETAIL_COMPANY_A_CUSTOMER));
        Assert.assertTrue(customerPage.entityIsNotPresent(RETAIL_COMPANY_B_CUSTOMER));

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(DOOR_SENSOR_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(SMOKE_SENSOR_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(SMART_SHELF_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(CHILLER_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(MOTION_SENSOR_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(FREEZER_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(SMART_BIN_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(OCCUPANCY_SENSOR_DEVICE_PROFILE));
        Assert.assertTrue(profilesPage.entityIsNotPresent(LIQUID_LEVEL_SENSOR_DEVICE_PROFILE));

        sideBarMenuView.openAssetProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(SUPERMARKET_ASSET_PROFILE));

        sideBarMenuView.dashboardGroupsBtn().click();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(SUPERMARKET_USER_SHARED_DASHBOARD_GROUP));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP));

        dashboardPage.entity("All").click();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD));
        Assert.assertTrue(dashboardPage.entityIsNotPresent(SMART_SUPERMARKET_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Smart Retail")
    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkThingsBoardIoTGateway = solutionTemplatesInstalledView.getThingsBoardIoTGatewayLink();
        String linkThingsBoardMQTTGateway = solutionTemplatesInstalledView.getThingsBoardMQTTGatewayLink();
        String linkThingsBoardIntegration = solutionTemplatesInstalledView.getThingsBoardIntegration();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getId().toString();
        String dashboard1Id = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getId().toString();
        String entityGroup1Id = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(2, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId));
        Assert.assertTrue(urls.contains(Const.URL + "/dashboards/groups/" + entityGroup1Id + "/" + dashboard1Id));
        Assert.assertEquals(HTTP_API_DOCS_URL, linkHttpApi);
        Assert.assertEquals(CONNECTIVITY_DOCS_URL, linkConnectionDevices);
        Assert.assertEquals(THINGSBOARD_IOT_GATEWAY_DOCS_URL, linkThingsBoardIoTGateway);
        Assert.assertEquals(THINGSBOARD_MQTT_GATEWAY_DOCS_URL, linkThingsBoardMQTTGateway);
        Assert.assertEquals(THINGSBOARD_INTEGRATION_DOCS_URL, linkThingsBoardIntegration);
    }
}
