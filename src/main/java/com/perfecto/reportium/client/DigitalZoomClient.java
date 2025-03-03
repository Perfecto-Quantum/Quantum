package com.perfecto.reportium.client;

import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.result.TestResult;

public interface DigitalZoomClient {
    /**
     * Create a new test execution
     *
     * @param name    Test name
     * @param context Testing env context, e.g. CI build number
     */
    void testStart(String name, TestContext context);

    /**
     * Indicates that the test has stopped and its execution status.
     *
     * @param testResult Test execution result
     */
    void testStop(TestResult testResult);

    /**
     * Indicates that the test has stopped and its execution status and context.
     * @param testResult Test execution result
     * @param testContext Testing env context, e.g. CI build number
     */
    void testStop(TestResult testResult, TestContext testContext);

    /**
     * Log a new logical step for the current test, e.g. "Submit shopping cart"
     *
     * @param description Step description
     * @since engine 10.2
     */
    void stepStart(String description);

    /**
     * Log the end of the current logical step for the current test
     *
     * @since engine 10.2
     */
    void stepEnd();

    /**
     * Log the end of the current logical step for the current test
     *
     * @param message step message
     * @since engine 10.2
     */
    void stepEnd(String message);

    /**
     * Returns the URL to the created online report in Perfecto's reporting solution.
     * <p>
     * The report is based on <strong>all</strong> tests that match the current execution context, and is not
     * limited to a single functional test execution.
     *
     * @return URL to the created online report
     */
    String getReportUrl();
}
