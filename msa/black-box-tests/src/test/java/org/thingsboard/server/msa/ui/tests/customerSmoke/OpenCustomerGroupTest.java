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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;

public class OpenCustomerGroupTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Epic("Customers smoke tests")
    @Feature("Open customer group")
    @Test(groups = "smoke")
    @Description("Open customer group by click on its name")
    public void openWindowByRightCornerBtn() {
        sideBarMenuView.goToCustomerGroups();
        customerPage.setEntityGroupName();
        String entityGroupName = customerPage.getEntityGroupName();
        customerPage.entity(entityGroupName).click();

        Assert.assertTrue(urlContains(getEntityGroupByName(EntityType.CUSTOMER, entityGroupName).getId().toString()));
        Assert.assertNotNull(customerPage.entityGroupTableHeader());
        Assert.assertTrue(customerPage.entityGroupTableHeader().isDisplayed());
        Assert.assertTrue(customerPage.entityGroupTableHeader().getText().contains(entityGroupName));
        Assert.assertNotNull(customerPage.entityGroupHeader(entityGroupName));
        Assert.assertTrue(customerPage.entityGroupHeader(entityGroupName).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Open customer group")
    @Test(groups = "smoke")
    @Description("Open customer group by click on 'Open entity group' btn in customer group view")
    public void openWindowByViewBtn() {
        sideBarMenuView.goToCustomerGroups();
        customerPage.setEntityGroupName();
        String entityGroupName = customerPage.getEntityGroupName();
        customerPage.detailsBtn(entityGroupName).click();
        customerPage.openEntityGroupViewBtn().click();

        Assert.assertTrue(urlContains(getEntityGroupByName(EntityType.CUSTOMER, entityGroupName).getId().toString()));
        Assert.assertNotNull(customerPage.entityGroupTableHeader());
        Assert.assertTrue(customerPage.entityGroupTableHeader().isDisplayed());
        Assert.assertTrue(customerPage.entityGroupTableHeader().getText().contains(entityGroupName));
        Assert.assertNotNull(customerPage.entityGroupHeader(entityGroupName));
        Assert.assertTrue(customerPage.entityGroupHeader(entityGroupName).isDisplayed());
    }
}