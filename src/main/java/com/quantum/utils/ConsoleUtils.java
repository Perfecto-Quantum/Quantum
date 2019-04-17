package com.quantum.utils;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.testng.Reporter;

/**
 *
 */
public class ConsoleUtils {

	/*219 DB  U+2588      █   full block
	220 DC  U+2584      ▄   lower half block
	221 DD  U+258C      ▌   left half block
	222 DE  U+2590      ▐   right half block
	223 DF  U+2580      ▀   upper half block*/

	public static final String block = new String(Character.toChars(0x2588));
	public static final String upper_block = new String(Character.toChars(0x2580));
	public static final String lower_block = new String(Character.toChars(0x2584));
	public static final String right_block = new String(Character.toChars(0x2590));
	public static final String left_block = new String(Character.toChars(0x258C));
	public static final String WARNING_PADDING = upper_block + lower_block;

	public static String getDeviceDesc(Capabilities caps) {
		if (ConfigurationUtils.isDevice(caps))
			return ("" + caps.getCapability("model")).replace(" ", "") + " " + getDeviceName(caps);
		else return getPlatformName(caps) + " " + getDeviceName(caps) + " " + getVersion(caps);
	}
	
	public static String getDeviceName(Capabilities caps) {
		return caps.getCapability("deviceName") == null ? 
				(caps.getCapability("deviceDbName") == null ?
						(caps.getCapability("description") == null ? "" : caps.getCapability("description") + "")
						: caps.getCapability("deviceDbName") + "")
				: caps.getCapability("deviceName") + "";
	}
	
	public static String getPlatformName(Capabilities caps) {
		return caps.getCapability("platformName") == null ? 
				(ConfigurationUtils.isDesktopBrowser(caps) ?
						("ANY".equals(caps.getCapability("platform")) 
							? "Desktop" : caps.getCapability("platform") + "") 
						: (caps.getCapability("os") == null ? "Device" : caps.getCapability("os") +"")) 
				: caps.getCapability("platformName") + "";
	}
	
	public static String getVersion(Capabilities caps) {
		return caps.getCapability("platformVersion") == null ? 
				(caps.getVersion() == null || caps.getVersion().isEmpty() ?
						caps.getBrowserName() : caps.getVersion())
				: caps.getCapability("platformVersion") + "";
	}



	public static String getTestName(Capabilities caps) {
		return caps.getCapability("scriptName") == null ? "" : caps.getCapability("scriptName") + "";
	}

	public static String getThreadName() {
		return getThreadName(ConfigurationUtils.getActualDeviceCapabilities(ConfigurationUtils.getTestBundle()));
	}

	public static String getThreadName(Capabilities caps){
		return String.format("[%s]", String.format("%5.41s", getDeviceDesc(caps).replace("null", "")));
	}
	
	public static void setThreadName(Capabilities caps) {
		Thread.currentThread().setName(getThreadName(caps)); 
	}

	public static void setThreadName() {
		Thread.currentThread().setName(getThreadName());
	}
	
	public static synchronized void logThread(String msg) {
		Reporter.log(msg, false);
		System.out.println(Thread.currentThread().getName() + ": " + msg);
		System.out.flush();
	}

	public static synchronized void logWarningBlocks(String msg) {
		msg = (msg + "").split("\n")[0];
		logThread(StringUtils.center(upper_block + " " + msg + " ", msg.length() + 12, WARNING_PADDING));
	}

	public static synchronized void logInfoBlocks(String msg) {
		logInfoBlocks(msg, block + " ", 12);
	}

	public static synchronized void logInfoBlocks(String msg, int padding) {
		logInfoBlocks(msg, block + " ", padding);
	}

	public static synchronized void logInfoBlocks(String msg, String blockType) {
		logInfoBlocks(msg, blockType, 12);
	}

	public static synchronized void logInfoBlocks(String msg, String blockType, int padding) {
		msg = (" " + msg +  " ").split("\n")[0];
		logThread(StringUtils.center(msg, msg.length() + padding, blockType));
	}

	public static synchronized void printBlockRow(String msg, String blockType) {
		printBlockRow(msg, blockType, 2);
	}

	public static synchronized void printBlockRow(String msg, String blockType, int padding) {
		logThread(block + StringUtils.repeat(blockType, (msg.length() + padding) / blockType.length()) + block);
	}

	public static synchronized void surroundWithSquare(String msg) {
		printBlockRow(msg, ConsoleUtils.upper_block, 2);
		logInfoBlocks(msg, 2);
		printBlockRow(msg, ConsoleUtils.lower_block, 2);
	}
	
	public static synchronized void logError(String msg) {
		logError(msg, 1);
	}
	
	public static synchronized void logFullError(String msg) {
		logError(msg, 10000);
	}

	public static synchronized void logError(String msg, int numErrorLinesToPrint) {
		Iterator<String> msgLines = Arrays.asList((msg + "").split("\n")).listIterator();
		msg = "";
		while (numErrorLinesToPrint-- > 0 && msgLines.hasNext()) {
			msg = msgLines.next();
			Reporter.log(msg, false);
			System.err.println(Thread.currentThread().getName() + ": " + msg);
			System.err.flush();
			msg = "\t";
		}
	}

}
