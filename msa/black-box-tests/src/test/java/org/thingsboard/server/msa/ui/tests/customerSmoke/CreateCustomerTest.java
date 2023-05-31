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
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_CUSTOMER_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_CUSTOMER_MESSAGE;

public class CreateCustomerTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String customerName;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerName != null) {
            testRestClient.deleteCustomer(getCustomerByName(customerName).getId());
            customerName = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer specifying the name (text/numbers /special characters)")
    public void createCustomer() {
        String customerName = ENTITY_NAME + random();

        sideBarMenuView.goToAllCustomers();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.addBtnC().click();
        this.customerName = customerName;
        customerPage.refreshBtn().click();

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertTrue(customerPage.customer(customerName).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add customer after specifying the name (text/numbers /special characters) with full information")
    public void createCustomerWithFullInformation() {
        String customerName = ENTITY_NAME + random();
        String text = "Text";
        String email = "email@mail.com";
        String number = "12015550123";

        sideBarMenuView.goToAllCustomers();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.selectCountryAddEntityView();
        customerPage.descriptionAddEntityView().sendKeys(text);
        customerPage.cityAddEntityView().sendKeys(text);
        customerPage.stateAddEntityView().sendKeys(text);
        customerPage.zipAddEntityView().sendKeys(text);
        customerPage.addressAddEntityView().sendKeys(text);
        customerPage.address2AddEntityView().sendKeys(text);
        customerPage.phoneNumberAddEntityView().sendKeys(number);
        customerPage.emailAddEntityView().sendKeys(email);
        customerPage.addBtnC().click();
        this.customerName = customerName;
        customerPage.setCustomerEmail(customerName);
        customerPage.setCustomerCountry(customerName);
        customerPage.setCustomerCity(customerName);
        customerPage.entity(customerName).click();

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertEquals(customerPage.entityViewTitle().getText(), customerName);
        Assert.assertEquals(customerPage.titleFieldEntityView().getAttribute("value"), customerName);
        Assert.assertEquals(customerPage.countrySelectMenuEntityView().getText(), customerPage.getCountry());
        Assert.assertEquals(customerPage.descriptionEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.cityEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.stateEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.zipEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.addressEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.address2EntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.phoneNumberEntityView().getAttribute("value"), "+" + number);
        Assert.assertEquals(customerPage.emailEntityView().getAttribute("value"), email);
        Assert.assertEquals(customerPage.getCustomerEmail(), email);
        Assert.assertEquals(customerPage.getCustomerCountry(), customerPage.getCountry());
        Assert.assertEquals(customerPage.getCustomerCity(), text);
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add customer without the name")
    public void createCustomerWithoutName() {
        sideBarMenuView.goToAllCustomers();
        customerPage.plusBtn().click();
        customerPage.addBtnC().click();

        Assert.assertTrue(customerPage.addEntityView().isDisplayed(), "Add entity view steel open");
        Assert.assertEquals(customerPage.errorMessage().getText(), "Title is required.", "Error message");
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Create customer only with spase in name")
    public void createCustomerWithOnlySpace() {
        sideBarMenuView.goToAllCustomers();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(Keys.SPACE);
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_CUSTOMER_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityView());
        Assert.assertTrue(customerPage.addEntityView().isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Create a customer with the same name")
    public void createCustomerSameName() {
        sideBarMenuView.goToAllCustomers();
        customerPage.setCustomerName();
        String customerName = customerPage.getCustomerName();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_CUSTOMER_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityView());
        Assert.assertTrue(customerPage.addEntityView().isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add customer specifying the name (text/numbers /special characters) without refresh")
    public void createCustomerWithoutRefresh() {
        String customerName = ENTITY_NAME + random();

        sideBarMenuView.goToAllCustomers();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.addBtnC().click();
        this.customerName = customerName;

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertTrue(customerPage.customer(customerName).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 40, groups = "smoke")
    @Description("Go to customer documentation page")
    public void documentation() {
        String urlPath = "/docs/pe/user-guide/ui/customers/";

        sideBarMenuView.goToAllCustomers();
        customerPage.setCustomerName();
        customerPage.customer(customerPage.getCustomerName()).click();
        customerPage.goToHelpPage();

        Assert.assertTrue(urlContains(urlPath), "URL contains " + urlPath);
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(groups = "smoke")
    @Description("Go to customer documentation page")
    public void createCustomerAddAndRemovePhoneNumber() {
        String customerName = ENTITY_NAME;
        String number = "12015550123";

        sideBarMenuView.goToAllCustomers();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.enterText(customerPage.phoneNumberAddEntityView(), number);
        customerPage.clearInputField(customerPage.phoneNumberAddEntityView());
        customerPage.addBtnC().click();
        this.customerName = customerName;
        customerPage.entity(customerName).click();

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").isEmpty(), "Phone field is empty");
    }
}
