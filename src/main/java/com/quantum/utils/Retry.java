package com.quantum.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.quantum.utils.DeviceUtils;

public class Retry implements IRetryAnalyzer  {

	int counter = 0;
	int retryLimit = ConfigurationManager.getBundle().getInt("retry.count");

	
	@Override
	public boolean retry(ITestResult result) {
		
		if (counter < retryLimit) {
			counter++;
			
			return true;
		}

		return false;

	}
}