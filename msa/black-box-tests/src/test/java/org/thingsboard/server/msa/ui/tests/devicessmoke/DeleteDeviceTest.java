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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Delete device")
public class DeleteDeviceTest extends AbstractDeviceTest {

    @BeforeMethod
    public void createDevice() {
        Device device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();
    }

    @Test(groups = "smoke")
    @Description("Remove the device by clicking on the trash icon in the right side of device")
    public void deleteDeviceByRightSideBtn() {
        sideBarMenuView.goToAllDevices();
        devicePage.deleteDeviceByRightSideBtn(deviceName);
        devicePage.refreshBtn().click();

        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Remove device by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void deleteSelectedDevice() {
        sideBarMenuView.goToAllDevices();
        devicePage.deleteSelected(deviceName);
        devicePage.refreshBtn().click();

        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Remove the device by clicking on the 'Delete device' btn in the entity view")
    public void deleteDeviceFromDetailsTab() {
        sideBarMenuView.goToAllDevices();
        devicePage.entity(deviceName).click();
        devicePage.deleteDeviceFromDetailsTab();
        devicePage.refreshBtn();

        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Remove the device by clicking on the trash icon in the right side of device without refresh")
    public void deleteDeviceWithoutRefresh() {
        sideBarMenuView.goToAllDevices();
        devicePage.deleteDeviceByRightSideBtn(deviceName);

        devicePage.assertEntityIsNotPresent(deviceName);
    }
}
