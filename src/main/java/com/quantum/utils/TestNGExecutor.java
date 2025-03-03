package com.quantum.utils;

import java.util.List;

import org.testng.TestNG;


public class TestNGExecutor {
	
	public static void executeTestNGSuiteFiles(List<String> testSuites) {
		
		TestNG testNG = new TestNG();
		testNG.setTestSuites(testSuites);
		testNG.run();	
	}
}
