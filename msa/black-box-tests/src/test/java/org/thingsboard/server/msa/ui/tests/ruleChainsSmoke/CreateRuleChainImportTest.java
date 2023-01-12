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
package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.OpenRuleChainPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import java.util.ArrayList;

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_IMPORT_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_TXT_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class CreateRuleChainImportTest extends AbstractDriverBaseTest {
    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;
    private OpenRuleChainPageHelper openRuleChainPage;
    private final String absolutePathToFileImportRuleChain = getClass().getClassLoader().getResource(IMPORT_RULE_CHAIN_FILE_NAME).getPath();
    private final String absolutePathToFileImportTxt = getClass().getClassLoader().getResource(IMPORT_TXT_FILE_NAME).getPath();
    private String ruleChainName;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
        openRuleChainPage = new OpenRuleChainPageHelper(driver);
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
    public void importRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);

        Assert.assertNotNull(ruleChainsPage.importingFile(IMPORT_RULE_CHAIN_FILE_NAME));
        Assert.assertTrue(ruleChainsPage.importingFile(IMPORT_RULE_CHAIN_FILE_NAME).isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void importRuleChainAndDeleteFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.clearImportFileBtn().click();

        Assert.assertNotNull(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(IMPORT_RULE_CHAIN_FILE_NAME));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void importTxtFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportTxt);

        Assert.assertNotNull(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void importRuleChainAndSave() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        openRuleChainPage.doneBtn().click();
        openRuleChainPage.waitUntilDoneBtnDisable();
        ruleChainName = IMPORT_RULE_CHAIN_NAME;
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(IMPORT_RULE_CHAIN_NAME));
        Assert.assertTrue(ruleChainsPage.entity(IMPORT_RULE_CHAIN_NAME).isDisplayed());
    }

    @Test(priority = 40, groups = "smoke")
    @Description
    public void importRuleChainAndSaveWithSameName() {
        String ruleChainName = IMPORT_RULE_CHAIN_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        openRuleChainPage.doneBtn().click();
        openRuleChainPage.waitUntilDoneBtnDisable();
        sideBarMenuView.ruleChainsBtn().click();

        boolean entityNotNull = ruleChainsPage.entity(ruleChainName) != null;
        boolean entitiesSizeMoreOne = ruleChainsPage.entities(ruleChainName).size() > 1;
        ArrayList<Boolean> entityIsDisplayed = new ArrayList<>();
        ruleChainsPage.entities(ruleChainName).forEach(x -> entityIsDisplayed.add(x.isDisplayed()));

        testRestClient.deleteRuleChain(getRuleChainByName(ruleChainName).getId());

        Assert.assertTrue(entityNotNull);
        Assert.assertTrue(entitiesSizeMoreOne);
        entityIsDisplayed.forEach(Assert::assertTrue);
    }
}
