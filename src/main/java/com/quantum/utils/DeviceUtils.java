/**
/**
 *
 */
package com.quantum.utils;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.hamcrest.Matchers;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.remote.RemoteWebElement;

//import com.perfectomobile.httpclient.device.DeviceParameter;
//import com.perfectomobile.httpclient.device.DeviceResult;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebElement;
import com.qmetry.qaf.automation.util.Validator;
import com.quantum.axe.AxeHelper;

import io.appium.java_client.AppiumDriver;

/**
* DeviceUtils class contains set of utility methods, which help in interacting with 
* the test device on Perfecto Continues testing platform.
*/

public class DeviceUtils {

//	private static final String REPOSITORY_KEY = "perfecto.repository.folder";

	/**
	 * Utility method to get {@link com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver}
	 * 
	 * @return Instance of {@link com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver}
	 */
	
	public static QAFExtendedWebDriver getQAFDriver() {
		return new WebDriverTestBase().getDriver();
	}

	/**
	 * Utility method Verify Text using Perfecto's Visual testing.
	 * 
	 * @param text - Text to verify.
	 * 
	 * @return Boolean representing whether the text is available or not.
	 */
	public static boolean verifyVisualText(String text) {
		String message = String.format("Text: '%s' should be present", text);
		String result = isText(text, null);
		return Validator.verifyThat(message,result ,Matchers.equalTo("true"));
	}

	/**
	 * Utility method Assert Text using Perfecto's Visual testing.
	 * 
	 * @param text - Text to verify.
	 * 
	 */
	public static void assertVisualText(String text) {
		String message = String.format("Text: '%s' must be present", text);
		String result = isText(text, 60);
		Validator.assertThat(message,result, Matchers.equalTo("true"));
	}
	
	/**
	 *
	 *	Install app in the device using Perfecto command
	 *
	 * @param filePath          - The full repository path, including directory and
	 *                         file name, where to locate the application.
	 * @param shouldInstrument  - Boolean value representing whether to instrument or not.
	 * 
	 */

