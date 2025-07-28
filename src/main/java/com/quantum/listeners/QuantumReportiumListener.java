package com.quantum.listeners;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.openqa.selenium.WebDriver;
import org.testng.IInvokedMethod;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.xml.XmlTest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.perfecto.reportium.WebDriverProvider;
import com.perfecto.reportium.client.ReportiumClient;
import com.perfecto.reportium.client.ReportiumClientFactory;
import com.perfecto.reportium.model.CustomField;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.PerfectoExecutionContext;
import com.perfecto.reportium.model.PerfectoExecutionContext.PerfectoExecutionContextBuilder;
import com.perfecto.reportium.model.Project;
import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.TestContext.Builder;
import com.perfecto.reportium.test.result.TestResult;
import com.perfecto.reportium.test.result.TestResultFactory;
import com.perfecto.reportium.testng.ReportiumTestNgListener;
import com.qmetry.qaf.automation.core.CheckpointResultBean;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.MessageTypes;
import com.qmetry.qaf.automation.core.TestBaseProvider;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.step.QAFTestStepListener;
import com.qmetry.qaf.automation.step.StepExecutionTracker;
import com.qmetry.qaf.automation.step.TestStep;
import com.qmetry.qaf.automation.step.client.Scenario;
import com.qmetry.qaf.automation.step.client.text.BDDDefinitionHelper.ParamType;
import com.qmetry.qaf.automation.ui.WebDriverTestCase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.quantum.utils.ConfigurationUtils;
import com.quantum.utils.ConsoleUtils;
import com.quantum.utils.DeviceUtils;
import com.quantum.utils.DriverUtils;
import com.quantum.utils.ReportUtils;

import io.cucumber.core.backend.ObjectFactory;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.options.RuntimeOptionsBuilder;

class Messages {
	List<String> StackTraceErrors;
	List<String> CustomFields;
	List<String> Tags;
	String CustomError;
	String JsonFile;

	public String getJsonFile() {
		return this.JsonFile;
	}

	public void setJsonFile(String jsonFile) {
		this.JsonFile = jsonFile;
	}

	public String getCustomError() {
		return this.CustomError;
	}

	public void setCustomError(String customError) {
		this.CustomError = customError;
	}

	public List<String> getStackTraceErrors() {
		return this.StackTraceErrors;
	}

	public void setStackTraceErrors(List<String> error) {
		this.StackTraceErrors = error;
	}

	public List<String> getCustomFields() {
		return this.CustomFields;
	}

	public void setCustomFields(List<String> customFields) {
		this.CustomFields = customFields;
	}

	public List<String> getTags() {
		return this.Tags;
	}

	public void setTags(List<String> tags) {
		this.Tags = tags;
	}
}

