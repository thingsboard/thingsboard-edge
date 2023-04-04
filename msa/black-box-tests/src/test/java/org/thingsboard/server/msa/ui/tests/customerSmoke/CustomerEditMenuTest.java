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
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_CUSTOMER_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PHONE_NUMBER_ERROR_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class CustomerEditMenuTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private LoginPageHelper loginPage;
    private CustomerPageHelper customerPage;
    private String customerName;

    @BeforeClass
    public void login() {
        loginPage = new LoginPageHelper(driver);
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        loginPage.authorizationTenant();
    }

    @AfterMethod
    public void delete() {
        if (customerName != null) {
            testRestClient.deleteCustomer(getCustomerByName(customerName).getId());
            customerName = null;
        }
    }

    @BeforeMethod
    public void reLogin() {
        if (getJwtTokenFromLocalStorage() == null) {
            loginPage.authorizationTenant();
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 10, groups = "smoke")
    @Description("Change title by edit menu")
    public void changeTitle() {
        String newCustomerName = "Changed" + getRandomNumber();
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomers();
        customerPage.entity(customerName).click();
        customerPage.setCustomerHeaderName();
        String titleBefore = customerPage.getCustomerHeaderName();
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(newCustomerName);
        customerPage.doneBtnEditView().click();
        this.customerName = newCustomerName;
        customerPage.setCustomerHeaderName();
        String titleAfter = customerPage.getCustomerHeaderName();

        Assert.assertNotEquals(titleBefore, titleAfter);
        Assert.assertEquals(titleAfter, newCustomerName);
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Delete title and save")
    public void deleteTitle() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomers();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.titleFieldEntityView().clear();

        Assert.assertFalse(customerPage.doneBtnEditViewVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space in title")
    public void saveOnlyWithSpace() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomers();
        customerPage.setCustomerName();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(" ");
        customerPage.doneBtnEditView().click();
        customerPage.setCustomerHeaderName();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_CUSTOMER_MESSAGE);
        Assert.assertEquals(customerPage.getCustomerName(), customerPage.getCustomerHeaderName());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        String name = ENTITY_NAME + random();
        testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(name, description));
        customerName = name;

        sideBarMenuView.goToAllCustomers();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(newDescription);
        customerPage.doneBtnEditView().click();
        customerPage.setDescription();

        Assert.assertEquals(customerPage.getDescription(), finalDescription);
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add country")
    public void addCountry() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomers();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.selectCountryEntityView();
        customerPage.doneBtnEditView().click();

        Assert.assertEquals(customerPage.countrySelectMenuEntityView().getText(), customerPage.getCountry());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add phone number")
    public void addPhoneNumber() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;
        String number = "2015550123";

        sideBarMenuView.goToAllCustomers();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.enterPhoneNumber(number);

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").contains(number));
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "incorrectPhoneNumber")
    @Description("Add incorrect phone number")
    public void addIncorrectPhoneNumber(String number) {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomers();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.enterPhoneNumber(number);

        Assert.assertFalse(customerPage.doneBtnIsEnable());
        Assert.assertNotNull(customerPage.errorMessage());
        Assert.assertTrue(customerPage.errorMessage().isDisplayed());
        Assert.assertEquals(customerPage.errorMessage().getText(), PHONE_NUMBER_ERROR_MESSAGE);
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 30, groups = "smoke")
    @Description("Add all information")
    public void addAllInformation() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;
        String text = "Text";
        String email = "email@mail.com";
        String number = "2015550123";

        sideBarMenuView.goToAllCustomers();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.selectCountryEntityView();
        customerPage.descriptionEntityView().sendKeys(text);
        customerPage.cityEntityView().sendKeys(text);
        customerPage.stateEntityView().sendKeys(text);
        customerPage.zipEntityView().sendKeys(text);
        customerPage.addressEntityView().sendKeys(text);
        customerPage.address2EntityView().sendKeys(text);
        customerPage.enterPhoneNumber(number);
        customerPage.emailEntityView().sendKeys(email);
        customerPage.doneBtnEditView().click();

        Assert.assertEquals(customerPage.countrySelectMenuEntityView().getText(), customerPage.getCountry());
        Assert.assertEquals(customerPage.descriptionEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.cityEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.stateEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.zipEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.addressEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.address2EntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.phoneNumberEntityView().getAttribute("value"), "+1" + number);
        Assert.assertEquals(customerPage.emailEntityView().getAttribute("value"), email);
    }
}