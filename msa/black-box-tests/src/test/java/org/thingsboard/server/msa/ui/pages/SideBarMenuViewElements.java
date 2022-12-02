/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
    private static final String ALL_GROUPS = "/..//span[contains(text(),'All')]";
    private static final String CUSTOMER_GROUPS_BTN = "//mat-toolbar//a[@href='/customerGroups']";
    private static final String ALL_CUSTOMER_GROUPS_BTN = CUSTOMER_GROUPS_BTN + ALL_GROUPS;
    private static final String DASHBOARD_GROUPS_BTN = "//mat-toolbar//a[@href='/dashboardGroups']";
    private static final String ALL_DASHBOARD_GROUPS_BTN = DASHBOARD_GROUPS_BTN + ALL_GROUPS;

    public WebElement ruleChainsBtn() {
        return waitUntilElementToBeClickable(RULE_CHAINS_BTN);
    }

    public WebElement customerGroupsBtn() {
        return waitUntilElementToBeClickable(CUSTOMER_GROUPS_BTN);
    }

    public WebElement allCustomerGroupBtn() {
        customerGroupsBtn().click();
        return waitUntilElementToBeClickable(ALL_CUSTOMER_GROUPS_BTN);
    }

    public WebElement dashboardBtn() {
        waitUntilElementToBeClickable(DASHBOARD_GROUPS_BTN).click();
        return waitUntilElementToBeClickable(ALL_DASHBOARD_GROUPS_BTN);
    }
}