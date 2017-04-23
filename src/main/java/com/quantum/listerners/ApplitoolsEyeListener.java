package com.quantum.listerners;

import com.applitools.eyes.MatchLevel;
import com.qmetry.qaf.automation.step.QAFTestStepListener;
import com.qmetry.qaf.automation.step.StepExecutionTracker;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import com.applitools.eyes.Eyes;
import com.applitools.eyes.TestResults;
import com.qmetry.qaf.automation.core.ConfigurationManager;

/**
 * Created by lirona on 26/03/2017.
 */
public class ApplitoolsEyeListener implements QAFTestStepListener, ITestListener {
    @Override
    public void onFailure(StepExecutionTracker stepExecutionTracker) {

    }

    @Override
    public void beforExecute(StepExecutionTracker stepExecutionTracker) {

    }

    @Override
    public void afterExecute(StepExecutionTracker stepExecutionTracker) {

    }

    @Override
    public void onTestStart(ITestResult result) {

    }

    @Override
    public void onTestSuccess(ITestResult result) {

    }

    @Override
    public void onTestFailure(ITestResult result) {

    }

    @Override
    public void onTestSkipped(ITestResult result) {

    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {

    }

    @Override
    public void onStart(ITestContext context) {
        System.out.print("Starting Applitools Eyes driver...");
        String applitoolsAPIKey = ConfigurationManager.getBundle().getString("applitools.key");
        if (null != applitoolsAPIKey) {
            Eyes e = new Eyes();
            e.setApiKey(applitoolsAPIKey);
            String applitoolsMatchLevel = ConfigurationManager.getBundle().getString("applitools.matchLevel"); //Exact/Strict/Content/Layout/Layout2 (Recommended)
            if (null != applitoolsMatchLevel){
                switch (applitoolsMatchLevel){
                    case "Exact" :
                        e.setMatchLevel(MatchLevel.EXACT);
                        break;
                    case "Strict" :
                        e.setMatchLevel(MatchLevel.STRICT);
                        break;
                    case "Content" :
                        e.setMatchLevel(MatchLevel.CONTENT);
                        break;
                    case "Layout" :
                        e.setMatchLevel(MatchLevel.LAYOUT);
                        break;
                    case "Layout2" :
                        e.setMatchLevel(MatchLevel.LAYOUT2);
                        break;
                    default :
                        System.out.println("Wrong Applitools Match Level configuration - eye object will use default match level.");
                        break;
                }
            }
            ConfigurationManager.getBundle().setProperty("Eyes", e);
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.print("Closing Applitools Eyes driver...\n");
        String applitoolsAPIKey = ConfigurationManager.getBundle().getString("applitools.key");
        Eyes e = null;
        if (null != applitoolsAPIKey) {
            try {
                e = (Eyes) ConfigurationManager.getBundle().getObject("Eyes");
                TestResults results = e.close();
                // Add validator
                System.out.println("Applitools report URL: " + results.getUrl());
            }finally {
                if (null != e)
                    e.abortIfNotClosed();
            }
        }
    }
}
