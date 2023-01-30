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
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ProfilesPageHelper extends ProfilesPageElements {
    public ProfilesPageHelper(WebDriver driver) {
        super(driver);
    }

    private String name;
    private String ruleChain;
    private String mobileDashboard;
    private String queue;
    private String description;
    private String profile;

    public void setName() {
        this.name = profileViewNameField().getAttribute("value");
    }

    public void setRuleChain() {
        this.ruleChain = profileViewRuleChainField().getAttribute("value");
    }

    public void setMobileDashboard() {
        this.mobileDashboard = profileViewMobileDashboardField().getAttribute("value");
    }

    public void setQueue() {
        this.queue = profileViewQueueField().getAttribute("value");
    }

    public void setDescription() {
        scrollToElement(profileViewDescriptionField());
        this.description = profileViewDescriptionField().getAttribute("value");
    }

    public void setProfileName() {
        this.profile = profileNames().get(0).getText();
    }

    public void setProfileName(int number) {
        this.profile = profileNames().get(number).getText();
    }

    public String getName() {
        return this.name;
    }

    public String getRuleChain() {
        return this.ruleChain;
    }

    public String getMobileDashboard() {
        return this.mobileDashboard;
    }

    public String getQueue() {
        return this.queue;
    }

    public String getDescription() {
        return this.description;
    }

    public String getProfileName() {
        return this.profile;
    }

    public void createDeviceProfileEnterName(CharSequence keysToEnter) {
        enterText(addDeviceProfileNameField(), keysToEnter);
    }

    public void addDeviceProfileViewChooseRuleChain(String ruleChain) {
        addDeviceProfileRuleChainField().click();
        entityFromList(ruleChain).click();
    }

    public void addAssetProfileViewChooseRuleChain(String ruleChain) {
        addAssetProfileRuleChainField().click();
        entityFromList(ruleChain).click();
    }

    public void addDeviceProfileViewChooseMobileDashboard(String mobileDashboard) {
        addDeviceProfileMobileDashboardField().click();
        entityFromList(mobileDashboard).click();
    }

    public void addAssetProfileViewChooseMobileDashboard(String mobileDashboard) {
        addAssetProfileMobileDashboardField().click();
        entityFromList(mobileDashboard).click();
    }

    public void addDeviceProfileViewChooseQueue(String queue) {
        addDeviceProfileQueueField().click();
        entityFromList(queue).click();
        waitUntilAttributeContains(addDeviceProfileQueueField(), "aria-expanded", "false");
    }

    public void addAssetsProfileViewChooseQueue(String queue) {
        addAssetProfileQueueField().click();
        entityFromList(queue).click();
        waitUntilAttributeContains(addAssetProfileQueueField(), "aria-expanded", "false");
    }

    public void addDeviceProfileViewEnterDescription(String description) {
        addDeviceDescriptionField().sendKeys(description);
    }

    public void addAssetProfileViewEnterDescription(String description) {
        addAssetDescriptionField().sendKeys(description);
    }

    public void openCreateDeviceProfileView() {
        plusBtn().click();
        createNewDeviceProfileBtn().click();
    }

    public void openCreateAssetProfileView() {
        plusBtn().click();
        createNewAssetProfileBtn().click();
    }

    public void addAssetProfileViewEnterName(String name) {
        addAssetProfileNameField().click();
        addAssetProfileNameField().sendKeys(name);
    }

    public void openImportDeviceProfileView() {
        plusBtn().click();
        importDeviceProfileBtn().click();
    }

    public void openImportAssetProfileView() {
        plusBtn().click();
        importAssetProfileBtn().click();
    }

    public boolean deleteDeviceProfileFromViewBtnIsNotDisplayed() {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(getDeviseProfileViewDeleteBtn())));
    }

    public boolean deleteAssetProfileFromViewBtnIsNotDisplayed() {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(getAssetProfileViewDeleteBtn())));
    }

    public void goToProfileHelpPage() {
        jsClick(profileViewHelpBtn());
        goToNextTab(2);
    }

    public void sortByNameDown() {
        doubleClick(sortByNameBtn());
    }

    public boolean profileIsNotPresent(String name) {
        return elementsIsNotPresent(getEntity(name));
    }

    public boolean checkBoxIsDisplayed(String name) {
        return waitUntilPresenceOfElementLocated(getCheckbox(name)).isDisplayed();
    }
}