	public static void installApp(String filePath, boolean shouldInstrument) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("file", filePath);
		if (shouldInstrument) {
			params.put("instrument", "instrument");
		}
		getQAFDriver().executeScript("mobile:application:install", params);
	}

	/**
	 *
	 *	Install app in the device using Perfecto command
	 *
	 * @param repoKey     - Key used in Configuration manager getbundle to retrieve App Perfecto repo path.
	 * @param instrument  - String value representing whether to instrument or not.
	 * 
	 */
	public static void installApp(String repoKey, String instrument) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("file", getBundle().getString(repoKey, repoKey));
		params.put("instrument", getBundle().getString(instrument, instrument));

		String resultStr = (String) getQAFDriver().executeScript("mobile:application:install", params);
		System.out.println(resultStr);
	}

	/**
	 *
	 * @param repoKey          - The full repository path, including directory and
	 *                         file name, where to locate the application.
	 * @param instrument       - Perform instrumentation.. Possible values
	 *                         :noinstrument (default) | instrument.
	 * @param sensorInstrument - Enable device sensor. Possible values: nosensor
	 *                         (default)| sensor.
	 * @param resignEnable     - Re-sign the app with a Perfecto code-signing
	 *                         certificate that has the cloud device provisioned.
	 *                         Possible values false (default) | true.
	 */
	public static void installApp(String repoKey, String instrument, String sensorInstrument, String resignEnable) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("file", getBundle().getString("repoKey", repoKey));
		if (!instrument.isEmpty())
			params.put("instrument", instrument);
		if (!sensorInstrument.isEmpty())
			params.put("sensorInstrument", sensorInstrument);
		if (!resignEnable.isEmpty())
			params.put("resign", resignEnable);

		String resultStr = (String) getQAFDriver().executeScript("mobile:application:install", params);
		System.out.println(resultStr);
	}

	/**
	 *
	 * For Android devices: - The appplication manifest.xml file must include
	 * internet access permission:
	 * &lt;uses-permission android:name="android.permission.INTERNET"/&gt; - The
	 * application will automatically be signed with an Android debug key to enable
	 * native object automation.
	 * 
	 * @param repoKey             - The full repository path, including directory
	 *                            and file name, where to locate the application.
	 * @param instrument          - Perform instrumentation.. Possible values
	 *                            :noinstrument (default) | instrument.
	 * @param sensorInstrument    - Enable device sensor. Possible values: nosensor
	 *                            (default)| sensor.
	 * @param certificateFile     - The repository path, including directory and
	 *                            file name, of the certificate for certifying the
	 *                            application after instrumentation. This is the
	 *                            Keystore file in Android devices.
	 * @param certificateUser     - The user for certifying the application after
	 *                            instrumentation.This is the Key Alias in Android
	 *                            devices.
	 * @param certificatePassword - The password for certifying the application
	 *                            after instrumentation. This is the Keystore
	 *                            Password in Android devices.
	 * @param certificateParams   - The key password parameter for certifying the
	 *                            application after instrumentation. This is the Key
	 *                            Password in Android devices. The value must be
	 *                            preceded with "keypass".
	 *
	 */

	public static void installInstrumantedAppOnAndroid(String repoKey, String instrument, String sensorInstrument,
			String certificateFile, String certificateUser, String certificatePassword, String certificateParams) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("file", getBundle().getString("repoKey", repoKey));
		if (!getBundle().getString(instrument, instrument).isEmpty())
			params.put("instrument", instrument);
		if (!sensorInstrument.isEmpty())
			params.put("sensorInstrument", sensorInstrument);

		if (getBundle().getString(instrument, instrument).equals("instrument")) {

			params.put("certificate.file", getBundle().getString("certificateFile", certificateFile));
			params.put("certificate.user", getBundle().getString("certificateUser", certificateUser));
			params.put("certificate.password", getBundle().getString("certificateFile", certificatePassword));
			params.put("certificate.params", getBundle().getString("certificateFile", certificateParams));

		}

		String resultStr = (String) getQAFDriver().executeScript("mobile:application:install", params);
		System.out.println(resultStr);
	}
	
	/**
	 *
	 *	Internal Utility method to create Map Parameter for app start/stop.
	 *
	 * @param app     - App name/identifier value
	 * @param by  	  - String value representing app identification type : name/identifier.
	 * 
	 */

	private static Map<String, String> getAppParams(String app, String by) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(by, app);
		return params;
	}

	// by = "name" or "identifier"
	
	/**
	 *
	 *	Method to app start using Perfecto method.
	 *
	 * @param app     - App name/identifier value
	 * @param by  	  - String value representing app identification type : name/identifier.
	 * 
	 */
	public static void startApp(String app, String by) {
		getQAFDriver().executeScript("mobile:application:open", getAppParams(app, by));
	}

	// by = "name" or "identifier"
	/**
	 *
	 *	Method to app stop using Perfecto method.
	 *
	 * @param app     - App name/identifier value
	 * @param by  	  - String value representing app identification type : name/identifier.
	 * 
	 */
	public static void closeApp(String app, String by) {
		closeApp(app, by, false);
	}

	// by = "name" or "identifier"
		/**
		 *
		 *	Method to app stop using Perfecto method with ignoring Exception.
		 *
		 * @param app     - App name/identifier value
		 * @param by  	  - String value representing app identification type : name/identifier.
		 * @param ignoreExceptions - Boolean value representing whether to ignore exception or not.
		 */
	public static void closeApp(String app, String by, boolean ignoreExceptions) {

		try {
			getQAFDriver().executeScript("mobile:application:close", getAppParams(app, by));
		} catch (Exception e) {
			if (!ignoreExceptions) {
				throw e;
			}

		}
	}

	// by = "name" or "identifier" with driver
	/**
	 *
	 *	Method to app stop using Perfecto method with ignoring Exception.
	 *
	 * @param app     - App name/identifier value
	 * @param by  	  - String value representing app identification type : name/identifier.
	 * @param ignoreExceptions - Boolean value representing whether to ignore exception or not.
	 * @param driver - {@link QAFExtendedWebDriver}
	 */
	public static void closeApp(String app, String by, boolean ignoreExceptions, QAFExtendedWebDriver driver) {

		try {
			driver.executeScript("mobile:application:close", getAppParams(app, by));
		} catch (Exception e) {
			if (!ignoreExceptions) {

				throw e;
			}

		}
	}

	// by = "name" or "identifier"
	/**
	 *
	 *	Method to Clean app using Perfecto method.
	 *
	 * @param app     - App name/identifier value
	 * @param by  	  - String value representing app identification type : name/identifier.
	 */
	public static void cleanApp(String app, String by) {
		getQAFDriver().executeScript("mobile:application:clean", getAppParams(app, by));
	}

	// by = "name" or "identifier"
	/**
	 *
	 *	Method to Unistall app using Perfecto method.
	 *
	 * @param app     - App name/identifier value
	 * @param by  	  - String value representing app identification type : name/identifier.
	 */
	public static void uninstallApp(String app, String by) {
		getQAFDriver().executeScript("mobile:application:uninstall", getAppParams(app, by));
	}

	/**
	 *
	 *	Method to Unistall All apps using Perfecto method.
	 *
	 */
	public static void uninstallAllApps() {
		Map<String, String> params = new HashMap<String, String>();
		getQAFDriver().executeScript("mobile:application:reset", params);
	}

	/**
	 *
	 *	Method to get app information using Perfecto method.
	 *
	 *	@param property App property to retrieve.
	 *
	 *	@return String representing the Application property required.
	 */
	public static String getAppInfo(String property) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("property", property);
		return (String) getQAFDriver().executeScript("mobile:application:info", params);
	}
	
	/**
	 *
	 *	Utility method to verify app information using Perfecto method.
	 *
	 *	@param propertyName App property to verify.
	 *
	 *	@param propertyValue Expected App property.
	 *
	 *	@return boolean represents the status of app information validation.
	 */

	public static boolean verifyAppInfo(String propertyName, String propertyValue) {
		
		String message = String.format("%s should be %s", propertyName,propertyValue);
		String appInfo = getAppInfo(propertyName);
		return Validator.verifyThat(message, appInfo,Matchers
				.equalTo(propertyValue));
	}

	/**
	 *
	 *	Utility method to Assert app information using Perfecto method.
	 *
	 *	@param propertyName App property to Assert.
	 *
	 *	@param propertyValue Expected App property.
	 *
	 */
	public static void assertAppInfo(String propertyName, String propertyValue) {
		String appOrientation = getAppInfo(propertyName);
		String message = String.format("%s must be %s", propertyName,propertyValue);
		Validator.assertThat(message, appOrientation,Matchers
				.equalTo(propertyValue));
	}
	
	/**
	 *
	 *	Utility method to Switch app context using Perfecto method.
	 *
	 *	@param context App context example NATIVE_APP.
	 *
	 */

	public static void switchToContext(String context) {
		
		AppiumDriver appiumDriver = AppiumUtils.getAppiumDriver();
		
		String className = appiumDriver.getClass().getName();
		
		if(className.startsWith("io.appium")) {
			
			if(className.contains("AndroidDriver")) {
				AppiumUtils.getAndroidDriver().context(context);
			}else {
				AppiumUtils.getIOSDriver().context(context);
			}
			
		}
			
		
//		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(getQAFDriver());
//		Map<String, String> params = new HashMap<String, String>();
//		params.put("name", context);
//		executeMethod.execute(DriverCommand.SWITCH_TO_CONTEXT, params);
	}

	/**
	 *
	 *	Utility method to wait for text using Perfecto visual method.
	 *
	 *	@param text text to wait for.
	 * 
	 *	@param seconds int max time to wait 
	 *
	 */
	public static void waitForPresentTextVisual(String text, int seconds) {
		
		String message = String.format("Text: '%s' should be present within max timeout - %s seconds", text, seconds);
		String result = isText(text, seconds);
		Validator.verifyThat(message,result, Matchers.equalTo("true"));
	}

	/**
	 *
	 *	Utility method to wait for Image using Perfecto visual method.
	 *
	 *	@param image Image to wait for.
	 * 
	 *	@param seconds int max time to wait 
	 *
	 */
	public static void waitForPresentImageVisual(String image, int seconds) {
		
		String message = String.format("Image: '%s' should be present within max timeout - %s seconds", image, seconds);
		String result = isImg(image, seconds);
		
		Validator.verifyThat(message,result, Matchers.equalTo("true"));
	}

	/**
	 *
	 *	Utility method to execute Perfecto Image Checkpoint method.
	 *
	 *	@param img Image to wait for.
	 * 
	 *	@param timeout int max time to wait 
	 *
	 *	@return String value representing whether the Image is present or not.
	 *
	 */
	
	public static String isImg(String img, Integer timeout) {
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

	/**
	 *
	 *	Utility method to assert visually Image using Perfecto visual method.
	 *
	 *	@param img image to wait for.
	 *
	 */
	public static void assertVisualImg(String img) {
		String message = String.format("Image %s must be visible", img);
		String result = isImg(img, 180);
		Validator.assertThat(message,result, Matchers.equalTo("true"));
	}
	
	/**
	 *
	 *	Utility method to verify visually Image using Perfecto visual method.
	 *
	 *	@param img image to wait for.
	 *
	 *	@return Verification result of Image check using Perfecto visual method.
	 *
	 */

	public static boolean verifyVisualImg(String img) {
		String message = String.format("Image %s should be visible", img);
		String result = isImg(img, 180);
		return Validator.verifyThat(message,result, Matchers.equalTo("true"));
	}

	/**
	 * Visual Text Checkpoint based on the text sent in and a threshold of 100
	 * 
	 * @param text    - Text to compare
	 * @param timeout - timeout amount to search
	 * @return true if found or false if not found
	 */
	public static String isText(String text, Integer timeout) {
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
	 * Visual Text Checkpoint based on the text sent in
	 * 
	 * @param text      - Text to compare
	 * @param timeout   - timeout amount to search
	 * @param threshold - String threshold to match the text with
	 * @return true if found or false if not found
	 * 
	 */
	public static String isText(String text, Integer timeout, String threshold) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("content", text);
		if (timeout != null) {
			params.put("timeout", timeout);
		}
		params.put("threshold", threshold);
		Object result = getQAFDriver().executeScript("mobile:checkpoint:text", params);
		return result.toString();
	}

	/**
	 * @return the current context - "NATIVE_APP", "WEBVIEW", "VISUAL"
	 */
	public static String getCurrentContext() {
		
		AppiumDriver appiumDriver = AppiumUtils.getAppiumDriver();
		
		String className = appiumDriver.getClass().getName();
		
		if(className.startsWith("io.appium")) {
			
			if(className.contains("AndroidDriver")) {
				return AppiumUtils.getAndroidDriver().getContext();
			}else {
				return AppiumUtils.getIOSDriver().getContext();
			}
			
		}else {
			return "";
		}
		
//		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(getQAFDriver());
//		return (String) executeMethod.execute(DriverCommand.GET_CURRENT_CONTEXT_HANDLE, null);
	}

	/**
	 * @return Get all Available Context Handles - "NATIVE_APP", "WEBVIEW", "VISUAL"
	 */
	public static String getCurrentContextHandles() {
		
		
AppiumDriver appiumDriver = AppiumUtils.getAppiumDriver();
		
		String className = appiumDriver.getClass().getName();
		
		Set<String> contextHandles;
		
		if(className.startsWith("io.appium")) {
			
			if(className.contains("AndroidDriver")) {
				contextHandles = AppiumUtils.getAndroidDriver().getContextHandles();
				return String.join(",", contextHandles);
			}else {
				contextHandles = AppiumUtils.getIOSDriver().getContextHandles();
				return String.join(",", contextHandles);
			}
			
		}else {
			return "";
		}
		
//		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(getQAFDriver());
//		@SuppressWarnings("unchecked")
//		ArrayList<String> al = (ArrayList<String>) executeMethod.execute(DriverCommand.GET_CONTEXT_HANDLES, null);
//		StringBuffer sb = new StringBuffer();
//
//		for (String s : al) {
//			sb.append(s);
//			sb.append(",");
//		}
//		String str = sb.toString();
//		System.out.println(str);
//
//		return str;
	}

	// device utils

	/**
	 * Clicks on a single or sequence of physical device keys. Mouse-over the device
	 * keys to identify them, then input into the Keys parameter according to the
	 * required syntax.
	 * <p>
	 * Common keys include: LEFT, RIGHT, UP, DOWN, OK, BACK, MENU, VOL_UP, VOL_DOWN,
	 * CAMERA, CLEAR.
	 * <p>
	 * The listed keys are not necessarily supported by all devices. The available
	 * keys depend on the device.
	 *
	 * @param keySequence the single or sequence of keys to click
	 */
	public static void pressKey(String keySequence) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("keySequence", keySequence);
		getQAFDriver().executeScript("mobile:presskey", params);
	}

	/**
	 * Performs the swipe gesture according to the start and end coordinates.
	 * <p>
	 * Example swipe left:<br>
	 * start: 60%,50% end: 10%,50%
	 *
	 * @param start write in format of x,y. can be in pixels or
	 *              percentage(recommended).
	 * @param end   write in format of x,y. can be in pixels or
	 *              percentage(recommended).
	 */
	public static void swipe(String start, String end) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("start", start);
		params.put("end", end);

		getQAFDriver().executeScript("mobile:touch:swipe", params);

	}

	/**
	 * Performs the tap gesture according to location coordinates with durations in
	 * seconds.
	 * <p>
	 *
	 * @param point   write in format of x,y. can be in pixels or
	 *                percentage(recommended).
	 *
	 * @param seconds The duration, in seconds, for performing the touch operation.
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
	 * @param point write in format of x,y. can be in pixels or
	 *              percentage(recommended).
	 */
	public static void touch(String point) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("location", point); // 50%,50%

		getQAFDriver().executeScript("mobile:touch:tap", params);
	}

	/**
	 * Performs the double touch gesture according to the point coordinates.
	 *
	 * @param point write in format of x,y. can be in pixels or
	 *              percentage(recommended).
	 */
	public static void doubleTouch(String point) {
		Map<String, Object> params = new HashMap<>();
		params.put("location", point);
		params.put("operation", "double");
		getQAFDriver().executeScript("mobile:touch:tap", params);
	}

	/**
	 * Performs the long touch gesture according to the point coordinates.
	 *
	 * @param point write in format of x,y. can be in pixels or
	 *              percentage(recommended).
	 */
	public static void longTouch(String point) {
		Map<String, Object> params = new HashMap<>();
		params.put("location", point);
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
	 * @param restValue the "next" operation, or the "landscape" or "portrait"
	 *                  state.
	 * @param by        the "state" or "operation"
	 */
	// TODO: need additional description.
	public static void rotateDevice(String restValue, String by) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(by, restValue);
		getQAFDriver().executeScript("mobile:handset:rotate", params);
	}
	
	/**
	 *
	 *	Utility method to Set location using Perfecto method.
	 *
	 *	@param location String to set location
	 *
	 *	@param by String Set location strategy - "address" or "coordinates"
	 *
	 */

	// by = "address" or "coordinates"
	public static void setLocation(String location, String by) {

		Map<String, String> params = new HashMap<String, String>();
		params.put(by, location);

		getQAFDriver().executeScript("mobile:location:set", params);
	}

	/**
	 *
	 *	Utility method to Assert location using Perfecto method.
	 *
	 *	@param location String to set location
	 *
	 *
	 */
	public static void assertLocation(String location) {
		String deviceLocation = getDeviceLocation();
		Validator.assertThat("The device location", deviceLocation, Matchers
				.equalTo(location));
	}

	/**
	 *
	 *	Utility method to Verify location using Perfecto method.
	 *
	 *	@param location String to set location
	 *
	 *	@return Boolean value whether the location is as expected.
	 */
	public static boolean verifyLocation(String location) {
		String deviceLocation = getDeviceLocation();
		return Validator.verifyThat("The device location", deviceLocation, Matchers
				.equalTo(location));
	}
	
	/**
	 *
	 *	Utility method to get Device location using Perfecto method.
	 *
	 *	@return String value the location of Perfecto Device.
	 */
	public static String getDeviceLocation() {
		Map<String, String> params = new HashMap<String, String>();
		return (String) getQAFDriver().executeScript("mobile:location:get", params);
	}

	/**
	 *
	 *	Utility method to reset Device location using Perfecto method.
	 */
	public static void resetLocation() {
		Map<String, String> params = new HashMap<String, String>();
		getQAFDriver().executeScript("mobile:location:reset", params);
	}

	/**
	 *
	 *	Utility method to navigate to Device Home screen using Perfecto method.
	 */
	public static void goToHomeScreen() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("target", "All");

		getQAFDriver().executeScript("mobile:handset:ready", params);
	}

	/**
	 *
	 *	Utility method to Lock Device for given duration using Perfecto method.
	 *
	 *	@param lockTimeInSeconds - Device Lock time in seconds
	 */
	public static void lockDevice(int lockTimeInSeconds) {
		Map<String, Integer> params = new HashMap<String, Integer>();
		params.put("timeout", lockTimeInSeconds);

		getQAFDriver().executeScript("mobile:screen:lock", params);
	}

	/**
	 *
	 *	Utility method to Set timezone of Device using Perfecto method.
	 *
	 *	@param timezone - Timezone to set
	 */
	public static void setTimezone(String timezone) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("timezone", timezone);

		getQAFDriver().executeScript("mobile:timezone:set", params);
	}

	/**
	 *
	 *	Utility method to Get timezone of Device using Perfecto method.
	 *
	 *	@return Sting Timezone of device
	 */
	public static String getTimezone() {
		Map<String, String> params = new HashMap<String, String>();

		return (String) getQAFDriver().executeScript("mobile:timezone:get", params);
	}

	/**
	 *
	 *	Utility method to Assert timezone of Device using Perfecto method.
	 *
	 *	@param timezone Timezone of device
	 */
	public static void assertTimezone(String timezone) {
		String deviceTimezone = getTimezone();
		Validator.assertThat("The device timezone", deviceTimezone, Matchers.equalTo(timezone));
	}

	/**
	 *
	 *	Utility method to Verify timezone of Device using Perfecto method.
	 *
	 *	@param timezone Timezone of device
	 *
	 *	@return Boolean representing result of timezone match.
	 */
	public static boolean verifyTimezone(String timezone) {
		return Validator.verifyThat("The device timezone should be " + timezone, getTimezone(),
				Matchers.equalTo(timezone));
	}

	/**
	 *
	 *	Utility method to reset timezone of Device using Perfecto method.
	 */
	public static void resetTimezone() {
		Map<String, String> params = new HashMap<String, String>();
		getQAFDriver().executeScript("mobile:timezone:reset", params);
	}

	/**
	 *
	 *	Utility method to take screenshot using Perfecto method.
	 *
	 *	@param repositoryPath Screenshot Path of Perfecto repository
	 *
	 *	@param shouldSave boolean value representing whether screenshot to save or not.
	 */
	public static void takeScreenshot(String repositoryPath, boolean shouldSave) {
		Map<String, String> params = new HashMap<String, String>();
		if (shouldSave) {
			params.put("key", repositoryPath);
		}
		getQAFDriver().executeScript("mobile:screen:image", params);
	}

	/**
	 *
	 *	Utility method to Start image injection in Device using Perfecto method.
	 *
	 *	@param repositoryFile Image File Path of Perfecto repository
	 *
	 *	@param app App identifier value.
	 *
	 *	@param by App identifying stategy. by = "name" or "identifier"
	 */
	public static void startImageInjection(String repositoryFile, String app, String by) {
		Map<String, Object> params = new HashMap<>();
		params.put("repositoryFile", repositoryFile);
		params.put(by, app);
		getQAFDriver().executeScript("mobile:image.injection:start", params);

	}

	/**
	 *
	 *	Utility method to Stop image injection in Device using Perfecto method.
	 *
	 */
	public static void stopImageInjection() {
		Map<String, Object> params = new HashMap<>();
		new WebDriverTestBase().getDriver().executeScript("mobile:image.injection:stop", params);
	}

	/**
	 *
	 *	Utility method to simulate finger print authentication on Device using Perfecto method.
	 *
	 *	@param identifier - App identifier value.
	 *
	 *	@param by - App identifying stategy. by = "name" or "identifier"
	 *
	 *	@param resultAuth - Result to Simulate.
	 *
	 *	@param errorType - Error type to simulate in case of failed authentication
	 *
	 */
	public static void setFingerprint(String by, String identifier, String resultAuth, String errorType) {

		Map<String, Object> params = new HashMap<>();
		params.put(by, identifier);
		params.put("resultAuth", resultAuth);
		if (!errorType.isEmpty() && !errorType.equals("")) {
			params.put("errorType", errorType);
		}

		getQAFDriver().executeScript("mobile:fingerprint:set", params);
	}

	/**
	 *
	 *	Utility method to simulate Sensor authentication on Device using Perfecto method.
	 *
	 *	@param identifier - App identifier value.
	 *
	 *	@param by - App identifying stategy. by = "name" or "identifier"
	 *
	 *	@param resultAuth - Result to Simulate.
	 *
	 *	@param errorType - Error type to simulate in case of failed authentication
	 *
	 */
	public static void setSensorAuthentication(String by, String identifier, String resultAuth, String errorType) {

		Map<String, Object> params = new HashMap<>();
		params.put(by, identifier);
		params.put("resultAuth", resultAuth);
		if (!errorType.isEmpty() && !errorType.equals("")) {
			params.put("errorType", errorType);
		}
		getQAFDriver().executeScript("mobile:sensorAuthentication:set", params);
	}

	/**
	 *
	 *	Utility method to Start capturing HAR file on Device using Perfecto method.
	 *
	 */
	
	public static void generateHAR() {
		Map<String, Object> params = new HashMap<>();
		params.put("generateHarFile", "true");
		getQAFDriver().executeScript("mobile:vnetwork:start", params);

	}

	/**
	 *
	 *	Utility method to Stop capturing HAR file on Device using Perfecto method.
	 *
	 */
	public static void stopGenerateHAR() {
		Map<String, Object> params = new HashMap<>();
		getQAFDriver().executeScript("mobile:vnetwork:stop", params);
	}

	
	/**
	 *
	 *	Utility method to Inject audio file on Device using Perfecto method.
	 *
	 *	@param file Audio file path in Perfecto repository.
	 */
	public static void audioInject(String file) {
		Map<String, Object> params = new HashMap<>();
		params.put("key", file);
		getQAFDriver().executeScript("mobile:audio:inject", params);

	}

	/**
	 *
	 *	Utility method to Get Device Property using Perfecto method.
	 *
	 *	@param property Device property to retrieve.
	 *
	 *	@return String representing required device property.
	 */
	public static String getDeviceProperty(String property) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("property", property);
		return (String) getQAFDriver().executeScript("mobile:handset:info", params);

	}

	/**
	 * Sets the picker wheel to the value specified.
	 * 
	 * @param picker    - WebElement that holds the XCUIElementTypePicker
	 * @param direction - Direction to spin the spinner, either next or previous
	 *                  defaults to next
	 * @param value     - value to compare this must be exact
	 */
	public static void setPickerWheel(RemoteWebElement picker, String direction, String value) {
		value = value.replaceAll("[^\\x00-\\x7F]", "");
		String name = picker.getAttribute("value").replaceAll("[^\\x00-\\x7F]", "");
		while (!name.equals(value)) {
			System.out.println(name);
			pickerwheelStep(picker, direction);
			// title based will retrieve the title as a string,
			// view based will retrieve a string that represent the view
			// (uniqueness depends on the developer of the app).
			name = picker.getAttribute("value").replaceAll("[^\\x00-\\x7F]", "");
		}
	}

	/**
	 * Returns the selected value from the picker wheel
	 * 
	 * @param element - WebElement that holds the XCUIElementTypePicker
	 * @return string value of the value attribute
	 */
	public static String pickerwheelGet(RemoteWebElement element) {
		return element.getAttribute("value");
	}

	/**
	 * Moves the pickerwheel in one step using a default of 0.15
	 * 
	 * @param element   - WebElement that holds the XCUIElementTypePicker
	 * @param direction - Direction to spin the spinner, either next or previous
	 *                  defaults to next
	 */
	public static void pickerwheelStep(RemoteWebElement element, String direction) {
		pickerwheelStep(element, direction, 0.15);
	}

	/**
	 * Moves the pickerwheel in one step
	 * 
	 * @param element   - WebElement that holds the XCUIElementTypePicker
	 * @param direction - Direction to spin the spinner, either next or previous
	 *                  defaults to next
	 * @param offset    - the offset of the picker this represents 1 slide
	 */
	public static void pickerwheelStep(RemoteWebElement element, String direction, double offset) {
		Map<String, Object> params = new HashMap<>();
		params.put("order", direction);
		params.put("offset", offset);
		params.put("element", element.getId());
		getQAFDriver().executeScript("mobile: selectPickerWheelValue", params);
	}

	/**
	 * Sets the picker wheel to the value specified.
	 * 
	 * @param locator   - Locator to find the element based on
	 * @param direction - Direction to spin the spinner, either next or previous
	 *                  defaults to next
	 * @param value     - value to compare this must be exact
	 */
	public static void setPickerWheel(String locator, String direction, String value) {
		setPickerWheel((RemoteWebElement) getQAFDriver().findElement(locator), direction, value);
	}

	/**
	 * This function will calculate the location of the element on the device and
	 * manually tap the point location of the middle of the element. This function
	 * accounts that there may be a header to offset from.
	 * 
	 * @param loc        - locator to find the element to be clicked
	 * @param addressBar - navigation bar that takes up the top half of the device
	 *                   outside of the webview
	 */
	public static void touchObject(String loc, String addressBar) {
		int bannerY = getOffset(addressBar);
		int scaleFactor = getScale();
		// Gets the rectangle of the element we want to click
		Rectangle rect = new QAFExtendedWebElement(loc).getRect();
		// calculates the middle x value using the rectangle and multiplying the scale
		int x = (rect.getX() + (rect.getWidth() / 2)) * scaleFactor;
		// calculates the middle y value using the rectangle, adding the offset
		// and multiplying the scale
		int y = (rect.getY() + (rect.getHeight() / 2) + bannerY) * scaleFactor;
		// Touch the device at the point calculated
		touch(x + "," + y);
	}

	/**
	 * Slides the provided object to the left
	 * 
	 * @param loc object to slide
	 */
	public static void slideObjectLeft(String loc) {
		// uses 0.5 to get the middle of the Y
		float y = 0.5f;
		// Since we are sliding left, we want to start on the right side of the element
		// and end on the left side
		float startX = (2.0f / 3.0f);
		float endX = (1.0f / 3.0f);
		// This calls the slide object using the constant values we set for
		// the default left slide
		slideObject(loc, startX, endX, y);
	}

	/**
	 * Slides the provided object
	 * 
	 * @param loc        object to slide
	 * @param xStartMult - x point to start on
	 * @param xEndMult   - y point to end on
	 * @param yMult      - y point for both the start and stop
	 */
	public static void slideObject(String loc, float xStartMult, float xEndMult, float yMult) {
		slideObject(loc, xStartMult, xEndMult, yMult, yMult);
	}

	/**
	 * 
	 * Slides the provided object
	 * 
	 * @param loc        object to slide
	 * @param xStartMult - x point to start on
	 * @param xEndMult   - y point to end on
	 * @param yStartMult - y point to start on
	 * @param yEndMult   - y point to end on
	 */
	public static void slideObject(String loc, float xStartMult, float xEndMult, float yStartMult, float yEndMult) {
		// Gets the current scale of the device
		int scaleFactor = getScale();
		// Gets the rectangle of the object to use the x,y and width, height
		Rectangle rect = new QAFExtendedWebElement(loc).getRect();
		// Gets point to start y
		int startY = Math.round(((rect.getY() + (rect.getHeight() * yStartMult))) * scaleFactor);
		// Gets point to stop y
		int endY = Math.round((rect.getY() + (rect.getHeight() * yEndMult)) * scaleFactor);
		// Gets the point to start x
		int startX = Math.round((rect.getX() + (rect.getWidth() * xStartMult)) * scaleFactor);
		// gets the point to stop y
		int endX = Math.round((rect.getX() + ((rect.getWidth()) * xEndMult)) * scaleFactor);
		// swipes using the points provided
		swipe(startX + "," + startY, endX + "," + endY);
	}

	/**
	 * Gets the current application sacale for the device
	 * 
	 * @return integer value of scale
	 */
	public static int getScale() {
		// Gets the resolution of the current device
		String deviceRes = getDeviceProperty("resolution");
		// Gets the width of the root application viewport
		int appWidth = new QAFExtendedWebElement("xpath=/*/*").getSize().getWidth();
		// compares the resolution to the application dimensions to find out what the
		// pixel scale is
		return Math.round(Integer.parseInt(deviceRes.split("\\*")[0]) / appWidth);
	}

	/**
	 * Gets the offset of the header values to offset y value of the header element
	 * 
	 * @param addressBar - header element to measure
	 * @return the y offset of the element
	 */
	public static int getOffset(String addressBar) {
		return getOffset(addressBar, "NATIVE_APP");
	}

	/**
	 * Gets the offset of the header values to offset y value of the header element
	 * 
	 * @param addressBar - header element to measure
	 * @param context    - context of the element to use
	 * @return the y offset of the element
	 */
	public static int getOffset(String addressBar, String context) {
		// Stores the current context so we can switch to it at the end
		String curContext = DeviceUtils.getCurrentContext();
		// Switch to native context
		switchToContext(context);
		// Gets the rectangle of the welement to get the x,y and width height
		Rectangle view = new QAFExtendedWebElement(addressBar).getRect();
		switchToContext(curContext); // Switch back to the original context
		// this gets the application size of the area above the viewport
		// we will use this to offset the element
		return (view.getY() + view.getHeight());
	}

	/**
	 * Performs an audit of the accessibility features in the application. To check
	 * the entire application, this command needs to be repeated for each
	 * application screen.
	 *
	 * @param tagName - The tag that is appended to the name of the audit report to
	 *                help match it to the application screen.
	 */
	public static void checkAccessibility(String tagName) {
		// declare the Map for script parameters
		String browserName = DriverUtils.getDriver().getCapabilities().getCapability("browserName").toString();
		String platformName = DriverUtils.getDriver().getCapabilities().getCapability("platformName").toString();
		if (platformName.equalsIgnoreCase("ios") && browserName.equalsIgnoreCase("safari")) {
			Log logger = LogFactoryImpl.getLog(DeviceUtils.class);
			logger.error(
					"Accessibility testing is not supported for Safari browser on iPhone/iPad. Skipping Accessibility check.");
		} else {
			if (DriverUtils.getDriver().getCapabilities().getCapability("driverClass") != null) {
				Map<String, Object> params = new HashMap<>();
				params.put("tag", tagName);
				getQAFDriver().executeScript("mobile:checkAccessibility:audit", params);
			} else {
				startAxe();
			}
		}
	}

	/**
	 * Generates an external voice call recording to the selected destination It is
	 * possible to select multiple destinations that may include devices, users, and
	 * phone numbers. There is no default. To use, at least one destination must be
	 * selected.
	 *
	 * @param toHandset - The destination device. It is possible to select multiple
	 *                  devices.
	 * @param toUser    - The user for this command. It is possible to select
	 *                  multiple users.
	 * @param toLogical - user | none The user currently running the script.
	 * @param toNumber  - The destination phone number. It is possible to select
	 *                  multiple phone numbers. Format - +[country code][area
	 *                  code][phone number]
	 *
	 *	@throws java.lang.Exception Exception thrown by method if no Destination is selected.
	 */
	public static void cloudCall(String toHandset, String toUser, String toLogical, String toNumber) throws Exception {
		if (toHandset.isEmpty() && toUser.isEmpty() && toLogical.isEmpty() && toNumber.isEmpty())
			throw new Exception("Please select at least one destination");

		Map<String, Object> pars = new HashMap<>();
		if (!toHandset.isEmpty())
			pars.put("to.handset", toHandset);
		if (!toUser.isEmpty())
			pars.put("to.user", toUser);
		if (!toLogical.isEmpty())
			pars.put("to.logical", toLogical);
		if (!toNumber.isEmpty())
			pars.put("to.number", toNumber);
		getQAFDriver().executeScript("mobile:gateway:call", pars);
	}

	/**
	 * Sends an email message to the selected destination It is possible to select
	 * multiple destinations that may include email addresses, devices, and users.
	 * There is no default. At least one destination must be selected. If not
	 * specified, the message subject and body are be defaulted to none and “test
	 * email”.
	 *
	 * Confirm that the destination device is configured to receive email messages.
	 *
	 * @param subject   - The message subject for this command. &lt;default is "none"&gt;.
	 * @param body      - The message text for this command. &lt;default is "test
	 *                  email"&gt;.
	 * @param toHandset - The destination device. It is possible to select multiple
	 *                  devices.
	 * @param toAddress - The email address for this command.
	 * @param toUser    - The user for this command. It is possible to select
	 *                  multiple users.
	 * @param toLogical - user | none The user currently running the script.
	 *
	 *
	 *	@throws java.lang.Exception Exception thrown by method if no Destination is selected.
	 */
	public static void cloudEmail(String subject, String body, String toHandset, String toAddress, String toUser,
			String toLogical) throws Exception {
		if (toHandset.isEmpty() && toAddress.isEmpty() && toUser.isEmpty() && toLogical.isEmpty())
			throw new Exception("Please select at least one destination");

		Map<String, Object> pars = new HashMap<>();
		if (!subject.isEmpty())
			pars.put("subject", subject);
		if (!body.isEmpty())
			pars.put("body", body);
		if (!toHandset.isEmpty())
			pars.put("to.handset", toHandset);
		if (!toAddress.isEmpty())
			pars.put("to.address", toAddress);
		if (!toUser.isEmpty())
			pars.put("to.user", toUser);
		if (!toLogical.isEmpty())
			pars.put("to.logical", toLogical);
		getQAFDriver().executeScript("mobile:gateway:email", pars);
	}

	/**
	 * Sends an SMS message to the selected destination. It is possible to select
	 * multiple destinations that may include devices, users, and phones. There is
	 * no default. To use, at least one destination must be selected.
	 *
	 * @param body      - The message text for this command. &lt;default is "test
	 *                  email"&gt;.
	 * @param toHandset - The destination device. It is possible to select multiple
	 *                  devices.
	 * @param toUser    - The user for this command. It is possible to select
	 *                  multiple users.
	 * @param toLogical - user | none The user currently running the script.
	 * @param toNumber  - The destination phone number. It is possible to select
	 *                  multiple phone numbers. Format - +[country code][area
	 *                  code][phone number]
	 *
	 *	@throws java.lang.Exception Exception thrown by method if no Destination is selected.
	 */
	public static void cloudSMS(String body, String toHandset, String toUser, String toLogical, String toNumber)
			throws Exception {
		if (toHandset.isEmpty() && toUser.isEmpty() && toLogical.isEmpty() && toNumber.isEmpty())
			throw new Exception("Please select at least one destination");

		Map<String, Object> pars = new HashMap<>();
		if (!body.isEmpty())
			pars.put("body", body);
		if (!toHandset.isEmpty())
			pars.put("to.handset", toHandset);
		if (!toUser.isEmpty())
			pars.put("to.user", toUser);
		if (!toLogical.isEmpty())
			pars.put("to.logical", toLogical);
		if (!toNumber.isEmpty())
			pars.put("to.number", toNumber);
		getQAFDriver().executeScript("mobile:gateway:sms", pars);
	}

	/**
	 * Utility method to start Axe tool on Perfecto Browsers for Accessibility testing.
	 */
	private static void startAxe() {
		AxeHelper axe = new AxeHelper(DeviceUtils.getQAFDriver());
		axe.runAxe();
		axe.startHighlighter("violations");
		StringBuilder errors = new StringBuilder();
		int errorCount = 0;
		
		Map<String, ?> violation;
		
		String impact;
		String summary;
		String html;
		String selector;

		String message;
		
		while (true) {
			violation = axe.nextHighlight();
			System.out.println("violation: " + violation);
			if (violation == null) {
				break;
			}

			errorCount++;
			String ruleId = (String) violation.get("issue");
			@SuppressWarnings("unchecked")
			Map<String, String> node = (Map<String, String>) violation.get("node");

			impact = node.get("impact");
			summary = node.get("failureSummary");
			html = node.get("html");
			selector = (String) violation.get("target");

			message = String.format("%s - %s%n %s%n Selector:\t%s%n HTML:\t\t%s%n%n", impact, ruleId,
					summary, selector, html);

			DriverUtils.getDriver().getScreenshotAs(OutputType.BASE64);
			ReportUtils.getReportClient().reportiumAssert(message, false);
			errors.append(message);
		}

		if (errorCount > 0) {
			final Capabilities capabilities = DriverUtils.getDriver().getCapabilities();
			final String platform = String.valueOf(capabilities.getCapability("platformName"));
			final String version = String.valueOf(capabilities.getCapability("platformVersion"));
			final String browserName = String.valueOf(capabilities.getCapability("browserName"));
			final String browserVersion = String.valueOf(capabilities.getCapability("browserVersion"));
			String browserVersionFormatted;
			
			if ("null".equals(browserName)) {
				browserVersionFormatted = "default browser";
			} else {
				browserVersionFormatted = browserName + "-" + browserVersion;
			}
			message = String.format("%n%s-%s %s : %d violations on %s%nReport Link: %s%n", platform, version,
					browserVersionFormatted, errorCount, "https://www.google.com/",
					ReportUtils.getReportClient().getReportUrl());
			
			message = String.format("%s%n%s%n", message, errors);
//			throw new AccessibilityException(message);
		}
	}

	/**
	 * Utility method to start Device vital collection using Perfecto method
	 */
	public static void startVitals() {
		Map<String, String> params = new HashMap<>();
		getQAFDriver().executeScript("mobile:vitals:start", params);
	}

	/**
	 * Utility method to start Device vital collection using Perfecto method
	 * 
	 * @param vitals - List of vitals to collect.
	 */
	public static void startVitals(List<String> vitals) {
		Map<String, Object> params = new HashMap<>();
		params.put("vitals", vitals);
		getQAFDriver().executeScript("mobile:vitals:start", params);
	}

	/**
	 * Utility method to start Device vital collection using Perfecto method
	 * 
	 * @param vitals - List of vitals to collect.
	 * 
	 * @param interval - Duration of collection of vitals.
	 */
	public static void startVitals(List<String> vitals, Integer interval) {
		Map<String, Object> params = new HashMap<>();
		params.put("vitals", vitals);
		params.put("interval", interval);
		getQAFDriver().executeScript("mobile:vitals:start", params);
	}

	/**
	 * Utility method to stop Device vital collection using Perfecto method
	 */
	public static void stopVitals() {
		Map<String, Object> params = new HashMap<>();
		getQAFDriver().executeScript("mobile:vitals:stop", params);
	}
}