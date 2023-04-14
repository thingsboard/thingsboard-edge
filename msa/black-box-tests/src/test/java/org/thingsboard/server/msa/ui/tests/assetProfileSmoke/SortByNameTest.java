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
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultAssetProfile;

public class SortByNameTest extends AbstractDriverBaseTest {
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
    @Feature("Sort by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort asset profile 'UP'")
    public void specialCharacterUp(String name) {
        testRestClient.postAssetProfile(defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.sortByNameBtn().click();
        profilesPage.setProfileName();

        Assert.assertEquals(profilesPage.getProfileName(), name);
    }

    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort asset profile 'UP'")
    public void allSortUp(String assetProfile, String assetProfileSymbol, String assetProfileNumber) {
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileSymbol));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfile));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileNumber));

        sideBarMenuView.openAssetProfiles();
        profilesPage.sortByNameBtn().click();
        profilesPage.setProfileName(0);
        String firstAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(1);
        String secondAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(2);
        String thirdAssetProfile = profilesPage.getProfileName();

        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfile).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileNumber).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileSymbol).getId());

        Assert.assertEquals(firstAssetProfile, assetProfileSymbol);
        Assert.assertEquals(secondAssetProfile, assetProfileNumber);
        Assert.assertEquals(thirdAssetProfile, assetProfile);
    }

    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort asset profile 'DAWN'")
    public void specialCharacterDown(String name) {
        testRestClient.postAssetProfile(defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.sortByNameDown();
        profilesPage.setProfileName(profilesPage.allEntity().size() - 1);

        Assert.assertEquals(profilesPage.getProfileName(), name);
    }

    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort asset profile 'DOWN'")
    public void allSortDown(String assetProfile, String assetProfileSymbol, String assetProfileNumber) {
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileSymbol));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfile));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileNumber));

        sideBarMenuView.openAssetProfiles();
        int lastIndex = profilesPage.allEntity().size() - 1;
        profilesPage.sortByNameDown();
        profilesPage.setProfileName(lastIndex);
        String firstAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(lastIndex - 1);
        String secondAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(lastIndex - 2);
        String thirdAssetProfile = profilesPage.getProfileName();

        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfile).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileNumber).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileSymbol).getId());

        Assert.assertEquals(firstAssetProfile, assetProfileSymbol);
        Assert.assertEquals(secondAssetProfile, assetProfileNumber);
        Assert.assertEquals(thirdAssetProfile, assetProfile);
    }
}
