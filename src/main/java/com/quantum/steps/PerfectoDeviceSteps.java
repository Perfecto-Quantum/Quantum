package com.quantum.steps;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.Assert;

import com.qmetry.qaf.automation.step.QAFTestStepProvider;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebElement;
import com.quantum.utils.DeviceUtils;
import com.quantum.utils.DriverUtils;
import com.quantum.utils.ReportUtils;

import cucumber.api.java.en.Then;

/**
 * The class PerfectoDeviceSteps provides methods for working with a device,
 * with cucumber steps annotations.
 * <p>
 * Example: Working with a device.
 * 
 * <pre>
 * Scenario: 
 * 	Given I go to the device home screen
 * 	Then I open browser to webpage "https://community.perfectomobile.com/"
 * 	Then I should see text "GETTING STARTED"
 * 	Then I take a screenshot and save to PRIVATE:dir1/dir2/name.png
 * </pre>
 * 
 * @author shanil
 * @see <a href=
 *      "https://github.com/PerfectoCode/Quantum/wiki/BDD-Implementation">BDD
 *      Implementation</a>
 *
 */
@QAFTestStepProvider(prefix = "cucmber")
public class PerfectoDeviceSteps {

	/**
	 * Rotates the device to landscape mode.
	 */
	@Then("^I rotate the device to landscape$")
	public void rotateToLandscape() {
		DeviceUtils.rotateDevice("landscape", "state");
	}

	/**
	 * Rotates the device to portrait mode.
	 */
	@Then("^I rotate the device to portrait$")
	public void rotateToPortrait() {
		DeviceUtils.rotateDevice("portrait", "state");
	}

	/**
	 * Rotates the device to its next state.
	 */
	@Then("^I rotate the device$")
	public void rotateDevice() {
		DeviceUtils.rotateDevice("next", "operation");
	}

	/**
	 * Sets the device location using latitude,longitude coordinates (decimal
	 * degrees) format. This enables testing a location-aware app that uses Location
	 * Services, without moving the device from place to place to generate location
	 * data.
	 * <p>
	 * Confirm that the "Allow mock locations" setting is enabled. Go to: Settings,
	 * Developer options, Allow mock locations.
	 * <p>
	 * Example: 43.642659,-79.387050
	 * 
	 * @param coordinates
	 *            the location coordinates to set
	 */
	@Then("^I set the device location to the coordinates \"(.*?)\"$")
	public void setLocationByCoordinates(String coordinates) {
		DeviceUtils.setLocation(coordinates, "coordinates");
	}
	
	
	/**
	 * This step will switch the current driver to the given driver name. The driver names can be perfecto, device2 or perfectoRemote, device2Remote respectively for Appium Mobile Driver and Remote Desktop Drivers.
	 * To use you must assign at least two parameter 
	 * groups in your testNG config containing either capabilities assigned to a driver name or 
	 * pointing to an env.resource file containing the set of capabilities associated with the driver name.
	 * <p>
	 * Example: 
	 * <p>
	 * &lt;parameter name="perfecto.capabilities.platformName" value="Android" />
	 * <p>&lt;parameter name="perfecto.env.resources" value="src/main/resources/android2" />
	 *  * <p>
	 * &lt;parameter name="device2.capabilities.platformName" value="Android" />
	 * <p>&lt;parameter name="device2.env.resources" value="src/main/resources/android2" />
	 * <p>
	 * <b>Note:</b> 
	 * If AppiumDriver is set to true you must also have the appropriate Appium driver class assigned to the driver name
	 * <p>
	 * &lt;parameter name="perfecto.capabilities.driverClass" value="io.appium.java_client.android.AndroidDriver" />
	 * <p>or
	 * <p>&lt;parameter name="device2.capabilities.driverClass" value="io.appium.java_client.ios.IOSDriver" />
	 * <p>
	 * I switch to driver "perfecto"
	 * 
	 * @param driverName
	 *            The name of the driver you are switching to "perfecto" or "device2" or "perfectoRemote" or "device2Remote"
	 */
	@Then("^I switch to driver \"([^\"]*)\"$")
	public static void switchToDriver(String driverName) {

		DriverUtils.switchToDriver(driverName);

	}
	
	
	

