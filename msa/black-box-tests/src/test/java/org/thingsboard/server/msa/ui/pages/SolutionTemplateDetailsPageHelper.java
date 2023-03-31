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

import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Fail.fail;

public class SolutionTemplateDetailsPageHelper extends SolutionTemplateDetailsPageElements {
    public SolutionTemplateDetailsPageHelper(WebDriver driver) {
        super(driver);
    }

    private String headOfTitleCardName;
    private String titleCardParagraphText;
    private String solutionDescriptionParagraphText;

    public void setHeadOfTitleName() {
        headOfTitleCardName = headOfTitleCard().getText();
    }

    public void setTitleCardParagraphText() {
        titleCardParagraphText = titleCardParagraph().getText();
    }

    public void setSolutionDescriptionParagraphText() {
        solutionDescriptionParagraphText = solutionDescriptionParagraph().getText();
    }

    public String getHeadOfTitleCardName() {
        return headOfTitleCardName;
    }

    public String getTitleCardParagraphText() {
        return titleCardParagraphText;
    }

    public String getSolutionDescriptionParagraphText() {
        return solutionDescriptionParagraphText;
    }

    private void checkScreenshotContent(WebElement screenshot, String urlScreenshotPath) {
        if (!waitUntilVisibilityOfElementLocated(screenshot).isDisplayed()) {
            fail("Screenshot at point " + screenshots().indexOf(screenshot) + " isn't displayed");
            if (!screenshot.getCssValue("background-image").contains(urlScreenshotPath)) {
                fail("Screenshot at point " + screenshots().indexOf(screenshot) + " isn't " + urlScreenshotPath.split("/")[1]);
            }
        }
    }

    private boolean screenshotsAreCorrected(String urlScreenshotPath) {
        sleep(3); //wait until images completely load on page
        for (WebElement screenshot : screenshots()) {
            int imageNumber = screenshots().indexOf(screenshot);
            checkScreenshotContent(screenshot, urlScreenshotPath);
            Allure.step("Check screenshot at point " + imageNumber, () ->
                    captureScreen(driver, "Screenshot #" + imageNumber));
            if (imageNumber != screenshotCircles().size() - 1) {
                swipeScreenshotRightBtn().click();
                waitUntilInvisibilityOfElementLocated(screenshot);
            }
        }
        return true;
    }

    public boolean temperatureHumiditySensorsScreenshotsAreCorrected() {
        return screenshotsAreCorrected("temperature_sensors/temperature-sensors");
    }

    public boolean smartOfficeScreenshotsAreCorrected() {
        return screenshotsAreCorrected("smart_office/smart-office");
    }

    public boolean fleetTrackingScreenshotsAreCorrected() {
        return screenshotsAreCorrected("fleet_tracking/fleet-tracking");
    }

    public boolean airQualityMonitoringScreenshotsAreCorrected() {
        return screenshotsAreCorrected("air_quality_index/air-quality-index");
    }

    public boolean waterMeteringScreenshotsAreCorrected() {
        return screenshotsAreCorrected("water_metering/water-metering");
    }

    public boolean smartRetailScreenshotsAreCorrected() {
        return screenshotsAreCorrected("smart_retail/smart-retail");
    }

    public boolean smartIrrigationScreenshotsAreCorrected() {
        return screenshotsAreCorrected("smart_irrigation/smart-irrigation");
    }

    public boolean assignedLivingScreenshotsAreCorrected() {
        return screenshotsAreCorrected("assisted_living/assisted-living");
    }
}

