package com.quantum.utils;

import com.applitools.eyes.Eyes;
import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.NewTestException;
import com.perfecto.reportium.client.ReportiumClient;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


/** Created by lirona on 28/03/2017 **/

public class ApplitoolsUtils {

    public static void checkWindow(String tag){
        Object eyeObject = ConfigurationManager.getBundle().getObject("Eyes");
        if (null != eyeObject){
            Eyes eye = (Eyes) eyeObject;
            WebDriver driver = new WebDriverTestBase().getDriver();
            if (!eye.getIsOpen()) {

                //Selenium case:
                if (ConfigurationUtils.getBaseBundle().getPropertyValue("driver.name").contains("Remote")){
                    driver = eye.open(driver, driver.getCurrentUrl(), ConfigurationUtils.getBaseBundle().getPropertyValue("applitools.testName")); //TODO: check what to do with test names? should be on property? definable per check?
                }

                //Appium case:
                else{
                    driver = eye.open(driver, AppiumUtils.getAppiumDriver().getCapabilities().getCapability("appName").toString(), ConfigurationUtils.getBaseBundle().getPropertyValue("applitools.testName"));
                }
                //eye.setViewportSize(driver, new RectangleSize(412, 604));
            }
            try {
                eye.checkWindow(tag);
            }   catch (NewTestException ex) { System.out.println(ex.getMessage()); }
                catch (Exception ex) { ConsoleUtils.logError(ex.getMessage()); }
        }
        else {
            ConsoleUtils.logError("Eye object was not initiated!");
            String applitoolsAPIKey = ConfigurationManager.getBundle().getString("applitools.key");
            if (null != applitoolsAPIKey) {
                Eyes e = new Eyes();
                e.setApiKey(applitoolsAPIKey);
                String applitoolsMatchLevel = ConfigurationManager.getBundle().getString("applitools.matchLevel"); //Exact/Strict/Content/Layout/Layout2 (Recommended)
                if (null != applitoolsMatchLevel){
                    switch (applitoolsMatchLevel){
                        case "Exact" :
                            e.setMatchLevel(MatchLevel.EXACT);
                            break;
                        case "Strict" :
                            e.setMatchLevel(MatchLevel.STRICT);
                            break;
                        case "Content" :
                            e.setMatchLevel(MatchLevel.CONTENT);
                            break;
                        case "Layout" :
                            e.setMatchLevel(MatchLevel.LAYOUT);
                            break;
                        case "Layout2" :
                            e.setMatchLevel(MatchLevel.LAYOUT2);
                            break;
                        default :
                            System.out.println("Wrong Applitools Match Level configuration - eye object will use default match level.");
                            break;
                    }
                }
                ConfigurationManager.getBundle().setProperty("Eyes", e);
                try{
                    checkWindow(tag);
                } catch (NewTestException ex) { System.out.println(ex.getMessage()); }
            }
        }
    }

    public static void checkRegion(WebElement element, String tag){
        Object eyeObject = ConfigurationManager.getBundle().getObject("Eyes");

        if (null != eyeObject) {
            Eyes eye = (Eyes) eyeObject;
            WebDriver driver = new WebDriverTestBase().getDriver();
            if (!eye.getIsOpen()) {

                //Selenium case:
                if (ConfigurationUtils.getBaseBundle().getPropertyValue("driver.name").contains("Remote")) {
                    driver = eye.open(driver, driver.getCurrentUrl(), ConfigurationUtils.getBaseBundle().getPropertyValue("applitools.testName")); //TODO: check what to do with test names? should be on property? definable per check?
                }

                //Appium case:
                else {
                    driver = eye.open(driver, AppiumUtils.getAppiumDriver().getCapabilities().getCapability("appName").toString(), ConfigurationUtils.getBaseBundle().getPropertyValue("applitools.testName"));
                }
            }
            try {
                eye.checkRegion(element, tag);
            } catch (Exception ex) {
                ConsoleUtils.logError(ex.getMessage());
            }
        }
        else { ConsoleUtils.logError("Eye object was not initiated!"); }
    }


    public static void closeApplitoolsEyes(){
    //get eye
        Object eyeObject = ConfigurationManager.getBundle().getObject("Eyes");
        if (null != eyeObject){
            Eyes eye = (Eyes) eyeObject;
            try{
              eye.close();
            } finally {
                eye.abortIfNotClosed(); //TODO: check how abortIfNotClosed reacts to null eye object
            }
        }
    }
}
