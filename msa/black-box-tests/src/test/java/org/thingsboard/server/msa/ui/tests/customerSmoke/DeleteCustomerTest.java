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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class DeleteCustomerTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private RuleChainsPageHelper ruleChainsPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the customer by clicking on the trash icon in the right side of refresh")
    public void removeCustomerByRightSideBtn() {
        String customer = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customer));

        sideBarMenuView.goToAllCustomers();
        String deletedCustomer = customerPage.deleteRuleChainTrash(customer);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.entityIsNotPresent(deletedCustomer));
    }

    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove customer by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedCustomer() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));

        sideBarMenuView.goToAllCustomers();
        String deletedCustomer = customerPage.deleteSelected(customerName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedCustomer));
    }

    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the customer by clicking on the 'Delete customer' btn in the entity view")
    public void removeFromCustomerView() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));

        sideBarMenuView.goToAllCustomers();
        customerPage.entity(customerName).click();
        jsClick(customerPage.customerViewDeleteBtn());
        customerPage.warningPopUpYesBtn().click();
        jsClick(customerPage.refreshBtn());

        Assert.assertTrue(customerPage.entityIsNotPresent(customerName));
    }

    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the customer by clicking on the trash icon in the right side of customer without refresh")
    public void removeCustomerByRightSideBtnWithoutRefresh() {
        String customer = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customer));

        sideBarMenuView.goToAllCustomers();
        String deletedCustomer = customerPage.deleteRuleChainTrash(customer);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.entityIsNotPresent(deletedCustomer));
    }
}
