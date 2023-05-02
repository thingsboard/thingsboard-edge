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
package org.thingsboard.server.msa.ui.tests.assignee;

import io.qameta.allure.Description;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.msa.ui.pages.AlarmWidgetElements;
import org.thingsboard.server.msa.ui.pages.CreateWidgetPopupHelper;
import org.thingsboard.server.msa.ui.pages.DashboardPageHelper;
import org.thingsboard.server.msa.ui.utils.Const;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;

public class AssignFromAlarmWidgetTest extends AbstractAssignTest {

    private Dashboard dashboard;
    private DashboardPageHelper dashboardPage;
    private AlarmWidgetElements alarmWidget;

    @BeforeClass
    public void create() {
        dashboardPage = new DashboardPageHelper(driver);
        CreateWidgetPopupHelper createWidgetPopup = new CreateWidgetPopupHelper(driver);
        alarmWidget = new AlarmWidgetElements(driver);

        dashboard = testRestClient.postDashboard(EntityPrototypes.defaultDashboardPrototype("Dashboard"));
        sideBarMenuView.goToAllDashboards();
        dashboardPage.entity(dashboard.getName()).click();
        dashboardPage.editBtn().click();
        dashboardPage.openSelectWidgetsBundleMenu();
        dashboardPage.openCreateWidgetPopup();
        createWidgetPopup.goToCreateEntityAliasPopup("Alias");
        createWidgetPopup.selectFilterType("Single entity");
        createWidgetPopup.selectType("Device");
        createWidgetPopup.selectEntity(deviceName);
        createWidgetPopup.addAliasBtn().click();
        createWidgetPopup.addWidgetBtn().click();
        dashboardPage.increaseSizeOfTheWidget();
        dashboardPage.doneBtn().click();
    }

    @AfterClass
    public void delete() {
        deleteDashboardById(dashboard.getId());
    }

    @BeforeMethod
    public void goToDashboardPage() {
        sideBarMenuView.goToAllDashboards();
        dashboardPage.entity(dashboard.getName()).click();
    }

    @Description("Can assign alarm to yourself")
    @Test
    public void assignAlarmToYourself() {
        alarmWidget.assignAlarmTo(alarmType, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmWidget.assignedUser(Const.TENANT_EMAIL));
    }

    @Description("Can reassign alarm to another user")
    @Test
    public void reassignAlarm() {
        alarmWidget.assignAlarmTo(assignedAlarmType, userWithNameEmail);

        assertIsDisplayed(alarmWidget.assignedUser(userName));
    }

    @Description("Can unassign alarm")
    @Test
    public void unassignedAlarm() {
        alarmWidget.unassignedAlarm(assignedAlarmType);

        assertIsDisplayed(alarmWidget.unassigned(assignedAlarmType));
    }

    @Description("Assign alarm to yourself from details of alarm")
    @Test
    public void assignAlarmToYourselfFromDetails() {
        alarmWidget.alarmDetailsBtn(alarmType).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmWidget.assignedUser(Const.TENANT_EMAIL));
    }

    @Description("Reassign alarm to another user from details of alarm")
    @Test
    public void reassignAlarmFromDetails() {
        alarmWidget.alarmDetailsBtn(assignedAlarmType).click();
        alarmDetailsView.assignAlarmTo(userWithNameEmail);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmWidget.assignedUser(userName));
    }

    @Description("Unassign alarm from details of alarm")
    @Test
    public void unassignedAlarmFromDetails() {
        alarmWidget.alarmDetailsBtn(assignedAlarmType).click();
        alarmDetailsView.unassignedAlarm();
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmWidget.unassigned(assignedAlarmType));
    }

    @Description("Search by email")
    @Test
    public void searchByEmail() {
        alarmWidget.searchAlarm(alarmType, Const.TENANT_EMAIL);
        alarmWidget.setUsers();

        assertThat(alarmWidget.getUsers()).hasSize(1).as("Search result contains search input").contains(Const.TENANT_EMAIL);
        alarmWidget.assignUsers().forEach(this::assertIsDisplayed);
    }

    @Description("Search by name")
    @Test(groups = "broken")
    public void searchByName() {
        alarmWidget.searchAlarm(alarmType, userName);
        alarmWidget.setUsers();

        assertThat(alarmWidget.getUsers()).hasSize(1).as("Search result contains search input").contains(userName);
        alarmWidget.assignUsers().forEach(this::assertIsDisplayed);
    }
}
