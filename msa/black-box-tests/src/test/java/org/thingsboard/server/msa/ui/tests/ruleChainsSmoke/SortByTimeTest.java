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
package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class SortByTimeTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;
    private String ruleChainName;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (ruleChainName != null) {
            testRestClient.deleteRuleChain(getRuleChainByName(ruleChainName).getId());
            ruleChainName = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void sortByTimeDown() {
        String ruleChain = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain));
        ruleChainName = ruleChain;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(0).getText();

        Assert.assertEquals(firstListElement, lastCreated);
        Assert.assertNotNull(ruleChainsPage.createdTimeEntity(ruleChainName, lastCreated));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void sortByTimeUp() {
        String ruleChain = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain));
        ruleChainName = ruleChain;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByTimeBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(ruleChainsPage.createdTime().size() - 1).getText();

        Assert.assertEquals(firstListElement, lastCreated);
        Assert.assertNotNull(ruleChainsPage.createdTimeEntity(ruleChainName, lastCreated));
    }
}