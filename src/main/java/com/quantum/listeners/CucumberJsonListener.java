package com.quantum.listeners;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import static com.qmetry.qaf.automation.core.ConfigurationManager.*;
import com.qmetry.qaf.automation.step.QAFTestStepListener;
import com.qmetry.qaf.automation.step.StepExecutionTracker;
import com.qmetry.qaf.automation.util.JSONUtil;
import com.quantum.utils.DriverUtils;
import static com.quantum.utils.CucumberJsonUtils.*;

public class CucumberJsonListener implements QAFTestStepListener,ITestListener, ISuiteListener {

	private static final ThreadLocal<List<JsonObject>> threadLocalSteps =
	        ThreadLocal.withInitial(ArrayList::new);
	    private ConcurrentLinkedQueue<JsonObject> scenariosSuite = null;
    private File runFolder;

    @Override
    public void onTestStart(ITestResult result) {
        getBundle().setProperty("test_start_timestamp", Instant.now().toString());
        threadLocalSteps.set(new ArrayList<>());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
    	List<JsonObject> steps = threadLocalSteps.get();
        JsonObject scenario = null;
		try {
			scenario = buildScenario(result, steps);
		} catch (IOException e) {
			e.printStackTrace();
		}
        if (scenario != null) {
            writeScenario(result, scenario);
        }
        threadLocalSteps.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
    	List<JsonObject> steps = threadLocalSteps.get();
        JsonObject scenario = null;
		try {
			scenario = buildScenario(result, steps);
		} catch (IOException e) {
			e.printStackTrace();
		}
        if (scenario != null) {
            writeScenario(result, scenario);
        }
        threadLocalSteps.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
    	List<JsonObject> steps = threadLocalSteps.get();
        JsonObject scenario = null;
		try {
			scenario = buildScenario(result, steps);
		} catch (IOException e) {
			e.printStackTrace();
		}
        if (scenario != null) {
            writeScenario(result, scenario);
        }
        threadLocalSteps.remove();
    }

    @Override
    public void onFailure(StepExecutionTracker stepExecutionTracker) {
        stepExecutionTracker.setSuccess(false);
    }

    @Override
    public void beforExecute(StepExecutionTracker stepExecutionTracker) {
        stepExecutionTracker.setSuccess(true);
    }

    @Override
    public void afterExecute(StepExecutionTracker stepExecutionTracker) {
        JsonObject obj = buildStep(stepExecutionTracker);
        List<JsonObject> stepObjects = threadLocalSteps.get();
        stepObjects.add(obj);
    }

    @Override
    public void onStart(ITestContext context) {
        threadLocalSteps.set(new ArrayList<>());
    }

    @Override
    public void onFinish(ITestContext context) {
        // no-op; suite-level accumulation is done in per-suite listener
    }

    @Override
    public void onStart(ISuite suite) {
    	scenariosSuite = new ConcurrentLinkedQueue<>();
        String reportRoot = getBundle().getString("jsonReport.dir", "json-reports");
        String timestampFolder = "run_" + Instant.now().toString().replace(":", "-").replace(".", "-");
        runFolder = new File(reportRoot, timestampFolder);
        if (!runFolder.exists() && !runFolder.mkdirs()) {
            System.err.println("Unable to create run directory: " + runFolder.getAbsolutePath());
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        // per-test files are written when each test completes; no suite-level aggregation here
    }

    private void writeScenario(ITestResult result, JsonObject scenario) {
        if (runFolder == null) {
            return;
        }

        String testName = result.getName();
        String safeName = testName.replaceAll("[^a-zA-Z0-9._-]", "_");
        File testFolder = new File(runFolder, safeName);
        if (!testFolder.exists() && !testFolder.mkdirs()) {
            System.err.println("Unable to create test directory: " + testFolder.getAbsolutePath());
            return;
        }

        String fileName = new File(testFolder, "result.json").getAbsolutePath();
        JSONUtil.writeJsonObjectToFile(fileName, scenario);
    }

}
