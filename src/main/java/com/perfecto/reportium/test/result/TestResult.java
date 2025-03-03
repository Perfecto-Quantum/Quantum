package com.perfecto.reportium.test.result;

/**
 * Marker interface denoting a test result
 */
public interface TestResult {

    void visit(TestResultVisitor visitor);
}
