package com.quantum.steps;

import com.qmetry.qaf.automation.step.QAFTestStepProvider;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebElement;
import cucumber.api.java.en.Then;
import com.quantum.utils.ApplitoolsUtils;
import org.openqa.selenium.WebElement;

/**
 * Created by lirona on 26/03/2017.
 */
@QAFTestStepProvider(prefix="cucmber")
public class ApplitoolsSteps {

    @Then("I check window \"(.*?)\"")
    public static void IcheckWindow(String tag){ ApplitoolsUtils.checkWindow(tag);}

    @Then("I check region \"(.*?)\"")
    public static void IcheckRegion(String element) {
        QAFExtendedWebDriver driver = new WebDriverTestBase().getDriver();
        try {
            QAFExtendedWebElement webElement = new QAFExtendedWebElement(element); //TODO: Get element from repository
            ApplitoolsUtils.checkRegion(webElement, element);
        } catch (Exception ex) {
            System.out.println("Locator for element : " + element + "could not be found, please make sure the element exists in your repository.");
        }
        //TODO: Implement - get the element from the locator (or element repository) and send it to CheckRegion function.
    }
}

