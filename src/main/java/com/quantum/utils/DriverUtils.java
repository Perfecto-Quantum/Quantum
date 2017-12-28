package com.quantum.utils;

import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;

@SuppressWarnings("rawtypes")
public class DriverUtils {

	public static AppiumDriver getAppiumDriver() {
		return (AppiumDriver) getDriver().getUnderLayingDriver();
	}

	public static TouchAction getTouchAction() {
		return new TouchAction(getAppiumDriver());
	}

	public static IOSDriver getIOSDriver() {
		return (IOSDriver) getAppiumDriver();
	}

	public static AndroidDriver getAndroidDriver() {
		return (AndroidDriver) getAppiumDriver();
	}

	public static QAFExtendedWebDriver getDriver() {
		return new WebDriverTestBase().getDriver();
	}

	public static boolean isRunningOnIOS() {
		return (null != getDriver().getCapabilities().getCapability("platformName")) && (ConfigurationManager.getBundle()
				.getString("driver.capabilities.platformName").toLowerCase().contains("ios") || ConfigurationManager.getBundle()
				.getString("perfecto.capabilities.platformName").toLowerCase().contains("ios"));
	}

	public static boolean isRunningOnAndroid() {
		return (null != getDriver().getCapabilities().getCapability("platformName")) && (ConfigurationManager.getBundle()
				.getString("driver.capabilities.platformName").toLowerCase().contains("android") || ConfigurationManager.getBundle()
				.getString("perfecto.capabilities.platformName").toLowerCase().contains("android"));

	}


}
