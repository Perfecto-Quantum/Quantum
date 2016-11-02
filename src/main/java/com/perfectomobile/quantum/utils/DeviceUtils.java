/**
 * 
 */
package com.perfectomobile.quantum.utils;

import com.perfectomobile.httpclient.device.DeviceParameter;
import com.perfectomobile.httpclient.device.DeviceResult;
import com.perfectomobile.quantum.steps.PerfectoQAFSteps;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.util.Validator;
import org.hamcrest.Matchers;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

/**
 * @author chirag.jayswal
 */
public class DeviceUtils {

	private static final String REPOSITORY_KEY = "perfecto.repository.folder";

	public static boolean verifyVisualText(RemoteWebDriver driver, String text) {
		return Validator.verifyThat(isText(driver, text, null), Matchers.equalTo("true"));
	}

	public static void assertVisualText(RemoteWebDriver driver, String text) {
		Validator.assertThat("Text: \"" + text + "\" should be present",
				isText(driver, text, null), Matchers.equalTo("true"));
	}

	public static void installApp(String filePath, RemoteWebDriver d,
			boolean shouldInstrument) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("file", filePath);
		if (shouldInstrument) {
			params.put("instrument", "instrument");
		}
		d.executeScript("mobile:application:install", params);
	}

	public void installAppOnDevice(DeviceResult device) {
		getBundle().setProperty("driver.name", "appiumRemoteDriver");

		getBundle().setProperty("driver.capabilities.deviceName", device.getResponseValue(DeviceParameter.DEVICE_ID));
		QAFExtendedWebDriver driver = new WebDriverTestBase().getDriver();

		PerfectoQAFSteps.installApp(REPOSITORY_KEY,
				getBundle().getString("app.instrumentation", "noinstrument"));
		driver.quit();
	}

	private static Map<String, String> getAppParams(String app, String by) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(by, app);
		return params;
	}

	// by = "name" or "identifier"
	public static void startApp(RemoteWebDriver driver, String app, String by) {
		driver.executeScript("mobile:application:open", getAppParams(app, by));
	}
	// by = "name" or "identifier"
	public static void closeApp(RemoteWebDriver driver, String app, String by) {
		driver.executeScript("mobile:application:close", getAppParams(app, by));
	}
	// by = "name" or "identifier"
	public static void cleanApp(RemoteWebDriver driver, String app, String by) {
		driver.executeScript("mobile:application:clean", getAppParams(app, by));
	}
	// by = "name" or "identifier"
	public static void uninstallApp(RemoteWebDriver driver, String app, String by) {
		driver.executeScript("mobile:application:uninstall", getAppParams(app, by));
	}

	public static void uninstallAllApps(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		driver.executeScript("mobile:application:reset", params);
	}

	public static String getAppInfo(RemoteWebDriver driver, String property) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("property", property);
		return (String) driver.executeScript("mobile:application:info", params);
	}

	public static boolean verifyAppInfo(RemoteWebDriver driver, String propertyName,
			String propertyValue) {
		return Validator.verifyThat(getAppInfo(driver, propertyName),
				Matchers.equalTo(propertyValue));
	}

	public static void assertAppInfo(RemoteWebDriver driver, String propertyName,
			String propertyValue) {
		String appOrientation = getAppInfo(driver, propertyName);
		Validator.assertThat(appOrientation, Matchers.equalTo(propertyValue));
	}

	public static void switchToContext(RemoteWebDriver driver, String context) {
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(driver);
		Map<String, String> params = new HashMap<String, String>();
		params.put("name", context);
		executeMethod.execute(DriverCommand.SWITCH_TO_CONTEXT, params);
	}

	public static void waitForPresentTextVisual(RemoteWebDriver driver, String text,
			int seconds) {
		isText(driver, text, seconds);
	}

	public static void waitForPresentImageVisual(RemoteWebDriver driver, String image,
			int seconds) {
		isImg(driver, image, seconds);
	}

	private static String isImg(RemoteWebDriver driver, String img, Integer timeout) {
		String context = getCurrentContext(driver);
		switchToContext(driver, "VISUAL");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("content", img);
		params.put("measurement", "accurate");
		params.put("source", "primary");
		params.put("threshold", "90");
		params.put("timeout", timeout);
		params.put("match", "bounded");
		params.put("imageBounds.needleBound", 25);
		Object result = driver.executeScript("mobile:checkpoint:image", params);
		switchToContext(driver, context);
		return result.toString();
	}

	public static void assertVisualImg(RemoteWebDriver driver, String img) {
		Validator.assertThat("Image " + img + " should be visible",
				isImg(driver, img, 180), Matchers.equalTo("true"));
	}

	public static boolean verifyVisualImg(RemoteWebDriver driver, String img) {
		return Validator.verifyThat(isImg(driver, img, 180), Matchers.equalTo("true"));
	}

	private static String isText(RemoteWebDriver driver, String text, Integer timeout) {
		String context = getCurrentContext(driver);
		switchToContext(driver, "VISUAL");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("content", text);
		if (timeout != null) {
			params.put("timeout", timeout);
		}
		Object result = driver.executeScript("mobile:checkpoint:text", params);
		switchToContext(driver, context);
		return result.toString();
	}

	/**
	 * @param driver
	 * @return the current context - "NATIVE_APP", "WEBVIEW", "VISUAL"
	 */
	public static String getCurrentContext(RemoteWebDriver driver) {
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(driver);
		return (String) executeMethod.execute(DriverCommand.GET_CURRENT_CONTEXT_HANDLE,
				null);
	}

	// device utils

	/**
	 * Clicks on a single or sequence of physical device keys.
	 * Mouse-over the device keys to identify them, then input into the Keys
	 * parameter according to the required syntax.
	 * <p>
	 * Common keys include:
	 * LEFT, RIGHT, UP, DOWN, OK, BACK, MENU, VOL_UP, VOL_DOWN, CAMERA, CLEAR.
	 * <p>
	 * The listed keys are not necessarily supported by all devices. The
	 * available keys depend on the device.
	 *
	 * @param driver
	 *            the RemoteWebDriver
	 * @param keySequence
	 *            the single or sequence of keys to click
	 */
	public static void pressKey(RemoteWebDriver driver, String keySequence) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("keySequence", keySequence);
		driver.executeScript("mobile:presskey", params);
	}

	/**
	 * Performs the swipe gesture according to the start and end coordinates.
	 * <p>
	 * Example swipe left:<br/>
	 * start: 60%,50% end: 10%,50%
	 *
	 * @param driver
	 *            the RemoteWebDriver
	 * @param start
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 * @param end
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 */
	public static void swipe(RemoteWebDriver driver, String start, String end) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("start", start);
		params.put("end", end);

		driver.executeScript("mobile:touch:swipe", params);

	}

	/**
	 * Performs the touch gesture according to the point coordinates.
	 * 
	 * @param driver
	 *            the RemoteWebDriver
	 * @param point
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 */
	public static void touch(RemoteWebDriver driver, String point) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("location", point); // 50%,50%

		driver.executeScript("mobile:touch:tap", params);
	}

	/**
	 * Hides the virtual keyboard display.
	 * 
	 * @param driver
	 *            the RemoteWebDriver
	 */
	public static void hideKeyboard(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("mode", "off");

		driver.executeScript("mobile:keyboard:display", params);

	}

	/**
	 * Rotates the device to landscape, portrait, or its next state.
	 * 
	 * @param driver
	 *            the RemoteWebDriver
	 * @param restValue
	 *            the "next" operation, or the "landscape" or "portrait" state.
	 * @param by
	 *            the "state" or "operation"
	 */
	// TODO: need additional description.
	public static void rotateDevice(RemoteWebDriver driver, String restValue, String by) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(by, restValue);
		driver.executeScript("mobile:handset:rotate", params);
	}

	// by = "address" or "coordinates"
	public static void setLocation(RemoteWebDriver driver, String location, String by) {

		Map<String, String> params = new HashMap<String, String>();
		params.put(by, location);

		driver.executeScript("mobile:location:set", params);
	}

	public static void assertLocation(RemoteWebDriver driver, String location) {
		String deviceLocation = getDeviceLocation(driver);
		Validator.assertThat("The device location", deviceLocation,
				Matchers.equalTo(location));

	}

	public static boolean verifyLocation(RemoteWebDriver driver, String location) {
		String deviceLocation = getDeviceLocation(driver);
		return Validator.verifyThat(deviceLocation, Matchers.equalTo(location));
	}

	public static String getDeviceLocation(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		return (String) driver.executeScript("mobile:location:get", params);
	}

	public static void resetLocation(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		driver.executeScript("mobile:location:reset", params);
	}

	public static void goToHomeScreen(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("target", "All");

		driver.executeScript("mobile:handset:ready", params);
	}

	public static void lockDevice(RemoteWebDriver driver, int sec) {
		Map<String, Integer> params = new HashMap<String, Integer>();
		params.put("timeout", sec);

		driver.executeScript("mobile:screen:lock", params);
	}

	public static void setTimezone(RemoteWebDriver driver, String timezone) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("timezone", timezone);

		driver.executeScript("mobile:timezone:set", params);
	}

	public static String getTimezone(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();

		return (String) driver.executeScript("mobile:timezone:get", params);
	}

	public static void assertTimezone(RemoteWebDriver driver, String timezone) {
		String deviceTimezone = getTimezone(driver);
		Validator.assertThat("The device timezone", deviceTimezone,
				Matchers.equalTo(timezone));
	}

	public static boolean verifyTimezone(RemoteWebDriver driver, String timezone) {
		return Validator.verifyThat(getTimezone(driver), Matchers.equalTo(timezone));
	}

	public static void resetTimezone(RemoteWebDriver driver) {
		Map<String, String> params = new HashMap<String, String>();
		driver.executeScript("mobile:timezone:reset", params);
	}

	public static void takeScreenshot(RemoteWebDriver driver, String repositoryPath,
			boolean shouldSave) {
		Map<String, String> params = new HashMap<String, String>();
		if (shouldSave) {
			params.put("key", repositoryPath);
		}
		driver.executeScript("mobile:screen:image", params);
	}
}
