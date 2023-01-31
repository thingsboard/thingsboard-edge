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

import java.util.List;

public class RuleChainsPageElements extends OtherPageElementsHelper {
    public RuleChainsPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String MAKE_ROOT_BTN = ENTITY + "/../..//mat-icon[contains(text(),' flag')]/../..";
    private static final String ROOT = ENTITY + "/../..//mat-icon[text() = 'check_box']";
    private static final String ROOT_DISABLE = ENTITY + "/../..//mat-icon[text() = 'check_box_outline_blank']";
    private static final String CREATED_TIME = ENTITY + "/../..//mat-cell/span[contains(text(),'%s')]";
    private static final String CREATE_RULE_CHAIN_BTN = "//span[contains(text(),'Create new rule chain')]";
    private static final String CREATE_RULE_CHAIN_NAME_FIELD = "//form[@class='ng-untouched ng-pristine ng-invalid']//input[@formcontrolname='name']";
    private static final String RULE_CHAINS_NAMES_WITHOUT_ROOT = "//mat-icon[contains(text(),'check_box_outline_blank')]/../../../mat-cell[contains(@class,'name')]/span";
    private static final String DELETE_RULE_CHAIN_FROM_VIEW_BTN = "//span[contains(text(),' Delete')]";
    private static final String IMPORT_RULE_CHAIN_BTN = "//span[contains(text(),'Import rule chain')]";
    private static final String OPEN_RULE_CHAIN_FROM_VIEW = "//span[contains(text(),'Open rule chain')]";
    private static final String MAKE_ROOT_FROM_VIEW = "(//span[contains(text(),' Make rule chain root ')]/..)[1]";
    private static final String ROOT_ACTIVE_CHECKBOXES = "//mat-icon[text() = 'check_box']";
    private static final String ALL_NAMES = "//mat-icon[contains(text(),'check')]/../../../mat-cell[contains(@class,'name')]/span";
    private static final String HEADER_NAME_VIEW = "//header//div[@class='tb-details-title']/span";
    private static final String EDIT_PENCIL_BTN = "//tb-entity-details-panel//mat-icon[contains(text(),'edit')]/ancestor::button";
    private static final String DONE_BTN_EDIT_VIEW = "//mat-icon[contains(text(),'done')]/ancestor::button";

    public String getDeleteRuleChainFromViewBtn() {
        return DELETE_RULE_CHAIN_FROM_VIEW_BTN;
    }

    public WebElement makeRootBtn(String entityName) {
        return waitUntilElementToBeClickable(String.format(MAKE_ROOT_BTN, entityName));
    }

    public List<WebElement> rootCheckBoxesEnable() {
        return waitUntilVisibilityOfElementsLocated(ROOT_ACTIVE_CHECKBOXES);
    }

    public WebElement rootCheckBoxEnable(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ROOT, entityName));
    }

    public WebElement rootCheckBoxDisable(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ROOT_DISABLE, entityName));
    }

    public WebElement createRuleChainBtn() {
        return waitUntilElementToBeClickable(CREATE_RULE_CHAIN_BTN);
    }

    public WebElement importRuleChainBtn() {
        return waitUntilElementToBeClickable(IMPORT_RULE_CHAIN_BTN);
    }

    public WebElement nameField() {
        return waitUntilElementToBeClickable(CREATE_RULE_CHAIN_NAME_FIELD);
    }

    public List<WebElement> notRootRuleChainsNames() {
        return waitUntilVisibilityOfElementsLocated(RULE_CHAINS_NAMES_WITHOUT_ROOT);
    }

    public WebElement deleteBtnFromView() {
        return waitUntilElementToBeClickable(DELETE_RULE_CHAIN_FROM_VIEW_BTN);
    }

    public WebElement openRuleChainFromViewBtn() {
        return waitUntilElementToBeClickable(OPEN_RULE_CHAIN_FROM_VIEW);
    }

    public List<WebElement> entities(String name) {
        return waitUntilVisibilityOfElementsLocated(String.format(ENTITY, name));
    }

    public WebElement makeRootFromViewBtn() {
        return waitUntilElementToBeClickable(MAKE_ROOT_FROM_VIEW);
    }

    public List<WebElement> allNames() {
        return waitUntilVisibilityOfElementsLocated(ALL_NAMES);
    }

    public WebElement createdTimeEntity(String name, String time) {
        return waitUntilElementToBeClickable(String.format(CREATED_TIME, name, time));
    }

    public WebElement ruleChainViewHeaderName() {
        return waitUntilVisibilityOfElementLocated(HEADER_NAME_VIEW);
    }

    public WebElement editPencilRuleChainViewBtn() {
        waitUntilVisibilityOfElementsLocated(EDIT_PENCIL_BTN);
        return waitUntilElementToBeClickable(EDIT_PENCIL_BTN);
    }

    public WebElement doneBtnEditRuleChainView() {
        return waitUntilElementToBeClickable(DONE_BTN_EDIT_VIEW);
    }

    public WebElement doneBtnEditRuleChainViewVisible() {
        return waitUntilVisibilityOfElementLocated(DONE_BTN_EDIT_VIEW);
    }
}
