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
package org.thingsboard.server.msa.ui.tests.solutiontemplates.smartretail;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.tests.solutiontemplates.AbstractSolutionTemplateTest;
import org.thingsboard.server.msa.ui.utils.Const;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SUPERMARKET_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_5_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_6_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_S1_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_S2_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_S3_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_ASSET_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_DEVICES_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_DEVICES_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_USER_SHARED_DASHBOARD_GROUP;

@Feature("Installation")
@Story("Smart Retail")
public class SmartRetailInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteSmartRetail();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void smartRetailOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).isEqualTo(Const.URL + "/solutionTemplates/smart_retail");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).isEqualTo("Smart Retail");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("Smart Retail template");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description").contains("Smart Retail template");
        solutionTemplateDetailsPage.smartRetailScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installSmartRetailFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
        assertThat(solutionTemplatesInstalledView.dashboardsSmartSupermarketAdministrationFirstParagraphSmartRetail().getText())
                .as("First paragraph of SSA dashboard section solution instruction").contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.dashboardsSmartSupermarketFirstParagraphSmartRetail().getText())
                .as("First paragraph of SS dashboard section solution instruction").contains(SMART_SUPERMARKET_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersSmartSupermarketAdministrationThirdParagraphSmartRetail().getText())
                .as("Third paragraph of customer section in solution instruction").contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installSmartRetailFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
        assertThat(solutionTemplatesInstalledView.dashboardsSmartSupermarketAdministrationFirstParagraphSmartRetail().getText())
                .as("First paragraph of SSA dashboard section solution instruction").contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.dashboardsSmartSupermarketFirstParagraphSmartRetail().getText())
                .as("First paragraph of SS dashboard section solution instruction").contains(SMART_SUPERMARKET_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersSmartSupermarketAdministrationThirdParagraphSmartRetail().getText())
                .as("Third paragraph of customer section in solution instruction").contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallSmartRetailPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallSmartRetailPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

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
        assertThat(ruleChainsPage.entity(SUPERMARKET_DEVICES_RULE_CHAIN).isDisplayed())
                .as(SUPERMARKET_DEVICES_RULE_CHAIN + " is displayed").isTrue();

        sideBarMenuView.goToRoles();
        List.of(SMART_RETAIL_READ_ONLY_ROLE, SMART_RETAIL_USER_ROLE, SMART_RETAIL_ADMINISTRATOR_ROLE)
                .forEach(r -> assertThat(rolesPage.entity(r).isDisplayed()).as(r + " is displayed").isTrue());

        sideBarMenuView.goToCustomerGroups();
        assertThat(customerPage.entity(SMART_RETAIL_CUSTOMER_GROUP).isDisplayed())
                .as(SMART_RETAIL_CUSTOMER_GROUP + " is displayed").isTrue();

        customerPage.entity("All").click();
        customerPage.manageCustomersUserBtn(RETAIL_COMPANY_A_CUSTOMER).click();
        List.of(user1RetailCompanyA, user2RetailCompanyA).forEach(u -> assertThat(usersPage.entity(u).isDisplayed())
                .as(u + " is displayed").isTrue());

        usersPage.groupsBtn().click();
        List.of(SMART_RETAIL_USERS_USER_GROUP, SMART_RETAIL_ADMINISTRATORS_USER_GROUP)
                .forEach(ug -> assertThat(usersPage.entity(ug).isDisplayed()).as(ug + " is displayed").isTrue());

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersUserBtn(RETAIL_COMPANY_B_CUSTOMER).click();
        List.of(user1RetailCompanyB, user2RetailCompanyB).forEach(u -> assertThat(usersPage.entity(u).isDisplayed())
                .as(u + " is displayed").isTrue());

        usersPage.groupsBtn().click();
        List.of(SMART_RETAIL_USERS_USER_GROUP, SMART_RETAIL_ADMINISTRATORS_USER_GROUP)
                .forEach(ug -> assertThat(usersPage.entity(ug).isDisplayed()).as(ug + " is displayed").isTrue());

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(RETAIL_COMPANY_A_CUSTOMER).click();
        devicePage.changeItemsCountPerPage(30);
        List.of(CHILLER3_DEVICE, CHILLER65644_DEVICE, DOOR_SENSOR_1_DEVICE, DOOR_SENSOR_2_DEVICE, DOOR_SENSOR_3_DEVICE,
                        DOOR_SENSOR_4534_DEVICE, FREEZER_1_DEVICE, FREEZER_43545_DEVICE, LIQUID_LEVEL_SENSOR_1_DEVICE,
                        LIQUID_LEVEL_SENSOR_2_DEVICE, LIQUID_LEVEL_SENSOR_3_DEVICE, LIQUID_LEVEL_SENSOR_4_DEVICE, MOTION_SENSOR_1_DEVICE,
                        OCCUPANCY_SENSOR_DEVICE, SMART_BIN_1_DEVICE, SMART_BIN_2_DEVICE, SMART_BIN_3_DEVICE, SMART_BIN_4_DEVICE,
                        SMART_SHELF_457321_DEVICE, SMART_SHELF_557322_DEVICE, SMART_SHELF_765765_DEVICE, SMOKE_SENSOR_1_DEVICE,
                        SMOKE_SENSOR_2_DEVICE, SMOKE_SENSOR_3_DEVICE, SMOKE_SENSOR_4_DEVICE, SMOKE_SENSOR_5_DEVICE, SMOKE_SENSOR_6_DEVICE)
                .forEach(d -> assertThat(devicePage.entity(d).isDisplayed()).as(d + " is displayed").isTrue());

        devicePage.groupsBtn().click();
        assertThat(devicePage.entity(SUPERMARKET_DEVICES_DEVICE_GROUP).isDisplayed())
                .as(SUPERMARKET_DEVICES_DEVICE_GROUP + " is displayed").isTrue();

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(RETAIL_COMPANY_B_CUSTOMER).click();
        List.of(SMART_SHELF_89546_DEVICE, CHILLER_378876_DEVICE, FREEZER_67478, DOOR_SENSOR_3456_DEVICE)
                .forEach(d -> assertThat(devicePage.entity(d).isDisplayed()).as(d + " is displayed").isTrue());

        devicePage.groupsBtn().click();
        assertThat(devicePage.entity(SUPERMARKET_DEVICES_DEVICE_GROUP).isDisplayed())
                .as(SUPERMARKET_DEVICES_DEVICE_GROUP + " is displayed").isTrue();

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersAssetGroupsBtn(RETAIL_COMPANY_A_CUSTOMER).click();
        List.of(SUPERMARKETS_S1_ASSET, SUPERMARKETS_S2_ASSET).forEach(a -> assertThat(assetPage.entity(a).isDisplayed())
                .as(a + " is displayed").isTrue());

        assetPage.groupsBtn().click();
        assertThat(assetPage.entity(SUPERMARKETS_ASSET_GROUP).isDisplayed())
                .as(SUPERMARKETS_ASSET_GROUP + " is displayed").isTrue();

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersAssetGroupsBtn(RETAIL_COMPANY_B_CUSTOMER).click();
        assertThat(assetPage.entity(SUPERMARKETS_S3_ASSET).isDisplayed())
                .as(SUPERMARKETS_S3_ASSET + " is displayed").isTrue();

        assetPage.groupsBtn().click();
        assertThat(assetPage.entity(SUPERMARKETS_ASSET_GROUP).isDisplayed())
                .as(SUPERMARKETS_ASSET_GROUP + " is displayed").isTrue();

        sideBarMenuView.openDeviceProfiles();
        List.of(DOOR_SENSOR_DEVICE_PROFILE, SMOKE_SENSOR_DEVICE_PROFILE, SMART_SHELF_DEVICE_PROFILE, CHILLER_DEVICE_PROFILE,
                MOTION_SENSOR_DEVICE_PROFILE, FREEZER_DEVICE_PROFILE, SMART_BIN_DEVICE_PROFILE, OCCUPANCY_SENSOR_DEVICE_PROFILE,
                LIQUID_LEVEL_SENSOR_DEVICE_PROFILE).forEach(d -> assertThat(devicePage.entity(d).isDisplayed())
                .as(d + " is displayed").isTrue());

        sideBarMenuView.openAssetProfiles();
        assertThat(profilesPage.entity(SUPERMARKET_ASSET_PROFILE).isDisplayed())
                .as(SUPERMARKET_ASSET_PROFILE + " is displayed").isTrue();

        sideBarMenuView.goToDashboardGroups();
        List.of(SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP)
                .forEach(db -> assertThat(dashboardPage.entity(db).isDisplayed()).as(db + " is displayed").isTrue());

        dashboardPage.entity("All").click();
        List.of(SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD, SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD, SMART_SUPERMARKET_DASHBOARD)
                .forEach(db -> assertThat(dashboardPage.entity(db).isDisplayed()).as(db + " is displayed").isTrue());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void smartRetailDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartRetailInstallBtn();
        testRestClient.postSmartRetail();
        refreshPage();
        scrollToElement(solutionTemplatesHomePage.smartRetail());

        invisibilityOf(element);
        assertThat(solutionTemplatesHomePage.smartRetailDeleteBtn().isDisplayed())
                .as("Smart Retail delete btn is displayed").isTrue();
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void smartRetailDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartRetail();
        refreshPage();

        invisibilityOf(element);
        assertThat(solutionTemplateDetailsPage.deleteBtn().isDisplayed()).as("Delete btn is displayed").isTrue();
    }


    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void smartRetailOpenInstruction() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();

        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void smartRetailCloseInstruction() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void smartRetailCloseInstructionByCloseBtn() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void smartRetailInstructionGoToMainDashboard() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void smartRetailDetailsPageOpenInstruction() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void smartRetailDetailsPageCloseInstruction() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void smartRetailDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void smartRetailDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartRetail();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.entityIsNotPresent(SUPERMARKET_DEVICES_RULE_CHAIN);

        sideBarMenuView.goToRoles();
        List.of(SMART_RETAIL_READ_ONLY_ROLE, SMART_RETAIL_USER_ROLE, SMART_RETAIL_ADMINISTRATOR_ROLE)
                .forEach(r -> rolesPage.entityIsNotPresent(r));

        sideBarMenuView.goToCustomerGroups();
        customerPage.entityIsNotPresent(SMART_RETAIL_CUSTOMER_GROUP);

        customerPage.entity("All").click();
        List.of(RETAIL_COMPANY_A_CUSTOMER, RETAIL_COMPANY_B_CUSTOMER).forEach(c -> customerPage.entityIsNotPresent(c));

        sideBarMenuView.openDeviceProfiles();
        List.of(DOOR_SENSOR_DEVICE_PROFILE, SMOKE_SENSOR_DEVICE_PROFILE, SMART_SHELF_DEVICE_PROFILE, CHILLER_DEVICE_PROFILE,
                MOTION_SENSOR_DEVICE_PROFILE, FREEZER_DEVICE_PROFILE, SMART_BIN_DEVICE_PROFILE, OCCUPANCY_SENSOR_DEVICE_PROFILE,
                LIQUID_LEVEL_SENSOR_DEVICE_PROFILE).forEach(dp -> profilesPage.entityIsNotPresent(dp));

        sideBarMenuView.openAssetProfiles();
        profilesPage.entityIsNotPresent(SUPERMARKET_ASSET_PROFILE);

        sideBarMenuView.goToDashboardGroups();
        List.of(SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP)
                .forEach(dg -> dashboardPage.entityIsNotPresent(dg));

        dashboardPage.entity("All").click();
        List.of(SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD, SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD, SMART_SUPERMARKET_DASHBOARD)
                .forEach(db -> dashboardPage.entityIsNotPresent(db));
    }

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
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP,
                SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String dashboard1Id = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP,
                SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();
        String entityGroup1Id = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(2).contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId)
                .contains(Const.URL + "/dashboards/groups/" + entityGroup1Id + "/" + dashboard1Id);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkThingsBoardIoTGateway).as("ThingsBoard IoT Gateway link").isEqualTo(THINGSBOARD_IOT_GATEWAY_DOCS_URL);
        assertThat(linkThingsBoardMQTTGateway).as("ThingsBoard MQTT Gateway link").isEqualTo(THINGSBOARD_MQTT_GATEWAY_DOCS_URL);
        assertThat(linkThingsBoardIntegration).as("ThingsBoard Integration link").isEqualTo(THINGSBOARD_INTEGRATION_DOCS_URL);
    }
}
