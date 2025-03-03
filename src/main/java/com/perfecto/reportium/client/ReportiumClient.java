package com.perfecto.reportium.client;

import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.result.TestResult;

/**
 * Main Reportium client API
 * <p>
 * This interface is used for reporting a test script actions, namely
 * <ol>
 * <li>Starting a new functional test</li>
 * <li>Functional steps that are taken as part of the test</li>
 * <li>Test result (pass/fail) when it ends</li>
 * </ol>
 * <p>
 * Creation of concrete instances is done by {@link ReportiumClientFactory} methods
 */
public interface ReportiumClient extends DigitalZoomClient {

    /**
     * Log a new logical step for the current test, e.g. "Submit shopping cart"
     * This method is deprecated. Please use stepStart
     *
     * @param description Step description
     */
    @Deprecated
    void testStep(String description);

    /**
     * Adding assertions to the Execution Report. This method will not fail the test
     *
     * @param message will be used to label the assertion
     * @param status  indicates the result of the verification operation
     * @since engine 10.2
     */
    void reportiumAssert(String message, boolean status);

}
