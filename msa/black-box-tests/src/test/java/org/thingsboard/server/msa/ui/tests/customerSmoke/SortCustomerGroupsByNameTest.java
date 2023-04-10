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
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

public class SortCustomerGroupsByNameTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String customerGroupName;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerGroupName != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerGroupName).getId());
            customerGroupName = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customer groups by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort customers 'UP'")
    public void specialCharacterUp(String name) {
        customerGroupName = name;
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.sortByNameBtn().click();
        customerPage.setEntityGroupName();

        Assert.assertEquals(customerPage.getEntityGroupName(), customerGroupName);
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customer groups by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort customers 'DOWN'")
    public void specialCharacterDown(String name) {
        customerGroupName = name;
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.sortByNameDown();
        customerPage.setEntityGroupName(customerPage.entityGroups().size() - 1);

        Assert.assertEquals(customerPage.getEntityGroupName(), customerGroupName);
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customer groups by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort customers 'UP'")
    public void allSortUp(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerSymbol, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customer, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerNumber, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.sortByNameBtn().click();
        customerPage.setEntityGroupName(0);
        String firstGroup = customerPage.getEntityGroupName();
        customerPage.setEntityGroupName(1);
        String secondGroup = customerPage.getEntityGroupName();
        customerPage.setEntityGroupName(2);
        String thirdGroup = customerPage.getEntityGroupName();

        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customer).getId());
        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerNumber).getId());
        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerSymbol).getId());

        Assert.assertEquals(firstGroup, customerSymbol);
        Assert.assertEquals(secondGroup, customerNumber);
        Assert.assertEquals(thirdGroup, customer);
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customer groups by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort customers 'DOWN'")
    public void allSortDown(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerSymbol, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customer, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerNumber, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        int lastIndex = customerPage.entityGroups().size() - 1;
        customerPage.sortByNameDown();
        customerPage.setEntityGroupName(lastIndex);
        String firstGroup = customerPage.getEntityGroupName();
        customerPage.setEntityGroupName(lastIndex - 1);
        String secondGroup = customerPage.getEntityGroupName();
        customerPage.setEntityGroupName(lastIndex - 2);
        String thirdGroup = customerPage.getEntityGroupName();

        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customer).getId());
        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerNumber).getId());
        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerSymbol).getId());

        Assert.assertEquals(firstGroup, customerSymbol);
        Assert.assertEquals(secondGroup, customerNumber);
        Assert.assertEquals(thirdGroup, customer);
    }
}
