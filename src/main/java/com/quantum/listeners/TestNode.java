package com.quantum.listeners;

import org.testng.ITestResult;
import org.testng.xml.XmlGroups;
import org.testng.xml.XmlRun;
import org.testng.xml.XmlTest;

import com.google.common.base.Objects;
import com.qmetry.qaf.automation.step.client.Scenario;

public class TestNode {
	
	private XmlTest xmlTest;
	
	private String testName;
	
	public static String[] getScenarioGroups(ITestResult testResult){
		Object objScenario = testResult.getMethod().getInstance();
		String[] groups = new String[0];
		
		if(objScenario instanceof Scenario) {
			Scenario currentScenario = (Scenario) objScenario;
			groups = currentScenario.getM_groups(); //testResult.getMethod().getGroups();
		}
		
		return groups;
	}
	
	
	public static String getTestName(XmlTest xmlTest) {
//		Map<String,String> parameters = xmlTest.getAllParameters();
//		
//		Set<Entry<String, String>> paramKeySet = parameters.entrySet();
//		
//		StringBuilder nameStrBuilder = new StringBuilder();
//		
//		for(Entry<String, String> keySet : paramKeySet) {
//			nameStrBuilder.append(keySet.getValue());
//			nameStrBuilder.append("-");
//		}
//		
//		nameStrBuilder.append(" Failed");
		
//		return nameStrBuilder.toString();
		
		return xmlTest.getName();
	}
	
	public TestNode(XmlTest xmlTest) {
		this.xmlTest = xmlTest;
		this.testName = getTestName(xmlTest);
	}
	
	public void addUniqueGroup(String uniqueGroup) {
		
		XmlGroups uniqueGroupXML = xmlTest.getXmlGroups();
		
		XmlRun xmlRun = uniqueGroupXML.getRun();
		
		xmlRun.onInclude(uniqueGroup);
		
		uniqueGroupXML.setRun(xmlRun);
		
		xmlTest.setGroups(uniqueGroupXML);
	}
	
	public XmlTest getCurrentTest() {
		return this.xmlTest;
	}
	
	@Override
	public boolean equals(Object testNode) {
		
		String currentTestName = getTestName(((TestNode)testNode).getCurrentTest()) ;
		
		boolean result = testName.equals(currentTestName);
		return result;
	}
	
	@Override
	public int hashCode() {
		
		String currentTestName = getTestName(((TestNode)this).getCurrentTest()) ;
		
		int thisHash = Objects.hashCode(currentTestName);

		return thisHash;
	}

}
