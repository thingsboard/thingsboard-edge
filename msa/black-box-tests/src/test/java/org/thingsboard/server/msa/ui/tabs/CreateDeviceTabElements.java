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

public class CreateDeviceTabElements extends AbstractBasePage {
    public CreateDeviceTabElements(WebDriver driver) {
        super(driver);
    }

    private static final String CREATE_DEVICE_NAME_FIELD = "//tb-device-wizard//input[@formcontrolname='name']";
    private static final String CREATE_NEW_DEVICE_PROFILE_RADIO_BTN = "//span[text() = 'Create new device profile']/ancestor::mat-radio-button";
    private static final String SELECT_EXISTING_DEVICE_PROFILE_RADIO_BTN = "//span[text() = 'Select existing device profile']/ancestor::mat-radio-button";
    private static final String DEVICE_PROFILE_TITLE_FIELD = "//input[@formcontrolname='newDeviceProfileTitle']";
    private static final String ADD_BTN = "//span[text() = 'Add']";
    private static final String CLEAR_PROFILE_FIELD_BTN = "//button[@aria-label='Clear']";
    private static final String ENTITY_FROM_DROPDOWN = "//div[@role = 'listbox']//span[text() = '%s']";
    private static final String ASSIGN_ON_CUSTOMER_FIELD = "//input[@formcontrolname='entity']";
    private static final String OWNER_AND_GROUPS_OPTION_BNT = "//div[text() = 'Owner and groups']/ancestor::mat-step-header";
    private static final String CUSTOMER_FROM_DROPDOWN = "//div[@role='listbox']/mat-option//span[contains(text(),'%s')]";
    private static final String DEVICE_LABEL_FIELD = "//tb-device-wizard//input[@formcontrolname='label']";
    private static final String CHECKBOX_GATEWAY = "//tb-device-wizard//mat-checkbox[@formcontrolname='gateway']//label";
    private static final String CHECKBOX_OVERWRITE_ACTIVITY_TIME = "//tb-device-wizard//mat-checkbox[@formcontrolname='overwriteActivityTime']//label";
    private static final String DESCRIPTION_FIELD = "//tb-device-wizard//textarea[@formcontrolname='description']";
    private static final String CLEAR_OWNER_FIELD_BTN = "//tb-owner-autocomplete//button[@aria-label='Clear']";

    public WebElement nameField() {
        return waitUntilElementToBeClickable(CREATE_DEVICE_NAME_FIELD);
    }

    public WebElement createNewDeviceProfileRadioBtn() {
        return waitUntilElementToBeClickable(CREATE_NEW_DEVICE_PROFILE_RADIO_BTN);
    }

    public WebElement selectExistingDeviceProfileRadioBtn() {
        return waitUntilElementToBeClickable(SELECT_EXISTING_DEVICE_PROFILE_RADIO_BTN);
    }

    public WebElement deviceProfileTitleField() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_TITLE_FIELD);
    }

    public WebElement addBtn() {
        return waitUntilElementToBeClickable(ADD_BTN);
    }

    public WebElement clearProfileFieldBtn() {
        return waitUntilElementToBeClickable(CLEAR_PROFILE_FIELD_BTN);
    }

    public WebElement entityFromDropdown(String customerTitle) {
        return waitUntilElementToBeClickable(String.format(ENTITY_FROM_DROPDOWN, customerTitle));
    }

    public WebElement assignOnCustomerField() {
        return waitUntilElementToBeClickable(ASSIGN_ON_CUSTOMER_FIELD);
    }

    public WebElement ownerAndGroupsOptionBtn() {
        return waitUntilElementToBeClickable(OWNER_AND_GROUPS_OPTION_BNT);
    }

    public WebElement customerFromDropDown(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_FROM_DROPDOWN, entityName));
    }

    public WebElement deviceLabelField() {
        return waitUntilElementToBeClickable(DEVICE_LABEL_FIELD);
    }

    public WebElement checkboxGateway() {
        return waitUntilElementToBeClickable(CHECKBOX_GATEWAY);
    }

    public WebElement checkboxOverwriteActivityTime() {
        return waitUntilElementToBeClickable(CHECKBOX_OVERWRITE_ACTIVITY_TIME);
    }

    public WebElement descriptionField() {
        return waitUntilElementToBeClickable(DESCRIPTION_FIELD);
    }

    public WebElement clearOwnerFieldBtn() {
        return waitUntilElementToBeClickable(CLEAR_OWNER_FIELD_BTN);
    }
}
