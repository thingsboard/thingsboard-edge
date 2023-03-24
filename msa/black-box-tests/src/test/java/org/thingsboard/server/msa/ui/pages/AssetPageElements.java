package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class AssetPageElements extends OtherPageElementsHelper{
    public AssetPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String ALL_GROUP_NAMES = "//mat-icon[contains(text(),'check')]/ancestor::mat-row/mat-cell[contains(@class,'name')]/span";
    private static final String ALL_NAMES = "//mat-cell[contains(@class,'cdk-column-column1')]/span";

    public List<WebElement> allGroupNames() {
        return waitUntilElementsToBeClickable(ALL_GROUP_NAMES);
    }

    public List<WebElement> allNames() {
        return waitUntilElementsToBeClickable(ALL_NAMES);
    }
}
