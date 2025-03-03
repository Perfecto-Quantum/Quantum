package com.quantum.utils;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

public class FrameworkEntryClass {
	
	private static final Log logger = LogFactoryImpl.getLog(FrameworkEntryClass.class);

	public static void main(String[] suiteFiles) {

		if (suiteFiles.length == 0) {
			System.out.println("No TestNG File provided!!");
			return;
		}

		try {
			
			logger.info("Test Suite execution started");

			// Create Temp folder to copy TestNG files.
			JarTestNGUtils.createTempFolder();

			// Copy TestNG file(s) outside of Jar.
			List<String> testSuites = JarTestNGUtils.moveTestNGFiles(suiteFiles);

			// Execute copied TestNG file(s).
			TestNGExecutor.executeTestNGSuiteFiles(testSuites);
			
			logger.info("Test Suite execution completed");

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			System.exit(1);
		}

		System.exit(0);
	}

}
