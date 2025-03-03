package com.perfecto.reportium.client;

import com.perfecto.reportium.test.result.TestResultFailure;
import com.perfecto.reportium.test.result.TestResultSuccess;
import com.perfecto.reportium.test.result.TestResultVisitor;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class PerfectoTestResultVisitor implements TestResultVisitor {

    private final static String FAILURE_REASON_PARAM_NAME = "failureReason";

    private Map<String, Object> params;

    public PerfectoTestResultVisitor(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public void visit(TestResultSuccess testResultSuccess) {
        params.put("success", true);
    }

    @Override
    public void visit(TestResultFailure testResultFailure) {
        params.put("success", false);
        params.put("failureDescription", testResultFailure.getMessage());
        String failureReasonName = testResultFailure.getFailureReasonName();
        if (StringUtils.isNotBlank(failureReasonName)) {
            params.put(FAILURE_REASON_PARAM_NAME, failureReasonName);
        }
    }
}
