package com.perfecto.reportium.testng;

import com.perfecto.reportium.client.ReportiumClientProvider;
import com.perfecto.reportium.client.DigitalZoomClient;
import com.perfecto.reportium.exception.ReportiumException;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.Project;
import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.result.TestResultFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

public abstract class BaseReportiumTestNgListener implements IInvokedMethodListener {
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            // Before execution of test method
            reportTestStart(testResult);
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            // After execution of test method
            reportTestEnd(testResult);
        }
    }

    /**
     * Returns the test name reported to Reportium.
     * Override this method to use custom test names, e.g. using the class's simple name (without the package),
     * or for using a different separator between the test name and the method name.
     *
     * @param result TestNG's ITestResult instance
     * @return Test name to be reported for the test execution instance in Reportium
     */
    protected String getTestName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "::" + result.getName();
    }

    protected void reportTestStart(ITestResult testResult) {
        ReportiumClientProvider.get().testStart(getTestName(testResult), new TestContext());
    }

    protected void reportTestEnd(ITestResult testResult) {
        int status = testResult.getStatus();
        DigitalZoomClient client = ReportiumClientProvider.get();
        if (client != null) {
            switch (status) {
                case ITestResult.FAILURE:
                    client.testStop(TestResultFactory.createFailure(testResult.getThrowable()));
                    break;
                case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
                case ITestResult.SUCCESS:
                    client.testStop(TestResultFactory.createSuccess());
                    break;
                case ITestResult.SKIP:
                    // might get here when retry analyzer decided to retry
                    break;
                case ITestResult.STARTED:
                default:
                    throw new ReportiumException("Unexpected status " + status);
            }
        }
    }

    protected DigitalZoomClient getReportiumClient() {
        return ReportiumClientProvider.get();
    }

    /**
     * Returns the job details used to identify the tests running by this instance of TestNG.
     * the default implementation creates the job based on the environment variables:
     * <ul>
     * <li>{@link com.perfecto.reportium.client.Constants.SDK#jobNameParameterNameV1}</li>
     * <li>{@link com.perfecto.reportium.client.Constants.SDK#jobNumberParameterNameV1}</li>
     * </ul>
     * <p>
     * Override this method to provide custom Job details.
     *
     * @return Job used to identify current test executions
     */
    protected Job getJob() {
        return null;
    }

    /**
     * Returns the project details used to identify the tests running by this instance of TestNG.
     * the default implementation creates the connection based on the environment variables:
     * <ul>
     * <li>{@link com.perfecto.reportium.client.Constants.SDK#projectNameParameterNameV1}</li>
     * <li>{@link com.perfecto.reportium.client.Constants.SDK#projectVersionParameterNameV1}</li>
     * </ul>
     * <p>
     * Override this method to provide custom Project details.
     *
     * @return Project used to identify current test executions
     */
    protected Project getProject() {
        return null;
    }

    /**
     * Extract predefined tags from the class annotations and the method annotations.
     * the default implementation extracts tags based on the environment variable:
     * <ul>
     * <li>{@link com.perfecto.reportium.client.Constants.SDK#jvmTagsParameterNameV1}</li>
     * </ul>
     * <p>
     * Override this method to provide custom tags without specifying them as environment variables.
     *
     * @param testResult test instance
     * @return list of tags extracted from test class and method annotations
     */
    protected String[] getTags(ITestResult testResult) {
        return null;
    }
}
