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
package org.thingsboard.server.msa.ui.tests.rulechainssmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

@Feature("Delete several rule chains")
public class DeleteSeveralRuleChainsTest extends AbstractRuleChainTest {

    @Test(priority = 10, groups = "smoke")
    @Description("Remove several rule chains by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top")
    public void canDeleteSeveralRuleChainsByTopBtn() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteSelected(2);
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertRuleChainsIsNotPresent(ruleChainName);
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Remove several rule chains by mark all the rule chains on the page by clicking in the topmost checkbox" +
            " and then clicking on the trash icon in the menu that appears")
    public void selectAllRuleChain() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();
        jsClick(ruleChainsPage.deleteSelectedBtn());
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertRuleChainsIsNotPresent(ruleChainName);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top")
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        assertIsDisable(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by mark all the rule chains on the page by clicking in the topmost checkbox" +
            " and then clicking on the trash icon in the menu that appears")
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Test(priority = 30, groups = "smoke")
    @Description("Remove several rule chains by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top without refresh")
    public void deleteSeveralRuleChainsByTopBtnWithoutRefresh() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteSelected(2);

        Assert.assertTrue(ruleChainsPage.assertRuleChainsIsNotPresent(ruleChainName));
    }
}
