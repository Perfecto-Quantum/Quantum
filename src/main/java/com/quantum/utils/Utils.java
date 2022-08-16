package com.quantum.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.QAFTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebElement;
import com.quantum.steps.PerfectoApplicationSteps;

import io.appium.java_client.touch.offset.PointOption;

public class Utils {

	public static final String KEY_DEVICE_MODEL_IPHONE_11 = "iPhone-11";

	/**
	 * Return true if element is displayed after X milliseconds.
	 *
	 * @param element   - Element that is to be checked for visibility.
	 * @param milliSecs - the wait duration
	 * @return - true or false
	 */
	public static boolean isDisplayed(QAFWebElement element, int milliSecs) {
		try {
			element.waitForVisible(milliSecs);
			return true;
		} catch (Exception e) {
			System.out.println("Element was not displayed and wait for Visible timed out method threw an exception.");
			return false;
		}
	}

	/**
	 * Return true if element is not present after X milliseconds.
	 *
	 * @param element   - Element that is to be checked for present.
	 * @param milliSecs - the wait duration
	 * @return - true or false
	 */
	public static boolean isNotPresent(QAFWebElement element, int milliSecs) {
		try {
			element.waitForNotPresent(milliSecs);
			return true;
		} catch (Exception e) {
			System.out.println("Element was still displayed and wait for Visible timed out method threw an exception.");
			return false;
		}
	}

	/**
	 * /** Return true if element is present after X milliseconds.
	 *
	 * @param element   - Element that is to be checked for present.
	 * @param milliSecs - the wait duration
	 * @return - true or false
	 */
	public static boolean isPresent(QAFWebElement element, int milliSecs) {
		try {
			element.waitForPresent(milliSecs);
			return true;
		} catch (Exception e) {
			System.out.println("Element was not present and wait for present method threw an exception.");
			return false;
		}
	}

	/**
	 * Return the device property value.
	 *
	 * @param propertyName - Element that is to be checked for visibility.
	 * @return - property value
	 */
	public static String getDeviceProperty(String propertyName) {
		Map<String, Object> params = new HashMap<>();
		params.put("property", propertyName);
		String property = (String) DeviceUtils.getQAFDriver().executeScript("mobile:device:info", params);
		return property;
	}

	/**
	 * Swipe horizontal the device property value.
	 *
	 * @param element - Element that is to be checked for present.
	 * @return - property value
	 */
	public static boolean horizontalSwipe(QAFWebElement element) {
		int x = element.getLocation().getX();
		int y = element.getLocation().getY();
		int height = element.getSize().getHeight();
		int width = element.getSize().getWidth();
		int startX = (width / 4);
		int startY = (height / 2) + y;
		int endX = (width / 2) + x;
		AppiumUtils.getTouchAction().press(PointOption.point(startX, startY)).moveTo(PointOption.point(endX, startY))
				.release().perform();
		return true;
	}

	public static boolean horizontalSwipeWithScaleFactor(QAFWebElement ele) {

		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (width / 4) + x;
		int startY = (height / 2) + y;
		int endX = (width / 2) + x;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + startY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		return true;

	}

	// Function to swipe a web element from right to left
	public static boolean horizntalSwipeWithScaleFactorRightToLeft(QAFWebElement ele) {

		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (3 * width / 4) + x;
		int startY = (height / 2) + y;
		int endX = (width / 3) + x;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + startY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;

	}

	// Function to swipe a web element from right to left
	public static boolean horizntalSwipeWithScaleFactorRightToLeftRemoveCart(QAFWebElement ele) {

		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (3 * width / 4) + x;
		int startY = (height / 2) + y;
		int endX = (width / 4) + x;
		// int endX=startX/5;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + startY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;

	}

	// Function to perform a long swipe on a web element from right to left
	public static boolean horizntalLongSwipeWithScaleFactorRightToLeft(QAFWebElement ele) {

		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (4 * width / 5) + x;
		int startY = (height / 2) + y;
		int endX = 10;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + startY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;

	}

	// Function to swipe a web element from Left to right
	public static boolean horizntalSwipeWithScaleFactorLeftToRight(QAFWebElement ele) {

		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (width / 5) + x;
		int startY = (height / 4) + y;
		int endX = (3 * width / 4) + x;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + startY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;

	}

