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

public class CustomerPageElements extends OtherPageElementsHelper {
    public CustomerPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String CUSTOMER = "//mat-row//span[contains(text(),'%s')]";
    private static final String EMAIL = ENTITY + "/../..//mat-cell[contains(@class, 'mat-column-column2')]/span";
    private static final String COUNTRY = ENTITY + "/../..//mat-cell[contains(@class, 'mat-column-column3')]/span";
    private static final String CITY = ENTITY + "/../..//mat-cell[contains(@class, 'mat-column-column4')]/span";
    private static final String TITLES = "//mat-cell[contains(@class,'cdk-column-column1')]/span";
    protected static final String EDIT_MENU_DASHBOARD_FIELD = "//input[@formcontrolname='dashboard']";
    private static final String EDIT_MENU_DASHBOARD = "//div[@class='cdk-overlay-pane']//span/span";
    private static final String MANAGE_CUSTOMERS_USERS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),' account_circle')]";
    private static final String MANAGE_CUSTOMERS_ASSETS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),' domain')]/parent::button";
    private static final String MANAGE_CUSTOMERS_GROUPS_BTN = ENTITY + "/../..//mat-icon[contains(text(),'supervisor_account')]/../..";
    private static final String MANAGE_CUSTOMER_ENTITY_VIEW_BTN = ENTITY + "/../..//mat-icon[contains(text(),'view_quilt')]/../..";
    private static final String MANAGE_CUSTOMERS_DEVICES_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'devices_other')]/parent::button";
    private static final String MANAGE_CUSTOMERS_DASHBOARDS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'dashboard')]/parent::button";
    private static final String MANAGE_CUSTOMERS_EDGE_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'router')]/parent::button";
    private static final String ADD_USER_EMAIL = "//tb-add-user-dialog//input[@formcontrolname='email']";
    private static final String ACTIVATE_WINDOW_OK_BTN = "//span[contains(text(),'OK')]";
    private static final String USER_LOGIN_BTN = "//mat-icon[@data-mat-icon-name='login']";
    private static final String USERS_WIDGET = "//tb-widget";
    private static final String SELECT_COUNTRY_MENU = "//mat-form-field//mat-select[@formcontrolname='country']";
    private static final String COUNTRIES = "//span[@class='mdc-list-item__primary-text']";
    protected static final String INPUT_FIELD = "//input[@formcontrolname='%s']";
    protected static final String INPUT_FIELD_NAME_TITLE = "title";
    private static final String INPUT_FIELD_NAME_CITY = "city";
    private static final String INPUT_FIELD_NAME_STATE = "state";
    private static final String INPUT_FIELD_NAME_ZIP = "zip";
    private static final String INPUT_FIELD_NAME_ADDRESS = "address";
    private static final String INPUT_FIELD_NAME_ADDRESS2 = "address2";
    private static final String INPUT_FIELD_NAME_EMAIL = "email";
    private static final String INPUT_FIELD_NAME_NUMBER = "phoneNumber";
    private static final String INPUT_FIELD_NAME_ASSIGNED_LIST = "entity";
    private static final String ASSIGNED_BTN = "//button[@type='submit']";
    private static final String HIDE_HOME_DASHBOARD_TOOLBAR = "//mat-checkbox[@formcontrolname='homeDashboardHideToolbar']//label";
    private static final String FILTER_BTN = "//tb-filters-edit";
    private static final String TIME_BTN = "//tb-timewindow";
    private static final String CUSTOMER_ICON_HEADER = "//div[@class='tb-breadcrumb']/span[3]/span";
    private static final String CUSTOMER_USER_ICON_HEADER = "User groups";
    private static final String CUSTOMER_ASSETS_ICON_HEADER = "Assets";
    private static final String CUSTOMER_DEVICES_ICON_HEADER = "Devices";
    private static final String CUSTOMER_DASHBOARD_ICON_HEADER = "Dashboards";
    private static final String CUSTOMER_EDGE_ICON_HEADER = "edge instances";
    private static final String CUSTOMER_USER_ICON_HEAD = "(//mat-drawer-content//span[contains(@class,'tb-entity-table')])[1]";
    private static final String MANAGE_BTN_VIEW = "//span[contains(text(),'%s')]";
    private static final String MANAGE_CUSTOMERS_USER_GROUPS_BTN_VIEW = "Manage user groups";
    private static final String MANAGE_CUSTOMERS_ASSET_GROUP_BTN_VIEW = "Manage asset groups";
    private static final String MANAGE_CUSTOMERS_GROUP_BTN_VIEW = "Manage customer groups";
    private static final String MANAGE_CUSTOMER_ENTITY_VIEW_BTN_VIEW = "Manage entity view groups";
    private static final String MANAGE_CUSTOMERS_DEVICE_GROUPS_BTN_VIEW = "Manage device groups";
    private static final String MANAGE_CUSTOMERS_DASHBOARD_BTN_VIEW = "Manage dashboard groups";
    private static final String MANAGE_CUSTOMERS_EDGE_BTN_VIEW = "Manage edge groups";
    private static final String DELETE_FROM_VIEW_BTN = "//tb-customer//span[contains(text(),' Delete')]";
    private static final String HEADER_NAME_VIEW = "//span[text()='Customer details']/parent::div/div/span";

    public WebElement titleFieldAddEntityView() {
        return waitUntilElementToBeClickable(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_TITLE));
    }

    public WebElement titleFieldEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_TITLE));
    }

    public WebElement customer(String entityName) {
        return waitUntilElementToBeClickable(String.format(CUSTOMER, entityName));
    }

    public WebElement email(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(EMAIL, entityName));
    }

    public WebElement country(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(COUNTRY, entityName));
    }

    public WebElement city(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(CITY, entityName));
    }

    public List<WebElement> entityTitles() {
        return waitUntilVisibilityOfElementsLocated(TITLES);
    }

    public WebElement editMenuDashboardField() {
        return waitUntilVisibilityOfElementLocated(EDIT_MENU_DASHBOARD_FIELD);
    }

    public WebElement editMenuDashboard() {
        return waitUntilElementToBeClickable(EDIT_MENU_DASHBOARD);
    }

    public WebElement phoneNumberEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_NUMBER));
    }

    public WebElement phoneNumberAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_NUMBER));
    }

    public WebElement manageCustomersUserBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_USERS_BTN, title));
    }

    public WebElement manageCustomersAssetGroupsBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_ASSETS_BTN, title));
    }

    public WebElement manageCustomerGroupsBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_GROUPS_BTN, title));
    }

    public WebElement manageCustomerEntityViewBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMER_ENTITY_VIEW_BTN, title));
    }

    public WebElement manageCustomersDeviceGroupsBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_DEVICES_BTN, title));
    }

    public WebElement manageCustomersDashboardsBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_DASHBOARDS_BTN, title));
    }

    public WebElement manageCustomersEdgeGroupsBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_EDGE_BTN, title));
    }

    public WebElement addUserEmailField() {
        return waitUntilElementToBeClickable(ADD_USER_EMAIL);
    }

    public WebElement activateWindowOkBtn() {
        return waitUntilElementToBeClickable(ACTIVATE_WINDOW_OK_BTN);
    }

    public WebElement userLoginBtn() {
        return waitUntilElementToBeClickable(USER_LOGIN_BTN);
    }

    public WebElement usersWidget() {
        return waitUntilVisibilityOfElementLocated(USERS_WIDGET);
    }

    public WebElement countrySelectMenuEntityView() {
        return waitUntilElementToBeClickable(SELECT_COUNTRY_MENU);
    }

    public WebElement countrySelectMenuAddEntityView() {
        return waitUntilElementToBeClickable(ADD_ENTITY_VIEW + SELECT_COUNTRY_MENU);
    }

    public List<WebElement> countries() {
        return waitUntilElementsToBeClickable(COUNTRIES);
    }

    public WebElement cityEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_CITY));
    }

    public WebElement cityAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_CITY));
    }

    public WebElement stateEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_STATE));
    }

    public WebElement stateAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_STATE));
    }

    public WebElement zipEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ZIP));
    }

    public WebElement zipAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_ZIP));
    }

    public WebElement addressEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS));
    }

    public WebElement addressAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS));
    }

    public WebElement address2EntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS2));
    }

    public WebElement address2AddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS2));
    }

    public WebElement emailEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_EMAIL));
    }

    public WebElement emailAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_EMAIL));
    }

    public WebElement assignedField() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ASSIGNED_LIST));
    }

    public WebElement submitAssignedBtn() {
        return waitUntilElementToBeClickable(ASSIGNED_BTN);
    }

    public WebElement hideHomeDashboardToolbarCheckbox() {
        return waitUntilElementToBeClickable(HIDE_HOME_DASHBOARD_TOOLBAR);
    }

    public WebElement filterBtn() {
        return waitUntilVisibilityOfElementLocated(FILTER_BTN);
    }

    public WebElement timeBtn() {
        return waitUntilVisibilityOfElementLocated(TIME_BTN);
    }

    public WebElement customerUserIconHeader() {
        return waitUntilVisibilityOfElementLocated(CUSTOMER_ICON_HEADER);
    }

    public WebElement customerAssetsIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_ASSETS_ICON_HEADER));
    }

    public WebElement customerDevicesIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_DEVICES_ICON_HEADER));
    }

    public WebElement customerDashboardIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_DASHBOARD_ICON_HEADER));
    }

    public WebElement customerEdgeIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_EDGE_ICON_HEADER));
    }

    public WebElement customerManageWindowIconHead() {
        return waitUntilVisibilityOfElementLocated(CUSTOMER_USER_ICON_HEAD);
    }

    public WebElement manageCustomersUserGroupsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_USER_GROUPS_BTN_VIEW));
    }

    public WebElement manageCustomersAssetGroupsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_ASSET_GROUP_BTN_VIEW));
    }

    public WebElement manageCustomerGroupsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_GROUP_BTN_VIEW));
    }

    public WebElement manageEntityViewBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMER_ENTITY_VIEW_BTN_VIEW));
    }

    public WebElement manageCustomersDeviceGroupsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_DEVICE_GROUPS_BTN_VIEW));
    }

    public WebElement manageCustomersDashboardsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_DASHBOARD_BTN_VIEW));
    }

    public WebElement manageCustomersEdgeGroupsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_EDGE_BTN_VIEW));
    }

    public WebElement customerViewDeleteBtn() {
        return waitUntilElementToBeClickable(DELETE_FROM_VIEW_BTN);
    }

    public WebElement headerNameCustomerView() {
        return waitUntilVisibilityOfElementLocated(HEADER_NAME_VIEW);
    }
}
