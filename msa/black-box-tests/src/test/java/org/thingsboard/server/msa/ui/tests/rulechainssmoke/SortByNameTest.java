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
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

@Feature("Sort rule chain by name")
public class SortByNameTest extends AbstractRuleChainTest {

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort rule chain 'UP'")
    public void specialCharacterUp(String name) {
        ruleChainName = name;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameBtn().click();
        ruleChainsPage.setRuleChainName(0);

        assertThat(ruleChainsPage.getRuleChainName()).as("First rule chain after sort").isEqualTo(ruleChainName);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Sort rule chain by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort rule chain 'UP'")
    public void allSortUp(String ruleChain, String ruleChainSymbol, String ruleChainNumber) {
        RuleChainId ruleChainSymbolId = testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainSymbol)).getId();
        RuleChainId ruleChainId = testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain)).getId();
        RuleChainId ruleChainNumberId = testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainNumber)).getId();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameBtn().click();
        ruleChainsPage.setRuleChainName(0);
        String firstRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(1);
        String secondRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(2);
        String thirdRuleChain = ruleChainsPage.getRuleChainName();

        testRestClient.deleteRuleChain(ruleChainId);
        testRestClient.deleteRuleChain(ruleChainNumberId);
        testRestClient.deleteRuleChain(ruleChainSymbolId);

        assertThat(firstRuleChain).as("First rule chain with symbol in name").isEqualTo(ruleChainSymbol);
        assertThat(secondRuleChain).as("Second rule chain with number in name").isEqualTo(ruleChainNumber);
        assertThat(thirdRuleChain).as("Third rule chain with number in name").isEqualTo(ruleChain);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Sort rule chain by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort rule chain 'DOWN'")
    public void specialCharacterDown(String ruleChainName) {
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameDown();
        ruleChainsPage.setRuleChainName(ruleChainsPage.allNames().size() - 1);

        assertThat(ruleChainsPage.getRuleChainName()).as("Last rule chain after sort").isEqualTo(ruleChainName);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Sort rule chain by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort rule chain 'DOWN'")
    public void allSortDown(String ruleChain, String ruleChainSymbol, String ruleChainNumber) {
        RuleChainId ruleChainSymbolId = testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainSymbol)).getId();
        RuleChainId ruleChainId = testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain)).getId();
        RuleChainId ruleChainNumberId = testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainNumber)).getId();

        sideBarMenuView.ruleChainsBtn().click();
        int lastIndex = ruleChainsPage.allNames().size() - 1;
        ruleChainsPage.sortByNameDown();
        ruleChainsPage.setRuleChainName(lastIndex);
        String firstRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(lastIndex - 1);
        String secondRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(lastIndex - 2);
        String thirdRuleChain = ruleChainsPage.getRuleChainName();

        testRestClient.deleteRuleChain(ruleChainId);
        testRestClient.deleteRuleChain(ruleChainNumberId);
        testRestClient.deleteRuleChain(ruleChainSymbolId);

        assertThat(firstRuleChain).as("First from the end rule chain with symbol in name").isEqualTo(ruleChainSymbol);
        assertThat(secondRuleChain).as("Second from the end rule chain with number in name").isEqualTo(ruleChainNumber);
        assertThat(thirdRuleChain).as("Third rule from the end chain with number in name").isEqualTo(ruleChain);
    }
}