	/**
	 * Sets the device location using address (Google Geocoding) format. This
	 * enables testing a location-aware app that uses Location Services, without
	 * moving the device from place to place to generate location data.
	 * <p>
	 * Confirm that the "Allow mock locations" setting is enabled. Go to: Settings,
	 * Developer options, Allow mock locations.
	 * <p>
	 * Example: 1600 Amphitheatre Parkway, Mountain View, CA
	 * 
	 * @param address
	 *            the location address to set
	 */
	@Then("^I set the device location to the address \"(.*?)\"$")
	public void setLocationByAddress(String address) {
		DeviceUtils.setLocation(address, "address");
	}

	/**
	 * Checks the device location using latitude,longitude coordinates (decimal
	 * degrees) format. Stops the test in case of failure.
	 * <p>
	 * Example: 43.642659,-79.387050
	 * 
	 * @param coordinates
	 *            the location coordinates to check
	 */
	@Then("^the device coordinates must be \"(.*?)\"$")
	public void assertLocationCoordinates(String coordinates) {
		DeviceUtils.assertLocation(coordinates);
	}

	/**
	 * Verifies the device location using latitude,longitude coordinates (decimal
	 * degrees) format. The test will continue to run in case of failure.
	 * <p>
	 * Example: 43.642659,-79.387050
	 * 
	 * @param coordinates
	 *            the location coordinates to verify
	 * @return <code>true</code> if the location is verified, <code>false</code>
	 *         otherwise
	 */
	@Then("^the device coordinates should be \"(.*?)\"$")
	public boolean verifyLocationCoordinates(String coordinates) {
		return DeviceUtils.verifyLocation(coordinates);
	}

	/**
	 * Resets the device location. This command should be used after the setting the
	 * location to stop setting the device location.
	 * <p>
	 * This operation returns the device to its current location.
	 */
	@Then("^I reset the device location$")
	public void resetLocation() {
		DeviceUtils.resetLocation();
	}

	/**
	 * Brings the device to its idle / home screen. This is done by navigating the
	 * device back to the home screen.
	 * <p>
	 * For iOS and Android devices, the device is unlocked and returned to its
	 * default rotate orientation.
	 * <p>
	 * Use this command at the beginning of a script, to ensure a known starting
	 * point for the user.
	 */
	@Then("^I go to the device home screen$")
	public void goToHomeScreen() {
		DeviceUtils.goToHomeScreen();
	}

	/**
	 * Performs the swipe gesture to the left.
	 */
	@Then("^I swipe left$")
	public void swipeLeft() {
		DeviceUtils.swipe("60%,50%", "10%,50%");
	}

	/**
	 * Performs the swipe gesture to the right.
	 */
	@Then("^I swipe right")
	public void swipeRight() {
		DeviceUtils.swipe("40%,50%", "90%,50%");
	}

	/**
	 * Performs the scroll up gesture.
	 */
	@Then("^I scroll up$")
	public void scrollUp() {
		DeviceUtils.swipe("50%,40%", "50%,60%");
	}

	/**
	 * Performs the scroll down gesture.
	 */
	@Then("^I scroll down$")
	public void scrollDown() {
		DeviceUtils.swipe("50%,60%", "50%,40%");
	}

	/**
	 * Locks the device screen for the duration set in seconds, and unlocks the
	 * device.
	 * 
	 * @param seconds
	 *            the lock screen duration
	 */
	@Then("^I lock the device for \"(\\d*\\.?\\d*)\" seconds$")
	public void lockDevice(int seconds) {
		DeviceUtils.lockDevice(seconds);
	}

	/**
	 * Sets the device timezone.
	 * 
	 * @param timezone
	 *            the new timezone Id
	 */
	@Then("^I set timezone to \"(.*?)\"")
	public void setTimezone(String timezone) {
		DeviceUtils.setTimezone(timezone);
	}

