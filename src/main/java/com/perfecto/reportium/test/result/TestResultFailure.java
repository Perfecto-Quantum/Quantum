package com.perfecto.reportium.test.result;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Objects;

/**
 * Denotes a failed test execution with the failure reason and exception
 */
public class TestResultFailure implements TestResult {

    public static final int MESSAGE_MAX_LENGTH = 4096;
    public static final String TRIMMED_TEXT_SUFFIX = "...";

    private String message;
    private String failureReasonName;

    public TestResultFailure(String reason, Throwable throwable, String failureReasonName) {
        this.failureReasonName = failureReasonName;

        if (throwable != null) {
            this.message = ExceptionUtils.getStackTrace(throwable);
            if (StringUtils.isNotBlank(reason) && !Objects.equals(reason, throwable.getMessage())) {
                this.message = reason + ". Stack Trace: " + this.message;
            }
        } else if (StringUtils.isNotBlank(reason)) {
            this.message = reason;
        }

        if (message != null && message.length() > MESSAGE_MAX_LENGTH) {
            message = message.substring(0, MESSAGE_MAX_LENGTH - TRIMMED_TEXT_SUFFIX.length()) + TRIMMED_TEXT_SUFFIX;
        }
    }

    public String getMessage() {
        return message;
    }

    public String getFailureReasonName() {
        return failureReasonName;
    }

    @Override
    public void visit(TestResultVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("message", message)
                .append("failureReasonName", failureReasonName)
                .toString();
    }
}
