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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.internal.ConstructorOrMethod;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;

import static org.testng.internal.Utils.log;
import static org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest.captureScreen;

@Slf4j
public class TestListener implements ITestListener {

    WebDriver driver;

    @Override
    public void onTestStart(ITestResult result) {
        log.info("===>>> Test started: " + result.getName());
    }

    /**
     * Invoked when a test succeeds
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("<<<=== Test completed successfully: " + result.getName());
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return;
        }
        driver = ((AbstractDriverBaseTest) result.getInstance()).getDriver();
        captureScreen(driver, "success");
    }

    /**
     * Invoked when a test fails
     */
    @Override
    public void onTestFailure(ITestResult result) {
        log.info("<<<=== Test failed: " + result.getName());
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return;
        }
        driver = ((AbstractDriverBaseTest) result.getInstance()).getDriver();
        captureScreen(driver, "failure");
    }

    /**
     * Invoked when a test skipped
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        log.info("<<<=== Test skipped: " + result.getName());
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return;
        }
        driver = ((AbstractDriverBaseTest) result.getInstance()).getDriver();
        captureScreen(driver, "skipped");
    }
}
