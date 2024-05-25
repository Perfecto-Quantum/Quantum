package com.qmetry.qaf.automation.ui.webdriver;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.qmetry.qaf.automation.ui.JsToolkit;
import com.qmetry.qaf.automation.ui.UiDriver;
import com.qmetry.qaf.automation.util.StringMatcher;

/*
 * 
 * Note: Moved the Interface out of QAF to handle deprecation 
 * of HasInputDevices class.
 * 
 * 
 */

public interface QAFWebDriver extends UiDriver, WebDriver, TakesScreenshot, JavascriptExecutor, FindsByCustomStretegy, 
 HasCapabilities {

public QAFWebElement findElement(By by);

public List<QAFWebElement> getElements(By by);

public QAFWebElement findElement(String locator);

public List<QAFWebElement> findElements(String locator);

//public Mouse getMouse();
//
//public Keyboard getKeyboard();
//
//public TouchScreen getTouchScreen();


public void waitForAjax(JsToolkit toolkit, long... timeout);

public void waitForAjax(long... timeout);

public void waitForAnyElementPresent(QAFWebElement... elements);

public void waitForAllElementPresent(QAFWebElement... elements);

public void waitForAnyElementVisible(QAFWebElement... elements);

public void waitForAllElementVisible(QAFWebElement... elements);


public void waitForWindowTitle(StringMatcher titlematcher, long... timeout);

public void waitForCurrentUrl(StringMatcher matcher, long... timeout) ;

public void waitForNoOfWindows(int count, long... timeout);

public boolean verifyTitle(StringMatcher text, long... timeout);

public boolean verifyCurrentUrl(StringMatcher text, long... timeout);

public boolean verifyNoOfWindows(int count, long... timeout);

public void assertTitle(StringMatcher text, long... timeout);


public void assertCurrentUrl(StringMatcher text, long... timeout);

}
