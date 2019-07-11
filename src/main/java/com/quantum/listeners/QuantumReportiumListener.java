package com.quantum.listeners;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.ArrayUtils;
import org.openqa.selenium.WebDriver;
import org.testng.IInvokedMethod;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

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
import com.qmetry.qaf.automation.step.client.TestNGScenario;
import com.qmetry.qaf.automation.step.client.text.BDDDefinitionHelper.ParamType;
import com.qmetry.qaf.automation.ui.WebDriverTestCase;
import com.quantum.utils.ConsoleUtils;
import com.quantum.utils.DeviceUtils;
import com.quantum.utils.DriverUtils;
import com.quantum.utils.ReportUtils;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;

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

public class QuantumReportiumListener extends ReportiumTestNgListener implements QAFTestStepListener, ITestListener {

	public static final String PERFECTO_REPORT_CLIENT = "perfecto.report.client";

	public static ReportiumClient getReportClient() {
		return (ReportiumClient) getBundle().getObject(PERFECTO_REPORT_CLIENT);
	}

	public Messages parseFailureJsonFile(String actualMessage) {
		String jsonStr = null;
		String failureConfigLoc = ConfigurationManager.getBundle().getString("failureReasonConfig", "src/main/resources/failureReasons.json");
		
		File failureConfigFile = new File(failureConfigLoc);
		
		if (!failureConfigFile.exists()) {
			System.out.println("Ignoring Failure Reasons because JSON file was not found in path: " + failureConfigLoc);
			return null;
		}
		
		
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setLenient();
		Gson gson = gsonBuilder.create();
		JsonReader reader = null;

		try {
			reader = new JsonReader(new FileReader(failureConfigLoc));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Problem parsing Failure Reason JSON file: " + failureConfigLoc);
			e.printStackTrace();
		}
		Messages[] response = gson.fromJson(reader, Messages[].class);
		
		for (Messages messages : response) {
			if (messages.getStackTraceErrors() == null) {
				System.out.println("Failure Reason JSON file has wrong formmat, please read here https://developers.perfectomobile.com/pages/viewpage.action?pageId=31103917: " + failureConfigLoc);
				return null;
			
			}
				
			for (String error : ListUtils.emptyIfNull(messages.getStackTraceErrors()))	{
				if (actualMessage.contains(error)) {
					messages.setJsonFile(failureConfigLoc);
					return messages;
			}
		}
//			if (messages.getStackTraceErrors().toString().contains(actualMessage)) {
//				messages.setJsonFile(failureConfigLoc);
//				return messages;
//			}

		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onStart(ITestContext context) {
		if (getBundle().getString("remote.server", "").contains("perfecto")) {

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

	@Override
	public void onTestStart(ITestResult testResult) {
		if (getBundle().getString("remote.server", "").contains("perfecto")) {

			// get custom fields "%name-value" from groups
			// compile actual groups
			String[] groups = testResult.getMethod().getGroups();
			ArrayList<String> groupsFinal = new ArrayList<String>();

			ArrayList<CustomField> cfc = new ArrayList<CustomField>();
			for (String string : groups) {
				if (string.startsWith(getBundle().getString("custom.field.identifier", "%"))) {
					try {
						cfc.add(new CustomField(
								string.split(getBundle().getString("custom.field.delimiter", "-"))[0].substring(1),
								string.split(getBundle().getString("custom.field.delimiter", "-"))[1]));
					} catch (Exception ex) {
						throw new NullPointerException(
								"Custom field key/value pair not delimited properly.  Example of proper default usage: %Developer-Jeremy.  Check application properties custom.field.delimiter and custom.field.identifier for custom values that may have been set.");
					}
				} else {
					groupsFinal.add(string);
				}
			}

			Builder testContext = new TestContext.Builder();
			if (groupsFinal.size() > 0) {
				testContext.withTestExecutionTags(groupsFinal.toString().replace('[', ' ').replace(']',' ').split(","));
			}

			if (cfc.size() > 0) {
				testContext.withCustomFields(cfc);
			}

			createReportiumClient(testResult).testStart(
					testResult.getMethod().getMethodName() + getDataDrivenText(testResult), testContext.build());

			if (testResult.getParameters().length > 0 && getBundle().getBoolean("addFullDataToReport", false)) {
				logStepStart("Test Data used");
				ReportUtils.reportComment(testResult.getParameters()[0].toString());
				logStepEnd();
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
		//logStepEnd();
		String msg = "END STEP: " + stepExecutionTracker.getStep().getDescription();
		ConsoleUtils.logInfoBlocks(msg, ConsoleUtils.upper_block + " ", 10);
	}

	@Override
	public void onFailure(StepExecutionTracker stepExecutionTracker) {

	}

	@Override
	public void onTestSuccess(ITestResult testResult) {
		ReportiumClient client = getReportClient();
		if (null != client) {
			client.testStop(TestResultFactory.createSuccess());
			logTestEnd(testResult);
		}

		tearIt(testResult);
	}

	@Override
	public void onTestFailure(ITestResult testResult) {
		ReportiumClient client = getReportClient();
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
				client.testStop(TestResultFactory.createFailure(failMsg.isEmpty() ? "An error occurred" : failMsg,
						new Exception(
								"There was some validation failure in the scenario which did not provide any throwable object.")));
			} else {
				ExceptionUtils.getStackTrace(testResult.getThrowable());
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

					Builder testContext = new TestContext.Builder();

					if (cfc.size() > 0) {
						testContext.withCustomFields(cfc);
					}

					if (tagsFinal.size() > 0) {
						testContext.withTestExecutionTags(tagsFinal);
					}

					TestResult reportiumResult = TestResultFactory.createFailure(
							failMsg.isEmpty() ? "An error occurred" : failMsg, testResult.getThrowable(), customError);
					client.testStop(reportiumResult, testContext.build());
				} else {
					client.testStop(TestResultFactory.createFailure(failMsg.isEmpty() ? "An error occurred" : failMsg,
							testResult.getThrowable()));
				}
			}

			logTestEnd(testResult);

			tearIt(testResult);
		}
	}

	private void tearIt(ITestResult testResult) {
		if ((testResult.getTestContext().getCurrentXmlTest().getParallel().toString().equalsIgnoreCase("methods")
				& testResult.getTestClass().getName().toLowerCase().contains("scenario"))
				|| ConfigurationManager.getBundle().getString("global.datadriven.parallel", "false")
						.equalsIgnoreCase("true")
				|| testResult.getTestContext().getCurrentXmlTest().getXmlClasses().get(0).getName()
						.contains("com.qmetry.qaf.automation.step.client.excel.ExcelTestFactory")
				|| testResult.getTestContext().getCurrentXmlTest().getXmlClasses().get(0).getName()
						.contains("com.qmetry.qaf.automation.step.client.csv.KwdTestFactory")

		) {
			Object testInstance = testResult.getInstance();
			((WebDriverTestCase) testInstance).getTestBase().tearDown();
		}
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		ReportiumClient client = getReportClient();
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

	@Override
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
		String allTags = xmlTestName + "," + suiteName
				+ (System.getProperty("reportium-tags") == null ? "" : "," + System.getProperty("reportium-tags"));

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
				System.out.println("Adding driver with name - " + driverName);
				DriverUtils.switchToDriver(driverName);
				driverList.put(driverName, DeviceUtils.getQAFDriver());
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
		List<String> optionsList = cucumberOptions.getFilters().stream().map(object -> Objects.toString(object, null))
				.collect(Collectors.toList());
		optionsList.addAll(cucumberOptions.getFeaturePaths());
		optionsList.addAll(cucumberOptions.getGlue());

		return ArrayUtils.addAll(super.getTags(testResult), optionsList.toArray(new String[optionsList.size()]));
	}

	private RuntimeOptions getCucumberOptions(ITestResult testResult) {
		try {
			return new RuntimeOptionsFactory(Class.forName(testResult.getTestClass().getName())).create();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void addReportLink(ITestResult result, String url) {
		((TestNGScenario) result.getMethod()).getMetaData().put("Perfecto-report",
				"<a href=\"" + url + "\" target=\"_blank\">view</a>");
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
				"\\\"([a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/? ]*)[$][{](([a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/? ]*))}([a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/? ]*)\\\"");

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

			System.out.println(paramNames);

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
}
