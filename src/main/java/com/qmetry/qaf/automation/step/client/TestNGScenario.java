package com.qmetry.qaf.automation.step.client;

import static com.qmetry.qaf.automation.data.MetaDataScanner.getMetadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.NoOpTestClass;
import org.testng.internal.TestNGMethod;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.internal.objects.DefaultTestObjectFactory;
import org.testng.xml.XmlSuite.ParallelMode;
import org.testng.xml.XmlTest;

import com.qmetry.qaf.automation.step.TestStep;
import com.qmetry.qaf.automation.step.TestStepCompositer;
import com.qmetry.qaf.automation.util.ClassUtil;
import com.qmetry.qaf.automation.util.StringUtil;

/**
 * com.qmetry.qaf.automation.step.client.TestNGScenario.java
 * 
 * @author chirag.jayswal
 */
public class TestNGScenario extends TestNGMethod {

	/**
	 * 
	 */
	
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 6225163528424712337L;
	//private Scenario scenario;
	private TestStepCompositer testStepCompositer;

	private Map<String, Object> metadata;
	private String qualifiledName;

	public TestNGScenario(TestNGMethod testNGMethod) {
		this(testNGMethod.getConstructorOrMethod().getMethod(), (IAnnotationFinder) ClassUtil.getField("m_annotationFinder", testNGMethod), testNGMethod.getXmlTest(), testNGMethod.getInstance());
		if(null!=testNGMethod.getTestClass()) {
			setTestClass(testNGMethod.getTestClass());
		}
	}
	
	public TestNGScenario(Method method, IAnnotationFinder finder, XmlTest xmlTest, Object instance) {
//		super(method, finder, xmlTest, instance);
		
		super(new DefaultTestObjectFactory(), method, finder, xmlTest, instance);
		init(instance);
	}

	private void init(Object instance) {
		if (Scenario.class.isAssignableFrom(getRealClass())) {
			Scenario scenario = (Scenario) instance;
			testStepCompositer=scenario;
			if (scenario.getPriority() < 1000 || !getXmlTest().getParallel().isParallel()
					|| getXmlTest().getParallel().equals(ParallelMode.TESTS)) {
				setPriority(scenario.getPriority());
			}
			
			
			String[] groups = scenario.getM_groups();
			setGroups(groups);
			setGroupsDependedUpon(scenario.getM_groupsDependedUpon(), new ArrayList<String>());
			setMethodsDependedUpon(scenario.getM_methodsDependedUpon());
			setDescription(scenario.getDescription());
			setEnabled(scenario.isM_enabled());
			setAlwaysRun(scenario.isM_isAlwaysRun());
			setIgnoreMissingDependencies(scenario.getIgnoreMissingDependencies());
			metadata = scenario.getMetadata();
			qualifiledName = scenario.getTestName();
			setTimeOut(scenario.getTimeOut());
			
		} else {
			metadata = getMetadata(getConstructorOrMethod().getMethod(), true);
			qualifiledName = getRealClass().getName() + "." + getMethodName();
		}
		if(!metadata.containsKey("name")) {
			metadata.put("name", getMethodName());
		}
		metadata.put("sign", getSignature());

		//formatMetaData(metadata);
	}

	//package access
	void setTestStepCompositer(TestStepCompositer testStepCompositer) {
		this.testStepCompositer = testStepCompositer;
	}
	
	private Scenario getScenario() {
		if (Scenario.class.isAssignableFrom(getRealClass())) {
			return  (Scenario) testStepCompositer;
		}
		return null;
	}
	
	@Override
	public String getMethodName() {
		return getScenario() != null ? getScenario().getTestName() : super.getMethodName();
	}

	@Override
	public String getSignature() {
		return getScenario() != null ? computeSign() : super.getSignature();
	}

	private String computeSign() {
		StringBuilder result = new StringBuilder(getScenario().getSignature());

		result.append("[pri:").append(getPriority()).append(", instance:").append(getInstance()).append("]");
		return result.toString();
	}

	public Map<String, Object> getMetaData() {
		return metadata;
	}

	// useful to correct invocation count in case of retry
	public int decAndgetCurrentInvocationCount() {
		m_currentInvocationCount = new AtomicInteger(getCurrentInvocationCount() - 1);
		return super.getCurrentInvocationCount();
	}

	@Override
	public String getQualifiedName() {
		return qualifiledName;
	}

	public Collection<String> getSteps() {
		if (testStepCompositer != null) {
			List<String> steps = new ArrayList<String>();
			for (TestStep step : testStepCompositer.getSteps()) {
				steps.add(step.getDescription());
			}
			return steps;
		}
		return Collections.emptyList();
	}
	
	public String getClassOrFileName(){
		if (getScenario() != null) {
			if (StringUtil.isNotBlank(getScenario().getFileName())) {
				return getScenario().getFileName();
			}
		}
		return getRealClass().getName();
	}
	
	public BaseTestMethod clone() {
		TestNGScenario clone = new TestNGScenario(getConstructorOrMethod().getMethod(), getAnnotationFinder(),
				getXmlTest(), getInstance());

		ITestClass tc = getTestClass();
		NoOpTestClass testClass = new NoOpTestClass(tc);
		testClass.setBeforeTestMethods(clone(tc.getBeforeTestMethods()));
		testClass.setAfterTestMethod(clone(tc.getAfterTestMethods()));
		clone.m_testClass = testClass;
		return clone;
	}

	private ITestNGMethod[] clone(ITestNGMethod[] sources) {
		ITestNGMethod[] clones = new ITestNGMethod[sources.length];
		for (int i = 0; i < sources.length; i++) {
			clones[i] = sources[i].clone();
		}
		return clones;
	}
}
