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
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.tabs.AssignDeviceTabHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Assign to customer")
public class AssignToCustomerTest extends AbstractDeviceTest {

    private AssignDeviceTabHelper assignDeviceTab;
    private CustomerPageHelper customerPage;
    private CustomerId customerId;
    private Device device;
    private Device device1;
    private String customerName;

    @BeforeClass
    public void create() {
        assignDeviceTab = new AssignDeviceTabHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        Customer customer = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(ENTITY_NAME + random()));
        customerId = customer.getId();
        customerName = customer.getName();
        device1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device " + random()));
    }

    @AfterClass
    public void deleteCustomer() {
        deleteCustomerById(customerId);
        deleteCustomerByName("Public");
        deleteDeviceByName(device1.getName());
    }

    @BeforeMethod
    public void createDevice() {
        device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();
    }

    @Test(groups = "smoke")
    @Description("Assign to customer by right side of device btn")
    public void assignToCustomerByRightSideBtn() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignBtn(deviceName).click();
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceOwnerOnPage(deviceName));
        assertThat(devicePage.deviceOwnerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Assign to customer by 'Assign to customer' btn on details tab")
    public void assignToCustomerFromDetailsTab() {
        sideBarMenuView.goToDevicesPage();
        devicePage.device(deviceName).click();
        devicePage.assignBtnDetailsTab().click();
        assignDeviceTab.assignOnCustomer(customerName);
        String customerInAssignedField = devicePage.assignFieldDetailsTab().getAttribute("value");
        devicePage.closeDeviceDetailsViewBtn().click();
        assertIsDisplayed(devicePage.deviceOwnerOnPage(deviceName));
        assertThat(devicePage.deviceOwnerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);
        assertThat(customerInAssignedField)
                .as("Customer in details tab added correctly").isEqualTo(customerName);

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Assign marked device by btn on the top")
    public void assignToCustomerMarkedDevice() {
        sideBarMenuView.goToDevicesPage();
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceOwnerOnPage(deviceName));
        assertThat(devicePage.deviceOwnerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Unassign from customer by right side of device btn")
    public void unassignedFromCustomerByRightSideBtn() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);

        sideBarMenuView.goToDevicesPage();
        WebElement element = devicePage.deviceOwnerOnPage(deviceName);
        devicePage.unassignedDeviceByRightSideBtn(deviceName);
        assertInvisibilityOfElement(element);

        sideBarMenuView.goToAllDevices();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Unassign from customer by 'Unassign from customer' btn on details tab")
    public void unassignedFromCustomerFromDetailsTab() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);

        sideBarMenuView.goToDevicesPage();
        WebElement customerInColumn = devicePage.deviceOwnerOnPage(deviceName);
        devicePage.device(deviceName).click();
        WebElement assignFieldDetailsTab = devicePage.assignFieldDetailsTab();
        devicePage.unassignedDeviceFromDetailsTab();
        assertInvisibilityOfElement(customerInColumn);
        assertInvisibilityOfElement(assignFieldDetailsTab);

        devicePage.closeDeviceDetailsViewBtn().click();
        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Can't assign device on several customer")
    public void assignToSeveralCustomer() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);
        sideBarMenuView.goToDevicesPage();

        assertIsDisable(devicePage.assignBtnVisible(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Can't assign public device")
    public void assignPublicDevice() {
        testRestClient.setDevicePublic(device.getId());

        sideBarMenuView.goToDevicesPage();
        assertIsDisable(devicePage.assignBtnVisible(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Assign several devices by btn on the top")
    public void assignSeveralDevices() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignSelectedDevices(deviceName, device1.getName());
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceOwnerOnPage(deviceName));
        assertThat(devicePage.deviceOwnerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);
        assertThat(devicePage.deviceOwnerOnPage(device1.getName()).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();
        List.of(deviceName, device1.getName()).
                forEach(d -> assertIsDisplayed(devicePage.device(d)));
    }
}
