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
package org.thingsboard.server.msa.ui.tests.assetProfileSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class DeleteAssetProfileTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void removeAssetProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void removeAssetProfileFromView() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.entity(name).click();
        profilesPage.assetProfileViewDeleteBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void removeSelectedAssetProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.checkBox(name).click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDefaultAssetProfile() {
        sideBarMenuView.openAssetProfiles();

        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDefaultAssetProfileFromView() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.entity("default").click();

        Assert.assertTrue(profilesPage.deleteAssetProfileFromViewBtnIsNotDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeSelectedDefaultAssetProfile() {
        sideBarMenuView.openAssetProfiles();

        Assert.assertNotNull(profilesPage.presentCheckBox("default"));
        Assert.assertFalse(profilesPage.presentCheckBox("default").isDisplayed());
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void removeAssetProfileWithoutRefresh() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }
}
