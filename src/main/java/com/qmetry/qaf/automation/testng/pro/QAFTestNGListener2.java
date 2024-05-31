/*******************************************************************************
 * Copyright (c) 2019 Infostretch Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.qmetry.qaf.automation.testng.pro;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.testng.IInvokedMethod;
import org.testng.IRetryAnalyzer;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Parameters;
import org.testng.xml.XmlSuite;

import com.qmetry.qaf.automation.core.CheckpointResultBean;
import com.qmetry.qaf.automation.core.LoggingBean;
import com.qmetry.qaf.automation.core.QAFTestBase;
import com.qmetry.qaf.automation.core.TestBaseProvider;
import com.qmetry.qaf.automation.integration.ResultUpdator;
import com.qmetry.qaf.automation.integration.TestCaseRunResult;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.step.client.TestNGScenario;
import com.qmetry.qaf.automation.testng.RetryAnalyzer;
import com.qmetry.qaf.automation.testng.dataprovider.DataProviderUtil;
import com.qmetry.qaf.automation.testng.report.ReporterUtil;
import com.qmetry.qaf.automation.util.ClassUtil;
import com.qmetry.qaf.automation.util.StringUtil;

/**
 * All in one Listener for ISFW. If this listener is added, you don't required
 * to add any other ISFW specific listener.
 * 
 * @author Chirag Jayswal.
 */
