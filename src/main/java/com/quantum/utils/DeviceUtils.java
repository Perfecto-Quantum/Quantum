/**
/**
 *
 */
package com.quantum.utils;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteExecuteMethod;

import com.perfectomobile.httpclient.device.DeviceParameter;
import com.perfectomobile.httpclient.device.DeviceResult;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.util.Validator;

public class DeviceUtils {

	private static final String REPOSITORY_KEY = "perfecto.repository.folder";

	public static QAFExtendedWebDriver getQAFDriver() {
		return new WebDriverTestBase().getDriver();
	}

	public static boolean verifyVisualText(String text) {
		return Validator.verifyThat("Text: \"" + text + "\" should be present",
				isText(text, null), Matchers.equalTo("true"));
	}

	public static void assertVisualText(String text) {
		Validator.assertThat("Text: \"" + text + "\" must be present",
				isText(text, 60), Matchers.equalTo("true"));
	}

	public static void installApp(String filePath, boolean shouldInstrument) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("file", filePath);
		if (shouldInstrument) {
			params.put("instrument", "instrument");
		}
		getQAFDriver().executeScript("mobile:application:install", params);
	}

	public void installAppOnDevice(DeviceResult device) {
		getBundle().setProperty("getQAFDriver().name", "appiumRemotegetQAFDriver()");

		getBundle().setProperty("getQAFDriver().capabilities.deviceName", device.getResponseValue(DeviceParameter.DEVICE_ID));

		installApp(REPOSITORY_KEY, getBundle().getString("app.instrumentation", "noinstrument"));

		getQAFDriver().quit();

	}

	public void installApp(String repoKey, String instrument) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("file", getBundle().getString(repoKey,repoKey));
		params.put("instrument", getBundle().getString(instrument, instrument));

		String resultStr = (String) getQAFDriver().executeScript("mobile:application:install", params);
		System.out.println(resultStr);
	}


	private static Map<String, String> getAppParams(String app, String by) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(by, app);
		return params;
	}

	// by = "name" or "identifier"
	public static void startApp(String app, String by) {
		getQAFDriver().executeScript("mobile:application:open", getAppParams(app, by));
	}
	// by = "name" or "identifier"
	public static void closeApp(String app, String by) { closeApp(app, by, false);
	}
	// by = "name" or "identifier"
	public static void closeApp(String app, String by, boolean ignoreExceptions) {

		try {
			getQAFDriver().executeScript("mobile:application:close", getAppParams(app, by));
		} catch (Exception e){
			if (!ignoreExceptions) {

				throw e;
			}


		}
	}

	// by = "name" or "identifier" with driver
	public static void closeApp(String app, String by, boolean ignoreExceptions, QAFExtendedWebDriver driver) {

		try {
			driver.executeScript("mobile:application:close", getAppParams(app, by));
		} catch (Exception e){
			if (!ignoreExceptions) {

				throw e;
			}


		}
	}

	// by = "name" or "identifier"
	public static void cleanApp(String app, String by) {
		getQAFDriver().executeScript("mobile:application:clean", getAppParams(app, by));
	}
	// by = "name" or "identifier"
	public static void uninstallApp(String app, String by) {
		getQAFDriver().executeScript("mobile:application:uninstall", getAppParams(app, by));
	}

	public static void uninstallAllApps() {
		Map<String, String> params = new HashMap<String, String>();
		getQAFDriver().executeScript("mobile:application:reset", params);
	}

	public static String getAppInfo(String property) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("property", property);
		return (String) getQAFDriver().executeScript("mobile:application:info", params);
	}

	public static boolean verifyAppInfo(String propertyName,
			String propertyValue) {
		return Validator.verifyThat(propertyName + " should be " + propertyValue,
				getAppInfo(propertyName), Matchers.equalTo(propertyValue));
	}

	public static void assertAppInfo(String propertyName,
			String propertyValue) {
		String appOrientation = getAppInfo(propertyName);
		Validator.assertThat(propertyName + " must be " + propertyValue,
				appOrientation, Matchers.equalTo(propertyValue));
	}

	public static void switchToContext(String context) {
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(getQAFDriver());
		Map<String, String> params = new HashMap<String, String>();
		params.put("name", context);
		executeMethod.execute(DriverCommand.SWITCH_TO_CONTEXT, params);
	}

	public static void waitForPresentTextVisual(String text,
			int seconds) {
		 Validator.verifyThat("Text: \"" + text + "\" should be present after " + seconds + "seconds",
				isText(text, seconds), Matchers.equalTo("true"));
	}

	public static void waitForPresentImageVisual(String image,
			int seconds) {
		Validator.verifyThat("Image: \"" + image + "\" should be visible after " + seconds + "seconds",
				isImg(image, seconds), Matchers.equalTo("true"));
	}

	private static String isImg(String img, Integer timeout) {
		String context = getCurrentContext();
		switchToContext("VISUAL");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("content", img);
		params.put("measurement", "accurate");
		params.put("source", "primary");
		params.put("threshold", "90");
		params.put("timeout", timeout);
		params.put("match", "bounded");
		params.put("imageBounds.needleBound", 25);
		Object result = getQAFDriver().executeScript("mobile:checkpoint:image", params);
		switchToContext(context);
		return result.toString();
	}

	public static void assertVisualImg(String img) {
		Validator.assertThat("Image " + img + " must be visible",
				isImg(img, 180), Matchers.equalTo("true"));
	}

	public static boolean verifyVisualImg(String img) {
		return Validator.verifyThat("Image " + img + " should be visible",
				isImg(img, 180), Matchers.equalTo("true"));
	}
	/**
	 * Visual Text Checkpoint based on the text sent in and a threshold of 100
	 * @param text - Text to compare
	 * @param timeout - timeout amount to search
	 * @return true if found or false if not found
	 */
	private static String isText(String text, Integer timeout) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("content", text);
		if (timeout != null) {
			params.put("timeout", timeout);
		}
		params.put("threshold", "100");
		Object result = getQAFDriver().executeScript("mobile:checkpoint:text", params);
		return result.toString();
	}

	/**
	 * @return the current context - "NATIVE_APP", "WEBVIEW", "VISUAL"
	 */
	public static String getCurrentContext() {
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(getQAFDriver());
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
	 * @param keySequence
	 *            the single or sequence of keys to click
	 */
	public static void pressKey(String keySequence) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("keySequence", keySequence);
		getQAFDriver().executeScript("mobile:presskey", params);
	}

	/**
	 * Performs the swipe gesture according to the start and end coordinates.
	 * <p>
	 * Example swipe left:<br/>
	 * start: 60%,50% end: 10%,50%
	 *
	 * @param start
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 * @param end
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 */
	public static void swipe(String start, String end) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("start", start);
		params.put("end", end);

		getQAFDriver().executeScript("mobile:touch:swipe", params);

	}

	/**
	 * Performs the tap gesture according to location coordinates with durations in seconds.
	 * <p>
	 *
	 * @param point
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 *
	 * @param seconds
	 *            The duration, in seconds, for performing the touch operation.
	 */
	public static void longTouch(String point, int seconds) {

		Map<String, Object> params = new HashMap<>();
		params.put("location", point);
		params.put("operation", "single");
		params.put("duration", seconds);
		new WebDriverTestBase().getDriver().executeScript("mobile:touch:tap", params);
	}

	/**
	 * Performs the touch gesture according to the point coordinates.
	 * 
	 * @param point
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 */
	public static void touch(String point) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("location", point); // 50%,50%

		getQAFDriver().executeScript("mobile:touch:tap", params);
	}

	/**
	 * Performs the double touch gesture according to the point coordinates.
	 *
	 * @param point
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 */
	public static void doubleTouch(String point) {
		Map<String, Object> params = new HashMap<>();
		params.put("location",point);
		params.put("operation", "double");
		getQAFDriver().executeScript("mobile:touch:tap", params);
	}

	/**
	 * Performs the long touch gesture according to the point coordinates.
	 *
	 * @param point
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended).
	 */
	public static void longTouch(String point) {
		Map<String, Object> params = new HashMap<>();
		params.put("location",point);
		params.put("operation", "double");
		getQAFDriver().executeScript("mobile:touch:tap", params);
	}

	/**
	 * Hides the virtual keyboard display.
	 * 
	 */
	public static void hideKeyboard() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("mode", "off");

		getQAFDriver().executeScript("mobile:keyboard:display", params);

	}

	/**
	 * Rotates the device to landscape, portrait, or its next state.
	 * 
	 * @param restValue
	 *            the "next" operation, or the "landscape" or "portrait" state.
	 * @param by
	 *            the "state" or "operation"
	 */
	// TODO: need additional description.
	public static void rotateDevice(String restValue, String by) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(by, restValue);
		getQAFDriver().executeScript("mobile:handset:rotate", params);
	}

	// by = "address" or "coordinates"
	public static void setLocation(String location, String by) {

		Map<String, String> params = new HashMap<String, String>();
		params.put(by, location);

		getQAFDriver().executeScript("mobile:location:set", params);
	}

	public static void assertLocation(String location) {
		String deviceLocation = getDeviceLocation();
		Validator.assertThat("The device location", deviceLocation,
				Matchers.equalTo(location));

	}

	public static boolean verifyLocation(String location) {
		String deviceLocation = getDeviceLocation();
		return Validator.verifyThat("The device location",
				deviceLocation, Matchers.equalTo(location));
	}

	public static String getDeviceLocation() {
		Map<String, String> params = new HashMap<String, String>();
		return (String) getQAFDriver().executeScript("mobile:location:get", params);
	}

	public static void resetLocation() {
		Map<String, String> params = new HashMap<String, String>();
		getQAFDriver().executeScript("mobile:location:reset", params);
	}

	public static void goToHomeScreen() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("target", "All");

		getQAFDriver().executeScript("mobile:handset:ready", params);
	}

	public static void lockDevice(int sec) {
		Map<String, Integer> params = new HashMap<String, Integer>();
		params.put("timeout", sec);

		getQAFDriver().executeScript("mobile:screen:lock", params);
	}

	public static void setTimezone(String timezone) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("timezone", timezone);

		getQAFDriver().executeScript("mobile:timezone:set", params);
	}

	public static String getTimezone() {
		Map<String, String> params = new HashMap<String, String>();

		return (String) getQAFDriver().executeScript("mobile:timezone:get", params);
	}

	public static void assertTimezone(String timezone) {
		String deviceTimezone = getTimezone();
		Validator.assertThat("The device timezone", deviceTimezone,
				Matchers.equalTo(timezone));
	}

	public static boolean verifyTimezone(String timezone) {
		return Validator.verifyThat("The device timezone should be " + timezone,
				getTimezone(), Matchers.equalTo(timezone));
	}

	public static void resetTimezone() {
		Map<String, String> params = new HashMap<String, String>();
		getQAFDriver().executeScript("mobile:timezone:reset", params);
	}

	public static void takeScreenshot(String repositoryPath,
			boolean shouldSave) {
		Map<String, String> params = new HashMap<String, String>();
		if (shouldSave) {
			params.put("key", repositoryPath);
		}
		getQAFDriver().executeScript("mobile:screen:image", params);
	}

	// by = "name" or "identifier"
	public static void startImageInjection(String repositoryFile, String app, String by) {
		Map<String, Object> params = new HashMap<>();
		params.put("repositoryFile", repositoryFile);
		params.put(by, app);
		getQAFDriver().executeScript("mobile:image.injection:start", params);

	}

	public static void stopImageInjection() {
		Map<String, Object> params = new HashMap<>();
		new WebDriverTestBase().getDriver().executeScript("mobile:image.injection:stop", params);
	}

	public static void setFingerprint(String by, String identifier, String resultAuth, String errorType) {

		Map<String, Object> params = new HashMap<>();
		params.put(by, identifier);
		params.put("resultAuth", resultAuth);
		params.put("errorType", errorType);
		getQAFDriver().executeScript("mobile:fingerprint:set", params);
	}

	public static void setSensorAuthentication(String by, String identifier, String resultAuth, String errorType) {

		Map<String, Object> params = new HashMap<>();
		params.put(by, identifier);
		params.put("resultAuth", resultAuth);
		params.put("errorType", errorType);
		getQAFDriver().executeScript("mobile:sensorAuthentication:set", params);
	}

	public static void generateHAR()
	{
		Map<String, Object> params = new HashMap<>();
		params.put("generateHarFile", "true");
		getQAFDriver().executeScript("mobile:vnetwork:start", params);

	}

	public static void stopGenerateHAR()
	{
		Map<String, Object> params = new HashMap<>();
		getQAFDriver().executeScript("mobile:vnetwork:stop", params);

	}

	public static void audioInject(String file)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("key", file);
		getQAFDriver().executeScript("mobile:audio:inject", params);

	}

	public static String getDeviceProperty(String property) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("property",property);
		return  (String) getQAFDriver().executeScript("mobile:handset:info", params);

	}


}