public class QuantumReportiumListener extends ReportiumTestNgListener
		implements QAFTestStepListener, ITestListener, ISuiteListener {

//	private static final Log logger = LogFactoryImpl.getLog(QuantumReportiumListener.class);

	public static final String PERFECTO_REPORT_CLIENT = "perfecto.report.client";
	
	private final Log logger = LogFactoryImpl.getLog(QuantumReportiumListener.class);

	public static ReportiumClient getReportClient() {
		return (ReportiumClient) getBundle().getObject(PERFECTO_REPORT_CLIENT);
	}

	@Override
	public void onStart(ISuite suite) {

		String quantumVersion = ConfigurationUtils.getQuantumVersion();

		if (null != quantumVersion) {
			ConsoleUtils.surroundWithSquare("Quantum Version : " + quantumVersion);
		}

		FailedTestSuite.resetResultFolder();
	}

	public void onFinish(ISuite suite) {
		// Failed test retry
		FailedTestSuite.saveXml();
	}

	public Messages parseFailureJsonFile(String actualMessage) {
//		String jsonStr = null;
		String failureConfigLoc = ConfigurationManager.getBundle().getString("failureReasonConfig",
				"src/main/resources/failureReasons.json");

		File failureConfigFile = new File(failureConfigLoc);

		if (!failureConfigFile.exists()) {
			logger.debug ("Ignoring Failure Reasons because JSON file was not found in path: " + failureConfigLoc);
			return null;
		}

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setLenient();
		Gson gson = gsonBuilder.create();
		JsonReader reader = null;

		try {
			reader = new JsonReader(new FileReader(failureConfigLoc));
		} catch (FileNotFoundException e) {
			logger.error("Problem parsing Failure Reason JSON file: " + failureConfigLoc);
			logger.error(e);
		}
		Messages[] response = gson.fromJson(reader, Messages[].class);

		for (Messages messages : response) {
			if (messages.getStackTraceErrors() == null) {
				logger.error(
						"Failure Reason JSON file has wrong formmat, please read here https://developers.perfectomobile.com/pages/viewpage.action?pageId=31103917: "
								+ failureConfigLoc);
				return null;

			}

			for (String error : ListUtils.emptyIfNull(messages.getStackTraceErrors())) {
				if (actualMessage.contains(error)) {
					messages.setJsonFile(failureConfigLoc);
					return messages;
				}
			}
			// if (messages.getStackTraceErrors().toString().contains(actualMessage)) {
			// messages.setJsonFile(failureConfigLoc);
			// return messages;
			// }

		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onStart(ITestContext context) {

		if (isExecutingOnPerfecto()) {

			String suiteTestName = context.getCurrentXmlTest().getName();

			getBundle().setProperty("Suite test name", suiteTestName);

			List<String> stepListeners = getBundle().getList(ApplicationProperties.TESTSTEP_LISTENERS.key);
			if (!stepListeners.contains(this.getClass().getName())) {
				stepListeners.add(this.getClass().getName());
				getBundle().setProperty(ApplicationProperties.TESTSTEP_LISTENERS.key, stepListeners);
			}

			if (getBundle().getBoolean("perfecto.default.driver.listener", true)) {
				List<String> driverListeners = getBundle()
						.getList(ApplicationProperties.WEBDRIVER_COMMAND_LISTENERS.key);
				if (!driverListeners.contains(PerfectoDriverListener.class.getName())) {
					driverListeners.add(PerfectoDriverListener.class.getName());
					getBundle().setProperty(ApplicationProperties.WEBDRIVER_COMMAND_LISTENERS.key, driverListeners);
				}
			}
		}
	}

	private boolean isExecutingOnPerfecto() {
		return getBundle().getString("remote.server", "").contains("perfecto");
	}

	@Override
	public void onTestStart(ITestResult testResult) {

		String testName = testResult.getTestName();

		testName = testName + getDataDrivenText(testResult);

		if (isExecutingOnPerfecto()) {

			getBundle().setProperty("ScenarioExecution", testResult.getMethod().getMethodName());

			// compile actual groups
			String[] groups = TestNode.getScenarioGroups(testResult);

			ArrayList<String> groupsFinal = new ArrayList<String>();

			ArrayList<CustomField> cfc = new ArrayList<CustomField>();

			String uniqueIdentifierPrefix = getBundle().getString(FailedTestSuite.UNIQUE_TEST_PREFIX_KEY, "");

			for (String group : groups) {

				// Find Unique Identifier from the Scenario
				if (group.startsWith(uniqueIdentifierPrefix)) {
					getBundle().addProperty(FailedTestSuite.UNIQUE_TEST_IDENTIFIER, group);
				}

				if (group.startsWith(getBundle().getString("custom.field.identifier", "%"))) {
					try {
						cfc.add(new CustomField(
								group.split(getBundle().getString("custom.field.delimiter", "-"))[0].substring(1),
								group.split(getBundle().getString("custom.field.delimiter", "-"))[1]));
					} catch (Exception ex) {
						throw new NullPointerException(
								"Custom field key/value pair not delimited properly.  Example of proper default usage: %Developer-Jeremy.  Check application properties custom.field.delimiter and custom.field.identifier for custom values that may have been set.");
					}
				} else {
					groupsFinal.add(group);
				}
			}
			try {

				cfc.add(new CustomField("quantumFrameworkVersion", "3.0.0"));

			} catch (Exception e) {
				logger.error("On Test Start : " + e);
			}

			// Get custom fields "%name-value" from groups
			if (ConfigurationManager.getBundle().getString("custom.field") != null) {
				String customFieldValue = ConfigurationManager.getBundle().getString("custom.field");
				String[] customFieldPairs = customFieldValue.split(",");
				for (String customFieldPair : customFieldPairs) {
					try {
						cfc.add(new CustomField(
								customFieldPair.split(getBundle().getString("custom.field.delimiter", "-"))[0],
								customFieldPair.split(getBundle().getString("custom.field.delimiter", "-"))[1]));
					} catch (Exception ex) {
						new NullPointerException(
								"Custom field key/value pair not delimited properly.  Example of proper default usage: %Developer-Jeremy.  Check application properties custom.field.delimiter and custom.field.identifier for custom values that may have been set.")
								.printStackTrace();
					}
				}
			}

			Builder<?> testContext = new TestContext.Builder<>();

			if (groupsFinal.size() > 0) {
				testContext
						.withTestExecutionTags(groupsFinal.toString().replace('[', ' ').replace(']', ' ').split(","));
			}

			if (cfc.size() > 0) {
				testContext.withCustomFields(cfc);
			}

			createReportiumClient(testResult).testStart(testName, testContext.build());

			if (testResult.getParameters().length > 0 && getBundle().getBoolean("addFullDataToReport", false)) {
				logStepStart("Test Data used");
				ReportUtils.reportComment(testResult.getParameters()[0].toString());
				logStepEnd();
			}
			if (isExecutingOnPerfecto()) {
				if (ConfigurationManager.getBundle().getString("perfecto.harfile.enable", "false").equals("true")) {
					String platformName = DriverUtils.getDriver().getCapabilities().getCapability("platformName")
							.toString();
					if (platformName != null && platformName.equalsIgnoreCase("android")
							|| platformName.equalsIgnoreCase("ios") || platformName.equalsIgnoreCase("any")
							|| platformName.equalsIgnoreCase("linux")) {
						ReportUtils.logStepStart("Start generate Har file");
						DeviceUtils.generateHAR();
					}
					if (platformName != null && platformName.equalsIgnoreCase("mac")) {
						Object deviceName = DriverUtils.getDriver().getCapabilities().getCapability("deviceName");
						if (deviceName != null) {
							if (deviceName.toString().toLowerCase().contains("iphone")
									|| deviceName.toString().toLowerCase().contains("ipad")) {
								ReportUtils.logStepStart("Start generate Har file");
								DeviceUtils.generateHAR();
							}
						}
					}
				}
			}
		}

		Map<Object, Object> dataPasser = new HashMap<Object, Object>();
		ConfigurationManager.getBundle().addProperty("dataPasser" + Thread.currentThread(), dataPasser);
	}

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		if (method.isTestMethod()) {
			// Before execution of test method
			ConsoleUtils.surroundWithSquare("TEST STARTED: " + getTestName(testResult)
					+ (testResult.getParameters().length > 0 ? " [" + testResult.getParameters()[0] + "]" : ""));

		}
	}

	@Override
	public void beforExecute(StepExecutionTracker stepExecutionTracker) {
		String stepDescription = getProcessStepDescription(stepExecutionTracker.getStep());
		String msg = "BEGIN STEP: " + stepDescription;
		ConsoleUtils.logInfoBlocks(msg, ConsoleUtils.lower_block + " ", 10);
		logStepStart(stepDescription);
	}

	@Override
	public void afterExecute(StepExecutionTracker stepExecutionTracker) {
		logStepEnd();
		String msg = "END STEP: " + stepExecutionTracker.getStep().getDescription();
		ConsoleUtils.logInfoBlocks(msg, ConsoleUtils.upper_block + " ", 10);
	}

	@Override
	public void onFailure(StepExecutionTracker stepExecutionTracker) {

	}

	@Override
	public void onTestSuccess(ITestResult testResult) {

		getBundle().setProperty("ScenarioExecution", "FromListener");
		getBundle().setProperty("device_not_available", false);

		ReportiumClient client = getReportClient();

		if (ConfigurationManager.getBundle().getPropertyValue("perfecto.harfile.enable").equals("true")) {
			Object platformNameObj = DriverUtils.getDriver().getCapabilities().getCapability("platformName");

			String platformName = null == platformNameObj ? "" : ((String) platformNameObj).toLowerCase();

			switch (platformName) {
			case "":
				break;
			case "mac": {
				Object deviceNameObj = DriverUtils.getDriver().getCapabilities().getCapability("deviceName");
				if (deviceNameObj != null) {

					String deviceName = deviceNameObj.toString().toLowerCase();
					if (deviceName.contains("iphone") || deviceName.contains("ipad")) {
						DeviceUtils.stopGenerateHAR();
					}
				}
			}
				break;
			default:
				DeviceUtils.stopGenerateHAR();

			}

//			if (platformName != null && platformName.equalsIgnoreCase("android") || platformName.equalsIgnoreCase("ios")
//					|| platformName.equalsIgnoreCase("any") || platformName.equalsIgnoreCase("linux"))
//				DeviceUtils.stopGenerateHAR();
//
//			if (platformName != null && platformName.equalsIgnoreCase("mac")) {
//				Object deviceNameObj = DriverUtils.getDriver().getCapabilities().getCapability("deviceName");
//				if (deviceNameObj != null) {
//
//					String deviceName = deviceNameObj.toString().toLowerCase();
//					if (deviceName.contains("iphone") || deviceName.contains("ipad")) {
//						DeviceUtils.stopGenerateHAR();
//					}
//				}
//			}
		}

		if (null != client) {
			client.testStop(TestResultFactory.createSuccess());
			logTestEnd(testResult);
		}

		tearIt(testResult);
	}

	@Override
	public void onTestFailure(ITestResult testResult) {

		getBundle().setProperty("ScenarioExecution", "FromListener");
		getBundle().setProperty("device_not_available", false);

		// Retry failed tests
		try {
			XmlTest currentTest = testResult.getMethod().getXmlTest();
			FailedTestSuite.addTest(currentTest);
		} catch (Exception e) {
			logger.error("On Test Failure : " + e.getLocalizedMessage());
		} finally {
			ConfigurationManager.getBundle().clearProperty(FailedTestSuite.UNIQUE_TEST_IDENTIFIER);
		}

		ReportiumClient client = getReportClient();

		TestResult reportiumResult;

		if (ConfigurationManager.getBundle().getPropertyValue("perfecto.harfile.enable").equals("true")) {

			String platformName = DriverUtils.getDriver().getCapabilities().getCapability("platformName").toString();
			if (platformName != null && platformName.equalsIgnoreCase("android") || platformName.equalsIgnoreCase("ios")
					|| platformName.equalsIgnoreCase("any") || platformName.equalsIgnoreCase("linux"))
				DeviceUtils.stopGenerateHAR();
			if (platformName != null && platformName.equalsIgnoreCase("mac")) {
				Object deviceName = DriverUtils.getDriver().getCapabilities().getCapability("deviceName");
				if (deviceName != null) {
					if (deviceName.toString().toLowerCase().contains("iphone")
							|| deviceName.toString().toLowerCase().contains("ipad")) {
						ReportUtils.logStepStart("Start generate Har file");
						DeviceUtils.stopGenerateHAR();
					}
				}
			}
		}
		if (null != client) {

			String failMsg = "";
			List<CheckpointResultBean> checkpointsList = TestBaseProvider.instance().get().getCheckPointResults();
			for (CheckpointResultBean result : checkpointsList) {
				if (result.getType().equals(MessageTypes.TestStepFail.toString())) {
					failMsg += "Step:" + result.getMessage() + " failed" + "\n";
					// List<CheckpointResultBean> subList = result.getSubCheckPoints();
					// for (CheckpointResultBean sub : subList) {
					// if (sub.getType().equals(MessageTypes.Fail.toString())){
					// failMsg += sub.getMessage() + "\n";
					// }
					// }
				}
			}

			if (testResult.getThrowable() == null) {

				Exception exp = new Exception(
						"There was some validation failure in the scenario which did not provide any throwable object.");

				try {

					client.testStop(
							TestResultFactory.createFailure(failMsg.isEmpty() ? "An error occurred" : failMsg, exp));
				} catch (Exception e) {
					ConsoleUtils.logWarningBlocks(e.getMessage());
				}

			} else {
				String exceptionThown = ExceptionUtils.getStackTrace(testResult.getThrowable());

				String actualExceptionMessage = testResult.getThrowable().toString();
				Messages message = parseFailureJsonFile(actualExceptionMessage);

				if (message != null) {
					String customError = message.getCustomError();
					List<String> customFields = ListUtils.emptyIfNull(message.getCustomFields());
					List<String> tags = ListUtils.emptyIfNull(message.getTags());
					String fileLoc = message.getJsonFile();

					ArrayList<CustomField> cfc = new ArrayList<CustomField>();

					for (String customField : customFields) {
						try {
							cfc.add(new CustomField(
									customField.split(getBundle().getString("custom.field.delimiter", "-"))[0],
									customField.split(getBundle().getString("custom.field.delimiter", "-"))[1]));
						} catch (Exception ex) {
							throw new NullPointerException(
									"Custom field key/value pair not delimited properly in failure reason json file: "
											+ fileLoc
											+ ".  Example of proper default usage: Developer-Jeremy.  Check application properties custom.field.delimiter for custom values that may have been set.");
						}
					}

					ArrayList<String> tagsFinal = new ArrayList<String>();
					for (String tag : tags) {
						tagsFinal.add(tag);
					}

					Builder<?> testContext = new TestContext.Builder<>();

					if (cfc.size() > 0) {
						testContext.withCustomFields(cfc);
					}

					if (tagsFinal.size() > 0) {
						testContext.withTestExecutionTags(tagsFinal);
					}

					reportiumResult = TestResultFactory.createFailure(failMsg.isEmpty() ? "An error occurred" : failMsg,
							testResult.getThrowable(), customError);

					try {
						client.testStop(reportiumResult, testContext.build());
					} catch (Exception e) {
						ConsoleUtils.logWarningBlocks(e.getMessage());
					}

				} else {

					reportiumResult = TestResultFactory.createFailure(failMsg.isEmpty() ? "An error occurred" : failMsg,
							testResult.getThrowable());

					try {
						client.testStop(reportiumResult);
					} catch (Exception e) {
						ConsoleUtils.logWarningBlocks(e.getMessage());
					}
				}

				ConsoleUtils.logWarningBlocks(exceptionThown);
			}

			handleWebDriverFailure(testResult);

			logTestEnd(testResult);

			tearIt(testResult);

		}
	}

	private void handleWebDriverFailure(ITestResult testResult) {
		try {
			QAFExtendedWebDriver driver = DeviceUtils.getQAFDriver();
			if (driver != null) {

				driver.getUnderLayingDriver().getPageSource();
//				driver.getUnderLayingDriver().getWindowHandle();
			}
		} catch (Exception e) {
			Object testInstance = testResult.getInstance();
			((WebDriverTestCase) testInstance).getTestBase().tearDown();

		}
	}

	private void tearIt(ITestResult testResult) {
		
		XmlTest xmlTest = testResult.getTestContext().getCurrentXmlTest();
		
		boolean doResetDriver = resetDriver(testResult);
		
		String className = xmlTest.getXmlClasses().get(0).getName();
		
		String isGlobalDatadrivenParallel = ConfigurationManager.getBundle().getString("global.datadriven.parallel", "false");
		
		if ((xmlTest.getParallel().toString().equalsIgnoreCase("methods")
				& testResult.getTestClass().getName().toLowerCase().contains("scenario"))
				|| isGlobalDatadrivenParallel.equalsIgnoreCase("true")
				|| className.contains("com.qmetry.qaf.automation.step.client.excel.ExcelTestFactory")
				|| className.contains("com.qmetry.qaf.automation.step.client.csv.KwdTestFactory")
				|| doResetDriver

		) {
			Object testInstance = testResult.getInstance();
			((WebDriverTestCase) testInstance).getTestBase().tearDown();
		}
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		getBundle().setProperty("ScenarioExecution", "FromListener");
		getBundle().setProperty("device_not_available", false);
		ReportiumClient client = getReportClient();

		// Retry failed tests
		try {
			XmlTest currentTest = result.getMethod().getXmlTest();
			FailedTestSuite.addTest(currentTest);
		} catch (Exception e) {
			logger.error("On Test Skipped : " + e.getLocalizedMessage());
		} finally {
			ConfigurationManager.getBundle().clearProperty(FailedTestSuite.UNIQUE_TEST_IDENTIFIER);
		}

		if (null != client) {
			// By default all the skipped tests will be failed, if you want
			if (ConfigurationManager.getBundle().getString("skippedTests", "fail").equalsIgnoreCase("pass")) {
				client.testStop(TestResultFactory.createSuccess());
			} else {
				String failureMessage = result.getThrowable().getMessage();
				failureMessage = (failureMessage.isEmpty() || failureMessage == null) ? "This test was skipped"
						: result.getThrowable().getMessage();
				client.testStop(TestResultFactory.createFailure(failureMessage, result.getThrowable()));
			}
			logTestEnd(result);
		}

	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {

	}

	@Override
	public void onFinish(ITestContext context) {

//		((FileHandler)ConfigurationManager.getBundle().getProperty("seleniumfile")).flush();
//		((FileHandler)ConfigurationManager.getBundle().getProperty("seleniumfile")).close();

//		((FileHandler)ConfigurationManager.getBundle().getProperty("remotewdfile")).flush();
//		((FileHandler)ConfigurationManager.getBundle().getProperty("remotewdfile")).close();

	}

	public static void logTestStep(String message) {
		try {
			getReportClient().stepStart(message);
		} catch (Exception e) {
			// ignore...
		}
	}

	public static void logStepStart(String message) {
		try {
			getReportClient().stepStart(message);
		} catch (Exception e) {
			// ignore...
		}
	}

	public static void logStepEnd() {
		try {
			getReportClient().stepEnd();
		} catch (Exception e) {
			// ignore...
		}
	}

	public static void logAssert(String message, boolean status) {
		try {
			getReportClient().reportiumAssert(message, status);
		} catch (Exception e) {
			// ignore...
		}
	}

	private void logTestEnd(ITestResult testResult) {
		String endText = "TEST " + (testResult.isSuccess() ? "PASSED" : "FAILED") + ": ";
		addReportLink(testResult, getReportClient().getReportUrl());

		ConsoleUtils.logWarningBlocks(
				"REPORTIUM URL: " + getReportClient().getReportUrl().replace("[", "%5B").replace("]", "%5D"));

		ConsoleUtils.surroundWithSquare(endText + getTestName(testResult)
				+ (testResult.getParameters().length > 0 ? " [" + testResult.getParameters()[0] + "]" : ""));

	}

//	@Override
	protected String getTestName(ITestResult result) {

		return result.getTestName() == null ? result.getMethod().getMethodName() : result.getTestName();
	}

	/**
	 * Creates client and set into configuration for later use during test execution
	 * using {@link #getReportiumClient()}.
	 *
	 * param testResult
	 * 
	 * @return newly created {@link ReportiumClient} object
	 */
	@Override
	protected ReportiumClient createReportiumClient(ITestResult testResult) {
		ReportiumClient reportiumClient = new ReportiumClientFactory().createLoggerClient();

		String suiteName = testResult.getTestContext().getSuite().getName();
		String prjName = getBundle().getString("project.name", suiteName);
		String prjVer = getBundle().getString("project.ver", "1.0");
		String xmlTestName = testResult.getTestContext().getName();

		String reportiumTags = System.getProperty("reportium-tags");

		StringBuilder tagsStringBuilder = new StringBuilder(xmlTestName);
		tagsStringBuilder = tagsStringBuilder.append(",").append(suiteName);

		tagsStringBuilder = (reportiumTags == null) ? tagsStringBuilder
				: tagsStringBuilder.append(",").append(reportiumTags);

		String allTags = tagsStringBuilder.toString();

		Object testInstance = testResult.getInstance();

		HashMap<String, WebDriver> driverList = new HashMap<String, WebDriver>();
		String driverNameList = ConfigurationManager.getBundle().getString("driverNameList", "");

		if (driverNameList.isEmpty()) {
			WebDriver driver = null;
			if (testInstance instanceof WebDriverTestCase)
				driver = ((WebDriverTestCase) testInstance).getDriver();
			else if (testInstance instanceof WebDriverProvider)
				driver = ((WebDriverProvider) testInstance).getWebDriver();
			driverList.put("Default Driver", driver);
		} else {
			for (String driverName : driverNameList.split(",")) {
				logger.info("Adding driver with name - " + driverName);
//				ConfigurationManager.getBundle().setProperty("driver_name", driverName);
				DriverUtils.switchToDriver(driverName);
				driverList.put(driverName, DeviceUtils.getQAFDriver());
				logger.info("Added driver with name - " + driverName);
			}
		}

		PerfectoExecutionContextBuilder perfectoExecutionContextBuilder = new PerfectoExecutionContext.PerfectoExecutionContextBuilder()
				.withProject(new Project(prjName, prjVer)).withContextTags(allTags.split(","))
				.withJob(new Job(getBundle().getString("JOB_NAME", System.getProperty("reportium-job-name")),
						getBundle().getInt("BUILD_NUMBER",
								System.getProperty("reportium-job-number") == null ? 0
										: Integer.parseInt(System.getProperty("reportium-job-number"))))
						.withBranch(System.getProperty("reportium-job-branch")));
		for (String driverName : driverList.keySet()) {

			perfectoExecutionContextBuilder.withWebDriver(driverList.get(driverName), driverName);
		}

		reportiumClient = new ReportiumClientFactory()
				.createPerfectoReportiumClient(perfectoExecutionContextBuilder.build());

		getBundle().setProperty(PERFECTO_REPORT_CLIENT, reportiumClient);

		return reportiumClient;
	}

	@Override
	protected String[] getTags(ITestResult testResult) {

		RuntimeOptions cucumberOptions = getCucumberOptions(testResult);

		List<URI> featurePathsURI = cucumberOptions.getFeaturePaths();

		List<String> featurePaths = featurePathsURI.stream().map(URI::getPath).collect(Collectors.toList());

		List<URI> gluePathsURI = cucumberOptions.getGlue();

		List<String> gluePaths = gluePathsURI.stream().map(URI::getPath).collect(Collectors.toList());

		List<Pattern> filterPatterns = cucumberOptions.getNameFilters();

//		List<String> patterns =filterPatterns
//				.stream().map(Object::toString)
//				.collect(Collectors.toList());

		List<String> optionsList = filterPatterns.stream().map(Object::toString).collect(Collectors.toList());

		optionsList.addAll(featurePaths);
		optionsList.addAll(gluePaths);

		return ArrayUtils.addAll(super.getTags(testResult), optionsList.toArray(new String[optionsList.size()]));
	}

	@SuppressWarnings("unchecked")
	private RuntimeOptions getCucumberOptions(ITestResult testResult) {
		try {

			String className = testResult.getTestClass().getName();

			Class<? extends ObjectFactory> testResultClass = (Class<? extends ObjectFactory>) Class.forName(className);

			return new RuntimeOptionsBuilder().setObjectFactoryClass(testResultClass).build();

//			return new RuntimeOptionsFactory(Class.forName(testResult.getTestClass().getName())).create();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void addReportLink(ITestResult result, String url) {

		Object testNGMethodObj = result.getMethod().getInstance();

		if (testNGMethodObj instanceof Scenario) {
			Scenario scenario = (Scenario) testNGMethodObj;
			Map<String, Object> metaData = scenario.getMetadata();

			metaData.put("Perfecto-report", "<a href=\"" + url + "\" target=\"_blank\">view</a>");

		}

//		((TestNGScenario) testNGMethod).getMetaData().put("Perfecto-report",
//				"<a href=\"" + url + "\" target=\"_blank\">view</a>");
	}

	@SuppressWarnings("rawtypes")
	private String getDataDrivenText(ITestResult testResult) {

		String result = "";
		if (testResult.getParameters().length > 0) {

			Map map = (Map) testResult.getParameters()[0];
			if (map.containsKey("recDescription")) {
				result = " [" + map.get("recDescription") + "]";
			} else if (map.containsKey("recId")) {
				result = " [" + map.get("recId") + "]";
			}
		}
		return result;
	}

	public static List<String> getArgNames(String def) {
		// Pattern p = Pattern.compile("[$][{](.*?)}");
		// Pattern p = Pattern.compile("\"(.*?)[$][{](.*?)}\"");
		// String allChars = "[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]";
		Pattern p = Pattern.compile(
				"\\\"([a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\\\\|,.<>\\/? ]*)[${](([a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\\\\|,.<>\\/? ]*))}([a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\\\\|,.<>\\/? ]*)\\\"");
		Matcher matcher = p.matcher(def);
		List<String> args = new ArrayList<String>();
		while (matcher.find()) {
			String paramName = matcher.group();
			String finalParamNam = paramName.substring(1, paramName.length() - 2);
			args.add(finalParamNam.replace("${", "{"));
		}
		return args;
	}

	@SuppressWarnings("unchecked")
	private String getProcessStepDescription(TestStep step) {
		// process parameters in step;

		String description = step.getDescription();

		// if (step instanceof CustomStep) {

		Object[] actualArgs = step.getActualArgs();
		String def = step.getDescription();

		if ((actualArgs != null) && (actualArgs.length > 0)) {
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.putAll(step.getStepExecutionTracker().getContext());
			List<String> paramNames = getArgNames(def);

			logger.debug("Get Process Step Description : " + paramNames);

			if ((paramNames != null) && (!paramNames.isEmpty())) {

				for (int i = 0; i < paramNames.size(); i++) {
					String paramName = paramNames.get(i).trim();
					// remove starting { and ending } from parameter name
					paramName = paramName.substring(1, paramName.length() - 1).split(":", 2)[0];

					// in case of data driven test args[0] should not be overriden
					// with steps args[0]
					if ((actualArgs[i] instanceof String)) {

						String pstr = (String) actualArgs[i];

						if (pstr.startsWith("${") && pstr.endsWith("}")) {
							String pname = pstr.substring(2, pstr.length() - 1);
							actualArgs[i] = paramMap.containsKey(pstr) ? paramMap.get(pstr)
									: paramMap.containsKey(pname) ? paramMap.get(pname)
											: getBundle().containsKey(pstr) ? getBundle().getObject(pstr)
													: getBundle().getObject(pname);
						} else if (pstr.indexOf("$") >= 0) {
							pstr = getBundle().getSubstitutor().replace(pstr);
							actualArgs[i] = StrSubstitutor.replace(pstr, paramMap);
						}
						// continue;
						ParamType ptype = ParamType.getType(pstr);
						if (ptype.equals(ParamType.MAP)) {
							Map<String, Object> kv = new Gson().fromJson(pstr, Map.class);
							paramMap.put(paramName, kv);
							for (String key : kv.keySet()) {
								paramMap.put(paramName + "." + key, kv.get(key));
							}
						} else if (ptype.equals(ParamType.LIST)) {
							List<Object> lst = new Gson().fromJson(pstr, List.class);
							paramMap.put(paramName, lst);
							for (int li = 0; li < lst.size(); li++) {
								paramMap.put(paramName + "[" + li + "]", lst.get(li));
							}
						}
					}

					paramMap.put("${args[" + i + "]}", actualArgs[i]);
					paramMap.put("args[" + i + "]", actualArgs[i]);
					paramMap.put(paramName, actualArgs[i]);

				}

				description = StrSubstitutor.replace(description, paramMap);

			}
		}
		return description;
	}

	private boolean resetDriver(ITestResult result) {
		
		String driverResetTimerFlag = ConfigurationManager.getBundle().getString("perfecto.driver.restart.timer.flag",
				"false");
		
		int driverResetTimerValue = ConfigurationManager.getBundle().getInt("perfecto.driver.restart.timer.value",
				3600);

		boolean driverResetTag = false;
		try {
			Scenario sc = (Scenario) result.getInstance();
			String[] m_groups = sc.getM_groups();
			for (String tag : m_groups) {
				if (tag.equalsIgnoreCase("@RestartDriverAfterTimeout")) {
					logger.info("Driver Reset tag found!");
					driverResetTag = true;
				}
			}
		} catch (Exception e) {
			logger.warn("Gherkin scenarios were not found so skipping the reset driver tag check.");
		}

		if (driverResetTimerFlag.equalsIgnoreCase("true") || driverResetTag) {
			long currentTimeInMsec = System.currentTimeMillis();
			long driverStartTimeInMsec = ConfigurationManager.getBundle().getLong(PerfectoDriverListener.DRIVER_START_TIMER);
			
			long timeElapsedInMsec = currentTimeInMsec - driverStartTimeInMsec;
			
			long timeElapsedInSec = (long) Math.ceil(timeElapsedInMsec);

//			Check the timer and the tag for restart @RestartDriverAfterTimeout
			if ( (timeElapsedInSec > driverResetTimerValue) || driverResetTag) {
				logger.info("Closing the driver and restarting the driver");
				return true;
			}
			return false;
		}
		return false;
	}
}
