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
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.DEVICE_PROFILE_IS_REQUIRED_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_DEVICE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.NAME_IS_REQUIRED_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_DEVICE_MESSAGE;

@Feature("Create device")
public class CreateDeviceTest extends AbstractDeviceTest {

    @Test(groups = "smoke")
    @Description("Add device after specifying the name (text/numbers /special characters)")
    public void createDevice() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(deviceName);
        devicePage.addBtnC().click();
        devicePage.refreshBtn().click();

        assertIsDisplayed(devicePage.entity(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Add device after specifying the name and description (text/numbers /special characters)")
    public void createDeviceWithDescription() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(deviceName);
        devicePage.enterDescription(deviceName);
        devicePage.addBtnC().click();
        devicePage.refreshBtn().click();
        devicePage.entity(deviceName).click();
        devicePage.setHeaderName();

        assertThat(devicePage.getHeaderName()).as("Header of device details tab").isEqualTo(deviceName);
        assertThat(devicePage.descriptionEntityView().getAttribute("value"))
                .as("Description in device details tab").isEqualTo(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Add device without the name")
    public void createDeviceWithoutName() {
        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.nameField().click();
        devicePage.addBtnC().click();

        assertIsDisplayed(devicePage.addDeviceView());
        assertThat(devicePage.errorMessage().getText()).as("Text of warning message").isEqualTo(NAME_IS_REQUIRED_MESSAGE);
    }

    @Test(groups = "smoke")
    @Description("Create device only with spase in name")
    public void createDeviceWithOnlySpace() {
        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(" ");
        devicePage.addBtnC().click();

        assertIsDisplayed(devicePage.warningMessage());
        assertThat(devicePage.warningMessage().getText()).as("Text of warning message").isEqualTo(EMPTY_DEVICE_MESSAGE);
        assertIsDisplayed(devicePage.addDeviceView());
    }

    @Test(groups = "smoke")
    @Description("Create a device with the same name")
    public void createDeviceWithSameName() {
        Device device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();

        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(deviceName);
        devicePage.addBtnC().click();

        assertIsDisplayed(devicePage.warningMessage());
        assertThat(devicePage.warningMessage().getText()).as("Text of warning message").isEqualTo(SAME_NAME_WARNING_DEVICE_MESSAGE);
        assertIsDisplayed(devicePage.addDeviceView());
    }

    @Test(groups = "smoke")
    @Description("Add device after specifying the name (text/numbers /special characters) without refresh")
    public void createDeviceWithoutRefresh() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(deviceName);
        devicePage.addBtnC().click();

        assertIsDisplayed(devicePage.entity(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Add device without device profile")
    public void createDeviceWithoutDeviceProfile() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(deviceName);
        devicePage.clearProfileFieldBtn().click();
        devicePage.addBtnC().click();

        assertIsDisplayed(devicePage.errorMessage());
        assertThat(devicePage.errorMessage().getText()).as("Text of warning message").isEqualTo(DEVICE_PROFILE_IS_REQUIRED_MESSAGE);
        assertIsDisplayed(devicePage.addDeviceView());
    }

    @Test(groups = "smoke")
    @Description("Add device with enabled gateway")
    public void createDeviceWithEnableGateway() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(deviceName);
        devicePage.checkboxGatewayCreate().click();
        devicePage.addBtnC().click();

        assertIsDisplayed(devicePage.device(deviceName));
        assertIsDisplayed(devicePage.checkboxGatewayPage(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Add device with enabled overwrite activity time for connected")
    public void createDeviceWithEnableOverwriteActivityTimeForConnected() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(deviceName);
        devicePage.checkboxGatewayCreate().click();
        devicePage.checkboxOverwriteActivityTimeCreate().click();
        devicePage.addBtnC().click();
        devicePage.device(deviceName).click();

        assertThat(devicePage.checkboxOverwriteActivityTimeDetails().getAttribute("class").contains("selected"))
                .as("Overwrite activity time for connected is enable").isTrue();
    }

    @Test(groups = "smoke")
    @Description("Add device with label")
    public void createDeviceWithLabel() {
        deviceName = ENTITY_NAME + random();
        String deviceLabel = "device label " + random();

        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(deviceName);
        devicePage.enterLabel(deviceLabel);
        devicePage.addBtnC().click();

        assertIsDisplayed(devicePage.deviceLabelOnPage(deviceName));
        assertThat(devicePage.deviceLabelOnPage(deviceName).getText()).as("Label added correctly").isEqualTo(deviceLabel);
    }

    @Test(groups = "smoke")
    @Description("Add device with assignee on customer")
    public void createDeviceWithOwner() {
        deviceName = ENTITY_NAME + random();
        String customer = "Customer A";

        sideBarMenuView.goToAllDevices();
        devicePage.openCreateDeviceView();
        devicePage.enterName(deviceName);
        devicePage.changeOwnerOn(customer);
        devicePage.addBtnC().click();

        assertIsDisplayed(devicePage.deviceOwnerOnPage(deviceName));
        assertThat(devicePage.deviceOwnerOnPage(deviceName).getText())
                .as("Owner changed correctly").isEqualTo(customer);
    }

    @Test(groups = "smoke")
    @Description("Go to devices documentation page")
    public void documentation() {
        String urlPath = "docs/pe/user-guide/ui/devices/";

        sideBarMenuView.goToAllDevices();
        devicePage.entity("Thermostat T1").click();
        devicePage.goToHelpPage();

        assertThat(urlContains(urlPath)).as("Redirected URL contains " + urlPath).isTrue();
    }
}
