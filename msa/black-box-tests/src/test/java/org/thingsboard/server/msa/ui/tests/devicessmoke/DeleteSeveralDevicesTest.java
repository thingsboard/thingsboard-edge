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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Delete several devices")
public class DeleteSeveralDevicesTest extends AbstractDeviceTest {

    private String deviceName1;
    private String deviceName2;

    @BeforeMethod
    public void createDevices() {
        Device device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        Device device1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName1 = device.getName();
        deviceName2 = device1.getName();
    }

    @AfterMethod
    public void deleteDevices() {
        deleteDeviceByName(deviceName1);
        deleteDeviceByName(deviceName2);
    }

    @Test(groups = "smoke")
    @Description("Remove several devices by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top")
    public void deleteSeveralDevicesByTopBtn() {
        sideBarMenuView.goToAllDevices();
        devicePage.deleteSelectedDevices(deviceName1, deviceName2);
        devicePage.refreshBtn().click();

        List.of(deviceName1, deviceName2)
                .forEach(d -> devicePage.assertEntityIsNotPresent(d));
    }

    @Test(groups = "smoke")
    @Description("Remove several devices by mark all the devices on the page by clicking in the topmost checkbox" +
            " and then clicking on the trash icon in the menu that appears")
    public void selectAllDevices() {
        sideBarMenuView.goToAllDevices();
        devicePage.selectAllCheckBox().click();
        devicePage.deleteSelectedBtn().click();

        assertIsDisplayed(devicePage.warningPopUpTitle());
        assertThat(devicePage.warningPopUpTitle().getText()).as("Warning title contains true correct of selected devices")
                .contains(String.valueOf(devicePage.markCheckbox().size()));
    }

    @Test(groups = "smoke")
    @Description("Remove several devices by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top without refresh")
    public void deleteSeveralWithoutRefresh() {
        sideBarMenuView.goToAllDevices();
        devicePage.deleteSelectedDevices(deviceName1, deviceName2);

        List.of(deviceName1, deviceName2)
                .forEach(d -> devicePage.assertEntityIsNotPresent(d));
    }
}
