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
package org.thingsboard.server.msa.ui.tests.alarmassignee;

import io.qameta.allure.Epic;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.AlarmDetailsEntityTabHelper;
import org.thingsboard.server.msa.ui.pages.AlarmDetailsViewHelper;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DevicePageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

@Epic("Alarm assign")
abstract public class AbstractAssignTest extends AbstractDriverBaseTest {

    protected AlarmId alarmId;
    protected AlarmId assignedAlarmId;
    protected DeviceId deviceId;
    protected UserId userId;
    protected UserId userWithNameId;
    protected CustomerId customerId;
    protected String deviceName;
    protected String userName;
    protected String customerTitle;
    protected String userEmail;
    protected String userWithNameEmail;
    protected String alarmType;
    protected String assignedAlarmType;
    protected SideBarMenuViewHelper sideBarMenuView;
    protected AlarmDetailsEntityTabHelper alarmPage;
    protected DevicePageHelper devicePage;
    protected CustomerPageHelper customerPage;
    protected AlarmDetailsViewHelper alarmDetailsView;

    @BeforeClass
    public void generateCommonTestEntity() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        alarmPage = new AlarmDetailsEntityTabHelper(driver);
        devicePage = new DevicePageHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        alarmDetailsView = new AlarmDetailsViewHelper(driver);

        userName = "User " + random();
        customerTitle = "Customer " + random();
        userEmail = random() + "@thingsboard.org";
        userWithNameEmail = random() + "@thingsboard.org";
        alarmType = "Test alarm " + random();
        assignedAlarmType = "Test assigned alarm " + random();

        customerId = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(customerTitle)).getId();
        userId = testRestClient.postUser(EntityPrototypes.defaultUser(userEmail, getCustomerByName(customerTitle).getId()),
                getCustomerUserGroupByCustomerTitleAndGroupName(customerTitle, EntityGroup.GROUP_CUSTOMER_ADMINS_NAME).getId()).getId();
        userWithNameId = testRestClient.postUser(EntityPrototypes.defaultUser(userWithNameEmail, getCustomerByName(customerTitle).getId(), userName),
                getCustomerUserGroupByCustomerTitleAndGroupName(customerTitle, EntityGroup.GROUP_CUSTOMER_ADMINS_NAME).getId()).getId();
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device ", customerId)).getName();
        deviceId = testRestClient.getDeviceByName(deviceName).getId();
    }

    @AfterClass
    public void deleteCommonEntities() {
        deleteCustomerById(customerId);
    }

    @BeforeMethod
    public void createCommonTestAlarms() {
        alarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, alarmType)).getId();
        assignedAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, assignedAlarmType, userId)).getId();
    }

    @AfterMethod
    public void deleteCommonCreatedAlarms() {
        deleteAlarmsByIds(alarmId, assignedAlarmId);
    }

    public void loginByUser(String userEmail) {
        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersUserBtn(customerTitle).click();
        customerPage.getUserLoginBtnByEmail(userEmail).click();
    }
}
