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
package org.thingsboard.server.msa.ui.tabs;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class ChangeDeviceOwnerTabElements extends AbstractBasePage {
    public ChangeDeviceOwnerTabElements(WebDriver driver) {
        super(driver);
    }

    private static final String CHANGE_OWNER_FIELD_FIELD = "//input[@formcontrolname='owner']";
    private static final String CUSTOMER_FROM_DROPDOWN = "//div[@role='listbox']/mat-option//b[contains(text(),'%s')]";
    private static final String CHANGE_OWNER_BTN = "//button[@type='submit']";
    private static final String YES_BTN_CONFIRM_CHANGE_OWNER = "//tb-confirm-dialog//span[text() = 'Yes']/parent::button";
    private static final String CLEAR_BTN = "//button[@aria-label='Clear']";
    private static final String ERROR_OWNER_REQUIRED = "//mat-error[contains(text(), 'Target owner is required')]";

    public WebElement changeOwnerField() {
        return waitUntilElementToBeClickable(CHANGE_OWNER_FIELD_FIELD);
    }

    public WebElement customerFromDropDown(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_FROM_DROPDOWN, entityName));
    }

    public WebElement changeOwnerBtn() {
        return waitUntilElementToBeClickable(CHANGE_OWNER_BTN);
    }

    public WebElement changeOwnerBtnVisible() {
        return waitUntilVisibilityOfElementLocated(CHANGE_OWNER_BTN);
    }

    public WebElement yesBtnConfirmChangeOwner() {
        return waitUntilElementToBeClickable(YES_BTN_CONFIRM_CHANGE_OWNER);
    }

    public WebElement clearBtn() {
        return waitUntilElementToBeClickable(CLEAR_BTN);
    }

    public WebElement errorOwnerRequired() {
        return waitUntilVisibilityOfElementLocated(ERROR_OWNER_REQUIRED);
    }
}
