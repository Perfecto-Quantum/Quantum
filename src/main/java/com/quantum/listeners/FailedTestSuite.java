package com.quantum.listeners;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.testng.xml.XmlGroups;
import org.testng.xml.XmlRun;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.step.QAFTestStepListener;

public class FailedTestSuite {
	
	public static String UNIQUE_TEST_PREFIX_KEY = "scenario.unique.identifier.prefix";
	public static String UNIQUE_TEST_IDENTIFIER = "test.unique.identifier";
	
	private static final Log logger = LogFactoryImpl.getLog(FailedTestSuite.class);
	
	private  ConcurrentHashMap<TestNode,TestNode> failedTests;
	
	private volatile static FailedTestSuite failedTestSuite;
	
	private XmlSuite failedXmlSuite = null;
	
	private FailedTestSuite(XmlTest currentTest) {
		failedTests = new ConcurrentHashMap<TestNode,TestNode>();
	}
	
	private void cloneSuite(XmlTest currentTest) {
		
		XmlSuite currentSuite = currentTest.getSuite();
		String failedSuiteName = currentSuite.getName() + " - Failed";
		
		failedXmlSuite = (XmlSuite) currentSuite.clone();
		failedXmlSuite.setName(failedSuiteName);
		failedXmlSuite.setTests(new ArrayList<XmlTest>());
	}
	
	private String getUniqueIdentifier() {
		return ConfigurationManager
				.getBundle()
				.getString(UNIQUE_TEST_IDENTIFIER, "");
	}
	
	public static int incrementFailedTest() {
		int failedTestCounter = ConfigurationManager.getBundle().getInt("scenario.failed.test.counter",1);
		ConfigurationManager.getBundle().setProperty("scenario.failed.test.counter", failedTestCounter+1);
		return failedTestCounter;
	}
	
	private void addAllFailedTests() {
		Set<Entry<TestNode, TestNode>> tests = failedTests.entrySet();
		
		for(Entry<TestNode, TestNode> test: tests) {
			failedXmlSuite.addTest(test.getValue().getCurrentTest());
		}
	}
	
	private void addFailedTest(XmlTest currentTest) {
		
		String uniqueIdentifier = getUniqueIdentifier();
		
		if (!uniqueIdentifier.isBlank()) {
			
			logger.debug("Adding Failed test with uinque identifier - " + uniqueIdentifier);
			
			XmlRun xmlRun = new XmlRun();;
			XmlGroups uniqueGroup =new XmlGroups();
			xmlRun.onInclude(uniqueIdentifier);
			uniqueGroup.setRun(xmlRun);
			
//			if(currentTest.getIncludedGroups().size()==1) {
//				if(currentTest.getIncludedGroups().contains(uniqueIdentifier)) {
//					uniqueGroup = currentTest.getXmlGroups();
//				}else {
//					
//				}
//			}else {
//				
//				if(currentTest.getIncludedGroups().contains(uniqueIdentifier)) {
//					if(currentTest.getIncludedGroups().contains(uniqueIdentifier)) return;
//				}else {
//					xmlRun = new XmlRun();
//					xmlRun.onInclude(uniqueIdentifier);
//					uniqueGroup = currentTest.getXmlGroups();
//					uniqueGroup.setRun(xmlRun);
//				}
//			}
			
			String failedTestName = currentTest.getName().endsWith("- Failed") ? currentTest.getName():
				TestNode.getTestName(currentTest);
			
			XmlTest currentFailedTest = (XmlTest) currentTest.clone();
			
			currentFailedTest.setThreadCount(currentTest.getThreadCount());
			currentFailedTest.setName(failedTestName);
			
			currentFailedTest.setGroups(uniqueGroup);
			
			TestNode testNode = new TestNode(currentFailedTest);
			
			failedTests.put(testNode,testNode);
			
			logger.debug("Added Failed test with uinque identifier - " + uniqueIdentifier);

		} else {
			logger.debug("No Unique Tag found");
		}
	}

