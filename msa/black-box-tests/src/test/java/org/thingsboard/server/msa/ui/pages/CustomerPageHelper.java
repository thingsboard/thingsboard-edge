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

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

@Slf4j
public class CustomerPageHelper extends CustomerPageElements {
    public CustomerPageHelper(WebDriver driver) {
        super(driver);
    }

    private String customerName;
    private String country;
    private String dashboard;
    private String dashboardFromView;
    private String description;
    private String customerEmail;
    private String customerCountry;
    private String customerCity;
    private String headerName;

    public void setCustomerHeaderName() {
        this.headerName = headerNameCustomerView().getText();
    }

    public String getCustomerHeaderName() {
        return headerName;
    }

    public void setCustomerName() {
        this.customerName = entityTitles().get(0).getText();
    }

    public void setCustomerName(int number) {
        this.customerName = entityTitles().get(number).getText();
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCountry() {
        this.country = countries().get(0).getText();
    }

    public String getCountry() {
        return country;
    }

    public void setDashboard() {
        this.dashboard = listOfEntity().get(0).getText();
    }

    public void setDashboardFromView() {
        this.dashboardFromView = editMenuDashboardField().getAttribute("value");
    }

    public void setDescription() {
        scrollToElement(descriptionEntityView());
        this.description = descriptionEntityView().getAttribute("value");
    }

    public String getDashboard() {
        return dashboard;
    }

    public String getDashboardFromView() {
        return dashboardFromView;
    }

    public String getDescription() {
        return description;
    }

    public void setCustomerEmail(String title) {
        this.customerEmail = email(title).getText();
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerCountry(String title) {
        this.customerCountry = country(title).getText();
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public void setCustomerCity(String title) {
        this.customerCity = city(title).getText();
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void changeTitleEditMenu(String newTitle) {
        titleFieldEntityView().click();
        titleFieldEntityView().clear();
        wait.until(ExpectedConditions.textToBe(By.xpath(String.format(INPUT_FIELD, INPUT_FIELD_NAME_TITLE)), ""));
        titleFieldEntityView().sendKeys(newTitle);
    }

    public void chooseDashboard(String dashboardName) {
        editMenuDashboardField().click();
        editMenuDashboard(dashboardName).click();
    }

    public void createCustomersUser() {
        plusBtn().click();
        addUserEmailField().sendKeys(getRandomNumber() + "@gmail.com");
        addBtnC().click();
        activateWindowOkBtn().click();
    }

    public void selectCountryEntityView() {
        countrySelectMenuEntityView().click();
        setCountry();
        countries().get(0).click();
    }

    public void selectCountryAddEntityView() {
        countrySelectMenuAddEntityView().click();
        setCountry();
        countries().get(0).click();
    }

    public void assignedDashboard() {
        new DashboardPageElements(driver).openDashboardCroupBtn().get(0).click();
        plusBtn().click();
        assignedField().click();
        setDashboard();
        listOfEntity().get(0).click();
        submitAssignedBtn().click();
    }

    public boolean customerIsNotPresent(String title) {
        return elementsIsNotPresent(getEntity(title));
    }

    public void sortByTitleDown() {
        doubleClick(sortByTitleBtn());
    }

    public void addCustomerViewEnterName(CharSequence keysToEnter) {
        enterText(titleFieldAddEntityView(), keysToEnter);
    }

    public boolean doneBtnIsEnable() {
        waitUntilAttributeContains(doneBtnEditViewVisible(), "disabled", "true");
        return doneBtnEditViewVisible().isEnabled();
    }

    public void enterPhoneNumber(String number) {
        phoneNumberEntityView().click();
        phoneNumberEntityView().sendKeys(number);
        phoneNumberEntityView().sendKeys(Keys.TAB);
    }

    public void waitUntilCustomerNotVisible(String customerName) {
        waitUntilInvisibilityOfElementLocated(entity(customerName));
    }
}
