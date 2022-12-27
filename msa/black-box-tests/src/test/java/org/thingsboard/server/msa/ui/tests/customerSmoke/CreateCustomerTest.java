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
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_CUSTOMER_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_CUSTOMER_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

public class CreateCustomerTest extends AbstractDriverBaseTest {

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
    public void createCustomer() {
        String customerName = ENTITY_NAME;

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(customerName);
        customerPage.addBtnC().click();
        this.customerName = customerName;
        customerPage.refreshBtn().click();

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertTrue(customerPage.customer(customerName).isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createCustomerWithFullInformation() {
        String customerName = ENTITY_NAME;
        String text = "Text";
        String email = "email@mail.com";
        String number = "12015550123";

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().click();
        customerPage.titleFieldAddEntityView().sendKeys(customerName);
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

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createCustomerAddAndRemovePhoneNumber() {
        String customerName = ENTITY_NAME;
        String number = "12015550123";

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(customerName);
        customerPage.phoneNumberAddEntityView().sendKeys(number);
        customerPage.phoneNumberAddEntityView().clear();
        customerPage.addBtnC().click();
        this.customerName = customerName;
        customerPage.entity(customerName).click();

        Assert.assertEquals(customerPage.phoneNumberEntityView().getAttribute("value"), "");
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createCustomerWithoutName() {
        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.plusBtn().click();

        Assert.assertFalse(customerPage.addBtnV().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createCustomerWithOnlySpace() {
        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(" ");
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_CUSTOMER_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityView());
        Assert.assertTrue(customerPage.addEntityView().isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createCustomerSameName() {
        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.setCustomerName();
        String customerName = customerPage.getCustomerName();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(customerName);
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_CUSTOMER_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityView());
        Assert.assertTrue(customerPage.addEntityView().isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createCustomerWithoutRefresh() {
        String customerName = ENTITY_NAME;

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(customerName);
        customerPage.addBtnC().click();
        this.customerName = customerName;

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertTrue(customerPage.customer(customerName).isDisplayed());
    }

    @Test(priority = 40, groups = "smoke")
    @Description
    public void documentation() {
        String urlPath = "/docs/pe/user-guide/ui/customers/";

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.setCustomerName();
        customerPage.customer(customerPage.getCustomerName()).click();
        customerPage.goToHelpPage();

        Assert.assertTrue(urlContains(urlPath));
    }
}