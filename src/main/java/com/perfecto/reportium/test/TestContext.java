package com.perfecto.reportium.test;

import com.perfecto.reportium.model.CustomField;
import com.perfecto.reportium.model.util.ExecutionContextPopulator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.*;

/**
 * Class denoting the test context, e.g. one or all of the following: test suite, CI build number, SCM branch name, ...
 */
public class TestContext {

    private final Set<String> testExecutionTags;
    private final Set<CustomField> customFields;

    public TestContext(String... testExecutionTags) {
        if (testExecutionTags != null && testExecutionTags.length > 0) {
            Set<String> tags = new HashSet<>();
            for (String tag : testExecutionTags) {
                if (StringUtils.isNotBlank(tag)) {
                    tags.add(tag);
                }
            }
            this.testExecutionTags = Collections.unmodifiableSet(tags);
        } else {
            this.testExecutionTags = Collections.emptySet();
        }
        this.customFields = Collections.unmodifiableSet(new HashSet<CustomField>());
    }

    protected TestContext(Builder builder) {
        testExecutionTags = Collections.unmodifiableSet(builder.testExecutionTags);
        this.customFields = Collections.unmodifiableSet(ExecutionContextPopulator.populateMissingCustomFieldsPropertiesFromEnvVariables(builder.customFields));
    }

    public Set<String> getTestExecutionTags() {
        return testExecutionTags;
    }

    public Set<CustomField> getCustomFields() {
        return customFields;
    }

    public static class Builder<T extends Builder<T>> {
        private Set<String> testExecutionTags;
        private Set<CustomField> customFields;

        public Builder() {
            this.testExecutionTags = new HashSet<>();
            this.customFields = new HashSet<>();
        }

        public Builder(TestContext copy) {
            this.testExecutionTags = copy.testExecutionTags;
            this.customFields = copy.customFields;
        }

        public T withTestExecutionTags(Collection<String> testExecutionTags) {
            if (testExecutionTags != null && testExecutionTags.size() > 0) {
                for (String testExecutionTag : testExecutionTags) {
                    if (StringUtils.isNotBlank(testExecutionTag)) {
                        this.testExecutionTags.add(testExecutionTag);
                    }
                }
            }
            return (T) this;
        }

        public T withTestExecutionTags(String... testExecutionTags) {
            if (testExecutionTags != null) {
                withTestExecutionTags(Arrays.asList(testExecutionTags));
            }
            return (T) this;
        }

        public T withCustomFields(Collection<CustomField> customFields) {
            if (customFields != null && customFields.size() > 0) {
                for (CustomField customField : customFields) {
                    if (customField != null) {
                        this.customFields.add(customField);
                    }
                }
            }
            return (T) this;
        }

        public T withCustomFields(CustomField... customFields) {
            if (customFields != null) {
                withCustomFields(Arrays.asList(customFields));
            }
            return (T) this;
        }

        public TestContext build() {
            return new TestContext(this);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("testExecutionTags", testExecutionTags)
                .append("customFields", customFields)
                .toString();
    }
}
