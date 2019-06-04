package com.quantum.utils;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.JavascriptExecutor;

import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.TestBaseProvider;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;

@SuppressWarnings("rawtypes")
public class DriverUtils {

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getDataPasser() {
		return (Map<String, Object>) ConfigurationManager.getBundle().getObject("dataPasser" + Thread.currentThread());
	}

	public static void putDataPasser(String key, Object value) {
		getDataPasser().put(key, value);
	}

	public static Object getDataPasserValue(String key) {
		return getDataPasser().get(key);
	}

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

	public boolean isRunningAndroid() {
		if (getOS().equalsIgnoreCase("android")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isRunningIOS() {
		if (getOS().equalsIgnoreCase("ios")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Switches to either a new WebDriver session with a mobile device / web browser
	 * or returns to an open session. To use you must assign at least two parameter
	 * groups in your testNG config containing either capabilities assigned to a
	 * driver name or pointing to an env.resource file containing the set of
	 * capabilities associated with the driver name.
	 * <p>
	 * Example:
	 * <p>
	 * &lt;parameter name="perfecto.capabilities.platformName" value="Android" />
	 * <p>
	 * &lt;parameter name="perfecto2.env.resources"
	 * value="src/main/resources/android2" />
	 * 
	 * @param driverName
	 *            The name of the driver you are switching to "perfecto" or
	 *            "perfecto2" or in case of browser drivers the parameter value should be "perfectoRemote" or "perfecto2Remote"
	 */
	public static void switchToDriver(String driverName) {
		if(driverName.contains("Driver")) {
			driverName = driverName.substring(0,driverName.lastIndexOf("Driver"));
		}
		TestBaseProvider.instance().get().setDriver(driverName + "Driver");
		String envResources = ConfigurationManager.getBundle()
				.getString(driverName.replaceAll("(?i)remote", "") + ".env.resources");
		ConfigurationManager.getBundle().setProperty("env.resources", envResources);
		
		ConfigurationUtils.setActualDeviceCapabilities(getDriver().getCapabilities().asMap());
		
	}

	private String getOS() {
		Map<String, String> params = new HashMap<>();
		params.put("property", "os");
		String properties = (String) DriverUtils.getDriver().executeScript("mobile:handset:info", params);
		return properties;
	}

	/**
	 * This method will delete the browser cookies for the given url. It will also
	 * clear the local storage and the session storage using the javascript executor
	 * in the browser window
	 * 
	 * @param url
	 */
	public static void clearBrowserCacheAndCookies(String url) {
		DeviceUtils.getQAFDriver().get(url);
		DeviceUtils.getQAFDriver().manage().deleteAllCookies();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		JavascriptExecutor js = (JavascriptExecutor) DeviceUtils.getQAFDriver();
		// System.out.println("--Local Storage Clear Start--");
		js.executeScript(String.format("window.localStorage.clear();"));
		js.executeScript(String.format("window.sessionStorage.clear();"));
		// System.out.println("--Local Storage Clear End--");
	}

}
