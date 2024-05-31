package com.quantum.utils;

import java.util.Map;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.TestBaseProvider;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;

import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;

/**
* DriverUtils class contains set of utility methods, which help in interacting with 
* the underlying driver.
*/

@SuppressWarnings({ "rawtypes", "deprecation" })
public class DriverUtils {
	
	/**
	 * <p>Method to retrieve collection of Data to be passed between Scenarios.
	 * </p>
	 * @return Collection of data needed to be passed from 
	 * one scenario to the other.
	 */
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getDataPasser() {
		return (Map<String, Object>) ConfigurationManager.getBundle().getObject("dataPasser" + Thread.currentThread());
	}

	/**
	 * <p>Method to Put data in collection of Data to be passed between Scenarios.
	 * </p>
	 * @param key the key representing the data.
	 * @param value the actual value to be stored.
	 */
	public static void putDataPasser(String key, Object value) {
		getDataPasser().put(key, value);
	}

	/**
	 * <p>Method to Get data from collection of Data to be passed between Scenarios.
	 * </p>
	 * @param key the key representing the data.
	 * @return value of the data associated with given key.
	 */
	public static Object getDataPasserValue(String key) {
		return getDataPasser().get(key);
	}

	/**
	 * <p>Method to Get Appium Driver initialized in current test.
	 * </p>
	 * @return Instance of type  {@link RemoteWebDriver}.
	 * 
	 */
	public static RemoteWebDriver getAppiumDriver() {

		WebDriver driver = getDriver().getUnderLayingDriver();

		if(driver instanceof RemoteWebDriver){
			return (RemoteWebDriver) driver;
		}

		if(driver instanceof AndroidDriver){
			return (AndroidDriver) driver;
		}

		if(driver instanceof IOSDriver){
			return (IOSDriver) driver;
		}
		return null ;
	}

	/**
	 * <p>Method to create TouchAction Object instance.
	 * </p>
	 * @return Instance of type  {@link io.appium.java_client.TouchAction}
	 * 
	 */
	@Deprecated
	public static TouchAction getTouchAction() {

		return AppiumUtils.getTouchAction();
//		return new TouchAction(getAppiumDriver());
	}

	/**
	 * <p>Method to Get IOS Driver initialized in current test.
	 * </p>
	 * @return Instance of type  {@link io.appium.java_client.ios.IOSDriver}
	 * 
	 */
	public static IOSDriver getIOSDriver() {
		return (IOSDriver) getAppiumDriver();
	}

	/**
	 * <p>Method to Get Android Driver initialized in current test.
	 * </p>
	 * @return Instance of type {@link io.appium.java_client.android.AndroidDriver}
	 * 
	 */
	public static AndroidDriver getAndroidDriver() {


		return (AndroidDriver) getAppiumDriver();
	}

	/**
	 * <p>Method to Get QAFExtendedWebDriver initialized in current test.
	 * </p>
	 * @return Instance of type {@link com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver}
	 * 
	 */
	public static QAFExtendedWebDriver getDriver() {
		return new WebDriverTestBase().getDriver();
	}

	@Deprecated
	/**
	 * <p>Method to check whether the current execution is happening on Android device or not.
	 * </p>
	 * @return boolean value.
	 */
	public boolean isRunningAndroid() {
		if (getOS().equalsIgnoreCase("android")) {
			return true;
		} else {
			return false;
		}
	}

	@Deprecated
	/**
	 * <p>Method to check whether the current execution is happening on IOS device or not.
	 * </p>
	 * @return boolean value.
	 */
	public boolean isRunningIOS() {
		if (getOS().equalsIgnoreCase("ios")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * <p>Method to check whether the current execution is happening on Android device or not.
	 * </p>
	 * @return boolean value.
	 */
	public static boolean isAndroid() {
		return Platform.ANDROID.name().equalsIgnoreCase(getOS());
	}

	/**
	 * <p>Method to check whether the current execution is happening on IOS device or not.
	 * </p>
	 * @return boolean value.
	 */
	public static boolean isIOS() {
		return Platform.IOS.name().equalsIgnoreCase(getOS());
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
	 * &lt;parameter name="perfecto.capabilities.platformName" value="Android" /&gt;
	 * <p>
	 * &lt;parameter name="perfecto2.env.resources"
	 * value="src/main/resources/android2" /&gt;
	 * 
	 * @param driverName The name of the driver you are switching to "perfecto" or
	 *                   "perfecto2" or in case of browser drivers the parameter
	 *                   value should be "perfectoRemote" or "perfecto2Remote"
	 */
	public static void switchToDriver(String driverName) {
		if (driverName.contains("Driver")) {
			driverName = driverName.substring(0, driverName.lastIndexOf("Driver"));
		}
		
		TestBaseProvider.instance().get().setDriver(driverName + "Driver");
		DeviceUtils.getQAFDriver();
		String envResources = ConfigurationManager.getBundle()
				.getString(driverName.replaceAll("(?i)remote", "") + ".env.resources");
		ConfigurationManager.getBundle().setProperty("env.resources", envResources);

		ConfigurationUtils.setActualDeviceCapabilities(getDriver().getCapabilities().asMap());

	}
	
	/**
	 * Internal method to get the targeted Device/VM OS on which current test is executing.
	 * 
	 * @return String representing the current OS.
	 */

	private static String getOS() {

		
		WebDriver driver = DriverUtils.getDriver();
		
		if (null!=driver) {
			
			if(driver instanceof QAFExtendedWebDriver) {
				QAFExtendedWebDriver qafDriver = (QAFExtendedWebDriver) driver;
				Capabilities cap = qafDriver.getCapabilities();
				String platformName = cap.getPlatformName().name();
				return platformName;
			}else {
				return "";
			}

		} else {
			driver = DriverUtils.getAppiumDriver();
			
			if(null != driver ) {
				if(driver instanceof AndroidDriver) {
					return Platform.ANDROID.name();
				}else {
					if(driver instanceof IOSDriver) {
						return Platform.IOS.name();
					}
				}
			}
			
			return "";
		}
	}

	/**
	 * This method will delete the browser cookies for the given url. It will also
	 * clear the local storage and the session storage using the javascript executor
	 * in the browser window
	 * 
	 * @param url String representing the Application URL.
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
