package com.qmetry.qaf.automation.ui.webdriver;

import java.net.URL;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CommandPayload;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.http.HttpClient;

import com.qmetry.qaf.automation.ui.JsToolkit;
import com.qmetry.qaf.automation.util.LocatorUtil;
import com.qmetry.qaf.automation.util.StringMatcher;
import com.quantum.utils.Appium2Capabilities;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;

public class QAFExtendedAndroidDriver extends AndroidDriver implements QAFWebDriver, QAFWebDriverCommandListener {
	
	private AppiumDriver underLayingDriver;
	private Appium2Capabilities capabilities;
	
	
	public QAFExtendedAndroidDriver(URL remoteAddress, HttpClient.Factory httpClientFactory, Capabilities capabilities) {
		
		super(remoteAddress,httpClientFactory,capabilities);
	}
	
	
	public WebDriver getUnderLayingDriver() {
		return underLayingDriver;
	}
	public Capabilities getCapabilities() {
		return capabilities;
	}


	@Override
	public String takeScreenShot() {
		return super.getScreenshotAs(OutputType.BASE64);
	}



	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public WebElement findElementByCustomStretegy(String stretegy, String loc) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public List<WebElement> findElementsByCustomStretegy(String stretegy, String loc) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void beforeCommand(QAFExtendedWebDriver driver, CommandTracker commandHandler) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void afterCommand(QAFExtendedWebDriver driver, CommandTracker commandHandler) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onFailure(QAFExtendedWebDriver driver, CommandTracker commandHandler) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void beforeInitialize(Capabilities desiredCapabilities) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onInitialize(QAFExtendedWebDriver driver) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onInitializationFailure(Capabilities desiredCapabilities, Throwable t) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<QAFWebElement> getElements(By by) {
		return (List<QAFWebElement>) (List<? extends WebElement>)super.findElements(by);
	}
	
	@Override
	public QAFExtendedWebElement findElement(By by) {		
		QAFExtendedWebElement element = (QAFExtendedWebElement) super.findElement(by);
		element.setBy(by);
		element.cacheable = true;
		return element;
	}

	@Override
	public QAFWebElement findElement(String locator) {
		return ElementFactory.$(locator);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<QAFWebElement> findElements(String locator) {
		return (List<QAFWebElement>) (List<? extends WebElement>) findElements(LocatorUtil.getBy(locator));
	}



	protected Response execute(CommandPayload payload) {
	    return execute(payload.getName(), payload.getParameters());
	}

	@Override
	public AppiumDriver assertExtensionExists(String extName) {
		return super.assertExtensionExists(extName);
	}
	
	@Override
	public AppiumDriver markExtensionAbsence(String arg0) {
		return super.markExtensionAbsence(arg0);
	}

//	@Override
//	public ExecutesMethod assertExtensionExists(String extName) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//
//	@Override
//	public ExecutesMethod markExtensionAbsence(String extName) {
//		// TODO Auto-generated method stub
//		return null;
//	}


	@Override
	public void waitForAjax(JsToolkit toolkit, long... timeout) {
		
	}


	@Override
	public void waitForAjax(long... timeout) {
		
	}


	@Override
	public void waitForAnyElementPresent(QAFWebElement... elements) {
		
	}


	@Override
	public void waitForAllElementPresent(QAFWebElement... elements) {
		
	}


	@Override
	public void waitForAnyElementVisible(QAFWebElement... elements) {
		
	}


	@Override
	public void waitForAllElementVisible(QAFWebElement... elements) {
		
	}


	@Override
	public void waitForWindowTitle(StringMatcher titlematcher, long... timeout) {
		
	}


	@Override
	public void waitForCurrentUrl(StringMatcher matcher, long... timeout) {
		
	}


	@Override
	public void waitForNoOfWindows(int count, long... timeout) {
		
	}


	@Override
	public boolean verifyTitle(StringMatcher text, long... timeout) {
		
		return false;
	}
	
	


	@Override
	public boolean verifyCurrentUrl(StringMatcher text, long... timeout) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean verifyNoOfWindows(int count, long... timeout) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void assertTitle(StringMatcher text, long... timeout) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void assertCurrentUrl(StringMatcher text, long... timeout) {
		// TODO Auto-generated method stub
		
	}

}
