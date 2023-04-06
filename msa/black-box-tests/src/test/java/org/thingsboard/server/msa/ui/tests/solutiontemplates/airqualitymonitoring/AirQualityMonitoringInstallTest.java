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
package org.thingsboard.server.msa.ui.tests.solutiontemplates.airqualitymonitoring;

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
import static org.thingsboard.server.msa.ui.utils.Const.HTTP_API_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_5_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_CITY_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_CITY_ASSET_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_CITY_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_SENSOR_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_SENSOR_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LOS_ANGELES_CA_ASSET;

@Feature("Installation")
@Story("Air Quality Monitoring")
public class AirQualityMonitoringInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteAirQualityMonitoring();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void airQualityMonitoringOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).isEqualTo(Const.URL + "/solutionTemplates/air_quality_index");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).isEqualTo("Air Quality Monitoring");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("AIR Quality Monitoring template");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description").contains("Air Quality Monitoring template");
        solutionTemplateDetailsPage.airQualityMonitoringScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installAirQualityMonitoringFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
        assertThat(solutionTemplatesInstalledView.alarmFirstParagraphAirQualityMonitoring().getText())
                .as("First paragraph of solution instruction").contains(AIR_QUALITY_MONITORING_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installAirQualityMonitoringFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
        assertThat(solutionTemplatesInstalledView.alarmFirstParagraphAirQualityMonitoring().getText())
                .as("First paragraph of solution instruction").contains(AIR_QUALITY_MONITORING_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallAirQualityMonitoringPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallAirQualityMonitoringPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.ruleChainsBtn().click();
        List.of(AQI_SENSOR_RULE_CHAIN, AQI_CITY_RULE_CHAIN).forEach(rc -> assertThat(ruleChainsPage.entity(rc).isDisplayed())
                .as(rc + " is displayed").isTrue());

        sideBarMenuView.goToDeviceGroups();
        assertThat(devicePage.entity(AQI_SENSOR_DEVICE_GROUP).isDisplayed()).as(AQI_SENSOR_DEVICE_GROUP + " is displayed").isTrue();

        devicePage.entity("All").click();
        List.of(AIR_QUALITY_SENSOR_1_DEVICE, AIR_QUALITY_SENSOR_2_DEVICE, AIR_QUALITY_SENSOR_3_DEVICE, AIR_QUALITY_SENSOR_4_DEVICE,
                AIR_QUALITY_SENSOR_5_DEVICE).forEach(d -> assertThat(devicePage.entity(d).isDisplayed()).as(d + " is displayed").isTrue());

        sideBarMenuView.goToAssetGroups();
        assertThat(assetPage.entity(AQI_CITY_ASSET_GROUP).isDisplayed()).as(AQI_CITY_ASSET_GROUP + " is displayed").isTrue();

        assetPage.entity("All").click();
        assertThat(assetPage.entity(LOS_ANGELES_CA_ASSET).isDisplayed()).as(LOS_ANGELES_CA_ASSET + " is displayed").isTrue();

        sideBarMenuView.openAssetProfiles();
        assertThat(profilesPage.entity(AQI_CITY_ASSET_PROFILE).isDisplayed()).as(AQI_CITY_ASSET_PROFILE + " is displayed").isTrue();

        sideBarMenuView.goToAllDashboards();
        List.of(AIR_QUALITY_MONITORING_DASHBOARD, AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD)
                .forEach(db -> assertThat(dashboardPage.entity(db).isDisplayed()).as(db + " is displayed").isTrue());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void airQualityMonitoringDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.airQualityMonitoringInstallBtn();
        testRestClient.postAirQualityMonitoring();
        refreshPage();

        invisibilityOf(element);
        assertThat(solutionTemplatesHomePage.airQualityMonitoringDeleteBtn().isDisplayed())
                .as("Air Quality Monitoring delete btn is displayed").isTrue();
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void airQualityMonitoringDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postAirQualityMonitoring();
        refreshPage();

        invisibilityOf(element);
        assertThat(solutionTemplateDetailsPage.deleteBtn().isDisplayed()).as("Delete btn is displayed").isTrue();
    }

    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void airQualityMonitoringOpenInstruction() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstructionBtn().click();

        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void airQualityMonitoringCloseInstruction() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void airQualityMonitoringCloseInstructionByCloseBtn() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void airQualityMonitoringDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void airQualityMonitoringDetailsPageOpenInstruction() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertThat(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp().isDisplayed())
                .as("Solution template installed popup is displayed").isTrue();
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void airQualityMonitoringDetailsPageDetailsPageCloseInstruction() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void airQualityMonitoringDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        invisibilityOf(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void airQualityMonitoringInstructionGoToMainDashboard() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteAirQualityMonitoring();

        sideBarMenuView.ruleChainsBtn().click();
        List.of(AQI_SENSOR_RULE_CHAIN, AQI_CITY_RULE_CHAIN).forEach(rc -> ruleChainsPage.entityIsNotPresent(rc));

        sideBarMenuView.goToDeviceGroups();
        devicePage.entityIsNotPresent(AQI_SENSOR_DEVICE_GROUP);

        devicePage.entity("All").click();
        List.of(AIR_QUALITY_SENSOR_1_DEVICE, AIR_QUALITY_SENSOR_2_DEVICE, AIR_QUALITY_SENSOR_3_DEVICE, AIR_QUALITY_SENSOR_4_DEVICE,
                AIR_QUALITY_SENSOR_5_DEVICE).forEach(d -> devicePage.entityIsNotPresent(d));

        sideBarMenuView.goToAssetGroups();
        assetPage.entityIsNotPresent(AQI_CITY_ASSET_GROUP);

        assetPage.entity("All").click();
        assetPage.entityIsNotPresent(LOS_ANGELES_CA_ASSET);

        sideBarMenuView.openAssetProfiles();
        profilesPage.entityIsNotPresent(AQI_CITY_ASSET_PROFILE);

        sideBarMenuView.goToAllDashboards();
        List.of(AIR_QUALITY_MONITORING_DASHBOARD, AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD)
                .forEach(db -> dashboardPage.entityIsNotPresent(db));
    }

    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkAlarmRules = solutionTemplatesInstalledView.getAlarmRulesLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(1).contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkAlarmRules).as("Alarm rule link").isEqualTo(ALARM_RULES_DOCS_URL);
        assertThat(linkDeviceProfile).as("Device profile link").isEqualTo(getBaseUiUrl() + "/profiles/deviceProfiles");
    }
}
