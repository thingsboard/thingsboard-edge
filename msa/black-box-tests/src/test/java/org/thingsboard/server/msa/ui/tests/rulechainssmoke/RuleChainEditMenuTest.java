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
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_RULE_CHAIN_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

@Feature("Edit rule chain")
public class RuleChainEditMenuTest extends AbstractRuleChainTest {

    @Test(priority = 10, groups = "smoke")
    @Description("Change name by edit menu")
    public void changeName() {
        String newRuleChainName = "Changed" + getRandomNumber();
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.setHeaderName();
        String nameBefore = ruleChainsPage.getHeaderName();
        ruleChainsPage.editPencilRuleChainViewBtn().click();
        ruleChainsPage.changeNameEditMenu(newRuleChainName);
        ruleChainsPage.doneBtnEditRuleChainView().click();
        ruleChainName = newRuleChainName;
        ruleChainsPage.setHeaderName();
        String nameAfter = ruleChainsPage.getHeaderName();

        assertThat(nameAfter).as("The name has changed").isNotEqualTo(nameBefore);
        assertThat(nameAfter).as("The name has changed correctly").isEqualTo(newRuleChainName);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Delete name and save")
    public void deleteName() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilRuleChainViewBtn().click();
        ruleChainsPage.changeNameEditMenu("");

        assertIsDisable(ruleChainsPage.doneBtnEditViewVisible());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space")
    public void saveOnlyWithSpace() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilRuleChainViewBtn().click();
        ruleChainsPage.changeNameEditMenu(" ");
        ruleChainsPage.doneBtnEditRuleChainView().click();

        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText()).as("Text of warning message").isEqualTo(EMPTY_RULE_CHAIN_MESSAGE);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName, description));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilRuleChainViewBtn().click();
        ruleChainsPage.descriptionEntityView().sendKeys(newDescription);
        ruleChainsPage.doneBtnEditRuleChainView().click();
        ruleChainsPage.setDescription();

        assertThat(ruleChainsPage.getDescription()).as("The description changed correctly").isEqualTo(finalDescription);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "enable")
    @Description("Enable debug mode/Disable debug mode")
    public void debugMode(boolean debugMode) {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName, debugMode));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilRuleChainViewBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditRuleChainView().click();

        if (debugMode) {
            assertThat(ruleChainsPage.debugCheckboxView().getAttribute("class").contains("selected"))
                    .as("Debug mode is enable").isFalse();
        } else {
            assertThat(ruleChainsPage.debugCheckboxView().getAttribute("class").contains("selected"))
                    .as("Debug mode is enable").isTrue();
        }
    }
}
