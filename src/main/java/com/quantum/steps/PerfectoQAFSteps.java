/*******************************************************************************
 * QMetry Automation Framework provides a powerful and versatile platform to author 
 * Automated Test Cases in Behavior Driven, Keyword Driven or Code Driven approach
 *                
 * Copyright 2016 Infostretch Corporation
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE
 *
 * You should have received a copy of the GNU General Public License along with this program in the name of LICENSE.txt in the root folder of the distribution. If not, see https://opensource.org/licenses/gpl-3.0.html
 *
 * See the NOTICE.TXT file in root folder of this source files distribution 
 * for additional information regarding copyright ownership and licenses
 * of other open source software / files used by QMetry Automation Framework.
 *
 * For any inquiry or need additional information, please contact support-qaf@infostretch.com
 *******************************************************************************/


package com.quantum.steps;

import com.google.common.collect.ImmutableMap;
import com.qmetry.qaf.automation.step.QAFTestStep;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteExecuteMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

/**
 * com.qmetry.qaf.automation.step.PerfectMobileSteps.java
 * This class defines steps and methods that can be used for perfecto-mobile.
 * @author chirag.jayswal
 */
public final class PerfectoQAFSteps {


	@QAFTestStep(description="install application {repository-key} with {instrument-noinstrument}")
	public static void installApp(String repoKey, String instrument) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("file", getBundle().getString(repoKey,repoKey));
		params.put("instrument", getBundle().getString(instrument, instrument));

		String resultStr = (String) getDriver().executeScript("mobile:application:install", params);
		System.out.println(resultStr);
	}
	
	@QAFTestStep(description="open {appname} application")
	public static void openApplication(String appName) {
		// open application command
		String command = "mobile:application:open";
		// open application
		getDriver().executeScript(command, ImmutableMap.of("name", appName));
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(getDriver());
		executeMethod.execute(DriverCommand.SWITCH_TO_CONTEXT,  ImmutableMap.of("name", "NATIVE"));
	}

	@QAFTestStep(description = "close {appname application}")
	public static void closeApplication(String appName) {

		// open application command
		String command = "mobile:application:close";
		// open application
		getDriver().executeScript(command, ImmutableMap.of("name", appName));

	}
	@QAFTestStep(description="switch to {context-name} context")
	public static void switchToContext(String context) {
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(getDriver());
		executeMethod.execute(DriverCommand.SWITCH_TO_CONTEXT,  ImmutableMap.of("name", context));
	}

	public static String getCurrentContextHandle() {
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(getDriver());
		String context = (String) executeMethod.execute(DriverCommand.GET_CURRENT_CONTEXT_HANDLE, null);
		return context;
	}

	@SuppressWarnings("unchecked")
	public static List<String> getContextHandles() {
		RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(getDriver());
		List<String> contexts = (List<String>) executeMethod.execute(DriverCommand.GET_CONTEXT_HANDLES, null);
		return contexts;
	}

	/**
	 * Clicks on a single or sequence of physical device keys.
	 * Mouse-over the device keys to identify them, then input into the Keys parameter according to the required syntax.
	 * <p>
	 * Common keys include:
	 * LEFT, RIGHT, UP, DOWN, OK, BACK, MENU, VOL_UP, VOL_DOWN, CAMERA, CLEAR.
	 * <p>
	 * The listed keys are not necessarily supported by all devices. The available keys depend on the device.
	 *
     * @param driver the RemoteWebDriver
     * @param keySequence the single or sequence of keys to click
     */
	@QAFTestStep(description="press {keySequence} keys on mobile")
    public static void pressKey(String keySequence) {
        getDriver().executeScript("mobile:presskey",  ImmutableMap.of("keySequence", keySequence));
    }

    /**
     * Performs the swipe gesture according to the start and end coordinates.
     * <p>
     * Example swipe left:<br/>
     * start: 60%,50% end: 10%,50%
     *
     * @param driver the RemoteWebDriver
     * @param start write in format of x,y. can be in pixels or percentage(recommended).
     * @param end write in format of x,y. can be in pixels or percentage(recommended).
     */
	@QAFTestStep(description="swipe from {start-point} to {end-point}")
    public static void swipe(String start, String end) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("start", start);
        params.put("end", end);

        getDriver().executeScript("mobile:touch:swipe", params);

    }

    /**
     * Performs the touch gesture according to the point coordinates.
     * 
     * @param driver the RemoteWebDriver
     * @param point write in format of x,y. can be in pixels or percentage(recommended) for example 50%,50%.
     */
	@QAFTestStep(description="touch at {point} point")
    public static void touch(String point) {
        getDriver().executeScript("mobile:touch:tap", ImmutableMap.of("location", point));
    }

    /**
     * Hides the virtual keyboard display.
     * 
     * @param driver the RemoteWebDriver
     */
	@QAFTestStep(description="hide mobile keyboard")
    public static void hideKeyboard() {
        getDriver().executeScript("mobile:keyboard:display", ImmutableMap.of("mode", "off"));
    }

    /**
     * Rotates the device to landscape, portrait, or its next state.
     * 
     * @param driver the RemoteWebDriver
     * @param restValue the "next" operation, or the "landscape" or "portrait" state.
     * @param by the "state" or "operation"
     */
    //TODO: need additional description.
    public static void rotateDevice(String restValue, String by) {
        getDriver().executeScript("mobile:handset:rotate", ImmutableMap.of(by, restValue));
    }

    //by = "address" or "coordinates"
    public static void setLocation(String location, String by) {
        getDriver().executeScript("mobile:location:set", ImmutableMap.of(by, location));
    }
    
	@QAFTestStep(description="set location to {address} address")
    public static void setLocationByAdderess(String location) {
        setLocation(location, "address");
    }

	@QAFTestStep(description="set location to {cords} cordinates")
    public static void setLocationByCoordinates(String location) {
        setLocation(location, "coordinates");
    }
    public static String getDeviceLocation() {
        return (String) getDriver().executeScript("mobile:location:get", ImmutableMap.of());
    }


    public static void resetLocation() {
        getDriver().executeScript("mobile:location:reset", ImmutableMap.of());
    }

	@QAFTestStep(description="go to home screen")
    public static void goToHomeScreen() {
        getDriver().executeScript("mobile:handset:ready", ImmutableMap.of("target", "All"));
    }

	@QAFTestStep(description="lock mobile device with {timeout} seconds timeout")
    public static void lockDevice(int sec) {
        getDriver().executeScript("mobile:screen:lock", ImmutableMap.of("timeout", sec));
    }

	@QAFTestStep(description="set mobile timezone to {timezone}")
    public static void setTimezone(String timezone) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("timezone", timezone);

        getDriver().executeScript("mobile:timezone:set", ImmutableMap.of("timezone", timezone));
    }

    public static String getTimezone() {
        return (String) getDriver().executeScript("mobile:timezone:get", ImmutableMap.of());
    }


	@QAFTestStep(description="reset mobile timezone")
    public static void resetTimezone() {
        getDriver().executeScript("mobile:timezone:reset", ImmutableMap.of());
    }

	private static QAFExtendedWebDriver getDriver() {
		return new WebDriverTestBase().getDriver();
	}
}
