package com.perfecto.reportium.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.perfecto.reportium.exception.ReportiumException;
import com.perfecto.reportium.model.CustomField;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.PerfectoExecutionContext;
import com.perfecto.reportium.model.Project;
import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.result.TestResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.*;
import java.util.logging.Logger;

import static com.perfecto.reportium.model.util.ExecutionContextPopulator.EQUALS;

/**
 * Reportium instance that transmits test data to MCM
 */
class PerfectoReportiumClient implements ReportiumClient {

    private final Logger LOGGER = Logger.getLogger(PerfectoReportiumClient.class.getName());

    private final static String START_TEST_COMMAND = "mobile:test:start";
    private final static String START_STEP_COMMAND = "mobile:step:start";
    private final static String END_STEP_COMMAND = "mobile:step:end";
    private final static String END_TEST_COMMAND = "mobile:test:end";
    private final static String ASSERT_COMMAND = "mobile:status:assert";
    private final static String MULTIPLE_EXECUTIONS_COMMAND = "mobile:execution:multiple";
    private final static String CUSTOM_FIELDS_PARAM_NAME = "customFields";
    private final static String TAGS_PARAM_NAME = "tags";

    @Deprecated
    private final static String TEST_STEP_COMMAND = "mobile:test:step";

    private final PerfectoExecutionContext perfectoExecutionContext;

    /**
     * Creates a new instance
     *
     * @param perfectoExecutionContext Test execution context details
     */
    PerfectoReportiumClient(PerfectoExecutionContext perfectoExecutionContext) {
        this.perfectoExecutionContext = perfectoExecutionContext;

        // Report multiple executions event
        reportMultipleExecutionsEvent();
    }

    private void reportMultipleExecutionsEvent() {
        // Check if needs to support multiple executions
        List<Pair<String, WebDriver>> webDriverPairs = perfectoExecutionContext.getWebDriverPairs();
        if (webDriverPairs.size() > 1) {
            // Prepare the ExternalId/Alias map
            List<Map<String, String>> externalIdAliases = new LinkedList<>();
            for (Pair<String, WebDriver> webDriverPair : webDriverPairs) {
                Map<String, String> externalIdAlias = new HashMap<>();
                externalIdAlias.put("externalId", extractExternalId(webDriverPair.getRight()));
                externalIdAlias.put("alias", normalizeAlias(webDriverPair.getLeft()));
                externalIdAliases.add(externalIdAlias);
            }

            // Serialize the parameter
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(externalIdAliases);

            // Prepare the command params
            Map<String, Object> params = new HashMap<>();
            params.put("externalIdAliases", json);

            // Send multiple executions command
            executeScript(MULTIPLE_EXECUTIONS_COMMAND, params);
        }
    }

    private String normalizeAlias(String alias) {
        // Normalized the alias
        return StringUtils.trim(alias)
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .replaceAll("^[ ]+|[ ]+$", "");
    }

    private String extractExternalId(WebDriver webDriver) {
        // Extract the external id
        return (String) ((HasCapabilities) webDriver).getCapabilities().getCapability(Constants.Capabilities.executionId);
    }

    @Override
    public void testStart(String name, TestContext context) {
        // Send test-start command
        Map<String, Object> params = new HashMap<>();
        Job job = perfectoExecutionContext.getJob();
        if (job != null) {
            params.put("jobName", job.getName());
            params.put("jobNumber", job.getNumber());
            params.put("jobBranch", job.getBranch());
        }
        Project project = perfectoExecutionContext.getProject();
        if (project != null) {
            params.put("projectName", project.getName());
            params.put("projectVersion", project.getVersion());
        }

        params.put("name", name);
        List<String> tags = new ArrayList<>(perfectoExecutionContext.getContextTags());
        tags.addAll(context.getTestExecutionTags());
        params.put(TAGS_PARAM_NAME, tags);

        List<String> customFieldsPair = new ArrayList<>();
        Set<String> existingCustomFields = new HashSet<>();
        createCustomFieldsParamsValuePairs(context.getCustomFields(), existingCustomFields, customFieldsPair);
        createCustomFieldsParamsValuePairs(perfectoExecutionContext.getCustomFields(), existingCustomFields, customFieldsPair);
        params.put(CUSTOM_FIELDS_PARAM_NAME, customFieldsPair);
        executeScript(START_TEST_COMMAND, params);
    }

    private void createCustomFieldsParamsValuePairs(Set<CustomField> populatedCustomFields,
                                                    Set<String> currentCustomFieldsNames,
                                                    List<String> pairs) {
        if (pairs == null) {
            return;
        }

        if (currentCustomFieldsNames == null) {
            currentCustomFieldsNames = new HashSet<>();
        }

        for (CustomField customField : populatedCustomFields) {
            String value = StringUtils.isBlank(customField.getValue()) ? "" : customField.getValue();
            String name = customField.getName();
            if (StringUtils.isBlank(name)) {
                throw new ReportiumException("Custom field name cannot be empty");
            }
            if (!currentCustomFieldsNames.contains(name))
                pairs.add(name + EQUALS + value);
            currentCustomFieldsNames.add(name);
        }
    }

    @Override
    @Deprecated
    public void testStep(String description) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", description);
        executeScript(TEST_STEP_COMMAND, params);
    }

    @Override
    public void stepStart(String description) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", description);
        executeScript(START_STEP_COMMAND, params);
    }

    @Override
    public void stepEnd() {
        stepEnd(null);
    }

    @Override
    public void stepEnd(String message) {
        Map<String, Object> params = new HashMap<>();
        params.put("message", message);
        executeScript(END_STEP_COMMAND, params);
    }

    @Override
    public void testStop(TestResult testResult) {
        testStop(testResult, new TestContext());
    }

    @Override
    public void testStop(TestResult testResult, TestContext testContext) {
        Map<String, Object> params = new HashMap<>();
        testResult.visit(new PerfectoTestResultVisitor(params));

        if (testContext != null) {
            Set<String> testExecutionTags = testContext.getTestExecutionTags();
            if (testExecutionTags != null && !testExecutionTags.isEmpty()) {
                List<String> tags = new ArrayList<>(testExecutionTags);
                params.put(TAGS_PARAM_NAME, tags);
            }

            Set<CustomField> customFields = testContext.getCustomFields();
            if (customFields != null && !customFields.isEmpty()) {
                List<String> customFieldsPair = new ArrayList<>();
                createCustomFieldsParamsValuePairs(customFields, new HashSet<String>(), customFieldsPair);
                params.put(CUSTOM_FIELDS_PARAM_NAME, customFieldsPair);
            }
        }

        executeScript(END_TEST_COMMAND, params);
    }

    @Override
    public void reportiumAssert(String message, boolean status) {
        Map<String, Object> params = new HashMap<>();
        params.put("message", message);
        params.put("status", status);
        executeScript(ASSERT_COMMAND, params);
    }

    @Override
    public String getReportUrl() {
        WebDriver webDriver = perfectoExecutionContext.getWebDriver();
        if (!(webDriver instanceof HasCapabilities)) {
            // Driver is expected to have capabilities to run via Perfecto grid.
            throw new ReportiumException("WebDriver instance is assumed to have Selenium Capabilities");
        }

        Object value = ((HasCapabilities) webDriver).getCapabilities().getCapability(Constants.Capabilities.executionReportUrl);
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    private void executeScript(String script, Map<String, Object> params) {
        // Execute script
        WebDriver webDriver = perfectoExecutionContext.getWebDriver();
        ((JavascriptExecutor) webDriver).executeScript(script, params);
    }
}