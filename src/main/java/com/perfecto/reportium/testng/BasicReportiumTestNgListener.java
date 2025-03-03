package com.perfecto.reportium.testng;

import com.perfecto.reportium.client.ReportiumClientProvider;
import com.perfecto.reportium.WebDriverProvider;
import com.perfecto.reportium.client.ReportiumClient;
import com.perfecto.reportium.client.ReportiumClientFactory;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.PerfectoExecutionContext;
import com.perfecto.reportium.model.Project;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

public class BasicReportiumTestNgListener extends BaseReportiumTestNgListener {
    private final static ReportiumClientFactory reportiumClientFactory = new ReportiumClientFactory();

    @Override
    protected void reportTestStart(ITestResult testResult) {
        ReportiumClientProvider.set(createReportiumClient(testResult));
        super.reportTestStart(testResult);
    }

    protected ReportiumClient createReportiumClient(ITestResult testResult) {
        Project project = getProject();
        Job job = getJob();
        String[] tags = getTags(testResult);
        PerfectoExecutionContext perfectoExecutionContext =
                new PerfectoExecutionContext.PerfectoExecutionContextBuilder()
                        .withContextTags(tags)
                        .withWebDriver(getWebDriver(testResult))
                        .withJob(job)
                        .withProject(project)
                        .build();
        return reportiumClientFactory.createPerfectoReportiumClient(perfectoExecutionContext);
    }

    protected WebDriver getWebDriver(ITestResult testResult) {
        Object testInstance = testResult.getInstance();
        if (testInstance instanceof WebDriverProvider) {
            return ((WebDriverProvider) testInstance).getWebDriver();
        }
        throw new RuntimeException("Unable to get WebDriver instance");
    }
}
