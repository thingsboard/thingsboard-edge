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
package org.thingsboard.server.msa.ui.tests.solutionTemplates.temperatureHumiditySensors;

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
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CUSTOMER_D;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CUSTOMER_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.READ_ONLY_ROLES;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SENSOR_C1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SENSOR_T1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_SENSOR_DEVICE_PROFILE;

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

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
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

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installTemperatureHumiditySensorsFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphTemperatureHumiditySensor().getText().contains(TEMPERATURE_HUMIDITY_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphTemperatureHumiditySensor().getText().contains(TEMPERATURE_HUMIDITY_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.alarmFirstParagraphTemperatureHumiditySensor().getText().contains(TEMPERATURE_HUMIDITY_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.customersFirstParagraphTemperatureHumiditySensor().getText().contains(TEMPERATURE_HUMIDITY_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installTemperatureHumiditySensorsFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
        Assert.assertNotNull(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionFirstParagraphTemperatureHumiditySensor().getText().contains(TEMPERATURE_HUMIDITY_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.solutionInstructionThirdParagraphTemperatureHumiditySensor().getText().contains(TEMPERATURE_HUMIDITY_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.alarmFirstParagraphTemperatureHumiditySensor().getText().contains(TEMPERATURE_HUMIDITY_DASHBOARD));
        Assert.assertTrue(solutionTemplatesInstalledView.customersFirstParagraphTemperatureHumiditySensor().getText().contains(TEMPERATURE_HUMIDITY_DASHBOARD));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallTemperatureHumiditySensorsPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallTemperatureHumiditySensorsPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.setFirstUserName();
        solutionTemplatesInstalledView.setSecondUserName();
        String user1 = solutionTemplatesInstalledView.getFirstUserName();
        String user2 = solutionTemplatesInstalledView.getSecondUserName();
        solutionTemplatesInstalledView.closeBtn().click();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entity(TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN).isDisplayed());

        sideBarMenuView.goToRoles();

        Assert.assertTrue(rolesPage.entity(READ_ONLY_ROLES).isDisplayed());

        sideBarMenuView.goToAllCustomers();

        Assert.assertTrue(customerPage.entity(CUSTOMER_D).isDisplayed());

        customerPage.manageCustomersDeviceGroupsBtn(CUSTOMER_D).click();

        Assert.assertTrue(devicePage.entity(SENSOR_C1_DEVICE).isDisplayed());

        devicePage.groupsBtn().click();

        Assert.assertTrue(customerPage.entity(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP).isDisplayed());

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersUserBtn(CUSTOMER_D).click();

        Assert.assertTrue(usersPageHelper.entity(user1).isDisplayed());
        Assert.assertTrue(usersPageHelper.entity(user2).isDisplayed());

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entity(TEMPERATURE_SENSOR_DEVICE_PROFILE).isDisplayed());

        sideBarMenuView.goToDeviceGroups();

        Assert.assertTrue(devicePage.entity(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP).isDisplayed());

        devicePage.entity(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP).click();

        Assert.assertTrue(devicePage.entity(SENSOR_T1_DEVICE).isDisplayed());

        sideBarMenuView.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entity(TEMPERATURE_HUMIDITY_DASHBOARD).isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void temperatureHumiditySensorsDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn();
        testRestClient.postTemperatureHumidity();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplatesHomePage.temperatureHumiditySensorsDeleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void temperatureHumiditySensorsDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postTemperatureHumidity();
        refreshPage();

        Assert.assertTrue(invisibilityOf(element));
        Assert.assertTrue(solutionTemplateDetailsPage.deleteBtn().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void temperatureHumiditySensorsOpenInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void temperatureHumiditySensorsCloseInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void temperatureHumiditySensorsCloseInstructionByCloseBtn() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void temperatureHumiditySensorsInstructionGoToMainDashboard() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void temperatureHumiditySensorsDetailsPageOpenInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        Assert.assertTrue(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void temperatureHumiditySensorsDetailsPageCloseInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void temperatureHumiditySensorsDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        Assert.assertTrue(invisibilityOf(element));
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void temperatureHumiditySensorsDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId, getUrl());
    }

    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test(groups = "broken")
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteTemperatureHumidity();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.elementIsNotPresent(TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN));

        sideBarMenuView.goToRoles();

        Assert.assertTrue(rolesPage.entityIsNotPresent(READ_ONLY_ROLES));

        sideBarMenuView.goToAllCustomers();

        Assert.assertTrue(customerPage.entityIsNotPresent(CUSTOMER_D));

        sideBarMenuView.openDeviceProfiles();

        Assert.assertTrue(profilesPage.entityIsNotPresent(TEMPERATURE_SENSOR_DEVICE_PROFILE));

        sideBarMenuView.goToDeviceGroups();

        Assert.assertTrue(devicePage.entityIsNotPresent(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP));

        devicePage.entity("All").click();

        Assert.assertTrue(devicePage.entityIsNotPresent(SENSOR_T1_DEVICE));

        sideBarMenuView.goToAllDashboards();

        Assert.assertTrue(dashboardPage.entityIsNotPresent(TEMPERATURE_HUMIDITY_DASHBOARD));
    }
    @Epic("Solution templates")
    @Feature("Installation")
    @Story("Temperature & Humidity Sensors")
    @Test(groups = {"broken"})
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getId().toString();

        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId));
        Assert.assertEquals(DASHBOARD_GIDE_DOCS_URL, guide);
        Assert.assertEquals(HTTP_API_DOCS_URL, linkHttpApi);
        Assert.assertEquals(CONNECTIVITY_DOCS_URL, linkConnectionDevices);
        Assert.assertEquals(ALARM_RULES_DOCS_URL, linkAlarmRule);
        Assert.assertEquals(getBaseUiUrl() + "/profiles/deviceProfiles", linkDeviceProfile);
    }
}
