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
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class SideBarMenuViewElements extends AbstractBasePage {
    public SideBarMenuViewElements(WebDriver driver) {
        super(driver);
    }

    private static final String RULE_CHAINS_BTN = "//mat-toolbar//a[@href='/ruleChains']";
    private static final String CUSTOMERS_BTN = "//mat-toolbar//a[@href='/customers']";
    private static final String DASHBOARD_BTN = "//mat-toolbar//a[@href='/dashboards']";
    private static final String PROFILES_DROPDOWN = "//mat-toolbar//mat-icon[text()='badge']/ancestor::span//span[contains(@class,'pull-right')]";
    private static final String DEVICE_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/deviceProfiles']";
    private static final String ASSET_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/assetProfiles']";
    private static final String DASHBOARDS_BTN = "//mat-toolbar//a[@href='/dashboards']";
    private static final String SOLUTION_TEMPLATES = "//mat-toolbar//a[@href='/solutionTemplates']";
    private static final String ROLES_BTN = "//mat-toolbar//a[@href='/security-settings/roles']/span";
    private static final String DEVICES_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Devices']";
    private static final String ASSETS_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Assets']";
    private static final String ENTITIES_DROPDOWN = "//mat-toolbar//mat-icon[text()='category']/ancestor::span//span[contains(@class,'pull-right')]";
    private static final String SECURITY_DROPDOWN = "//mat-toolbar//mat-icon[text()='security']/ancestor::span//span[contains(@class,'pull-right')]";
    private static final String ADVANCED_FEATURES_DROPDOWN = "//mat-toolbar//mat-icon[text()='construction']/ancestor::span//span[contains(@class,'pull-right')]";
    private static final String SCHEDULER_BTN = "//mat-toolbar//span[text()='Scheduler']";

    public WebElement ruleChainsBtn() {
        return waitUntilElementToBeClickable(RULE_CHAINS_BTN);
    }

    public WebElement customersBtn() {
        return waitUntilElementToBeClickable(CUSTOMERS_BTN);
    }

    public WebElement profilesDropdown() {
        return waitUntilElementToBeClickable(PROFILES_DROPDOWN);
    }

    public WebElement deviceProfileBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_BTN);
    }

    public WebElement assetProfileBtn() {
        return waitUntilElementToBeClickable(ASSET_PROFILE_BTN);
    }

    public WebElement dashboardsBtn() {
        return waitUntilElementToBeClickable(DASHBOARDS_BTN);
    }

    public WebElement solutionTemplates() {
        return waitUntilElementToBeClickable(SOLUTION_TEMPLATES);
    }

    public WebElement rolesBtn() {
        return waitUntilElementToBeClickable(ROLES_BTN);
    }

    public WebElement devicesBtn() {
        return waitUntilElementToBeClickable(DEVICES_BTN);
    }

    public WebElement assetsBtn() {
        return waitUntilElementToBeClickable(ASSETS_BTN);
    }

    public WebElement entitiesDropdown() {
        return waitUntilElementToBeClickable(ENTITIES_DROPDOWN);
    }

    public WebElement securityDropdown() {
        return waitUntilElementToBeClickable(SECURITY_DROPDOWN);
    }

    public WebElement advancedFeaturesDropdown() {
        return waitUntilElementToBeClickable(ADVANCED_FEATURES_DROPDOWN);
    }

    public WebElement schedulerBtn() {
        return waitUntilElementToBeClickable(SCHEDULER_BTN);
    }
}
