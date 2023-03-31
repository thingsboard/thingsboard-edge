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
import org.openqa.selenium.WebElement;

public class SideBarMenuViewHelper extends SideBarMenuViewElements {
    public SideBarMenuViewHelper(WebDriver driver) {
        super(driver);
    }

    public void openDeviceProfiles() {
        openProfilesDropdown();
        deviceProfileBtn().click();
    }

    public void openAssetProfiles() {
        openProfilesDropdown();
        assetProfileBtn().click();
    }

    public void goToAllCustomers() {
        customersBtn().click();
    }

    public void goToCustomerGroups() {
        goToAllCustomers();
        new CustomerPageHelper(driver).groupsBtn().click();
    }

    public void goToAllDevices() {
        openEntitiesDropdown();
        devicesBtn().click();
    }

    public void goToDeviceGroups() {
        goToAllDevices();
        new DevicePageElements(driver).groupsBtn().click();
    }

    public void goToAllAssets() {
        openEntitiesDropdown();
        assetsBtn().click();
    }

    public void goToAssetGroups() {
        goToAllAssets();
        new AssetPageElements(driver).groupsBtn().click();
    }

    public void goToAllDashboards() {
        dashboardsBtn().click();
    }

    public void goToDashboardGroups() {
        goToAllDashboards();
        new DashboardPageHelper(driver).groupsBtn().click();
    }

    public void goToRoles() {
        openSecurityDropdown();
        rolesBtn().click();
    }

    public void goToScheduler() {
        openAdvancedFeaturesDropdown();
        schedulerBtn().click();
    }

    public void openEntitiesDropdown() {
        if (entitiesDropdownIsClose()) {
            entitiesDropdown().click();
        }
    }

    public void openSecurityDropdown() {
        if (securityDropdownIsClose()) {
            securityDropdown().click();
        }
    }

    public void openProfilesDropdown() {
        if (profilesDropdownIsClose()) {
            profilesDropdown().click();
        }
    }

    public void openAdvancedFeaturesDropdown() {
        if (advancedFeaturesDropdownIsClose()) {
            advancedFeaturesDropdown().click();
        }
    }

    public boolean entitiesDropdownIsClose() {
        return dropdownIsClose(entitiesDropdown());
    }

    public boolean securityDropdownIsClose() {
        return dropdownIsClose(securityDropdown());
    }

    public boolean profilesDropdownIsClose() {
        return dropdownIsClose(profilesDropdown());
    }

    public boolean advancedFeaturesDropdownIsClose() {
        return dropdownIsClose(advancedFeaturesDropdown());
    }

    private boolean dropdownIsClose(WebElement dropdown) {
        return !dropdown.getAttribute("class").contains("tb-toggled");
    }
}
