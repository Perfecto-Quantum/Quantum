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


package com.quantum.utils;

import com.google.common.base.Function;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class AppiumUtils {

    public static AppiumDriver getAppiumDriver() {
        return checkType(AppiumDriver.class);
    }

    public static IOSDriver getIOSDriver() {
        return checkType(IOSDriver.class);
    }

    public static AndroidDriver getAndroidDriver() {
        return checkType(AndroidDriver.class);
    }

	   /**
     * Swipe from Bottom to Top.
     */
    public static void swipeUp() {
        Point[] points = getXYtoVSwipe();
        getAppiumDriver().swipe(points[0].x, points[0].y, points[1].x, points[1].y, 1);
    }
    
    /**
     * Swipe from Top to Bottom.
     */
    public static void swipeDown() {
        Point[] points = getXYtoVSwipe();
        getAppiumDriver().swipe(points[1].x, points[1].y, points[0].x, points[0].y, 1);
    }
    
    /**
     * Swipe from Right to Left.
     */
    public static void swipeLeft() {
        Point[] points = getXYtoHSwipe();
        getAppiumDriver().swipe(points[0].x, points[0].y, points[1].x, points[1].y, 1);
    }
    
    /**
     * Swipe from Left to Right
     */
    public static void swipeRight() {
        Point[] points = getXYtoHSwipe();
        getAppiumDriver().swipe(points[1].x, points[1].y, points[0].x, points[0].y, 1);
    }
    
    /**
     *
     * @return start and end points for vertical(top-bottom) swipe
     */
    private static Point[] getXYtoVSwipe() {
        // Get screen size.
        Dimension size = getAppiumDriver().manage().window().getSize();
        
        // Find x which is in middle of screen width.
        int startEndx = size.width / 2;
        // Find starty point which is at bottom side of screen.
        int starty = (int) (size.height * 0.70);
        // Find endy point which is at top side of screen.
        int endy = (int) (size.height * 0.30);
        
        return new Point[] { new Point(startEndx, starty), new Point(startEndx, endy) };
    }
    
    /**
     *
     * @return start and end points for horizontal(left-right) swipe
     */
    private static Point[] getXYtoHSwipe() {
        // Get screen size.
        Dimension size = getAppiumDriver().manage().window().getSize();
        
        //Find starting point x which is at right side of screen.
        int startx = (int) (size.width * 0.70);
        //Find ending point x which is at left side of screen.
        int endx = (int) (size.width * 0.30);
        //Find y which is in middle of screen height.
        int startEndy = size.height / 2;
        
        return new Point[] { new Point(startx, startEndy), new Point(endx, startEndy) };
    }

    // IOS Implementation

    // Generic scroll using send keys
    // Pass in values to be selected as a String array to the list parameter
    // Method will loop through looking for scroll wheels based on the number of
    // values you supply
    // For instance Month, Day, Year for a birthday would have this loop 3 times
    // dynamically selecting each scroll wheel
    public static void iosScrollKeys(String[] list) {
        System.out.println("Starting the process");
        for (int i = 0; i < list.length; i++) {
            MobileElement we = (MobileElement) getIOSDriver().findElementByXPath("//UIAPickerWheel["+(i+1)+"]");
            we.sendKeys(list[i]);
        }
        System.out.println("Ending Process");
    }

    // Scrolling using a slower however necessary process in some cases where
    // the value can't be set for ios scroll wheels
    // Pass in values to be selected as a String array to the list parameter
    // Method will loop through looking for scroll wheels based on the number of
    // values you supply
    // For instance Month, Day, Year for a birthday would have this loop 3 times
    // dynamically selecting each scroll wheel
    // Some additional leg work needs to be done here to determine if you should
    // be scrolling up or down
    // In the below implementation i'm checking the current month against the
    // month i supplied (by name) and
    // based on this i'm telling the scroll code to go up or down searching for
    // the value
    // additionally i'm checking the same for the day and year
    public static void iosScrollChecker(String[] list) {
        for (int i = 0; i < list.length; i++) {

            IOSDriver driver = getIOSDriver();
            MobileElement me = (MobileElement) driver.findElement(By.xpath("//UIAPickerWheel["+(i+1)+"]"));
            int mget = getMonthInt(me.getText().split(",")[0]);

            if (i == 0) {
                if (mget > getMonthInt(list[i])) {
                    scrollAndSearch(driver, list[i], me, true);
                } else {
                    scrollAndSearch(driver, list[i], me, false);
                }
            } else {
                if (Integer.parseInt(me.getText().split(",")[0]) > Integer.parseInt(list[i])) {
                    scrollAndSearch(driver, list[i], me, true);
                } else {
                    scrollAndSearch(driver, list[i], me, false);
                }
            }
        }
    }

    // Used to get the dynamic location of an object
    // Code here shouldn't be modified
    private static void scrollAndSearch(IOSDriver driver, String value, MobileElement me, Boolean direction) {
        String x = getLocationX(me);
        String y = getLocationY(me);
        while (!driver.findElementByXPath(getXpathFromElement(me)).getText().contains(value)) {
            swipe(driver, x, y, direction);
        }
    }

    // Performs the swipe and search operation
    // Code here shouldn't be modified
    private static void swipe(IOSDriver driver, String start, String end, Boolean up) {
        String direction;
        if (up) {
            direction = start + "," + (Integer.parseInt(end) + 70);
        } else {
            direction = start + "," + (Integer.parseInt(end) - 70);
        }

        Map<String, Object> params1 = new HashMap<>();
        params1.put("location", start + "," + end);
        params1.put("operation", "down");
        driver.executeScript("mobile:touch:tap", params1);

        Map<String, Object> params2 = new HashMap<>();
        List<String> coordinates2 = new ArrayList<>();

        coordinates2.add(direction);
        params2.put("location", coordinates2);
        params2.put("auxiliary", "notap");
        params2.put("duration", "3");
        driver.executeScript("mobile:touch:drag", params2);

        Map<String, Object> params3 = new HashMap<>();
        params3.put("location", direction);
        params3.put("operation", "up");
        driver.executeScript("mobile:touch:tap", params3);
    }

    // Android Implementation

    // Generic scroll using send keys
    // Pass in values to be selected as a String array to the list parameter
    // Method will loop through looking for scroll wheels based on the number of
    // values you supply
    // For instance Month, Day, Year for a birthday would have this loop 3 times
    // dynamically selecting each scroll wheel
    // Code here shouldn't be modified
    public static void androidScrollKeys(String[] list) {
        AndroidDriver driver = getAndroidDriver();
        for (int i = 0; i < list.length; i++) {

            By meX = By.xpath("//android.widget.NumberPicker["+(i+1)+"]/android.widget.EditText[1]");
            fluentWait(driver, meX);
            MobileElement me = (MobileElement) driver.findElement(meX);

            TouchAction touchAction6 = new TouchAction(driver);
            touchAction6.longPress(me).release();
            driver.performTouchAction(touchAction6);

            driver.getKeyboard().pressKey(convertAndroidMonthName(list[i]) + "");
        }
    }

    // Used to get the integer for a month based on the string of the month
    private static int getMonthInt(String month) {
        int monthInt = 0;
        switch (month) {
            case "Jan":
                monthInt = 1;
                break;
            case "January":
                monthInt = 1;
                break;
            case "February":
                monthInt = 2;
                break;
            case "Feb":
                monthInt = 2;
                break;
            case "March":
                monthInt = 3;
                break;
            case "Mar":
                monthInt = 3;
                break;
            case "April":
                monthInt = 4;
                break;
            case "Apr":
                monthInt = 4;
                break;
            case "May":
                monthInt = 5;
                break;
            case "June":
                monthInt = 6;
                break;
            case "Jun":
                monthInt = 6;
                break;
            case "July":
                monthInt = 7;
                break;
            case "Jul":
                monthInt = 7;
                break;
            case "August":
                monthInt = 8;
                break;
            case "Aug":
                monthInt = 8;
                break;
            case "September":
                monthInt = 9;
                break;
            case "Sep":
                monthInt = 9;
                break;
            case "October":
                monthInt = 10;
                break;
            case "Oct":
                monthInt = 10;
                break;
            case "November":
                monthInt = 11;
                break;
            case "Nov":
                monthInt = 11;
                break;
            case "December":
                monthInt = 12;
                break;
            case "Dec":
                monthInt = 12;
                break;
        }
        return monthInt;
    }

    // converts the full string of the month to androids short form name
    private static String convertAndroidMonthName(String month) {
        String monthName = "";
        switch (month) {
            case "January":
                monthName = "Jan";
                break;
            case "February":
                monthName = "Feb";
                break;
            case "March":
                monthName = "Mar";
                break;
            case "April":
                monthName = "Apr";
                break;
            case "May":
                monthName = "May";
                break;
            case "June":
                monthName = "Jun";
                break;
            case "July":
                monthName = "Jul";
                break;
            case "August":
                monthName = "Aug";
                break;
            case "September":
                monthName = "Sep";
                break;
            case "October":
                monthName = "Oct";
                break;
            case "November":
                monthName = "Nov";
                break;
            case "December":
                monthName = "Dec";
                break;
            default:
                monthName = month;
                break;
        }
        return monthName;
    }

    // Parses webelement to retrieve the xpath used for identification
    private static String getXpathFromElement(MobileElement me) {
        return (me.toString().split("-> xpath: ")[1]).substring(0, (me.toString().split("-> xpath: ")[1]).length() - 1);
    }

    // Gets the objects X location in pixels
    private static String getLocationX(MobileElement me) {
        int x = me.getLocation().x;
        int width = (Integer.parseInt(me.getAttribute("width")) / 2) + x;
        return width + "";
    }

    // Gets the objects X location in pixels
    private static String getLocationY(MobileElement me) {
        int y = me.getLocation().y;
        int height = (Integer.parseInt(me.getAttribute("height")) / 2) + y;
        return height + "";
    }

    // performs a wait command on a web element
    private static MobileElement fluentWait(AppiumDriver driver, By xpath) {
        MobileElement waitElement = null;

        FluentWait<RemoteWebDriver> fwait = new FluentWait<RemoteWebDriver>(driver).withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS).ignoring(NoSuchElementException.class)
                .ignoring(TimeoutException.class);

        try {
            waitElement = (MobileElement) fwait.until(new Function<RemoteWebDriver, WebElement>() {
                public WebElement apply(RemoteWebDriver driver) {
                    return driver.findElement(xpath);
                }
            });
        } catch (Exception e) {
        }
        return waitElement;
    }

    @SuppressWarnings(value = "Unchecked")
    private static <T> T checkType(Class<T> expectedClass) {
        T driver = (T) new WebDriverTestBase().getDriver().getUnderLayingDriver();
        if (expectedClass.isInstance(driver))
            return driver;
        else throw new ClassCastException(driverErrorMsg(expectedClass, driver.getClass()));
    }

    private static String driverErrorMsg(Class expectedClass, Class driverClass) {
        String stepWarning = String.format("Underlying driver is an %s.  This step requires an %s.", driverClass.getSimpleName(), expectedClass.getSimpleName());
        ConsoleUtils.logWarningBlocks("ERROR: " + stepWarning);
        return String.format(stepWarning + "\n\tSet following properties to use required driver:" +
                "\n\t\tperfecto.capabilities.driverClass=%s" +
                "\n\t\tdriver.name=perfectoDriver", expectedClass.getName());
    }
}
