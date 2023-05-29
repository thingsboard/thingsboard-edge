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

import java.util.List;

public class DashboardPageElements extends OtherPageElementsHelper {
    public DashboardPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String TITLES = "//mat-cell[contains(@class,'cdk-column-column1')]/span";
    private static final String ASSIGNED_BTN = ENTITY + "/../..//mat-icon[contains(text(),' assignment_ind')]/../..";
    private static final String MANAGE_ASSIGNED_ENTITY_LIST_FIELD = "//input[@formcontrolname='entity']";
    private static final String MANAGE_ASSIGNED_ENTITY = "//mat-option//span[contains(text(),'%s')]";
    private static final String MANAGE_ASSIGNED_UPDATE_BTN = "//button[@type='submit']";
    private static final String OPEN_DASHBOARD_GROUP_BTN = "//mat-icon[contains(text(),'view_list')]";
    private static final String ALL_GROUP_NAMES = "//mat-icon[contains(text(),'check')]/ancestor::mat-row/mat-cell[contains(@class,'name')]/span";
    private static final String GROUPS_BTN = "//a[@href='/dashboards/groups']/span[@class='mdc-tab__content']";
    private static final String EDIT_BTN = "//mat-icon[text() = 'edit']/parent::button";
    private static final String ADD_BTN = "//tb-footer-fab-buttons//button";
    private static final String CREAT_NEW_DASHBOARD_BTN = "//mat-icon[text() = 'insert_drive_file']/parent::button";
    private static final String ALARM_WIDGET_BUNDLE = "//mat-card-title[text() = 'Alarm widgets']/ancestor::mat-card";
    private static final String ALARM_TABLE_WIDGET = "//img[@alt='Alarms table']/ancestor::mat-card";
    private static final String WIDGET_SE_CORNER = "//div[contains(@class,'handle-se')]";
    private static final String DONE_BTN = "//tb-footer-fab-buttons/following-sibling::button//mat-icon[text() = 'done']/parent::button";

    public List<WebElement> entityTitles() {
        return waitUntilVisibilityOfElementsLocated(TITLES);
    }

    public WebElement assignedBtn(String title) {
        return waitUntilElementToBeClickable(String.format(ASSIGNED_BTN, title));
    }

    public WebElement manageAssignedEntityListField() {
        return waitUntilElementToBeClickable(MANAGE_ASSIGNED_ENTITY_LIST_FIELD);
    }

    public WebElement manageAssignedEntity(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_ASSIGNED_ENTITY, title));
    }

    public WebElement manageAssignedUpdateBtn() {
        return waitUntilElementToBeClickable(MANAGE_ASSIGNED_UPDATE_BTN);
    }

    public List<WebElement> openDashboardCroupBtn() {
        return waitUntilElementsToBeClickable(OPEN_DASHBOARD_GROUP_BTN);
    }

    public List<WebElement> allGroupName() {
        return waitUntilElementsToBeClickable(ALL_GROUP_NAMES);
    }

    public WebElement groupsBtn() {
        return waitUntilElementToBeClickable(GROUPS_BTN);
    }


    public WebElement editBtn() {
        return waitUntilElementToBeClickable(EDIT_BTN);
    }

    public WebElement addBtn() {
        return waitUntilElementToBeClickable(ADD_BTN);
    }

    public WebElement createNewDashboardBtn() {
        return waitUntilElementToBeClickable(CREAT_NEW_DASHBOARD_BTN);
    }

    public WebElement alarmWidgetBundle() {
        return waitUntilElementToBeClickable(ALARM_WIDGET_BUNDLE);
    }

    public WebElement alarmTableWidget() {
        return waitUntilElementToBeClickable(ALARM_TABLE_WIDGET);
    }

    public WebElement widgetSECorner() {
        return waitUntilElementToBeClickable(WIDGET_SE_CORNER);
    }

    public WebElement doneBtn() {
        return waitUntilVisibilityOfElementLocated(DONE_BTN);
    }
}
