package com.perfecto.reportium.test.result;

/**
 * Factory for creating test results
 */
public class TestResultFactory {

    /**
     * Creates a successful test execution result
     *
     * @return An object denoting a successful test execution
     */
    public static TestResult createSuccess() {
        return new TestResultSuccess();
    }

    /**
     * Creates a failed test execution result
     *
     * @param message Test failure reason
     * @return An object denoting a failed test execution
     */
    public static TestResult createFailure(String message) {
        return createFailure(message, null, null);
    }

    /**
     * Creates a failed test execution result
     *
     * @param throwable Test failure stacktrace
     * @return An object denoting a failed test execution
     */
    public static TestResult createFailure(Throwable throwable) {
        return createFailure(null, throwable, null);
    }

    /**
     * Creates a failed test execution result
     *
     * @param throwable Test failure stacktrace
     * @param failureReason Test failure reason
     * @return An object denoting a failed test execution
     */
    public static TestResult createFailure(Throwable throwable, String failureReason) {
        return createFailure(null, throwable, failureReason);
    }

    /**
     * Creates a failed test execution result
     *
     * @param message Test failure message
     * @param throwable Test failure stacktrace
     * @return An object denoting a failed test execution
     */
    public static TestResult createFailure(String message, Throwable throwable) {
        return createFailure(message, throwable, null);
    }

    /**
     * Creates a failed test execution result
     *
     * @param message Test failure message
     * @param throwable Test failure stacktrace
     * @param failureReason Test failure reason
     * @return An object denoting a failed test execution
     */
    public static TestResult createFailure(String message, Throwable throwable, String failureReason) {
        return new TestResultFailure(message, throwable, failureReason);
    }
}
