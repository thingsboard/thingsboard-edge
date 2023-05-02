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

public class CreateWidgetPopupElements extends AbstractBasePage {
    public CreateWidgetPopupElements(WebDriver driver) {
        super(driver);
    }

    private static final String ENTITY_ALIAS = "//input[@formcontrolname='entityAlias']";
    private static final String CREATE_NEW_ALIAS_BTN = "//a[text() = 'Create a new one!']/parent::span";
    private static final String FILTER_TYPE_FIELD = "//div[contains(@class,'tb-entity-filter')]//mat-select//span";
    private static final String TYPE_FIELD = "//mat-select[@formcontrolname='entityType']//span";
    private static final String OPTION_FROM_DROPDOWN = "//span[text() = ' %s ']";
    private static final String ENTITY_FIELD = "//input[@formcontrolname='entity']";
    private static final String ADD_ALIAS_BTN = "//tb-entity-alias-dialog//span[text() = ' Add ']/parent::button";
    private static final String ADD_WIDGET_BTN = "//tb-add-widget-dialog//span[text() = ' Add ']/parent::button";
    private static final String ENTITY_FROM_DROPDOWN = "//b[text() = '%s']";

    public WebElement entityAlias() {
        return waitUntilElementToBeClickable(ENTITY_ALIAS);
    }

    public WebElement createNewAliasBtn() {
        return waitUntilElementToBeClickable(CREATE_NEW_ALIAS_BTN);
    }

    public WebElement filterTypeFiled() {
        return waitUntilElementToBeClickable(FILTER_TYPE_FIELD);
    }

    public WebElement typeFiled() {
        return waitUntilElementToBeClickable(TYPE_FIELD);
    }

    public WebElement optionFromDropdown(String type) {
        return waitUntilElementToBeClickable(String.format(OPTION_FROM_DROPDOWN, type));
    }

    public WebElement entityFiled() {
        return waitUntilElementToBeClickable(ENTITY_FIELD);
    }

    public WebElement addAliasBtn() {
        return waitUntilElementToBeClickable(ADD_ALIAS_BTN);
    }

    public WebElement addWidgetBtn() {
        return waitUntilElementToBeClickable(ADD_WIDGET_BTN);
    }

    public WebElement entityFromDropdown(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ENTITY_FROM_DROPDOWN, entityName));
    }
}
