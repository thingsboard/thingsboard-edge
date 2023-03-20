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
package org.thingsboard.server.msa.ui.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ContainerTestSuite;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.fail;
import static org.thingsboard.server.msa.TestProperties.getBaseUiUrl;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

@Slf4j
abstract public class AbstractDriverBaseTest extends AbstractContainerTest {

    protected WebDriver driver;
    private final Dimension dimension = new Dimension(WIDTH, HEIGHT);
    private static final int WIDTH = 1680;
    private static final int HEIGHT = 1050;
    private static final String REMOTE_WEBDRIVER_HOST = "http://localhost:4444";
    protected static final PageLink pageLink = new PageLink(10);
    private static final ContainerTestSuite instance = ContainerTestSuite.getInstance();
    private JavascriptExecutor js;

    @BeforeClass
    public void startUp() throws MalformedURLException {
        log.info("===>>> Setup driver");
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("-remote-allow-origins=*"); //temporary fix after updating google chrome
        if (instance.isActive()) {
            RemoteWebDriver remoteWebDriver = new RemoteWebDriver(new URL(REMOTE_WEBDRIVER_HOST), options);
            remoteWebDriver.setFileDetector(new LocalFileDetector());
            driver = remoteWebDriver;
        } else {
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
        driver.manage().window().setSize(dimension);
        openBaseUiUrl();
    }

    @BeforeMethod
    public void open() {
        openBaseUiUrl();
    }

    @AfterMethod
    public void addScreenshotToReport() {
        captureScreen(driver, "After test page screenshot");
    }

    @AfterClass
    public void teardown() {
        log.info("<<<=== Teardown");
        driver.quit();
    }

    public String getJwtTokenFromLocalStorage() {
        js = (JavascriptExecutor) driver;
        return (String) js.executeScript("return window.localStorage.getItem('jwt_token');");
    }

    public void openBaseUiUrl() {
        driver.get(getBaseUiUrl());
    }

    public String getUrl() {
        return driver.getCurrentUrl();
    }

    public WebDriver getDriver() {
        return driver;
    }

    protected boolean urlContains(String urlPath) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(5000));
        try {
            wait.until(ExpectedConditions.urlContains(urlPath));
        } catch (WebDriverException e) {
            return fail("URL not contains " + urlPath);
        }
        return driver.getCurrentUrl().contains(urlPath);
    }

    public void jsClick(WebElement element) {
        js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", element);
    }

    public static RuleChain getRuleChainByName(String name) {
        try {
            return testRestClient.getRuleChains(pageLink).getData().stream()
                    .filter(s -> s.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such rule chain with name: " + name);
            return null;
        }
    }

    public static Customer getCustomerByName(String name) {
        try {
            return testRestClient.getCustomers(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such customer with name: " + name);
            return null;
        }
    }

    public static DeviceProfile getDeviceProfileByName(String name) {
        try {
            return testRestClient.getDeviceProfiles(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such device profile with name: " + name);
            return null;
        }
    }

    public static AssetProfile getAssetProfileByName(String name) {
        try {
            return testRestClient.getAssetProfiles(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such asset profile with name: " + name);
            return null;
        }
    }

    public static EntityGroupInfo getEntityGroupByName(EntityType entityType, String name) {
        try {
            return testRestClient.getEntityGroups(entityType).stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such " + entityType.name() + " with name: " + name);
            return null;
        }
    }

    public void captureScreen(WebDriver driver, String screenshotName) {
        if (driver instanceof TakesScreenshot) {
            Allure.addAttachment(screenshotName,
                    new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        }
    }
}
