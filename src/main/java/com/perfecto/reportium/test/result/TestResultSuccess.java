package com.perfecto.reportium.test.result;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Denotes a successful test execution
 */
public class TestResultSuccess implements TestResult {

    @Override
    public void visit(TestResultVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .toString();
    }
}
