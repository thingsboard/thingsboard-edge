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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;
import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class DeleteSeveralCustomerGroupTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelper(driver).authorizationTenant();
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void canDeleteSeveralCustomersByTopBtn() {
        String entityGroupName1 = ENTITY_NAME + "1";
        String entityGroupName2 = ENTITY_NAME + "2";
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(entityGroupName1, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(entityGroupName2, EntityType.CUSTOMER));

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.clickOnCheckBoxes(2);
        customerPage.deleteSelectedBtn().click();
        customerPage.warningPopUpYesBtn().click();
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.customerIsNotPresent(entityGroupName1));
        Assert.assertTrue(customerPage.customerIsNotPresent(entityGroupName2));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void selectAllCustomers() {
        String entityGroupName1 = ENTITY_NAME + "1";
        String entityGroupName2 = ENTITY_NAME + "2";
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(entityGroupName1, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(entityGroupName2, EntityType.CUSTOMER));

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.selectAllCheckBox().click();
        customerPage.deleteSelectedBtn().click();
        customerPage.warningPopUpYesBtn().click();
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.customerIsNotPresent(entityGroupName1));
        Assert.assertTrue(customerPage.customerIsNotPresent(entityGroupName2));
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void deleteSeveralCustomersByTopBtnWithoutRefresh() {
        String entityGroupName1 = ENTITY_NAME + "1";
        String entityGroupName2 = ENTITY_NAME + "2";
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(entityGroupName1, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(entityGroupName2, EntityType.CUSTOMER));

        sideBarMenuView.customerGroupsBtn().click();
        customerPage.clickOnCheckBoxes(2);
        customerPage.deleteSelectedBtn().click();
        customerPage.warningPopUpYesBtn().click();

        Assert.assertTrue(customerPage.customerIsNotPresent(entityGroupName1));
        Assert.assertTrue(customerPage.customerIsNotPresent(entityGroupName2));
    }
}
