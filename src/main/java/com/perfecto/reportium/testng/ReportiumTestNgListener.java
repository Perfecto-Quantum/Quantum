package com.perfecto.reportium.testng;

import org.testng.IClass;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener to report test start / end to Reportium backend
 */
public class ReportiumTestNgListener extends BasicReportiumTestNgListener {

    @Override
    protected String[] getTags(ITestResult testResult) {
        Class<? extends IClass> testClass = testResult.getTestClass().getClass();
        List<String> tags = extractTagsFromAnnotations(testClass.getAnnotations());

        Method method = testResult.getMethod().getConstructorOrMethod().getMethod();
        if (method != null) {
            tags.addAll(extractTagsFromAnnotations(method.getAnnotations()));
        }
        return tags.toArray(new String[tags.size()]);
    }

    protected List<String> extractTagsFromAnnotations(Annotation[] annotations) {
        List<String> tags = new ArrayList<>();

        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Test.class)) {
                Test testAnnotation = ((Test) annotation);
                addNonEmptyTag(tags, testAnnotation.suiteName());
                addNonEmptyTag(tags, testAnnotation.testName());
                addNonEmptyTag(tags, testAnnotation.description());
                for (String group : testAnnotation.groups()) {
                    addNonEmptyTag(tags, group);
                }
            }
        }
        return tags;
    }

    private void addNonEmptyTag(List<String> tags, String value) {
        if (value != null && value.length() > 0) {
            tags.add(value);
        }
    }
}
