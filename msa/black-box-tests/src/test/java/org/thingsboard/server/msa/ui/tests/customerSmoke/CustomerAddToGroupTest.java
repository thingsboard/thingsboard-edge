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
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_GROUP_NAME_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultEntityGroupPrototype;

public class CustomerAddToGroupTest extends AbstractDriverBaseTest {
    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String title;
    private String name;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod()
    public void delete() {
        testRestClient.deleteCustomer(getCustomerByName(title).getId());
        if (name != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, name).getId());
            name = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer specifying to group")
    public void addGroup() {
        name = ENTITY_NAME + random() + '1';
        title = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(name, EntityType.CUSTOMER));

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        jsClick(customerPage.selectGroupViewExistField());
        customerPage.entityFromDropDown(name).click();
        customerPage.selectGroupViewSubmitBtn().click();
        sideBarMenuView.customerGroupsBtn().click();
        customerPage.entity(name).click();

        Assert.assertNotNull(customerPage.entity(title));
        Assert.assertTrue(customerPage.entity(title).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer specifying to group without select group")
    public void addGroupWithoutSelect() {
        title = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();

        Assert.assertFalse(customerPage.selectGroupViewSubmitBtnVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer's group specifying the name (text/numbers /special characters)")
    public void createNewEntityGroup() {
        title = ENTITY_NAME + random();
        String groupName = title + '1';
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.enterText(customerPage.selectGroupViewNewGroupField(), groupName);
        customerPage.selectGroupViewSubmitBtn().click();
        name = groupName;
        sideBarMenuView.customerGroupsBtn().click();

        Assert.assertNotNull(customerPage.entity(groupName));
        Assert.assertTrue(customerPage.entity(groupName).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer's group without the name")
    public void createNewEntityGroupWithoutName() {
        title = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();

        Assert.assertFalse(customerPage.selectGroupViewSubmitBtnVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create customer's group only with spase in name")
    public void createNewEntityGroupWithSpace() {
        title = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.enterText(customerPage.selectGroupViewNewGroupField(), " ");
        customerPage.selectGroupViewSubmitBtn().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_GROUP_NAME_MESSAGE);
        Assert.assertNotNull(customerPage.addToEntityGroupView());
        Assert.assertTrue(customerPage.addToEntityGroupView().isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create a customer's group with the same name")
    public void addGroupWithSameName() {
        title = ENTITY_NAME + random();
        name = ENTITY_NAME + random() + '1';
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(name, EntityType.CUSTOMER));

        sideBarMenuView.goToAllCustomerGroupBtn();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.enterText(customerPage.selectGroupViewNewGroupField(), name);
        customerPage.selectGroupViewSubmitBtn().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE);
        Assert.assertNotNull(customerPage.addToEntityGroupView());
        Assert.assertTrue(customerPage.addToEntityGroupView().isDisplayed());
    }
}