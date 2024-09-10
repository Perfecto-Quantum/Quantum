package com.qmetry.qaf.automation.ui;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.safari.SafariOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public interface IBeforeLocalDriverInit {
	
	public static String CHROME_DRIVER_EXEC_PATH_KEY = "webdriver.chrome.driver";
	public static String FIREFOX_DRIVER_EXEC_PATH_KEY = "webdriver.firefox.driver";
	public static String EDGE_DRIVER_EXEC_PATH_KEY = "webdriver.edge.driver";
	
	default boolean hasLocalDriverExecPath(String key) {
		return System.getProperty(key)!=null;
	}
	
	default void setUpBrowserExec(Class<? extends WebDriver> of) {
		String className = of.getName().toUpperCase();
		
		if(className.contains("CHROMEDRIVER")) {
			
			if(!hasLocalDriverExecPath(CHROME_DRIVER_EXEC_PATH_KEY)) {
				WebDriverManager.chromedriver().setup();
			}

			return;
		}
		
		if(className.contains("FIREFOXDRIVER")) {
			if(!hasLocalDriverExecPath(FIREFOX_DRIVER_EXEC_PATH_KEY)) {
				WebDriverManager.firefoxdriver().setup();
			}
			return;
		}
		
		if(className.contains("EDGEDRIVER")) {
			
			if(!hasLocalDriverExecPath(EDGE_DRIVER_EXEC_PATH_KEY)) {
				WebDriverManager.edgedriver().setup();
			}
			return;
		}
		
		if(className.contains("SAFARIDRIVER")) {
			return ;
		}
		
		throw new UnsupportedBrowserException(className);
	}
	
	default AbstractDriverOptions<?> getDriverOptions(Class<? extends WebDriver> of, Capabilities capabilities) throws UnsupportedBrowserException{
		String className = of.getName().toUpperCase();
		
		AbstractDriverOptions<?> driverOptions = null;
		
		if(className.contains("CHROMEDRIVER")) {
			driverOptions = new ChromeOptions();
		}
		
		if(className.contains("FIREFOXDRIVER")) {
			driverOptions = new FirefoxOptions();
		}
		
		if(className.contains("EDGEDRIVER")) {
			driverOptions = new EdgeOptions();
		}
		
		if(className.contains("SAFARIDRIVER")) {
			driverOptions = new SafariOptions();
		}
		
		if(null != driverOptions) {
			
			Map<String, Object> capMap = capabilities.asMap();
			
			Set<Entry<String, Object>> capEntries = capMap.entrySet();
			
			for(Entry<String, Object> entry : capEntries) {
				driverOptions.setCapability(entry.getKey(), entry.getValue());
			}
			return driverOptions;
		}
		
		throw new UnsupportedBrowserException(className);
	}

}
