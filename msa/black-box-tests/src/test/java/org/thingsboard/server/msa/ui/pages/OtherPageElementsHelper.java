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
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class OtherPageElementsHelper extends OtherPageElements {
    public OtherPageElementsHelper(WebDriver driver) {
        super(driver);
    }

    private String headerName;
    private String entityGroupName;

    public void setHeaderName() {
        this.headerName = headerNameView().getText();
    }

    public void setEntityGroupName() {
        this.entityGroupName = entityGroups().get(0).getText();
    }

    public void setEntityGroupName(int i) {
        this.entityGroupName = entityGroups().get(i).getText();
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getEntityGroupName() {
        return entityGroupName;
    }

    public boolean assertEntityIsNotPresent(String entityName) {
        return elementIsNotPresent(getEntity(entityName));
    }

    public void goToHelpPage() {
        helpBtn().click();
        goToNextTab(2);
    }

    public void goToHelpEntityGroupPage() {
        helpBtnEntityGroup().click();
        goToNextTab(2);
    }

    public void clickOnCheckBoxes(int count) {
        for (int i = 0; i < count; i++) {
            checkBoxes().get(i).click();
        }
    }

    public void changeNameEditMenu(CharSequence keysToSend) {
        nameFieldEditMenu().click();
        nameFieldEditMenu().clear();
        nameFieldEditMenu().sendKeys(keysToSend);
    }

    public void changeDescription(String newDescription) {
        descriptionEntityView().click();
        descriptionEntityView().clear();
        descriptionEntityView().sendKeys(newDescription);
    }

    public String deleteRuleChainTrash(String entityName) {
        deleteBtn(entityName).click();
        warningPopUpYesBtn().click();
        return entityName;
    }

    public String deleteSelected(String entityName) {
        checkBox(entityName).click();
        jsClick(deleteSelectedBtn());
        warningPopUpYesBtn().click();
        return entityName;
    }

    public void deleteSelected(int countOfCheckBoxes) {
        clickOnCheckBoxes(countOfCheckBoxes);
        jsClick(deleteSelectedBtn());
        warningPopUpYesBtn().click();
    }

    public void searchEntity(String namePath) {
        searchBtn().click();
        searchField().sendKeys(namePath);
        sleep(0.5);
    }

    public void doubleClickOnEntityGroup(String entityGroupName) {
        doubleClick(entity(entityGroupName));
    }

    public void sortByNameDown() {
        doubleClick(sortByNameBtn());
    }

    public void changeOwner(String customerName) {
        changeOwnerViewField().click();
        entityFromDropDown(customerName).click();
        changeOwnerViewChangeOwnerBtn().click();
        warningPopUpYesBtn().click();
    }

    public void changeItemsCountPerPage(int itemCount) {
        itemsPerPage().click();
        WebElement element = itemsCount(itemCount);
        element.click();
        waitUntilInvisibilityOfElementLocated(element);
    }
}
