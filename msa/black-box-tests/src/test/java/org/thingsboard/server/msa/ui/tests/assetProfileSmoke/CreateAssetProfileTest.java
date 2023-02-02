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
package org.thingsboard.server.msa.ui.tests.assetProfileSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_ASSET_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE;

public class CreateAssetProfileTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;
    private String name;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (name != null) {
            testRestClient.deleteAssetProfile(getAssetProfileByName(name).getId());
            name = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void createAssetProfile() {
        String name = ENTITY_NAME + random();

        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(name);
        profilesPage.addAssetProfileAddBtn().click();
        this.name = name;
        profilesPage.refreshBtn().click();

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createAssetProfileWithDetails() {
        String name = ENTITY_NAME + random();
        String ruleChain = "Root Rule Chain";
        String mobileDashboard = "Firmware";
        String queue = "Main";
        String description = "Description";

        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(name);
        profilesPage.addAssetProfileViewChooseRuleChain(ruleChain);
        profilesPage.addAssetProfileViewChooseMobileDashboard(mobileDashboard);
        profilesPage.addAssetsProfileViewChooseQueue(queue);
        profilesPage.addAssetProfileViewEnterDescription(description);
        profilesPage.addAssetProfileAddBtn().click();
        this.name = name;
        profilesPage.refreshBtn().click();
        profilesPage.entity(name).click();
        profilesPage.setName();
        profilesPage.setRuleChain();
        profilesPage.setMobileDashboard();
        profilesPage.setQueue();
        profilesPage.setDescription();

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
        Assert.assertEquals(name, profilesPage.getName());
        Assert.assertEquals(ruleChain, profilesPage.getRuleChain());
        Assert.assertEquals(mobileDashboard, profilesPage.getMobileDashboard());
        Assert.assertEquals(queue, profilesPage.getQueue());
        Assert.assertEquals(description, profilesPage.getDescription());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createAssetProfileWithSameName() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(name);
        profilesPage.addAssetProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE);
        Assert.assertNotNull(profilesPage.addAssetProfileView());
        Assert.assertTrue(profilesPage.addAssetProfileView().isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createAssetProfileWithoutName() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();

        Assert.assertFalse(profilesPage.addBtnV().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createAssetProfileWithOnlySpaceInName() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(" ");
        profilesPage.addAssetProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), EMPTY_ASSET_PROFILE_MESSAGE);
        Assert.assertNotNull(profilesPage.addAssetProfileView());
        Assert.assertTrue(profilesPage.addAssetProfileView().isDisplayed());
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void createAssetProfileWithoutRefresh() {
        String name = ENTITY_NAME + random();

        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(name);
        profilesPage.addAssetProfileAddBtn().click();
        this.name = name;

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
    }

    @Test(priority = 40, groups = "smoke")
    @Description
    public void documentation() {
        String urlPath = "docs/pe/user-guide/asset-profiles/";

        sideBarMenuView.openAssetProfiles();
        profilesPage.allEntity().get(0).click();
        profilesPage.goToProfileHelpPage();

        Assert.assertTrue(urlContains(urlPath));
    }
}
