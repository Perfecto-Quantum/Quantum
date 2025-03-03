package com.perfecto.reportium.test.result;

public interface TestResultVisitor {

    void visit(TestResultSuccess testResultSuccess);

    void visit(TestResultFailure testResultFailure);
}