	/**
	 * Checks the device timezone. Stops the test in case of failure.
	 * 
	 * @param timezone
	 *            the new timezone Id to check
	 */
	@Then("^the device timezone must be \"(.*?)\"")
	public void assertTimezone(String timezone) {
		DeviceUtils.assertTimezone(timezone);
	}

	/**
	 * Verifies the device timezone. The test will continue to run in case of
	 * failure.
	 * 
	 * @param timezone
	 *            the timezone Id to verify
	 * @return <code>true</code> if the timezone is verified, <code>false</code>
	 *         otherwise
	 */
	@Then("^the device timezone should be \"(.*?)\"")
	public boolean verifyTimezone(String timezone) {
		return DeviceUtils.verifyTimezone(timezone);
	}

	/**
	 * Resets the device timezone Id to the default.
	 */
	@Then("^I reset the device timezone$")
	public void resetTimezone() {
		DeviceUtils.resetTimezone();
	}

	/**
	 * Gets a digital screenshot of the current screen display, and places it in the
	 * report.
	 */
	@Then("^I take a screenshot$")
	public void takeScreenshot() {
		DeviceUtils.takeScreenshot(null, false);
	}

	/**
	 * Gets a digital screenshot of the current screen display, and saves it to the
	 * repository.
	 * 
	 * @param repositoryPath
	 *            the full repository path, including directory and file name, where
	 *            to save the file. Example - PRIVATE:dir1/dir2/name.png
	 */
	@Then("^I take a screenshot and save to \"(.*?)\"$")
	public void takeScreenshot(String repositoryPath) {
		DeviceUtils.takeScreenshot(repositoryPath, true);
	}

	/**
	 * Hides the virtual keyboard display.
	 */
	@Then("^I hide keyboard$")
	public void hideKeyboard() {
		DeviceUtils.hideKeyboard();
	}

	@Then("^I press mobile \"(.*?)\" key$")
	public static void pressMobileKeys(String keySequence) {
		DeviceUtils.pressKey(keySequence);
	}

	/**
	 * Performs the touch gesture according to the point coordinates.
	 *
	 * @param point
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended) for example 50%,50%.
	 */
	@Then("^I touch on \"(.*?)\" point$")
	public static void touch(String point) {
		DeviceUtils.touch(point);
	}

	/**
	 * Performs the double touch gesture according to the point coordinates.
	 *
	 * @param point
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended) for example 50%,50%.
	 */
	@Then("^I double click on \"(.*?)\" point$")
	public static void doubleTouch(String point) {
		DeviceUtils.doubleTouch(point);
	}

	/**
	 * Performs the double touch gesture according to the point coordinates.
	 *
	 * @param locator
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended) for example 50%,50%.
	 */
	@Then("^I double click on \"(.*?)\"")
	public static void doubleClickElement(String locator) {

		QAFExtendedWebElement myElement = new QAFExtendedWebElement(locator);

		Point location = myElement.getLocation();
		Dimension size = myElement.getSize();

		// determine location to click and convert to an appropriate string
		int xToClick = location.getX() + (size.getWidth() / 2);
		int yToClick = location.getY() + (size.getHeight() / 2);
		String clickLocation = xToClick + "," + yToClick;

		DeviceUtils.doubleTouch(clickLocation);

	}

	/**
	 * Performs the lo touch gesture according to the point coordinates.
	 *
	 * @param locator
	 *            write in format of x,y. can be in pixels or
	 *            percentage(recommended) for example 50%,50%.
	 */
	@Then("^I tap on \"(.*?)\" for \"(\\d*\\.?\\d*)\" seconds$")
	public static void tapElement(String locator, int seconds) {

		QAFExtendedWebElement myElement = new QAFExtendedWebElement(locator);

		Point location = myElement.getLocation();
		Dimension size = myElement.getSize();

		// determine location to click and convert to an appropriate string
		int xToClick = location.getX() + (size.getWidth() / 2);
		int yToClick = location.getY() + (size.getHeight() / 2);
		String clickLocation = xToClick + "," + yToClick;

		DeviceUtils.longTouch(clickLocation, seconds);

	}

