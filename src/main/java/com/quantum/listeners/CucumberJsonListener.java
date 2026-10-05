package com.quantum.listeners;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	private static final ThreadLocal<ConcurrentLinkedQueue<JsonObject>> threadLocalScenarios = 
	        ThreadLocal.withInitial(ConcurrentLinkedQueue::new);
	    
	    private static final ThreadLocal<ConcurrentLinkedQueue<JsonObject>> threadLocalSteps = 
	        ThreadLocal.withInitial(ConcurrentLinkedQueue::new);
	    private List<JsonObject> scenariosSuite = null;
	    
	    @Override
	    public void onTestStart(ITestResult result) {
	        getBundle().setProperty("test_start_timestamp", Instant.now().toString());
	        getBundle().setProperty("stepObjects", threadLocalSteps.get());
	    }
	    
	    @Override
	    public void onTestSuccess(ITestResult result) {
	    	getBundle().setProperty("stepObjects", threadLocalSteps.get());
	        JsonObject scenario = null;
			try {
				scenario = buildScenario(result);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        threadLocalScenarios.get().add(scenario);
	        threadLocalSteps.remove();
	        Map<String, Object> pars = new HashMap<>();
	        DriverUtils.getDriver().executeScript("mobile:vnetwork:stop", pars); 
	    }
	    
	    @Override
	    public void onTestFailure(ITestResult result) {
	    	getBundle().setProperty("stepObjects", threadLocalSteps.get());
	        JsonObject scenario = null;
			try {
				scenario = buildScenario(result);
			} catch (IOException e) {
				e.printStackTrace();
			}
	        threadLocalScenarios.get().add(scenario);
	        threadLocalSteps.remove();
	    }
	    
	    @Override
	    public void onTestSkipped(ITestResult result) {
	    	getBundle().setProperty("stepObjects", threadLocalSteps.get());
	        JsonObject scenario = null;
			try {
				scenario = buildScenario(result);
			} catch (IOException e) {
				e.printStackTrace();
			}
	        threadLocalScenarios.get().add(scenario);
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
	        ConcurrentLinkedQueue<JsonObject> stepObjects = threadLocalSteps.get();
	        stepObjects.add(obj);
	    }
	    
	    @Override
	    public void onStart(ITestContext context) {
	        threadLocalScenarios.set(new ConcurrentLinkedQueue<JsonObject>());
	        threadLocalSteps.set(new ConcurrentLinkedQueue<JsonObject>());
	    }
	    
	    @Override
	    public void onFinish(ITestContext context) {
	        
	        ConcurrentLinkedQueue<JsonObject> scenarios = threadLocalScenarios.get();
	        scenariosSuite.addAll(scenarios);
	    }
	    
	    @Override
	    public void onStart(ISuite suite) {
	    	// TODO Auto-generated method stub
	    	scenariosSuite = new ArrayList<>();
	    }
	    
	    @Override
	    public void onFinish(ISuite suite) {
	    	JsonArray array = new JsonArray();
	        for (JsonObject scenario : scenariosSuite) {
	            array.add(scenario);
	        }
	        
	        String fileName = getBundle().getString("jsonReport.dir", "json-reports")+File.separator+suite.getName() +"_"+ System.currentTimeMillis()+File.separator + "result.json";
	        System.out.println("Writing to file: " + fileName + " with " + array.size() + " scenarios");
	        JSONUtil.writeJsonObjectToFile(fileName, array);
	    	
	    }

}
