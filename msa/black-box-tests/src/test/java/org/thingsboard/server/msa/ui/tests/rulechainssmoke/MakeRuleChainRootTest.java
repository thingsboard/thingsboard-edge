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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Make rule chain root")
public class MakeRuleChainRootTest extends AbstractRuleChainTest {

    @AfterMethod
    public void makeRoot() {
        setRootRuleChain("Root Rule Chain");
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Make rule chain root by clicking on the 'Make rule chain root' icon in the right corner")
    public void makeRuleChainRootByRightCornerBtn() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.makeRootBtn(ruleChain).click();
        ruleChainsPage.warningPopUpYesBtn().click();

        assertIsDisplayed(ruleChainsPage.rootCheckBoxEnable(ruleChain));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Make rule chain root by clicking on the 'Make rule chain root' button in the entity view")
    public void makeRuleChainRootFromView() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.detailsBtn(ruleChain).click();
        ruleChainsPage.makeRootFromViewBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.closeEntityViewBtn().click();

        assertIsDisplayed(ruleChainsPage.rootCheckBoxEnable(ruleChain));
    }

    @Test(priority = 30, groups = "smoke")
    @Description("Make multiple root rule chains (only one rule chain can be root)")
    public void multiplyRoot() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.detailsBtn(ruleChain).click();
        jsClick(ruleChainsPage.makeRootFromViewBtn());
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.closeEntityViewBtn().click();

        assertThat(ruleChainsPage.rootCheckBoxesEnable()).as("Enable only 1 root checkbox").hasSize(1);
    }
}