	private static FailedTestSuite getFailedSuite(XmlTest currentTest) {

		if (Objects.isNull(failedTestSuite)) {

			synchronized (QAFTestStepListener.class) {
				if (Objects.isNull(failedTestSuite)) {
					failedTestSuite = new FailedTestSuite(currentTest);
					failedTestSuite.cloneSuite(currentTest);
				}
			}
		}

		return failedTestSuite;
	}
	
	private void addGroupToExistingTest(TestNode currentTestNode) {
		TestNode existingTestNode = this.failedTests.get(currentTestNode);
		String uniqueIdentifier = getUniqueIdentifier();
		
		XmlTest xmlTest = existingTestNode.getCurrentTest();
		
		List<String> includedGroups = xmlTest.getIncludedGroups();
		
		if(!includedGroups.contains(uniqueIdentifier)) {
			existingTestNode.addUniqueGroup(uniqueIdentifier);
		}
	}
	
	public static void addTest(XmlTest currentTest) {
		
		FailedTestSuite failedTestSuite = getFailedSuite(currentTest);
		
		TestNode currentTestNode = new TestNode(currentTest);
		
		synchronized (failedTestSuite) {
			boolean result = failedTestSuite.failedTests.containsKey(currentTestNode);
			
			if(result) {
				failedTestSuite.addGroupToExistingTest(currentTestNode);
			}else {
				failedTestSuite.addFailedTest(currentTest);
			}
		}
		
	}
	
	public static void resetResultFolder() {
		
		String folderPath = getFailedTestFolder();
		
		File quantumFailedSuiteFolder = new File(folderPath);
		
		if(quantumFailedSuiteFolder.exists()) {
			
			File[] quantumFailedSuiteFiles = quantumFailedSuiteFolder.listFiles();
			
			for(File quantumFailedSuiteFile:quantumFailedSuiteFiles) {
				quantumFailedSuiteFile.delete();
			}
			
			quantumFailedSuiteFolder.delete();
		}
		
		quantumFailedSuiteFolder.mkdirs();
	}
	
	public static String getFailedTestFolder() {
		String folderPath = ConfigurationManager.getBundle().getPropertyValueOrNull("failed.testng.file.path");
		
		if(Objects.isNull(folderPath)) {
			folderPath = "test-output/quantum-failed-suite";
		}
		
		return folderPath;
	}
	
	public static void saveXml() {
		
		if(Objects.isNull(failedTestSuite)) {	
			logger.debug("The test suite is not initialized yet");
			return;
		}
		
		if((ConfigurationManager.getBundle().getString("generate.failed.testng.file", "true") == "true") && failedTestSuite.failedTests.size()>0) {
			
			failedTestSuite.addAllFailedTests();
			
			String folderPath = getFailedTestFolder();
			
			String quantumFailedSuiteXmlFileStr = (folderPath.endsWith("/")? 
					folderPath : folderPath + "/") + "failed-suite.xml";
			
			File quantumFailedSuiteXmlFile = new File(quantumFailedSuiteXmlFileStr);
			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	        
			try {
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				Document doc = docBuilder.parse(new InputSource(new StringReader(failedTestSuite.failedXmlSuite.toXml())));
				try(FileOutputStream xmlFileOutput = new FileOutputStream(quantumFailedSuiteXmlFile)){
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
			        Transformer transformer = transformerFactory.newTransformer();
			        DOMSource source = new DOMSource(doc);
			        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://testng.org/testng-1.0.dtd");
			        StreamResult result = new StreamResult(xmlFileOutput);
			        transformer.transform(source, result);
			        
			        logger.info("Quantum Failed TestNG Xml file generated - " + quantumFailedSuiteXmlFileStr);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (TransformerConfigurationException e) {
					e.printStackTrace();
				} catch (TransformerException e) {
					e.printStackTrace();
				}
			} catch (SAXException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (ParserConfigurationException e1) {
				e1.printStackTrace();
			}
		}else {
			logger.info("Quantum Failed TestNG Xml file NOT generated because no failed tests OR 'generate.failed.testng.file' parameter was not set to true ");
		}
	}

}
