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
package org.thingsboard.server.msa.ui.tests.solutiontemplates.temperaturehumiditysensors;

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
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CUSTOMER_D;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CUSTOMER_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.READ_ONLY_ROLES;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SENSOR_C1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SENSOR_T1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_SENSOR_DEVICE_PROFILE;

@Feature("Installation")
@Story("Temperature & Humidity Sensors")
public class TemperatureHumiditySensorsInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteTemperatureHumidity();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void temperatureHumiditySensorsOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).isEqualTo(Const.URL + "/solutionTemplates/temperature_sensors");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).isEqualTo("Temperature & Humidity Sensors");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("Temperature & Humidity sensors");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description").contains("Temperature & Humidity template");
        solutionTemplateDetailsPage.temperatureHumiditySensorsScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installTemperatureHumiditySensorsFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionThirdParagraphTemperatureHumiditySensor().getText())
                .as("Third paragraph of solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.alarmFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of alarm section in solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of customers section in solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installTemperatureHumiditySensorsFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionThirdParagraphTemperatureHumiditySensor().getText())
                .as("Third paragraph of solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.alarmFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of alarm section in solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of customers section in solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallTemperatureHumiditySensorsPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallTemperatureHumiditySensorsPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

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
        assertThat(ruleChainsPage.entity(TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN).isDisplayed())
                .as(TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN + " is displayed").isTrue();

        sideBarMenuView.goToRoles();
        assertThat(rolesPage.entity(READ_ONLY_ROLES).isDisplayed()).as(READ_ONLY_ROLES + " is displayed").isTrue();

        sideBarMenuView.goToAllCustomers();
        assertThat(customerPage.entity(CUSTOMER_D).isDisplayed()).as(CUSTOMER_D + " is displayed").isTrue();

        customerPage.manageCustomersDeviceGroupsBtn(CUSTOMER_D).click();
        assertThat(devicePage.entity(SENSOR_C1_DEVICE).isDisplayed()).as(SENSOR_C1_DEVICE + " is displayed").isTrue();

        devicePage.groupsBtn().click();
        assertThat(devicePage.entity(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP).isDisplayed())
                .as(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP + " is displayed").isTrue();

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersUserBtn(CUSTOMER_D).click();
        List.of(user1, user2).forEach(u -> assertThat(usersPage.entity(u).isDisplayed()).as(u + " is displayed").isTrue());

        sideBarMenuView.openDeviceProfiles();
        assertThat(profilesPage.entity(TEMPERATURE_SENSOR_DEVICE_PROFILE).isDisplayed())
                .as(TEMPERATURE_SENSOR_DEVICE_PROFILE + " is displayed").isTrue();

        sideBarMenuView.goToDeviceGroups();
        assertThat(devicePage.entity(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP).isDisplayed())
                .as(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP + " is displayed").isTrue();

        devicePage.entity(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP).click();
        assertThat(devicePage.entity(SENSOR_T1_DEVICE).isDisplayed()).as(SENSOR_T1_DEVICE + " is displayed").isTrue();

        sideBarMenuView.goToAllDashboards();
        assertThat(dashboardPage.entity(TEMPERATURE_HUMIDITY_DASHBOARD).isDisplayed())
                .as(TEMPERATURE_HUMIDITY_DASHBOARD + " is displayed").isTrue();
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void temperatureHumiditySensorsDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn();
        testRestClient.postTemperatureHumidity();
        refreshPage();

        invisibilityOf(element);
        assertThat(solutionTemplatesHomePage.temperatureHumiditySensorsDeleteBtn().isDisplayed())
                .as("Temperature & Humidity Sensors delete btn is displayed").isTrue();
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void temperatureHumiditySensorsDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postTemperatureHumidity();
        refreshPage();

        invisibilityOf(element);
        assertThat(solutionTemplateDetailsPage.deleteBtn().isDisplayed()).as("Delete btn is displayed").isTrue();
    }

    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void temperatureHumiditySensorsOpenInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();

        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void temperatureHumiditySensorsCloseInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void temperatureHumiditySensorsCloseInstructionByCloseBtn() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void temperatureHumiditySensorsInstructionGoToMainDashboard() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void temperatureHumiditySensorsDetailsPageOpenInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void temperatureHumiditySensorsDetailsPageCloseInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void temperatureHumiditySensorsDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void temperatureHumiditySensorsDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteTemperatureHumidity();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.entityIsNotPresent(TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN);

        sideBarMenuView.goToRoles();
        rolesPage.entityIsNotPresent(READ_ONLY_ROLES);

        sideBarMenuView.goToAllCustomers();
        customerPage.entityIsNotPresent(CUSTOMER_D);

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entityIsNotPresent(TEMPERATURE_SENSOR_DEVICE_PROFILE);

        sideBarMenuView.goToDeviceGroups();
        devicePage.entityIsNotPresent(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP);

        devicePage.entity("All").click();
        devicePage.entityIsNotPresent(SENSOR_T1_DEVICE);

        sideBarMenuView.goToAllDashboards();
        dashboardPage.entityIsNotPresent(TEMPERATURE_HUMIDITY_DASHBOARD);
    }

    @Test
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
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(1).contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(guide).as("Dashboard guide link").isEqualTo(DASHBOARD_GIDE_DOCS_URL);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkAlarmRule).as("Alarm rule link").isEqualTo(ALARM_RULES_DOCS_URL);
        assertThat(linkDeviceProfile).as("Device profile link").isEqualTo(getBaseUiUrl() + "/profiles/deviceProfiles");
    }
}
