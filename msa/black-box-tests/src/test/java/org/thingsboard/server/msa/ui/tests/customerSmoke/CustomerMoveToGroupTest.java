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

public class CustomerMoveToGroupTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private final String title = ENTITY_NAME + random();
    private String groupName1;
    private String groupName2;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        testRestClient.deleteCustomer(getCustomerByName(title).getId());
        if (groupName1 != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, groupName1).getId());
            groupName1 = null;
        }
        if (groupName2 != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, groupName2).getId());
            groupName2 = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Move customer to group")
    public void addGroup() {
        String groupName1 = "group1" + random();
        String groupName2 = "group2" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName1, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName2, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName1).getId());
        this.groupName1 = groupName1;
        this.groupName2 = groupName2;

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.entity(groupName1).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        jsClick(customerPage.selectGroupViewExistField());
        customerPage.entityFromDropDown(groupName2).click();
        customerPage.selectGroupViewSubmitBtn().click();
        sideBarMenuView.customerGroupsBtn().click();
        customerPage.entity(groupName2).click();

        Assert.assertNotNull(customerPage.entity(title));
        Assert.assertTrue(customerPage.entity(title).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Move customer to group without select")
    public void moveToGroupWithoutSelect() {
        String groupName = "group" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.entity(groupName1).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();

        Assert.assertFalse(customerPage.selectGroupViewSubmitBtnVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create new customer group")
    public void createNewEntityGroup() {
        String groupName = "group1" + random();
        String newGroupName = "group2" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.entity(groupName1).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.selectGroupViewNewGroupField().sendKeys(newGroupName);
        customerPage.selectGroupViewSubmitBtn().click();
        groupName2 = newGroupName;
        sideBarMenuView.customerGroupsBtn().click();

        Assert.assertNotNull(customerPage.entity(newGroupName));
        Assert.assertTrue(customerPage.entity(newGroupName).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer's group without the name")
    public void createNewEntityGroupWithoutName() {
        String groupName = "group" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.entity(groupName).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();

        Assert.assertFalse(customerPage.selectGroupViewSubmitBtnVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create customer's group only with spase in name")
    public void createNewEntityGroupWithSpace() {
        String groupName = "group" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.entity(groupName).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.selectGroupViewNewGroupField().sendKeys(" ");
        customerPage.selectGroupViewSubmitBtn().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_GROUP_NAME_MESSAGE);
        Assert.assertNotNull(customerPage.addToEntityGroupView());
        Assert.assertTrue(customerPage.addToEntityGroupView().isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create a customer's group with the same name")
    public void addGroupWithSameName() {
        String groupName = "group" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.entity(groupName).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.enterText(customerPage.selectGroupViewNewGroupField(), groupName);
        customerPage.selectGroupViewSubmitBtn().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE);
        Assert.assertNotNull(customerPage.addToEntityGroupView());
        Assert.assertTrue(customerPage.addToEntityGroupView().isDisplayed());
    }
}
