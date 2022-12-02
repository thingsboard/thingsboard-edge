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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_GROUP_NAME_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

public class CreateCustomerEntityGroupTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String customerGroupName;

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
        if (customerGroupName != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerGroupName).getId());
            customerGroupName = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void createCustomerGroup() {
        customerGroupName = ENTITY_NAME;

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.plusBtn().click();
        customerPage.addEntityGroupViewNameField().sendKeys(customerGroupName);
        customerPage.addBtnC().click();
        customerPage.refreshBtn().click();

        Assert.assertNotNull(customerPage.entity(customerGroupName));
        Assert.assertTrue(customerPage.entity(customerGroupName).isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createCustomerGroupWithoutName() {
        sideBarMenuView.customerGroupsBtn().click();
        customerPage.plusBtn().click();
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.addEntityGroupView());
        Assert.assertTrue(customerPage.errorMessage().isDisplayed());
        Assert.assertEquals(customerPage.errorMessage().getText(), "Name is required.");
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createCustomerWithOnlySpace() {
        sideBarMenuView.customerGroupsBtn().click();
        customerPage.plusBtn().click();
        customerPage.addEntityGroupViewNameField().sendKeys(" ");
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_GROUP_NAME_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityGroupView());
        Assert.assertTrue(customerPage.addEntityGroupView().isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createCustomerGroupWithSameName() {
        customerGroupName = ENTITY_NAME;
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.plusBtn().click();
        customerPage.addEntityGroupViewNameField().sendKeys(customerGroupName);
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityGroupView());
        Assert.assertTrue(customerPage.addEntityGroupView().isDisplayed());
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void documentation() {
        String urlPath = "/docs/pe/user-guide/groups/";

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.allEntity().get(0).click();
        customerPage.goToHelpEntityGroupPage();

        Assert.assertTrue(urlContains(urlPath));
    }
}