	/**
	 * Generate Har file. The HAR file will be included in the Reporting artifacts
	 * for the automation report.
	 *
	 */
	@Then("^Start generate Har file$")
	public static void generateHar() {
		DeviceUtils.generateHAR();
	}

	/**
	 * Stop generatimg Har file.
	 *
	 */
	@Then("^Stop generate Har file$")
	public static void stopGenerateHar() {
		DeviceUtils.stopGenerateHAR();
	}

	/**
	 * Add Comment to Report
	 *
	 */
	@Then("^Add report comment \\\"(.*?)\\\"$")
	public static void addReportComment(String comment) {
		ReportUtils.reportComment(comment);
	}
	/**
     * 
     * @param locator - The pickerwheel element must be this specific 
     * 					type ("XCUIElementTypePickerWheel"), not “XCUIElementTypePicker” 
     * 					or any other parent/child of the pickerwheel.
     * @param value - value to compare this must be exact
     * @param direction - Direction to spin the spinner, either next or previous defaults to next
     */
    @Then("^I pick \"(.*?)\" from \"(.*?)\" in the direction \"(.*?)\"$")
    public static void setPickerWheel(String value, String locator, String direction){
    	if(!direction.contains("next") && !direction.contains("previous"))
    	{
    		direction = "next";
    	}
    	DeviceUtils.setPickerWheel( (RemoteWebElement) new QAFExtendedWebElement(locator), direction, value);
    }
    /**
     * 
     * @param locator - The pickerwheel element must be this specific 
     * 					type ("XCUIElementTypePickerWheel"), not “XCUIElementTypePicker” 
     * 					or any other parent/child of the pickerwheel.
     * @param value - value to compare this must be exact
     */
    @Then("^I pick the next \"(.*?)\" from \"(.*?)\"$")
    public static void setNextPickerWheel(String value, String locator){
    	DeviceUtils.setPickerWheel( (RemoteWebElement) new QAFExtendedWebElement(locator), "next", value);
    }
    /**
     * 
     * @param locator - The pickerwheel element must be this specific 
     * 					type ("XCUIElementTypePickerWheel"), not “XCUIElementTypePicker” 
     * 					or any other parent/child of the pickerwheel.
     * @param value - value to compare this must be exact
     */
    @Then("^I pick the previous \"(.*?)\" from \"(.*?)\"$")
    public static void setPreviousPickerWheel(String value, String locator){
    	DeviceUtils.setPickerWheel( (RemoteWebElement) new QAFExtendedWebElement(locator), "previous", value);
    }
    /**
     * Picks the next value of the specific pickerwheel
     * @param locator - The pickerwheel element must be this specific 
     * 					type ("XCUIElementTypePickerWheel"), not “XCUIElementTypePicker” 
     * 					or any other parent/child of the pickerwheel.
     */
    @Then("^I pick the next value from \"(.*?)\"$")
    public static void pickNext(String locator){
    	DeviceUtils.pickerwheelStep((RemoteWebElement) new QAFExtendedWebElement(locator), "next");
    }
    /**
     * Picks the previous value of the specific pickerwheel
     * @param locator - The pickerwheel element must be this specific 
     * 					type ("XCUIElementTypePickerWheel"), not “XCUIElementTypePicker” 
     * 					or any other parent/child of the pickerwheel.
     */
    @Then("^I pick the previous value from \"(.*?)\"$")
    public static void pickPrevious(String locator){
    	DeviceUtils.pickerwheelStep( (RemoteWebElement) new QAFExtendedWebElement(locator), "previous");
    }
    /**
     * Picks the previous value of the specific pickerwheel
     * @param locator - The pickerwheel element must be this specific 
     * 					type ("XCUIElementTypePickerWheel"), not “XCUIElementTypePicker” 
     * 					or any other parent/child of the pickerwheel.
     */
    @Then("^I validate \"(.*?)\" has the value \"(.*?)\"$")
    public static void getValue(String locator, String value){
    	Assert.assertEquals(new QAFExtendedWebElement(locator).getAttribute("value").replaceAll("[^\\x00-\\x7F]", ""), value.replaceAll("[^\\x00-\\x7F]", ""),"The value did not match.");
    }
}
