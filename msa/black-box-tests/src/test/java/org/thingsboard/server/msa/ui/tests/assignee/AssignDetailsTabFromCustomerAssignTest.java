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
import io.qameta.allure.Feature;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.utils.Const;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

@Feature("Assign from details tab of entity (by customer)")
public class AssignDetailsTabFromCustomerAssignTest extends AbstractAssignTest {

    private AlarmId tenantAlarmId;
    private DeviceId tenantDeviceId;
    private String tenantDeviceName;
    private String tenantAlarmType;
    private AlarmId assignedTenantAlarmId;

    @BeforeMethod
    public void generateTenantEntity() {
        if (getJwtTokenFromLocalStorage() == null) {
            new LoginPageHelper(driver).authorizationTenant();
        }
        tenantAlarmType = "Test tenant alarm " + random();

        tenantDeviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("")).getName();
        tenantDeviceId = testRestClient.getDeviceByName(tenantDeviceName).getId();
        tenantAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(tenantDeviceId, tenantAlarmType)).getId();
    }

    @AfterMethod
    public void deleteTenantEntity() {
        deleteAlarmsByIds(tenantAlarmId, assignedTenantAlarmId);
        deleteDeviceById(tenantDeviceId);
        clearStorage();
    }

    @Description("Can assign alarm to yourself")
    @Test
    public void assignAlarmToYourselfCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarmType, userEmail);

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    @Description("Can reassign alarm from himself to another customer user")
    @Test
    public void reassignAlarmByCustomerFromAnotherCustomerUser() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedAlarmType, userName);

        assertIsDisplayed(alarmPage.assignedUser(userName));
    }

    @Description("Can unassign alarm from himself")
    @Test
    public void unassignedAlarmFromCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarmType);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarmType));
    }

    @Description("Unassign alarm from any other customer user")
    @Test
    public void unassignedAlarmFromAnotherUserFromCustomer() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarmType);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarmType));
    }

    @Description("Unassign alarm from any tenant user")
    @Test
    public void unassignedAlarmFromTenant() {
        String assignedTenantAlarmType = "Test tenant assigned alarm " + random();
        assignedTenantAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, assignedTenantAlarmType)).getId();

        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedTenantAlarmType, Const.TENANT_EMAIL);
        devicePage.closeDetailsViewBtn().click();
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedTenantAlarmType);

        assertIsDisplayed(alarmPage.unassigned(assignedTenantAlarmType));
    }

    @Description("Check the display of names (emails)")
    @Test
    public void checkTheDisplayOfNamesEmailsFromCustomer() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarmType, Const.TENANT_EMAIL);
        devicePage.closeDetailsViewBtn().click();
        devicePage.checkBox(tenantDeviceName).click();
        devicePage.changeOwnerBtn().click();
        devicePage.changeOwner(customerTitle);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Description("Check the reassign tenant for old alarm on device")
    @Test
    public void reassignTenantForOldAlarm() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarmType, Const.TENANT_EMAIL);
        devicePage.closeDetailsViewBtn().click();
        devicePage.checkBox(tenantDeviceName).click();
        devicePage.changeOwnerBtn().click();
        devicePage.changeOwner(customerTitle);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarmType, userEmail);

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    @Description("Check the reassign tenant for old alarm on device")
    @Test
    public void reassignTenantForOldAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarmType, Const.TENANT_EMAIL);
        devicePage.closeDetailsViewBtn().click();
        devicePage.checkBox(tenantDeviceName).click();
        devicePage.changeOwnerBtn().click();
        devicePage.changeOwner(customerTitle);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.alarmDetailsBtn(tenantAlarmType).click();
        alarmDetailsView.assignAlarmTo(userEmail);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }
}