public class QAFTestNGListener2 extends QAFTestNGListener
// implements
// IAnnotationTransformer2,
// IMethodInterceptor,
// IResultListener,
// ISuiteListener,
// IInvokedMethodListener2,
// IMethodSelector
{
	private final Log logger = LogFactoryImpl.getLog(getClass());

	public QAFTestNGListener2() {
		logger.debug("QAFTestNGListener registered!...");

	}

	@Override
	public void onStart(final ISuite suite) {
		if (skipReporting())
			return;
		super.onStart(suite);
		ReporterUtil.createMetaInfo(suite);
	}

	@Override
	public void onFinish(ISuite suite) {
		if (skipReporting())
			return;
		super.onFinish(suite);
		logger.debug("onFinish: start");
		ReporterUtil.createMetaInfo(suite);
		logger.debug("onFinish: done");

	}

	@Override
	public void onStart(ITestContext testContext) {
		super.onStart(testContext);
		if (!skipReporting()) {
			if(!skipUpdateReport()) {
				ReporterUtil.updateOverview(testContext, null);
			}
		}
	}
	
	public static boolean skipUpdateReport() {
		return getBundle().getBoolean("skip.qaf.report.update", true);
	}

	@Override
	public void onFinish(ITestContext testContext) {
		if (skipReporting())
			return;

		super.onFinish(testContext);
		
		if(!skipUpdateReport()) {
			ReporterUtil.updateOverview(testContext, null);
		}
		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void transform(ITestAnnotation testAnnotation, Class clazz, Constructor arg2, Method method) {
		try {
			if (null != method) {
				if (null != method.getParameterTypes() && (method.getParameterTypes().length > 0)
						&& !method.isAnnotationPresent(Parameters.class)) {
					DataProviderUtil.setQAFDataProvider(testAnnotation, method);
				}

				String tmtURL = getBundle().getString(method.getName() + ".testspec.url");
				if (StringUtil.isNotBlank(tmtURL)) {
					String desc = String.format("%s<br/><a href=\"%s\">[test-spec]</a>",
							testAnnotation.getDescription(), tmtURL);
					testAnnotation.setDescription(desc);
				}
				if (getBundle().getBoolean("report.javadoc.link", false)) {
					String linkRelPath = String.format("%s%s.html#%s",
							getBundle().getString("javadoc.folderpath", "../../../docs/tests/"),
							method.getDeclaringClass().getCanonicalName().replaceAll("\\.", "/"),
							ClassUtil.getMethodSignture(method, false));

					String desc = String.format(
							"%s " + getBundle().getString("report.javadoc.link.format",
									"<a href=\"%s\" target=\"_blank\">[View-doc]</a>"),
							testAnnotation.getDescription(), linkRelPath);
					testAnnotation.setDescription(desc);
				}
				testAnnotation.setDescription(getBundle().getSubstitutor().replace(testAnnotation.getDescription()));
				
				testAnnotation.setRetryAnalyzer((Class<? extends IRetryAnalyzer>)Class
						.forName(ApplicationProperties.RETRY_ANALYZER.getStringVal(RetryAnalyzer.class.getName())));

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void afterInvocation(final IInvokedMethod method, final ITestResult tr, final ITestContext context) {
		super.afterInvocation(method, tr, context);
	}

	@Override
	protected void report(ITestResult tr) {
		super.report(tr);
		if(!getBundle().getBoolean("cucumber.run.mode", false)) {
			deployResult(tr);
			if (!getBundle().getBoolean("disable.qaf.testng.reporter", true)) {
				QAFTestBase stb = TestBaseProvider.instance().get();
				final List<CheckpointResultBean> checkpoints = new ArrayList<CheckpointResultBean>(
						stb.getCheckPointResults());

				// pro
				final List<LoggingBean> logs = new ArrayList<LoggingBean>(stb.getLog());
				ITestContext testContext = (ITestContext) tr.getAttribute("context");
				ReporterUtil.createMethodResult(testContext, tr, logs, checkpoints);
			}
		}
		if (tr.getStatus() != ITestResult.SKIP) {
			getBundle().clearProperty(RetryAnalyzer.RETRY_INVOCATION_COUNT);
		}
	}

	@SuppressWarnings("unchecked")
	private void deployResult(ITestResult tr) {
		try {
			if (ResultUpdator.getResultUpdatorsCnt()>0 && ((tr.getStatus() == ITestResult.FAILURE)
					|| (tr.getStatus() == ITestResult.SUCCESS || tr.getStatus() == ITestResult.SKIP))) {

				TestCaseRunResult.Status status = tr.getStatus() == ITestResult.SUCCESS ? TestCaseRunResult.Status.PASS
						: tr.getStatus() == ITestResult.FAILURE ? TestCaseRunResult.Status.FAIL
								: TestCaseRunResult.Status.SKIPPED;

				Map<String, Object> params = new HashMap<String, Object>();	
				Map<String, Object> metadata;
				String clsName;
				Collection<String> steps;

				if(tr.getMethod() instanceof TestNGScenario) {
					TestNGScenario scenario = (TestNGScenario) tr.getMethod();
					metadata = scenario.getMetaData();
					params.putAll(metadata);
					clsName= scenario.getClassOrFileName();
					steps = scenario.getSteps();

				}else {
					metadata = new HashMap<String, Object>();
					metadata.put("name",tr.getName());
					clsName=tr.getMethod().getRealClass().getName();
					steps = Collections.emptyList();
				}
				
				params.put("duration", tr.getEndMillis() - tr.getStartMillis());

				Map<String, Object> executionInfo = new HashMap<String, Object>();
				XmlSuite suite = tr.getTestContext().getSuite().getXmlSuite();
				if(suite.getParentSuite()==null) {
					executionInfo.put("testName", tr.getTestContext().getName());
					executionInfo.put("suiteName", suite.getName());
				}else {
					executionInfo.put("testName", suite.getName()+"_"+tr.getTestContext().getName());
					executionInfo.put("suiteName", suite.getParentSuite().getName());
				}
				
				Map<String, Object> runPrams = new HashMap<String, Object>(
						tr.getTestContext().getCurrentXmlTest().getAllParameters());
				runPrams.putAll(ConfigurationConverter.getMap(getBundle().subset("env")));
				executionInfo.put("env", runPrams);
				int retryCount = getBundle().getInt(RetryAnalyzer.RETRY_INVOCATION_COUNT, 0);
				boolean willRetry =  getBundle().getBoolean(RetryAnalyzer.WILL_RETRY, false);
				getBundle().clearProperty(RetryAnalyzer.WILL_RETRY);
				if(retryCount>0) {
					executionInfo.put("retryCount", retryCount);
				}
				TestCaseRunResult testCaseRunResult = new TestCaseRunResult(status, metadata,
						tr.getParameters(), executionInfo, steps, tr.getStartMillis(),willRetry,tr.getMethod().isTest() );
				testCaseRunResult.setClassName(clsName);
				if (tr.getMethod().getGroups() != null && tr.getMethod().getGroups().length > 0) {
					testCaseRunResult.getMetaData().put("groups", tr.getMethod().getGroups());
				}
				testCaseRunResult.getMetaData().put("description",tr.getMethod().getDescription());
				testCaseRunResult.setThrowable(tr.getThrowable());
				ResultUpdator.updateResult(testCaseRunResult);
			}
		} catch (Exception e) {
			logger.warn("Unable to deploy result", e);
		}
	}

	private boolean skipReporting() {
		return getBundle().getBoolean("disable.qaf.testng.reporter", true)
				|| getBundle().getBoolean("cucumber.run.mode", false);
	}
}
