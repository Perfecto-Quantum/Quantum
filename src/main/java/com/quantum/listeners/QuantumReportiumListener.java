package com.quantum.listeners;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.IInvokedMethod;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.google.gson.Gson;
import com.perfecto.reportium.WebDriverProvider;
import com.perfecto.reportium.client.ReportiumClient;
import com.perfecto.reportium.client.ReportiumClientFactory;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.PerfectoExecutionContext;
import com.perfecto.reportium.model.Project;
import com.perfecto.reportium.test.TestContext;
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
import com.qmetry.qaf.automation.step.client.text.BDDDefinitionHelper;
import com.qmetry.qaf.automation.step.client.text.BDDDefinitionHelper.ParamType;
import com.qmetry.qaf.automation.ui.WebDriverTestCase;
import com.qmetry.qaf.automation.util.FileUtil;
import com.quantum.utils.ConsoleUtils;
import com.quantum.utils.ReportUtils;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;

/**
 * Created by mitchellw on 9/27/2016.
 */
public class QuantumReportiumListener extends ReportiumTestNgListener implements QAFTestStepListener, ITestListener {

	public static final String PERFECTO_REPORT_CLIENT = "perfecto.report.client";

	public static ReportiumClient getReportClient() {
		return (ReportiumClient) getBundle().getObject(PERFECTO_REPORT_CLIENT);
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
			createReportiumClient(testResult).testStart(
					testResult.getMethod().getMethodName() + getDataDrivenText(testResult),
					new TestContext(testResult.getMethod().getGroups()));
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
		logStepEnd();
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

			if(testResult.getThrowable() == null) {
				client.testStop(TestResultFactory.createFailure(failMsg.isEmpty() ? "An error occurred" : failMsg,
						new Exception("There was some validation failure in the scenario which did not provide any throwable object.")));
			} else {
				String actualExceptionMessage = ExceptionUtils.getStackTrace(testResult.getThrowable());
				String failureReason = findFailureReason(actualExceptionMessage);
				if (!failureReason.isEmpty()) {
					TestResult reportiumResult = TestResultFactory.createFailure(
							failMsg.isEmpty() ? "An error occurred" : failMsg, testResult.getThrowable(), failureReason);
					client.testStop(reportiumResult);
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
						.equalsIgnoreCase("true")) {
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
		WebDriver driver = null;
		if (testInstance instanceof WebDriverTestCase)
			driver = ((WebDriverTestCase) testInstance).getDriver();
		else if (testInstance instanceof WebDriverProvider)
			driver = ((WebDriverProvider) testInstance).getWebDriver();
		if (driver != null) {
			PerfectoExecutionContext perfectoExecutionContext = new PerfectoExecutionContext.PerfectoExecutionContextBuilder()
					.withProject(new Project(prjName, prjVer)).withContextTags(allTags.split(","))
					.withJob(new Job(getBundle().getString("JOB_NAME", System.getProperty("reportium-job-name")),
							getBundle().getInt("BUILD_NUMBER",
									System.getProperty("reportium-job-number") == null ? 0
											: Integer.parseInt(System.getProperty("reportium-job-number"))))
													.withBranch(System.getProperty("reportium-job-branch")))
					.withWebDriver(driver).build();

			reportiumClient = new ReportiumClientFactory().createPerfectoReportiumClient(perfectoExecutionContext);
		}
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
		Pattern p = Pattern.compile("[$][{](.*?)}");
		Matcher matcher = p.matcher(def);
		List<String> args = new ArrayList<String>();
		while (matcher.find()) {
			args.add(matcher.group().replace("$", ""));
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
			Map<String, Object> paramMap = step.getStepExecutionTracker().getContext();
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
		// }
		return description;
	}

	/**
	 * This method is used to find the failure reason from the given JSON file in
	 * location - src/main/resources/failureReasons.json
	 * 
	 * @param actualExceptionMessage
	 *            - The failure exception stacktrace from the test failure
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private static String findFailureReason(String actualExceptionMessage) {
		String jsonStr;
		String failureConfigLoc = ConfigurationManager.getBundle().getString("failureReasonConfig",
				"src/main/resources/failureReasons.json");
		try {
			jsonStr = FileUtil.readFileToString(new File(failureConfigLoc));
			JSONArray frArr = new JSONArray(jsonStr);
			List<String> failureReasons = new ArrayList<String>();
			for (int i = 0; i < frArr.length(); i++) {
				JSONObject jsonObj = frArr.getJSONObject(i);
				String tempKey = "";
				for (String key : jsonObj.keySet()) {
					tempKey = key;
					failureReasons.add(key);
				}
				JSONArray tempArray = jsonObj.getJSONArray(tempKey);

				for (int j = 0; j < tempArray.length(); j++) {
					String excepMsg = tempArray.getString(j);
					if (actualExceptionMessage.contains(excepMsg)) {
						return tempKey;
					}
				}
			}
			return "";
		} catch (IOException e) {
			return "";
		}
	}
}
