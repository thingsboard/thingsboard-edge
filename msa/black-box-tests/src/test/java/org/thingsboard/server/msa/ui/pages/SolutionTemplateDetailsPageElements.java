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
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

import java.util.List;

public class SolutionTemplateDetailsPageElements extends AbstractBasePage {
    public SolutionTemplateDetailsPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String HEAD_OF_TITLE_CARD = "//h2";
    private static final String TITLE_CARD_PARAGRAPH = "//div[contains(@class,'title-container')]//p[1]";
    private static final String SOLUTION_DESCRIPTION_PARAGRAPH = "//h3/../p[1]";
    private static final String SCREENSHOT_CIRCLES = "//ul/li/div";
    private static final String SCREENSHOTS = "//ngx-hm-carousel//section/article[@ngx-hm-carousel-item]/div";
    private static final String SWIPE_SCREENSHOT_RIGHT_BTN = "//mat-icon[text()='keyboard_arrow_right']//ancestor::button";
    private static final String INSTALL_BTN = "//span[contains(text(),'Install')]/parent::button";
    private static final String DELETE_BTN = "//span[contains(text(),'Delete')]/parent::button";
    private static final String INSTRUCTION_BTN = "//div[@fxlayoutalign='end center']/button[@color='primary']";

    public WebElement headOfTitleCard() {
        return waitUntilVisibilityOfElementLocated(HEAD_OF_TITLE_CARD);
    }

    public WebElement titleCardParagraph() {
        return waitUntilVisibilityOfElementLocated(TITLE_CARD_PARAGRAPH);
    }

    public WebElement solutionDescriptionParagraph() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_DESCRIPTION_PARAGRAPH);
    }

    public List<WebElement> screenshotCircles() {
        return waitUntilElementsToBeClickable(SCREENSHOT_CIRCLES);
    }

    public List<WebElement> screenshots() {
        return waitUntilPresenceOfElementsLocated(SCREENSHOTS);
    }

    public WebElement swipeScreenshotRightBtn() {
        return waitUntilElementToBeClickable(SWIPE_SCREENSHOT_RIGHT_BTN);
    }

    public WebElement installBtn() {
        return waitUntilElementToBeClickable(INSTALL_BTN);
    }

    public WebElement deleteBtn() {
        return waitUntilElementToBeClickable(DELETE_BTN);
    }

    public WebElement instructionBtn() {
        return waitUntilElementToBeClickable(INSTRUCTION_BTN);
    }
}
