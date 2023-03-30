package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class SchedulerPageHelper extends SchedulerPageElements{
    public SchedulerPageHelper(WebDriver driver) {
        super(driver);
    }

    public boolean schedulerIsNotPresent(String schedulerName) {
        return elementIsNotPresent(String.format(SCHEDULER, schedulerName));
    }
}