	// Function to perform a long swipe on a web element from left to right
	public static boolean horizntalLongSwipeWithScaleFactorLeftToRight(QAFWebElement ele) {

		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = width / 6 + x;
		int startY = (height / 2) + y;
		int endX = width + x - 10;
		// x + width - 10;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + startY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;

	}

	// Function to perform a long swipe on a web element from left to right
	public static boolean horizntalLongSwipeWithScaleFactorLeftToRightCustom(QAFWebElement ele) {

		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = width / 6 + x;
		int startY = (height / 2) + y + 100;
		int endX = width + x - 10;
		// x + width - 10;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + startY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;

	}

	// Vertical swipe down with scale factor
	public static boolean verticalSwipeWithScaleFactor(QAFWebElement ele) {
		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (width / 2) + x;
		// int startY = (height) + y - 20;
		int startY = (height) * 3 / 4 + y;
		int endX = (width / 2);
		// int endY = y + 20;
		int endY = (height) / 4 + y;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", startX + "," + endY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;
	}

	// Vertical swipe down with scale factor
	public static boolean verticalSwipeInOrderHistory(QAFWebElement ele) {
		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = ((width / 2) + x) * 2;
		// int startY = (height) + y - 20;
		int startY = ((height) * 3 / 4 + y) * 2;
		int endX = ((width / 2)) * 2;
		// int endY = y + 20;
		int endY = ((height) / 4 + y) * 2;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", startX + "," + endY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;
	}

	// Vertical swipe up with scale factor
	public static boolean verticalSwipeUpWithScaleFactor(QAFWebElement ele) {
		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (width / 2) + x;
		// int startY = (height) + y - 20;
		int startY = (height) * 3 / 4 - y;
		int endX = (width / 2);
		// int endY = y + 20;
		int endY = (height) / 4 - y;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", startX + "," + endY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;
	}

	// Vertical swipe up with scale factor only on the top portion of scroll
	// view
	public static boolean verticalSwipeWithScaleFactorTopHalf(QAFWebElement ele) {
		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (width / 2) + x;
		// int startY = (height) + y - 20;
		int startY = (height) * 1 / 3 + y;
		int endX = (width / 2);
		// int endY = y + 20;
		int endY = (height) / 6 + y;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", startX + "," + endY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;
	}

	// Vertical swipe up half way with scale factor
	public static boolean verticalSwipeHalfwayUp(QAFWebElement ele) {
		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (width / 2) + x;
		int startY = 3 * (height) / 4 + y + 100 * scalevalu;
		int endX = (width / 2);
		int endY = 3 * (height) / 4 + y;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + endY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;
	}

