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
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_ASSET_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class AssetProfileEditMenuTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;
    private String name;

    @BeforeClass
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

    @Epic("Asset profiles smoke")
    @Feature("Edit asset profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Change name by edit menu")
    public void changeName() {
        String name = ENTITY_NAME + random();
        String newName = "Changed" + getRandomNumber();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.entity(name).click();
        profilesPage.setHeaderName();
        String nameBefore = profilesPage.getHeaderName();
        jsClick(profilesPage.profileViewEditPencilBtn());
        profilesPage.changeNameEditMenu(newName);
        profilesPage.profileViewDoneBtn().click();
        this.name = newName;
        profilesPage.setHeaderName();
        String nameAfter = profilesPage.getHeaderName();

        Assert.assertNotEquals(nameBefore, nameAfter);
        Assert.assertEquals(nameAfter, newName);
    }

    @Epic("Asset profiles smoke")
    @Feature("Edit asset profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Delete name and save")
    public void deleteName() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.entity(name).click();
        jsClick(profilesPage.profileViewEditPencilBtn());
        profilesPage.changeNameEditMenu("");

        Assert.assertFalse(profilesPage.profileViewVisibleDoneBtn().isEnabled());
    }

    @Epic("Asset profiles smoke")
    @Feature("Edit asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space")
    public void saveWithOnlySpaceInName() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.entity(name).click();
        jsClick(profilesPage.profileViewEditPencilBtn());
        profilesPage.changeNameEditMenu(Keys.SPACE);
        profilesPage.profileViewDoneBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), EMPTY_ASSET_PROFILE_MESSAGE);
    }

    @Epic("Asset profiles smoke")
    @Feature("Edit asset profile")
    @Test(priority = 30, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name, description));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.entity(name).click();
        jsClick(profilesPage.profileViewEditPencilBtn());
        profilesPage.profileViewDescriptionField().sendKeys(newDescription);
        profilesPage.profileViewDoneBtn().click();
        profilesPage.setDescription();

        Assert.assertEquals(profilesPage.getDescription(), finalDescription);
    }
}
