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


package com.perfectomobile.quantum.steps;

import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

/**
 * com.qmetry.qaf.automation.step.AppiumSteps.java
 * 
 * @author chirag.jayswal
 */
public final class AppiumSteps {
	   /**
     * Swipe from Bottom to Top.
     */
    public static void swipeUp() {
        Point[] points = getXYtoVSwipe();
        getDriver().swipe(points[0].x, points[0].y, points[1].x, points[1].y, 1);
    }
    
    /**
     * Swipe from Top to Bottom.
     */
    public static void swipeDown() {
        Point[] points = getXYtoVSwipe();
        getDriver().swipe(points[1].x, points[1].y, points[0].x, points[0].y, 1);
    }
    
    /**
     * Swipe from Right to Left.
     */
    public static void swipeLeft() {
        Point[] points = getXYtoHSwipe();
        getDriver().swipe(points[0].x, points[0].y, points[1].x, points[1].y, 1);
    }
    
    /**
     * Swipe from Left to Right
     */
    public static void swipeRight() {
        Point[] points = getXYtoHSwipe();
        getDriver().swipe(points[1].x, points[1].y, points[0].x, points[0].y, 1);
    }
    
    /**
     *
     * @return start and end points for vertical(top-bottom) swipe
     */
    private static Point[] getXYtoVSwipe() {
        // Get screen size.
        Dimension size = getDriver().manage().window().getSize();
        
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
        Dimension size = getDriver().manage().window().getSize();
        
        //Find starting point x which is at right side of screen.
        int startx = (int) (size.width * 0.70);
        //Find ending point x which is at left side of screen.
        int endx = (int) (size.width * 0.30);
        //Find y which is in middle of screen height.
        int startEndy = size.height / 2;
        
        return new Point[] { new Point(startx, startEndy), new Point(endx, startEndy) };
    }
    
    private static AppiumDriver getDriver() {
        return (AppiumDriver) new WebDriverTestBase().getDriver().getUnderLayingDriver();
    }

}
