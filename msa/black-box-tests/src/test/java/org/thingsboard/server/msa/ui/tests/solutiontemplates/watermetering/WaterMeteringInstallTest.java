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
package org.thingsboard.server.msa.ui.tests.solutiontemplates.watermetering;

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

@Feature("Installation")
@Story("Water metering")
public class WaterMeteringInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteWaterMetering();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void waterMeteringOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).isEqualTo(Const.URL + "/solutionTemplates/water_metering");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).isEqualTo("Water metering");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("Water Metering template");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description")
                .contains("Water Metering template");
        solutionTemplateDetailsPage.waterMeteringScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installWaterMeteringFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphWaterMetering().getText())
                .as("First paragraph of dashboard section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionSecondParagraphWaterMetering().getText())
                .as("Second paragraph of dashboard section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersFirstParagraphWaterMetering().getText())
                .as("First paragraph of customers section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installWaterMeteringFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphWaterMetering().getText())
                .as("First paragraph of dashboard section in solution instruction ").contains(WATER_METERING_TENANT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionSecondParagraphWaterMetering().getText())
                .as("Second paragraph of dashboard section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersFirstParagraphWaterMetering().getText())
                .as("First paragraph of customers section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallWaterMeteringPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallWaterMeteringPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

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
        List.of(WATER_METERING_SOLUTION_MAIN_RULE_CHAIN, WATER_METERING_SOLUTION_ALARM_ROUTING_RULE_CHAIN,
                        WATER_METERING_SOLUTION_TENANT_ALARM_ROUTING_RULE_CHAIN)
                .forEach(rc -> assertThat(ruleChainsPage.entity(rc).isDisplayed()).as(rc + " is displayed").isTrue());

        sideBarMenuView.goToRoles();
        List.of(WATER_METERING_READ_ONLY_ROLES, WATER_METERING_USER_ROLES)
                .forEach(r -> assertThat(rolesPage.entity(r).isDisplayed()).as(r + " is displayed").isTrue());

        sideBarMenuView.goToCustomerGroups();
        assertThat(customerPage.entity(WATER_METERING_CUSTOMER_GROUP).isDisplayed())
                .as(WATER_METERING_CUSTOMER_GROUP + " is displayed").isTrue();

        customerPage.entity("All").click();
        List.of(WATER_METERING_CUSTOMER_A_CUSTOMER, WATER_METERING_CUSTOMER_B_CUSTOMER)
                .forEach(c -> assertThat(customerPage.entity(c).isDisplayed()).as(c + " is displayed").isTrue());

        customerPage.manageCustomersUserBtn(WATER_METERING_CUSTOMER_A_CUSTOMER).click();
        assertThat(usersPage.entity(userCustomerA).isDisplayed())
                .as(userCustomerA + " is displayed in " + WATER_METERING_CUSTOMER_A_CUSTOMER).isTrue();

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersUserBtn(WATER_METERING_CUSTOMER_B_CUSTOMER).click();
        assertThat(usersPage.entity(userCustomerB).isDisplayed())
                .as(userCustomerB + " is displayed in " + WATER_METERING_CUSTOMER_B_CUSTOMER).isTrue();

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(WATER_METERING_CUSTOMER_A_CUSTOMER).click();
        devicePage.groupsBtn().click();
        assertThat(devicePage.entity(WATER_METERS_DEVICE_GROUP).isDisplayed())
                .as(WATER_METERS_DEVICE_GROUP + " is displayed in " + WATER_METERING_CUSTOMER_A_CUSTOMER).isTrue();

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(WATER_METERING_CUSTOMER_B_CUSTOMER).click();
        devicePage.groupsBtn().click();
        assertThat(devicePage.entity(WATER_METERS_DEVICE_GROUP).isDisplayed())
                .as(WATER_METERS_DEVICE_GROUP + " is displayed in " + WATER_METERING_CUSTOMER_B_CUSTOMER).isTrue();

        sideBarMenuView.openDeviceProfiles();
        assertThat(profilesPage.entity(WATER_METER_DEVICE_PROFILE_WM).isDisplayed())
                .as(WATER_METER_DEVICE_PROFILE_WM + " is displayed").isTrue();

        sideBarMenuView.goToAllDevices();
        List.of(WM0000123_DEVICE, WM0000124_DEVICE, WM0000125_DEVICE)
                .forEach(d -> assertThat(devicePage.entity(d).isDisplayed()).as(d + " is displayed").isTrue());

        sideBarMenuView.goToDashboardGroups();
        List.of(WATER_METERING_DASHBOARD_GROUP, WATER_METERING_SHARED_DASHBOARD_GROUP)
                .forEach(dg -> assertThat(dashboardPage.entity(dg).isDisplayed()).as(dg + " is displayed").isTrue());

        dashboardPage.entity("All").click();
        List.of(WATER_METERING_USER_DASHBOARD, WATER_METERING_TENANT_DASHBOARD)
                .forEach(d -> assertThat(dashboardPage.entity(d).isDisplayed()).as(d + " is displayed").isTrue());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void waterMeteringDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.waterMeteringInstallBtn();
        testRestClient.postWaterMetering();
        refreshPage();
        scrollToElement(solutionTemplatesHomePage.waterMetering());

        invisibilityOf(element);
        assertThat(solutionTemplatesHomePage.waterMeteringDeleteBtn().isDisplayed()).
                as("Water metering delete btn is displayed").isTrue();
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void waterMeteringDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postWaterMetering();
        refreshPage();

        invisibilityOf(element);
        assertThat(solutionTemplateDetailsPage.deleteBtn().isDisplayed()).as("Delete btn is displayed").isTrue();
    }

    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void waterMeteringOpenInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();

        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void waterMeteringCloseInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void waterMeteringCloseInstructionByCloseBtn() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void waterMeteringInstructionGoToMainDashboard() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void waterMeteringDetailsPageOpenInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void waterMeteringDetailsPageCloseInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void waterMeteringDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void waterMeteringDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteWaterMetering();

        sideBarMenuView.ruleChainsBtn().click();
        List.of(WATER_METERING_SOLUTION_MAIN_RULE_CHAIN, WATER_METERING_SOLUTION_ALARM_ROUTING_RULE_CHAIN,
                WATER_METERING_SOLUTION_TENANT_ALARM_ROUTING_RULE_CHAIN).forEach(rc -> ruleChainsPage.entityIsNotPresent(rc));

        sideBarMenuView.goToRoles();
        List.of(WATER_METERING_READ_ONLY_ROLES, WATER_METERING_USER_ROLES).forEach(r -> rolesPage.entityIsNotPresent(r));

        sideBarMenuView.goToCustomerGroups();
        customerPage.entityIsNotPresent(WATER_METERING_CUSTOMER_GROUP);
        customerPage.entity("All").click();
        List.of(WATER_METERING_CUSTOMER_A_CUSTOMER, WATER_METERING_CUSTOMER_B_CUSTOMER)
                .forEach(c -> customerPage.entityIsNotPresent(c));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entityIsNotPresent(WATER_METER_DEVICE_PROFILE_WM);

        sideBarMenuView.goToDeviceGroups();
        devicePage.entityIsNotPresent(WATER_METERS_DEVICE_GROUP);

        devicePage.entity("All").click();
        List.of(WM0000123_DEVICE, WM0000124_DEVICE, WM0000125_DEVICE).forEach(d -> devicePage.entityIsNotPresent(d));

        sideBarMenuView.goToDashboardGroups();
        List.of(WATER_METERING_DASHBOARD_GROUP, WATER_METERING_SHARED_DASHBOARD_GROUP).forEach(db -> dashboardPage.entityIsNotPresent(db));

        dashboardPage.entity("All").click();
        List.of(WATER_METERING_USER_DASHBOARD, WATER_METERING_TENANT_DASHBOARD).forEach(db -> dashboardPage.entityIsNotPresent(db));
    }

    @Test
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
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(1).contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(guide).as("Dashboard guide link").isEqualTo(DASHBOARD_GIDE_DOCS_URL);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkAlarmRule).as("Alarm rule link").isEqualTo(ALARM_RULES_DOCS_URL);
        assertThat(linkDeviceProfile).as("Device profile link").isEqualTo(getBaseUiUrl() + "/profiles/deviceProfiles");
    }
}
