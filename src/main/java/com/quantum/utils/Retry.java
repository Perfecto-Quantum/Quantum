package com.quantum.utils;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;
import static com.qmetry.qaf.automation.util.StringUtil.toStringWithSufix;

import java.util.List;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.step.client.TestNGScenario;

public class Retry implements IRetryAnalyzer {
	public static String RETRY_INVOCATION_COUNT = "retry.invocation.count";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.testng.IRetryAnalyzer#retry(org.testng.ITestResult)
	 */
	@Override
	public boolean retry(ITestResult result) {

		boolean shouldRetry = shouldRetry(result);
		if (shouldRetry) {
			try {
				if (result.getMethod() instanceof TestNGScenario) {
					((TestNGScenario) result.getMethod()).decAndgetCurrentInvocationCount();
				}
			} catch (Exception e) {
				System.err.println(e);
			}
			int retryInvocationCount = getRetryCount() + 1;
			System.err.println(
					"Retrying [" + result.getName() + "]" + toStringWithSufix(retryInvocationCount) + " time.");

			getBundle().addProperty(RETRY_INVOCATION_COUNT, retryInvocationCount);

			// correct failed invocation numbers for data driven test case.
			List<Integer> failedInvocations = result.getMethod().getFailedInvocationNumbers();
			if (null != failedInvocations && !failedInvocations.isEmpty()) {
				int lastFailedIndex = failedInvocations.size() - 1;
				failedInvocations.remove(lastFailedIndex);
			}

		} else {
			getBundle().clearProperty(RETRY_INVOCATION_COUNT);
		}

		return shouldRetry;
	}

	public boolean shouldRetry(ITestResult result) {
		// Throwable reason = result.getThrowable();
		int retryCount = getRetryCount();
		boolean shouldRetry = (result.getStatus() == ITestResult.FAILURE)
				&& (ApplicationProperties.RETRY_CNT.getIntVal(0) > retryCount);

		return shouldRetry;
	}

	protected int getRetryCount() {
		return getBundle().getInt(RETRY_INVOCATION_COUNT, 0);
	}
}