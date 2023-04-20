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
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class DeleteRuleChainTest extends AbstractDriverBaseTest {
    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the rule chain by clicking on the trash icon in the right side of rule chain")
    public void removeRuleChainByRightSideBtn() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain));
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove rule chain by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedRuleChain() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain));
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the rule chain by clicking on the 'Delete rule chain' btn in the entity view")
    public void removeFromRuleChainView() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainFromView(ruleChainName);
        jsClick(ruleChainsPage.refreshBtn());

        Assert.assertTrue(ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain));
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by clicking on the trash icon in the right side of rule chain")
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertFalse(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME).isEnabled());
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove root rule chain by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by clicking on the 'Delete rule chain' btn in the entity view")
    public void removeFromRootRuleChainView() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ROOT_RULE_CHAIN_NAME).click();

        Assert.assertTrue(ruleChainsPage.deleteBtnInRootRuleChainIsNotDisplayed());
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the rule chain with device profile by clicking on the trash icon in the right side of rule chain")
    public void removeProfileRuleChainByRightSideBtn() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteBtn(deletedRuleChain).click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(deletedRuleChain));
        Assert.assertTrue(ruleChainsPage.entity(deletedRuleChain).isDisplayed());
        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the rule chain with device profile by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedProfileRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected("Thermostat");
        ruleChainsPage.refreshBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(deletedRuleChain));
        Assert.assertTrue(ruleChainsPage.entity(deletedRuleChain).isDisplayed());
        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the rule chain with device profile by clicking on the 'Delete rule chain' btn in the entity view")
    public void removeFromProfileRuleChainView() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(deletedRuleChain).click();
        jsClick(ruleChainsPage.deleteBtnFromView());
        ruleChainsPage.warningPopUpYesBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(deletedRuleChain));
        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 30, groups = "smoke")
    @Description("Remove the rule chain by clicking on the trash icon in the right side of rule chain without refresh")
    public void removeRuleChainByRightSideBtnWithoutRefresh() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ruleChainName);

        Assert.assertTrue(ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain));
    }
}
