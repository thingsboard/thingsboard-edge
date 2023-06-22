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

import java.util.HashSet;
import java.util.Set;

public class SolutionTemplatesInstalledViewHelper extends SolutionTemplatesInstalledViewElements {
    public SolutionTemplatesInstalledViewHelper(WebDriver driver) {
        super(driver);
    }

    public void waitUntilInstallFinish() {
        waitUntilInvisibilityOfElementLocated(solutionTemplateInstallProgressPopUp());
    }

    private String firstUserName;
    private String secondUserName;

    public void setFirstUserName() {
        this.firstUserName = user1().getText();
    }

    public void setSecondUserName() {
        this.secondUserName = user2().getText();
    }

    public String getFirstUserName() {
        return this.firstUserName;
    }

    public String getSecondUserName() {
        return this.secondUserName;
    }

    private String getLink(String stepName, WebElement element) {
        return Allure.step(stepName, () -> {
            element.click();
            goToNextTab(2);
            captureScreen(driver, stepName);
            String url = driver.getCurrentUrl();
            driver.close();
            goToNextTab(1);
            return url;
        });
    }

    public Set<String> getDashboardLinks() {
        Set<String> urls = new HashSet<>();
        return Allure.step("Check redirect on main dashboard page", () -> {
            linkDashboardBtn().forEach(linkBtn -> {
                int number = 1;
                linkBtn.click();
                goToNextTab(2);
                captureScreen(driver, "Check redirect on main dashboard page #" + number);
                urls.add(driver.getCurrentUrl());
                driver.close();
                goToNextTab(1);
                number++;
            });
            return urls;
        });
    }

    public String getGuideLink() {
        return getLink("Check redirect on dashboard development guide page", linkGuideBtn());
    }

    public String getHttpApiLink() {
        return getLink("Check redirect on HTTP API documentation page", linkHttpApiBtn());
    }

    public String getConnectionDevicesLink() {
        return getLink("Check redirect on connection device documentation page", linkConnectionDevices());
    }

    public String getAlarmRuleLink() {
        return getLink("Check redirect on alarm rule documentation page", linkAlarmRuleBtn());
    }

    public String getDeviceProfileLink() {
        return getLink("Check redirect on device profile page", linkDeviceProfileBtn());
    }

    public String getAlarmRulesLink() {
        return getLink("Check redirect on device profile page", linkAlarmRulesBtn());
    }

    public String getThingsBoardIoTGatewayLink() {
        return getLink("Check redirect on ThingsBoard IoT Gateway documentation page", linkThingsBoardIoTGateway());
    }

    public String getThingsBoardMQTTGatewayLink() {
        return getLink("Check redirect on ThingsBoard IoT Gateway documentation page", linkThingsBoardMQTTGateway());
    }

    public String getThingsBoardIntegration() {
        return getLink("Check redirect on ThingsBoard Integration documentation page", linkIntegration());
    }

    public void goToMainDashboard() {
        solutionTemplateInstalledPopUp();
        jsClick(goToMainDashboardPageBtn());
        waitUntilUrlContainsText("dashboards");
    }
}