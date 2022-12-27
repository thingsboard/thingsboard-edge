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
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_CUSTOMER_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PHONE_NUMBER_ERROR_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class CustomerEditMenuTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String customerName;

    @BeforeMethod
    public void login() {
        openLocalhost();
        new LoginPageHelper(driver).authorizationTenant();
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerName != null) {
            testRestClient.deleteCustomer(getCustomerByName(customerName).getId());
            customerName = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void changeTitle() {
        String newCustomerName = "Changed" + getRandomNumber();
        String customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomerGroupBtn();
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

    @Test(priority = 20, groups = "smoke")
    @Description
    public void deleteTitle() {
        String customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.titleFieldEntityView().clear();

        Assert.assertFalse(customerPage.doneBtnEditViewVisible().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void saveOnlyWithSpace() {
        String customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomerGroupBtn();
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

    @Test(priority = 20, groups = "smoke")
    @Description
    public void editDescription() {
        String customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;
        String description = "Description";

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(description);
        customerPage.doneBtnEditView().click();
        String description1 = customerPage.descriptionEntityView().getAttribute("value");
        customerPage.editPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(description);
        customerPage.doneBtnEditView().click();
        String description2 = customerPage.descriptionEntityView().getAttribute("value");
        customerPage.editPencilBtn().click();
        customerPage.changeDescription("");
        customerPage.doneBtnEditView().click();

        Assert.assertEquals(description, description1);
        Assert.assertEquals(description + description, description2);
        Assert.assertTrue(customerPage.descriptionEntityView().getAttribute("value").isEmpty());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void addCountry(){
        String customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.selectCountryEntityView();
        customerPage.doneBtnEditView().click();

        Assert.assertEquals(customerPage.countrySelectMenuEntityView().getText(), customerPage.getCountry());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void addPhoneNumber() {
        String customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;
        String number = "2015550123";

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.enterPhoneNumber(number);
        customerPage.doneBtnEditView().click();

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").contains(number));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void deletePhoneNumber() {
        String customerName = ENTITY_NAME;
        String number = "+12015550123";
        testRestClient.postCustomer(defaultCustomerPrototype(customerName, number));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.phoneNumberEntityView().click();
        customerPage.phoneNumberEntityView().sendKeys(Keys.CONTROL + "A" + Keys.BACK_SPACE);
        customerPage.doneBtnEditView().click();

        Assert.assertEquals(customerPage.phoneNumberEntityView().getAttribute("value"), "");
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "incorrectPhoneNumber")
    @Description
    public void addIncorrectPhoneNumber(String number) {
        String customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.enterPhoneNumber(number);
        customerPage.doneBtnEditViewVisible().click();

        Assert.assertFalse(customerPage.doneBtnIsEnable());
        Assert.assertNotNull(customerPage.errorMessage());
        Assert.assertTrue(customerPage.errorMessage().isDisplayed());
        Assert.assertEquals(customerPage.errorMessage().getText(), PHONE_NUMBER_ERROR_MESSAGE);
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void addAllInformation() {
        String customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;
        String text = "Text";
        String email = "email@mail.com";
        String number = "2015550123";

        sideBarMenuView.goToAllCustomerGroupBtn();
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