	// Swipe and find an entry with vertical swipe. Also, adjust vertical
	// location if its close to the top/bottom of the page
	public static void verticalSwipeAndFindElementWithLocationAdjustmnt(QAFWebElement parent,
			QAFWebElement elementToFind) {
		boolean entryFound = false;
		for (int i = 0; i < 20; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				int parentLocation = parent.getLocation().getY();
				int childLocation = elementToFind.getLocation().getY();
				int parentHeight = parent.getSize().getHeight();
				if (childLocation > (parentHeight + parentLocation - 30)) {
					Utils.verticalSwipeHalfwayUp(parent);
				} else if (childLocation < (parentLocation + 30)) {
					Utils.verticalSwipeHalfwayDown(parent);
				}
				if (Utils.isDisplayed(elementToFind, 3000)) {
					entryFound = true;
					break;
				}
			} else {
				Utils.verticalSwipeWithScaleFactor(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	// Swipe and find an entry with vertical swipe. Also, adjust vertical
	// location if its close to the top/bottom of the page
	public static void verticalSwipeAndFindElementWithLocationAdjustmntOrdersPage(QAFWebElement parent,
			QAFWebElement elementToFind) {
		boolean entryFound = false;
		verticalSwipeDownWithScaleFactor(parent);
		int scalevalu = DeviceUtils.getScale();
		for (int i = 0; i < 12; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				int parentLocation = parent.getLocation().getY();
				int childLocation = elementToFind.getLocation().getY();
				int parentHeight = parent.getSize().getHeight();
				if (childLocation > (parentHeight + parentLocation - 100 * scalevalu)) {
//					Utils.verticalSwipeWithScaleFactorInCart(parent);
					Utils.verticalSwipeHalfwayUp(parent);
				} else if (childLocation < (parentLocation + 30)) {
					Utils.verticalSwipeHalfwayDown(parent);
				}
				if (Utils.isDisplayed(elementToFind, 3000)) {
					entryFound = true;
					break;
				}
			} else {
				Utils.verticalSwipeWithScaleFactor(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	// Swipe and find an entry with vertical swipe. Also, adjust vertical
	// location if its close to the top/bottom of the page
	public static void verticalSwipeAndFindElementWithLocationAdjustmntCart(QAFWebElement parent,
			QAFWebElement elementToFind) {
		boolean entryFound = false;
		verticalSwipeDownWithScaleFactor(parent);
		int scalevalu = DeviceUtils.getScale();
		for (int i = 0; i < 12; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				int parentLocation = parent.getLocation().getY();
				int childLocation = elementToFind.getLocation().getY();
				int parentHeight = parent.getSize().getHeight();
				if (childLocation > (parentHeight + parentLocation - 100 * scalevalu)) {
					Utils.verticalSwipeWithScaleFactorInCart(parent);
				} else if (childLocation < (parentLocation + 30)) {
					Utils.verticalSwipeHalfwayDown(parent);
				}
				if (Utils.isDisplayed(elementToFind, 3000)) {
					entryFound = true;
					break;
				}
			} else {
				Utils.verticalSwipeWithScaleFactor(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	// Swipe and find an entry with vertical swipe. Also, adjust vertical
	// location if its close to the top/bottom of the page
	public static void verticalSwipeDownAndFindElementWithLocationAdjustmnt(QAFWebElement parent,
			QAFWebElement elementToFind) {
		boolean entryFound = false;
		for (int i = 0; i < 12; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				int parentLocation = parent.getLocation().getY();
				int childLocation = elementToFind.getLocation().getY();
				int parentHeight = parent.getSize().getHeight();
				if (childLocation > (parentHeight + parentLocation - 30)) {
					Utils.verticalSwipeHalfwayUp(parent);
				} else if (childLocation < (parentLocation + 30)) {
					Utils.verticalSwipeHalfwayDown(parent);
				}
				if (Utils.isDisplayed(elementToFind, 3000)) {
					entryFound = true;
					break;
				}
			} else {
				Utils.verticalSwipeDownWithScaleFactor(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	// Swipe and scroll down with vertical swipe.
	public static void verticalSwipeAndScrollDown(QAFWebElement parent) {
		for (int i = 0; i < 5; i++) {
			Utils.verticalSwipeWithScaleFactor(parent);
		}
	}

	// Swipe and find an entry with vertical swipe. Also, adjust vertical
	// location if its close to the top/bottom of the page. This function is
	// specific to Sort andFilter page
	public static void verticalSwipeAndFindElementSortAndFilter(QAFWebElement parent, QAFWebElement elementToFind) {
		boolean entryFound = false;
		for (int i = 0; i < 12; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				int parentLocation = parent.getLocation().getY();
				int childLocation = elementToFind.getLocation().getY();
				int parentHeight = parent.getSize().getHeight();
				for (int j = 0; j < 5; j++) {
					if (childLocation > (parentHeight + parentLocation - 30)) {
						Utils.verticalSwipeHalfwayUp(parent);
					} else if (childLocation < (parentLocation + 30)) {
						Utils.verticalSwipeHalfwayDown(parent);
					}
				}
				entryFound = true;
				break;
			} else {
				Utils.verticalSwipeWithScaleFactor(parent);
			}
		}
		for (int i = 0; i < 12; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				int parentLocation = parent.getLocation().getY();
				int childLocation = elementToFind.getLocation().getY();
				int parentHeight = parent.getSize().getHeight();
				for (int j = 0; j < 5; j++) {
					if (childLocation > (parentHeight + parentLocation - 30)) {
						Utils.verticalSwipeHalfwayUp(parent);
					} else if (childLocation < (parentLocation + 30)) {
						Utils.verticalSwipeHalfwayDown(parent);
					}
				}
				entryFound = true;
				break;
			} else {
				Utils.verticalSwipeDownWithScaleFactor(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	// Swipe and find an entry with vertical swipe
	public static void verticalSwipeAndFindElement(QAFWebElement parent, QAFWebElement elementToFind) {
		boolean entryFound = false;
		for (int i = 0; i < 12; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				entryFound = true;
				break;
			} else {
				Utils.verticalSwipeWithScaleFactor(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	public static boolean verticalSwipeWithScaleFactorInCart(QAFWebElement parent) {
		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = parent.getLocation().getX() * scalevalu;
		int y = parent.getLocation().getY() * scalevalu;
		int height = parent.getSize().getHeight() * scalevalu;
		int width = parent.getSize().getWidth() * scalevalu;
		int startX = (width / 2) + x;
		int startY = (height) + y - 20;
		// int startY = (height) * 1 / 2 + y;
		int endX = (width / 2);
		int endY = y + 20;
		// int endY = (height) / 6 + y;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", startX + "," + endY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;
	}

	// Swipe vertically and find element in cart
	public static void verticalSwipeAndFindElementInCart(QAFWebElement parent, QAFWebElement elementToFind) {
		boolean entryFound = false;
		for (int i = 0; i < 51; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				entryFound = true;
				break;
			} else {
				verticalSwipeWithScaleFactorInCart(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	// Swipe and find an entry with vertical swipe only on the top half of
	// scroll view
	public static void verticalSwipeAndFindElementTopHalf(QAFWebElement parent, QAFWebElement elementToFind) {
		boolean entryFound = false;
		for (int i = 0; i < 12; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				entryFound = true;
				break;
			} else {
//				Utils.verticalSwipeWithScaleFactorTopHalf(parent);
				Utils.verticalSwipeWithScaleFactor(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	// Swipe and validate entry is not found
	public static void verticalSwipeAndValidateElementNotPresent(QAFWebElement parent, QAFWebElement elementToFind) {
		boolean entryFound = false;
		for (int i = 0; i < 12; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				entryFound = true;
				break;
			} else {
				Utils.verticalSwipeWithScaleFactor(parent);
			}
		}
		if (entryFound) {
			ReportUtils.logAssert("Element found while it is not expected to be found", false);
		} else {
			ReportUtils.logAssert("Element not found as expected", true);
		}
	}

	// Swipe specified number of times and validate entry is not found
	public static void verticalSwipeAndValidateElementNotPresent(QAFWebElement parent, QAFWebElement elementToFind,
			int numTimesToSwipe) {
		boolean entryFound = false;
		for (int i = 0; i < numTimesToSwipe; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				entryFound = true;
				break;
			} else {
				Utils.verticalSwipeWithScaleFactor(parent);
			}
		}
		if (entryFound) {
			ReportUtils.logAssert("Element found while it is not expected to be found", false);
		} else {
			ReportUtils.logAssert("Element not found as expected", true);
		}
	}

	// Vertical swipe down halfway with scale factor
	public static boolean verticalSwipeHalfwayDown(QAFWebElement ele) {

		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (width / 2) + x;
		int startY = height / 6 + y;
		int endX = (width / 2);
		int endY = height / 6 + y + 100 * scalevalu;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + endY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;
	}

	// Vartical swipe down with scale factor
	public static boolean verticalSwipeDownWithScaleFactor(QAFWebElement ele) {

		int scalevalu = DeviceUtils.getScale();
		System.out.println("Device Status" + scalevalu);
		int x = ele.getLocation().getX() * scalevalu;
		int y = ele.getLocation().getY() * scalevalu;
		int height = ele.getSize().getHeight() * scalevalu;
		int width = ele.getSize().getWidth() * scalevalu;
		int startX = (width / 2) + x;
		int startY = y + 20;
		int endX = (width / 2);
		int endY = (height) + y - 20;
		System.out.println("variables" + startX + startY + endX);

		Map<String, Object> params = new HashMap<>();
		params.put("start", startX + "," + startY);
		params.put("end", endX + "," + endY);
		params.put("duration", 2);
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
		// AppiumUtils.getTouchAction().press(startX, startY).moveTo(endX,
		// startY).release().perform();
		return true;
	}

	// Swipe and find an entry with vertical swipe downwards
	public static void verticalSwipeDownAndFindElement(QAFWebElement parent, QAFWebElement elementToFind) {
		boolean entryFound = false;
		for (int i = 0; i < 10; i++) {
			if (Utils.isDisplayed(elementToFind, 1000)) {
				entryFound = true;
				break;
			} else {
				Utils.verticalSwipeDownWithScaleFactor(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	// Swipe horizontally and find element
	public static void horizontalSwipeRightAndFindElement(QAFWebElement parent, QAFWebElement elementToFind) {
		boolean entryFound = false;
		for (int i = 0; i < 12; i++) {
			if (Utils.isDisplayed(elementToFind, 10000)) {
				// entryFound = true;
				break;
			} else {
				Utils.horizntalLongSwipeWithScaleFactorRightToLeft(parent);
			}
		}
		if (!entryFound) {
			ReportUtils.logAssert("Could not find element", false);
		}
	}

	public static boolean tapCenterOfElement(QAFWebElement ele) {

		int eleX = ele.getLocation().getX();
		int eleY = ele.getLocation().getY();
		int height = ele.getSize().getHeight();
		int width = ele.getSize().getWidth();
		int X = eleX + (width / 2);
		int Y = eleY + (height / 2);
		AppiumUtils.getTouchAction().press(PointOption.point(X, Y)).release().perform();
		return true;
	}

//
//    public static void objectOptimizationOnDevice(String deviceModel, boolean status) {
//        if (deviceModel.equalsIgnoreCase(Utils.getDeviceInfo())) {
//            MoreActions.objectOptimizationXcuiTest(status);
//        }
//    }

	/**
	 * This method will validate that the radio button is selected for the text that
	 * is present on the right of the button.
	 *
	 * @param text - The text of the button
	 */
	public static boolean validateRadioButtonSelected(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/Radio_Selected_button.png");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "right");
		imageParams.put("relation.inline", "horizontal");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	// To validate the value arrow is pointing to
	public static boolean validateArrowBtnPointedTo(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/Up-arrow-updated.png");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "above");
		imageParams.put("relation.inline", "vertical");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	// To validate reclamation entry icon
	public static boolean validateREIcon(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/RE_Icon.png");
		imageParams.put("threshold", "80");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "right");
		imageParams.put("relation.inline", "horizontal");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	// To validate reclamation inquiry icon
	public static boolean validateRIIcon(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/RI_Icon.png");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "right");
		imageParams.put("relation.inline", "horizontal");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	// To validate maintenance icon
	public static boolean validateMaintenanceIcon(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/Maintenance_Icon.png");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "right");
		imageParams.put("relation.inline", "horizontal");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	// To validate tag icon
	public static boolean validateTagIcon(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/Tag_Icon_New.png");
		imageParams.put("threshold", "80");
		imageParams.put("timeout", "90");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "right");
		imageParams.put("relation.inline", "horizontal");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	// To validate purchase flag icon
	public static boolean validatePurchaseFlagIcon(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/Flag_Icon.png");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "right");
		imageParams.put("relation.inline", "horizontal");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	// To validate claim icon
	public static boolean validateClaimIcon(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/Claim_Icon.png");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "right");
		imageParams.put("relation.inline", "horizontal");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	// To validate order icon
	public static boolean validateOrderIcon(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/Order_Icon.png");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "right");
		imageParams.put("relation.inline", "horizontal");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	public static boolean validatePriceAuditIcon(RemoteWebDriver driver, String text) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("content", "\"" + text + "\"");
//		params.put("source", "native");
		params.put("content", text);
		driver.executeScript("mobile:text:find", params);

		Map<String, String> imageParams = new HashMap<String, String>();
		imageParams.put("content", "PUBLIC:iUNFI_Images/PriceAuditTaskImage.png");
		imageParams.put("match", "bounded");
		imageParams.put("relation.direction", "right");
		imageParams.put("relation.inline", "horizontal");

		Object result = driver.executeScript("mobile:checkpoint:image", imageParams);
		return Boolean.parseBoolean(result.toString());
	}

	public static String truncate(String value, int length) {
		// Ensure String length is longer than requested size.
		if (value.length() > length) {
			return value.substring(0, length);
		} else {
			return value;
		}
	}

	public static String truncateLastTwoDigit(String value, int length) {
		// Ensure String length is longer than requested size.
		if (value.length() > length) {
			return value.substring(length - 1, value.length());
		} else {
			return value;
		}
	}

	public static String dateFormatting(String fromDate) {
		Date date;
		String newDateFormat = "";
		try {
			DateFormat formatter = new SimpleDateFormat("M/dd/yy");
			date = (Date) formatter.parse(fromDate);
			formatter = new SimpleDateFormat("MM/dd/yy");
			newDateFormat = (String) formatter.format(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newDateFormat;
	}

	public static void coordinateClick(QAFWebElement ele) {
		Point captureBtnPoint = ele.getLocation();
		Dimension size = ele.getSize();
		int width = size.getWidth();
		int height = size.getHeight();
		int scale = DeviceUtils.getScale();

		int x = (captureBtnPoint.getX() * scale) + ((width * scale) / 2);
		int y = 0;
		if ((KEY_DEVICE_MODEL_IPHONE_11.equalsIgnoreCase(Utils.getDeviceProperty("model")))) {
//			Assuming 8 pixel of the upper notch of the phone - will need to confirm in the next execution that this has not adversely impacted other cases
			y = captureBtnPoint.getY() * scale + ((height * scale) / 2) + 10;
		} else {
			y = captureBtnPoint.getY() * scale + ((height * scale) / 2);
		}
		Map<String, String> params2 = new HashMap<String, String>();
		params2.put("location", x + "," + y);
		params2.put("duration", "1");

		DeviceUtils.getQAFDriver().executeScript("mobile:touch:tap", params2);
	}

	public static void installApp(String buildPath) {
		Map<String, Object> params = new HashMap<>();

		params.put("file", buildPath);
		params.put("instrument", "noinstrument");
		params.put("resign", "true");
		DeviceUtils.getQAFDriver().executeScript("mobile:application:install", params);
	}

	public static void verticalScrollWithPercent() {
		Map<String, Object> params = new HashMap<>();
		params.put("start", "50%,35%");
		params.put("end", "50%,80%");
		params.put("duration", "10");
		DeviceUtils.getQAFDriver().executeScript("mobile:touch:swipe", params);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void switchAirPlaneModeOnOff(boolean status) {
		// if (ConfigurationManager.getBundle().getString("OSType", "not
		// mentioned").equalsIgnoreCase("iOS")) {
		if (DeviceUtils.getQAFDriver().getCapabilities().getPlatform().equals(Platform.IOS)) {

			Map<String, Object> appName = new HashMap<>();
			appName.put("identifier", "com.apple.Preferences");
			DeviceUtils.getQAFDriver().executeScript("mobile:application:open", appName);
			QAFTestBase.pause(1000);
			DeviceUtils.getQAFDriver().executeScript("mobile:application:close", appName);
			QAFTestBase.pause(1000);
			DeviceUtils.getQAFDriver().executeScript("mobile:application:open", appName);
			QAFTestBase.pause(1000);
			// PerfectoApplicationSteps.switchNativeContext();
			// DeviceUtils.getQAFDriver().findElementByXPath("//*[@value=\"Wi-Fi\"]").click();

			PerfectoApplicationSteps.switchNativeContext();
			WebElement switchOnOff = DeviceUtils.getQAFDriver()
					.findElement(By.xpath("//UIASwitch[@label='Airplane Mode']"));
			if (status && switchOnOff.getAttribute("value").equals("0")) {
				switchOnOff.click();
				ConfigurationManager.getBundle().setProperty("currentAirplaneStatus", "true");
			} else if (!status && switchOnOff.getAttribute("value").equals("1")) {
				switchOnOff.click();
				ConfigurationManager.getBundle().setProperty("currentAirplaneStatus", "false");

			}
			DeviceUtils.closeApp("identifier", "com.apple.Preferences");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// switchOnOff.click();
			// CommonFunctionsUtility.launchApp();
			DeviceUtils.startApp("iUNFI", "name");
			DeviceUtils.switchToContext("NATIVE");
			QAFTestBase.pause(2 * 1000);
			// PerfectoApplicationSteps.switchWebviewContext();
		} else {
			System.out.println("Device OS was not mentioned");
		}
	}

	public static void performingEnterKeyOnSpecifiedTextBox(QAFWebElement webElement_textBox) {
		JavascriptExecutor jsExec = DeviceUtils.getQAFDriver();
		String enterKeyPressScript = "let input = arguments[0]; \r\n"
				+ "let keydownEvent = new KeyboardEvent(\"keydown\", {\r\n"
				+ "    bubbles: true, cancelable: true, keyCode: 13\r\n" + "});\r\n"
				+ "input.dispatchEvent(keydownEvent);\r\n" + "let keyupEvent = new KeyboardEvent(\"keyup\", {\r\n"
				+ "    bubbles: true, cancelable: true, keyCode: 13\r\n" + "});\r\n"
				+ "input.dispatchEvent(keyupEvent);";

		jsExec.executeScript(enterKeyPressScript, webElement_textBox);
	}

	/**
	 * Inputting text into the text box using Javascript code
	 *
	 */
	public static void javaScriptSendkeys(QAFWebElement webElementTextBox, String text) {
		JavascriptExecutor jsExec = DeviceUtils.getQAFDriver();
		String jsScript = "let input = arguments[0]; \r\n" + "let lastValue = input.value;\r\n"
				+ "input.value = arguments[1];\r\n" + "let event = new Event('input', { bubbles: true });\r\n"
				+ "// hack React15\r\n" + "event.simulated = true;\r\n" + "// hack React16 descriptor value，\r\n"
				+ "let tracker = input._valueTracker;\r\n" + "if (tracker) {\r\n" + "  tracker.setValue(lastValue);\r\n"
				+ "}\r\n" + "input.dispatchEvent(event);";

		jsExec.executeScript(jsScript, webElementTextBox, text);
	}

	/**
	 * Click event from javascript
	 *
	 * @param element : element to be clicked
	 */
	public static void javaScriptClick(QAFWebElement element) {
		JavascriptExecutor executor = DeviceUtils.getQAFDriver();
		executor.executeScript("arguments[0].click();", element);
	}

	/**
	 * Click event from javascript
	 *
	 * @param element : element to be clicked
	 */
	public static void javaScriptClickEvent(QAFWebElement element) {
		JavascriptExecutor js = (JavascriptExecutor) DeviceUtils.getQAFDriver();
		String onClickScript = "if(document.createEvent){var evObj = document.createEvent('MouseEvents');evObj.initEvent('click',true, false);arguments[0].dispatchEvent(evObj);} else if(document.createEventObject){ arguments[0].fireEvent('onclick');}";
		String mouseHoverScript = "if(document.createEvent){var evObj = document.createEvent('MouseEvents');evObj.initEvent('mouseover',true, false); arguments[0].dispatchEvent(evObj);} else if(document.createEventObject){ arguments[0].fireEvent('onmouseover');}";

		js.executeScript(mouseHoverScript, element);
		js.executeScript(onClickScript, element);
	}

	/**
	 * Scroll to element using Javascript code
	 *
	 * @param element : element to be clicked
	 */
	public static void javaScriptScrollToElement(QAFWebElement element) {
		JavascriptExecutor executor = DeviceUtils.getQAFDriver();
		executor.executeScript("arguments[0].scrollIntoView();", element);
	}

	/**
	 * Scroll down to the end of the page using using Javascript code
	 *
	 */
	public static void scrollDownTillEndOfThePage() {
		if (DeviceUtils.getQAFDriver().getCapabilities().getBrowserName().equalsIgnoreCase("Internet Explorer")) {
//            Utils.pauseElement(3000);
			QAFTestBase.pause(3000);
			JavascriptExecutor jse = DeviceUtils.getQAFDriver();
			jse.executeScript("window.scrollBy(0,8000)");
		} else {
			QAFTestBase.pause(3000);
			JavascriptExecutor jse = DeviceUtils.getQAFDriver();
			jse.executeScript("window.scrollTo(0, document.body.scrollHeight)");
		}
		QAFTestBase.pause(3000);
		// JavascriptExecutor jse=(JavascriptExecutor)driver;
		// jse.executeScript("document.getElementById(<id>).scrollIntoView(true)");
		// //Utils.pauseElement(3000);
	}

}