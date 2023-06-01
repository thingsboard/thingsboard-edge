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
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class DevicePageHelper extends DevicePageElements {
    public DevicePageHelper(WebDriver driver) {
        super(driver);
    }

    private String description;
    private String label;

    public void openDeviceAlarms(String deviceName) {
        if (!deviceDetailsView().isDisplayed()) {
            device(deviceName).click();
        }
        deviceDetailsAlarmsBtn().click();
    }

    public void openCreateDeviceView() {
        plusBtn().click();
        addDeviceBtn().click();
    }

    public void deleteDeviceByRightSideBtn(String deviceName) {
        deleteBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    public void deleteDeviceFromDetailsTab() {
        deleteBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }

    public void setDescription() {
        scrollToElement(descriptionEntityView());
        description = descriptionEntityView().getAttribute("value");
    }

    public void setLabel() {
        label = deviceLabelDetailsField().getAttribute("value");
    }

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public void changeDeviceProfile(String deviceProfileName) {
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileName).click();
    }

    public void unassignedDeviceByRightSideBtn(String deviceName) {
        unassignBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    public void unassignedDeviceFromDetailsTab() {
        unassignBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }

    public void selectDevices(String... deviceNames) {
        for (String deviceName : deviceNames) {
            checkBox(deviceName).click();
        }
    }

    public void changeOwnerSelectedDevices(String... deviceNames) {
        selectDevices(deviceNames);
        changeOwnerDeviceBtn().click();
    }

    public void deleteSelectedDevices(String... deviceNames) {
        selectDevices(deviceNames);
        deleteSelectedBtn().click();
        warningPopUpYesBtn().click();
    }

    public void filterDeviceByDeviceProfile(String deviceProfileTitle) {
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileTitle).click();
        submitBtn().click();
    }

    public void filterDeviceByState(String state) {
        deviceStateSelect().click();
        entityFromDropdown(" " + state + " ").click();
        sleep(2); //wait until the action is counted
        submitBtn().click();
    }

    public void filterDeviceByDeviceProfileAndState(String deviceProfileTitle, String state) {
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileTitle).click();
        deviceStateSelect().click();
        entityFromDropdown(" " + state + " ").click();
        sleep(2); //wait until the action is counted
        submitBtn().click();
    }

    public void makeDevicePublicByRightSideBtn(String deviceName) {
        makeDevicePublicBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    public void makeDevicePublicFromDetailsTab() {
        makeDevicePublicBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }

    public void makeDevicePrivateByRightSideBtn(String deviceName) {
        makeDevicePrivateBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    public void makeDevicePrivateFromDetailsTab() {
        makeDevicePrivateBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }
}
