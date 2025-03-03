package com.perfecto.reportium.client;

import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.result.TestResult;

import java.util.logging.Logger;

/**
 * A Reportium client implementation that outputs to {@link java.util.logging.Logger}.
 * <p>
 * Useful for test authoring phase when tests are running locally.
 */
class LoggerReportiumClient implements ReportiumClient {

    private final Logger logger = Logger.getLogger("ReportiumLogger");

    @Override
    public void testStart(String name, TestContext context) {
        logger.info("Starting test - " + name + " - with context " + context.getTestExecutionTags());
    }

    @Override
    @Deprecated
    public void testStep(String description) {
        logger.info("Executing step - " + description);
    }

    @Override
    public void stepStart(String description) {
        logger.info("Starting step - " + description);
    }

    @Override
    public void stepEnd() {
        logger.info("Ending step");
    }

    @Override
    public void stepEnd(String message) {
        logger.info("Ending step - " + message);
    }

    @Override
    public void reportiumAssert(String message, boolean status) {
        logger.info("Reportium assert - status: " + status + " - with message: " + message);
    }

    @Override
    public void testStop(TestResult testResult) {
        logger.info("Test result: " + testResult);
    }

    @Override
    public void testStop(TestResult testResult, TestContext testContext) {
        logger.info("Test result: " + testResult + " with test context " + testContext);
    }

    @Override
    public String getReportUrl() {
        return "N/A - local logger";
    }
}
