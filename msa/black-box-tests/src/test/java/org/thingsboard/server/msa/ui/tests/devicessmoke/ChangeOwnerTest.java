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
import org.openqa.selenium.Keys;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.tabs.ChangeDeviceOwnerTabHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Change owner")
public class ChangeOwnerTest extends AbstractDeviceTest {

    private ChangeDeviceOwnerTabHelper changeDeviceOwnerTab;
    private CustomerPageHelper customerPage;
    private Device device;
    private Device device1;
    private String customerName;

    @BeforeClass
    public void create() {
        changeDeviceOwnerTab = new ChangeDeviceOwnerTabHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        Customer customer = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(ENTITY_NAME + random()));
        customerName = customer.getName();
        device1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device " + random()));
    }

    @AfterClass
    public void deleteCustomer() {
        deleteCustomerByName(customerName);
        deleteDeviceByName(device1.getName());
    }

    @BeforeMethod
    public void createDevice() {
        device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();
    }

    @Test(groups = "smoke")
    @Description("Change owner")
    public void changeOwner() {
        sideBarMenuView.goToDevicesPage();
        devicePage.changeOwnerSelectedDevices(deviceName);
        changeDeviceOwnerTab.changeOwnerOn(customerName);
        assertIsDisplayed(devicePage.deviceOwnerOnPage(deviceName));
        assertThat(devicePage.deviceOwnerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();

        assertIsDisplayed(devicePage.device(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Can't change owner with empty owner field")
    public void changeOwnerWithoutEnterName() {
        sideBarMenuView.goToDevicesPage();
        devicePage.changeOwnerSelectedDevices(deviceName);
        changeDeviceOwnerTab.selectOwner(customerName);
        changeDeviceOwnerTab.clearBtn().click();
        changeDeviceOwnerTab.changeOwnerField().sendKeys(Keys.ESCAPE);

        assertIsDisplayed(changeDeviceOwnerTab.errorOwnerRequired());
        assertIsDisable(changeDeviceOwnerTab.changeOwnerBtnVisible());
    }

    @Test(groups = "smoke")
    @Description("Can't change owner with only space in owner field")
    public void changeOwnerWithOnlySpaseInChangeOwnerField() {
        sideBarMenuView.goToDevicesPage();
        devicePage.changeOwnerSelectedDevices(deviceName);
        changeDeviceOwnerTab.changeOwnerField().sendKeys(" ");
        changeDeviceOwnerTab.changeOwnerField().sendKeys(Keys.ESCAPE);

        assertIsDisable(changeDeviceOwnerTab.changeOwnerBtnVisible());
    }

    @Test(groups = "smoke")
    @Description("Change owner for several device")
    public void changeOwnerForSeveralDevice() {
        sideBarMenuView.goToDevicesPage();
        devicePage.changeOwnerSelectedDevices(deviceName, device1.getName());
        changeDeviceOwnerTab.changeOwnerOn(customerName);
        List.of(deviceName, device1.getName())
                .forEach(d -> assertIsDisplayed(devicePage.deviceOwnerOnPage(d)));
        List.of(deviceName, device1.getName())
                .forEach(d -> assertThat(devicePage.deviceOwnerOnPage(d).getText())
                        .as("Customer added correctly").isEqualTo(customerName));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();

        List.of(deviceName, device1.getName())
                .forEach(d -> assertIsDisplayed(devicePage.device(d)));
    }
}
